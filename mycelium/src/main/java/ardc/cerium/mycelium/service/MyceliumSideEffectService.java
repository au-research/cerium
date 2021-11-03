package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.*;
import ardc.cerium.mycelium.rifcs.executor.*;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.util.DataSourceUtil;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class MyceliumSideEffectService {

	public static final String QUEUE_PREFIX = "mycelium.affected.queue";

	public static final String REQUEST_ATTRIBUTE_REQUEST_ID = "SIDE_EFFECT_REQUEST_ID";

	private final RedissonClient redissonClient;

	private MyceliumService myceliumService;

	private MyceliumRequestService myceliumRequestService;

	public void setMyceliumService(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
		myceliumRequestService = myceliumService.getMyceliumRequestService();
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

	public void queueSideEffects(Request request, List<SideEffect> sideEffects) {
		String requestId = request.getAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID);
		String queueID = getQueueID(requestId);
		sideEffects.forEach(sideEffect -> {
			addToQueue(queueID, sideEffect);
			log.debug("Added Side Effect[class={}] to Queue[queueID={}]", sideEffect.getClass(), queueID);
		});
	}

	@Async
	public void workQueue(String queueID) {
		log.debug("Start working RQueue[id={}]", queueID);
		RQueue<SideEffect> queue = getQueue(queueID);
		while (!queue.isEmpty()) {
			SideEffect sideEffect = queue.poll();
			Executor executor = ExecutorFactory.get(sideEffect, myceliumService);

			if (executor == null) {
				log.error("No executor found for sideEffect[class={}]", sideEffect.getClass());
				continue;
			}

			log.debug("Executing sideEffect[class={}] with executor[class={}]", sideEffect.getClass(),
					executor.getClass());
			executor.handle();
		}
		log.info("Finish working RQueue[id={}]", queueID);
	}

	@Async
	public void workQueue(String queueID, Request request) {
		this.workQueue(queueID);

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

		if (DuplicateInheritanceExecutor.detect(before, after, myceliumService)) {
			log.debug("Detected DuplicateInheritanceSideEffect");
			sideEffects.add(new DuplicateInheritanceSideEffect(after.getRegistryObjectId()));
		}

		// detect if a record title is updated (record is not created)
		if (before != null && TitleChangeExecutor.detect(before, after, myceliumService)) {
			sideEffects
					.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
		}

		if (before != null && GrantsNetworkForgoExecutor.detect(before, after, myceliumService)) {
			sideEffects.add(new GrantsNetworkForgoSideEffect(before.getRegistryObjectId(), before.getRegistryObjectKey(), before.getRegistryObjectClass()));
		}

		if (GrantsNetworkInheritenceExecutor.detect(before, after, myceliumService)) {
			sideEffects.add(new GrantsNetworkInheritenceSideEffect(after.getRegistryObjectId(),
					after.getRegistryObjectClass()));
		}

		return sideEffects;
	}

	public List<SideEffect> detectChanges(DataSource before, DataSource after) {
		List<SideEffect> sideEffects = new ArrayList<>();

		if (PrimaryKeyDeletionExecutor.detect(before, after, myceliumService)) {
			List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(after, before);
			differences.forEach(pk -> {
				Vertex roVertex = myceliumService.getRegistryObjectVertexFromKey(pk.getKey());
				if (roVertex != null) {
					String registryObjectId = roVertex.getIdentifier();
					SideEffect effect = new PrimaryKeyDeletionSideEffect(before.getId(), pk.getKey(), registryObjectId);
					sideEffects.add(effect);
				}
			});
		}

		if (PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)) {
			List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(before, after);
			differences.forEach(pk -> sideEffects.add(new PrimaryKeyAdditionSideEffect(before.getId(), pk)));
		}

		return sideEffects;
	}

}
