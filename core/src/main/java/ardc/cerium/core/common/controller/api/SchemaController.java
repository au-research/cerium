package ardc.cerium.core.common.controller.api;

import ardc.cerium.core.exception.XMLValidationException;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Schema API")
@RestController
@RequestMapping("/api/resources/schemas/")
public class SchemaController {

	@Autowired
	SchemaService service;

	@GetMapping("")
	public ResponseEntity<?> index() {
		List<Schema> schemas = service.getSchemas();

		return ResponseEntity.ok().body(schemas);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Schema> show(@PathVariable String id) {
		if (!service.supportsSchema(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " not found or not supported");
		}

		Schema schema = service.getSchemaByID(id);
		return ResponseEntity.ok().body(schema);
	}

	@PostMapping(value = "/{id}/validate")
	public ResponseEntity<?> validateSchema(@PathVariable String id, @RequestBody String payload) throws Exception {

		// TODO handle malformed XML here?
		if (!service.supportsSchema(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " not found or not supported");
		}

		Schema schema = service.getSchemaByID(id);

		try {
			service.validate(schema, payload);
			return ResponseEntity.ok().body(null);
		}
		catch (XMLValidationException e) {
			return ResponseEntity.badRequest().body("Validation Failed: " + e.getMessage());
		}
	}

}
