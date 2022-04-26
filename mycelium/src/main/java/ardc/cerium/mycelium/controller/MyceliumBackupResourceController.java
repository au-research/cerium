package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping(value = "/api/resources/mycelium-backups", produces = { MediaType.APPLICATION_JSON_VALUE })
public class MyceliumBackupResourceController {

    final MyceliumBackupService backupService;

    public MyceliumBackupResourceController(MyceliumBackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping(value = "")
    public ResponseEntity<?> createBackup(@RequestParam(required = true) String backupId, @RequestParam(required = true) String dataSourceId) {
        backupService.createBackup(backupId, dataSourceId);
        return ResponseEntity.ok("Done");
    }

    @PostMapping(value="/{backupId}/_restore")
    public ResponseEntity<?> restoreBackup(@PathVariable String backupId, @RequestParam(required = true) String dataSourceId, @RequestParam(required = false) String correctedDataSourceId) {

        if (correctedDataSourceId == null) {
            correctedDataSourceId = dataSourceId;
        }

        backupService.restoreBackup(backupId, dataSourceId, correctedDataSourceId);
        return ResponseEntity.ok("Done");
    }
}
