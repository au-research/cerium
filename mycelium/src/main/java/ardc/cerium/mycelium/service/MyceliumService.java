package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.common.util.XMLUtil;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
		}
		catch (IOException e) {
			log.error("Failed to write to payload path: {} with content {}", payloadPath, xml);
		}

		return request;
	}

	public void validateRequest(Request request) {

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		if (payloadPath == null) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// validate payload
		String payload;
		try {
			payload = Helpers.readFile(payloadPath);
		} catch (IOException e) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// test payload is empty
		if (payload.isBlank()) {
			throw new ContentNotSupportedException("Payload is empty");
		}

		// test payload is well formed xml
		Element domDocument = XMLUtil.getDomDocument(payload);
		if (domDocument == null) {
			throw new ContentNotSupportedException("Payload is not well-formed XML");
		}

		// todo test payload is rifcs
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
	 * Search for relationships.
	 * @param criteriaList a list of {@link SearchCriteria} to start the search in
	 * @param pageable the pagination and sorting provided by {@link Pageable}
	 * @return a {@link PageImpl} of {@link Relationship}
	 */
	public Page<Relationship> search(List<SearchCriteria> criteriaList, Pageable pageable) {
		List<Relationship> result = new ArrayList<>(graphService.search(criteriaList, pageable));
		int total = graphService.searchCount(criteriaList);
		return new PageImpl<>(result, pageable, total);
	}

	/**
	 * Finds a {@link Collection} of {@link Vertex} of RegistryObject that is considered identical.
	 * Identical Registry object shares the same Identifier (isSameAs to the same
	 * Identifier). This property is transitive
	 * @param origin the {@link Vertex} to start the search in
	 * @return a {@link Collection} of {@link Vertex} that contains all the identical {@link Vertex}
	 */
	public Collection<Vertex> getDuplicateRegistryObject(Vertex origin) {
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());

		// only return the RegistryObject
		return sameAsNodeCluster.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.collect(Collectors.toList());
	}

}
