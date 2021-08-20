package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.solr.EdgeDocument;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ardc.cerium.mycelium.provider.RIFCSGraphProvider.RELATION_RELATED_TO;

@Slf4j
@Service
public class MyceliumIndexingService {

	public static final String ORIGIN_GRANTS_NETWORK = "GrantsNetwork";

	private final SolrTemplate solrTemplate;

	private final RelationshipDocumentRepository relationshipDocumentRepository;

	private final GraphService graphService;

	private final VertexRepository vertexRepository;

	public MyceliumIndexingService(SolrTemplate solrTemplate,
			RelationshipDocumentRepository relationshipDocumentRepository, GraphService graphService,
			VertexRepository vertexRepository) {
		this.solrTemplate = solrTemplate;
		this.relationshipDocumentRepository = relationshipDocumentRepository;
		this.graphService = graphService;
		this.vertexRepository = vertexRepository;
	}

	/**
	 * Index a {@link Vertex} by taking the data available in the Graph Service and index
	 * them into SOLR
	 *
	 * Covers: Direct (include PrimaryKey) and GrantsNetwork. Include Reverse Edges,
	 * Source Duplicates and Target Duplicates
	 * @param from the {@link Vertex} to index
	 */
	public void indexVertex(Vertex from) {

		// index all direct (1 step away) relationships, source duplicates and target
		// duplicates included
		indexDirectRelationships(from);

		// index implicit links based on the class
		switch (from.getObjectClass()) {
		case "collection":
			indexImplicitLinksForCollection(from);
			break;
		case "party":
			indexImplicitLinksForParty(from);
			break;
		case "activity":
			indexImplicitLinksForActivity(from);
			break;
		}
	}

