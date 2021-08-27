package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({ MyceliumSideEffectService.class })
class MyceliumSideEffectServiceTest {

	@Autowired
	MyceliumSideEffectService myceliumSideEffectService;

	@MockBean
	GraphService graphService;

	@MockBean
	MyceliumIndexingService myceliumIndexingService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@MockBean
	RedissonClient redissonClient;

	@Test
	void detectChanges_TitleChangeSideEffect_NotChanged() {
		RecordState before = new RecordState();
		before.setTitle("Old");

		RecordState after = new RecordState();
		after.setTitle("Old");

		assertThat(myceliumSideEffectService.detectTitleChange(before, after)).isFalse();

		List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
		assertThat(sideEffects.stream().filter(sideEffect -> sideEffect instanceof TitleChangeSideEffect).count())
				.isEqualTo(0);
	}

	@Test
	void detectChanges_TitleChangeSideEffect_Changed() {
		RecordState before = new RecordState();
		before.setTitle("Old");

		RecordState after = new RecordState();
		after.setTitle("New");

		assertThat(myceliumSideEffectService.detectTitleChange(before, after)).isTrue();

		List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
		assertThat(sideEffects.stream().filter(sideEffect -> sideEffect instanceof TitleChangeSideEffect).count())
				.isEqualTo(1);
	}

}