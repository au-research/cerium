package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.solr.EdgeDocument;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class IndexAPIController {


	@Autowired
	GraphService graphService;

	@Autowired
	RelationshipDocumentRepository repository;

	@Autowired
	VertexRepository vertexRepository;

	@Autowired
	SolrTemplate solrTemplate;
	@Autowired
	MyceliumService myceliumService;

	@PostMapping("/index-record")
	public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {
		// todo refactor logic into MyceliumService

		log.debug("Starting to index {}", registryObjectId);

		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		// todo handle from is null

		// index direct (reverse + duplicates) relationships
		Collection<Relationship> relationships = myceliumService.getAllDirectFrom(from, PageRequest.of(0, 1000));
		log.info("Found {} total related vertices", relationships.size());
		relationships.forEach(relationship -> {
			Vertex to = relationship.getTo();
			log.debug("Related Entity {}", to);

			Collection<Vertex> toRelatedObjects = myceliumService.getDuplicateRegistryObject(to);
			if (toRelatedObjects.size() > 0) {
				log.debug("Resolved {} relatedObjects", toRelatedObjects.size());
				// resolves to 1 or more registryObjects
				toRelatedObjects.forEach(toRelatedObject -> {
					indexRelation(from, toRelatedObject, relationship.getRelations());
				});
			}
			else {
				// does not resolve to registryObject it's a relatedInfo relation
				log.debug("Does not resolve to any relatedObject. Index as RelatedInfo");
				indexRelation(from, to, relationship.getRelations());
			}
		});

		if (from.getObjectClass().equals("collection")) {
			indexCollectionGrantsNetwork(from);
		}

		if (from.getObjectClass().equals("party")) {
			indexPartyGrantsNetwork(from);
		}

		if (from.getObjectClass().equals("activity")) {
			indexActivityGrantsNetwork(from);
		}

		return ResponseEntity.ok("Done!");
	}

	public void indexActivityGrantsNetwork(Vertex from) {
		// child collections
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildCollections(from.getIdentifier())) {
			stream.forEach(collection -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("hasOutput");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} hasOutput {}", from.getIdentifier(), collection.getIdentifier());
				indexRelation(from, collection, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

		// child activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildActivities(from.getIdentifier())) {
			stream.forEach(activity -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("hasPart");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} hasPart {}", from.getIdentifier(), activity.getIdentifier());
				indexRelation(from, activity, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

		// parent activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentActivities(from.getIdentifier())) {
			stream.forEach(activity -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isPartOf");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isPartOf {}", from.getIdentifier(), activity.getIdentifier());
				indexRelation(from, activity, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

		// funder
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentParties(from.getIdentifier())) {
			stream.forEach(party -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isFundedBy");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isFundedBy {}", from.getIdentifier(), party.getIdentifier());
				indexRelation(from, party, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}
	}

	@Transactional(readOnly = true)
	public void indexPartyGrantsNetwork(Vertex from) {

		// child activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildActivities(from.getIdentifier())) {
			stream.forEach(activity -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isFunderOf");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isFunderOf {}", from.getIdentifier(), activity.getIdentifier());
				indexRelation(from, activity, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

		// child collections
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkChildCollections(from.getIdentifier())) {
			stream.forEach(collection -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isFunderOf");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isFunderOf {}", from.getIdentifier(), collection.getIdentifier());
				indexRelation(from, collection, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}
	}

	@Transactional(readOnly = true)
	public void indexCollectionGrantsNetwork(Vertex from) {

		Collection<String> myDuplicateIDs = myceliumService.getDuplicateRegistryObject(from).stream().map(Vertex::getIdentifier).collect(Collectors.toList());

		// child collection
		log.info("Start Child Collections");
		try (Stream<Vertex> stream = vertexRepository.streamChildCollections(from.getIdentifier())) {
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier())).forEach(collection -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("hasPart");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.debug("GrantsNetwork: {} hasPart {}", from.getIdentifier(), collection.getIdentifier());
				indexRelation(from, collection, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}
		log.info("Finished Child Collections");

		// parent collection
		log.info("Indexing Parent Collections");
		try (Stream<Vertex> stream = vertexRepository.streamParentCollections(from.getIdentifier())){
			stream.filter(collection -> !myDuplicateIDs.contains(collection.getIdentifier())).forEach(collection -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isPartOf");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.debug("GrantsNetwork: {} isPartOf {}", from.getIdentifier(), collection.getIdentifier());
				indexRelation(from, collection, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}
		log.info("Finished Parent Collections");

		// parent activities
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentActivities(from.getIdentifier())) {
			stream.forEach(activity -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isOutputOf");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isOutputOf {}", from.getIdentifier(), activity.getIdentifier());
				indexRelation(from, activity, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

		// funder
		try (Stream<Vertex> stream = vertexRepository.streamAllGrantsNetworkParentParties(from.getIdentifier())) {
			stream.forEach(party -> {
				EdgeDTO implicitIsPartOf = new EdgeDTO();
				implicitIsPartOf.setType("isFundedBy");
				implicitIsPartOf.setOrigin("GrantsNetwork");
				log.info("GrantsNetwork: {} isFundedBy {}", from.getIdentifier(), party.getIdentifier());
				indexRelation(from, party, new ArrayList<>(List.of(implicitIsPartOf)));
			});
		}

	}

	private void indexRelation(Vertex from, Vertex to, List<EdgeDTO> relations) {
		log.info("Indexing relation from {} to {}", from.getIdentifier(), to.getIdentifier());
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

	private void indexRelationshipDocument(RelationshipDocument doc) {

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
			repository.save(existingDocument);
		}
		else {
			log.debug("Does not found existing, saving new doc");
			repository.save(doc);
		}
	}

}
