package ardc.cerium.core.common.controller.api;

import ardc.cerium.core.common.dto.UserDTO;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "About Me", description = "Display information about the current logged in user")
@RequestMapping(value = "/api/me/", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class MeController {

	@Autowired
	KeycloakService kcService;

	@GetMapping(value = "")
	@Operation(summary = "Describes the current logged in user", description = "Retrieve the current user details")
	@ApiResponse(responseCode = "200")
	public ResponseEntity<UserDTO> whoami(HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);

		ModelMapper mapper = new ModelMapper();
		UserDTO dto = mapper.map(user, UserDTO.class);
		return ResponseEntity.ok(dto);
	}

}
