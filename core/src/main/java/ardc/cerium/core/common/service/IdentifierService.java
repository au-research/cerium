package ardc.cerium.core.common.service;

import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.repository.specs.IdentifierSpecification;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentifierService {

	private final IdentifierRepository repository;

	private final IdentifierMapper mapper;

	private final RecordService recordService;

	private final ValidationService validationService;

	public IdentifierService(IdentifierRepository repository, IdentifierMapper mapper, RecordService recordService,
			ValidationService validationService) {
		this.repository = repository;
		this.mapper = mapper;
		this.recordService = recordService;
		this.validationService = validationService;
	}

	/**
	 * Search for {@link Identifier}
	 * @param specs the {@link IdentifierSpecification} for determining the filters
	 * @param pageable the {@link Pageable} for pagination and row limit
	 * @return a {@link Page} of {@link Identifier}
	 */
	public Page<Identifier> search(IdentifierSpecification specs, Pageable pageable) {
		return repository.findAll(specs, pageable);
	}

	/**
	 * Find an identifier by id
	 * @param id the uuid of the Identifier
	 * @return the identifier if it exists, null if not
	 */
	public Identifier findById(String id) {
		Optional<Identifier> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	public Identifier findByValueAndType(String value, Identifier.Type type) {
		return repository.findFirstByValueIgnoreCaseAndType(value, type);
	}

	public Identifier findByValueAndTypeAndDomain(String value, Identifier.Type type, String domain) {
		return repository.findByValueAndTypeAndDomain(value, type, domain);
	}

	/**
	 * Tell if an identifier exists by id
	 * @param id the uuid of the Identifier
	 * @return if the uuid correlate to an existing version
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	public Identifier save(@NotNull Identifier newIdentifier) {
		Identifier existingIdentifier = findByValueAndTypeAndDomain(newIdentifier.getValue(), newIdentifier.getType(),
				newIdentifier.getDomain());
		if (existingIdentifier != null) {
			throw new ForbiddenOperationException(String.format("Identifier %s with type %s and domain %s already exists",
					newIdentifier.getValue(), newIdentifier.getType(), newIdentifier.getDomain()));
		}
		return repository.saveAndFlush(newIdentifier);
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	public Identifier create(IdentifierDTO dto, User user) {
		Identifier identifier = mapper.convertToEntity(dto);

		// validate record existence
		if (!recordService.exists(dto.getRecord().toString())) {
			throw new RecordNotFoundException(dto.getRecord().toString());
		}

		// validate record ownership
		Record record = recordService.findById(dto.getRecord().toString());
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("User does not have access to create Identifier for this record");
		}

		// defaults
		identifier.setRecord(record);
		identifier.setCreatedAt(new Date());
		identifier.setUpdatedAt(new Date());

		// import scope overwrite
		Allocation allocation = new Allocation(record.getAllocationID());
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			identifier.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : identifier.getCreatedAt());
			identifier.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : identifier.getUpdatedAt());
		}

		identifier = repository.save(identifier);

		return identifier;
	}

	/**
	 * Update a record
	 * @param identifier to be updated
	 * @return The identifier that has updated
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	public Identifier update(@NotNull Identifier identifier) {
		identifier.setUpdatedAt(new Date());
		repository.save(identifier);
		return identifier;
	}

	/**
	 * Permanently delete the identifier
	 * @param id the uuid of the Identifier
	 */
	@Transactional
	public void delete(String id) {
		repository.deleteById(UUID.fromString(id));
	}

}
