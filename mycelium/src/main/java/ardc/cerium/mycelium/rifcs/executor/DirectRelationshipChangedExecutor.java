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
            if (after == null && before != null && !before.getOutbounds().isEmpty()) {
                return true;
            }
            // the record is newly added
            if (before == null && after != null && !after.getOutbounds().isEmpty()) {
                return true;
            }

            List<Relationship> differences = RelationUtil.getRelationshipsDifferences(after, before);
            return !differences.isEmpty();

        }

        @Override
        public void handle() {
            String originRegistryObjectId = sideEffect.getRegistryObjectId();
            // added or removed
            String relatedRegistryObjectId = sideEffect.getRelatedObjectId();
            String action = sideEffect.getAction();
            String recordTitle = sideEffect.getTitle();
            String recordClass = sideEffect.getRecordClass();
            String recordType = sideEffect.getRecordType();
            String relationshipType = sideEffect.getRelationshipType();

            Vertex vertex = getMyceliumService().getVertexFromRegistryObjectId(originRegistryObjectId);

            // Add non existing titles to portal Index
            if(action.equals("add")){
                log.debug("DirectRelationshipChangedExecutor add to{} ;title {} ", originRegistryObjectId, recordTitle);
                getMyceliumService().getMyceliumIndexingService().addRelatedTitleToPortalIndex(
                        originRegistryObjectId, recordClass, recordType, recordTitle, relationshipType);
                // if the origin Node is removed the RDA portal index was also removed
                if(vertex != null){
                    log.debug("DirectRelationshipChangedExecutor add reverse to {} ;title {} ", relatedRegistryObjectId, vertex.getTitle());
                    getMyceliumService().getMyceliumIndexingService().addRelatedTitleToPortalIndex(
                            relatedRegistryObjectId, vertex.getObjectClass(), vertex.getObjectType(), vertex.getTitle(), relationshipType);
                }
            }
            // remove existing title from portal index
            else{
                log.debug("DirectRelationshipChangedExecutor remove from {} ;title {} ", originRegistryObjectId, recordTitle);
                getMyceliumService().getMyceliumIndexingService().deleteRelatedTitleFromPortalIndex(
                        originRegistryObjectId, recordClass, recordType, recordTitle, relationshipType);
                // if the origin Node is removed the RDA portal index was also removed
                if(vertex != null){
                    log.debug("DirectRelationshipChangedExecutor remove reverse from {} ;title {} ", relatedRegistryObjectId, vertex.getTitle());
                    getMyceliumService().getMyceliumIndexingService().deleteRelatedTitleFromPortalIndex(
                            relatedRegistryObjectId, vertex.getObjectClass(), vertex.getObjectType(), vertex.getTitle(), relationshipType);
                }
            }
        }

}
