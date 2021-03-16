package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.Record;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { IGSNRecordService.class })
class IGSNRecordServiceTest {

	@Test
	@DisplayName("IGSNRecordService.create() returns an instance of Record with Record.type=IGSN prefilled")
	void create() {
		Record record = IGSNRecordService.create();
		assertThat(record).isNotNull();
		assertThat(record).isInstanceOf(Record.class);
		assertThat(record.getType()).isEqualTo(IGSNRecordService.recordType);
	}

}