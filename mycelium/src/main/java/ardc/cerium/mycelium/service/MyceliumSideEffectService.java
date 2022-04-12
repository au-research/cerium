package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.*;
import ardc.cerium.mycelium.rifcs.executor.*;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.util.DataSourceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class MyceliumSideEffectService {

	public static final String QUEUE_PREFIX = "mycelium.queue";

	public static final String REQUEST_ATTRIBUTE_REQUEST_ID = "SIDE_EFFECT_REQUEST_ID";

	private final RedissonClient redissonClient;

	private GraphService graphService;

	private MyceliumService myceliumService;

	private MyceliumRequestService myceliumRequestService;

	public void setMyceliumService(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
		myceliumRequestService = myceliumService.getMyceliumRequestService();
		graphService = myceliumService.getGraphService();
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
		String queueID = getQueueID(request.getId().toString());
		sideEffects.forEach(sideEffect -> {
			addToQueue(queueID, sideEffect);
			log.debug("Added Side Effect[class={}] to Queue[queueID={}]", sideEffect.getClass(), queueID);
		});
	}

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

	public void workQueue(String queueID, Request request) {

		request.setStatus(Request.Status.RUNNING);
		myceliumRequestService.save(request);

		Logger requestLogger = myceliumService.getMyceliumRequestService().getRequestService().getLoggerFor(request);
		RQueue<SideEffect> queue = getQueue(queueID);
		requestLogger.info("Started working Queue[id={}, size={}]", queueID, queue.size());

		while (!queue.isEmpty()) {
			SideEffect sideEffect = queue.poll();

			Executor executor = ExecutorFactory.get(sideEffect, myceliumService);

			if (executor == null) {
				log.error("No executor found for sideEffect[class={}]", sideEffect.getClass());
				continue;
			}

			requestLogger.info("Handling SideEffect[class={}] with executor[class={}]", sideEffect.getClass(), executor.getClass());
			executor.handle();
		}
		requestLogger.info("Finished queue processing Queue[id={}]", queueID);

		request.setStatus(Request.Status.COMPLETED);
		myceliumRequestService.save(request);

		// todo callback when the queue finished depends on the request
	}

	@Async
	public void workQueueAsync(String queueID, Request request) {
		this.workQueue(queueID, request);
	}

	@Async
	public void workQueueAsync(String queueID, Request request, ApplicationEvent event) {
		workQueue(queueID, request);
		myceliumService.publishEvent(event);
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

		// when a registryObject is deleted, and it happens to be a PrimaryKey in a dataSource
		if (PrimaryKeyDeletionExecutor.detect(before, after, myceliumService)) {
			String dataSourceId = before.getDataSourceId();
			DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);
			String registryObjectKey = before.getRegistryObjectKey();
			dataSource.getPrimaryKeySetting().getPrimaryKeys().stream()
					.filter(primaryKey -> primaryKey.getKey().equals(registryObjectKey)).findFirst()
					.ifPresent(pk -> {
						String registryObjectId = before.getRegistryObjectId();
						String registryObjectClass = before.getRegistryObjectClass();
						if (pk.getRelationTypeFromActivity() != null) {
							sideEffects.add(new PrimaryKeyDeletionSideEffect(dataSourceId, registryObjectKey,
									registryObjectId, registryObjectClass, pk.getRelationTypeFromActivity()));
						}
						if (pk.getRelationTypeFromParty() != null) {
							sideEffects.add(new PrimaryKeyDeletionSideEffect(dataSourceId, registryObjectKey,
									registryObjectId, registryObjectClass, pk.getRelationTypeFromParty()));
						}
						if (pk.getRelationTypeFromService() != null) {
							sideEffects.add(new PrimaryKeyDeletionSideEffect(dataSourceId, registryObjectKey,
									registryObjectId, registryObjectClass, pk.getRelationTypeFromService()));
						}
						if (pk.getRelationTypeFromCollection() != null) {
							sideEffects.add(new PrimaryKeyDeletionSideEffect(dataSourceId, registryObjectKey,
									registryObjectId, registryObjectClass, pk.getRelationTypeFromCollection()));
						}
					});
		}

		if (RelatedInfoRealisationExecutor.detect(before, after, myceliumService)) {
			List<Vertex> realisedIdentifiers = RelatedInfoRealisationExecutor.getRealisedIdentifiers(before, after, myceliumService);
			for (Vertex realisedIdentifier : realisedIdentifiers) {
				sideEffects.add(new RelatedInfoRealisationSideEffect(after.getRegistryObjectId(),
						realisedIdentifier.getIdentifier(),
						realisedIdentifier.getIdentifierType(),
						after.getTitle(),
						after.getRegistryObjectClass(),
						after.getRegistryObjectType()));
			}
		}

		// when a registryObject is created, and it happens to be a PrimaryKey in a dataSource
		if (PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)) {
			DataSource dataSource = myceliumService.getDataSourceById(after.getDataSourceId());
			dataSource.getPrimaryKeySetting().getPrimaryKeys().stream()
					.filter(primaryKey -> primaryKey.getKey().equals(after.getRegistryObjectKey())).findFirst()
					.ifPresent(pk -> sideEffects.add(new PrimaryKeyAdditionSideEffect(after.getDataSourceId(), pk)));
		}

		if (DuplicateForgoExecutor.detect(before, after, myceliumService)) {
			List<String> priorRegistryObjectIds = before.getIdentical().stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
					.map(Vertex::getIdentifier)
					.filter(identifier -> !identifier.equals(before.getRegistryObjectId()))
					.collect(Collectors.toList());
			sideEffects.add(new DuplicateForgoSideEffect(priorRegistryObjectIds));
		}

		if (DuplicateInheritanceExecutor.detect(before, after, myceliumService)) {
			sideEffects.add(new DuplicateInheritanceSideEffect(after.getRegistryObjectId()));
		}

		if (before != null && GrantsNetworkForgoExecutor.detect(before, after, myceliumService)) {
			sideEffects.add(new GrantsNetworkForgoSideEffect(before.getRegistryObjectId(),
					before.getRegistryObjectKey(), before.getRegistryObjectClass()));
		}

		if (GrantsNetworkInheritenceExecutor.detect(before, after, myceliumService)) {
			sideEffects.add(new GrantsNetworkInheritenceSideEffect(after.getRegistryObjectId(),
					after.getRegistryObjectClass()));
		}

