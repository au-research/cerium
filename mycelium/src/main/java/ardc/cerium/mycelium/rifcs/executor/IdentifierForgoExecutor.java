package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.IdentifierForgoSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class IdentifierForgoExecutor extends Executor {

    private final IdentifierForgoSideEffect sideEffect;

    private final MyceliumIndexingService myceliumIndexingService;

    public IdentifierForgoExecutor( IdentifierForgoSideEffect sideEffect, MyceliumService myceliumService) {
        this.sideEffect = sideEffect;
        this.setMyceliumService(myceliumService);
        this.myceliumIndexingService = myceliumService.getIndexingService();
    }


    /**
     * Detect if {@link IdentifierForgoSideEffect} is applicable
     *
     * This {@link SideEffect} is applicable when the before {@link RecordState} has
     * Identifiers that not present in the after state
     * @param before the before {@link RecordState}
     * @param after the after {@link RecordState}
     * @return boolean
     */
    public static boolean detect(RecordState before, RecordState after) {
        // if it had no previous Identifiers then nothing to lose
        if(before == null || before.getIdentifiers().isEmpty()){
            return false;
        }
        // record is deleted
        if(after == null || after.getIdentifiers().isEmpty()){
            return true;
        }
        // if all that was is still contained in the after then false
        // yes it can be simplified but it's much easier to read
        if(after.getIdentifiers().containsAll(before.getIdentifiers())){
            log.debug("Got All Identifiers b:{}, a:{}", before.getIdentifiers(), after.getIdentifiers());
            return false;
        }
        return true;
    }

    /**
     *
     * to process the IdentifierForgoSideEffect
     * remove Identifier Relationships from the Relationships Index from solr
     * remove the current record's title from portal Index when applicable
     *
     */
    @Override
    public void handle() {
        // remove from indexing service relationships to the given record
        // if it was caused by the identifier that the record has "lost"

        String identifier = sideEffect.getDeletedIdentifier();
        String registryObjectId = sideEffect.getRegistryObjectId();
        log.debug("Handling SideEffect forgoIdentifier: {}", identifier);


        // find all the RelationshipDocument that to_identifier=<realisedIdentifier>
        Cursor<RelationshipDocument> cursor = this.myceliumIndexingService.cursorFor(new Criteria("to_identifier").is(identifier));
        while (cursor.hasNext()) {
            RelationshipDocument doc = cursor.next();

            String fromId = doc.getFromId();
            log.debug("deleting relationship doc from_id {}", fromId);
            // remove RelatedInfo relationships to the Identifier
            this.myceliumIndexingService.deleteRelationshipDocument(doc);

            // reprocess the relationship source vertex after removing the relationship
            Vertex vertex = getMyceliumService().getVertexFromRegistryObjectId(fromId);
            this.myceliumIndexingService.indexDirectRelationships(vertex);
        }
        // TODO
        // find all registry Objects from the Vertex that are related to this Identifier
        // remove the record's from the portal Index

        // the related_<class>_title is the title of the record we need to remove from the portal Index
        // related_collection_title
        // related_party_multi
        // this.myceliumIndexingService.deleteRelatedTitleFromPortalIndex(from_id, to_title, to_class, to_type);
    }
}
