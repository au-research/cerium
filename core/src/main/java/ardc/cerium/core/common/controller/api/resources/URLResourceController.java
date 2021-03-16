package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.common.dto.URLDTO;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.exception.APIExceptionResponse;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.URLService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/urls", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "URL Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class URLResourceController {

	@Autowired
	private URLService service;

	@Autowired
	private KeycloakService kcService;

	@GetMapping("")
	@Operation(summary = "Get all URLs",
			description = "Retrieves all url resources that the current user has access to")
	@ApiResponse(responseCode = "200",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = URL.class))))
	public ResponseEntity<?> index() {
		// todo obtain user from the kcService and find owned from said user
		// todo pagination
		List<URL> urls = service.findOwned();

		return ResponseEntity.ok(urls);
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single URL by id", description = "Retrieve the metadata for a single url by id")
	@ApiResponse(responseCode = "404", description = "URL is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "URL is found",
			content = @Content(schema = @Schema(implementation = URL.class)))
	public ResponseEntity<?> show(@Parameter(required = true, description = "the id of the URL (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		URL url = service.findById(id);
		if (url == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL " + id + " is not found");
		}

		return ResponseEntity.ok().body(url);
	}

	@PostMapping("/")
	@Operation(summary = "Creates a new URL", description = "Add a new URL to the registry")
	@ApiResponse(responseCode = "201", description = "URL is created",
			content = @Content(schema = @Schema(implementation = URL.class)))
	public ResponseEntity<?> store(@RequestBody URLDTO urlDTO, HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		URLDTO resultDTO = service.create(urlDTO, user);
		URI location = URI.create("/api/resources/urls/" + resultDTO.getId());
		return ResponseEntity.created(location).body(resultDTO);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a URL by ID", description = "Update an existing URL")
	@ApiResponse(responseCode = "202", description = "URL is updated",
			content = @Content(schema = @Schema(implementation = URL.class)))
	@ApiResponse(responseCode = "404", description = "URL is not found")
	public ResponseEntity<?> update(@PathVariable String id, @RequestBody URL updatedURL, HttpServletRequest request) {
		// ensure record exists
		if (!service.exists(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL " + id + " is not found");
		}

		Record record = updatedURL.getRecord();

		// todo validate record ownership

		// todo validate user permission to update record

		URL updated = service.update(updatedURL);

		return ResponseEntity.ok().body(updated);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a URL by ID", description = "Delete a URL from the registry")
	@ApiResponse(responseCode = "202", description = "URL is deleted")
	@ApiResponse(responseCode = "404", description = "URL is not found")
	public ResponseEntity<?> destroy(@PathVariable String id) {
		// todo DELETE a resource should always delete it
		if (!service.exists(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL " + id + " is not found");
		}

		URL url = service.findById(id);

		service.delete(id);

		return ResponseEntity.accepted().body(url);
	}

}
