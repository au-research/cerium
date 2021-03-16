package ardc.cerium.core.exception;

import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

public class APIExceptionResponse {

	private final String message;

	private Date timestamp;

	private int status;

	private String error;

	private String path;

	public APIExceptionResponse(String message) {
		this.message = message;
	}

	public APIExceptionResponse(String message, HttpStatus httpStatus, HttpServletRequest request) {
		this.message = message;
		this.setTimestamp(new Date());
		this.setStatus(httpStatus.value());
		this.setError(httpStatus.toString());
		this.setPath(request.getServletPath());
	}

	public String getMessage() {
		return message;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
