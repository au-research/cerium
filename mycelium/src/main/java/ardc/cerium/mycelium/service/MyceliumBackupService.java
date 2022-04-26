package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
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

	/**
	 * Create and store a new Backup for a given data source.
	 *
	 *
	 * The local graph of the DataSource Vertex will be stored at datasource.json The
	 * local graph of every RegistryObject that belongs to that DataSource will be stored
	 * at {id}.json where id is the RegistryObjectId
	 * @param backupId the URL safe backup ID
	 * @param dataSourceId the Id of the DataSource
	 */
	public void createBackup(String backupId, String dataSourceId) {

        // backup path, /var/data/registry-backups/{backupId}/datasources/{backupId}/graphs/
		String backupPath = String.format(RDAPackupPath + "/%s/datasources/%s/graphs/", backupId, dataSourceId);
		log.info("Creating backup to path {}", backupPath);

        // create the directory if it doesn't exist
		File backupDirectory = new File(backupPath);
        if (!backupDirectory.isDirectory()) {
            backupDirectory.mkdirs();
        }
        // todo handle directory not created or not writable

        ObjectMapper objectMapper = new ObjectMapper();

        // backup datasource vertex to datasource.json
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

        // backup record vertex to {registryObjectId}.json
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

	/**
     * Restore a specific data source by backup Id
     *
     *
	 * @param backupId
	 * @param dataSourceId
	 */
    public void restoreBackup(String backupId, String dataSourceId, String correctedDataSourceId) {

        // backup path, /var/data/registry-backups/{backupId}/datasources/{backupId}/graphs/
        String backupPath = String.format(RDAPackupPath + "/%s/datasources/%s/graphs/", backupId, dataSourceId);
        log.info("Restoring backup from path {}", backupPath);

        File backupDirectory = new File(backupPath);
        ObjectMapper objectMapper = new ObjectMapper();

        // read the directory and restore the graph file for every single file within it
        // todo parse only *.json file
        File[] files = backupDirectory.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            restoreGraphFile(file, correctedDataSourceId);
        }
    }

	/**
     * Restore a graph file
     *
	 * @param file
	 */
    public void restoreGraphFile(File file, String correctedDataSourceId) {
        log.debug("Restoring File "+file);
        try {
            Graph graph = objectMapper.readValue(file, Graph.class);

            graph.getVertices().forEach(vertex -> {

                // remove the generatedId from the backups
                // or else some will not restore properly
                vertex.setId(null);

                // update the correctedDataSourceId
                if (vertex.getIdentifierType().equals(RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE)) {
                    // is a data source vertex
                    vertex.setIdentifier(correctedDataSourceId);
                } else if(vertex.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE)) {
                    // is a normal registryObject vertex
                    vertex.setDataSourceId(correctedDataSourceId);
                }

            });

            graph.getEdges().forEach(edge -> {
                edge.setId(null);
            });


            graphService.ingestGraph(graph);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
