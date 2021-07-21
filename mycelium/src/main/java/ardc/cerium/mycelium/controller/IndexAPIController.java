package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.solr.EdgeDocument;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class IndexAPIController {

    @Autowired
    RelationshipDocumentRepository repository;

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
            } else {
                // does not resolve to registryObject it's a relatedInfo relation
                log.debug("Does not resolve to any relatedObject. Index as RelatedInfo");
                indexRelation(from, to, relationship.getRelations());
            }
        });

        // todo index GrantsNetwork (reverse + duplicates) relationships

        return ResponseEntity.ok("Done!");
	}

    private void indexRelation(Vertex from, Vertex to, List<EdgeDTO> relations) {
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


    private void indexRelationshipDocument(RelationshipDocument doc) {

        // search for existing document in an attempt to update it
        Query query = new SimpleQuery("*:*");
        query.addFilterQuery(new SimpleFilterQuery(new Criteria("from_id").is(doc.getFromId())));
        query.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier").is(doc.getToIdentifier())));
        query.addFilterQuery(new SimpleFilterQuery(new Criteria("to_identifier_type").is(doc.getToIdentifierType())));
        query.addProjectionOnField(new SimpleField("*"));
        query.addProjectionOnField(new SimpleField("[child parentFilter=type:relationship]"));
        Optional<RelationshipDocument> existing = solrTemplate.queryForObject("relationships", query, RelationshipDocument.class);

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
        } else {
            log.debug("Does not found existing, saving new doc");
            repository.save(doc);
        }
    }

}
