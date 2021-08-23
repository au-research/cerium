package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
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

	@Test
	void detectChanges_TitleChangeSideEffect_NotChanged() {
		RecordState before = new RecordState();
		before.setTitle("Old");

		RecordState after = new RecordState();
		after.setTitle("Old");

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

		List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
		assertThat(sideEffects.stream().filter(sideEffect -> sideEffect instanceof TitleChangeSideEffect).count())
				.isEqualTo(1);
	}

	@Test
	void handleSideEffect_shouldCallHandleMethod() {
		// given a list of sideEffect with only 1 side effect (we spy on it)
		TitleChangeSideEffect sideEffect = spy(new TitleChangeSideEffect("1", "Old Title", "New Title"));
		List<SideEffect> sideEffects = new ArrayList<>(List.of(sideEffect));

		// make sure sideEffect do nothing when handled (we don't test that here)
		doNothing().when(sideEffect).handle();

		// when handleSideEffects
		myceliumSideEffectService.handleSideEffects(sideEffects);

		// the handle method on the sideEffect is called once
		verify(sideEffect, times(1)).handle();
	}

}