package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RelationTypeGroup;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.VertexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/mycelium-identifier-objects",
        produces = { "application/json", "application/vnd.ardc.vertex.ro+json" })
@Slf4j
@RequiredArgsConstructor
public class MyceliumIdentifierResourceController {

    private final MyceliumService myceliumService;

    /**
     * Obtain a Graph for an Identifier Vertex
     * @param identifier_value the value of the identifier
     * @param identifier_type the type of the identifier
     * @return a string response
     */
    @GetMapping(path = "/graph")
    public ResponseEntity<?> getIdentifierGraph(
    @RequestParam("identifier_value") String identifier_value,
    @RequestParam("identifier_type") String identifier_type,
    @RequestParam(defaultValue = "true") boolean includeReverseExternal,
    @RequestParam(defaultValue = "true") boolean includeReverseInternal,
    @RequestParam(defaultValue = "true") boolean includeDuplicates,
    @RequestParam(defaultValue = "true") boolean includeGrantsNetwork,
    @RequestParam(defaultValue = "true") boolean includeInterLinking,
    @RequestParam(defaultValue = "true") boolean includeCluster) {
        log.info("Obtaining graph for [identifier={},type={}]", identifier_value, identifier_type);
        Vertex vertex = myceliumService.getGraphService().getVertexByIdentifier(identifier_value, identifier_type);

        if (vertex == null) {
            log.error("Vertex with identifier {} type {} doesn't exist", identifier_value, identifier_type);
            return ResponseEntity.badRequest()
                    .body(String.format("Vertex with identifier_value%s, identifier_type %s doesn't exist", identifier_value, identifier_type));
        }
            GraphService graphService = myceliumService.getGraphService();

            // todo includeReverseExternal=true
            // todo includeReverseInternal=true

            // obtain the immediate relationships, the grants network relationships as graphs
            // and merge the graph together
            Graph graph = new Graph();
            graph.addVertex(vertex);

            // relationTypeGrouping (clustering)
            Collection<RelationTypeGroup> relationTypeGroups = graphService.getRelationTypeGrouping(vertex);
            List<RelationTypeGroup> overLimitGroups = relationTypeGroups.stream().filter(g -> g.getCount() >= 20)
                    .collect(Collectors.toList());

            List<String> overLimitRelationType = new ArrayList<>();
            if (includeCluster) {
                overLimitGroups.forEach(group -> {
                    Vertex cluster = new Vertex(UUID.randomUUID().toString(), "ro:cluster");
                    cluster.setId(new Random().nextLong());
                    cluster.addLabel(Vertex.Label.Cluster);
                    group.getLabels().forEach(cluster::addLabel);
                    cluster.setObjectClass(group.getObjectClass());
                    cluster.setObjectType(group.getObjectType());
                    graph.addVertex(cluster);
                    graph.addEdge(new Edge(vertex, cluster, group.getRelation(), new Random().nextLong()));
                });
                overLimitRelationType = overLimitGroups.stream().map(RelationTypeGroup::getRelation)
                        .collect(Collectors.toList());
                log.debug("OverlimitRelationType: {}", overLimitRelationType);
            }

            // add the immediate relationships (include Duplicate), excludes the
            // overLimitRelationTypes
            graph.mergeGraph(graphService.getRegistryObjectGraph(vertex, overLimitRelationType));

            log.debug("Added registryObjectGraph Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
                    graph.getEdges().size());

            // add the GrantsNetworkPath
            if (includeGrantsNetwork) {
                graph.mergeGraph(graphService.getGrantsNetworkGraphUpwards(vertex, overLimitRelationType));
                log.debug("Added grantsNetworkgraphUpwards Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
                        graph.getEdges().size());

                graph.mergeGraph(graphService.getGrantsNetworkDownwards(vertex, overLimitRelationType));
                log.debug("Added grantsNetworkgraphDownwards Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
                        graph.getEdges().size());
            }

            // manually add the Duplicates into the Graph
            if (includeDuplicates) {
                Collection<Vertex> duplicateRegistryObjects = graphService.getDuplicateRegistryObject(vertex);
                duplicateRegistryObjects.stream().filter(v -> !v.getId().equals(vertex.getId())).forEach(duplicate -> {
                    graph.addVertex(duplicate);
                    log.debug("Adding duplicates to Graph {}, edges:{}", vertex.getIdentifier(), vertex.getIdentifierType());
                    graph.addEdge(new Edge(vertex, duplicate, RIFCSGraphProvider.RELATION_SAME_AS));
                });
                log.debug("Added duplicateGraph Graph[vertex: {}]", duplicateRegistryObjects.size());
            }


            if(vertex.getStatus() != null && vertex.getStatus().equals(Vertex.Status.DRAFT.name())){
                // remove its PUBLISHED for start
                Collection<Vertex> altVersions = graphService.getAltStatusRecord(vertex, Vertex.Status.PUBLISHED.name());
                graph.removeAll(altVersions);
                altVersions = new ArrayList<>(Collections.emptySet());
                for (Vertex v : graph.getVertices()) {
                    // remove all DRAFT versions if their PUBLISHED are in the graph
                    if (v.getStatus() != null && v.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
                        altVersions.addAll(graphService.getAltStatusRecord(v, Vertex.Status.DRAFT.name()));
                    }
                }
                graph.removeAll(altVersions);
            }

            // interlinking between current graph vertices
            if (includeInterLinking) {
                List<Vertex> otherDirectlyRelatedVertices = graph.getVertices().stream()
                        .filter(v -> !v.getIdentifier().equals(vertex.getIdentifier())).collect(Collectors.toList());

                log.debug("OtherDirectlyRelatedVertices count:{}", otherDirectlyRelatedVertices.size());
                graph.mergeGraph(graphService.getGraphBetweenVertices(otherDirectlyRelatedVertices));
                log.debug("Added interlinkingGraph Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
                        graph.getEdges().size());
            }

            // clean up the data

            graphService.removeDanglingVertices(graph);
            log.debug("Removed dangling vertices. Prepare to render graph");

            return ResponseEntity.ok(graph);

        }


    @PostMapping(path = "/update-title")
    public ResponseEntity<?> UpdateTitle(
            @RequestParam("identifier_value") String identifier_value,
            @RequestParam("identifier_type") String identifier_type,
            @RequestParam("title") String title){
        log.info("OUpdating title for [identifier={},type={},title{}]", identifier_value, identifier_type, title);


        String normalisedType = VertexUtil.getNormalisedIdentifierType(identifier_value, identifier_type);
        String normalisedValue = VertexUtil.getNormalisedIdentifierValue(identifier_value, normalisedType);

        Vertex vertex = myceliumService.getGraphService().getVertexByIdentifier(normalisedValue, normalisedType);

        if (vertex == null) {
            log.error("Vertex with identifier {} type {} doesn't exist", identifier_value, identifier_type);
            return ResponseEntity.badRequest()
                    .body(String.format("Vertex with identifier_value%s, identifier_type %s doesn't exist", identifier_value, identifier_type));
        }
        vertex.setTitle(title);
        vertex.setListTitle(title);
        myceliumService.getGraphService().ingestVertex(vertex);
        return ResponseEntity.ok("Title updated");
    }
}
