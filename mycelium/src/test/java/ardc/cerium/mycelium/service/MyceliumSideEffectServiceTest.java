package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DCIRelationChangeSideEffect;
import ardc.cerium.mycelium.rifcs.effect.ScholixRelationChangeSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.executor.Executor;
import ardc.cerium.mycelium.rifcs.executor.ExecutorFactory;
import ardc.cerium.mycelium.rifcs.executor.PrimaryKeyDeletionExecutor;
import ardc.cerium.mycelium.rifcs.executor.ScholixRelationChangeExecutor;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({MyceliumSideEffectService.class})
class MyceliumSideEffectServiceTest {

    @Autowired
    MyceliumSideEffectService myceliumSideEffectService;

    @MockBean
    RedissonClient redissonClient;

    @MockBean
    GraphService graphService;

    @MockBean
    MyceliumService myceliumService;

    @MockBean
    MyceliumRequestService myceliumRequestService;

    @MockBean
    RequestService requestService;

    @BeforeEach
	void setUp() {
        when(myceliumService.getMyceliumRequestService()).thenReturn(myceliumRequestService);
        when(myceliumService.getGraphService()).thenReturn(graphService);
        when(myceliumRequestService.getRequestService()).thenReturn(requestService);
        when(requestService.getLoggerFor(any(Request.class))).thenReturn(Mockito.mock(Logger.class));
	}

    @Test
	void setMyceliumService() {
        myceliumSideEffectService.setMyceliumService(myceliumService);
        assertThat(myceliumSideEffectService.getGraphService()).isEqualTo(graphService);
        assertThat(myceliumSideEffectService.getMyceliumRequestService()).isEqualTo(myceliumRequestService);
	}

    @Test
	void getQueueID() {
        assertThat(myceliumSideEffectService.getQueueID("test-queue")).isEqualTo("mycelium.queue.test-queue");
	}

    @Test
	void getQueue() {
        myceliumSideEffectService.getQueue("id");
        verify(redissonClient).getQueue("id");
	}

    @Test
	void addToQueue() {
        RQueue queue = Mockito.mock(RQueue.class);
        when(redissonClient.getQueue("id")).thenReturn(queue);
        myceliumSideEffectService.addToQueue("id", new ScholixRelationChangeSideEffect("1"));
        verify(queue, times(1)).add(any(SideEffect.class));
	}

    @Test
	void queueSideEffects() {
        UUID requestUUID = UUID.randomUUID();
        Request request = new Request();
        request.setId(requestUUID);

        String queueID = myceliumSideEffectService.getQueueID(request.getId().toString());
        RQueue queue = Mockito.mock(RQueue.class);
        when(redissonClient.getQueue(queueID)).thenReturn(queue);

        myceliumSideEffectService.queueSideEffects(request, Arrays.asList(
                new ScholixRelationChangeSideEffect("1"),
                new DCIRelationChangeSideEffect("1")
        ));

        verify(queue, times(2)).add(any(SideEffect.class));
	}

    /**
     * RandomSideEffect to test workQueue
     */
    private class RandomSideEffect extends SideEffect {

    };

    @Test
	void workQueue() {
        UUID requestUUID = UUID.randomUUID();
        Request request = new Request();
        request.setId(requestUUID);

        myceliumSideEffectService.setMyceliumService(myceliumService);

        String queueID = myceliumSideEffectService.getQueueID(request.getId().toString());

        RQueue queue = Mockito.mock(RQueue.class);
        when(redissonClient.getQueue(queueID)).thenReturn(queue);
        SideEffect sideEffect = new ScholixRelationChangeSideEffect("1");
        when(queue.poll())
                .thenReturn(sideEffect)
                .thenReturn(new RandomSideEffect())
                .thenReturn(null);
        when(queue.isEmpty())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        when(myceliumRequestService.save(request)).thenReturn(request);

        Executor executor = Mockito.mock(ScholixRelationChangeExecutor.class);
        try (MockedStatic<ExecutorFactory> utilities = Mockito.mockStatic(ExecutorFactory.class)){
            utilities.when(() -> ExecutorFactory.get(sideEffect, myceliumService)).thenReturn(executor);
            myceliumSideEffectService.workQueue(queueID, request);
            verify(executor, times(1)).handle();
        }
	}

}