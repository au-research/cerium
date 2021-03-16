package ardc.cerium.drvs.controller;

import ardc.cerium.drvs.dto.DRVSRequestDTO;
import ardc.cerium.drvs.dto.mapper.DRVSRequestMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.drvs.service.*;
import ardc.cerium.drvs.task.HarvestDOITask;
import ardc.cerium.drvs.task.ImportDRVSTask;
import ardc.cerium.core.exception.ContentNotSupportedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@ConditionalOnProperty(name = "app.drvs.enabled")
@RequestMapping(value = "/api/services/drvs/", produces = { MediaType.APPLICATION_JSON_VALUE })
@Tag(name = "DRVS Services", description = "API endpoints to interact with DRVS related Requests")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class DRVSServiceController {

	final DRVSImportService drvsImportService;

	final DOIHarvestService doiHarvestService;

	final CollectionValidationService collectionValidationService;

	final ApplicationEventPublisher applicationEventPublisher;

	final KeycloakService kcService;

	final DRVSRequestMapper requestMapper;

	final DRVSRequestValidationService drvsRequestValidationService;

	final DRVSIndexingService drvsIndexingService;

	final RecordService recordService;

	public DRVSServiceController(DRVSImportService drvsImportService, DOIHarvestService doiHarvestService,
								 KeycloakService kcService, DRVSRequestMapper requestMapper,
								 CollectionValidationService collectionValidationService,
								 ApplicationEventPublisher applicationEventPublisher,
								 DRVSRequestValidationService drvsRequestValidationService, DRVSIndexingService drvsIndexingService, RecordService recordService) {
		this.drvsImportService = drvsImportService;
		this.doiHarvestService = doiHarvestService;
		this.kcService = kcService;
		this.requestMapper = requestMapper;
		this.collectionValidationService = collectionValidationService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.drvsRequestValidationService = drvsRequestValidationService;
		this.drvsIndexingService = drvsIndexingService;
		this.recordService = recordService;
	}

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a DRVS Submission CSV to import DRVS Records to the Registry")
	public ResponseEntity<DRVSRequestDTO> importHandler(HttpServletRequest httpRequest,
                                                        @Parameter(name = "file", description = "The DRVS Submission CSV", required = true,
					schema = @Schema(implementation = MultipartFile.class)) @RequestParam("file") MultipartFile file,
                                                        @Parameter(name = "allocationID", description = "The AllocationID that the imported records will belong to",
					required = true, schema = @Schema(implementation = UUID.class)) @RequestParam String allocationID) {
		User user = kcService.getLoggedInUser(httpRequest);

		// create the request and save the file
		Request request = drvsImportService.createRequest(user, file);
		request.setAllocationID(UUID.fromString(allocationID));
		request.setAttribute(Attribute.ALLOCATION_ID, allocationID);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setAttribute(Attribute.NUM_OF_RECORDS_CREATED, 0);
		request.setAttribute(Attribute.NUM_OF_RECORDS_UPDATED, 0);

		// determine Owner information
		request.setAttribute(Attribute.OWNER_TYPE, Record.OwnerType.DataCenter.toString());

		// OwnerID will be the DataCenterID from the Allocation
		// The DRVS Allocation should only have 1 DataCenter
		String ownerID;
		Allocation allocation = kcService.getAllocationByResourceID(allocationID);
		try {
			DataCenter dataCenter = kcService.getDataCenterByUUID(allocation.getDataCenters().get(0).getId());
			ownerID = dataCenter.getId().toString();
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Failed to obtain DataCenter for Allocation %s", allocationID));
		}
		request.setAttribute(Attribute.OWNER_ID, ownerID);

		// accept the request and move on
		request.setStatus(Request.Status.ACCEPTED);
		request = drvsImportService.save(request);

		// validate the request
		try {
			drvsRequestValidationService.validate(request);
		}
		catch (ContentNotSupportedException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
		// run the job
		ImportDRVSTask task = new ImportDRVSTask(request, drvsImportService, applicationEventPublisher);
		task.run();

		// ensure the request is saved properly
		request = drvsImportService.save(request);

		// return the request
		DRVSRequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@Operation(summary = "Initiate a Harvest of DOI XML for all Records with given Allocation ID")
	@PostMapping("/bulk-harvest")
	public ResponseEntity<DRVSRequestDTO> bulkHarvest(HttpServletRequest httpRequest,
			@Parameter(name = "allocationID",
					description = "Start a harvest for all the records belong to this Allocation", required = true,
					schema = @Schema(implementation = UUID.class)) @RequestParam String allocationID) {

		User user = kcService.getLoggedInUser(httpRequest);
		String ownerID;
		Allocation allocation = kcService.getAllocationByResourceID(allocationID);
		try {
			DataCenter dataCenter = kcService.getDataCenterByUUID(allocation.getDataCenters().get(0).getId());
			ownerID = dataCenter.getId().toString();
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Failed to obtain DataCenter for Allocation %s", allocationID));
		}
		// create the request
		Request request = doiHarvestService.createHarvestRequest(user, "drvs-bulk-harvest");
		request.setAllocationID(UUID.fromString(allocationID));
		request.setAttribute(Attribute.ALLOCATION_ID, allocationID);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setAttribute(Attribute.OWNER_ID, ownerID);
		request.setStatus(Request.Status.ACCEPTED);
		request = doiHarvestService.save(request);

		// todo validate the request LATER (now just be trusting)

		// run the job asynchronously
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		threadPoolExecutor.execute(new HarvestDOITask(request, doiHarvestService, drvsIndexingService, applicationEventPublisher));

		request.setStatus(Request.Status.RUNNING);
		request.setMessage("Harvest initiated, progress can be viewed in your Request dashboard");
		request = drvsImportService.save(request);

		// return the request
		DRVSRequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@Operation(summary = "Initiate a Harvest of DOI XML for given Record ID")
	@PostMapping("/harvest")
	public ResponseEntity<DRVSRequestDTO> harvest(HttpServletRequest httpRequest,
			@Parameter(name = "recordID", description = "The UUID of the record to run harvest on", required = true,
					schema = @Schema(implementation = UUID.class)) @RequestParam String recordID,
			@Parameter(name = "allocationID", description = "The UUID of the Allocation the record belongs to",
					required = true, schema = @Schema(implementation = UUID.class)) @RequestParam String allocationID) {
		User user = kcService.getLoggedInUser(httpRequest);
		String ownerID;
		Allocation allocation = kcService.getAllocationByResourceID(allocationID);
		try {
			DataCenter dataCenter = kcService.getDataCenterByUUID(allocation.getDataCenters().get(0).getId());
			ownerID = dataCenter.getId().toString();
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Failed to obtain DataCenter for Allocation %s", allocationID));
		}
		// create the request
		Request request = doiHarvestService.createHarvestRequest(user, "drvs-single-harvest");
		request.setAllocationID(UUID.fromString(allocationID));
		request.setAttribute(Attribute.RECORD_ID, recordID);
		request.setAttribute(Attribute.ALLOCATION_ID, allocationID);
		request.setAttribute(Attribute.CREATOR_ID, user.getId().toString());
		request.setAttribute(Attribute.OWNER_ID, ownerID);
		request.setStatus(Request.Status.ACCEPTED);
		request = doiHarvestService.save(request);

		// todo validate the request LATER (now just be trusting)

		HarvestDOITask harvestDOITask = new HarvestDOITask(request, doiHarvestService, drvsIndexingService, applicationEventPublisher);
		harvestDOITask.run();

		request.setStatus(Request.Status.COMPLETED);
		request = drvsImportService.save(request);

		// return the request
		DRVSRequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

}
