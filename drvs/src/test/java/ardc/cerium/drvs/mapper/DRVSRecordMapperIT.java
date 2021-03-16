package ardc.cerium.drvs.mapper;

import ardc.cerium.drvs.TestHelper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.drvs.dto.DRVSRecordDTO;
import ardc.cerium.drvs.dto.mapper.DRVSRecordMapper;
import ardc.cerium.drvs.service.DRVSImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.transaction.Transactional;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@TestPropertySource(properties = { "app.drvs.enabled=true", "spring.jpa.open-in-view=false",
		"spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true" })
class DRVSRecordMapperIT {

    @Autowired
    DRVSRecordMapper drvsRecordMapper;

    @Autowired
    RecordRepository recordRepository;

    @Test
    @Transactional
	void mapFromRecordtoDRVSRecordDTO() {
        // given a Record
        Record record = TestHelper.mockRecord();
        record.setType(DRVSImportService.DRVS_RECORD_TYPE);
        record.setOwnerType(Record.OwnerType.DataCenter);
        record.setTitle("Test Record");
        record.setAllocationID(UUID.randomUUID());
        recordRepository.saveAndFlush(record);

        // when convert to DTO
        DRVSRecordDTO dto = drvsRecordMapper.getConverter().convert(record);

        // a submission is available
        assertThat(dto).isNotNull();
	}
}