package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.service.MyceliumBackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumBackupResourceController.class)
class MyceliumBackupResourceControllerTest {

    public static String END_POINT = "/api/resources/mycelium-backups";

    @MockBean
    MyceliumBackupService backupService;

    @Autowired
    MockMvc mockMvc;

    @Test
	void createsBackupWithPost() throws Exception {
        // @formatter:off
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(END_POINT)
                .queryParam("backupId", "test-backup")
                .queryParam("dataSourceId", "1")
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isOk());
        // @formatter:on

        verify(backupService, times(1)).createBackup("test-backup", "1");
	}

    @Test
	void restoreBackupWithPost() throws Exception {
        // @formatter:off
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(END_POINT + "/test-backup/_restore" )
                .queryParam("dataSourceId", "1")
                .queryParam("correctedDataSourceId", "2")
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isOk());
        // @formatter:on

        verify(backupService, times(1)).restoreBackup("test-backup", "1", "2");
	}
}