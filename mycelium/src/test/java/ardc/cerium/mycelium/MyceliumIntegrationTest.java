package ardc.cerium.mycelium;

import ardc.cerium.core.common.repository.RequestRepository;
import ardc.cerium.mycelium.repository.RelationshipDocumentRepository;
import ardc.cerium.mycelium.repository.VertexRepository;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest
@ActiveProfiles("integration")
public abstract class MyceliumIntegrationTest {

    @Autowired
    RequestRepository requestRepository;

    @Autowired
	VertexRepository vertexRepository;

    @Autowired
    RelationshipDocumentRepository relationshipDocumentRepository;

    @AfterEach
    public void tearDown() {
        requestRepository.deleteAll();
        vertexRepository.deleteAll();
        relationshipDocumentRepository.deleteAll();
    }

}
