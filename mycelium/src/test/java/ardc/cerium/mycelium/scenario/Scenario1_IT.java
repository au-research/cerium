package ardc.cerium.mycelium.scenario;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.MyceliumIntegrationTest;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.TestHelper;
import lombok.experimental.Helper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static ardc.cerium.mycelium.service.MyceliumRequestService.IMPORT_REQUEST_TYPE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class Scenario1_IT extends MyceliumScenarioTest {

	@Test
	void scenario1Test() throws IOException {
		// given a datasource
		DataSource dataSource = TestHelper.mockDataSource();
		myceliumService.importDataSource(dataSource);

		// create the ImportRequest
		Request importRequest = myceliumService.createRequest(TestHelper.mockRequestDTO(IMPORT_REQUEST_TYPE));

		// prepare the import payload
		List<String> titles = new ArrayList<>();
		titles.add("Relatinship scenario 1");
		titles.add("(AUTestingRecords) Example related party 2");
		List<String> importPayloads = TestHelper.getJSONImportPayload(
				Helpers.readFile("src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml"),
				titles, dataSource, null, 1);

		// import all the payloads
		importPayloads.forEach(payload -> {
			this.webTestClient
					.post().uri(uriBuilder -> uriBuilder.path(importRecordAPI)
							.queryParam("requestId", importRequest.getId()).build())
					.body(Mono.just(payload), String.class).exchange().expectStatus().isOk();
		});

		// RegistryObject key AUTCollection1R is created and exists in Neo4j
		String cKey = "AUTCollection1R";
		Vertex c = myceliumService.getRegistryObjectVertexFromKey(cKey);
		assertThat(c).isNotNull();
		assertThat(c.getTitle()).isEqualTo("Relatinship scenario 1");
		assertThat(c.getObjectClass()).isEqualTo("collection");

		// C has outbound relationships in neo4j
		Collection<Relationship> Crels = myceliumService.getGraphService()
				.getDuplicateOutboundRelationships(c.getIdentifier(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		assertThat(Crels).isNotNull();

		// C->P(key) exists in neo4j
		String pKey = "http://nla.gov.au/nla.party-AUTR1";
		List<Relationship> CtoP = Crels.stream().filter(relationship -> {
			return relationship.getTo().getIdentifier().equals(pKey);
		}).collect(Collectors.toList());
		assertThat(CtoP.size()).isEqualTo(1);

		// C-[hasCollector]->P in neo4j
		assertThat(CtoP.get(0).getRelations().get(0).getType()).isEqualTo("hasCollector");

		// P -[isCollectorOf]->C(id) in neo4j
		Vertex p = myceliumService.getRegistryObjectVertexFromKey(pKey);
		Collection<Relationship> Prels = myceliumService.getGraphService()
				.getDuplicateOutboundRelationships(p.getIdentifier(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		assertThat(Prels).isNotNull();
		List<Relationship> PtoC = Prels.stream().filter(relationship -> {
			return relationship.getTo().getIdentifier().equals(c.getIdentifier());
		}).collect(Collectors.toList());
		assertThat(PtoC.size()).isEqualTo(1);
		assertThat(PtoC.get(0).getRelations().get(0).getType()).isEqualTo("isCollectorOf");

		// index C
		this.webTestClient.post().uri(
				uriBuilder -> uriBuilder.path(indexRecordAPI).queryParam("registryObjectId", c.getIdentifier()).build())
				.exchange().expectStatus().isOk();

		// index P
		this.webTestClient.post().uri(
				uriBuilder -> uriBuilder.path(indexRecordAPI).queryParam("registryObjectId", p.getIdentifier()).build())
				.exchange().expectStatus().isOk();

		// expects C has relationships in SOLR
		Cursor<RelationshipDocument> cursor = myceliumService.getMyceliumIndexingService()
				.cursorFor(new Criteria("from_id").is(c.getIdentifier()));
		List<RelationshipDocument> FromCRels = new ArrayList<>();
		while (cursor.hasNext()) {
			FromCRels.add(cursor.next());
		}
		assertThat(FromCRels.size()).isEqualTo(1);

		// C-[hasCollector]->P in SOLR
		RelationshipDocument CtoPdoc = FromCRels.get(0);
		assertThat(CtoPdoc.getFromId()).isEqualTo(c.getIdentifier());
		assertThat(CtoPdoc.getToIdentifier()).isEqualTo(p.getIdentifier());
		assertThat(CtoPdoc.getRelations().stream().anyMatch(relation -> {
			return relation.getRelationType().equals("hasCollector");
		})).isTrue();
	}

}
