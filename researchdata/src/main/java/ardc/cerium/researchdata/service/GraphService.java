package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.model.Edge;
import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.RelationDocument;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.repository.VertexRepository;
import ardc.cerium.researchdata.util.Neo4jClientBiFunctionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;

@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    @Autowired
    VertexRepository vertexRepository;

    @Autowired
    Neo4jClient neo4jClient;

    public void ingestGraph(Graph graph) {
        graph.getVertices().forEach(this::ingestVertex);
        log.info("Ingested {} vertices", graph.getVertices().size());

        graph.getEdges().forEach(this::ingestEdge);
        log.info("Ingested {} edges", graph.getEdges().size());
    }

    public void ingestVertex(Vertex vertex) {
        // todo update the vertex
        if (!vertexRepository.existsVertexByIdentifierAndIdentifierType(vertex.getIdentifier(),
                vertex.getIdentifierType())) {
            vertexRepository.save(vertex);
        }
    }

    public Collection<RelationDocument> getRelationships(String identifier, String identifierType, int limit, int offset) {
        return neo4jClient
                .query("MATCH (origin:Vertex {identifier: $identifier, identifierType: $identifierType})\n" +
                        "WITH origin MATCH (origin)-[:isSameAs*1..5]-(duplicates) \n" +
                        "WITH collect(origin) + collect(duplicates) as identicals\n" +
                        "WITH identicals MATCH p=(n)-[r]-() WHERE n in identicals AND type(r) <> 'isSameAs'\n" +
                        "RETURN p SKIP $skip LIMIT $limit")
                .bind(identifier).to("identifier")
                .bind(identifierType).to("identifierType")
                .bind(limit).to("limit")
                .bind(offset).to("skip")
                .fetchAs(RelationDocument.class)
                .mappedBy(((typeSystem, record) -> Neo4jClientBiFunctionHelper.pathToRelationDocument(record, "p")))
                .all();
    }

    public void ingestEdge(Edge edge) {
        // building a cypher string and just run it
        String cypher = String.format(
                "MATCH (from:Vertex {identifier: '%s', identifierType: '%s'}) \n" + "WITH from \n"
                        + "MATCH (to:Vertex {identifier: '%s', identifierType: '%s'}) \n" + "WITH from, to\n"
                        + "MERGE (from)-[r:%s]->(to) RETURN type(r);",
                edge.getFrom().getIdentifier(), edge.getFrom().getIdentifierType(), edge.getTo().getIdentifier(),
                edge.getTo().getIdentifierType(), edge.getType());

        neo4jClient.query(cypher).run();

        // binding Relationship doesn't work
//		neo4jClient.query("MATCH (from:Vertex {identifier: $fromID, identifierType: $fromIDType}) WITH from \n"
//				+ "MATCH (to:Vertex {identifier: $toID, identifierType: $toIDType}) WITH from, to\n"
//				+ "MERGE (from)-[r:$relationType]->(to) RETURN type(r);")
//				.bind(edge.getFrom().getIdentifier()).to("fromID")
//				.bind(edge.getFrom().getIdentifierType()).to("fromIDType")
//				.bind(edge.getTo().getIdentifier()).to("toID")
//				.bind(edge.getTo().getIdentifierType()).to("toIDType")
//				.bind(edge.getType()).to("relationType")
//				.run();
    }

}
