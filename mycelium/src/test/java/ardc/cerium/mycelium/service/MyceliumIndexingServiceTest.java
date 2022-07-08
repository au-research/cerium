package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.filter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

	@Test
	void indexVertexCallsOtherMethods() {
		MyceliumIndexingService mockedService = Mockito.mock(MyceliumIndexingService.class);
		doCallRealMethod().when(mockedService).indexVertex(any(Vertex.class));
		Vertex vertex = new Vertex();
		mockedService.indexVertex(vertex);
		verify(mockedService, times(1)).deleteAllRelationship(vertex);
		verify(mockedService, times(1)).indexDirectRelationships(vertex);
		verify(mockedService, times(1)).indexGrantsNetworkRelationships(vertex);
	}

	@Test
	void deleteAllRelationship() {
		Vertex vertex = new Vertex();
		myceliumIndexingService.deleteAllRelationship(vertex);
		verify(relationshipDocumentRepository, times(1)).deleteAllByFromIdEquals(any());
		verify(relationshipDocumentRepository, times(1)).deleteAllByToIdentifierEquals(any());
	}

	@Test
	void deleteAllDataSourceRelationship() {
		Vertex vertex = new Vertex();
		myceliumIndexingService.deleteAllDataSourceRelationship("1");
		verify(relationshipDocumentRepository, times(1)).deleteAllByFromDataSourceId("1");
		verify(relationshipDocumentRepository, times(1)).deleteAllByToDataSourceId("1");
	}

	@Test
	void regenGrantsNetworkRelationships_DRAFT() {
		Vertex vertex = new Vertex();
		vertex.setStatus(Vertex.Status.DRAFT);

		MyceliumIndexingService mockedService = Mockito.mock(MyceliumIndexingService.class);
		doCallRealMethod().when(mockedService).regenGrantsNetworkRelationships(any(Vertex.class));

		mockedService.regenGrantsNetworkRelationships(vertex);
		verify(mockedService, times(0)).deleteGrantsNetworkEdges(vertex);
		verify(mockedService, times(0)).indexGrantsNetworkRelationships(vertex);
	}

	@Test
	void regenGrantsNetworkRelationships_PUBLISHED() {
		Vertex vertex = new Vertex();
		vertex.setStatus(Vertex.Status.PUBLISHED);

		MyceliumIndexingService mockedService = Mockito.mock(MyceliumIndexingService.class);
		doCallRealMethod().when(mockedService).regenGrantsNetworkRelationships(any(Vertex.class));

		mockedService.regenGrantsNetworkRelationships(vertex);
		verify(mockedService, times(1)).deleteGrantsNetworkEdges(vertex);
		verify(mockedService, times(1)).indexGrantsNetworkRelationships(vertex);
	}

	@Test
	void cursorFor() {
		Cursor<RelationshipDocument> cursor = myceliumIndexingService.cursorFor(new Criteria("from_id").is("1"));
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(solrTemplate, times(1)).queryForCursor(any(), queryCaptor.capture(), any());
		Query query = queryCaptor.getValue();
		assertThat(query.getSort()).isEqualTo(Sort.by("id"));
		assertThat(query.getProjectionOnFields().contains(new SimpleField("*"))).isTrue();
		// todo query.getFilterQueries contains from_id:1
		// assertThat(query.getFilterQueries().contains(new SimpleFilterQuery(new Criteria("from_id").is("1")))).isTrue();
	}

	@Test
	void findExistingRelationshipDocumentFromIDtoID() {
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		RelationshipDocument doc = myceliumIndexingService.findExistingRelationshipDocument("1", "2");
		verify(solrTemplate, times(1)).queryForObject(any(), queryCaptor.capture(), any());
		Query query = queryCaptor.getValue();

		List<Criteria> criterias = query.getFilterQueries().stream().map(filterQuery -> {
			return filterQuery.getCriteria();
		}).collect(Collectors.toList());

		assertThat(criterias.stream().filter(criteria -> {
			return criteria.getField().getName().equals("from_id");
		}).findFirst().orElse(null)).isNotNull();
		assertThat(criterias.stream().filter(criteria -> {
			return criteria.getField().getName().equals("to_identifier");
		}).findFirst().orElse(null)).isNotNull();
	}

	@Test
	void findExistingRelationshipDocumentFromID() {
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		RelationshipDocument doc = myceliumIndexingService.findExistingRelationshipDocument("1");
		verify(solrTemplate, times(1)).queryForObject(any(), queryCaptor.capture(), any());
		Query query = queryCaptor.getValue();

		List<Criteria> criterias = query.getFilterQueries().stream().map(filterQuery -> {
			return filterQuery.getCriteria();
		}).collect(Collectors.toList());

		assertThat(criterias.stream().filter(criteria -> {
			return criteria.getField().getName().equals("from_id");
		}).findFirst().orElse(null)).isNotNull();
		assertThat(criterias.stream().filter(criteria -> {
			return criteria.getField().getName().equals("to_identifier");
		}).findFirst().orElse(null)).isNull();
	}
}