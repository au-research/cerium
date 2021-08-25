package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumSideEffectService {

	public static final String QUEUE_PREFIX = "mycelium.affected.queue";

	private final GraphService graphService;

	private final MyceliumIndexingService myceliumIndexingService;

	private final RedissonClient redissonClient;

	public MyceliumSideEffectService(GraphService graphService, MyceliumIndexingService indexingService, RedissonClient redissonClient) {
		this.graphService = graphService;
		this.myceliumIndexingService = indexingService;
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
	public void workQueue(String queueID) {
		RQueue<SideEffect> queue = getQueue(queueID);
		while (!queue.isEmpty()) {
			SideEffect sideEffect = queue.poll();
			Executor executor = ExecutorFactory.get(sideEffect, graphService, myceliumIndexingService);

			if (executor == null) {
				log.error("No executor found for sideEffect[class={}]", sideEffect.getClass());
				continue;
			}
			log.debug("Found Executor for sideEffect[class={}]: executor[class={}]", sideEffect.getClass(), executor.getClass());

			executor.handle();
		}
		log.info("Finished Handling this queue");
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
			sideEffects.add(
					new DuplicateInheritanceSideEffect(after.getRegistryObjectId()));
		}

		if (before != null && detectTitleChange(before, after)) {
			sideEffects
					.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
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
		return !before.getTitle().equals(after.getTitle());
	}

}
