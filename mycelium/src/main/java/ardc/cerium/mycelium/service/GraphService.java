package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RelationDocument;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.util.Neo4jClientBiFunctionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Spring Service to ingest/import Graph data
 *
 * @author Minh Duc Nguyen
 */
@Service
@Slf4j
public class GraphService {

	private final VertexRepository vertexRepository;

	private final Neo4jClient neo4jClient;

	public GraphService(VertexRepository vertexRepository, Neo4jClient neo4jClient) {
		this.vertexRepository = vertexRepository;
		this.neo4jClient = neo4jClient;
	}

	/**
	 * Ingest an entire {@link Graph}
	 * @param graph the {@link Graph} to ingest
	 */
	public void ingestGraph(Graph graph) {
		graph.getVertices().forEach(this::ingestVertex);
		log.info("Ingested {} vertices", graph.getVertices().size());

		graph.getEdges().forEach(this::ingestEdge);
		log.info("Ingested {} edges", graph.getEdges().size());
	}

	/**
	 * Ingest a single {@link Vertex} using SDN
	 * @param vertex the {@link Vertex}
	 */
	public void ingestVertex(Vertex vertex) {
		// todo update the vertex
		if (!vertexRepository.existsVertexByIdentifierAndIdentifierType(vertex.getIdentifier(),
				vertex.getIdentifierType())) {
			vertexRepository.save(vertex);
		}
	}

	public Collection<RelationDocument> getRelationships(String identifier, String identifierType, int limit,
			int offset) {
		return neo4jClient
				.query("MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType})\n"
						+ "WITH origin MATCH (origin)-[:isSameAs*1..5]-(duplicates) \n"
						+ "WITH collect(origin) + collect(duplicates) as identicals\n"
						+ "WITH identicals MATCH p=(n)-[r]-() WHERE n in identicals AND type(r) <> 'isSameAs'\n"
						+ "RETURN p SKIP $skip LIMIT $limit")
				.bind(identifier).to("identifier").bind(identifierType).to("identifierType").bind(limit).to("limit")
				.bind(offset).to("skip").fetchAs(RelationDocument.class)
				.mappedBy(((typeSystem, record) -> Neo4jClientBiFunctionHelper.pathToRelationDocument(record, "p")))
				.all();
	}

	/**
	 * Ingest the {@link Edge} using Cypher Query and {@link Neo4jClient}. This is due to
	 * the dynamic relationship and relationship properties requirements
	 * @param edge the {@link Edge} to ingest
	 */
	public void ingestEdge(Edge edge) {
		// building a cypher string and just run it
		String cypher = String.format(
				"MATCH (from:Vertex {identifier: '%s', identifierType: '%s'}) \n" + "WITH from \n"
						+ "MATCH (to:Vertex {identifier: '%s', identifierType: '%s'}) \n" + "WITH from, to\n"
						+ "MERGE (from)-[r:%s]->(to) RETURN type(r);",
				edge.getFrom().getIdentifier(), edge.getFrom().getIdentifierType(), edge.getTo().getIdentifier(),
				edge.getTo().getIdentifierType(), edge.getType());

		// todo relationship properties, description, url
		// todo re-create the relationship or update existing relationship with the same
		// type

		neo4jClient.query(cypher).run();

		// binding Relationship doesn't work
		// neo4jClient.query("MATCH (from:Vertex {identifier: $fromID, identifierType:
		// $fromIDType}) WITH from \n"
		// + "MATCH (to:Vertex {identifier: $toID, identifierType: $toIDType}) WITH from,
		// to\n"
		// + "MERGE (from)-[r:$relationType]->(to) RETURN type(r);")
		// .bind(edge.getFrom().getIdentifier()).to("fromID")
		// .bind(edge.getFrom().getIdentifierType()).to("fromIDType")
		// .bind(edge.getTo().getIdentifier()).to("toID")
		// .bind(edge.getTo().getIdentifierType()).to("toIDType")
		// .bind(edge.getType()).to("relationType")
		// .run();
	}

	/**
	 * Finds all nodes that has the isSameAs variable length matching
	 * @param identifier the String format of the identifier Value
	 * @param identifierType the IdentifierType of the origin node identifierValue
	 * @return the unique {@link Collection<Vertex>} that matches the query
	 */
	public Collection<Vertex> getSameAs(String identifier, String identifierType) {
		return neo4jClient
				.query("MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType})\n"
						+ "OPTIONAL MATCH (origin)-[:isSameAs*1..5]-(duplicates) \n"
						+ "WITH collect(origin) + collect(duplicates) as identicals\n" + "UNWIND identicals as n\n"
						+ "RETURN distinct n;")
				.bind(identifier).to("identifier").bind(identifierType).to("identifierType").fetchAs(Vertex.class)
				.mappedBy(((typeSystem, record) -> Neo4jClientBiFunctionHelper.toVertex(record, "n"))).all();
	}

}
