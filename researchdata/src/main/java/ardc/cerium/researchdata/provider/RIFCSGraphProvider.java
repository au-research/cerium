package ardc.cerium.researchdata.provider;

import ardc.cerium.researchdata.model.Edge;
import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.rifcs.RIFCSParser;
import ardc.cerium.researchdata.rifcs.model.*;

import java.util.ArrayList;
import java.util.List;

public class RIFCSGraphProvider {

    public static final String RIFCS_KEY_IDENTIFIER_TYPE = "ro:key";

    public static final String RELATION_SAME_AS = "isSameAs";

    public Graph get(String xml) {
        RegistryObjects registryObjects = RIFCSParser.parse(xml);
        Graph graph = new Graph();

        if (registryObjects == null || registryObjects.getRegistryObjects().size() == 0) {
            // log something gone wrong with parsing here
            return graph;
        }

        registryObjects.getRegistryObjects().forEach(registryObject -> {
            // key is a vertex and is the originNode
            String key = registryObject.getKey();
            Vertex originNode = new Vertex(key, RIFCS_KEY_IDENTIFIER_TYPE);
            originNode.addLabel(Vertex.Label.RegistryObject);
            graph.addVertex(originNode);

            // every identifier is a vertex & isSameAs
            List<Identifier> identifiers = registryObject.getIdentifiers();
            if (identifiers != null && identifiers.size() > 0) {
                registryObject.getIdentifiers().forEach(identifier -> {
                    Vertex identifierNode = new Vertex(identifier.getValue(), identifier.getType());
                    identifierNode.addLabel(Vertex.Label.Identifier);
                    graph.addVertex(identifierNode);
                    graph.addEdge(new Edge(originNode, identifierNode, RELATION_SAME_AS));
                });
            }

            // every relatedObject is a vertex & edge
            List<RelatedObject> relatedObjects = registryObject.getRelatedObjects();
            if (relatedObjects != null && relatedObjects.size() > 0) {
                relatedObjects.forEach(relatedObject -> {
                    Vertex relatedObjectNode = new Vertex(relatedObject.getKey(), RIFCS_KEY_IDENTIFIER_TYPE);
                    relatedObjectNode.addLabel(Vertex.Label.RegistryObject);
                    graph.addVertex(relatedObjectNode);
                    relatedObject.getRelation().forEach(relation -> {
                        Edge edge = new Edge(originNode, relatedObjectNode, relation.getType());
                        graph.addEdge(edge);
                    });
                });
            }

            // every relatedInfo is a vertex & edge
            List<RelatedInfo> relatedInfos = registryObject.getRelatedInfos();
            if (relatedInfos != null && relatedInfos.size() > 0) {
                relatedInfos.forEach(relatedInfo -> {

                    // relatedInfos can have many identifiers
                    List<Identifier> relatedInfoIdentifiers = relatedInfo.getIdentifiers() != null
                            ? relatedInfo.getIdentifiers() : new ArrayList<>();
                    List<Relation> relatedInfoRelations = relatedInfo.getRelation() != null ? relatedInfo.getRelation()
                            : new ArrayList<>();
                    relatedInfoIdentifiers.forEach(relatedInfoIdentifier -> {
                        Vertex relatedInfoNode = new Vertex(relatedInfoIdentifier.getValue(), relatedInfoIdentifier.getType());
                        relatedInfoNode.addLabel(Vertex.Label.Identifier);
                        graph.addVertex(relatedInfoNode);
                        relatedInfoRelations.forEach(relatedInfoRelation -> {
                            Edge edge = new Edge(originNode, relatedInfoNode, relatedInfoRelation.getType());
                            graph.addEdge(edge);
                        });
                    });
                });
            }

            // todo primary links
            // todo reverse links
        });


        return graph;
    }
}
