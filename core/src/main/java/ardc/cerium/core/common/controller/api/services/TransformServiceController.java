package ardc.cerium.core.common.controller.api.services;

import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.exception.SchemaNotSupportedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/services/transform")
public class TransformServiceController {

	@Autowired
	SchemaService schemaService;

	@PostMapping("")
	public ResponseEntity<?> transform(@RequestParam String fromSchemaID, @RequestParam String toSchemaID,
			@RequestBody String payload) {
		// validate fromSchema and toSchema
		Schema fromSchema = schemaService.getSchemaByID(fromSchemaID);
		if (fromSchema == null) {
			throw new SchemaNotSupportedException(fromSchemaID);
		}
		Schema toSchema = schemaService.getSchemaByID(toSchemaID);
		if (toSchema == null) {
			throw new SchemaNotSupportedException(toSchemaID);
		}

		// attempt to create the transformer
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		if (transformer == null) {
			throw new RuntimeException("Transformer not found");
		}

		// wrap the payload into a Version
		Version inputVersion = new Version();
		inputVersion.setSchema(fromSchema.getId());
		inputVersion.setContent(payload.getBytes());

		// interrogate the outputVersion
		Version outputVersion = transformer.transform(inputVersion);

		return ResponseEntity.ok(outputVersion.getContent());
	}

}
