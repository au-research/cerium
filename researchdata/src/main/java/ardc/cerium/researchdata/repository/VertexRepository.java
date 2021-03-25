package ardc.cerium.researchdata.repository;

import ardc.cerium.researchdata.model.Vertex;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface VertexRepository extends Neo4jRepository<Vertex, Long> {

	boolean existsVertexByIdentifierAndIdentifierType(String identifier, String type);

}
