package ardc.cerium.igsn.controller;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.model.schema.JSONSchema;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.igsn.exception.IGSNNoValidContentForSchema;
import ardc.cerium.igsn.exception.IGSNNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/public/igsn-description")
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Description Public API")
public class IGSNDescriptionPublicController {

	final IdentifierService identifierService;

	final VersionService versionService;

	final SchemaService schemaService;

	public IGSNDescriptionPublicController(IdentifierService identifierService, VersionService versionService,
			SchemaService schemaService) {
		this.identifierService = identifierService;
		this.versionService = versionService;
		this.schemaService = schemaService;
	}

	@GetMapping("")
	@Operation(summary = "Get an IGSN Descriptive content given the identifier",
			description = "Return the content of the version associate with the record for a given identifier")
	@PageableOperation
	public ResponseEntity<?> index(@RequestParam(name = "identifier") String identifierValue,
			@RequestParam(required = false, defaultValue = SchemaService.ARDCv1) String schema) {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		Record record = identifier.getRecord();
		Version version = versionService.findVersionForRecord(record, schema);
		if (version == null) {
			throw new IGSNNoValidContentForSchema(identifierValue, schema);
		}

		Schema schemaObject = schemaService.getSchemaByID(schema);

		MediaType mediaType = MediaType.APPLICATION_XML;
		if (schemaObject.getClass().equals(JSONSchema.class)) {
			mediaType = MediaType.APPLICATION_JSON;
		}

		return ResponseEntity.ok().contentType(mediaType).body(version.getContent());
	}

}
