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
		assertThat(relationLookupService.getLookupTable()).isNotNull();
		assertThat(relationLookupService.getLookupTable().size()).isGreaterThan(0);
	}

	@Test
	void itCanResolveARelationByType() {
		RelationLookupEntry entry = relationLookupService.resolve("isPartOf");
		assertThat(entry).isNotNull();
		assertThat(entry).isInstanceOf(RelationLookupEntry.class);
		assertThat(entry.getReverseRelationType()).isEqualTo("hasPart");
	}

	@Test
	void itCanTellIfARelationExistsInTheLookupTable() {
		assertThat(relationLookupService.contains("isPartOf")).isTrue();
		assertThat(relationLookupService.contains("weird relation")).isFalse();
	}

	@Test
	void itCanGetReverseRelation() {
		assertThat(relationLookupService.getReverse("isPartOf")).isEqualTo("hasPart");
		assertThat(relationLookupService.getReverse("weird relation")).isNull();
	}
}