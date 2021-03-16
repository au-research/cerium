package ardc.cerium.igsn.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.exception.APIExceptionResponse;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RequestNotFoundException;
import ardc.cerium.igsn.service.IGSNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.core.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Date;

@RestController
@RequestMapping(value = "/api/resources/igsn-requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Request Resource", description = "API endpoints to interact with IGSN related Requests")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IGSNRequestController {

	final KeycloakService kcService;

	final RequestService requestService;

	final IGSNService igsnService;

	final RequestMapper requestMapper;

	public IGSNRequestController(KeycloakService kcService, RequestService requestService, IGSNService igsnService,
			RequestMapper requestMapper) {
		this.kcService = kcService;
		this.requestService = requestService;
		this.igsnService = igsnService;
		this.requestMapper = requestMapper;
	}

	@PutMapping(value = "/{id}")
	@Operation(summary = "Restart a completed or failed IGSN Request",
			description = "Modifies the status of an existing IGSN request in order to stop or invoke processing. (Only RESTART is supported as of v1.0)<br/>" +
					"The \"<b>id</b>\" of a request can be found as \"<b>id</b>\" in the response body in all ACCEPTED IGSN requests<br/>" +
					"{<br/>" +
					"&nbsp;&nbsp;\"<b>id</b>\": \"<b>b7ffa7ac-3e6c-4aad-81a4-848528b90ddb</b>\",<br/>" +
					"&nbsp;&nbsp;\"status\": \"COMPLETED\",<br/>" +
					"&nbsp;&nbsp;\"type\": \"igsn.bulk-mint\",<br/>" +
					"&nbsp;&nbsp;\"createdBy\": \"b3cc6369-448a-4853-9b9f-2ab56f90a18d\",<br/>" +
					"&nbsp;&nbsp;\"createdAt\": \"2020-12-10T03:48:52.000+00:00\",<br/>" +
					"&nbsp;&nbsp;\"updatedAt\": \"2020-12-10T03:48:59.000+00:00\",<br/>" +
					"&nbsp;&nbsp;\"message\": \"Request completed with some errors\",<br/>" +
					"&nbsp;&nbsp;\"summary\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"TOTAL TIME\": \"0h 0m 7s\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"RECORDS FORBIDDEN\": \"1\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"IGSN REGISTERED\": \"1\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"RECORDS RECEIVED\": \"2\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"PROCESS TIME\": \"0h 0m 0s\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"IMPORT TIME\": \"0h 0m 0s\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"RECORDS CREATED\": \"1\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"REGISTER TIME\": \"0h 0m 7s\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"RECORDS UPDATED\": \"0\",<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"ERROR\": \"1\"<br/>" +
					"&nbsp;&nbsp;},<br/>" +
					"&nbsp;&nbsp;\"_links\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"self\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"href\": \"https://test.identifiers.ardc.edu.au/igsn-registry/api/resources/requests/b7ffa7ac-3e6c-4aad-81a4-848528b90ddb\"<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;},<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"logs\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"href\": \"https://test.identifiers.ardc.edu.au/igsn-registry/api/resources/requests/b7ffa7ac-3e6c-4aad-81a4-848528b90ddb/logs\"<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;},<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"identifiers\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"href\": \"https://test.identifiers.ardc.edu.au/igsn-registry/api/resources/requests/b7ffa7ac-3e6c-4aad-81a4-848528b90ddb/identifiers\"<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;},<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;\"records\": {<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"href\": \"https://test.identifiers.ardc.edu.au/igsn-registry/api/resources/requests/b7ffa7ac-3e6c-4aad-81a4-848528b90ddb/records\"<br/>" +
					"&nbsp;&nbsp;&nbsp;&nbsp;}<br/>" +
					"&nbsp;&nbsp;}<br/>" +
					"}",
			parameters = {@Parameter(name = "status", description = "The state which the Request is set to",
							schema = @Schema(description = "status", type = "string", allowableValues = { "RESTART" }, defaultValue = "RESTART"))},
			responses = {
					@ApiResponse(responseCode = "200", description = "Request is restarting",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "ForbiddenOperationException:<br/>" +
							"User is not the owner of the Request or Request doesn't exist<br/>" +
							"The Request is not Completed either successfully or failed yet<br/>" +
							"The value of the parameter 'status' is not supported (only RESTART is supported as of v1.0)<br/>" +
							"The request type is not IGSN only mint, bulk-mint, update, bulk-update, reserve and transfer requests are Supported",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> update(@PathVariable String id,
			@RequestParam(name = "status") String status, HttpServletRequest httpServletRequest){
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request;
		try {
			request = requestService.findOwnedById(id, user);
		}catch(RequestNotFoundException e ){
			throw new ForbiddenOperationException("Request with UUID " + id + " not found");
		}
		catch(ForbiddenOperationException e ){
			throw e;
		}catch(NumberFormatException e){
			throw new ForbiddenOperationException(e.getMessage());
		}
		Date date = new Date();
		Logger requestLog = requestService.getLoggerFor(request);
		// archive log at log.millieseconds
		// empty the log file
		// set request status to RESTARTED
		// re-run the request
		if(!(request.getStatus().equals(Request.Status.COMPLETED) || request.getStatus().equals(Request.Status.FAILED))){
			throw new ForbiddenOperationException("Only COMPLETED or FAILED Requests can be restarted");
		}
		else if(!status.equals("RESTART")){
			throw new ForbiddenOperationException("Only RESTART is supported");
		}
		else if(!request.getType().startsWith("ardc/cerium/core/igsn")){
			throw new ForbiddenOperationException("Only IGSN requests are supported");
		}
		// all should be ready to proceed
		try {
			requestLog.info("Restarted Request at: " + date);
			String logPath = requestService.getLoggerPathFor(request);
			File logFile = new File(logPath);
			File acrhived = new File(logFile.getParentFile().getPath() + File.separator + "logs." + date.getTime());
			InputStream is = new FileInputStream(logFile);
			OutputStream os = new FileOutputStream(acrhived);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			PrintWriter pw = new PrintWriter(logFile);
			pw.close();
			requestLog.info("Restarting Request at: " + date);
		}
		catch (Exception e) {

			requestLog.info("Couldn't archive existing logs");
		}
		request.setStatus(Request.Status.RESTARTED);
		request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, 0);
		request.setAttribute(Attribute.NUM_OF_RECORDS_CREATED, 0);
		request.setAttribute(Attribute.NUM_OF_RECORDS_UPDATED, 0);
		request.setAttribute(Attribute.NUM_OF_IGSN_REGISTERED, 0);
		request.setAttribute(Attribute.NUM_OF_ERROR, 0);
		request.setAttribute(Attribute.NUM_OF_RECORDS_FORBIDDEN, 0);
		request.setAttribute(Attribute.NUM_OF_FAILED_REGISTRATION, 0);
		igsnService.processMintOrUpdate(request);
		RequestDTO dto = requestMapper.getConverter().convert(request);

		return ResponseEntity.accepted().body(dto);
	}

}
