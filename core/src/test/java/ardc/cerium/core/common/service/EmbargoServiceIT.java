package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.EmbargoRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class EmbargoServiceIT{

    @Autowired
    EmbargoService embargoService;

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    EmbargoRepository embargoRepository;

    @AfterEach
    void cleanUp() {
        try {
            embargoRepository.deleteAll();
            recordRepository.deleteAll();

        }
        catch(Exception e)
        {
            System.out.print(e.getMessage());
        }
    }
    @Test
    void endAllEmbargoEnd() {

        int year = Calendar.getInstance().get(Calendar.YEAR);

        Date embargoEnd = Helpers.convertDate(Integer.toString(year - 1) +"-10-27");
        Date embargoEnd2 = Helpers.convertDate(Integer.toString(year + 1) +"-10-27");
        for (int i = 0; i < 4; i++) {
            Record record = TestHelper.mockRecord();
            record.setVisible(false);
            recordRepository.saveAndFlush(record);
            Embargo embargo = TestHelper.mockEmbargo(record);
            embargo.setEmbargoEnd(embargoEnd);
            embargoRepository.saveAndFlush(embargo);
         }

        for (int i = 0; i < 4; i++) {
            Record record = TestHelper.mockRecord();
            record.setVisible(false);
            recordRepository.saveAndFlush(record);
            Embargo embargo = TestHelper.mockEmbargo(record);
            embargo.setEmbargoEnd(embargoEnd2);
            embargoRepository.saveAndFlush(embargo);
        }

        Date newDate = new Date();
        assertThat(embargoRepository.findAll().size()).isEqualTo(8);
        assertThat(embargoService.findAllEmbargoToEnd(newDate).size()).isEqualTo(4);

        List<Record> records = recordRepository.findAll();
        for(Record record : records){
            assertThat(record.isVisible()).isEqualTo(false);
        }

        //Run the service to release the Embargo
        embargoService.endEmbargoList();

        assertThat(embargoRepository.findAll().size()).isEqualTo(4);
        assertThat(embargoService.findAllEmbargoToEnd(newDate).size()).isEqualTo(0);
        for(Embargo embargo : embargoRepository.findAll()){
            Record record = embargo.getRecord();
            assertThat(record.isVisible()).isEqualTo(false);
        }
    }

}
