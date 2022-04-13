package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TitleChangeExecutorTest {

	@MockBean
	MyceliumService myceliumService;

	@Test
	void detect_TitleChangeSideEffect_NotChanged() {
		RecordState before = new RecordState();
		before.setTitle("Old");
		before.setStatus("PUBLISHED");
		RecordState after = new RecordState();
		after.setTitle("Old");
		after.setStatus("PUBLISHED");

		assertThat(TitleChangeExecutor.detect(before, after, myceliumService)).isFalse();
	}

	@Test
	void detect_TitleChangeSideEffect_Changed() {
		RecordState before = new RecordState();
		before.setTitle("Old");
		before.setStatus("PUBLISHED");
		RecordState after = new RecordState();
		after.setTitle("New");
		after.setStatus("PUBLISHED");
		assertThat(TitleChangeExecutor.detect(before, after, myceliumService)).isTrue();
	}

	@Test
	void detect_TitleChangeSideEffect_Changed_but_Draft() {
		RecordState before = new RecordState();
		before.setTitle("Old");
		before.setStatus("DRAFT");
		RecordState after = new RecordState();
		after.setTitle("New");
		after.setStatus("DRAFT");
		assertThat(TitleChangeExecutor.detect(before, after, myceliumService)).isFalse();
	}
}