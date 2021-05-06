package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumService {

	public static final String IMPORT_REQUEST_TYPE = "mycelium-import";

	private final GraphService graphService;
	private final RequestService requestService;

	public MyceliumService(GraphService graphService, RequestService requestService) {
		this.graphService = graphService;
		this.requestService = requestService;
	}

	/**
	 * Proxy method to save the {@link Request} via the {@link RequestService}
	 * @param request the {@link Request} to persist
	 * @return the persisted {@link Request}
	 */
	public Request save(Request request) {
		return requestService.save(request);
	}

	/**
	 * Create a new Import Request with data path, log path and xml stored as payload
	 *
	 * @param xml the XML payload
	 * @return the created {@link Request}
	 */
	public Request createImportRequest(String xml) {
		Request request = new Request();
		request.setType(IMPORT_REQUEST_TYPE);
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());

		request = save(request);

		// create data path
		try {
			Path path = Paths.get(requestService.getDataPathFor(request));
			Files.createDirectories(path);
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			log.error("Failed creating data path {}", e.getMessage());
		}

		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));

		// payload path
		String dataPath = requestService.getDataPathFor(request);
		String payloadPath = dataPath + File.separator + "payload";
		request.setAttribute(Attribute.PAYLOAD_PATH, payloadPath);

		// save xml to payload
		try {
			Files.createFile(Paths.get(payloadPath));
			Files.writeString(Paths.get(payloadPath), xml);
		} catch (IOException e) {
			log.error("Failed to write to payload path: {} with content {}", payloadPath, xml);
		}

		return request;
	}

	public void ingest(String payload) {

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(payload);

		// insert into neo4j graph
		graphService.ingestGraph(graph);

		// todo reverse links generations
		// todo implicit links generations
	}

	/**
	 * Finds a {@link Collection<Vertex>} of RegistryObject that is considered identical.
	 * Identical Registry object shares the same Identifier (isSameAs to the same
	 * Identifier). This property is transitive
	 * @param origin the {@link Vertex} to start the search in
	 * @return a {@link Collection<Vertex>} that contains all the identical {@link Vertex}
	 */
	public Collection<Vertex> getDuplicateRegistryObject(Vertex origin) {
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());

		// only return the RegistryObject
		return sameAsNodeCluster.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.collect(Collectors.toList());
	}

}
