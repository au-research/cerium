package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.Vertex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Query("MATCH (n:Vertex {dataSourceId: $dataSourceId}) CALL { WITH n DETACH DELETE n } IN TRANSACTIONS OF 1000 ROWS;")
	void deleteByDataSourceId(@Param("dataSourceId")String dataSourceId);

	boolean existsVertexByIdentifierAndIdentifierType(String identifier, String type);

	Page<Vertex> getVertexByDataSourceId(String dataSourceId, Pageable pageable);

	@Query("MATCH (from:Vertex {identifier: $identifier, identifierType: $identifierType})-[:isSameAs*]-(to) RETURN to")
	Stream<Vertex> streamAllDuplicates(@Param("identifier") String identifier,
			@Param("identifierType") String identifierType);

	// @formatter:off
	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (n:RegistryObject {dataSourceId: $dataSourceId}) RETURN n;")
	Stream<Vertex> streamRegistryObjectByDataSourceId(@Param("dataSourceId") String dataSourceId);
	// @formatter:on

	// @formatter:off
	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (n:Vertex {identifierType: $identifierType}) RETURN n;")
	Stream<Vertex> streamVertexByIdentifierType(@Param("identifierType") String identifierType);
	// @formatter:on

	// @formatter:off
	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (n:RegistryObject {dataSourceId: $dataSourceId, objectClass: $objectClass}) RETURN n;")
	Stream<Vertex> streamRegistryObjectByDataSourceAndClass(@Param("dataSourceId") String dataSourceId, @Param("objectClass") String objectClass);
	// @formatter:on

	// @formatter:off
	@QueryHints(value = {
		@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
		@QueryHint(name = HINT_CACHEABLE, value = "false"),
		@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:Vertex {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "relationshipFilter: $filter, minLevel: 1, maxLevel: 100, labelFilter: '-Terminated'\n"
			+ "}) YIELD path WITH apoc.path.elements(path) AS elements\n" +
			"UNWIND range(0, size(elements)-2) AS index\n" +
			"WITH elements, index\n" +
			"WHERE index %2 = 0 AND elements[index+2].objectClass = $class\n" +
			"RETURN distinct elements[index+2] as n")
	Stream<Vertex> streamSpanningTreeFromId(@Param("identifier") String identifier, @Param("filter") String relationshipFilters, @Param("class") String classFilter);
	// @formatter:on

	Page<Vertex> getVertexByIdentifierTypeAndStatus(String identifierType, String status, Pageable pageable);

}
