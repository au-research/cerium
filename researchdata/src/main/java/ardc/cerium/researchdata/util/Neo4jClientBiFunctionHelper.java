package ardc.cerium.researchdata.util;

import ardc.cerium.researchdata.model.RelationDocument;
import ardc.cerium.researchdata.model.Vertex;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.springframework.data.neo4j.core.Neo4jClient;

/**
 * A set of useful BiFunction used in retrieving contents from Cypher Queries. Used
 * primarily in {@link Neo4jClient} use cases
 *
 * @author Minh Duc Nguyen
 */
public class Neo4jClientBiFunctionHelper {

	/**
	 * Converting {@link Record} results into {@link RelationDocument}, typically from a
	 * Cypher Query that returns a set of relations. Should be used in conjunction with a
	 * {@link Neo4jClient.RecordFetchSpec}
	 * @param record The {@link Record} that contains the {@link RelationshipValue}
	 * @param returnCallSign the call sign of the return statement, eg RETURN r
	 * @return the {@link RelationDocument} mapping document
	 */
	public static RelationDocument toRelationDocument(Record record, String returnCallSign) {
		RelationDocument relationDocument = new RelationDocument();
		RelationshipValue relationship = (RelationshipValue) record.get(returnCallSign);
		relationDocument.setRelationType(relationship.asRelationship().type());
		return relationDocument;
	}

	/**
	 * Converting the {@link Record} to {@link Vertex}, typically from a Cypher Query that
	 * returns a set of nodes. Should be used in conjunction with a
	 * {@link Neo4jClient.RecordFetchSpec}
	 * @param record the {@link Record} that contains the {@link Value}
	 * @param returnCallSign the call sign, eg RETURN n;
	 * @return the {@link Vertex} derived from the resulting {@link Value}
	 */
	public static Vertex toVertex(Record record, String returnCallSign) {
		Value value = record.get(returnCallSign);
		return new Vertex(value.get("identifier").asString(), value.get("identifierType").asString());
	}

	/**
	 * Converting a {@link Record} to {@link RelationDocument}, typically from a Cypher
	 * Query that returns a path. Should be used in conjunction with a
	 * {@link Neo4jClient.RecordFetchSpec}
	 * @param record the {@link Record} that contains the {@link PathValue}
	 * @param returnCallSign the call sign, eg, RETURN p;
	 * @return the {@link RelationDocument} contains all the information about this Path
	 */
	public static RelationDocument pathToRelationDocument(Record record, String returnCallSign) {
		PathValue value = (PathValue) record.get(returnCallSign);
		RelationDocument relationDocument = new RelationDocument();
		value.asPath().forEach(segment -> {
			relationDocument.setFromIdentifier(segment.start().get("identifier").asString());
			relationDocument.setFromIdentifierType(segment.start().get("identifierType").asString());
			relationDocument.setToIdentifier(segment.end().get("identifier").asString());
			relationDocument.setToIdentifierType(segment.end().get("identifierType").asString());
			relationDocument.setRelationType(segment.relationship().type());
		});
		return relationDocument;
	}

}
