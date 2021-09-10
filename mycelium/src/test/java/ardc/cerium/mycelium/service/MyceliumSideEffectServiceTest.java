package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({ MyceliumSideEffectService.class })
class MyceliumSideEffectServiceTest {

	@Autowired
	MyceliumSideEffectService myceliumSideEffectService;

	@MockBean
	GraphService graphService;

	@MockBean
	MyceliumIndexingService myceliumIndexingService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@MockBean
	RedissonClient redissonClient;

	@Test
	void detectChanges_TitleChangeSideEffect_NotChanged() {
		RecordState before = new RecordState();
		before.setTitle("Old");

		RecordState after = new RecordState();
		after.setTitle("Old");

		assertThat(myceliumSideEffectService.detectTitleChange(before, after)).isFalse();

		List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
		assertThat(sideEffects.stream().filter(sideEffect -> sideEffect instanceof TitleChangeSideEffect).count())
				.isEqualTo(0);
	}

	@Test
	void detectChanges_TitleChangeSideEffect_Changed() {
		RecordState before = new RecordState();
		before.setTitle("Old");

		RecordState after = new RecordState();
		after.setTitle("New");

		assertThat(myceliumSideEffectService.detectTitleChange(before, after)).isTrue();

		List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
		assertThat(sideEffects.stream().filter(sideEffect -> sideEffect instanceof TitleChangeSideEffect).count())
				.isEqualTo(1);
	}

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

		assertThat(MyceliumSideEffectService.isGrantsNetwork(relationship)).isTrue();

		// c1 hasAssociationWith c2 is not a grants network relation
		EdgeDTO c1HasAssociationWithC2 = new EdgeDTO();
		c1IsPartOfC2.setType("hasAssociationWith");
		relationship.setRelations(List.of(c1HasAssociationWithC2));
		assertThat(MyceliumSideEffectService.isGrantsNetwork(relationship)).isFalse();
	}

	@Test
	void detectGrantsNetworkForegoSideEffect_modification() {
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

		assertThat(myceliumSideEffectService.detectGrantsNetworkForegoSideEffect(before, after)).isTrue();
	}

	@Test
	void detectGrantsNetworkForegoSideEffect_deleted() {
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

		assertThat(myceliumSideEffectService.detectGrantsNetworkForegoSideEffect(before, null)).isTrue();
	}
}