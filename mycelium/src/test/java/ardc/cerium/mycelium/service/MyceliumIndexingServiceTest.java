package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@Import({ MyceliumIndexingService.class })
class MyceliumIndexingServiceTest {

	@Autowired
	MyceliumIndexingService myceliumIndexingService;

	@MockBean
	SolrTemplate solrTemplate;

	@MockBean
	RelationshipDocumentRepository relationshipDocumentRepository;

	@MockBean
	GraphService graphService;

	@MockBean
	VertexRepository vertexRepository;

	@Test
	void grantsNetworkisReverse() {
		// party
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("party", "collection", "isFunderOf")).isTrue();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("party", "activity", "isFunderOf")).isTrue();

		// activity
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("activity", "funder", "hasFunder")).isFalse();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("activity", "activity", "hasPart")).isTrue();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("activity", "activity", "isPartOf")).isFalse();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("activity", "collection", "hasOutput")).isTrue();

		// collection
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("collection", "funder", "isFundedBy")).isFalse();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("collection", "activity", "isOutputOf")).isFalse();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("collection", "collection", "isPartOf")).isFalse();
		assertThat(myceliumIndexingService.grantsNetworkIsTopDown("collection", "collection", "hasPart")).isTrue();
	}

}