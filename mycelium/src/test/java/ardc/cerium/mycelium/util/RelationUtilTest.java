package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RelationUtilTest {

    @Test
    void isGrantsNetwork() {
        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");
        Vertex c2 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c2.setObjectClass("collection");

        // c1 isPartOf c2 is a grants Network relation
        Relationship relationship = new Relationship();
        relationship.setFrom(c1);
        relationship.setTo(c2);
        EdgeDTO c1IsPartOfC2 = new EdgeDTO();
        c1IsPartOfC2.setType("isPartOf");
        relationship.setRelations(List.of(c1IsPartOfC2));

        assertThat(RelationUtil.isGrantsNetwork(relationship)).isTrue();

        // c1 hasAssociationWith c2 is not a grants network relation
        EdgeDTO c1HasAssociationWithC2 = new EdgeDTO();
        c1IsPartOfC2.setType("hasAssociationWith");
        relationship.setRelations(List.of(c1HasAssociationWithC2));
        assertThat(RelationUtil.isGrantsNetwork(relationship)).isFalse();
    }

}