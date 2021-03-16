package ardc.cerium.core.common.dto.mapper;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RequestMapper.class, ModelMapper.class })
class RequestMapperTest {

	@Autowired
	RequestMapper requestMapper;

	@Test
	void convertToEntity() {
		// given a dto, convert to a request retains data
		RequestDTO dto = new RequestDTO();
		dto.setType("ardc.cerium.core.igsn-import");

		Request actual = requestMapper.getConverter().reverse().convert(dto);

		assertThat(actual.getType()).isEqualTo("ardc.cerium.core.igsn-import");
	}

	@Test
	void convertSummary(){
		String summary = "TOTAL TIME: 0h 0m 14s, IMPORT TIME: 0h 0m 0s, REGISTER TIME: 0h 0m 8s,  RECORDS UPDATED:0,  RECORDS CREATED:1,  IGSN REGISTERED:1,  ERROR:0,  RECORDS RECEIVED:1,";
		Request request = TestHelper.mockRequest();
		request.setSummary(summary);
		RequestDTO dto = requestMapper.getConverter().convert(request);
		assert dto != null;
		assertThat(dto.getSummary().getClass().getName().equals(HashMap.class.getName()));

	}


}