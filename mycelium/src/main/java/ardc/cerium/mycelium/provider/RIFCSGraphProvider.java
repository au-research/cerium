package ardc.cerium.mycelium.provider;

import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.*;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import ardc.cerium.mycelium.rifcs.model.*;
import ardc.cerium.mycelium.service.RelationLookupService;
import ardc.cerium.mycelium.util.IdentifierUtil;
import ardc.cerium.mycelium.util.VertexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Base64;
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

	public static final String DATASOURCE_ID_IDENTIFIER_TYPE = "ds:id";

	public static final String RELATION_SAME_AS = "isSameAs";

	public static final String RELATION_RELATED_TO = "isRelatedTo";

	public static final String ORIGIN_IDENTIFIER = "Identifier";

	public static final String ORIGIN_RELATED_OBJECT = "RelatedObject";

	public static final String ORIGIN_RELATED_INFO = "RelatedInfo";

	public static final String ORIGIN_PRIMARY_LINK = "PrimaryLink";

	public static final String RELATION_HAS_ASSOCIATION_WITH = "hasAssociationWith";

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
		reversedEdge.setUrl(edge.getUrl());
		reversedEdge.setDescription(edge.getDescription());

		// flip the reverse value
		reversedEdge.setReverse(!edge.isReverse());
		return reversedEdge;
	}

	/**
	 * Parse a String JSON Payload into ingestable {@link RegistryObject}
	 *
	 * @param payload the string payload
	 * @return the {@link RegistryObject} deserialised
	 * @throws JsonProcessingException when the deserialisation failed
	 */
	public static RegistryObject parsePayloadToRegistryObject(String payload) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(payload, RegistryObject.class);
	}

	/**
	 * Obtain an ingestable {@link Graph} from a JSONPayload in String format.
	 *
	 * This graph includes id node, key node of the registryObject, all identifiers with
	 * sameAs relationships, direct relatedObject relationships + reverse and direct
	 * relatedInfo relationships + reverse. The graph also updated to include PrimaryKey
	 * relationships if the {@link AdditionalRelation} includes that
	 * @param registryObject deserialised {@link RegistryObject} from the JSONPayload
	 * @return the {@link Graph} obtained from the {@link RegistryObject}
	 */
	public Graph get(RegistryObject registryObject)  {
		Graph graph = new Graph();

		DataSource ds = registryObject.getDataSource();
		log.debug("Found dataSource in payload title:{}", ds.getTitle());

		String xml = new String(Base64.getDecoder().decode(registryObject.getRifcs()));
		log.debug("Found xml in payload {}", xml);

		RegistryObjects registryObjects = RIFCSParser.parse(xml);
		if (registryObjects == null || registryObjects.getRegistryObjects().size() == 0) {
			log.error("JSON payload doesn't contain rifcs xml");
			// todo throw and handle exception
			return graph;
		}

		// there is always 1 registryObject in the json payload
		ardc.cerium.mycelium.rifcs.model.RegistryObject rifcs = registryObjects.getRegistryObjects().get(0);

		// find the RegistryObject and have the ID as the originNode
		// keys and Identifiers shouldn't have leading and tailing white spaces
		String key = registryObject.getKey().trim();
		String keyFromPayload = rifcs.getKey().trim();
		log.debug("keys should match here {} {}", key, keyFromPayload);

		if (!key.equals(keyFromPayload)) {
			log.error("XML Key does not match registryObject key, {} and {}", key, keyFromPayload);
			// todo throw and handle exception
			return graph;
		}

		log.debug("RegistryObjectId: {}", registryObject.getRegistryObjectId());
		log.info("Ingesting RegistryObject[id={}, key={}]", registryObject.getRegistryObjectId(), key);

		// add the originNode, which is the ID node
		Vertex originNode = new Vertex(registryObject.getRegistryObjectId().toString(), RIFCS_ID_IDENTIFIER_TYPE);
		originNode.addLabel(Vertex.Label.RegistryObject);
		if(registryObject.getStatus().equals(Vertex.Status.PUBLISHED.name())){
			originNode.addLabel(Vertex.Label.PUBLISHED);
			originNode.setStatus(Vertex.Status.PUBLISHED);
		}
		else{
			originNode.addLabel(Vertex.Label.DRAFT);
			originNode.setStatus(Vertex.Status.DRAFT);
		}
		originNode.setObjectType(registryObject.getType());
		originNode.setObjectClass(registryObject.getClassification());
		originNode.setTitle(registryObject.getTitle());
		originNode.setUrl(registryObject.getPortalUrl());
		originNode.setDataSourceId(registryObject.getDataSource().getId().toString());
		originNode.setPublic(registryObject.getStatus().equals(Vertex.Status.PUBLISHED.name()));
		originNode.setGroup(registryObject.getGroup());
		originNode.setListTitle(registryObject.getListTitle());
		graph.addVertex(originNode);

		// add the key origin Node, (id)-[isSameAs]->(key)
		Vertex keyNode = new Vertex(key, RIFCS_KEY_IDENTIFIER_TYPE);
		keyNode.addLabel(Vertex.Label.Identifier);

		// Set the title for the ro:key node to equal to it's PUBLISHED counter-part
		// for use with Vertex title sorting in cypher queries
		if (originNode.getStatus().equals("PUBLISHED")) {
			keyNode.setTitle(originNode.getTitle());
		}

		graph.addVertex(keyNode);
		graph.addEdge(new Edge(originNode, keyNode, RELATION_SAME_AS));

		// every identifier is a vertex & isSameAs
		// (id)-[isSameAs]->(identifier)
		List<Identifier> identifiers = rifcs.getIdentifiers();
		if (identifiers != null && identifiers.size() > 0) {
			rifcs.getIdentifiers().forEach(identifier -> {
				try {
					Vertex identifierNode = new Vertex(identifier.getValue(), identifier.getType());
					identifierNode.addLabel(Vertex.Label.Identifier);

					// normalise and resolve the Identifier Vertex
					VertexUtil.normalise(identifierNode);
					VertexUtil.resolveVertex(identifierNode);
					identifierNode.setUrl(IdentifierUtil.getUrl(identifierNode.getIdentifier(), identifierNode.getIdentifierType()));

					// add it to the graph
					graph.addVertex(identifierNode);

					// create and add the edge from (ro:id origin)-[isSameAs]->(identifierNode) to the graph
					Edge edge = new Edge(originNode, identifierNode, RELATION_SAME_AS);
					edge.setOrigin(ORIGIN_IDENTIFIER);
					graph.addEdge(edge);

				}
				catch (ContentNotSupportedException e) {
					log.info(e.getMessage());
				}
			});
		}

		// every relatedObject is a vertex & edge
		// (id)-[r]->(relatedObjectKey)
		List<RelatedObject> relatedObjects = rifcs.getRelatedObjects();
		if (relatedObjects != null && relatedObjects.size() > 0) {
			relatedObjects.forEach(relatedObject -> {
				// RDA-772 the Registry's add_new creates empty placeholders (relatedObjects included)
				if(relatedObject.getKey() != null && !relatedObject.getKey().equals("")) {
					Vertex relatedObjectNode = new Vertex(relatedObject.getKey(), RIFCS_KEY_IDENTIFIER_TYPE);
					relatedObjectNode.addLabel(Vertex.Label.Identifier);
					graph.addVertex(relatedObjectNode);
					relatedObject.getRelation().forEach(relation -> {
						Edge edge = new Edge(originNode, relatedObjectNode, relation.getType());
						edge.setOrigin(ORIGIN_RELATED_OBJECT);
						edge.setUrl(relation.getUrl());
						edge.setDescription(relation.getDescription());
						graph.addEdge(edge);

						// reversed edge for relatedObject relationships
						graph.addEdge(getReversedEdge(edge));
					});
				}
			});

		}

		// every relatedInfo is a vertex & edge
		// (id)-[r]->(relatedInfoIdentifier)
		List<RelatedInfo> relatedInfos = rifcs.getRelatedInfos();
		if (relatedInfos != null && relatedInfos.size() > 0) {
			relatedInfos.forEach(relatedInfo -> {

				// relatedInfos can have many identifiers
				List<Identifier> relatedInfoIdentifiers = relatedInfo.getIdentifiers() != null
						? relatedInfo.getIdentifiers() : new ArrayList<>();
				List<Relation> relatedInfoRelations = relatedInfo.getRelation() != null ? relatedInfo.getRelation()
						: new ArrayList<>();
				relatedInfoIdentifiers.forEach(relatedInfoIdentifier -> {
					try {

						// create the relatedInfo Identifier vertex and sets the relevant values
						Vertex relatedInfoNode = new Vertex(relatedInfoIdentifier.getValue(),
								relatedInfoIdentifier.getType());
						relatedInfoNode.setTitle(relatedInfo.getTitle());
						relatedInfoNode.addLabel(Vertex.Label.Identifier);
						relatedInfoNode.setObjectType(relatedInfo.getType());
						relatedInfoNode.setObjectClass(relatedInfo.getType());
						relatedInfoNode.setNotes(relatedInfo.getNotes());

						// normalise and resolve the vertex
						VertexUtil.normalise(relatedInfoNode);
						VertexUtil.resolveVertex(relatedInfoNode);
						relatedInfoNode.setUrl(IdentifierUtil.getUrl(relatedInfoIdentifier.getValue(),relatedInfoIdentifier.getType()));

						// add it to the graph
						graph.addVertex(relatedInfoNode);

						// if there's no relatedInfo/relations, the default relation is
						// hasAssociationWith
						if (relatedInfoRelations.size() == 0) {
							Edge edge = new Edge(originNode, relatedInfoNode, RELATION_HAS_ASSOCIATION_WITH);
							edge.setOrigin(ORIGIN_RELATED_INFO);
							graph.addEdge(edge);

							// reversed edge for relatedInfo relationships
							graph.addEdge(getReversedEdge(edge));
						}

						// Otherwise, for each relation element, it's a separate edge
						relatedInfoRelations.forEach(relation -> {
							Edge edge = new Edge(originNode, relatedInfoNode, relation.getType());
							edge.setOrigin(ORIGIN_RELATED_INFO);
							edge.setUrl(relation.getUrl());
							edge.setDescription(relation.getDescription());
							graph.addEdge(edge);

							// reversed edge for relatedInfo relationships
							graph.addEdge(getReversedEdge(edge));
						});

					}catch(ContentNotSupportedException e){
						log.info(e.getMessage());
					}
				});
			});
		}

		// implicit PrimaryKey
		AdditionalRelation[] additionalRelations = registryObject.getAdditionalRelations();
		if (additionalRelations == null || additionalRelations.length == 0)
			return graph;

		for (AdditionalRelation additionalRelation : additionalRelations) {
			log.info("additionalRelation {}, {}", additionalRelation.getToKey(), additionalRelation.getRelationType());
			if (additionalRelation.getOrigin().equals("PRIMARY-KEY")) {
				Vertex relatedObjectNode = new Vertex(additionalRelation.getToKey(), RIFCS_KEY_IDENTIFIER_TYPE);
				relatedObjectNode.addLabel(Vertex.Label.Identifier);
				graph.addVertex(relatedObjectNode);
				Edge edge = new Edge(originNode, relatedObjectNode, additionalRelation.getRelationType());
				edge.setOrigin(ORIGIN_PRIMARY_LINK);
				graph.addEdge(edge);

				// reversed edge relationships
				graph.addEdge(getReversedEdge(edge));
			}
		}

		return graph;
	}

}
