package ardc.cerium.mycelium.scenario;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.EdgeDocument;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.util.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ardc.cerium.mycelium.service.MyceliumRequestService.IMPORT_REQUEST_TYPE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class Scenario16_IT extends MyceliumScenarioTest {

	@Test
	void scenario16Test() throws IOException {

		// given a datasource
		DataSource dataSource = TestHelper.mockDataSource();
		myceliumService.importDataSource(dataSource);

		// create the ImportRequest
		Request importRequest = myceliumService.createRequest(TestHelper.mockRequestDTO(IMPORT_REQUEST_TYPE));

		// payload 1
		List<String> importPayload1 = TestHelper.getJSONImportPayload(Helpers.readFile(
				"src/test/resources/scenarios/16_RelationshipScenario/16_RelationshipScenario_AUTestingRecords.xml"),
				null, dataSource, null, 1);
		importPayload1.forEach(payload -> {
			this.webTestClient
					.post().uri(uriBuilder -> uriBuilder.path(importRecordAPI)
							.queryParam("requestId", importRequest.getId()).build())
					.body(Mono.just(payload), String.class).exchange().expectStatus().isOk();
		});

		// payload 2
		List<String> importPayload2 = TestHelper.getJSONImportPayload(Helpers.readFile(
				"src/test/resources/scenarios/16_RelationshipScenario/16_RelationshipScenario_AUTestingRecords2.xml"),
				null, dataSource, null, 100);
		importPayload2.forEach(payload -> {
			this.webTestClient
					.post().uri(uriBuilder -> uriBuilder.path(importRecordAPI)
							.queryParam("requestId", importRequest.getId()).build())
					.body(Mono.just(payload), String.class).exchange().expectStatus().isOk();
		});

		// payload 3
		List<String> importPayload3 = TestHelper.getJSONImportPayload(Helpers.readFile(
				"src/test/resources/scenarios/16_RelationshipScenario/16_RelationshipScenario_AUTestingRecords3.xml"),
				null, dataSource, null, 200);
		importPayload3.forEach(payload -> {
			this.webTestClient
					.post().uri(uriBuilder -> uriBuilder.path(importRecordAPI)
							.queryParam("requestId", importRequest.getId()).build())
					.body(Mono.just(payload), String.class).exchange().expectStatus().isOk();
		});

		GraphService graphService = myceliumService.getGraphService();

		// after import, C3 isFundedBy C1 in neo4j
		String p1Key = "P1_16";
		Vertex p1 = graphService.getRegistryObjectByKey(p1Key);
		assertThat(p1).isNotNull();

		String c3Key = "C3_16";
		Vertex c3 = graphService.getRegistryObjectByKey(c3Key);
		assertThat(c3).isNotNull();

		// c3 has p1 as a grantsNetwork party parent
		List<Vertex> c3PartyParents = graphService.streamParentParty(c3)
				.collect(Collectors.toList());
		assertThat(c3PartyParents.stream().anyMatch(v -> {
			return v.getIdentifier().equals(p1.getIdentifier()) && v.getIdentifierType().equals(p1.getIdentifierType());
		})).isTrue();

		// index C3 and c3-[isFundedBy]->P1 and P1-[isFunderOf]->C3
		this.webTestClient.post().uri(uriBuilder -> uriBuilder.path(indexRecordAPI)
				.queryParam("registryObjectId", c3.getIdentifier()).build()).exchange().expectStatus().isOk();


		List<RelationshipDocument> fromC3Rels = TestHelper.cursorToList(
				myceliumService.getMyceliumIndexingService().cursorFor(new Criteria("from_id").is(c3.getIdentifier())));
		RelationshipDocument c3ToP1 = fromC3Rels.stream().filter(doc -> {
			return doc.getToIdentifier().equals(p1.getIdentifier());
		}).findFirst().orElse(null);

		// C3 has a relation to P1
		assertThat(c3ToP1).isNotNull();

		EdgeDocument c3isFundedByP1 = c3ToP1.getRelations().stream().filter(edgeDocument -> {
			return edgeDocument.getRelationType().equals("isFundedBy");
		}).findFirst().orElse(null);

		// C3 isFundedBy P1 in SOLR
		assertThat(c3isFundedByP1).isNotNull();

		// C3 isFundedBy P1 is a reversed relation (because GrantsNetwork top-down)
		assertThat(c3isFundedByP1.isRelationReverse()).isFalse();

		// P1 isFunderOf C3 in SOLR
		List<RelationshipDocument> fromP1Rels = TestHelper.cursorToList(
				myceliumService.getMyceliumIndexingService().cursorFor(new Criteria("from_id").is(p1.getIdentifier())));

		RelationshipDocument p1ToC3 = fromP1Rels.stream().filter(doc -> {
			return doc.getToIdentifier().equals(c3.getIdentifier());
		}).findFirst().orElse(null);

		// P1 has relation to C3
		assertThat(p1ToC3).isNotNull();

		EdgeDocument p1IsFunderOfC3 = p1ToC3.getRelations().stream().filter(edgeDocument -> edgeDocument.getRelationType().equals("isFunderOf"))
				.findFirst().orElse(null);

		// P1 isFunderOf C3
		assertThat(p1IsFunderOfC3).isNotNull();

		// the relation is a top-down direct relationship
		assertThat(p1IsFunderOfC3.isRelationReverse()).isTrue();
	}

}
