package ardc.cerium.mycelium.provider;

import ardc.cerium.mycelium.client.RDARegistryClient;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import ardc.cerium.mycelium.rifcs.model.*;
import ardc.cerium.mycelium.rifcs.IdentifierNormalisationService;
import ardc.cerium.mycelium.service.RelationLookupService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph Provider for RIFCS documents
 *
 * @author Minh Duc Nguyen
 */
@Slf4j
public class RIFCSGraphProvider {

	public static final String RIFCS_KEY_IDENTIFIER_TYPE = "ro:key";

	public static final String RIFCS_ID_IDENTIFIER_TYPE = "ro:id";

	public static final String RELATION_SAME_AS = "isSameAs";

	public static final String RELATION_RELATED_TO = "isRelatedTo";

	public static final String ORIGIN_IDENTIFIER = "Identifier";

	public static final String ORIGIN_RELATED_OBJECT = "RelatedObject";

	public static final String ORIGIN_RELATED_INFO = "RelatedInfo";

	public static final String RELATION_HAS_ASSOCIATION_WITH = "hasAssociationWith";

	private final RDARegistryClient rdaRegistryClient;

	public RIFCSGraphProvider(RDARegistryClient rdaRegistryClient) {
		this.rdaRegistryClient = rdaRegistryClient;
	}

	/**
	 * Obtain the {@link Graph} data for a given RIFCS XML payload
	 * @param xml the XML payload can contain multiple registryObject
	 * @return the generated {@link Graph}
	 */
	public Graph get(String xml) {
		RegistryObjects registryObjects = RIFCSParser.parse(xml);
		Graph graph = new Graph();

		if (registryObjects == null || registryObjects.getRegistryObjects().size() == 0) {
			// log something gone wrong with parsing here
			return graph;
		}

		registryObjects.getRegistryObjects().forEach(registryObject -> {
			// find the RegistryObject and have the ID as the originNode
			String key = registryObject.getKey();
			RegistryObject ro = rdaRegistryClient.getPublishedByKey(key);
			log.debug("Resolved to RegistryObject registryObjectId:{}", ro.getRegistryObjectId());

			Vertex originNode = new Vertex(ro.getRegistryObjectId().toString(), RIFCS_ID_IDENTIFIER_TYPE);
			originNode.addLabel(Vertex.Label.RegistryObject);
			originNode.setObjectType(ro.getType());
			originNode.setObjectClass(ro.getClassification());
			originNode.setTitle(ro.getTitle());
			graph.addVertex(originNode);

			// key is the identifier node
			Vertex keyNode = new Vertex(key, RIFCS_KEY_IDENTIFIER_TYPE);
			keyNode.addLabel(Vertex.Label.Identifier);
			graph.addVertex(keyNode);
			graph.addEdge(new Edge(originNode, keyNode, RELATION_SAME_AS));

			// every identifier is a vertex & isSameAs
			List<Identifier> identifiers = registryObject.getIdentifiers();
			if (identifiers != null && identifiers.size() > 0) {
				registryObject.getIdentifiers().forEach(identifier -> {
					identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
					Vertex identifierNode = new Vertex(identifier.getValue(), identifier.getType());
					identifierNode.addLabel(Vertex.Label.Identifier);
					graph.addVertex(identifierNode);
					Edge edge = new Edge(originNode, identifierNode, RELATION_SAME_AS);
					edge.setOrigin(ORIGIN_IDENTIFIER);
					graph.addEdge(edge);
				});
			}

			// every relatedObject is a vertex & edge
			List<RelatedObject> relatedObjects = registryObject.getRelatedObjects();
			if (relatedObjects != null && relatedObjects.size() > 0) {
				relatedObjects.forEach(relatedObject -> {
					Vertex relatedObjectNode = new Vertex(relatedObject.getKey(), RIFCS_KEY_IDENTIFIER_TYPE);
					relatedObjectNode.addLabel(Vertex.Label.Identifier);
					graph.addVertex(relatedObjectNode);
					relatedObject.getRelation().forEach(relation -> {
						Edge edge = new Edge(originNode, relatedObjectNode, relation.getType());
						edge.setOrigin(ORIGIN_RELATED_OBJECT);
						graph.addEdge(edge);

						// reversed edge for relatedObject relationships
						graph.addEdge(getReversedEdge(edge));
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
						relatedInfoIdentifier = IdentifierNormalisationService.getNormalisedIdentifier(relatedInfoIdentifier);
						Vertex relatedInfoNode = new Vertex(relatedInfoIdentifier.getValue(),
								relatedInfoIdentifier.getType());
						relatedInfoNode.setTitle(relatedInfo.getTitle());
						relatedInfoNode.addLabel(Vertex.Label.Identifier);
						relatedInfoNode.setObjectType(relatedInfo.getType());
						relatedInfoNode.setObjectClass(relatedInfo.getType());
						graph.addVertex(relatedInfoNode);

						// if there's no relatedInfo/relations, the default relation is hasAssociationWith
						if (relatedInfoRelations.size() == 0) {
							Edge edge = new Edge(originNode, relatedInfoNode, RELATION_HAS_ASSOCIATION_WITH);
							edge.setOrigin(ORIGIN_RELATED_INFO);
							graph.addEdge(edge);

							// reversed edge for relatedInfo relationships
							graph.addEdge(getReversedEdge(edge));
						}

						// otherwise for each relation element, it's a separate edge
						relatedInfoRelations.forEach(relatedInfoRelation -> {
							Edge edge = new Edge(originNode, relatedInfoNode, relatedInfoRelation.getType());
							edge.setOrigin(ORIGIN_RELATED_INFO);
							graph.addEdge(edge);

							// reversed edge for relatedInfo relationships
							graph.addEdge(getReversedEdge(edge));
						});
					});
				});
			}
		});

		return graph;
	}

	/**
	 * Obtain the reversed form of an Edge
	 *
	 * The reversed edge and flipped from, to and the relationType is also flipped
	 * @param edge the {@link Edge} to reverse
	 * @return the reversed {@link Edge}
	 */
	public static Edge getReversedEdge(Edge edge) {
		String reversedRelationType = RelationLookupService.getReverse(edge.getType(), RELATION_RELATED_TO);
		Edge reversedEdge = new Edge(edge.getTo(), edge.getFrom(), reversedRelationType);

		// copy the relevant data over
		reversedEdge.setInternal(edge.isInternal());
		reversedEdge.setOrigin(edge.getOrigin());
		reversedEdge.setPublic(edge.isPublic());

		// flip the reverse value
		reversedEdge.setReverse(! edge.isReverse());
		return reversedEdge;
	}

}
