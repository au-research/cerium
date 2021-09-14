package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.rifcs.RecordState;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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

        if (to == null) {
            return new ArrayList<>(from.getOutbounds());
        }

        return from.getOutbounds().stream().filter(relationship -> !to.getOutbounds().contains(relationship))
                .collect(Collectors.toList());
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
        log.debug("Checking Grants Network [from={}, to={}, types={}]", fromClass, toClass, relationTypes);

        // only check RegistryObject -> RegistryObject relations
        if (fromClass == null || toClass == null) {
            return false;
        }

        // collection isPartOf collection
        // collection hasPart collection
        if (fromClass.equals("collection") && toClass.equals("collection")) {
            return relationTypes.contains("isPartOf") || relationTypes.contains("hasPart");
        }

        // collection isProducedBy activity
        // collection isOutputOf activity
        if (fromClass.equals("collection") && toClass.equals("activity")) {
            return relationTypes.contains("isProducedBy") || relationTypes.contains("isOutputOf");
        }

        // collection isFundedBy party
        if (fromClass.equals("collection") && toClass.equals("party")) {
            return relationTypes.contains("isFundedBy");
        }

        // activity produces collection
        // activity hasOutput collection
        if (fromClass.equals("activity") && toClass.equals("collection")) {
            return relationTypes.contains("produces") || relationTypes.contains("hasOutput");
        }

        // activity isPartOf activity
        // activity hasPart activity
        if (fromClass.equals("activity") && toClass.equals("activity")) {
            return relationTypes.contains("isPartOf") || relationTypes.contains("hasPart");
        }

        // activity isFundedBy party
        if (fromClass.equals("activity") && toClass.equals("party")) {
            return relationTypes.contains("isFundedBy");
        }

        // party isFunderOf collection
        // party isFunderOf activity
        if (fromClass.equals("party") && (toClass.equals("collection") || toClass.equals("activity"))) {
            return relationTypes.contains("isFunderOf");
        }

        return false;
    }


}
