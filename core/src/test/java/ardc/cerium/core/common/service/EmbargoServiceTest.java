package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.EmbargoRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class EmbargoServiceTest {

	@InjectMocks
	private EmbargoService embargoService;

	@Mock
	private EmbargoRepository embargoRepository;

	@Mock
	private RecordRepository recordRepository;

	@BeforeEach
	void setUp() {
		embargoRepository.flush();
		embargoRepository.deleteAll();
		embargoRepository.flush();

		recordRepository.flush();
		recordRepository.deleteAll();
		recordRepository.flush();
	}

	@Test
	void findAllEmbargoEnd() {

		Date embargoEnd = Helpers.convertDate("2020-10-27");
		List<Embargo> embargoMock = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			Record record1 = TestHelper.mockRecord(UUID.randomUUID());
			recordRepository.save(record1);
			Embargo embargo1 = TestHelper.mockEmbargo(record1);
			embargo1.setEmbargoEnd(embargoEnd);
			embargo1.setRecord(record1);
			embargoMock.add(embargo1);
		}

		Date newDate = new Date();
		Mockito.when(embargoRepository.findAllByEmbargoEndLessThanEqual(newDate)).thenReturn(embargoMock);
		List<Embargo> embargos = embargoService.findAllEmbargoToEnd(newDate);
		assertThat(embargos.size()).isEqualTo(10);
	}

	@Test
	@DisplayName("Saving embargo calls repository.saveAndFlush")
	void save() {
		embargoService.save(TestHelper.mockEmbargo());
		verify(embargoRepository, times(1)).saveAndFlush(any(Embargo.class));
	}

	@Test
	@DisplayName("Saving embargo calls repository.saveAndFlush")
	void findByRecord() {

		Date embargoEnd = Helpers.convertDate("2020-10-27");

		Record record  = TestHelper.mockRecord(UUID.randomUUID());
		recordRepository.save(record );
		Embargo embargo = TestHelper.mockEmbargo(record );
		embargo.setEmbargoEnd(embargoEnd);
		embargo.setRecord(record);

		Embargo found = embargoService.findByRecord(record);

		verify(embargoRepository, times(1)).findByRecord(any(Record.class));
	}
}
