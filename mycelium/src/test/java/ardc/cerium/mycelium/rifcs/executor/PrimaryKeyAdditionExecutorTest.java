package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.PrimaryKeySetting;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class PrimaryKeyAdditionExecutorTest {

	@MockBean
	MyceliumService myceliumService;

	@Test
	void detect() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));

		DataSource before = new DataSource();

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		afterPKSetting.getPrimaryKeys().add(pk1);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyAdditionExecutor.detect(null, after, myceliumService)).isTrue();
		assertThat(PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)).isTrue();
		assertThat(PrimaryKeyAdditionExecutor.detect(after, after, myceliumService)).isFalse();
	}

	@Test
	void detect_noChange() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));

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

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)).isFalse();
	}

	@Test
	void detect_relationTypeChange() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));
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

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)).isTrue();
	}

	@Test
	void detect_RecordDoesntExist() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(null);

		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk2 = new PrimaryKey();
		pk2.setKey("ARDC");
		pk2.setRelationTypeFromCollection("hasAssociationWith");
		afterPKSetting.getPrimaryKeys().add(pk2);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)).isFalse();
	}

}