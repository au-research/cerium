package ardc.cerium.oai.controller;

import ardc.cerium.oai.exception.*;
import ardc.cerium.oai.model.ErrorFragment;
import ardc.cerium.oai.model.RequestFragment;
import ardc.cerium.oai.response.OAIExceptionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Locale;

@ControllerAdvice
@ConditionalOnProperty(name = "app.oai.enabled")
public class OAIPMHControllerAdvice {

	@Autowired
	MessageSource messageSource;

	@ExceptionHandler(value = { BadArgumentException.class, BadResumptionTokenException.class,
			CannotDisseminateFormatException.class, IdDoesNotExistException.class, NoRecordsMatchException.class,
			NoSetHierarchyException.class, NoMetadataFormatsException.class, BadVerbException.class })
	public ResponseEntity<Object> handleOAIException(OAIException ex, HttpServletRequest request, Locale locale)
			throws JsonProcessingException {

		OAIExceptionResponse response = new OAIExceptionResponse();

		ErrorFragment errorFragment = new ErrorFragment();
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		errorFragment.setValue(message);
		errorFragment.setCode(ex.getCode());

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		response.setRequest(requestFragment);
		response.setResponseDate(new Date());
		response.setError(errorFragment);

		// add xml declaration
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		// todo try catch here
		String xml = mapper.writeValueAsString(response);

		request.setAttribute("ExceptionMessage", message);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(xml);
	}

}
