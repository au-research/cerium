package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.util.VertexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Service
@Slf4j
@EnableScheduling
public class MyceliumVertexResolvingService {

    @Autowired
    GraphService graphService;

    @Autowired
    VertexRepository vertexRepository;

	/**
	* Resolve all the resolvable vertices
     * scheduled to run monthly
	*/
    @Scheduled(cron = "0 0 0 1 * *")
    public void resolveAllVertices() {

        // dois vertices
        log.info("Resolving all DOI vertices");
        try (Stream<Vertex> stream = graphService.streamVertexByIdentifierType("doi")) {
            stream.forEach(vertex -> this.resolve(vertex));
        }

        // orcid vertices
        log.info("Resolving all ORCID vertices");
        try (Stream<Vertex> stream = graphService.streamVertexByIdentifierType("orcid")) {
            stream.forEach(vertex -> this.resolve(vertex));
        }

    }

    public void resolve(Vertex vertex) {

        if (! shouldResolve(vertex)) {
            return;
        }

        log.debug("Resolving Vertex[identifier={}, type={}]", vertex.getIdentifier(), vertex.getIdentifierType());
        VertexUtil.resolveVertex(vertex);
        graphService.ingestVertex(vertex);
    }

    public boolean shouldResolve(Vertex vertex) {

        // don't resolve if the vertex has been resolved in the last 7 days
        Object lastResolvedValue = vertex.getMetaAttribute("lastResolved");
        if (lastResolvedValue != null) {
            Instant lastResolved = Instant.parse(lastResolvedValue.toString());
            Duration lastResolvedToNow = Duration.between(lastResolved, Instant.now());
            long sevenDaysInSeconds = 60 * 60 * 24 * 7;
            log.debug("LastResolved: {}, DurationLastResolvedToNow[seconds={}]", lastResolved, lastResolvedToNow.getSeconds());
            if (lastResolvedToNow.getSeconds() < sevenDaysInSeconds) {
                log.debug("Vertex[identifier={}, type={}] duration hasn't elapsed. Not resolving", vertex.getIdentifier(), vertex.getIdentifierType());
                return false;
            }
        }

        return true;
    }

}
