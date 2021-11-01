package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/resources/mycelium-datasources", produces = { MediaType.APPLICATION_JSON_VALUE })
public class MyceliumDataSourceResourceController {

	final MyceliumService myceliumService;

	public MyceliumDataSourceResourceController(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
	}

	@GetMapping(value = "")
	public ResponseEntity<List<DataSource>> index() {
		return ResponseEntity.ok().body(myceliumService.getDataSources());
	}

	@GetMapping(value = "/{dataSourceId}")
	public ResponseEntity<DataSource> show(@PathVariable String dataSourceId) {
		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);
		return ResponseEntity.ok(dataSource);
	}

	@PostMapping(value = "")
	public ResponseEntity<DataSource> store(@RequestBody DataSource dto) {
		// todo validation
		myceliumService.importDataSource(dto);
		return ResponseEntity.created(URI.create("/api/resources/mycelium-datasources/" + dto.getId())).body(dto);
	}

	@PutMapping(value = "/{dataSourceId}")
	public ResponseEntity<DataSource> update(@RequestBody DataSource dto) {
		// todo validation
		myceliumService.deleteDataSourceById(dto.getId());
		myceliumService.importDataSource(dto);
		return ResponseEntity.accepted().body(dto);
	}

	@DeleteMapping(value = "/{dataSourceId}")
	public ResponseEntity<?> destroy(@PathVariable String dataSourceId) {
		myceliumService.deleteDataSourceById(dataSourceId);
		return ResponseEntity.accepted().body(null);
	}

}
