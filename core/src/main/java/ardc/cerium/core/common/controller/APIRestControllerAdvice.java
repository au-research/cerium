package ardc.cerium.core.common.controller;

import ardc.cerium.core.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Locale;

@RestControllerAdvice
public class APIRestControllerAdvice {

	@Autowired
	MessageSource messageSource;

	/**
	 * Handles all NotFound case of the API
	 * @param ex The RuntimeException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { RecordNotFoundException.class, VersionNotFoundException.class })
	public ResponseEntity<Object> handleNotfound(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.NOT_FOUND, request);
		request.setAttribute("ExceptionMessage", message);
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles forbidden 403 operations
	 * @param ex The APIException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { ForbiddenOperationException.class })
	public ResponseEntity<Object> handleForbidden(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.FORBIDDEN, request);
		request.setAttribute("ExceptionMessage", message);
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	/**
	 * Handles forbidden 403 operations
	 * @param ex The APIException that is encountered
	 * @param request the HttpServeletRequest, to display the path
	 * @param locale the injected Locale for the service
	 * @return ResponseEntity
	 */
	@ExceptionHandler(value = { SchemaNotSupportedException.class, VersionContentAlreadyExistsException.class })
	public ResponseEntity<Object> handleBadRequest(APIException ex, HttpServletRequest request, Locale locale) {
		String message = messageSource.getMessage(ex.getMessageID(), ex.getArgs(), locale);
		APIExceptionResponse response = new APIExceptionResponse(message, HttpStatus.BAD_REQUEST, request);
		request.setAttribute("ExceptionMessage", message);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * A global response for all Validation Exception
	 * @param ex handles MethodArgumentNotValidException
	 * @param request The current request
	 * @return APIExceptionResponse
	 */
	@ExceptionHandler(value = { MethodArgumentNotValidException.class })
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		APIExceptionResponse response = new APIExceptionResponse(ex.getMessage());
		response.setTimestamp(new Date());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError(HttpStatus.BAD_REQUEST.toString());
		response.setPath(request.getServletPath());

		// Map<String, String> errors = new HashMap<>();
		// ex.getBindingResult().getAllErrors().forEach((error) -> {
		// String fieldName = ((FieldError) error).getField();
		// String errorMessage = error.getDefaultMessage();
		// errors.put(fieldName, errorMessage);
		// });

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

}
