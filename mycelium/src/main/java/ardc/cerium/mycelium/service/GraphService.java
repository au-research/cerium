package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.model.*;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.rifcs.RecordState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ardc.cerium.mycelium.provider.RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE;

/**
 * Spring Service to ingest/import Graph data
 *
 * @author Minh Duc Nguyen
 */
@Service
@Slf4j
@Getter
@Setter
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


	@PostConstruct
	public void boot() {
		insertIndices();
	}

	public void insertIndices() {
		neo4jClient.query("CREATE INDEX vertex_id IF NOT EXISTS FOR (n:Vertex) ON (n.identifier);").run();
		neo4jClient.query("CREATE INDEX ro_id IF NOT EXISTS FOR (n:RegistryObject) ON (n.identifier);").run();
		neo4jClient.query("CREATE INDEX vertex_type IF NOT EXISTS FOR (n:Vertex) ON (n.identifierType);").run();
		neo4jClient.query("CREATE INDEX ro_class IF NOT EXISTS FOR (n:RegistryObject) ON (n.objectClass);").run();
	}

	/**
	 * Ingest an entire {@link Graph}
	 * @param graph the {@link Graph} to ingest
	 */
	@Transactional
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
		Vertex existing = getVertexByIdentifier(vertex.getIdentifier(), vertex.getIdentifierType());
		if (existing == null) {
			// create
			vertex.setUpdatedAt(new Date());
			vertex.setCreatedAt(new Date());
			vertexRepository.save(vertex);
		} else {
			// update existing
			existing.setTitle(vertex.getTitle());
			existing.setUpdatedAt(new Date());
			existing.setObjectClass(vertex.getObjectClass());
			existing.setObjectType(vertex.getObjectType());
			existing.setUrl(vertex.getUrl());
			existing.setNotes(vertex.getNotes());
			existing.setGroup(vertex.getGroup());
			existing.setMeta(vertex.getMeta());
			vertexRepository.save(existing);
		}
	}

	/**
	 * Delete single {@link Vertex} using SDN
	 * @param vertex the {@link Vertex}
	 */
	@Transactional
	public void deleteVertex(Vertex vertex) {
		// todo update the vertex
		// we can't delete null so do a try catch
		try {
			if (vertexRepository.existsVertexByIdentifierAndIdentifierType(vertex.getIdentifier(),
					vertex.getIdentifierType())) {
				vertexRepository.delete(vertex);
			}
		}catch(Exception e){
			log.error("Unable to delete Vertex {}", vertex);
		}
	}

	public Collection<Relationship> getMyDuplicateRelationships(String identifier, String identifierType,
			Pageable pageable) {
		Vertex v = getVertexByIdentifier(identifier, identifierType);
		if(v == null){
			return Collections.emptyList();
		}
		String labelFilter = "";
		if(v.getStatus()!= null && v.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			labelFilter = "labelFilter: '-DRAFT', \n";
		}
		long offset = pageable.getOffset();
		if(offset < 0){
			offset = 0L;
		}
		String cypherQuery = "MATCH (origin:Vertex {identifier: '" + identifier + "', identifierType: '"+identifierType+"'})\n"
				+ "CALL apoc.path.subgraphNodes(origin, {\n"
				+ "relationshipFilter: 'isSameAs',\n"
				+ labelFilter
				+ "    minLevel: 0,\n"
				+ "    maxLevel: 10\n"
				+ "})\n"
				+ "YIELD node\n"
				+ "WITH collect(origin) + collect(node) as complete\n"
				+ "UNWIND complete as from\n"
				+ "WITH distinct from\n"
				+ "MATCH (from)-[r]->(to)\n"
				+ "WHERE type(r) <> 'isSameAs'\n"
				+ "RETURN from, to, collect(r) as relations\n" + "SKIP " + offset + " LIMIT "
				+ pageable.getPageSize() + ";";
		return getRelationships(cypherQuery);
	}

	public Collection<Relationship> getDuplicateOutboundRelationships(String identifier, String identifierType) {
		Vertex v = getVertexByIdentifier(identifier, identifierType);
		if(v == null){
			return Collections.emptyList();
		}
		String labelFilter = "";
		if(v.getStatus()!= null && v.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			labelFilter = "labelFilter: '-DRAFT', \n";
		}
		String cypherQuery = "MATCH (origin:Vertex {identifier: '" + identifier + "', identifierType: '" + identifierType +"'}) \n"
				+ "CALL apoc.path.subgraphNodes(origin, { \n"
				+ "relationshipFilter: 'isSameAs', \n"
				+ labelFilter
				+ "    minLevel: 0, \n"
				+ "    maxLevel: 10 \n"
				+ "}) \n"
				+ "YIELD node\n"
				+ "WITH collect(origin) + collect(node) as complete\n"
				+ "UNWIND complete as from\n"
				+ "WITH distinct from\n"
				+ "MATCH (from)-[r]->(to) \n"
				+ "WHERE type(r) <> 'isSameAs' \n"
				+ "RETURN from, to, collect(r) as relations;";
		log.debug("CypherQuery(getDuplicateOutboundRelationships): {}", cypherQuery);
		return getRelationships(cypherQuery);
	}

	/**
	 * Obtain all Outbound relationships from the starting node identified by the
	 * identifier and identifierType
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
	 * Obtain all Outbound relationships from the starting node identified by the
	 * identifier and identifierType
	 * @param identifier the Identifier String
	 * @param identifierType the type of the Identifier as String
	 * @return a {@link Collection} of {@link Relationship}
	 */
	public Collection<Relationship> getDirectInboundRelationships(String identifier, String identifierType) {
		String cypherQuery = "MATCH (to:Vertex {identifier: \"" + identifier + "\", identifierType: \""
				+ identifierType + "\"})\n" + "MATCH (to)<-[r]-(from)\n" + "WHERE type(r) <> \"isSameAs\"\n"
				+ "RETURN to, from, collect(r) as relations;";
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
		Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String updateAt = formatter.format(new Date());
		Statement statement = Cypher.match(from).match(to)
				.where(from.property("identifier").isEqualTo(Cypher.literalOf(edge.getFrom().getIdentifier())))
				.and(from.property("identifierType").isEqualTo(Cypher.literalOf(edge.getFrom().getIdentifierType())))
				.and(to.property("identifier").isEqualTo(Cypher.literalOf(edge.getTo().getIdentifier())))
				.and(to.property("identifierType").isEqualTo(Cypher.literalOf(edge.getTo().getIdentifierType())))
				.merge(relation)
				.set(relation.property("origin").to(Cypher.literalOf(edge.getOrigin())),
						relation.property("reverse").to(Cypher.literalOf(edge.isReverse())),
						relation.property("description").to(Cypher.literalOf(edge.getDescription())),
						relation.property("url").to(Cypher.literalOf(edge.getUrl())),
						relation.property("internal").to(Cypher.literalOf(edge.isInternal())),
						relation.property("public").to(Cypher.literalOf(edge.isPublic())),
						relation.property("updatedAt").to(Cypher.literalOf(updateAt)),
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

		if(identifier == null || identifier.isEmpty() || identifierType == null || identifierType.isEmpty()){
			return Collections.emptyList();
		}
		Vertex v = getVertexByIdentifier(identifier, identifierType);
		return getSameAs(v);
	}


	/**
	 * Finds all nodes that has the isSameAs variable length matching
	 * Remove any path that contains a vertex with DRAFT status if the origin Vertex v is PUBLISHED
	 * @param v {@link Vertex} the Vertex whose duplicates we are looking for
	 * @return the unique {@link Collection} of {@link Vertex} that matches the query
	 */
	public Collection<Vertex> getSameAs(Vertex v) {
		if(v == null){
			return Collections.emptyList();
		}
		String labelFilter = "";
		if(v.getStatus()!= null && v.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			labelFilter = "labelFilter: '-DRAFT', \n";
		}
		return neo4jClient
				.query("MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType}) \n"
						+ "CALL apoc.path.subgraphNodes(origin, { \n"
						+ "relationshipFilter: 'isSameAs', \n"
						+ labelFilter
						+ "minLevel: 0, \n"
						+ "maxLevel: 10 \n}) \n"
						+ "YIELD node \n"
						+ "RETURN node;")
				.bind(v.getIdentifier()).to("identifier").bind(v.getIdentifierType()).to("identifierType").fetchAs(Vertex.class)
				.mappedBy(((typeSystem, record) -> {
					Node node = record.get("node").asNode();
					return vertexMapper.getConverter().convert(node);
				})).all();
	}


	/**
	 * Finds all (should be only 1) nodes that is related to the given Vertex's ro:key Identifier
	 * that has the required status
	 * Currently we support RegistryObject Vertices with
	 * DRAFT or PUBLISHED status that are related with 'isSameAs' to an 'ro:key' IdentifierType Vertex
	 * This function is used to identify and remove "false" duplicates from a result tree/graph
	 * @param v {@link Vertex} the Vertex that has a DRAFT or PUBLISHED version
	 * @param status the status of the record to be returned (DRAFT or PUBLISHED)
	 * @return the unique {@link Collection} of {@link Vertex} that the given Vertex v is an alternative status of
	 */
	public Collection<Vertex> getAltStatusRecord(Vertex v, String status) {
		return neo4jClient
				.query("MATCH (origin:Vertex {identifier: $id, identifierType: 'ro:id'}) \n"
						+ "OPTIONAL MATCH (origin)-[:isSameAs]-(k{identifierType:'ro:key'})-[:isSameAs]-(duplicate{status:$status,identifierType:'ro:id'}) \n"
						+ "RETURN duplicate;")
				.bind(v.getIdentifier()).to("id").bind(status).to("status").fetchAs(Vertex.class)
				.mappedBy(((typeSystem, record) -> {
					if(record.get("duplicate").isNull()){
						return new Vertex();
					}else{
						Node node = record.get("duplicate").asNode();
						return vertexMapper.getConverter().convert(node);
					}
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
		Collection<Vertex> sameAsNodeCluster = getSameAs(origin.getIdentifier(), origin.getIdentifierType()).stream()
				.filter(v -> v.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());


		// Remove DRAFT records if origin is PUBLISHED
		if(origin.getStatus() == null || origin.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
			Predicate<Vertex> isRecord = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
			Predicate<Vertex> isDraft = v -> v.getStatus().equals(Vertex.Status.DRAFT.name());
			sameAsNodeCluster.removeIf(isRecord.and(isDraft));

		}else{
				// remove the PUBLISHED record that the DRAFT is a copy of
				Collection<Vertex> publishedVersions =  getAltStatusRecord(origin, Vertex.Status.PUBLISHED.name());
				sameAsNodeCluster.removeAll(publishedVersions);
		}

		return 	sameAsNodeCluster;
	}

	/**
	 * Obtaining the duplicate registryObjects. Also contains self.
	 *
	 * Utilize {@link #getSameAs(String, String)} with values obtained from the
	 * {@link Vertex}
	 * @param vertex the {@link Vertex} to obtain duplicate from
	 * @return a {@link Collection} of duplicate {@link Vertex}
	 */
	public Collection<Vertex> getSameAsRegistryObject(Vertex vertex) {
		Collection<Vertex> sameAsNodeCluster = getSameAs(vertex.getIdentifier(), vertex.getIdentifierType()).stream()
				.filter(v -> v.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());


		// Remove DRAFT records if origin is PUBLISHED
		if(vertex.getStatus() == null || vertex.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
			Predicate<Vertex> isRecord = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
			Predicate<Vertex> isDraft = v -> v.getStatus().equals(Vertex.Status.DRAFT.name());
			sameAsNodeCluster.removeIf(isRecord.and(isDraft));
		}else{
			// remove the PUBLISHED record that the DRAFT is a copy of
			Collection<Vertex> publishedVersion =  getAltStatusRecord(vertex, Vertex.Status.PUBLISHED.name());
			sameAsNodeCluster.removeAll(publishedVersion);
		}

		return 	sameAsNodeCluster;
	}


	/**
	 * Obtaining the Vertex with the specific type that relates to the specific Vertex with sameAs relationship.
	 * (specifically used to find the ro:key of a registryObject)
	 * Utilize {@link #getSameAs(String, String)} with values obtained from the
	 * {@link Vertex}
	 * @param vertex the {@link Vertex} to obtain duplicate from
	 * @param type String the type of the Identifier Vertex to be returned
	 * @return a {@link Collection} of duplicate {@link Vertex}
	 */
	public Optional<Vertex> getSameAsIdentifierWithType(Vertex vertex, String type) {
		return getSameAs(vertex.getIdentifier(), vertex.getIdentifierType()).stream()
				.filter(v -> v.getIdentifierType().equals(type)).findFirst();
	}

	/**
	 * Obtaining the Vertex with the specific type that relates to the specific Vertex with sameAs relationship.
	 * (specifically used to find the ro:id of a relatedObject)
	 * Utilize {@link #getSameAs(String, String)} with values obtained from the
	 * {@link Vertex}
	 * @param identifier the value
	 * @param identifierType the type
	 * @param type String the type of the Identifier Vertex to be returned
	 * @return a {@link Collection} of duplicate {@link Vertex}
	 */
	public Optional<Vertex> getSameAsIdentifierWithType(String identifier, String identifierType, String type) {
		return getSameAs(identifier, identifierType).stream()
				.filter(v -> v.getIdentifierType().equals(type)).findFirst();
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

	public Collection<String> getVertexIdentifiersByType(String identifierType) {
		return neo4jClient
				.query("MATCH (n:Vertex {identifierType: $identifierType})\n" + "RETURN n;")
				.bind(identifierType).to("identifierType").fetchAs(String.class)
				.mappedBy(((typeSystem, record) -> {
					Node node = record.get("n").asNode();
					return node.get("identifier").asString();
				})).all();
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

			// convert to toVertex
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
				.filter(v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).findFirst()
				.orElse(null);
		if (keyVertex == null) {
			log.error("No key vertex found for RegistryObject[id={}]", registryObjectId);
			return null;
		}

		// Remove DRAFT records if origin is PUBLISHED
		if(registryObjectVertex.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
			Predicate<Vertex> isRecord = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
			Predicate<Vertex> notPublishedStatus = v -> !v.getStatus().equals(registryObjectVertex.getStatus());
			sameAsNodeCluster.removeIf(isRecord.and(notPublishedStatus));
		}else{
			// remove the PUBLISHED record that the DRAFT is a copy of
			Collection<Vertex> publishedVersion =  getAltStatusRecord(registryObjectVertex, Vertex.Status.PUBLISHED.name());
			sameAsNodeCluster.removeAll(publishedVersion);
		}

		RecordState state = new RecordState();
		state.setRegistryObjectId(registryObjectId);
		state.setRegistryObjectKey(keyVertex.getIdentifier());
		state.setOrigin(registryObjectVertex);
		state.setTitle(registryObjectVertex.getTitle());
		state.setRegistryObjectClass(registryObjectVertex.getObjectClass());
		state.setRegistryObjectType(registryObjectVertex.getObjectType());
		state.setDataSourceId(registryObjectVertex.getDataSourceId());
		state.setStatus(registryObjectVertex.getStatus());
		log.debug("Got the Status {}", registryObjectVertex.getStatus());
		state.setGroup(registryObjectVertex.getGroup());
		state.setIdentical(sameAsNodeCluster);
		Collection<Vertex> vIdentifiers = getSameAs(registryObjectVertex.getIdentifier(),
				registryObjectVertex.getIdentifierType());
		// Remove ID and Key Identifiers before adding them to the identifiers state
		vIdentifiers.removeIf(v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)
				|| v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE));

		state.setIdentifiers(vIdentifiers);

		// outbound
		Collection<Relationship> outbounds = getDuplicateOutboundRelationships(registryObjectVertex.getIdentifier(),
				registryObjectVertex.getIdentifierType());

		// swap the origin to the vertex for all the outbounds
		outbounds = outbounds.stream().peek(relationship -> relationship.setFrom(registryObjectVertex))
				.collect(Collectors.toSet());

		state.setOutbounds(outbounds);

		return state;
	}

	/**
	 * Sets all ro:key Vertex that is not connected to a RegistryObject to "Terminated".
	 * Terminated nodes are then easily excluded by label for any path finding algorithm
	 */
	public void setRegistryObjectKeyNodeTerminated() {
		String cypherQuery = "MATCH (n:Vertex {identifierType:\"ro:key\"})\n"
				+ "WHERE NOT exists((n)-[:isSameAs]-(:RegistryObject)) SET n:Terminated;";
		ResultSummary resultSummary = neo4jClient.query(cypherQuery).run();
		log.debug("Terminate ro:key nodes ResultSummary[{}]", resultSummary.counters());
	}

	/**
	 * When a RegistryObject is reinstated (deleted and put back), the isSameAs relation
	 * between the ro:key Vertex and the RegistryObject vertex is then re-establish. This
	 * method will remove the "Terminated" label from the ro:key Vertex and thus enable
	 * path finding algorithm to the RegistryObject vertex again
	 */
	public void reinstateTerminatedNodes() {
		String cypherQuery = "MATCH (n:Vertex {identifierType:\"ro:key\"})-[:isSameAs]-(:RegistryObject)\n"
				+ "REMOVE n:Terminated;";
		ResultSummary resultSummary = neo4jClient.query(cypherQuery).run();
		log.debug("Reinstate ro:key nodes ResultSummary[{}]", resultSummary.counters());
	}

	/**
	 * Stream GrantsNetwork child collection from any node in the path
	 * @param from the Vertex to starts from
	 * @return a {@link Stream} of {@link Vertex} of child level Collection RegistryObject
	 * Vertex
	 */
	@Transactional(readOnly = true)
	public Stream<Vertex> streamChildCollection(Vertex from) {
		if(from == null){
			return null;
		}
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|hasPart>|outputs>|hasOutput>|isFunderOf>",
				"collection");
	}

	/**
	 * Stream GrantsNetwork child activity from any node in the path
	 * @param from the Vertex to starts from
	 * @return a {@link Stream} of {@link Vertex} of child level Activity RegistryObject
	 * Vertex
	 */
	@Transactional(readOnly = true)
	public Stream<Vertex> streamChildActivity(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|hasPart>", "activity");
	}

	/**
	 * Stream GrantsNetwork parent party from any node in the path. Used for finding the
	 * funder
	 * @param from the Vertex to starts from
	 * @return a {@link Stream} of {@link Vertex} of parent level Party RegistryObject
	 * Vertex
	 */
	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentParty(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isPartOf>|isOutputOf>|isFundedBy>",
				"party");
	}

	/**
	 * Stream GrantsNetwork parent activity from any node in the path
	 * @param from the Vertex to starts from
	 * @return a {@link Stream} of {@link Vertex} of parent level Activity RegistryObject
	 * Vertex
	 */
	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentActivity(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isPartOf>", "activity");
	}

	/**
	 * Stream GrantsNetwork parent collection from any node in the path
	 * @param from the Vertex to starts from
	 * @return a {@link Stream} of {@link Vertex} of parent level Collection
	 * RegistryObject Vertex
	 */
	@Transactional(readOnly = true)
	public Stream<Vertex> streamParentCollection(Vertex from) {
		return vertexRepository.streamSpanningTreeFromId(from.getIdentifier(), "isSameAs|isPartOf>", "collection");
	}

	/**
	 * Obtain a Graph of the {@link Vertex} of all immediate relationships
	 *
	 * Should include duplicate relationships
	 * @param from the {@link Vertex} at the center of all the relationships
	 * @param excludeRelationTypes the list of relationTypes to exclude (used for
	 * clustering capabilities)
	 * @return a {@link Graph} containing the current node and the immediate relationships
	 */
	public Graph getRegistryObjectGraph(Vertex from, List<String> excludeRelationTypes) {

		// add origin node
		Graph graph = new Graph();
		graph.addVertex(from);
		String draftFilter = "";
		// add direct relationships

		if(from.getStatus() == null || from.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			draftFilter = " labelFilter: '-DRAFT',\n";
		}

		StringBuilder cypherQuery = new StringBuilder("MATCH (origin:Vertex {identifier: '" + from.getIdentifier()
						+ "', identifierType: '" + from.getIdentifierType() + "'})\n"
						+ " CALL apoc.path.subgraphNodes(origin, {\n"
						+ " relationshipFilter: 'isSameAs',\n"
						+ draftFilter
						+ " minLevel: 0,\n"
						+ " maxLevel: 10\n"
						+ " })\n"
						+ " YIELD node\n"
						+ " WITH collect(origin) + collect(node) as complete\n"
				        + " UNWIND complete as from\n"
						+ " WITH distinct from\n"
						+ " MATCH (from)-[r]->(to)\n");
		cypherQuery.append("WHERE type(r) <> \"isSameAs\"\n");
		for (String relationType : excludeRelationTypes) {
			cypherQuery.append("AND type(r) <> \"").append(relationType).append("\"\n");
		}
		cypherQuery.append("RETURN from, to, collect(r) as relations;");
		Collection<Relationship> relationships = getRelationships(cypherQuery.toString());

		relationships.forEach(relationship -> {

			Vertex to = relationship.getTo();

			// obtain target-duplicates, this will bring resolved Vertices of ro:key
			// relationships as well as duplicated relationships
			Collection<Vertex> sameAsNodeCluster = getSameAs(to.getIdentifier(), to.getIdentifierType());


			Collection<Vertex> toRelatedObjects = sameAsNodeCluster.stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());

			if(from.getStatus() == null || from.getStatus().equals(Vertex.Status.PUBLISHED.name()) || toRelatedObjects.size() > 1){
				toRelatedObjects = sameAsNodeCluster.stream()
						.filter(vertex -> vertex.hasLabel(Vertex.Label.PUBLISHED)).collect(Collectors.toList());
			}

			if (toRelatedObjects.size() > 0) {
				// is a relatedObject relation
				toRelatedObjects.forEach(toRelatedObject -> {

					graph.addVertex(toRelatedObject);
					relationship.getRelations().forEach(relation -> graph
							.addEdge(new Edge(from, toRelatedObject, relation.getType(), relation.getId())));
				});
			}
			else if (!to.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)
					&& ! relationship.getRelations().stream()
							.anyMatch(relation -> relation.getType().equals(RIFCSGraphProvider.RELATION_SAME_AS))) {
				// does not resolve to registryObject it's a relatedInfo relation
				log.trace("Does not resolve to any relatedObject. Index as RelatedInfo");
				graph.addVertex(to);
				relationship.getRelations()
						.forEach(relation -> graph.addEdge(new Edge(from, to, relation.getType(), relation.getId())));
			}
		});

		return removeDanglingVertices(graph);
	}

	/**
	 * obtaining a set of graph and limit the target nodes to a set of IDs
	 * @param from the {@link Vertex} origin of the graph
	 * @param validTargetIdentifiers a list of identifiers
	 * @return the resulting {@link Graph}
	 */
	public Graph getRegistryObjectGraphWithTargetNodes(Vertex from, List<String> validTargetIdentifiers) {
		// add origin node
		Graph graph = new Graph();
		graph.addVertex(from);
		String draftFilter = "";

		if(from.getStatus() != null && from.getStatus().equals(Vertex.Status.DRAFT.name())){
				Collection<Vertex> publishedVersions = getAltStatusRecord(from, Vertex.Status.PUBLISHED.name());
				publishedVersions.forEach(v ->
				{
					log.debug("removing published version: {}", v.getIdentifier());
					validTargetIdentifiers.remove(v.getIdentifier());
				});
		}else{
			draftFilter = "labelFilter: '-DRAFT',";
		}

		String cypherQuery = String.format("MATCH (origin:Vertex {identifier: \"%s\", identifierType: \"%s\"})\n"
				+ " CALL apoc.path.subgraphNodes(origin, {\n"
				+ " relationshipFilter: 'isSameAs', %s\n"
				+ " minLevel: 0,\n"
				+ " maxLevel: 10\n"
				+ " })\n"
				+ " YIELD node\n"
				+ " WITH collect(origin) + collect(node) as complete\n"
				+ " UNWIND complete as from\n"
				+ " WITH distinct from\n"
				+ " MATCH (from)-[r]->(to)\n", from.getIdentifier(), from.getIdentifierType(), draftFilter);
		if (validTargetIdentifiers.size() > 0) {
			cypherQuery += "WHERE to.identifier IN [" + validTargetIdentifiers.stream()
					.map(s -> "\"" + s + "\"")
					.collect(Collectors.joining(", ")) +"]\n";
		}
		cypherQuery += "RETURN from, to, collect(r) as relations;";
		log.debug("getRegistryObjectGraphWithTargetNodes cypher: {}", cypherQuery);

		Collection<Relationship> relationships = getRelationships(cypherQuery.toString());

		relationships.forEach(relationship -> {

			Vertex to = relationship.getTo();

			// obtain target-duplicates, this will bring resolved Vertices of ro:key
			// relationships as well as duplicated relationships
			Collection<Vertex> sameAsNodeCluster = getSameAs(to.getIdentifier(), to.getIdentifierType());
			Collection<Vertex> toRelatedObjects = sameAsNodeCluster.stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());

			if (toRelatedObjects.size() > 0) {
				// is a relatedObject relation
				toRelatedObjects.forEach(toRelatedObject -> {
					graph.addVertex(toRelatedObject);
					relationship.getRelations().forEach(relation -> graph
							.addEdge(new Edge(from, toRelatedObject, relation.getType(), relation.getId())));
				});
			}
			else if (!to.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)
					&& ! relationship.getRelations().stream()
					.anyMatch(relation -> relation.getType().equals(RIFCSGraphProvider.RELATION_SAME_AS))) {
				// does not resolve to registryObject it's a relatedInfo relation
				log.trace("Does not resolve to any relatedObject. Index as RelatedInfo");
				graph.addVertex(to);
				relationship.getRelations()
						.forEach(relation -> graph.addEdge(new Edge(from, to, relation.getType(), relation.getId())));
			}
		});

		return removeDanglingVertices(graph);

	}

	/**
	 * Obtain a {@link Graph} for a set of paths from a cypher query
	 * @param cypherQuery the String cypherQuery to search on
	 * @return the resulting {@link Graph}
	 */
	public Graph getGraphsFromPaths(String cypherQuery) {
		log.debug("getGraphsFromPaths cypher: {}", cypherQuery);
		Collection<Graph> graphs = neo4jClient.query(cypherQuery)
				.fetchAs(Graph.class).mappedBy((typeSystem, record) -> {
					Graph graph = new Graph();
					Path path = record.get("path").asPath();
					path.nodes().forEach(node -> graph.addVertex(vertexMapper.getConverter().convert(node)));
					path.relationships().forEach(relationship -> {
						long startId = relationship.startNodeId();
						long endId = relationship.endNodeId();

						String relationType = relationship.type();

						Vertex from = graph.getVertices().stream().filter(vertex -> vertex.getId().equals(startId))
								.findFirst().orElse(null);
						Vertex to = graph.getVertices().stream().filter(vertex -> vertex.getId().equals(endId))
								.findFirst().orElse(null);

						// attempt to resolve any identifier to a registryObject
						if (from != null && !from.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE)) {
							Vertex resolvedRegistryObjectVertex = getDuplicateRegistryObject(from).stream()
									.findFirst().orElse(null);
							if (resolvedRegistryObjectVertex != null) {
								from = resolvedRegistryObjectVertex;
							}
						}
						if (to != null && !to.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE)) {
							Vertex resolvedRegistryObjectVertex = getDuplicateRegistryObject(to).stream()
									.findFirst().orElse(null);
							if (resolvedRegistryObjectVertex != null) {
								to = resolvedRegistryObjectVertex;
							}
						}

						if (from != null && to != null) {
							graph.addEdge(new Edge(from, to, relationType, relationship.id()));
						}
					});
					return graph;
				}).all();

		// merge all graphs into a single graph
		Graph mergedGraph = new Graph();
		graphs.forEach(graph -> {
			graph.getVertices().forEach(mergedGraph::addVertex);
			graph.getEdges().forEach(mergedGraph::addEdge);
		});

		collapseGraph(mergedGraph);
		removeDanglingVertices(mergedGraph);

		return mergedGraph;
	}

	/**
	 * Obtain a graph detailing the relationship path downwards with GrantsNetwork pathing
	 * @param origin the {@link Vertex} to start the path from
	 * @param excludedTypes the list of exluded relation type
	 * @return the {@link} Graph containing the GrantsNetwork nodes downwards, collapsed
	 */
	public Graph getGrantsNetworkDownwards(Vertex origin, List<String> excludedTypes) {
		String cypherQuery = "";

		List<String> relationTypes = Arrays.asList("hasPart", "hasOutput", "isFunderOf");

		// filter out the relationTypes that should be excluded
		relationTypes = relationTypes.stream().filter(relationType -> {
			return !excludedTypes.contains(relationType);
		}).collect(Collectors.toList());

		// add > to each relationTypes
		relationTypes = relationTypes.stream().map(relationType -> {
			return relationType + ">";
		}).collect(Collectors.toList());

		// add isSameAs
		relationTypes.add(0, "isSameAs");

		// join relationTypes
		// expected output would be 'isSameAs|hasPart>|hasOutput>|isFunderOf>'
		String relationshipFilter = String.join("|", relationTypes);

		if(origin.getStatus() != null && origin.getStatus().equals(Vertex.Status.DRAFT.name())){
			cypherQuery = "MATCH (origin:RegistryObject {identifier: '"+origin.getIdentifier()+"'}) CALL apoc.path.spanningTree(origin, {\n"
					+ " relationshipFilter: '"+relationshipFilter+"', minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
					+ "}) YIELD path RETURN path LIMIT 100;";
		}else {
			cypherQuery = "MATCH (origin:RegistryObject {identifier: '"+origin.getIdentifier()+"'}) CALL apoc.path.spanningTree(origin, {\n"
					+ " relationshipFilter: '"+relationshipFilter+"', minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
					+ ", labelFilter: '-DRAFT'}) YIELD path RETURN path LIMIT 100;";
		}
		return getGraphsFromPaths(cypherQuery);
	}

	/**
	 * Obtain a graph detailing the relationship path going upwards with GrantsNetwork
	 * pathing
	 * @param origin the {@link Vertex} to start the path from
	 * @param excludedTypes the list of exluded relation type
	 * @return the {@link} Graph containing the GrantsNetwork nodes upward, collapsed
	 */
	public Graph getGrantsNetworkGraphUpwards(Vertex origin, List<String> excludedTypes) {
		String cypherQuery = "";

		List<String> relationTypes = Arrays.asList("isPartOf", "isOutputOf", "isFundedBy");

		// filter out the relationTypes that should be excluded
		relationTypes = relationTypes.stream().filter(relationType -> {
			return !excludedTypes.contains(relationType);
		}).collect(Collectors.toList());

		// add > to each relationTypes
		relationTypes = relationTypes.stream().map(relationType -> {
			return relationType + ">";
		}).collect(Collectors.toList());

		// add isSameAs
		relationTypes.add(0, "isSameAs");

		// join relationTypes
		// expected output would be 'isSameAs|isPartOf>|isOutputOf>|isFundedBy>'
		String relationshipFilter = String.join("|", relationTypes);

		if(origin.getIdentifierType().equals(RIFCS_ID_IDENTIFIER_TYPE) && origin.getStatus().equals(Vertex.Status.DRAFT.name())){
			cypherQuery = "MATCH (origin:RegistryObject {identifier: '" + origin.getIdentifier() + "'}) CALL apoc.path.spanningTree(origin, {\n"
					+ " relationshipFilter: '"+relationshipFilter+"', minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
					+ "}) YIELD path RETURN path LIMIT 100;";
		}else {
			cypherQuery = "MATCH (origin:RegistryObject {identifier: '" + origin.getIdentifier() + "'}) CALL apoc.path.spanningTree(origin, {\n"
					+ " relationshipFilter: '"+relationshipFilter+"', minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
					+ ", labelFilter: '-DRAFT'}) YIELD path RETURN path LIMIT 100;";
		}
		log.debug("getGrantsNetworkGraphUpwards cypher: {}", cypherQuery);

		return getGraphsFromPaths(cypherQuery);
	}

	/**
	 * Collapse the given graph
	 *
	 * Collapsing a graph means joining the ro:key and the ro:id nodes together,
	 * effectively merging their relationships
	 * @param result a {@link Graph} that needs collapsing
	 * @return the merged and collapsed {@link Graph}
	 */
	public Graph collapseGraph(Graph result) {

		// collect identical pairs in the existing graph
		Map<Object, Object> pairs = new HashMap<>();
		result.getEdges().forEach(edge -> {
			String relationType = edge.getType();
			if (relationType.equals(RIFCSGraphProvider.RELATION_SAME_AS)) {
				pairs.put(edge.getFrom().getId(), edge.getTo().getId());
				pairs.put(edge.getTo().getId(), edge.getFrom().getId());
			}
		});

		// if there's no identical pair, no need for collapsing
		if (pairs.size() == 0) {
			return result;
		}

		// remove isSameAs edges
		result.setEdges(
				result.getEdges().stream().filter(edge -> !edge.getType().equals(RIFCSGraphProvider.RELATION_SAME_AS))
						.collect(Collectors.toList()));

		// swap around the edges using the pairs
		result.getEdges().forEach(edge -> {
			if (edge.getFrom().getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)) {
				// try to obtain the existing vertex in the graph if it's presented
				if (pairs.containsKey(edge.getFrom().getId())) {
					Long swappedId = (Long) pairs.get(edge.getFrom().getId());
					edge.setFrom(result.getVertices().stream().filter(vertex -> vertex.getId().equals(swappedId))
							.findFirst().orElse(null));
				}
				else {
					// if we can't find the vertex in the existing graph, obtain the
					// vertex from the database
					Vertex resolvedFromVertex = getRegistryObjectByKeyVertex(edge.getFrom());
					if (resolvedFromVertex != null) {
						edge.setFrom(resolvedFromVertex);
					}
					else {
						log.warn(
								"Failed to obtain resolved vertex for Vertex[identifier={}] for Edge[from.id={}, to.id={}, type={}]",
								edge.getFrom().getIdentifier(), edge.getFrom().getId(), edge.getTo().getId(),
								edge.getType());
					}
				}
			}

			// similar with from
			if (edge.getTo().getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)) {
				if (pairs.containsKey(edge.getTo().getId())) {
					Long swappedId = (Long) pairs.get(edge.getTo().getId());
					edge.setTo(result.getVertices().stream().filter(vertex -> vertex.getId().equals(swappedId))
							.findFirst().orElse(null));
				}
				else {
					Vertex resolvedToVertex = getRegistryObjectByKeyVertex(edge.getTo());
					if (resolvedToVertex != null) {
						edge.setTo(resolvedToVertex);
					}
					else {
						log.warn(
								"Failed to obtain resolved vertex for Vertex[identifier={}] for Edge[from.id={}, to.id={}, type={}]",
								edge.getTo().getIdentifier(), edge.getFrom().getId(), edge.getTo().getId(),
								edge.getType());
					}
				}
			}
		});

		// remove the ro:key vertices
		result.setVertices(result.getVertices().stream()
				.filter(vertex -> !vertex.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE))
				.collect(Collectors.toList()));

		return result;
	}

	/**
	 * Resolve 'ro:key' Vertex to the RegistryObject Vertex
	 * todo refactor to use getRegistryObjectByKey instead
	 * @param keyVertex the "ro:key" Vertex
	 * @return the {@link Vertex} resolved, or null if not found
	 */
	public Vertex getRegistryObjectByKeyVertex(Vertex keyVertex) {
		String cypherQuery = "MATCH (k:Identifier {identifier: $identifier, identifierType: 'ro:key'})-[:isSameAs]-(n:RegistryObject{status:'PUBLISHED'}) RETURN n;";
		return neo4jClient.query(cypherQuery).bind(keyVertex.getIdentifier()).to("identifier").fetchAs(Vertex.class)
				.mappedBy((typeSystem, record) -> {
					Node node = record.get("n").asNode();
					return vertexMapper.getConverter().convert(node);
				}).one().orElse(null);
	}

	/**
	 * Resolve 'ro:key' Vertex to the RegistryObject Vertex
	 * @param key the "ro:key" as string
	 * @return the {@link Vertex} resolved, or null if not found
	 */
	public Vertex getRegistryObjectByKey(String key) {
		String cypherQuery = "MATCH (k:Vertex {identifier: $identifier, identifierType: $identifierType})-[:isSameAs]-(n:RegistryObject{status:'PUBLISHED'}) RETURN n;";
		return neo4jClient.query(cypherQuery)
				.bind(key).to("identifier")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("identifierType")
				.fetchAs(Vertex.class)
				.mappedBy((typeSystem, record) -> {
					Node node = record.get("n").asNode();
					return vertexMapper.getConverter().convert(node);
				}).one().orElse(null);
	}

	/**
	 * Removes all vertices that doesn't have an edge connection from a given graph
	 * @param graph the {@link Graph} to clean up
	 * @return the cleaned up {@link Graph}
	 */
	public Graph removeDanglingVertices(Graph graph) {
		List<Long> validIds = new ArrayList<>();
		graph.getEdges().forEach(edge -> {
			validIds.add(edge.getFrom().getId());
			validIds.add(edge.getTo().getId());
		});
		graph.setVertices(graph.getVertices().stream().filter(vertex -> validIds.contains(vertex.getId()))
				.collect(Collectors.toList()));
		return graph;
	}

	/**
	 * Obtain the Graph that contains all the relationships between a list of given
	 * vertices
	 * @param vertices a list of {@link Vertex} to find any immediate relations between
	 * @return a {@link Graph}
	 */
	public Graph getGraphBetweenVertices(List<Vertex> vertices) {
		List<String> ids = vertices.stream().map(Vertex::getIdentifier).collect(Collectors.toList());
		Graph graph = new Graph();
		vertices.forEach(vertex -> {
			log.debug("obtaining subGraph for Vertex[identifier={}]", vertex.getIdentifier());

			// limit the subgraph to the set of current ids
			Graph subGraph = getRegistryObjectGraphWithTargetNodes(vertex, ids);

			// sanity check and cleaning
			subGraph.setVertices(subGraph.getVertices().stream().filter(v -> ids.contains(v.getIdentifier()))
					.collect(Collectors.toList()));
			subGraph.setEdges(subGraph.getEdges().stream().filter(
					edge -> ids.contains(edge.getFrom().getIdentifier()) && ids.contains(edge.getTo().getIdentifier()))
					.collect(Collectors.toList()));
			graph.mergeGraph(subGraph);

			log.debug("graphMerged for Vertex[identifier={}]", vertex.getIdentifier());
		});
		return graph;
	}

	/**
	 * Get the "facets" grouped by relationType, objectClass and objectType from a given
	 * {@link Vertex}. This functionality is primarily used for clustering detection
	 * @param from the {@link Vertex} to query immediate relationships from
	 * @return a {@link Collection} of {@link RelationTypeGroup} that contains the
	 * grouping
	 */
	public Collection<RelationTypeGroup> getRelationTypeGrouping(Vertex from) {
		String cypherQuery = "MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType})\n"
				+ "OPTIONAL MATCH (origin)-[:isSameAs*1..5]-(duplicates)\n"
				+ "WITH collect(origin) + collect(duplicates) as identical\n" + "UNWIND identical as from\n"
				+ "WITH distinct from\n"
				+ "MATCH (from)-[r]->(to)\n"
				+ "WHERE type(r) <> 'isSameAs' AND NOT to:Terminated\n"
				+ "RETURN labels(to) as labels, TYPE(r) as relation, to.objectClass as class, to.objectType as type, count(to) as total;";
		return neo4jClient.query(cypherQuery).bind(from.getIdentifier()).to("identifier").bind(from.getIdentifierType())
				.to("identifierType").fetchAs(RelationTypeGroup.class).mappedBy((typeSystem, record) -> {
					RelationTypeGroup group = new RelationTypeGroup();
					group.setLabels(record.get("labels").asList().stream().map(Object::toString)
							.map(Vertex.Label::valueOf).collect(Collectors.toList()));
					group.setRelation(record.get("relation").asString());
					group.setObjectClass(record.get("class").asString());
					group.setObjectType(record.get("type").asString());
					group.setCount(record.get("total").asInt());
					return group;
				}).all();
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamRegistryObjectFromDataSource(String dataSourceId) {
		return vertexRepository.streamRegistryObjectByDataSourceId(dataSourceId);
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamRegistryObjectFromDataSource(String dataSourceId, String objectClass) {
		return vertexRepository.streamRegistryObjectByDataSourceAndClass(dataSourceId, objectClass);
	}

	@Transactional(readOnly = true)
	public Stream<Vertex> streamVertexByIdentifierType(String identifierType) {
		return vertexRepository.streamVertexByIdentifierType(identifierType);
	}

	public void deletePrimaryKeyEdge(String key) {
		log.debug("Deleting PrimaryLink edges Vertex[key={}]", key);
		String cypherQuery = "MATCH (n:Vertex {identifier: $key, identifierType: 'ro:key'})-[r]-() WHERE r.origin = $origin DELETE r;";
		neo4jClient.query(cypherQuery).bind(key).to("key").bind(RIFCSGraphProvider.ORIGIN_PRIMARY_LINK).to("origin").run();
	}

	public Graph getNestedCollectionParents(Vertex origin) {

		String draftFilter = "";
		if(origin.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			draftFilter = ", labelFilter: '-DRAFT'";
		}

		String cypherQuery = "MATCH (origin:RegistryObject {identifier: '"+origin.getIdentifier()+"'}) CALL apoc.path.spanningTree(origin, {\n"
				+ " relationshipFilter: 'isSameAs|isPartOf>'"+draftFilter+", minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
				+ "}) YIELD path RETURN path LIMIT 100;";

		return getGraphsFromPaths(cypherQuery);
	}

	public Collection<Relationship> getNestedCollectionChildren(Vertex origin, Integer limit, Integer skip, List<String> excludeIDs) {
		String draftFilter = "";
		if(origin.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			draftFilter = ", labelFilter: '-DRAFT',\n";
		}

		String cypherQuery = "MATCH (origin:Vertex {identifier:'" + origin.getIdentifier() +"', identifierType:'"+origin.getIdentifierType()+"'})\n"
				+ " CALL apoc.path.subgraphNodes(origin, {\n"
				+ " relationshipFilter: 'isSameAs'\n"
				+ draftFilter
				+ " minLevel: 0,\n"
				+ " maxLevel: 10\n"
				+ " })\n"
				+ " YIELD node\n"
				+ " WITH collect(origin) + collect(node) as complete\n"
				+ " UNWIND complete as from\n"
				+ " WITH distinct from MATCH (from)-[r:hasPart]->(to) WHERE (to.identifierType = \"ro:id\" OR EXISTS((to)-[:isSameAs]-({identifierType:\"ro:id\"}))"
				+ " AND to.objectClass = 'collection')\n";

		if (excludeIDs.size() > 0) {
			String notIn = excludeIDs.stream().map(id -> {
				return "\"" + id + "\"";
			}).collect(Collectors.joining(",", "[", "]"));
			cypherQuery += "AND NOT to.identifier IN "+notIn+"\n";
		}
		cypherQuery += "RETURN from, to, collect(r) as relations ORDER BY toLower(to.title) ASC SKIP "+skip+" LIMIT "+limit+";";
		return getRelationships(cypherQuery);
	}

	public int getNestedCollectionChildrenCount(String registryObjectId, List<String> excludeIDs) {
		String cypherQuery = "MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType}) OPTIONAL MATCH (origin)-[:isSameAs*1..5]-(duplicates)\n" +
				"WITH collect(origin) + collect(duplicates) as identical UNWIND identical as from\n" +
				"WITH distinct from MATCH (from)-[r:hasPart]->(to) WHERE (to.identifierType = \"ro:id\" OR EXISTS((to)-[:isSameAs]-({identifierType:\"ro:id\"})))\n";

		if (excludeIDs.size() > 0) {
			String notIn = excludeIDs.stream().map(id -> {
				return "\"" + id + "\"";
			}).collect(Collectors.joining(",", "[", "]"));
			cypherQuery += "AND NOT to.identifier IN "+notIn+"\n";
		}

		cypherQuery += "RETURN count(r) as count;";
		return neo4jClient.query(cypherQuery).bind(registryObjectId).to("identifier").bind("ro:id")
				.to("identifierType").fetchAs(Integer.class).one().get();
	}

	public Graph getLocalGraph(Vertex origin) {
		String cypherQuery = "PROFILE MATCH path=(n:Vertex)-[r]-(n2) WHERE id(n) = $id RETURN path;";
//		log.debug("getLocalGraph cypher: {}", cypherQuery);

		Collection<Graph> graphs = neo4jClient.query(cypherQuery)
				.bind(origin.getId()).to("id")
				.fetchAs(Graph.class).mappedBy((typeSystem, record) -> {
					Graph graph = new Graph();
					Path path = record.get("path").asPath();
					path.nodes().forEach(node -> graph.addVertex(vertexMapper.getConverter().convert(node)));
					path.relationships().forEach(relationship -> {
						long startId = relationship.startNodeId();
						long endId = relationship.endNodeId();
						String relationType = relationship.type();
						Vertex from = graph.getVertices().stream().filter(vertex -> vertex.getId().equals(startId))
								.findFirst().orElse(null);
						Vertex to = graph.getVertices().stream().filter(vertex -> vertex.getId().equals(endId))
								.findFirst().orElse(null);

						// todo refactor to EdgeRelationshipMapper
						if (from != null && to != null) {
							Edge edge = new Edge(from, to, relationType, relationship.id());
							if (!relationship.get("description").isNull()) {
								edge.setDescription(relationship.get("description").asString());
							}
							if (!relationship.get("url").isNull()) {
								edge.setUrl(relationship.get("url").asString());
							}
							if (!relationship.get("origin").isNull()) {
								edge.setOrigin(relationship.get("origin").asString());
							}
							if (!relationship.get("reverse").isNull()) {
								edge.setReverse(relationship.get("reverse").asBoolean());
							}
							if (!relationship.get("duplicate").isNull()) {
								edge.setDuplicate(relationship.get("duplicate").asBoolean());
							}
							if (!relationship.get("public").isNull()) {
								edge.setPublic(relationship.get("public").asBoolean());
							}
							if (!relationship.get("internal").isNull()) {
								edge.setInternal(relationship.get("internal").asBoolean());
							}
							graph.addEdge(edge);
						}
					});
					return graph;
				}).all();

		// merge all graphs into a single graph
		Graph mergedGraph = new Graph();

		// origin should be in the graph regardless (even without relationships)
		mergedGraph.addVertex(origin);

		graphs.forEach(graph -> {
			graph.getVertices().forEach(mergedGraph::addVertex);
			graph.getEdges().forEach(mergedGraph::addEdge);
		});

		return mergedGraph;
	}

	public Page<Vertex> getAllRegistryObjects(Pageable pageable) {
		Page<Vertex> results = vertexRepository.getVertexByIdentifierTypeAndStatus(RIFCS_ID_IDENTIFIER_TYPE, "PUBLISHED", pageable);
		return results;
	}

}
