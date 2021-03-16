package ardc.cerium.igsn.task;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ProcessTitleTaskTest {

	@MockBean
	VersionService versionService;

	@MockBean
	RecordService recordService;

	@Autowired
	SchemaService schemaService;

	@Test
	void run() throws IOException {
		// record without title and a version with the title
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setTitle(null);
		Version version = TestHelper.mockVersion(record);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);

		// record with title that will be returned
		Record expected = spy(TestHelper.mockRecord(record.getId()));
		expected.setTitle("Something");

		// setup the world
		when(versionService.findVersionForRecord(any(Record.class), anyString())).thenReturn(version);
		when(recordService.save(any(Record.class))).thenReturn(expected);

		ProcessTitleTask task = new ProcessTitleTask(record, versionService, recordService, schemaService);
		task.run();

		// record.setTitle is called
		verify(expected, times(1)).setTitle(anyString());

		// save is called to persist the record after title processing
		verify(recordService, times(1)).save(any(Record.class));
	}

}