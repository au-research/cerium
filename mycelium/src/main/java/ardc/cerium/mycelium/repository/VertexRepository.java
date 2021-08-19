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

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|isPartOf>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (collection:RegistryObject {objectClass: 'collection'})"
			+ "WHERE collection IN targets AND collection.identifier <> $identifier RETURN DISTINCT collection")
	Stream<Vertex> streamParentCollections(@Param("identifier") String identifier);

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|hasPart>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (collection:RegistryObject {objectClass: 'collection'})"
			+ "WHERE collection IN targets AND collection.identifier <> $identifier RETURN DISTINCT collection")
	Stream<Vertex> streamChildCollections(@Param("identifier") String identifier);

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|isPartOf>|isOutputOf>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (activity:RegistryObject {objectClass: 'activity'})"
			+ "WHERE activity IN targets AND activity.identifier <> $identifier RETURN DISTINCT activity")
	Stream<Vertex> streamAllGrantsNetworkParentActivities(@Param("identifier") String identifier);

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|isFunderOf>|funds>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (activity:RegistryObject {objectClass: 'activity'})"
			+ "WHERE activity IN targets AND activity.identifier <> $identifier RETURN DISTINCT activity")
	Stream<Vertex> streamAllGrantsNetworkChildActivities(@Param("identifier") String identifier);

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|isFunderOf>|funds>|hasOutput>|hasPart>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (collection:RegistryObject {objectClass: 'collection'})"
			+ "WHERE collection IN targets AND collection.identifier <> $identifier RETURN DISTINCT collection")
	Stream<Vertex> streamAllGrantsNetworkChildCollections(@Param("identifier") String identifier);

	@QueryHints(value = {
			@QueryHint(name = HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = HINT_CACHEABLE, value = "false"),
			@QueryHint(name = READ_ONLY, value = "true")
	})
	@Query("MATCH (origin:RegistryObject {identifier: $identifier}) CALL apoc.path.spanningTree(origin, {\n"
			+ "    relationshipFilter: 'isSameAs|isPartOf>|isOutputOf>|isFundedBy>', minLevel: 1, maxLevel: 100\n"
			+ "}) YIELD path WITH nodes(path) as targets\n"
			+ "MATCH (party:RegistryObject {objectClass: 'party'})"
			+ "WHERE party IN targets AND party.identifier <> $identifier RETURN DISTINCT party")
	Stream<Vertex> streamAllGrantsNetworkParentParties(@Param("identifier") String identifier);

}
