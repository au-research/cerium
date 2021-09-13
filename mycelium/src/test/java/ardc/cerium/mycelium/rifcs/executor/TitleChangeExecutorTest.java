package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TitleChangeExecutorTest {

	@MockBean
	MyceliumService myceliumService;

	@Test
	void detect_TitleChangeSideEffect_NotChanged() {
		RecordState before = new RecordState();
		before.setTitle("Old");
		RecordState after = new RecordState();
		after.setTitle("Old");

		assertThat(TitleChangeExecutor.detect(before, after, myceliumService)).isFalse();
	}

	@Test
	void detect_TitleChangeSideEffect_Changed() {
		RecordState before = new RecordState();
		before.setTitle("Old");
		RecordState after = new RecordState();
		after.setTitle("New");

		assertThat(TitleChangeExecutor.detect(before, after, myceliumService)).isTrue();
	}
}