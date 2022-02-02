package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.RelatedInfoRealisationSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RelatedInfoRealisationExecutor extends Executor {

	private final RelatedInfoRealisationSideEffect sideEffect;

	public RelatedInfoRealisationExecutor(RelatedInfoRealisationSideEffect sideEffect,
			MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link RelatedInfoRealisationSideEffect} is applicable
	 *
	 * @param before the {@link DataSource} state before the mutation
	 * @param after the {@link DataSource} state after the mutation
	 * @param myceliumService the {@link MyceliumService} to access services
	 * @return true if {@link RelatedInfoRealisationSideEffect} is detected
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {

		// no identifier should be realised if the record is deleted
		if (after == null) {
			return false;
		}

		// if the after state contains additional Identifier Vertex and those identifier
		// vertices has non isSameAs relationships
		List<Vertex> realisedIdentifiers = getRealisedIdentifiers(before, after, myceliumService);

		return realisedIdentifiers.size() > 0;
	}

	public static List<Vertex> getRealisedIdentifiers(RecordState before, RecordState after,
			MyceliumService myceliumService) {

		// no identifier should be realised if the record is deleted
		if (after == null) {
			return new ArrayList<>();
		}

		// obtain identifier differences
		List<Vertex> addedIdentifiers = after.getIdentical().stream()
				.filter(vertex -> vertex.hasLabel(Vertex.Label.Identifier))
				.filter(vertex -> !vertex.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE))
				.collect(Collectors.toList());

		// if the record wasn't created but was updated, then get the differences
		if (before != null) {
			List<String> beforeIdentifierValues = before.getIdentical().stream().map(vertex -> vertex.getIdentifier()).collect(Collectors.toList());
			addedIdentifiers = after.getIdentical().stream()
					.filter(vertex -> !beforeIdentifierValues.contains(vertex.getIdentifier()))
					.collect(Collectors.toList());
		}

		// realised identifiers are identifiers that has direct relationships
		return addedIdentifiers.stream().filter(vertex -> {
			Collection<Relationship> directOutboundRelationships = myceliumService.getGraphService()
					.getDirectOutboundRelationships(vertex.getIdentifier(), vertex.getIdentifierType());
			return directOutboundRelationships.size() > 0;
		}).collect(Collectors.toList());
	}

	@Override
	public void handle() {

		String identifier = sideEffect.getIdentifier();
		String identifierType = sideEffect.getIdentifierType();
		String registryObjectId = sideEffect.getRegistryObjectId();
		String title = sideEffect.getTitle();
		String objectClass = sideEffect.getRecordClass();
		String objetType = sideEffect.getRecordType();

		MyceliumIndexingService indexingService = getMyceliumService().getIndexingService();

		// find all the RelationshipDocument that to_identifier=<realisedIdentifier>
		Cursor<RelationshipDocument> cursor = indexingService.cursorFor(new Criteria("to_identifier").is(identifier));
		while (cursor.hasNext()) {
			RelationshipDocument doc = cursor.next();

			String fromId = doc.getFromId();

			// remove RelatedInfo relationships to the Identifier
			indexingService.deleteRelationshipDocument(doc);

			// reprocess the relationship source vertex to include the new realised
			// RelatedObject relationship
			Vertex vertex = getMyceliumService().getVertexFromRegistryObjectId(fromId);
			// it should not happen but will result in nullPointException if it ever occurs
			if(vertex != null){
				indexingService.indexDirectRelationships(vertex);
			}
		}
		Vertex vertex = getMyceliumService().getIdentifierVertex(identifier, identifierType);
		Collection<Relationship> relationships = getMyceliumService().getGraphService().getDirectInboundRelationships(identifier, identifierType);
		relationships.forEach(relationship -> {
			// find all registry Objects from the Vertex that are related to this Identifier
			// remove the record's from the portal Index
			if(relationship.getFrom().getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE)){
				// the related_<class>_title is the title of the record we need to remove from the portal Index
				// related_collection_title
				// related_party_multi
				// in portal Index it goes both ways, so we need to delete both to and from
				getMyceliumService().getMyceliumIndexingService().addRelatedTitleToPortalIndex(relationship.getFrom().getIdentifier(),
						objectClass,
						objetType ,
						title);
				getMyceliumService().getMyceliumIndexingService().addRelatedTitleToPortalIndex(registryObjectId,
						relationship.getFrom().getObjectClass(),
						relationship.getFrom().getObjectType(),
						relationship.getFrom().getTitle());
			}});



	}

}
