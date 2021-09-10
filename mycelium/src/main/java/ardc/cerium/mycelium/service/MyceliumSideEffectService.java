package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.rifcs.effect.GrantsNetworkForgoSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import ardc.cerium.mycelium.rifcs.executor.Executor;
import ardc.cerium.mycelium.rifcs.executor.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumSideEffectService {

	public static final String QUEUE_PREFIX = "mycelium.affected.queue";

	public static final String REQUEST_ATTRIBUTE_REQUEST_ID = "SIDE_EFFECT_REQUEST_ID";

	private final GraphService graphService;

	private final MyceliumIndexingService myceliumIndexingService;

	private final MyceliumRequestService myceliumRequestService;

	private final RedissonClient redissonClient;

	public MyceliumSideEffectService(GraphService graphService, MyceliumIndexingService indexingService, MyceliumRequestService myceliumRequestService, RedissonClient redissonClient) {
		this.graphService = graphService;
		this.myceliumIndexingService = indexingService;
		this.myceliumRequestService = myceliumRequestService;
		this.redissonClient = redissonClient;
	}

	public String getQueueID(String requestId) {
		return String.format("%s.%s", QUEUE_PREFIX, requestId);
	}

	public RQueue<SideEffect> getQueue(String queueID) {
		return redissonClient.getQueue(queueID);
	}

	public void addToQueue(String queueID, SideEffect sideEffect) {
		getQueue(queueID).add(sideEffect);
	}

	@Async
	public void workQueue(String queueID, Request request) {
		log.debug("Start working RQueue[id={}]", queueID);
		RQueue<SideEffect> queue = getQueue(queueID);
		while (!queue.isEmpty()) {
			SideEffect sideEffect = queue.poll();
			Executor executor = ExecutorFactory.get(sideEffect, graphService, myceliumIndexingService);

			if (executor == null) {
				log.error("No executor found for sideEffect[class={}]", sideEffect.getClass());
				continue;
			}

			log.debug("Executing sideEffect[class={}] with executor[class={}]", sideEffect.getClass(), executor.getClass());
			executor.handle();
		}
		log.info("Finish working RQueue[id={}]", queueID);

		request.setStatus(Request.Status.COMPLETED);
		myceliumRequestService.save(request);
	}

	/**
	 * Detect changes
	 *
	 * TODO add Request tracking
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public List<SideEffect> detectChanges(RecordState before, RecordState after) {
		List<SideEffect> sideEffects = new ArrayList<>();

		// this shouldn't happen
		if (before == null && after == null) {
			return sideEffects;
		}

		if (detectDuplicateInheritanceSideEffect(before, after)) {
			sideEffects.add(new DuplicateInheritanceSideEffect(after.getRegistryObjectId()));
		}

		// detect if a record title is updated (record is not created)
		if (before != null && detectTitleChange(before, after)) {
			sideEffects
					.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
		}

		if (before != null && detectGrantsNetworkForegoSideEffect(before, after)) {
			sideEffects.add(new GrantsNetworkForgoSideEffect(before.getRegistryObjectId()));
		}

		return sideEffects;
	}

	/**
	 * Detect if {@link DuplicateInheritanceSideEffect} is applicable
	 *
	 * A record is considered to have duplicate inheritance is when it is created, and
	 * brings with it inheritable relationships to its duplicates
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public boolean detectDuplicateInheritanceSideEffect(RecordState before, RecordState after) {

		// only applies for record creation
		// todo investigate if this is necessary/applicable?
		if (before != null) {
			return false;
		}

		// if the afterStates have duplicate registryObject
		Vertex afterVertex = graphService.getVertexByIdentifier(after.getRegistryObjectId(),
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Collection<Vertex> myDuplicates = graphService.getSameAsRegistryObject(afterVertex).stream()
				.filter(v -> !v.getIdentifier().equals(afterVertex.getIdentifier())).collect(Collectors.toList());
		if (myDuplicates.size() == 0) {
			// only applicable if the record has duplicates
			return false;
		}

		Collection<Relationship> directOutbounds = graphService
				.getDirectOutboundRelationships(afterVertex.getIdentifier(), afterVertex.getIdentifierType());
		// only applicable if the record has direct outbound links
		return directOutbounds.size() != 0;
	}

	/**
	 * Detect if {@link TitleChangeSideEffect} is applicable
	 *
	 * This {@link SideEffect} is applicable when the after {@link RecordState} has a
	 * different title than the before state
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public boolean detectTitleChange(RecordState before, RecordState after) {
		return after != null && !before.getTitle().equals(after.getTitle());
	}

	/**
	 * Detect if {@link GrantsNetworkForgoSideEffect} is applicable
	 *
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public boolean detectGrantsNetworkForegoSideEffect(RecordState before, RecordState after) {
		// the record is deleted and the Before state relationships contain grants network relations
		if (after != null && before.getOutbounds().stream().anyMatch(MyceliumSideEffectService::isGrantsNetwork)) {
			return true;
		}

		// the record is updated, and the relations differences contain grants network relations
		List<Relationship> differences = getRelationshipsDifferences(before, after);
		return differences.stream().anyMatch(MyceliumSideEffectService::isGrantsNetwork);
	}

	/**
	 * Get a List of Relationships that are in from but not in to
	 *
	 * @param from the {@link RecordState} for from
	 * @param to the {@link RecordState} for to
	 * @return the differences comparing first to second
	 */
	public static List<Relationship> getRelationshipsDifferences(RecordState from, RecordState to) {

		if (to == null) {
			return new ArrayList<>(from.getOutbounds());
		}

		return from.getOutbounds().stream().filter(relationship -> !to.getOutbounds().contains(relationship))
				.collect(Collectors.toList());
	}

	/**
	 * Check if a Relationship is part of a grants network
	 * @param relationship the relationship to check
	 * @return true if the relationship is part of a grants network
	 */
	public static boolean isGrantsNetwork(Relationship relationship) {

		Set<String> relationTypes = relationship.getRelations().stream().map(EdgeDTO::getType)
				.collect(Collectors.toSet());

		String fromClass = relationship.getFrom().getObjectClass();
		String toClass = relationship.getTo().getObjectClass();

		// collection isPartOf collection
		// collection hasPart collection
		if (fromClass.equals("collection") && toClass.equals("collection")) {
			return relationTypes.contains("isPartOf") || relationTypes.contains("hasPart");
		}

		// collection isProducedBy activity
		// collection isOutputOf activity
		if (fromClass.equals("collection") && toClass.equals("activity")) {
			return relationTypes.contains("isProducedBy") || relationTypes.contains("isOutputOf");
		}

		// collection isFundedBy party
		if (fromClass.equals("collection") && toClass.equals("party")) {
			return relationTypes.contains("isFundedBy");
		}

		// activity produces collection
		// activity hasOutput collection
		if (fromClass.equals("activity") && toClass.equals("collection")) {
			return relationTypes.contains("produces") || relationTypes.contains("hasOutput");
		}

		// activity isPartOf activity
		// activity hasPart activity
		if (fromClass.equals("activity") && toClass.equals("activity")) {
			return relationTypes.contains("isPartOf") || relationTypes.contains("hasPart");
		}

		// activity isFundedBy party
		if (fromClass.equals("activity") && toClass.equals("party")) {
			return relationTypes.contains("isFundedBy");
		}

		// party isFunderOf collection
		// party isFunderOf activity
		if (fromClass.equals("party") && (toClass.equals("collection") || toClass.equals("activity"))) {
			return relationTypes.contains("isFunderOf");
		}

		return false;
	}

}
