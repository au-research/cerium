package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.Vertex;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface VertexRepository extends Neo4jRepository<Vertex, Long> {

	boolean existsVertexByIdentifierAndIdentifierType(String identifier, String type);

}
