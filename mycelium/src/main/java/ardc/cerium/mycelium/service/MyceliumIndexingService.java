package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.solr.EdgeDocument;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ardc.cerium.mycelium.provider.RIFCSGraphProvider.*;

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
		log.debug("Mycelium Indexing Service online");
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
		log.debug("Indexing Vertex[from={}]", from.getIdentifier());

		// index all direct (1 step away) relationships, source duplicates and target
		// duplicates included
		deleteAllRelationship(from);
		indexDirectRelationships(from);
		indexGrantsNetworkRelationships(from);
	}

	/**
	 * Index GrantsNetworkRelationships based on the class
	 * @param from the {@link Vertex} to index
	 */
	public void indexGrantsNetworkRelationships(Vertex from) {
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

	public void deleteAllRelationship(Vertex from) {
		relationshipDocumentRepository.deleteAllByFromIdEquals(from.getIdentifier());
		relationshipDocumentRepository.deleteAllByToIdentifierEquals(from.getIdentifier());
	}

	public void deleteGrantsNetworkEdges(Vertex from) {
		log.debug("Deleting Grants Network Edges from Vertex[id={}, type={}]", from.getIdentifier(),
				from.getIdentifierType());
		Cursor<RelationshipDocument> cursor = cursorFor(new Criteria("from_id").is(from.getIdentifier()));
		while (cursor.hasNext()) {
			RelationshipDocument doc = cursor.next();
			if (doc.getRelations() == null || doc.getRelations().size() == 0) {
				relationshipDocumentRepository.delete(doc);
				continue;
			}
			List<EdgeDocument> updatedEdges = doc.getRelations().stream()
					.filter(relation -> relation.getRelationOrigin() != null)
					.filter(relation -> !relation.getRelationOrigin().equals(ORIGIN_GRANTS_NETWORK))
					.filter(relation -> relation.getFromId() != null)
					.filter(relation -> relation.getFromId().equals(from.getIdentifier())).collect(Collectors.toList());
			if (updatedEdges.size() > 0) {
				doc.setRelations(updatedEdges);
				relationshipDocumentRepository.save(doc);
			}
			else {
				relationshipDocumentRepository.delete(doc);
			}
		}
		solrTemplate.commit("relationships");

		log.debug("Deleting Grants Network Edges to Vertex[id={}, type={}]", from.getIdentifier(),
				from.getIdentifierType());
		Cursor<RelationshipDocument> toCursor = cursorFor(new Criteria("to_identifier").is(from.getIdentifier()));
		while (toCursor.hasNext()) {
			RelationshipDocument doc = toCursor.next();
			if (doc.getRelations() == null || doc.getRelations().size() == 0) {
				relationshipDocumentRepository.delete(doc);
				continue;
			}
			List<EdgeDocument> updatedEdges = doc.getRelations().stream()
					.filter(relation -> relation.getRelationOrigin() != null)
					.filter(relation -> relation.getToIdentifier() != null)
					.filter(relation -> !relation.getRelationOrigin().equals(ORIGIN_GRANTS_NETWORK)
							&& relation.getToIdentifier().equals(from.getIdentifier()))
					.collect(Collectors.toList());
			if (updatedEdges.size() > 0) {
				doc.setRelations(updatedEdges);
				relationshipDocumentRepository.save(doc);
			}
			else {
				relationshipDocumentRepository.delete(doc);
			}
		}
		solrTemplate.commit("relationships");
	}

	public void deletePrimaryKeyEdges(String registryObjectId) {
		log.debug("Deleting PrimaryKey Edges from Vertex[id={}]", registryObjectId);
		Cursor<RelationshipDocument> cursor = cursorFor(new Criteria("from_id").is(registryObjectId));
		while (cursor.hasNext()) {
			RelationshipDocument doc = cursor.next();
			if (doc.getRelations() == null || doc.getRelations().size() == 0) {
				relationshipDocumentRepository.delete(doc);
				continue;
			}
			List<EdgeDocument> updatedEdges = doc.getRelations().stream()
					.filter(relation -> relation.getRelationOrigin() != null)
					.filter(relation -> !relation.getRelationOrigin().equals(ORIGIN_PRIMARY_LINK))
					.filter(relation -> relation.getFromId() != null)
					.filter(relation -> relation.getFromId().equals(registryObjectId)).collect(Collectors.toList());
			if (updatedEdges.size() > 0) {
				doc.setRelations(updatedEdges);
				relationshipDocumentRepository.save(doc);
			}
			else {
				relationshipDocumentRepository.delete(doc);
			}
		}
		solrTemplate.commit("relationships");

		log.debug("Deleting PrimaryKey Edges to Vertex[id={}]", registryObjectId);
		Cursor<RelationshipDocument> toCursor = cursorFor(new Criteria("to_identifier").is(registryObjectId));
		while (toCursor.hasNext()) {
			RelationshipDocument doc = toCursor.next();
			if (doc.getRelations() == null || doc.getRelations().size() == 0) {
				relationshipDocumentRepository.delete(doc);
				continue;
			}
			List<EdgeDocument> updatedEdges = doc.getRelations().stream()
					.filter(relation -> relation.getRelationOrigin() != null)
					.filter(relation -> relation.getToIdentifier() != null)
					.filter(relation -> !relation.getRelationOrigin().equals(ORIGIN_PRIMARY_LINK)
							&& relation.getToIdentifier().equals(registryObjectId))
					.collect(Collectors.toList());
			if (updatedEdges.size() > 0) {
				doc.setRelations(updatedEdges);
				relationshipDocumentRepository.save(doc);
			}
			else {
				relationshipDocumentRepository.delete(doc);
			}
		}
		solrTemplate.commit("relationships");
	}

	public void regenGrantsNetworkRelationships(Vertex from) {
		log.debug("Regenerating GrantsNetwork Relationship for Vertex[id={}, type={}]", from.getIdentifier(),
				from.getIdentifierType());
		deleteGrantsNetworkEdges(from);
		indexGrantsNetworkRelationships(from);
	}

	public Cursor<RelationshipDocument> cursorFor(Criteria... criterion) {
		Query query = new SimpleQuery("*:*");
		query.addSort(Sort.by("id"));
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("type").is("relationship")));
		for (Criteria criteria : criterion) {
			query.addFilterQuery(new SimpleFilterQuery(criteria));
		}
		query.addProjectionOnField(new SimpleField("*"));
		query.addProjectionOnField(
				new SimpleField("[child parentFilter=type:relationship childFilter=type:edge limit=100]"));

		return solrTemplate.queryForCursor("relationships", query, RelationshipDocument.class);
	}

	/**
	 * Index all relationships that are considered Direct
	 *
	 * todo improve pagination, currently only index 1000 direct edges This includes:
	 * RelatedObject, RelatedInfo, PrimaryKey. Reversed, Source Duplicates and Target
	 * Duplicates included
	 * @param from the {@link Vertex} to index from
	 */
	public void indexDirectRelationships(Vertex from) {
		log.debug("Indexing Direct Relationships Vertex[id={}]", from.getIdentifier());

		// todo convert the GraphService#getDuplicateRelationships function to use direct
		// repository interface & improve pagination
		// current implementation only covers 1000 relationships & source duplicates
		Collection<Relationship> relationships = graphService.getMyDuplicateRelationships(from.getIdentifier(),
				from.getIdentifierType(), PageRequest.of(0, 1000));

		log.debug("Found {} direct relationships for Vertex[id={}]", relationships.size(), from.getIdentifier());
		relationships.forEach(relationship -> {
			Vertex to = relationship.getTo();
			log.trace("RelatedEntity[id={}, type={}]", to.getIdentifier(), to.getIdentifierType());

			// target duplicates
			Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(to.getIdentifier(), to.getIdentifierType());
			Collection<Vertex> toRelatedObjects = sameAsNodeCluster.stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());
			log.trace("RelatedEntity Duplicate count: {}", toRelatedObjects.size());

			if (toRelatedObjects.size() > 0) {
				log.trace("Resolved {} relatedObjects", toRelatedObjects.size());
				toRelatedObjects.forEach(toRelatedObject -> {
					indexRelation(from, toRelatedObject, relationship.getRelations());

					List<EdgeDTO> reversedRelations = relationship.getRelations().stream()
							.map(edgeDTO -> RelationUtil.getReversed(edgeDTO, RELATION_RELATED_TO))
							.collect(Collectors.toList());
					indexRelation(toRelatedObject, from, reversedRelations);
				});
			}
			else if (!to.getIdentifierType().equals(RIFCS_KEY_IDENTIFIER_TYPE)) {
				// does not resolve to registryObject it's a relatedInfo relation
				log.trace("Does not resolve to any relatedObject. Index as RelatedInfo");
				indexRelation(from, to, relationship.getRelations());
			}
		});
	}

	/**
	 * Useful method to index a list of directional edges between 2 vertex into SOLR
	 *
	 * Constructs the {@link RelationshipDocument} and send them over to
	 * {@link #indexRelationshipDocument(RelationshipDocument)} for indexing
	 * @param from the source {@link Vertex}
	 * @param to the target {@link Vertex}
	 * @param relations the {@link List} of {@link EdgeDTO} that contains the relations
	 */
	public void indexRelation(Vertex from, Vertex to, List<EdgeDTO> relations) {
		List<String> relationTypes = relations.stream().map(EdgeDTO::getType).collect(Collectors.toList());
		List<Boolean> directions = relations.stream().map(EdgeDTO::isReverse).collect(Collectors.toList());
		// RDA-554 checking for issues with reverse direction
		log.debug("Indexing relation from [id={}] to [id={}] with edges[{}][{}]", from.getIdentifier(),
				to.getIdentifier(), relationTypes, directions);

		// build RelationshipDocument based on from, to and relations Edges
		RelationshipDocument doc = new RelationshipDocument();

		doc.setFromId(from.getIdentifier());
		doc.setFromClass(from.getObjectClass());
		doc.setFromType(from.getObjectType());
		doc.setFromTitle(from.getTitle());
		doc.setFromListTitle(from.getListTitle());
		doc.setFromGroup(from.getGroup());
		doc.setFromNotes(from.getNotes());
		doc.setFromUrl(from.getUrl());

		doc.setToIdentifier(to.getIdentifier());
		doc.setToIdentifierType(to.getIdentifierType());
		doc.setToClass(to.getObjectClass());
		doc.setToType(to.getObjectType());
		doc.setToTitle(to.getTitle());
		doc.setToListTitle(to.getListTitle());
		doc.setToGroup(to.getGroup());
		doc.setToNotes(to.getNotes());
		doc.setToUrl(to.getUrl());
		doc.setUpdatedAt(new Date());
		List<EdgeDocument> edges = new ArrayList<>();
		relations.forEach(relation -> {
			EdgeDocument edge = new EdgeDocument(relation.getType());

			// relationTypeText defaults to the raw value of the relation type
			String relationTypeText = relation.getType();

			// if we can resolve the relationType we'll get the proper text form for it
			RelationLookupEntry lookupEntry = RelationLookupService.resolve(relation.getType());
			if (lookupEntry != null) {
				if ("collection".equals(from.getObjectClass())) {
					relationTypeText = lookupEntry.getCollectionText();
				}
				else if ("activity".equals(from.getObjectClass())) {
					relationTypeText = lookupEntry.getActivityText();

				}
				else if ("service".equals(from.getObjectClass())) {
					relationTypeText = lookupEntry.getServiceText();
				}
				else if ("party".equals(from.getObjectClass())) {
					relationTypeText = lookupEntry.getPartyText();
				}
				// if the relationTypeText has not been set for this class then use the
				// default text
				if (relationTypeText.isEmpty()) {
					relationTypeText = lookupEntry.getDefaultText();
				}
			}

			edge.setRelationTypeText(relationTypeText);
			edge.setFromId(from.getIdentifier());
			edge.setToIdentifier(to.getIdentifier());
			edge.setRelationOrigin(relation.getOrigin());
			edge.setRelationInternal(RelationUtil.isInternal(from, to));
			edge.setRelationReverse(relation.isReverse());
			edge.setRelationDescription(relation.getDescription());
			edge.setRelationUrl(relation.getUrl());
			edge.setCreatedAt(relation.getCreatedAt());
			edge.setUpdatedAt(relation.getUpdatedAt());

			edges.add(edge);
		});
		doc.setRelations(edges);

		indexRelationshipDocument(doc);
	}

	public RelationshipDocument findExistingRelationshipDocument(String fromID, String toID) {
		Query query = new SimpleQuery("*:*");
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("from_id").is(fromID)));
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier").is(toID)));
		query.addProjectionOnField(new SimpleField("*"));
		query.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));

		return solrTemplate.queryForObject("relationships", query, RelationshipDocument.class).orElse(null);
	}

	public RelationshipDocument findExistingRelationshipDocument(String fromID) {
		Query query = new SimpleQuery("*:*");
		query.addFilterQuery(new SimpleFilterQuery(new Criteria("from_id").is(fromID)));
		query.addProjectionOnField(new SimpleField("*"));
		query.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));

		return solrTemplate.queryForObject("relationships", query, RelationshipDocument.class).orElse(null);
	}

	/**
	 * Index a {@link RelationshipDocument} into SOLR.
	 *
	 * Finds if the relationship document is available and attempts to update it if it is
	 * not. Supports partial updates to a degree
	 * @param doc the {@link RelationshipDocument} that will be persisted in SOLR
	 */
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
			// attempts to update the document with new additional relations

			RelationshipDocument existingDocument = existing.get();
			log.trace("Found existing RelationshipDocument[id={}]", existingDocument.getId());

			// add missing Relation
			doc.getRelations().forEach(relation -> {
				if (!existingDocument.getRelations().contains(relation)) {
					log.trace("Doesn't contain relation {} adding...", relation.getRelationType());
					existingDocument.getRelations().add(relation);
				}
			});

			// filter the relations (shouldn't have to do this)
			existingDocument.setRelations(existingDocument.getRelations().stream()
					.filter(relation -> relation.getFromId().equals(existingDocument.getFromId()))
					.filter(relation -> relation.getToIdentifier().equals(existingDocument.getToIdentifier()))
					.collect(Collectors.toList()));

			log.trace("Saving existing document");
			relationshipDocumentRepository.save(existingDocument);
		}
		else {
			// save the document because it does not exist
			log.trace("Does not found existing, saving new doc");
			relationshipDocumentRepository.save(doc);
		}
		solrTemplate.commit("relationships");
	}

	/**
	 * Index GrantsNetwork Implicit relationships for an activity
	 *
	 * Includes relations to child collections, child activities, parent activities and
	 * funders.
	 * @param from the activity {@link Vertex} for the registryObject
	 */
	@Transactional(readOnly = true)
	public void indexImplicitLinksForActivity(Vertex from) {
		log.debug("Indexing implicit links for activity Vertex[id={}]", from.getIdentifier());

		// the activity hasOutput all child collections
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|hasOutput>|hasPart>", "collection")) {
			stream.forEach(collection -> indexGrantsNetworkRelation(from, collection, "hasOutput"));
		}

		// the activity hasPart all child activities
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|hasPart>", "activity")) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "hasPart"));
		}

		// the activity isPartOf all parent activities
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isPartOf>", "activity")) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isPartOf"));
		}

		// the activity isFundedBy all funder
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isPartOf>|isOutputOf>|isFundedBy>", "party")) {
			stream.forEach(party -> indexGrantsNetworkRelation(from, party, "isFundedBy"));
		}
	}

	/**
	 * Index Implicit GrantsNetwork relationships for a party
	 *
	 * Includes relations to child collections and child activities
	 * @param from the party {@link Vertex} for the registryObject
	 */
	@Transactional(readOnly = true)
	public void indexImplicitLinksForParty(Vertex from) {
		log.debug("Indexing implicit links for party Vertex[id={}]", from.getIdentifier());

		// the party isFunderOf all child activities
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isFunderOf>|funds>|hasPart>", "activity")) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isFunderOf"));
		}

		// the party isFunderOf all child collections
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isFunderOf>|funds>|hasPart>|hasOutput>", "collection")) {
			stream.forEach(collection -> indexGrantsNetworkRelation(from, collection, "isFunderOf"));
		}
	}

	/**
	 * Index Implicit GrantsNetwork relationships for a collection
	 *
	 * Includes relations to child collections, parent collections, parent activities and
	 * funder
	 * @param from the party {@link Vertex} for the registryObject
	 */
	@Transactional(readOnly = true)
	public void indexImplicitLinksForCollection(Vertex from) {
		log.debug("Indexing implicit links for collection Vertex[id={}]", from.getIdentifier());

		// obtain all the duplicate registryObjectId so that they can be post-filtered out
		// since a record shouldn't implicitly relate to itself
		Collection<String> myDuplicateIDs = graphService.getSameAs(from.getIdentifier(), from.getIdentifierType())
				.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).map(Vertex::getIdentifier)
				.collect(Collectors.toList());

		// the collection hasPart all child collections
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|hasPart>", "collection")) {
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier()))
					.forEach(collection -> indexGrantsNetworkRelation(from, collection, "hasPart"));
		}

		// the collection isPartOf all parent collections
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isPartOf>", "collection")) {
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier()))
					.forEach(collection -> indexGrantsNetworkRelation(from, collection, "isPartOf"));
		}

		// the collection isOutputOf all parent activities
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isPartOf>|isOutputOf>", "activity")) {
			stream.forEach(activity -> indexGrantsNetworkRelation(from, activity, "isOutputOf"));
		}

		// the collection isFundedBy all funder
		try (Stream<Vertex> stream = vertexRepository.streamSpanningTreeFromId(from.getIdentifier(),
				"isSameAs|isPartOf>|isOutputOf>|isFundedBy>", "party")) {
			stream.forEach(party -> indexGrantsNetworkRelation(from, party, "isFundedBy"));
		}
	}

	/**
	 * Useful reusable function to index a GrantsNetwork relationship with automatic
	 * reverse edge
	 * @param from the source {@link Vertex}
	 * @param to the target {@link Vertex}
	 * @param relation the grantsNetwork relationType
	 */
	public void indexGrantsNetworkRelation(Vertex from, Vertex to, String relation) {

		// index the implicit edge but only between (to and from) registry Objects
		// RDA-624
		if (!to.getIdentifierType().equals(RIFCS_ID_IDENTIFIER_TYPE)) {
			log.warn("Indexes for Grants Network should have only To Registry Objects not {}:{}}",
					to.getIdentifierType(), to.getIdentifier());
			return;
		}
		if (!from.getIdentifierType().equals(RIFCS_ID_IDENTIFIER_TYPE)) {
			log.warn("Indexes for Grants Network should have only From Registry Objects not {}:{}}",
					from.getIdentifierType(), from.getIdentifier());
			return;
		}

		EdgeDTO edge = new EdgeDTO();
		edge.setOrigin(MyceliumIndexingService.ORIGIN_GRANTS_NETWORK);
		edge.setType(relation);
		edge.setReverse(!grantsNetworkIsTopDown(from.getObjectClass(), to.getObjectClass(), relation));
		indexRelation(from, to, new ArrayList<>(List.of(edge)));
		log.debug("Indexed GrantsNetwork Relation[from_id={}, to_id={}, relation={}]", from.getIdentifier(),
				to.getIdentifier(), edge.getType());

		// index the reversed edge
		EdgeDTO reversed = new EdgeDTO();
		reversed.setOrigin(MyceliumIndexingService.ORIGIN_GRANTS_NETWORK);
		String reversedRelationType = RelationLookupService.getReverse(relation, RELATION_RELATED_TO);
		reversed.setType(reversedRelationType);
		reversed.setReverse(! grantsNetworkIsTopDown(to.getObjectClass(), from.getObjectClass(), reversedRelationType));
		log.debug("Indexed (reversed) GrantsNetwork Relation[from_id={}, to_id={}, relation={}]", from.getIdentifier(),
				to.getIdentifier(), reversed.getType());

		indexRelation(to, from, new ArrayList<>(List.of(reversed)));
	}

	/**
	 * Determine if the relation is a top down relation in a Grants Network
	 *
	 * The flow party to activity to collection is considered top-down
	 * @param fromClass the String value of the origin vertex objectClass
	 * @param toClass the String value of the target vertex objectClass
	 * @param relationType the String value of the relationType
	 * @return true if the relation is top-down
	 */
	public boolean grantsNetworkIsTopDown(String fromClass, String toClass, String relationType) {
		if (fromClass.equals("party")) {
			return true;
		}
		else if (fromClass.equals("activity")) {
			if (toClass.equals("collection")) {
				return true;
			}
			else if (toClass.equals("party")) {
				return false;
			}
			else if (toClass.equals("activity")) {
				if (relationType.equals("isPartOf")) {
					return false;
				}
				else {
					return true;
				}
			}
		}
		else if (fromClass.equals("collection")) {
			if (toClass.equals("collection") && relationType.equals("hasPart")) {
				return true;
			}
			else {
				return false;
			}
		}

		return false;
	}

	public void deleteRelationship(String registryObjectId) {
		log.debug("Deleting Relationship Index RegistryObject[id={}]", registryObjectId);
		String query = String.format("from_id:%s to_identifier:%s", registryObjectId, registryObjectId);
		Query dataQuery = new SimpleQuery(new SimpleStringCriteria(query));
		dataQuery.addSort(Sort.by("id"));
		Cursor<RelationshipDocument> cursor = solrTemplate.queryForCursor("relationships", dataQuery,
				RelationshipDocument.class);
		while (cursor.hasNext()) {
			RelationshipDocument doc = cursor.next();
			String docID = doc.getId();

			// delete all edges that has this docID as _root_
			String edgeQuery = String.format("_root_:%s", docID);
			Query edgeDeleteQuery = new SimpleQuery(new SimpleStringCriteria(edgeQuery));
			solrTemplate.delete("relationships", edgeDeleteQuery);

			// and then delete the document itself
			relationshipDocumentRepository.delete(doc);
		}
	}

	/**
	 * Update the title of a registryObject in the relationships index
	 *
	 * todo update related_$class_title in portal collection
	 * @param registryObjectId the registryObjectId identifier
	 * @param updatedTitle the title to update it to
	 */
	public void updateTitle(String registryObjectId, String updatedTitle) {

		// update from_title to the updatedTitle of all RelationshipDocument that has the
		// from_id=registryObjectId
		Query fromIdQuery = new SimpleQuery("*:*");
		fromIdQuery.addProjectionOnField(new SimpleField("*"));
		fromIdQuery.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));
		fromIdQuery.addSort(Sort.by("id"));
		fromIdQuery.addFilterQuery(new SimpleFilterQuery(new Criteria("from_id").is(registryObjectId)));
		Cursor<RelationshipDocument> fromIdCursor = solrTemplate.queryForCursor("relationships", fromIdQuery,
				RelationshipDocument.class);
		int fromIdCounter = 0;
		while (fromIdCursor.hasNext()) {
			RelationshipDocument relatedFromThisId = fromIdCursor.next();
			relatedFromThisId.setFromTitle(updatedTitle);
			relationshipDocumentRepository.save(relatedFromThisId);
			log.trace("RelationshipDocument[id={}] updated [from_title={}]", relatedFromThisId.getId(), updatedTitle);
			fromIdCounter++;
		}
		log.debug("(from_title) Updated {} RelationshipDocument[from_id={}]", fromIdCounter, registryObjectId);

		// update to_title to the updatedTitle of all RelationshipDocument that has the
		// to_identifier=registryObjectId
		Query toIdQuery = new SimpleQuery("*:*");
		toIdQuery.addProjectionOnField(new SimpleField("*"));
		toIdQuery.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));
		toIdQuery.addSort(Sort.by("id"));
		toIdQuery.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier").is(registryObjectId)));
		Cursor<RelationshipDocument> toIdCursor = solrTemplate.queryForCursor("relationships", toIdQuery,
				RelationshipDocument.class);
		int toIdCounter = 0;
		while (toIdCursor.hasNext()) {
			RelationshipDocument relatedFromThisId = toIdCursor.next();
			relatedFromThisId.setToTitle(updatedTitle);
			relationshipDocumentRepository.save(relatedFromThisId);
			log.trace("RelationshipDocument[id={}] updated [to_title={}]", relatedFromThisId.getId(), updatedTitle);
			toIdCounter++;
		}
		log.debug("(to_title) Updated {} RelationshipDocument[to_identifier={}]", toIdCounter, registryObjectId);

	}

	public void deleteRelationshipDocument(RelationshipDocument doc) {
		relationshipDocumentRepository.delete(doc);
	}

}
