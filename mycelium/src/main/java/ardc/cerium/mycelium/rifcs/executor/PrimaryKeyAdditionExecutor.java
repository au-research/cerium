package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyAdditionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.RelationLookupService;
import ardc.cerium.mycelium.util.DataSourceUtil;
import ardc.cerium.mycelium.util.RelationUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	@Override
	public void handle() {
		// this primaryKey should be added to all RegistryObject belongs to this
		// DataSource
		GraphService graphService = getMyceliumService().getGraphService();
		PrimaryKey pk = sideEffect.getPrimaryKey();
		Vertex to = getMyceliumService().getRegistryObjectVertexFromKey(pk.getKey());
		DataSource dataSource = getMyceliumService().getDataSourceById(sideEffect.getDataSourceId());

		// insert the PK edges to neo4j and SOLR
		if (pk.getRelationTypeFromCollection() != null) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(
					dataSource.getId(), "collection")) {
				stream.forEach(from -> this.insertPKEdges(from, to, pk.getRelationTypeFromCollection()));
			}
		}

		if (pk.getRelationTypeFromActivity() != null) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(
					dataSource.getId(), "activity")) {
				stream.forEach(from -> this.insertPKEdges(from, to, pk.getRelationTypeFromActivity()));
			}
		}

		if (pk.getRelationTypeFromService() != null) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(
					dataSource.getId(), "service")) {
				stream.forEach(from -> this.insertPKEdges(from, to, pk.getRelationTypeFromService()));
			}
		}

		if (pk.getRelationTypeFromParty() != null) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(
					dataSource.getId(), "activity")) {
				stream.forEach(from -> this.insertPKEdges(from, to, pk.getRelationTypeFromParty()));
			}
		}

		// todo handle GrantsNetwork if the PrimaryKey relation is a GrantsNetwork edge
	}

	private void insertPKEdges(Vertex from, Vertex to, String relationType) {
		GraphService graphService = getMyceliumService().getGraphService();
		MyceliumIndexingService indexingService = getMyceliumService().getIndexingService();

		// insert into Neo4j
		Edge edge = new Edge(from, to, relationType);
		edge.setOrigin(RIFCSGraphProvider.ORIGIN_PRIMARY_LINK);
		Edge reversedEdge = RIFCSGraphProvider.getReversedEdge(edge);
		graphService.ingestEdge(edge);
		graphService.ingestEdge(reversedEdge);

		// index in SOLR
		EdgeDTO dto = RelationUtil.getEdgeDTO(edge);
		EdgeDTO reversedDTO = RelationUtil.getEdgeDTO(reversedEdge);
		indexingService.indexRelation(from, to, List.of(dto));
		indexingService.indexRelation(to, from, List.of(reversedDTO));
	}

}
