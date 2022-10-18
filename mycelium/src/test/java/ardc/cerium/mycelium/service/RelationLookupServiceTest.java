package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@Import(RelationLookupService.class)
class RelationLookupServiceTest {

	@Autowired
	RelationLookupService relationLookupService;

	@Test
	void itLoadsLookupTableProperly() throws IOException {
		relationLookupService.loadLookupTable();
		assertThat(RelationLookupService.getLookupTable()).isNotNull();
		assertThat(RelationLookupService.getLookupTable().size()).isGreaterThan(0);
	}

	@Test
	void itCanResolveARelationByType() {
		RelationLookupEntry entry = RelationLookupService.resolve("isPartOf");
		assertThat(entry).isNotNull();
		assertThat(entry).isInstanceOf(RelationLookupEntry.class);
		assertThat(entry.getReverseRelationType()).isEqualTo("hasPart");
	}

	@Test
	void itCanTellIfARelationExistsInTheLookupTable() {
		assertThat(RelationLookupService.contains("isPartOf")).isTrue();
		assertThat(RelationLookupService.contains("weird relation")).isFalse();
	}

	@Test
	void itCanGetReverseRelation() {
		assertThat(RelationLookupService.getReverse("isPartOf")).isEqualTo("hasPart");
		assertThat(RelationLookupService.getReverse("weird relation")).isEqualTo(null);
		assertThat(RelationLookupService.getReverse("isDistributedBy")).isEqualTo("distributor");
	}

	@Test
	void itCanGetReverseRelationWithSpace() {
		assertThat(RelationLookupService.getReverse("Is part of")).isEqualTo("hasPart");
		assertThat(RelationLookupService.getReverse("has part ici pant")).isEqualTo("isParticipantIn");
	}


	@Test
	void itCanGetReverseRelationWithDefaultValue() {
		assertThat(RelationLookupService.getReverse("weird relation", "isRelatedPerhaps")).isEqualTo("isRelatedPerhaps");
	}
}