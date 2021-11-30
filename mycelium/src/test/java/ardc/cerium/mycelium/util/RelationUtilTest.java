package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    }

    @Test
	void isGrantsNetwork_not() {
        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");
        Vertex c2 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c2.setObjectClass("collection");

        Relationship relationship = new Relationship();
        relationship.setFrom(c1);
        relationship.setTo(c2);

        EdgeDTO c1HasAssocC2 = new EdgeDTO();
        c1HasAssocC2.setType("hasAssociationWith");
        relationship.setRelations(List.of(c1HasAssocC2));

        // c1 hasAssociationWith c2 is not a grants network relation
        assertThat(RelationUtil.isGrantsNetwork(relationship)).isFalse();
	}

    /**
     * RDA-553
     *
     */
	@Test
    void isInternal_tests(){
        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);

        Vertex c2 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        // none has a datasource id
        assertThat(RelationUtil.isInternal(c1,c2)).isTrue();
        c2.setDataSourceId("1");
        // one has a datasource id
        assertThat(RelationUtil.isInternal(c1,c2)).isTrue();
        c1.setDataSourceId("1");
        // both have a datasource id and they are the same
        assertThat(RelationUtil.isInternal(c1,c2)).isTrue();
        c1.setDataSourceId("2");
        // one has a different datasource id
        assertThat(RelationUtil.isInternal(c1,c2)).isFalse();
    }
}