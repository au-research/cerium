package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({MyceliumBackupService.class, ObjectMapper.class})
class MyceliumBackupServiceTest {

    @MockBean
    GraphService graphService;

    @Autowired
    MyceliumBackupService myceliumBackupService;

    @Test
	void restoreGraphFile() {
        File file = new File("src/test/resources/backups/test-backup/datasources/1/graphs/1.json");
        myceliumBackupService.restoreGraphFile(file, "1");
        verify(graphService, times(1)).ingestGraph(any(Graph.class));
	}

    @Test
	void restoreBackup() {
        ReflectionTestUtils.setField(myceliumBackupService, "RDAPackupPath", "src/test/resources/backups/");
        myceliumBackupService.restoreBackup("test-backup", "1", "1");
        verify(graphService, times(4)).ingestGraph(any(Graph.class));
	}

    @TempDir
    Path tempDir;

    @Test
	void createBackup() {
        String path = tempDir.toString();
        ReflectionTestUtils.setField(myceliumBackupService, "RDAPackupPath", tempDir.toString());

        Vertex dataSourceVertex = new Vertex();
        dataSourceVertex.setId(1L);
        when(graphService.getVertexByIdentifier("1", "ds:id")).thenReturn(dataSourceVertex);
        Vertex vertex = new Vertex();
        vertex.setIdentifier("123");
        Stream<Vertex> stream = Arrays.asList(vertex).stream();
        when(graphService.streamRegistryObjectFromDataSource("1")).thenReturn(stream);

        myceliumBackupService.createBackup("test-memory-backup", "1");

        File backupDirectory = new File(path + "/test-memory-backup");
        assertThat(backupDirectory.exists()).isTrue();

        File dataSourceGraphsDirectory = new File(path + "/test-memory-backup/datasources/1/graphs");
        assertThat(dataSourceGraphsDirectory.exists()).isTrue();

        File dataSourceGraphFile = new File(path + "/test-memory-backup/datasources/1/graphs/datasource.json");
        assertThat(dataSourceGraphFile.exists()).isTrue();

        File vertexGraphFile = new File(path + "/test-memory-backup/datasources/1/graphs/123.json");
        assertThat(vertexGraphFile.exists()).isTrue();
	}
}