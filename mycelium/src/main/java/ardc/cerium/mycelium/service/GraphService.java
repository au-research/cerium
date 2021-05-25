package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.util.Neo4jClientBiFunctionHelper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.types.Node;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

	private final VertexMapper vertexMapper;

	private final EdgeDTOMapper edgeDTOMapper;

	public GraphService(VertexRepository vertexRepository, Neo4jClient neo4jClient, VertexMapper vertexMapper,
			EdgeDTOMapper edgeDTOMapper) {
		this.vertexRepository = vertexRepository;
		this.neo4jClient = neo4jClient;
		this.vertexMapper = vertexMapper;
		this.edgeDTOMapper = edgeDTOMapper;
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

	/**
	 * search the neo4j database based on a list of criteria and pagination
	 * @param criteriaList a list of {@link SearchCriteria} to start the search in
	 * @param pageable the pagination and sorting provided by {@link Pageable}
	 * @return a {@link Collection} of {@link Relationship}
	 */
	public Collection<Relationship> search(List<SearchCriteria> criteriaList, Pageable pageable) {

		// construct the cypher query
		String cypherQuery = getRelationshipCypherQuery(criteriaList);
		cypherQuery += " return from, to, collect(r) as relations skip " + pageable.getOffset() + " limit "
				+ pageable.getPageSize() + ";";
		log.debug("CypherQuery(search): {}", cypherQuery);

		// perform the query
		return getRelationships(cypherQuery);
	}

	/**
	 * Provide the total found based on a list of criteria
	 * @param criteriaList a list of {@link SearchCriteria} filters
	 * @return the total count of related objects that matches the search criteria
	 */
	public int searchCount(List<SearchCriteria> criteriaList) {
		String cypherQuery = getRelationshipCypherQuery(criteriaList);
		cypherQuery += " return count(*) as total";
		log.debug("CypherQuery(count): {}", cypherQuery);

		return neo4jClient.query(cypherQuery).fetchAs(Integer.class).one().get();
	}

	/**
	 * A cypher query constructor used by {@link #search(List, Pageable)} and
	 * {@link #searchCount(List)}. This cypher query is not completed (missing the return
	 * statement) that should be filled in afterwards
	 *
	 * todo consider refactoring to use cypher-dsl once the StringBuilder gets too crowded
	 * @param criteriaList a list of {@link SearchCriteria} filters
	 * @return the cypher query {@link String}
	 */
	public String getRelationshipCypherQuery(List<SearchCriteria> criteriaList) {
		StringBuilder cypherQuery = new StringBuilder("MATCH (from:RegistryObject)-[r]->(to) ");

		List<String> wheres = new ArrayList<>();

		// isSameAs relationship to be excluded
		wheres.add(String.format("type(r) <> \"%s\"", RIFCSGraphProvider.RELATION_SAME_AS));

		for (SearchCriteria criteria : criteriaList) {
			if (criteria.getKey().equals("fromIdentifierValue")) {
				wheres.add(String.format("from.identifier = \"%s\"", criteria.getValue()));
			}
			else if (criteria.getKey().equals("fromIdentifierType")) {
				wheres.add(String.format("from.identifierType = \"%s\"", criteria.getValue()));
			}
		}
		if (wheres.size() > 0) {
			cypherQuery.append(" WHERE ").append(String.join(" AND ", wheres));
		}
		return cypherQuery.toString();
	}

	/**
	 * Ingest the {@link Edge} using Cypher Query and {@link Neo4jClient}. This is due to
	 * the dynamic relationship and relationship properties requirements
	 * @param edge the {@link Edge} to ingest
	 */
	public void ingestEdge(Edge edge) {

		org.neo4j.cypherdsl.core.Node from = Cypher.node("Vertex").named("from");
		org.neo4j.cypherdsl.core.Node to = Cypher.node("Vertex").named("to");

		org.neo4j.cypherdsl.core.Relationship relation = from.relationshipTo(to, edge.getType()).named("r");

		Statement statement = Cypher.match(from).match(to)
				.where(from.property("identifier").isEqualTo(Cypher.literalOf(edge.getFrom().getIdentifier())))
				.and(from.property("identifierType").isEqualTo(Cypher.literalOf(edge.getFrom().getIdentifierType())))
				.and(to.property("identifier").isEqualTo(Cypher.literalOf(edge.getTo().getIdentifier())))
				.and(to.property("identifierType").isEqualTo(Cypher.literalOf(edge.getTo().getIdentifierType())))
				.merge(relation)
				.set(
						relation.property("origin").to(Cypher.literalOf(edge.getOrigin())),
						relation.property("reverse").to(Cypher.literalOf(edge.isReverse())),
						relation.property("internal").to(Cypher.literalOf(edge.isInternal())),
						relation.property("public").to(Cypher.literalOf(edge.isPublic())),
						relation.property("implicit").to(Cypher.literalOf(edge.isImplicit()))
				)
				.returning("r").build();

		String cypherQuery = statement.getCypher();

		neo4jClient.query(cypherQuery).run();
	}

	/**
	 * Finds all nodes that has the isSameAs variable length matching
	 * @param identifier the String format of the identifier Value
	 * @param identifierType the IdentifierType of the origin node identifierValue
	 * @return the unique {@link Collection} of {@link Vertex} that matches the query
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

	/**
	 * Obtain all relations for a given Vertex
	 *
	 * All outbound relations are returned except {@link RIFCSGraphProvider#RELATION_SAME_AS}
	 * @param vertex the {@link Vertex} to obtain Relationship from
	 * @param pageable the {@link Pageable} for pagination
	 * @return a collection of {@link Relationship}
	 */
	public Collection<Relationship> allRelationsFromVertex(Vertex vertex, Pageable pageable) {
		org.neo4j.cypherdsl.core.Node from = Cypher.node("Vertex").named("from");
		org.neo4j.cypherdsl.core.Node to = Cypher.node("Vertex").named("to");
		org.neo4j.cypherdsl.core.Relationship relation = from.relationshipTo(to).named("r");

		Statement statement = Cypher.match(relation)
				.where(from.property("identifier").isEqualTo(Cypher.literalOf(vertex.getIdentifier())))
				.and(from.property("identifierType").isEqualTo(Cypher.literalOf(vertex.getIdentifierType())))
				.and(Functions.type(relation).isNotEqualTo(Cypher.literalOf(RIFCSGraphProvider.RELATION_SAME_AS)))
				.returning(Functions.collect(relation).as("relations"), from.as("from"), to.as("to"))
				.skip(pageable.getOffset()).limit(pageable.getPageSize())
				.build();

		String cypherQuery = statement.getCypher();

		return getRelationships(cypherQuery);
	}

	/**
	 * Get a Collection of Relationship given a cypherQuery.
	 *
	 * Requires the cypherQuery to have a return statement of from, to and relations where the from and to are {@link Node} and relations is a collection of {@link org.neo4j.driver.types.Relationship}
	 * @param cypherQuery a proper well formatted cypherQuery
	 * @return a {@link Collection} of {@link Relationship}
	 */
	private Collection<Relationship> getRelationships(String cypherQuery) {
		return neo4jClient.query(cypherQuery).fetchAs(Relationship.class).mappedBy((typeSystem, record) -> {

			// convert from to fromVertex
			Node fromNode = record.get("from").asNode();
			Vertex fromVertex = vertexMapper.getConverter().convert(fromNode);

			// convert to to toVertex
			Node toNode = record.get("to").asNode();
			Vertex toVertex = vertexMapper.getConverter().convert(toNode);

			// convert collect(r) as relations to List<Edge> for Relationship
			List<EdgeDTO> edges = record.get("relations").asList().stream()
					.map(rel -> (org.neo4j.driver.types.Relationship) rel)
					.map(rel -> edgeDTOMapper.getConverter().convert(rel)).collect(Collectors.toList());

			// build and return the Relationship
			Relationship relationship = new Relationship();
			relationship.setFrom(fromVertex);
			relationship.setTo(toVertex);
			relationship.setRelations(edges);

			return relationship;
		}).all();
	}

	public void generateDuplicateRelationships(List<Vertex> vertices) {

		// for each imported Vertex (as origin)
		vertices.forEach(origin -> {
			// AllDupes contains Identifier and itself
			Collection<Vertex> AllDupes = getSameAs(origin.getIdentifier(), origin.getIdentifierType());

			// duplicates only contains true duplicate
			List<Vertex> duplicates = AllDupes.stream()
					.filter(duplicate -> !duplicate.getIdentifier().equals(origin.getIdentifier()) && !duplicate.getLabels().contains("Identifier")).collect(Collectors.toList());

			// foreach relations from any duplicate
			List<Edge> duplicateImplicitEdges = new ArrayList<>();

			duplicates.forEach(duplicate -> {
				// todo pagination when there's more than 100
				// todo generate reverse edge as well
				// todo ingestEdge on run time to avoid duplicateImplicitEdge memory build up
				Collection<Relationship> allRelationsFromVertex = allRelationsFromVertex(duplicate, PageRequest.of(0, 100));
				allRelationsFromVertex.forEach(relationship -> {
					relationship.getRelations().forEach(relation -> {
						Edge duplicateImplicit = new Edge(origin, relationship.getTo(), relation.getType());
						duplicateImplicit.setImplicit(true);
						duplicateImplicit.setOrigin("Duplicate");
						duplicateImplicitEdges.add(duplicateImplicit);
					});
				});
			});

			// create the duplicate implicit edges
			duplicateImplicitEdges.forEach(this::ingestEdge);
		});
	}
}
