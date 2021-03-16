package ardc.cerium.core.exception;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

public class APIException extends RuntimeException {

	public APIException() {
		super();
	}

	public APIException(String msg) {
		super(msg);
	}

	public String[] getArgs() {
		return new String[] {};
	}

	public String getMessageID() {
		return "";
	}

	public String getMessage(){
		Locale defaultLocale = Locale.getDefault();
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:messages");
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource.getMessage(getMessageID(), getArgs(), defaultLocale);
	}

}
