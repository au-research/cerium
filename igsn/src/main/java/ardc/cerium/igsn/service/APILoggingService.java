package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.User;
import ardc.cerium.igsn.entity.IGSNEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static ardc.cerium.core.common.util.Helpers.getClientIpAddress;

/**
 * A Service to enable logging specific for API requests and response
 */
@Service
public class APILoggingService {

	private final Logger log = LogManager.getLogger(APILoggingService.class);

	public static final String ExceptionMessage = "ExceptionMessage";

	/**
	 * Log the request and response.
	 *
	 * Using ECS Specification for layout. Uses log4j2 as the Logger Implementation to log
	 * structured data
	 * @see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-reference.html">ECS *
	 * specification</a>
	 * @param request the current request
	 * @param response the current response
	 */
	public void log(HttpServletRequest request, HttpServletResponse response) {
		ObjectNode ecs = getLoggableMessage(request, response);
		log.info(new ObjectMessage(ecs));
	}

	/**
	 * Return a ECS structure ready for logging
	 *
	 * @see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-reference.html">ECS
	 * specification</a>
	 * @param httpServletRequest the current request
	 * @param response the current request
	 * @return ObjectNode representing the ECS jackson ObjectNode
	 */
	public ObjectNode getLoggableMessage(HttpServletRequest httpServletRequest, HttpServletResponse response) {
		ObjectMapper mapper = new ObjectMapper();

		ObjectNode ecs = mapper.createObjectNode();

		// client
		// client.address can contain comma separated list of IP
		// client.ip should contain the FIRST IP Address in that comma separated list
		ObjectNode client = mapper.createObjectNode();
		String clientAddress = getClientIpAddress(httpServletRequest);
		client.put("address", clientAddress);
		client.put("ip", clientAddress.split(",")[0].trim());

		// client.user
		User user = (User) httpServletRequest.getAttribute(String.valueOf(User.class));
		if (user != null) {
			ObjectNode userNode = mapper.createObjectNode();
			userNode.put("email", user.getEmail());
			userNode.put("id", user.getId().toString());
			userNode.put("name", user.getUsername());
			userNode.set("roles", mapper.valueToTree(user.getRoles()));
			client.set("user", userNode);
		}

		ecs.set("client", client);

		// url
		String uri = httpServletRequest.getRequestURI();
		String fullURL = httpServletRequest.getRequestURL().toString();

		// if this is a forwarded Error Request, we'll have to rebuild the full URL
		if (httpServletRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) {
			uri = (String) httpServletRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
			try {
				URL rebuiltURL = new URL(httpServletRequest.getRequestURL().toString());
				String host = rebuiltURL.getHost();
				String userInfo = rebuiltURL.getUserInfo();
				String scheme = rebuiltURL.getProtocol();
				int port = rebuiltURL.getPort();
				String query = (String) httpServletRequest.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);
				URI rebuiltURI = new URI(scheme, userInfo, host, port, uri, query, null);
				fullURL = rebuiltURI.toString();
			}
			catch (MalformedURLException | URISyntaxException ignored) {

			}
		}
		ObjectNode url = mapper.createObjectNode();
		url.put("path", uri);
		url.put("full", fullURL);

		// parse the full URL to fill out the other bits
		try {
			URL parsedURL = new URL(fullURL);
			url.put("scheme", parsedURL.getProtocol());
			url.put("port", parsedURL.getPort());
		}
		catch (MalformedURLException ignored) {

		}

		if (httpServletRequest.getQueryString() != null) {
			url.put("query", httpServletRequest.getQueryString());
		}

		ecs.set("url", url);

		// http
		ObjectNode http = mapper.createObjectNode();

		// http.request
		ObjectNode httpRequest = mapper.createObjectNode();
		httpRequest.put("method", httpServletRequest.getMethod());
		String referrer = httpServletRequest.getHeader("referrer");
		if (referrer != null) {
			httpRequest.put("referrer", referrer);
		}
		http.set("request", httpRequest);

		// http.response
		ObjectNode httpResponse = mapper.createObjectNode();
		httpResponse.put("status_code", String.valueOf(response.getStatus()));
		http.set("response", httpResponse);

		ecs.set("http", http);

		// useragent
		ObjectNode userAgent = mapper.createObjectNode();
		userAgent.put("original", httpServletRequest.getHeader("User-Agent"));
		ecs.set("user_agent", userAgent);

		// service
		ObjectNode service = mapper.createObjectNode();
		service.put("type", "igsn");
		service.put("name", "igsn-registry");
		service.put("kind", "event");
		ecs.set("service", service);

		// custom metadata_registry fields
		ObjectNode metadataRegistry = mapper.createObjectNode();
		Request request = (Request) httpServletRequest.getAttribute(String.valueOf(Request.class));
		if (request != null) {
			metadataRegistry.set("request", mapper.valueToTree(request));
		}
		ecs.set("metadata_registry", metadataRegistry);

		// event
		ObjectNode event = mapper.createObjectNode();
		event.put("category", "web");
		event.put("action", determineEventAction(httpServletRequest));
		String outcome = (response.getStatus() < 200 || response.getStatus() > 299) ? "failure" : "success";
		event.put("outcome", outcome);
		ecs.set("event", event);

		// message
		String defaultMessage = String.format("%s %s %s", httpServletRequest.getMethod(), uri, response.getStatus());
		String exceptionMessage = (String) httpServletRequest.getAttribute(ExceptionMessage);
		ecs.put("message",
				exceptionMessage != null && !exceptionMessage.trim().equals("") ? exceptionMessage : defaultMessage);

		return ecs;
	}

	/**
	 * Determine the ECS field event.action based on the current request
	 *
	 * By default, returns "api". If there's an {@link Request} in the request, the action
	 * will be in the associated {@link IGSNEventType}
	 * @see <a href=
	 * "https://www.elastic.co/guide/en/ecs/current/ecs-event.html">ecs-event</a>
	 * @param request the current {@link HttpServletRequest}
	 * @return the String representation of the event.action value
	 */
	private String determineEventAction(@NotNull HttpServletRequest request) {

		// default action would be api
		String action = "api";

		// if it's an IGSN request, then the action is the type
		Request igsn = (Request) request.getAttribute(String.valueOf(Request.class));
		if (igsn != null) {
			action = igsn.getType();
		}

		return action;

	}

}
