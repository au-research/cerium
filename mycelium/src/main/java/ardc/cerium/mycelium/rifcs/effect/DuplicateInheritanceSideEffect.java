package ardc.cerium.mycelium.rifcs.effect;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.RelationLookupService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DuplicateInheritanceSideEffect extends SideEffect{

	/**
	* registryObjectId of the origin Vertex
	*/
    private final String registryObjectId;

    private final GraphService graphService;

    private final MyceliumIndexingService indexingService;

    public DuplicateInheritanceSideEffect(String registryObjectId, GraphService graphService, MyceliumIndexingService indexingService) {
        this.registryObjectId = registryObjectId;
        this.graphService = graphService;
        this.indexingService = indexingService;
    }

    @Override
    public void handle() {
        Vertex origin = graphService.getVertexByIdentifier(registryObjectId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        if (origin == null) {
            // todo log to request -> unknown registryObjectId
            return;
        }

        // find all duplicates of origin
       	Collection<Vertex> myDuplicates = graphService.getSameAsRegistryObject(origin).stream()
				.filter(v -> !v.getIdentifier().equals(registryObjectId)).collect(Collectors.toList());
		if (myDuplicates.size() == 0) {
		    // todo log to request -> no duplicates found
			return;
		}

        // find all directly related vertices
        Collection<Relationship> directOutbounds = graphService.getDirectOutboundRelationships(origin.getIdentifier(),
				origin.getIdentifierType());
		if (directOutbounds.size() == 0) {
		    // todo log to request -> no outbounds found
		    return;
		}

        // ensure (dupe)-[r]->(related) and (dupe)<-[rr]-(related) exists in Neo4j and SOLR
        directOutbounds.forEach(outbound -> {
            Vertex to = outbound.getTo();

            myDuplicates.forEach(dupe -> {
                indexingService.indexRelation(dupe, to, outbound.getRelations());

                List<EdgeDTO> reversedEdges = outbound.getRelations().stream()
						.map(edgeDTO -> {
						    EdgeDTO reversedEdge = new EdgeDTO();
						    reversedEdge.setReverse(true);
						    reversedEdge.setOrigin(edgeDTO.getOrigin());
						    reversedEdge.setType(RelationLookupService.getReverse(edgeDTO.getType()));
						    reversedEdge.setInternal(edgeDTO.isInternal());
						    reversedEdge.setPublic(edgeDTO.isPublic());
						    return reversedEdge;
                        }).collect(Collectors.toList());
                indexingService.indexRelation(to, dupe, reversedEdges);
            });
        });
    }
}
