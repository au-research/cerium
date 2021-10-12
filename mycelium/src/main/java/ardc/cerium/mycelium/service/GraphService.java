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
import ardc.cerium.mycelium.rifcs.RecordState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Service to ingest/import Graph data
 *
 * @author Minh Duc Nguyen
 */
@Service
@Slf4j
@Getter
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
		log.debug("Starting graph ingest verticesCount: {}, edgesCount: {}", graph.getVertices().size(),
				graph.getEdges().size());

		graph.getVertices().forEach(vertex -> {
			try {
				log.debug("Ingesting vertex id: {} type: {}", vertex.getIdentifier(), vertex.getIdentifierType());
				ingestVertex(vertex);
			}
			catch (Exception e) {
				log.error("Failed to ingest vertex id: {} Reason: {}", vertex.getIdentifier(), e.getMessage());
			}
		});

		graph.getEdges().forEach(edge -> {
			try {
				log.debug("Ingesting edge from {} to {} type: {}", edge.getFrom().getIdentifier(),
						edge.getTo().getIdentifier(), edge.getType());
				ingestEdge(edge);
			}
			catch (Exception e) {
				log.error("Failed to ingest edge from {} to {} type: {} Reason: {}", edge.getFrom().getIdentifier(),
						edge.getTo().getIdentifier(), edge.getType(), e.getMessage());
			}
		});
		log.debug("Finished ingesting graph");
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
	 * Delete single {@link Vertex} using SDN
	 * @param vertex the {@link Vertex}
	 */
	public void deleteVertex(Vertex vertex) {
		// todo update the vertex
		if (vertexRepository.existsVertexByIdentifierAndIdentifierType(vertex.getIdentifier(),
				vertex.getIdentifierType())) {
			vertexRepository.delete(vertex);
		}
	}


	public Collection<Relationship> getMyDuplicateRelationships(String identifier, String identifierType, Pageable pageable) {
		String cypherQuery = "MATCH (origin:Vertex {identifier: \""+identifier+"\", identifierType: \""+identifierType+"\"})\n" +
				"OPTIONAL MATCH (origin)-[:isSameAs*1..]-(duplicates)\n" +
				"WITH collect(origin) + collect(duplicates) as identical\n" +
				"UNWIND identical as from\n" +
				"WITH distinct from\n" +
				"MATCH (from)-[r]->(to)\n" +
				"WHERE type(r) <> \"isSameAs\"\n" +
				"RETURN from, to, collect(r) as relations\n" +
				"SKIP "+pageable.getOffset()+" LIMIT "+pageable.getPageSize()+";";
		return getRelationships(cypherQuery);
	}

	public Collection<Relationship> getDuplicateOutboundRelationships(String identifier, String identifierType) {
		String cypherQuery = "MATCH (origin:Vertex {identifier: \""+identifier+"\", identifierType: \""+identifierType+"\"})\n" +
				"OPTIONAL MATCH (origin)-[:isSameAs*1..]-(duplicates)\n" +
				"WITH collect(origin) + collect(duplicates) as identical\n" +
				"UNWIND identical as from\n" +
				"WITH distinct from\n" +
				"MATCH (from)-[r]->(to)\n" +
				"WHERE type(r) <> \"isSameAs\"\n" +
				"RETURN from, to, collect(r) as relations;";
		return getRelationships(cypherQuery);
	}


	/**
	 * Obtain all Outbound relationships from the starting node identified by the identifier and identifierType
	 *
	 * @param identifier the Identifier String
	 * @param identifierType the type of the Identifier as String
	 * @return a {@link Collection} of {@link Relationship}
	 */
	public Collection<Relationship> getDirectOutboundRelationships(String identifier, String identifierType) {
		String cypherQuery = "MATCH (from:Vertex {identifier: \"" + identifier + "\", identifierType: \""
				+ identifierType + "\"})\n" + "MATCH (from)-[r]->(to)\n" + "WHERE type(r) <> \"isSameAs\"\n"
				+ "RETURN from, to, collect(r) as relations;";
		return getRelationships(cypherQuery);
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
				.set(relation.property("origin").to(Cypher.literalOf(edge.getOrigin())),
						relation.property("reverse").to(Cypher.literalOf(edge.isReverse())),
						relation.property("internal").to(Cypher.literalOf(edge.isInternal())),
						relation.property("public").to(Cypher.literalOf(edge.isPublic())),
						relation.property("duplicate").to(Cypher.literalOf(edge.isDuplicate())))
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
				.mappedBy(((typeSystem, record) -> {
					Node node = record.get("n").asNode();
					return vertexMapper.getConverter().convert(node);
				})).all();
	}

	/**
	 * Finds a {@link Collection} of {@link Vertex} of RegistryObject that is considered
	 * identical. Identical Registry object shares the same Identifier (isSameAs to the
	 * same Identifier). This property is transitive
	 * @param origin the {@link Vertex} to start the search in
	 * @return a {@link Collection} of {@link Vertex} that contains all the identical
	 * {@link Vertex}
	 */
	public Collection<Vertex> getDuplicateRegistryObject(Vertex origin) {
		Collection<Vertex> sameAsNodeCluster = getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());

		// only return the RegistryObject
		return sameAsNodeCluster.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.collect(Collectors.toList());
	}

	/**
	 * Obtaining the duplicate registryObjects. Also contains self.
	 *
	 * Utilize {@link #getSameAs(String, String)} with values obtained from the {@link Vertex}
	 * @param vertex the {@link Vertex} to obtain duplicate from
	 * @return a {@link Collection} of duplicate {@link Vertex}
	 */
	public Collection<Vertex> getSameAsRegistryObject(Vertex vertex) {
		return getSameAs(vertex.getIdentifier(), vertex.getIdentifierType()).stream()
				.filter(v -> v.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());
	}

	public Vertex getVertexByIdentifier(String identifier, String identifierType) {
		return neo4jClient
				.query("MATCH (n:Vertex {identifier: $identifier, identifierType: $identifierType})\n" + "RETURN n;")
				.bind(identifier).to("identifier").bind(identifierType).to("identifierType").fetchAs(Vertex.class)
				.mappedBy(((typeSystem, record) -> {
					Node node = record.get("n").asNode();
					return vertexMapper.getConverter().convert(node);
				})).one().orElse(null);
	}

	/**
	 * Obtain all relations for a given Vertex
	 *
	 * All outbound relations are returned except
	 * {@link RIFCSGraphProvider#RELATION_SAME_AS}
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
				.skip(pageable.getOffset()).limit(pageable.getPageSize()).build();

		String cypherQuery = statement.getCypher();

		return getRelationships(cypherQuery);
	}

	/**
	 * Get a Collection of Relationship given a cypherQuery.
	 *
	 * Requires the cypherQuery to have a return statement of from, to and relations where
	 * the from and to are {@link Node} and relations is a collection of
	 * {@link org.neo4j.driver.types.Relationship}
	 * @param cypherQuery a proper well formatted cypherQuery
	 * @return a {@link Collection} of {@link Relationship}
	 */
	private Collection<Relationship> getRelationships(String cypherQuery) {
		log.debug("getRelationships cypher: {}", cypherQuery);
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

	/**
	 * Ingest relations from duplicate as Implicit Edges
	 * @param vertices a {@link List} of {@link Vertex} to generate and ingest Duplicate
	 * relations for
	 */
	public void generateDuplicateRelationships(List<Vertex> vertices) {

		// for each imported Vertex (as origin)
		vertices.forEach(origin -> {
			// AllDupes contains Identifier and itself
			Collection<Vertex> AllDupes = getSameAs(origin.getIdentifier(), origin.getIdentifierType());

			// duplicates only contains true duplicate
			List<Vertex> duplicates = AllDupes.stream()
					.filter(duplicate -> !duplicate.getIdentifier().equals(origin.getIdentifier())
							&& !duplicate.getLabels().contains("Identifier"))
					.collect(Collectors.toList());

			duplicates.forEach(duplicate -> {
				// todo pagination when there's more than 100
				Collection<Relationship> allRelationsFromVertex = allRelationsFromVertex(duplicate,
						PageRequest.of(0, 100));
				allRelationsFromVertex.forEach(relationship -> {
					relationship.getRelations().forEach(relation -> {
						Edge edge = new Edge(origin, relationship.getTo(), relation.getType());
						edge.setOrigin(relation.getOrigin());
						edge.setReverse(relation.isReverse());
						edge.setInternal(relation.isInternal());
						edge.setPublic(relation.isPublic());
						edge.setDuplicate(true);
						ingestEdge(edge);

						// reversed edge should also be inserted
						Edge reversed = RIFCSGraphProvider.getReversedEdge(edge);
						edge.setDuplicate(true);
						ingestEdge(reversed);
					});
				});
			});
		});
	}

	public Collection<Vertex> getParentCollection(String registryObjectId) {
		return neo4jClient.query("MATCH (origin:RegistryObject) WHERE origin.identifier = $identifier\n"
				+ "CALL apoc.path.spanningTree(origin, {\n"
				+ "    relationshipFilter: \"isSameAs|isPartOf>|isOutputOf>|isFundedBy>\",\n" + "    minLevel: 1,\n"
				+ "    maxLevel: 100})\n" + "YIELD path WITH nodes(path) as targets\n"
				+ "MATCH (collection:RegistryObject {objectClass: \"collection\"}) WHERE collection IN targets\n"
				+ "MATCH (party:RegistryObject {objectClass: \"party\"}) WHERE party IN targets\n"
				+ "RETURN DISTINCT collection SKIP 0 LIMIT 100")
				.bind(registryObjectId).to("identifier")
				.fetchAs(Vertex.class)
				.mappedBy((typeSystem, record) -> {
					Node n = record.get("collection").asNode();
					return vertexMapper.getConverter().convert(n);
				}).all();
	}

	public Collection<Vertex> getChildCollection(String registryObjectId) {
		return neo4jClient.query("MATCH (origin:RegistryObject) WHERE origin.identifier = $identifier\n"
						+ "CALL apoc.path.spanningTree(origin, {\n"
						+ "    relationshipFilter: $relationshipFilter, minLevel: $minLevel, maxLevel: $maxLevel"
						+ "}) YIELD path WITH nodes(path) as targets\n"
						+ "MATCH (collection:RegistryObject {objectClass: 'collection'}) WHERE collection IN targets\n"
						+ "RETURN DISTINCT collection SKIP 0 LIMIT 100")
				.bind(registryObjectId).to("identifier")
				.bind("isSameAs|hasPart>|outputs>|funds>|isFunderOf>|hasOutput>").to("relationshipFilter")
				.bind(1).to("minLevel")
				.bind(100).to("maxLevel")
				.fetchAs(Vertex.class)
				.mappedBy((typeSystem, record) -> {
					Node n = record.get("collection").asNode();
					return vertexMapper.getConverter().convert(n);
				}).all();
	}

	/**
	 * Obtain a {@link RecordState} of a RegistryObject given the registryObjectId from
	 * the Graph database as of this moment
	 * @param registryObjectId the registryObjectId identifier
	 * @return the {@link RecordState} or null if the RegistryObject is not present in the
	 * database
	 */
	public RecordState getRecordState(String registryObjectId) {

		// if the registryObjectId doesn't exist in the graph
		Vertex registryObjectVertex = getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (registryObjectVertex == null) {
			log.debug("No RegistryObjectID Vertex found for RegistryObject[id={}]", registryObjectId);
			return null;
		}

		// the registryObjectKey vertex should also exist
		Collection<Vertex> sameAsNodeCluster = getSameAs(registryObjectVertex.getIdentifier(),
				registryObjectVertex.getIdentifierType());
		Vertex keyVertex = sameAsNodeCluster.stream()
				.filter(v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).findFirst().orElse(null);
		if (keyVertex == null) {
			log.error("No key vertex found for RegistryObject[id={}]", registryObjectId);
			return null;
		}

		RecordState state = new RecordState();
		state.setRegistryObjectId(registryObjectId);
		state.setRegistryObjectKey(keyVertex.getIdentifier());
		state.setOrigin(registryObjectVertex);
		state.setTitle(registryObjectVertex.getTitle());
		state.setRegistryObjectClass(registryObjectVertex.getObjectClass());
		// TODO obtain group from vertex (require Vertex to have group property)
		state.setGroup(null);
		state.setIdentical(sameAsNodeCluster);

		// outbound
		Collection<Relationship> outbounds = getDuplicateOutboundRelationships(registryObjectVertex.getIdentifier(),
				registryObjectVertex.getIdentifierType());

		// swap the origin to the vertex for all the outbounds
		outbounds = outbounds.stream().peek(relationship -> relationship.setFrom(registryObjectVertex)).collect(Collectors.toSet());

		state.setOutbounds(outbounds);

		return state;
	}

	public void setRegistryObjectKeyNodeTerminated() {
		String cypherQuery = "MATCH (terminate:Vertex {identifierType:\"ro:key\"})\n"
				+ "WHERE NOT (terminate)-[:isSameAs]-()\n" + "SET terminate:Terminated;";
		ResultSummary resultSummary = neo4jClient.query(cypherQuery).run();
		log.debug("Terminate ro:key nodes ResultSummary[{}]", resultSummary.counters());
	}

	public void reinstateTerminatedNodes() {
		String cypherQuery = "MATCH (n:Vertex {identifierType:\"ro:key\"})\n"
				+ "WHERE (n)-[:isSameAs]-(:RegistryObject)\n" + "REMOVE n:Terminated;";
		ResultSummary resultSummary = neo4jClient.query(cypherQuery).run();
		log.debug("Reinstate ro:key nodes ResultSummary[{}]", resultSummary.counters());
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamChildCollection(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|hasPart>|outputs>|isFunderOf>", "collection");
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamChildActivity(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|hasPart>", "activity");
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentParty(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isOutputOf>|isFundedBy>", "party");
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentActivity(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isPartOf>", "activity");
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentCollection(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isPartOf>", "collection");
	}
}
