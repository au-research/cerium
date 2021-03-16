package ardc.cerium.igsn.config;

import ardc.cerium.core.exception.*;
import ardc.cerium.igsn.exception.IGSNNoValidContentForSchema;
import ardc.cerium.igsn.exception.IGSNNotFoundException;
import ardc.cerium.igsn.service.APILoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@RestControllerAdvice
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNControllerAdvice {

	@Autowired
	MessageSource messageSource;

	/**
	 * Handles Record Not Found
	 * @param ex The APIException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { IGSNNotFoundException.class, IGSNNoValidContentForSchema.class })
	public ResponseEntity<Object> handleNotfound(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.NOT_FOUND, request);
		request.setAttribute(APILoggingService.ExceptionMessage, message);
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles Validation exceptions
	 * @param ex The APIException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { XMLValidationException.class, JSONValidationException.class })
	public ResponseEntity<Object> handleInvalidPayload(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.BAD_REQUEST, request);
		request.setAttribute(APILoggingService.ExceptionMessage, message);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles Content Not supported Exceptions
	 * @param ex The APIException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { ContentNotSupportedException.class, ContentProviderNotFoundException.class })
	public ResponseEntity<Object> handleContentNotSupported(APIException ex, HttpServletRequest request,
			Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.BAD_REQUEST, request);
		request.setAttribute(APILoggingService.ExceptionMessage, message);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

}
