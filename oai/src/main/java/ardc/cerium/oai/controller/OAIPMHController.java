package ardc.cerium.oai.controller;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.oai.exception.BadVerbException;
import ardc.cerium.oai.exception.NoSetHierarchyException;
import ardc.cerium.oai.model.RequestFragment;
import ardc.cerium.oai.response.OAIResponse;
import ardc.cerium.oai.service.OAIPMHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping(value = "/api/services/oai-pmh", produces = MediaType.APPLICATION_XML_VALUE)
@ConditionalOnProperty(name = "app.oai.enabled")
public class OAIPMHController {

	@Autowired
	SchemaService schemaService;

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	OAIPMHService oaipmhService;

	@GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<OAIResponse> handle(HttpServletRequest request,
			@RequestParam(required = false, defaultValue = "") String verb,
			@RequestParam(required = false) String identifier, @RequestParam(required = false) String metadataPrefix,
			@RequestParam(required = false) String resumptionToken, @RequestParam(required = false) String from,
			@RequestParam(required = false) String until, @RequestParam(required = false) String set)
			throws IOException {

		if (!oaipmhService.isValidVerb(verb))
			throw new BadVerbException();

		if (set != null)
			throw new NoSetHierarchyException();

		RequestFragment requestFragment = new RequestFragment();
		requestFragment.setValue(request.getRequestURL().toString());
		requestFragment.setVerb(verb);

		OAIResponse response = new OAIResponse();
		switch (OAIPMHService.Verb.valueOf(verb.toUpperCase())) {
		case IDENTIFY:
			response = oaipmhService.identify();
			break;
		case GETRECORD:
			response = oaipmhService.getRecord(metadataPrefix, identifier);
			requestFragment.setIdentifier(identifier);
			requestFragment.setMetadataPrefix(metadataPrefix);
			break;
		case LISTRECORDS:
			response = oaipmhService.listRecords(metadataPrefix, resumptionToken, from, until);
			requestFragment.setMetadataPrefix(metadataPrefix);
			break;
		case LISTIDENTIFIERS:
			response = oaipmhService.listIdentifiers(metadataPrefix, resumptionToken, from, until);
			break;
		case LISTMETADATAFORMATS:
			response = oaipmhService.listMetadataFormats();
			break;
		case LISTSETS:
			response = oaipmhService.listSets();
			break;
		}

		response.setRequest(requestFragment);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(response);
	}

}
