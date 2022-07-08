package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.util.VertexUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.awt.print.Pageable;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({MyceliumVertexResolvingService.class})
class MyceliumVertexResolvingServiceTest {

    @Autowired
    MyceliumVertexResolvingService service;

    @MockBean
    GraphService graphService;

    @Test
	void shouldResolve() {
        Vertex vertex = new Vertex();
        assertThat(service.shouldResolve(vertex)).isTrue();

        vertex.setMetaAttribute("lastResolved", Instant.now().toString());
        assertThat(service.shouldResolve(vertex)).isFalse();
	}

    @Test
	void resolve() {
        Vertex vertex = new Vertex();
        try (MockedStatic<VertexUtil> util = Mockito.mockStatic(VertexUtil.class)) {
            util.when(() -> VertexUtil.resolveVertex(any(Vertex.class))).thenAnswer(invocationOnMock -> null);
            service.resolve(vertex);
            util.verify(() -> VertexUtil.resolveVertex(vertex));
            verify(graphService, times(1)).ingestVertex(vertex);
        }
	}

    @Test
	void resolveAllVertices() {
        service.resolveAllVertices();
        verify(graphService, times(1)).streamVertexByIdentifierType("doi");
        verify(graphService, times(1)).streamVertexByIdentifierType("orcid");
	}

    @Test
	void resolveCallsShouldResolve() {
        Vertex vertex = new Vertex();
        MyceliumVertexResolvingService mockedService = Mockito.mock(MyceliumVertexResolvingService.class);
        doCallRealMethod().when(mockedService).resolve(any(Vertex.class));
        mockedService.resolve(vertex);
        verify(mockedService, times(1)).shouldResolve(vertex);
	}
}