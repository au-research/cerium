package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyAdditionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.DataSourceUtil;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PrimaryKeyAdditionExecutor extends Executor {

	private final PrimaryKeyAdditionSideEffect sideEffect;

	public PrimaryKeyAdditionExecutor(PrimaryKeyAdditionSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link PrimaryKeyAdditionSideEffect} is applicable
	 * @param before the state of the {@link DataSource} before the mutation
	 * @param after the state of the {@link DataSource} after the mutation
	 * @param myceliumService the {@link MyceliumService} to access the graph and services
	 * @return true if there are new PrimaryKey settings added to the DataSource
	 */
	public static boolean detect(DataSource before, DataSource after, MyceliumService myceliumService) {

		// there must be differences
		List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(before, after);

		// the differences (new pk must have resolvable RegistryObject)
		differences = differences.stream().filter(pk -> {
			Vertex registryObjectVertex = myceliumService.getRegistryObjectVertexFromKey(pk.getKey());
			return registryObjectVertex != null;
		}).collect(Collectors.toList());

		return differences.size() > 0;
	}

	/**
	 * Detect if {@link PrimaryKeyAdditionSideEffect} is applicable
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @param myceliumService the {@link MyceliumService} to access the graph resources
	 * @return true if the RegistryObject imported should be a PrimaryKey
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {

		// registryObject is created, before has to be null
		if (before != null) {
			return false;
		}

		// check if the after state's data source settings has a primary key equals to
		// that of the record
		DataSource dataSource = myceliumService.getDataSourceById(after.getDataSourceId());
		return dataSource != null && dataSource.getPrimaryKeySetting().getPrimaryKeys().stream()
				.anyMatch(primaryKey -> primaryKey.getKey().equals(after.getRegistryObjectKey()));
	}

	@Override
	public void handle() {
		// this primaryKey should be added to all RegistryObject belongs to this
		// DataSource
		GraphService graphService = getMyceliumService().getGraphService();
		MyceliumIndexingService myceliumIndexingService = getMyceliumService().getMyceliumIndexingService();
		PrimaryKey pk = sideEffect.getPrimaryKey();
		String toKey = pk.getKey();
		Vertex roVertex = getMyceliumService().getRegistryObjectVertexFromKey(toKey);
		DataSource dataSource = getMyceliumService().getDataSourceById(sideEffect.getDataSourceId());

		// insert the PK edges to neo4j and SOLR
		if (pk.getRelationTypeFromCollection() != null) {
			String relationType = pk.getRelationTypeFromCollection();

			// insert the pk edge first
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(),
					"collection")) {
				stream.forEach(from -> this.insertPKEdges(from, toKey, relationType));
			}

			// then check for grantsNetwork
			if (RelationUtil.isGrantsNetwork("collection", roVertex.getObjectClass(), relationType)) {
				try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(),
						"collection")) {
					stream.forEach(from -> {
						// index GrantsNetwork for all child collections
						try (Stream<Vertex> childStream = graphService.streamChildCollection(from)) {
							childStream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
						}
					});
				}
			}
		}

		if (pk.getRelationTypeFromActivity() != null) {
			String relationType = pk.getRelationTypeFromActivity();

			// insert the pk edge first
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(),
					"activity")) {
				stream.forEach(from -> this.insertPKEdges(from, toKey, relationType));
			}

			// then check for grantsNetwork
			if (RelationUtil.isGrantsNetwork("activity", roVertex.getObjectClass(), relationType)) {
				try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(),
						"activity")) {
					stream.forEach(from -> {
						// index GrantsNetwork for all child activities
						try (Stream<Vertex> childStream = graphService.streamChildActivity(from)) {
							childStream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
						}
						// index GrantsNetwork for all child collections
						try (Stream<Vertex> childStream = graphService.streamChildCollection(from)) {
							childStream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
						}
					});
				}
			}
		}

		if (pk.getRelationTypeFromService() != null) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(),
					"service")) {
				stream.forEach(from -> this.insertPKEdges(from, toKey, pk.getRelationTypeFromService()));
			}
			// no GrantsNetwork affecting services
		}

		if (pk.getRelationTypeFromParty() != null) {
			String relationType = pk.getRelationTypeFromParty();

			// insert the pk edge
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(), "party")) {
				stream.forEach(from -> this.insertPKEdges(from, toKey, relationType));
			}

			if (RelationUtil.isGrantsNetwork("party", roVertex.getObjectClass(), relationType)) {
				try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSource.getId(), "party")) {
					stream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
				}
			}
		}
	}

	/**
	 * Helper function to insert a Primary Key into Neo4j and SOLR
	 * @param from the origin RegistryObject {@link Vertex}
	 * @param toKey the target PrimaryKey
	 * @param relationType the relationType to the PrimaryKey
	 */
	private void insertPKEdges(Vertex from, String toKey, String relationType) {
		log.debug("Inserting PrimaryKey Edge FromVertex[id={}] ToKey[id={}]", from.getIdentifier(), toKey);
		GraphService graphService = getMyceliumService().getGraphService();
		MyceliumIndexingService indexingService = getMyceliumService().getIndexingService();

		Vertex keyVertex = graphService.getVertexByIdentifier(toKey, RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex roVertex = getMyceliumService().getRegistryObjectVertexFromKey(toKey);

		// don't want to insert PK edge from itself to itself
		if (from.getIdentifier().equals(roVertex.getIdentifier())) {
			log.debug("Skipping Inserting self PrimaryKey Edge FromVertex[id={}] ToKey[id={}]", from.getIdentifier(),
					toKey);
			return;
		}

		// insert into Neo4j + reversed edge
		Edge edge = new Edge(from, keyVertex, relationType);
		edge.setOrigin(RIFCSGraphProvider.ORIGIN_PRIMARY_LINK);
		Edge reversedEdge = RIFCSGraphProvider.getReversedEdge(edge);
		graphService.ingestEdge(edge);
		graphService.ingestEdge(reversedEdge);

		// index in SOLR + reversed edge
		EdgeDTO dto = RelationUtil.getEdgeDTO(edge);
		EdgeDTO reversedDTO = RelationUtil.getEdgeDTO(reversedEdge);
		indexingService.indexRelation(from, roVertex, List.of(dto));
		indexingService.indexRelation(roVertex, from, List.of(reversedDTO));
	}

}
