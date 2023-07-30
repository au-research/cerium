package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DirectRelationshipChangedSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

    @Slf4j
    public class DirectRelationshipChangedExecutor extends Executor {

        private final DirectRelationshipChangedSideEffect sideEffect;

        public DirectRelationshipChangedExecutor(DirectRelationshipChangedSideEffect sideEffect, MyceliumService myceliumService) {
            this.sideEffect = sideEffect;
            this.setMyceliumService(myceliumService);
        }

        /**
         * Detect if {@link DirectRelationshipChangedSideEffect} is applicable
         * @param before the before {@link RecordState}
         * @param after the after {@link RecordState}
         * @param myceliumService the {@link MyceliumService} for additional business logic
         * @return boolean
         */
        public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
            // the record is deleted and the Before state relationships changed

            log.debug("DirectRelationshipChangedExecutor detecting");
            // It's a DRAFT
            if (after == null || after.getStatus().equals(Vertex.Status.DRAFT.name())) {
                return false;
            }
            // if before had something but after is empty
            if (before != null && !before.getOutbounds().isEmpty() && after.getOutbounds().isEmpty()) {
                return true;
            }
            // the record is newly and it has relatedObjects
            if (before == null && !after.getOutbounds().isEmpty()) {
                return true;
            }

            List<Relationship> differences = RelationUtil.getRelationshipsDifferences(after, before);
            log.debug("DirectRelationshipChangedExecutor detecting result:{}", !differences.isEmpty());
            return !differences.isEmpty();

        }

        @Override
        public void handle() {
            String originRegistryObjectId = sideEffect.getRegistryObjectId();
            // added or removed
            String relatedRegistryObjectId = sideEffect.getAffectedRegistryObjectId();
            String action = sideEffect.getAction();
            String recordTitle = sideEffect.getTitle();
            String recordClass = sideEffect.getRecordClass();
            String recordType = sideEffect.getRecordType();
            String relationshipType = sideEffect.getRelationshipType();

            // Add non existing titles to portal Index
            if(action.equals("add")){
                log.debug("DirectRelationshipChangedExecutor add to{} title {} ", relatedRegistryObjectId, recordTitle);
                getMyceliumService().getMyceliumIndexingService().addRelatedTitleToPortalIndex(
                        relatedRegistryObjectId, recordClass, recordType, recordTitle, relationshipType);
            }
            // remove existing title from portal index
            if(action.equals("delete")){
                log.debug("DirectRelationshipChangedExecutor remove from {} ;title {} ", relatedRegistryObjectId, recordTitle);
                getMyceliumService().getMyceliumIndexingService().deleteRelatedTitleFromPortalIndex(
                        relatedRegistryObjectId, recordClass, recordType, recordTitle, relationshipType);
            }
        }

}
