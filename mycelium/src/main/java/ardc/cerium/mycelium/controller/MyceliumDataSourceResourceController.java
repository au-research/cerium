package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.task.DeleteDataSourceTask;
import ardc.cerium.mycelium.task.ImportDataSourceTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

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
		log.info("Showing DataSource Records by Id: {}", dataSourceId);

		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);
		if (dataSource == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSource "+dataSourceId+" Not Found");
		}

		Page<Vertex> result = myceliumService.getVerticesByDataSource(dataSource, pageable);

		return ResponseEntity.ok(result);
	}

	@DeleteMapping(value = "/{dataSourceId}/vertices")
	public ResponseEntity<?> deleteRecords(@PathVariable String dataSourceId) {
		log.info("Deleting Records from data source by Id: {}", dataSourceId);

		DataSource dataSource = myceliumService.getDataSourceById(dataSourceId);
		if (dataSource == null) {
			log.error("DataSource with id:{} Not Found", dataSourceId);
			//throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSource "+dataSourceId+" Not Found");
		}

		// delete from index
		log.info("Deleting All RelationshipDocument for DataSource[id={}]", dataSourceId);
		myceliumService.getMyceliumIndexingService().deleteAllDataSourceRelationship(dataSourceId);

		// delete from graph database
		log.info("Deleting All Vertices for DataSource[id={}]", dataSourceId);
		myceliumService.deleteVerticesByDataSourceID(dataSourceId);

		myceliumService.getGraphService().setRegistryObjectKeyNodeTerminated();

		return ResponseEntity.ok().body("");
	}

}
