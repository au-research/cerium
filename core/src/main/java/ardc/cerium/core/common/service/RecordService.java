package ardc.cerium.core.common.service;

import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Service layer for operation against {@link Record}
 *
 * @author Minh Duc Nguyen
 * @version 1.0
 * @since 2020-09-25
 */
@Service
public class RecordService {

	private final RecordRepository repository;

	private final RecordMapper recordMapper;

	private final ValidationService validationService;

	public RecordService(RecordRepository repository, RecordMapper mapper, ValidationService validationService) {
		this.repository = repository;
		this.recordMapper = mapper;
		this.validationService = validationService;
	}

	/**
	 * Saves and persist a {@link Record}. Generates the UUID for the record
	 * @param record The {@link Record} to save and flush
	 * @return The {@link Record} persisted
	 */
	public Record save(Record record) {
		return repository.saveAndFlush(record);
	}

	/**
	 * Perform a search based on the predefined Search Specification
	 * {@link RecordSpecification}. To obtain DTO versions, it's recommended to use the
	 * DTOConverter available at {@link RecordMapper}. <pre>
	 * {@code result.map(recordMapper.getConverter();}
	 * </pre>
	 * @param specs an instance of {@link RecordSpecification} to search on
	 * @param pageable an instance of {@link Pageable} to determine pagination
	 * @return a {@link Page} of {@link Record} that matches the JPA specs applied
	 */
	public Page<Record> search(Specification<Record> specs, Pageable pageable) {
		return repository.findAll(specs, pageable);
	}

	/**
	 * Find and return a publicly available record
	 * @param id uuid of the record
	 * @return RecordDTO
	 */
	public Record findPublicById(String id) {
		Record record = findById(id);
		if (record == null) {
			throw new RecordNotFoundException(id);
		}

		if (!record.isVisible()) {
			throw new ForbiddenOperationException(String.format("Record %s is private", id));
		}

		return record;
	}

	/**
	 * Find a record by id todo unit test
	 * @param id String representation of a uuid
	 * @return the record if it exists, null if not
	 */
	public Record findById(String id) {
		Optional<Record> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	/**
	 * Find a record only if it's owned by the provided user
	 * @param id uuid of the record
	 * @param user the current logged in user
	 * @return RecordDTO
	 */
	public Record findOwnedById(String id, User user) {
		Optional<Record> opt = repository.findById(UUID.fromString(id));
		Record record = opt.orElseThrow(() -> new RecordNotFoundException(id));

		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("User does not own this record");
		}

		return record;
	}

	/**
	 * Find the earliest modifedAt date
	 * @return Date
	 */
	public Date findEarliest() {
		return repository.findEarliest();
	}

	/**
	 * Tell if a record exists by id todo handle soft delete
	 * @param id String uuid
	 * @return if the uuid correlate to an existing record
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	/**
	 * Creates the Record
	 * @param recordDTO Validated RecordDTO
	 * @param user User Model
	 * @return RecordDTO if the creation is successful
	 */
	public Record create(RecordDTO recordDTO, User user) {
		// recordDTO should already be @Valid
		Record record = recordMapper.convertToEntity(recordDTO);

		// validate user access
		Allocation allocation = new Allocation(record.getAllocationID());
		if (!validationService.validateAllocationScope(allocation, user, Scope.CREATE)) {
			throw new ForbiddenOperationException("User does not have access to create record for this allocation");
		}

		// default record sets
		record.setCreatedAt(new Date());
		record.setCreatorID(user.getId());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(user.getId());
		record.setVisible(true);

		// allow igsn:import scope to overwrite default data
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			record.setVisible(recordDTO.isVisible());
			record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
			record.setModifiedAt(
					recordDTO.getModifiedAt() != null ? recordDTO.getModifiedAt() : record.getModifiedAt());
			record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
			record.setOwnerID(recordDTO.getOwnerID() != null ? recordDTO.getOwnerID() : record.getOwnerID());
			record.setOwnerType(recordDTO.getOwnerType() != null ? recordDTO.getOwnerType() : record.getOwnerType());
		}

		record = repository.save(record);

		return record;
	}

	/**
	 * Deletes a record completely from the repository
	 * @param id the id of the record to be deleted
	 * @param user The User Model
	 * @return true if the record is deleted
	 */
	public boolean delete(String id, User user) {
		if (!exists(id)) {
			throw new RecordNotFoundException(id);
		}
		Record record = findById(id);
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't have permission to update this record");
		}
		repository.delete(record);
		return true;
	}

	public boolean delete(Record record) {
		repository.delete(record);
		return true;
	}

	/**
	 * Updates a record Validates record existence Validates User ownership
	 * @param recordDTO the dto of the record
	 * @param user User model
	 * @return RecordDTO
	 */
	public Record update(@NotNull RecordDTO recordDTO, User user) {
		if (!exists(recordDTO.getId().toString())) {
			throw new RecordNotFoundException(recordDTO.getId().toString());
		}
		Record record = findById(recordDTO.getId().toString());

		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't have permission to update this record");
		}

		// can update
		record.setVisible(recordDTO.isVisible());
		record.setAllocationID(
				recordDTO.getAllocationID() != null ? recordDTO.getAllocationID() : record.getAllocationID());
		record.setOwnerID(recordDTO.getOwnerID() != null ? recordDTO.getOwnerID() : record.getOwnerID());
		record.setOwnerType(recordDTO.getOwnerType() != null ? recordDTO.getOwnerType() : record.getOwnerType());

		// modification timestamp
		record.setModifierID(user.getId());
		record.setModifiedAt(new Date());

		// allow igsn:import scope to overwrite certain fields
		if (validationService.validateAllocationScope(new Allocation(record.getAllocationID()), user, Scope.IMPORT)) {
			record.setCreatedAt(recordDTO.getCreatedAt() != null ? recordDTO.getCreatedAt() : record.getCreatedAt());
			record.setModifiedAt(recordDTO.getModifiedAt() != null ? recordDTO.getModifiedAt() : record.getCreatedAt());
			record.setCreatorID(recordDTO.getCreatorID() != null ? recordDTO.getCreatorID() : record.getCreatorID());
			record.setModifierID(
					recordDTO.getModifierID() != null ? recordDTO.getModifierID() : record.getModifierID());
		}

		record = repository.save(record);

		return record;
	}


	public List<Record> findByAllocationID(UUID allocationID){
		return repository.findByAllocationID(allocationID);

	}
}
