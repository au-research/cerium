package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.RelationLookupService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RelationUtil {

    /**
     * Get a List of Relationships that are in from but not in to
     *
     * @param from the {@link RecordState} for from
     * @param to the {@link RecordState} for to
     * @return the differences comparing first to second
     */
    public static List<Relationship> getRelationshipsDifferences(RecordState from, RecordState to) {

        if (from == null && to == null) {
            return new ArrayList<>();
        }
        else if (to == null) {
            return new ArrayList<>(from.getOutbounds());
        }else if(from == null){
            return new ArrayList<>(to.getOutbounds());
        }else {
            List<Relationship> lostRelationships = from.getOutbounds().stream().filter(relationship -> !to.getOutbounds().contains(relationship))
                    .collect(Collectors.toList());
            List<Relationship> gainedRelationships = to.getOutbounds().stream().filter(relationship -> !from.getOutbounds().contains(relationship))
                    .collect(Collectors.toList());
            lostRelationships.addAll(gainedRelationships);
            return lostRelationships;
        }
    }

    /**
     * Check if a Relationship is part of a grants network
     * @param relationship the relationship to check
     * @return true if the relationship is part of a grants network
     */
    public static boolean isGrantsNetwork(Relationship relationship) {

        Set<String> relationTypes = relationship.getRelations().stream().map(EdgeDTO::getType)
                .collect(Collectors.toSet());

        String fromClass = relationship.getFrom().getObjectClass();
        String toClass = relationship.getTo().getObjectClass();
        log.trace("Checking Grants Network [from={}, to={}, types={}]", fromClass, toClass, relationTypes);

        // only check RegistryObject -> RegistryObject relations
        if (fromClass == null || toClass == null) {
            return false;
        }

        return relationTypes.stream().anyMatch(relationType -> isGrantsNetwork(fromClass, toClass, relationType));
    }

	/**
     * Helper function to determine if a single relation between 2 RegistryObject is considered a GrantsNetwork
     *
	 * @param fromClass the string of the 'from' class
	 * @param toClass the string of the 'to' class
	 * @param relationType the relationType string
	 * @return true if the relation is considered as part of a GrantsNetwork
	 */
    public static boolean isGrantsNetwork(String fromClass, String toClass, String relationType) {
        if (fromClass.equals("collection") && toClass.equals("collection")) {
            return relationType.equals("isPartOf") || relationType.equals("hasPart");
        }

        // collection isProducedBy activity
        // collection isOutputOf activity
        if (fromClass.equals("collection") && toClass.equals("activity")) {
            return relationType.equals("isProducedBy") || relationType.equals("isOutputOf");
        }

        // collection isFundedBy party
        if (fromClass.equals("collection") && toClass.equals("party")) {
            return relationType.equals("isFundedBy");
        }

        // activity produces collection
        // activity hasOutput collection
        if (fromClass.equals("activity") && toClass.equals("collection")) {
            return relationType.equals("produces") || relationType.equals("hasOutput");
        }

        // activity isPartOf activity
        // activity hasPart activity
        if (fromClass.equals("activity") && toClass.equals("activity")) {
            return relationType.equals("isPartOf") || relationType.equals("hasPart");
        }

        // activity isFundedBy party
        if (fromClass.equals("activity") && toClass.equals("party")) {
            return relationType.equals("isFundedBy");
        }

        // party isFunderOf collection
        // party isFunderOf activity
        if (fromClass.equals("party") && (toClass.equals("collection") || toClass.equals("activity"))) {
            return relationType.equals("isFunderOf");
        }

        return false;
    }

    public static boolean isDCIRelation(Relationship relationship) {
        Set<String> relationTypes = relationship.getRelations().stream().map(EdgeDTO::getType)
                .collect(Collectors.toSet());

        String fromClass = relationship.getFrom().getObjectClass();
        String toClass = relationship.getTo().getObjectClass();
        log.trace("Checking DCI Relation [from={}, to={}, types={}]", fromClass, toClass, relationTypes);

        return relationTypes.stream().anyMatch(relationType -> isDCIRelation(fromClass, toClass, relationType));
    }

    public static boolean isDCIRelation(String fromClass, String toClass, String relationType) {

        // author from party perspective, does not need to check toClass due to accepting relatedInfo
        if (fromClass.equals("party")) {
            List<String> validAuthorRelationTypes = Arrays.asList("hasPrincipalInvestigator", "hasAuthor", "hasCoInvestigator", "isOwnerOf", "isCollectorOf");
            return validAuthorRelationTypes.contains(relationType);
        }

        // author from collection perspective
        if (fromClass.equals("collection") && toClass != null && toClass.equals("party")) {
            List<String> validAuthorRelationTypes = Arrays.asList("IsPrincipalInvestigatorOf", "author", "coInvestigator", "isOwnedBy", "hasCollector");
            return validAuthorRelationTypes.contains(relationType);
        }

        // parent record reference
        if (fromClass.equals("collection")) {
            return relationType.equals("isPartOf");
        }

        // funding organisation from party
        if (fromClass.equals("party")) {
            return relationType.equals("isFunderOf");
        }

        // funding organisation from collection
        if (fromClass.equals("collection") && toClass != null && toClass.equals("party")) {
            return relationType.equals("isFundedBy");
        }

        return false;
    }

    public static EdgeDTO getEdgeDTO(Edge edge) {
        EdgeDTO dto = new EdgeDTO();
        dto.setType(edge.getType());
        dto.setInternal(edge.isInternal());
        dto.setPublic(edge.isPublic());
        dto.setOrigin(edge.getOrigin());
        dto.setReverse(edge.isReverse());
        dto.setDescription(edge.getDescription());
        dto.setUrl(edge.getUrl());
        return dto;
    }

    public static EdgeDTO getReversed(EdgeDTO edgeDTO) {
        EdgeDTO reversedEdge = new EdgeDTO();
        reversedEdge.setType(RelationLookupService.getReverse(edgeDTO.getType()));
        // RDA-554
        reversedEdge.setReverse(!edgeDTO.isReverse());
        reversedEdge.setOrigin(edgeDTO.getOrigin());
        reversedEdge.setInternal(edgeDTO.isInternal());
        reversedEdge.setPublic(edgeDTO.isPublic());
        reversedEdge.setDescription(edgeDTO.getDescription());
        reversedEdge.setUrl(edgeDTO.getUrl());
        return reversedEdge;
    }

    public static EdgeDTO getReversed(EdgeDTO edgeDTO, String defaultRelationType) {
        EdgeDTO reversedEdge = getReversed(edgeDTO);
        reversedEdge.setType(RelationLookupService.getReverse(edgeDTO.getType(), defaultRelationType));
        return reversedEdge;
    }

    public static boolean isInternal(Vertex from, Vertex to){
        // RDA-553
        // Internal edge is between two registry Objects that are part of the same datasource
        // if the datasource of the two registry Object are set and are different
        // the relation is not internal
        return from.getDataSourceId() == null || to.getDataSourceId() == null ||
                from.getDataSourceId().isEmpty() || to.getDataSourceId().isEmpty() ||
                from.getDataSourceId().equals(to.getDataSourceId());
    }

    public static boolean isScholixRelation(Relationship relationship) {

        Set<String> relationTypes = relationship.getRelations().stream().map(EdgeDTO::getType)
                .collect(Collectors.toSet());

        String fromClass = relationship.getFrom().getObjectClass();
        String toClass = relationship.getTo().getObjectClass();
        log.trace("Checking Scholix Relation [from={}, to={}, types={}]", fromClass, toClass, relationTypes);

        return relationTypes.stream().anyMatch(relationType -> isScholixRelation(fromClass, toClass, relationType));

    }

    public static boolean isScholixRelation(String fromClass, String toClass, String relationType) {

        // author
        if (fromClass.equals("party")) {
            List<String> validAuthorRelationTypes = Arrays.asList("IsPrincipalInvestigatorOf", "author", "coInvestigator", "isOwnerOf", "hasCollector");
            return validAuthorRelationTypes.contains(relationType);
        }

        return false;
    }
}
