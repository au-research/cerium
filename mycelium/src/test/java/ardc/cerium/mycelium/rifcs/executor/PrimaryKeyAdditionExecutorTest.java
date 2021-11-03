package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.PrimaryKeySetting;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PrimaryKeyAdditionExecutorTest {

	@Test
	void detect() {
		DataSource before = new DataSource();

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		afterPKSetting.getPrimaryKeys().add(pk1);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after)).isTrue();
		assertThat(PrimaryKeyAdditionExecutor.detect(after, after)).isFalse();
	}

	@Test
	void detect_noChange() {
		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk2 = new PrimaryKey();
		pk2.setKey("ARDC");
		pk2.setRelationTypeFromCollection("isFundedBy");
		afterPKSetting.getPrimaryKeys().add(pk2);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after)).isFalse();
	}

	@Test
	void detect_relationTypeChange() {
		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk2 = new PrimaryKey();
		pk2.setKey("ARDC");
		pk2.setRelationTypeFromCollection("hasAssociationWith");
		afterPKSetting.getPrimaryKeys().add(pk2);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after)).isTrue();
	}

}