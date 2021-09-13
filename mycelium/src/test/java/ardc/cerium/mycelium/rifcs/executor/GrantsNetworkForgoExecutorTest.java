package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class GrantsNetworkForgoExecutorTest {

	@MockBean
	MyceliumService myceliumService;

    @Test
    void detect_modification() {
        // before: c1 isPartOf c2
        // after: c1 hasAssociationWith c2

        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");
        Vertex c2 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c2.setObjectClass("collection");

        Relationship beforeRelation = new Relationship();
        beforeRelation.setFrom(c1);
        beforeRelation.setTo(c2);
        EdgeDTO c1IsPartOfC2 = new EdgeDTO();
        c1IsPartOfC2.setType("isPartOf");
        beforeRelation.setRelations(List.of(c1IsPartOfC2));

        Relationship afterRelation = new Relationship();
        afterRelation.setFrom(c1);
        afterRelation.setTo(c2);
        EdgeDTO c1HasAssocC2 = new EdgeDTO();
        c1HasAssocC2.setType("hasAssociationWith");
        afterRelation.setRelations(List.of(c1HasAssocC2));

        RecordState before = new RecordState();
        before.setOutbounds(Collections.singleton(beforeRelation));

        RecordState after = new RecordState();
        after.setOutbounds(Collections.singleton(afterRelation));

        assertThat(GrantsNetworkForgoExecutor.detect(before, after, myceliumService)).isTrue();
    }

    @Test
    void detect_deleted() {
        // before: c1 isPartOf c2
        // after: c1 is deleted

        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");
        Vertex c2 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c2.setObjectClass("collection");

        Relationship beforeRelation = new Relationship();
        beforeRelation.setFrom(c1);
        beforeRelation.setTo(c2);
        EdgeDTO c1IsPartOfC2 = new EdgeDTO();
        c1IsPartOfC2.setType("isPartOf");
        beforeRelation.setRelations(List.of(c1IsPartOfC2));

        RecordState before = new RecordState();
        before.setOutbounds(Collections.singleton(beforeRelation));

        assertThat(GrantsNetworkForgoExecutor.detect(before, null, myceliumService)).isTrue();
    }
}