	/**
	 * Index all relationships that are considered Direct
	 *
	 * This includes: RelatedObject, RelatedInfo, PrimaryKey. Reversed, Source Duplicates
	 * and Target Duplicates included
	 * @param from the {@link Vertex} to index from
	 */
	public void indexDirectRelationships(Vertex from) {

		// todo convert the GraphService#getDuplicateRelationships function to use direct repository interface & improve pagination
		// current implementation only covers 1000 relationships & source duplicates
		Collection<Relationship> relationships = graphService.getMyDuplicateRelationships(from.getIdentifier(),
				from.getIdentifierType(), PageRequest.of(0, 1000));

		relationships.forEach(relationship -> {
			Vertex to = relationship.getTo();
			log.debug("Related Entity {}", to);

			// target duplicates
			Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(to.getIdentifier(), to.getIdentifierType());
			Collection<Vertex> toRelatedObjects = sameAsNodeCluster.stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());

			if (toRelatedObjects.size() > 0) {
				log.debug("Resolved {} relatedObjects", toRelatedObjects.size());
				toRelatedObjects
						.forEach(toRelatedObject -> indexRelation(from, toRelatedObject, relationship.getRelations()));
			}
			else {
				// does not resolve to registryObject it's a relatedInfo relation
				log.debug("Does not resolve to any relatedObject. Index as RelatedInfo");
				indexRelation(from, to, relationship.getRelations());
			}
		});
	}

	public void indexRelation(Vertex from, Vertex to, List<EdgeDTO> relations) {
		log.debug("Indexing relation from {} to {}", from.getIdentifier(), to.getIdentifier());
		RelationshipDocument doc = new RelationshipDocument();
		doc.setFromId(from.getIdentifier());
		doc.setFromClass(from.getObjectClass());
		doc.setFromType(from.getObjectType());
		doc.setFromTitle(from.getTitle());
		doc.setToIdentifier(to.getIdentifier());
		doc.setToIdentifierType(to.getIdentifierType());
		doc.setToClass(to.getObjectClass());
		doc.setToType(to.getObjectType());
		doc.setToTitle(to.getTitle());
		List<EdgeDocument> edges = new ArrayList<>();
		relations.forEach(relation -> {
			EdgeDocument edge = new EdgeDocument(relation.getType());
			edge.setRelationOrigin(relation.getOrigin());
			edge.setRelationInternal(relation.isInternal());
			edge.setRelationReverse(relation.isReverse());
			edges.add(edge);
		});
		doc.setRelations(edges);
		indexRelationshipDocument(doc);
	}

	public void indexRelationshipDocument(RelationshipDocument doc) {

		// search for existing document in an attempt to update it
		Query query = new SimpleQuery("*:*");
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("from_id").is(doc.getFromId())));
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier").is(doc.getToIdentifier())));
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier_type").is(doc.getToIdentifierType())));
		query.addProjectionOnField(new SimpleField("*"));
		query.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));
		Optional<RelationshipDocument> existing = solrTemplate.queryForObject("relationships", query,
				RelationshipDocument.class);

		if (existing.isPresent()) {
			log.debug("Found existing");
			RelationshipDocument existingDocument = existing.get();

			doc.getRelations().forEach(relation -> {
				if (!existingDocument.getRelations().contains(relation)) {
					log.debug("Doesn't contain relation {} adding...", relation.getRelationType());
					existingDocument.getRelations().add(relation);
				}
			});

			log.debug("Saving existing document");
			relationshipDocumentRepository.save(existingDocument);
		}
		else {
			log.debug("Does not found existing, saving new doc");
			relationshipDocumentRepository.save(doc);
		}
	}

	@Transactional
	public void indexImplicitLinksForActivity(Vertex from) {

		// the activity hasOutput all child collections
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildCollections(from.getIdentifier())) {
			stream.forEach(collection -> indexGrantsNetworkRelation(from, collection, "hasOutput"));
		}

		// the activity hasPart all child activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildActivities(from.getIdentifier())) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "hasPart"));
		}

		// the activity isPartOf all parent activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentActivities(from.getIdentifier())) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isPartOf"));
		}

		// the activity isFundedBy all funder
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentParties(from.getIdentifier())) {
			stream.forEach(party -> indexGrantsNetworkRelation(from, party, "isFundedBy"));
		}
	}

	@Transactional(readOnly = true)
	public void indexImplicitLinksForParty(Vertex from) {

		// the party isFunderOf all child activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildActivities(from.getIdentifier())) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isFunderOf"));
		}

		// the party isFunderOf all child collections
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildCollections(from.getIdentifier())) {
			stream.forEach(collection -> indexGrantsNetworkRelation(from, collection, "isFunderOf"));
		}
	}

	@Transactional(readOnly = true)
	public void indexImplicitLinksForCollection(Vertex from) {

		Collection<String> myDuplicateIDs = graphService.getSameAs(from.getIdentifier(), from.getIdentifierType())
				.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).map(Vertex::getIdentifier)
				.collect(Collectors.toList());

		// the collection hasPart all child collections (not its duplicates)
		try (Stream<Vertex> stream = vertexRepository.streamChildCollections(from.getIdentifier())) {
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier()))
					.forEach(collection -> indexGrantsNetworkRelation(from, collection, "hasPart"));
		}

		// the collection isPartOf all parent collections (not its duplicates)
		try (Stream<Vertex> stream = vertexRepository.streamParentCollections(from.getIdentifier())) {
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier()))
					.forEach(collection -> indexGrantsNetworkRelation(from, collection, "isPartOf"));
		}

		// the collection isOutputOf all parent activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentActivities(from.getIdentifier())) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isOutputOf"));
		}

		// the collection isFundedBy all funder
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentParties(from.getIdentifier())) {
			stream.forEach(party -> indexGrantsNetworkRelation(from, party, "isFundedBy"));
		}
	}

	private void indexGrantsNetworkRelation(Vertex from, Vertex to, String relation) {

		// index the implicit edge
		EdgeDTO edge = new EdgeDTO();
		edge.setOrigin(MyceliumIndexingService.ORIGIN_GRANTS_NETWORK);
		edge.setType(relation);
		indexRelation(from, to, new ArrayList<>(List.of(edge)));

		// index the reversed edge
		EdgeDTO reversed = new EdgeDTO();
		reversed.setOrigin(MyceliumIndexingService.ORIGIN_GRANTS_NETWORK);
		reversed.setType(RelationLookupService.getReverse(relation, RELATION_RELATED_TO));
		reversed.setReverse(true);
		indexRelation(to, from, new ArrayList<>(List.of(reversed)));
	}

}
