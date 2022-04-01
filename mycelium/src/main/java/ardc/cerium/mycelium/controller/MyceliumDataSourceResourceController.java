package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.task.DeleteDataSourceTask;
import ardc.cerium.mycelium.task.ImportDataSourceTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping(value = "/api/resources/mycelium-datasources", produces = { MediaType.APPLICATION_JSON_VALUE })
public class MyceliumDataSourceResourceController {

	final MyceliumService myceliumService;

	public MyceliumDataSourceResourceController(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
	}

	@GetMapping(value = "")
	public ResponseEntity<List<DataSource>> index() {
		log.info("Showing all DataSources");
		return ResponseEntity.ok().body(myceliumService.getDataSources());
	}

	@GetMapping(value = "/{dataSourceId}")
	public ResponseEntity<DataSource> show(@PathVariable String dataSourceId) {
		log.info("Showing DataSourceById: {}", dataSourceId);
		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);
		return ResponseEntity.ok(dataSource);
	}

	@PostMapping(value = "")
	public ResponseEntity<DataSource> store(@RequestBody DataSource dto) {
		log.info("Storing new DataSource[id={}]", dto.getId());

		// todo validation
		ImportDataSourceTask task = new ImportDataSourceTask(myceliumService, dto);
		task.run();

		return ResponseEntity.created(URI.create("/api/resources/mycelium-datasources/" + dto.getId())).body(dto);
	}

	@PutMapping(value = "/{dataSourceId}")
	public ResponseEntity<DataSource> update(@RequestBody DataSource dto) {
		log.info("Updating existing DataSource[id={}]", dto.getId());

		// todo validation
		ImportDataSourceTask task = new ImportDataSourceTask(myceliumService, dto);
		task.run();

		return ResponseEntity.accepted().body(dto);
	}

	@DeleteMapping(value = "/{dataSourceId}")
	public ResponseEntity<?> destroy(@PathVariable String dataSourceId) {
		log.info("Deleting DataSource[id={}]", dataSourceId);

		// todo validation
		DeleteDataSourceTask task = new DeleteDataSourceTask(myceliumService, dataSourceId);
		task.run();

		return ResponseEntity.accepted().body(null);
	}

	@GetMapping(value = "/{dataSourceId}/vertices")
	public ResponseEntity<Page<Vertex>> showRecords(@PathVariable String dataSourceId, Pageable pageable) {
		log.info("Showing DataSource records by Id: {}", dataSourceId);
		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);

		Page<Vertex> result = myceliumService.getVerticesByDataSource(dataSource, pageable);

		return ResponseEntity.ok(result);
	}

	@DeleteMapping(value = "/{dataSourceId}/vertices")
	public ResponseEntity<?> deleteRecords(@PathVariable String dataSourceId) {

		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);

		// delete from index
		try (Stream<Vertex> stream = myceliumService.getGraphService().streamRegistryObjectFromDataSource(dataSourceId)) {
			stream.forEach(from -> {
				myceliumService.getIndexingService().deleteAllRelationship(from);
			});
		}

		// delete from graph database
		myceliumService.deleteVerticesByDataSource(dataSource);

		myceliumService.getGraphService().setRegistryObjectKeyNodeTerminated();

		return ResponseEntity.ok().body("");
	}

}
