package ardc.cerium.igsn.mapper;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.EmbargoService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.igsn.config.IGSNApplicationConfig;
import ardc.cerium.igsn.dto.IGSNRecordDTO;
import ardc.cerium.igsn.dto.mapper.IGSNRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { IGSNRecordMapper.class, ModelMapper.class, SchemaService.class})
@TestPropertySource(properties="app.igsn.enabled=true")
class IGSNRecordMapperTest {

	@Autowired
	IGSNRecordMapper mapper;

	@Autowired
	SchemaService schemaService;

	@MockBean
	IGSNApplicationConfig igsnApplicationConfig;

	@MockBean
	EmbargoService embargoService;

	@Test
	public void convertToEntity() throws IOException {
		when(igsnApplicationConfig.getPortalUrl()).thenReturn("http://localhost:8086/igsn-portal/");
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier igsn = TestHelper.mockIdentifier(record);
		record.setIdentifiers(Collections.singletonList(igsn));

		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		Version version = TestHelper.mockVersion(record);
		version.setContent(xml.getBytes());
		version.setCurrent(true);
		version.setSchema(SchemaService.ARDCv1);
		record.setCurrentVersions(Collections.singletonList(version));

		IGSNRecordDTO dto = mapper.getConverter().convert(record);

		assertThat(dto).isNotNull();
		assertThat(dto).isInstanceOf(IGSNRecordDTO.class);

		// ardc.cerium.core.igsn
		assertThat(dto.getIgsn()).isNotNull();
		assertThat(dto.getIgsn()).isInstanceOf(IdentifierDTO.class);

		// portalUrl
		assertThat(dto.getPortalUrl()).isNotBlank();

		// status
		assertThat(dto.getStatus()).isEqualTo("Registered");

		// no embargo
		assertThat(dto.getEmbargoDate()).isNull();
	}

	@Test
	void convertToEntity_reservedStatus() {
		when(igsnApplicationConfig.getPortalUrl()).thenReturn("http://localhost:8086/igsn-portal/");
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier igsn = TestHelper.mockIdentifier(record);
		igsn.setStatus(Identifier.Status.RESERVED);
		record.setIdentifiers(Collections.singletonList(igsn));

		IGSNRecordDTO dto = mapper.getConverter().convert(record);
		assertThat(dto).isNotNull();
		assertThat(dto.getStatus()).isEqualTo("Reserved");
	}

	@Test
	public void convertToEntity_embargo() {
		when(igsnApplicationConfig.getPortalUrl()).thenReturn("http://localhost:8086/igsn-portal/");
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier igsn = TestHelper.mockIdentifier(record);
		record.setIdentifiers(Collections.singletonList(igsn));

		Embargo embargo = TestHelper.mockEmbargo(record);
		when(embargoService.findByRecord(any(Record.class))).thenReturn(embargo);

		IGSNRecordDTO dtoWithEmbargo = mapper.getConverter().convert(record);
		assertThat(dtoWithEmbargo).isNotNull();
		assertThat(dtoWithEmbargo.getEmbargoDate()).isNotNull();
		assertThat(dtoWithEmbargo.getEmbargoDate()).isEqualTo(embargo.getEmbargoEnd());
	}
}