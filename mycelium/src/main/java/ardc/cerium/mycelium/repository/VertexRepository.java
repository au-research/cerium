package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.Vertex;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.stream.Stream;

import static org.hibernate.annotations.QueryHints.READ_ONLY;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;

public interface VertexRepository extends Neo4jRepository<Vertex, Long> {

	boolean existsVertexByIdentifierAndIdentifierType(String identifier, String type);

	@Query("MATCH (from:Vertex {identifier: $identifier, identifierType: $identifierType})-[:isSameAs*]-(to) RETURN to")
	Stream<Vertex> streamAllDuplicates(@Param("identifier") String identifier,
			@Param("identifierType") String identifierType);

	// @formatter:off
	@QueryHints(value = {
		@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
		@QueryHint(name = HINT_CACHEABLE, value = "false"),
		@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "relationshipFilter: $filter, minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (n:RegistryObject {objectClass: $class})"
			+ "WHERE n IN targets AND n.identifier <> $identifier RETURN DISTINCT n")
	Stream<Vertex> streamSpanningTreeFromId(@Param("identifier") String identifier, @Param("filter") String relationshipFilters, @Param("class") String classFilter);
	// @formatter:on

}
