package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.Neo4jTest;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import({ MyceliumService.class, GraphService.class, VertexMapper.class, ModelMapper.class, EdgeDTOMapper.class })
public class MyceliumServiceSearchTest extends Neo4jTest {

    @Autowired
    MyceliumService myceliumService;

    @Autowired
    GraphService graphService;

    @MockBean
    RequestService requestService;

    @Test
    @DisplayName("A search provides a good set of Pagination data")
	void genericSearch() throws IOException {
        // given an ingest
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        myceliumService.ingest(rifcs);

        // when search
        List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(new SearchCriteria("fromIdentifierValue", "AUTestingRecords2DCIRecords6", SearchOperation.EQUAL));
        criteriaList.add(new SearchCriteria("fromIdentifierType", "ro:key", SearchOperation.EQUAL));
        Page<Relationship> relationships = myceliumService.search(criteriaList, PageRequest.of(0, 5));
        assertThat(relationships).isNotNull();

        // expects relationships to be a page, the from & to are Vertices
        assertThat(relationships).isInstanceOf(PageImpl.class);
        assertThat(relationships.getContent().size()).isGreaterThan(0);
        assertThat(relationships.getContent().get(0).getFrom()).isInstanceOf(Vertex.class);
        assertThat(relationships.getContent().get(0).getFrom().getIdentifier()).isEqualTo("AUTestingRecords2DCIRecords6");
        assertThat(relationships.getContent().get(0).getFrom().getIdentifierType()).isEqualTo(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
        assertThat(relationships.getContent().get(0).getTo()).isInstanceOf(Vertex.class);
        assertThat(relationships.getContent().get(0).getRelations().size()).isGreaterThan(0);
        assertThat(relationships.getContent().get(0).getRelations().get(0)).isInstanceOf(EdgeDTO.class);
	}

    @Test
    @DisplayName("isSameAs to identifier relation is excluded from searching by default")
	void isSameAsMustNotReturn() throws IOException {
        // given an ingest
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        myceliumService.ingest(rifcs);

        // when search
        List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(new SearchCriteria("fromIdentifierValue", "AUTestingRecords2DCIRecords6", SearchOperation.EQUAL));
        criteriaList.add(new SearchCriteria("fromIdentifierType", "ro:key", SearchOperation.EQUAL));
        Page<Relationship> relationships = myceliumService.search(criteriaList, PageRequest.of(0, 5));
        assertThat(relationships).isNotNull();

        // relation to local identifier AODN must not return
        assertThat(relationships.getContent().stream().filter(relationship -> relationship.getTo().getIdentifier().contains("AODN")).findAny().orElse(null)).isNull();
        assertThat(relationships).isNotNull();
    }
}