// RDA-492 the related Objects have keys not ids so we must find the ids to modify their portal indexes
		if(DirectRelationshipChangedExecutor.detect(before, after , myceliumService)){
			if(after != null) {
				Collection<Relationship> relAfter = after.getOutbounds();
				for (Relationship rel : relAfter) {
					if ((before == null || !before.getOutbounds().contains(rel))) {
						Optional<Vertex> ro = graphService.getSameAsIdentifierWithType(rel.getTo().getIdentifier(),
								rel.getTo().getIdentifierType(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
						String action = "add";
						ro.ifPresent(vertex -> {
							log.debug("MyceliumSideEffectService add direct to:{}, status:{}",
									vertex.getIdentifier(), vertex.getStatus());
							if(vertex.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
								ArrayList<String> relationTypes = new ArrayList<>();
								rel.getRelations().forEach(relation -> {
									relationTypes.add(relation.getType());
								});
								sideEffects.add(new DirectRelationshipChangedSideEffect(
										after.getRegistryObjectId(), vertex.getIdentifier(), action, after.getRegistryObjectClass(),
										after.getRegistryObjectType(), after.getTitle(), StringUtils.join(relationTypes, ",")));
							}
						});
					}
				}
			}
			if(before != null) {
				Collection<Relationship> relBefore = before.getOutbounds();
				for (Relationship rel : relBefore) {
					if ((after == null || !after.getOutbounds().contains(rel))) {
						Optional<Vertex> ro = graphService.getSameAsIdentifierWithType(rel.getTo().getIdentifier(),
								rel.getTo().getIdentifierType(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
						String action = "remove";
						ro.ifPresent(vertex -> {
							log.debug("MyceliumSideEffectService remove direct to:{}, status:{}",
									vertex.getIdentifier(), vertex.getStatus());
							if(vertex.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
								ArrayList<String> relationTypes = new ArrayList<>();
								rel.getRelations().forEach(relation -> {
									relationTypes.add(relation.getType());
								});
								sideEffects.add(new DirectRelationshipChangedSideEffect(
										before.getRegistryObjectId(), vertex.getIdentifier(), action, before.getRegistryObjectClass(),
										before.getRegistryObjectType(), before.getTitle(), StringUtils.join(relationTypes, ",")));
							}
						});
					}
				}
			}
		}

		// find all Identifiers that the RegistryObject doesn't have anymore and create an IdentifierForgoSideEffect
		if (IdentifierForgoExecutor.detect(before, after)) {
			String registryObjectId = before.getRegistryObjectId();
			for (Vertex vIdentifier : before.getIdentifiers()) {
				if(after == null || after.getIdentifiers().isEmpty() || !after.getIdentifiers().contains(vIdentifier)) {
					log.debug("missing Identifier found {}", vIdentifier.getIdentifier());
					sideEffects.add(new IdentifierForgoSideEffect(registryObjectId,
							vIdentifier.getIdentifier(),
							vIdentifier.getIdentifierType(),
							before.getTitle(),
							before.getRegistryObjectClass(),
							before.getRegistryObjectType()));
				}
			}
		}

		// IMPORTANT to leave the title change side effect as last, and every other side
		// detect if a record title is updated (record is not created)
		if (before != null && TitleChangeExecutor.detect(before, after, myceliumService)) {
			sideEffects
					.add(new TitleChangeSideEffect(before.getRegistryObjectId(),
							before.getRegistryObjectClass(), before.getRegistryObjectType(),
							before.getTitle(), after.getTitle(), null));
		}
		
		return sideEffects;
	}

	/**
	 * Detect Changes for {@link DataSource}
	 * @param before the {@link DataSource} state before the mutation
	 * @param after the {@link DataSource} state after the mutation
	 * @return a {@link List} of {@link SideEffect} to be executed
	 */
	public List<SideEffect> detectChanges(DataSource before, DataSource after) {
		List<SideEffect> sideEffects = new ArrayList<>();

		// when a DataSourceSettings changed and PrimaryKey were removed
		if (PrimaryKeyDeletionExecutor.detect(before, after, myceliumService)) {
			List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(after, before);
			differences.forEach(pk -> {
				Vertex roVertex = myceliumService.getRegistryObjectVertexFromKey(pk.getKey());
				if (roVertex == null) {
					return;
				}

				if (pk.getRelationTypeFromCollection() != null) {
					sideEffects.add(new PrimaryKeyDeletionSideEffect(before.getId(), pk.getKey(),
							roVertex.getIdentifier(), roVertex.getObjectClass(), pk.getRelationTypeFromCollection()));
				}
				if (pk.getRelationTypeFromService() != null) {
					sideEffects.add(new PrimaryKeyDeletionSideEffect(before.getId(), pk.getKey(),
							roVertex.getIdentifier(), roVertex.getObjectClass(), pk.getRelationTypeFromService()));
				}
				if (pk.getRelationTypeFromParty() != null) {
					sideEffects.add(new PrimaryKeyDeletionSideEffect(before.getId(), pk.getKey(),
							roVertex.getIdentifier(), roVertex.getObjectClass(), pk.getRelationTypeFromParty()));
				}
				if (pk.getRelationTypeFromActivity() != null) {
					sideEffects.add(new PrimaryKeyDeletionSideEffect(before.getId(), pk.getKey(),
							roVertex.getIdentifier(), roVertex.getObjectClass(), pk.getRelationTypeFromActivity()));
				}

			});
		}

		// when a DataSourceSetting changed and PrimaryKey are added
		// applicable also when a DataSource is created or first imported
		if (PrimaryKeyAdditionExecutor.detect(before, after, myceliumService)) {
			List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(before, after);
			differences.forEach(pk -> sideEffects.add(new PrimaryKeyAdditionSideEffect(before.getId(), pk)));
		}

		return sideEffects;
	}

}
