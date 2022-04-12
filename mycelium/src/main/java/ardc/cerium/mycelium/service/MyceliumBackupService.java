package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
public class MyceliumBackupService {

    @Autowired
    GraphService graphService;

    @Autowired
    ObjectMapper objectMapper;

    @Value( "${rda.backup.path:/var/data/registry-backups/}" )
    private String RDAPackupPath;

    public void createBackup(String backupId, String dataSourceId) {

        String backupPath = String.format(RDAPackupPath + "/%s/datasources/%s/graphs/", backupId, dataSourceId);
        log.info("Creating backup to path {}", backupPath);

        File backupDirectory = new File(backupPath);
        if (!backupDirectory.isDirectory()) {
            backupDirectory.mkdirs();
        }

        ObjectMapper objectMapper = new ObjectMapper();

        // backup data sources to datasources-graph/{id}/datasource.json
        Vertex datasourceVertex = graphService.getVertexByIdentifier(dataSourceId, "ds:id");
        Graph dataSourceGraph = graphService.getLocalGraph(datasourceVertex);
        try {
            File file = new File(backupPath +"datasource.json");
            if (! file.isFile()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            objectMapper.writeValue(file, dataSourceGraph);
        } catch (IOException e) {
            // todo handle exception
            e.printStackTrace();
        }

        // backup record records to datasources-graph/{id}/{registryObjectId}.json
        try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSourceId)) {
            stream.forEach(vertex -> {
                Graph graph = graphService.getLocalGraph(vertex);
                try {
                    File file = new File(backupPath + vertex.getIdentifier()+".json");
                    if (! file.isFile()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    objectMapper.writeValue(file, graph);
                } catch (IOException e) {
                    // todo handle exception
                    e.printStackTrace();
                }
            });
        }
    }

    public void restoreBackup(String backupId, String dataSourceId) {
        String backupPath = String.format(RDAPackupPath + "/%s/datasources/%s/graphs/", backupId, dataSourceId);
        log.info("Restoring backup from path {}", backupPath);

        File backupDirectory = new File(backupPath);
        ObjectMapper objectMapper = new ObjectMapper();

        File[] files = backupDirectory.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            restoreGraphFile(file);
        }
    }

    public void restoreGraphFile(File file) {
        log.debug("Restoring File "+file);
        try {
            Graph graph = objectMapper.readValue(file, Graph.class);

            // remove the generatedId from the backups
            // or else some will not restore properly
            graph.getVertices().forEach(vertex -> {
                vertex.setId(null);
            });
            graph.getEdges().forEach(vertex -> {
                vertex.setId(null);
            });

            graphService.ingestGraph(graph);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
