package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.*;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class ExecutorFactoryTest {

    @MockBean
    MyceliumService myceliumService;

    @Test
    void get() {
        assertThat(ExecutorFactory.get(Mockito.mock(DuplicateInheritanceSideEffect.class), myceliumService))
				.isInstanceOf(DuplicateInheritanceExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(TitleChangeSideEffect.class), myceliumService))
                .isInstanceOf(TitleChangeExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(GrantsNetworkForgoSideEffect.class), myceliumService))
                .isInstanceOf(GrantsNetworkForgoExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(GrantsNetworkInheritenceSideEffect.class), myceliumService))
                .isInstanceOf(GrantsNetworkInheritenceExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(PrimaryKeyAdditionSideEffect.class), myceliumService))
                .isInstanceOf(PrimaryKeyAdditionExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(PrimaryKeyDeletionSideEffect.class), myceliumService))
                .isInstanceOf(PrimaryKeyDeletionExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(RelatedInfoRealisationSideEffect.class), myceliumService))
                .isInstanceOf(RelatedInfoRealisationExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(DuplicateForgoSideEffect.class), myceliumService))
                .isInstanceOf(DuplicateForgoExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(IdentifierForgoSideEffect.class), myceliumService))
                .isInstanceOf(IdentifierForgoExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(DirectRelationshipChangedSideEffect.class), myceliumService))
                .isInstanceOf(DirectRelationshipChangedExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(DCIRelationChangeSideEffect.class), myceliumService))
                .isInstanceOf(DCIRelationChangeExecutor.class);
        assertThat(ExecutorFactory.get(Mockito.mock(ScholixRelationChangeSideEffect.class), myceliumService))
                .isInstanceOf(ScholixRelationChangeExecutor.class);
        assertThat(ExecutorFactory.get(null, myceliumService)).isNull();
    }
}