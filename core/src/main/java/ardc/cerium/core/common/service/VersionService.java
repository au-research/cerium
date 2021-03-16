package ardc.cerium.core.common.service;

import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.event.RecordUpdatedEvent;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import ardc.cerium.core.exception.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VersionService {

	private final ApplicationEventPublisher publisher;

	private final VersionRepository repository;

	private final VersionMapper versionMapper;

	private final SchemaService schemaService;

	private final RecordService recordService;

	private final ValidationService validationService;

	public VersionService(ApplicationEventPublisher publisher, VersionRepository repository, VersionMapper mapper,
			SchemaService schemaService, RecordService recordService, ValidationService validationService) {
		this.publisher = publisher;
		this.repository = repository;
		this.versionMapper = mapper;
		this.schemaService = schemaService;
		this.recordService = recordService;
		this.validationService = validationService;
	}

	/**
	 * Provides the hash of a version. Uses the {@link #getHash(String)} to get the hash
	 * of the Version's content
	 * @param version the {@link Version} to obtain the hash from
	 * @return the hash of the version's content
	 */
	@NotNull
	public static String getHash(@NotNull Version version) {
		return getHash(new String(version.getContent()));
	}

	/**
	 * Provides the hash in sha1 for the provided String
	 * @param content the String to get the hash from
	 * @return the hash of that string
	 */
	@NotNull
	public static String getHash(String content) {
		return DigestUtils.sha1Hex(content);
	}

	/**
	 * End the life of a {@link Version} The registry supports soft deleting of a
	 * {@link Version} so it's recommended to use this method to end the effective use of
	 * that {@link Version}
	 * @param version the {@link Version} to end
	 * @param user the {@link User} to end it with
	 * @return the ended {@link Version}
	 */
	public Version end(@NotNull Version version, @NotNull User user) {
		version.setEndedAt(new Date());
		version.setCurrent(false);
		version.setEndedBy(user.getId());

		repository.save(version);
		return version;
	}

	/**
	 * Find a {@link Version} by id
	 * @param id the String uuid of the {@link Version}
	 * @return the {@link VersionDTO} if it exists, {@code null} if not
	 */
	public Version findById(String id) {
		Optional<Version> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	/**
	 * Find a Public {@link Version} by id Public {@link Version} are Versions that
	 * belongs to a {@link Record} that is visible
	 * @param id the String uuid of the {@link Version}
	 * @return {@link VersionDTO}
	 */
	public Version findPublicById(String id) {
		Version version = findById(id);
		if (version == null || !version.getRecord().isVisible()) {
			throw new VersionNotFoundException(id);
		}
		return version;
	}

	/**
	 * Search for {@link Version} with {@link VersionSpecification}
	 * @param specs {@link VersionSpecification} that includes {@link SearchCriteria}
	 * @param pageable {@link Pageable}
	 * @return page {@link VersionDTO}
	 */
	public Page<Version> search(VersionSpecification specs, Pageable pageable) {
		return repository.findAll(specs, pageable);
	}

	/**
	 * Search for all public {@link Version} that has a particular {@link Schema}
	 * @param schema {@link String}
	 * @param from {@link Date}
	 * @param until {@link Date}
	 * @param pageable {@link Pageable}
	 * @return a @{@link Page} of {@link Version}
	 */
	public Page<Version> findAllCurrentVersionsOfSchema(String schema, Date from, Date until, Pageable pageable) {
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		specs.add(new SearchCriteria("current", 1, SearchOperation.EQUAL));
		specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		specs.add(new SearchCriteria("type", "IGSN", SearchOperation.RECORD_EQUAL));
		if (from != null)
			specs.add(new SearchCriteria("modifiedAt", from, SearchOperation.DATE_GREATER_THAN_EQUAL));
		if (until != null)
			specs.add(new SearchCriteria("modifiedAt", until, SearchOperation.DATE_LESS_THAN_EQUAL));
		return search(specs, pageable);
	}

	/**
	 * Return a single {@link Version} for a {@link Record} given the Schema string
	 * @param record {@link Record}
	 * @param schema Schema id of a {@link ardc.cerium.core.common.model.Schema}
	 * @return Version {@link Version}
	 */
	public Version findVersionForRecord(Record record, String schema) {
		return repository.findFirstByRecordAndSchemaAndCurrentIsTrue(record, schema);
	}

	/**
	 * Tell if a version exists by id
	 * @param id the uuid of the Version
	 * @return if the uuid correlate to an existing version
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	/**
	 * Persist a {@link Version}
	 * @param version a {@link Version} to persist
	 * @return the persisted {@link Version}
	 */
	public Version save(@NotNull Version version) {
		if (version.getCreatedAt() == null) {
			version.setCreatedAt(new Date());
		}
		version = repository.saveAndFlush(version);
		return version;
	}

	/**
	 * Creates a {@link Version}
	 * @param dto the {@link VersionDTO} to create from
	 * @param user the logged in {@link User}
	 * @return the persisted {@link Version}
	 */
	public Version create(VersionDTO dto, User user) {
		// versionDTO should already be @Valid from ardc.cerium.oai.controller
		Version version = versionMapper.convertToEntity(dto);

		// validate schema
		if (!schemaService.supportsSchema(version.getSchema())) {
			throw new SchemaNotSupportedException(version.getSchema());
		}

		if (dto.getRecord() == null || !recordService.exists(dto.getRecord())) {
			throw new RecordNotFoundException(dto.getRecord());
		}
		Record record = recordService.findById(dto.getRecord());

		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't own this record");
		}

		// there's already a version with this data, schema and is current
		String hash = getHash(version);
		if (repository.existsBySchemaAndHashAndCurrent(version.getSchema(), hash, true)) {
			throw new VersionContentAlreadyExistsException(version.getSchema(), hash);
		}

		// todo validate version content -> schema validation

		// default
		version.setCreatedAt(new Date());
		version.setCreatorID(user.getId());
		version.setCurrent(true);
		version.setHash(getHash(version));

		// allow igsn:import scope to overwrite data
		Allocation allocation = new Allocation(record.getAllocationID());
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			version.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : version.getCreatedAt());
			version.setCreatorID(
					dto.getCreatorID() != null ? UUID.fromString(dto.getCreatorID()) : version.getCreatorID());
			version.setCurrent(dto.isCurrent());
		}

		// if this version is the current, end all previous versions of the same schema
		if (version.isCurrent()) {
			List<Version> previousVersions = repository.findAllByRecordAndSchemaAndCurrentIsTrue(version.getRecord(),
					version.getSchema());
			for (Version previousVersion : previousVersions) {
				this.end(previousVersion, user);
			}
		}

		version = repository.save(version);

		// RecordUpdatedEvent
		publisher.publishEvent(new RecordUpdatedEvent(version.getRecord(), user));

		return version;
	}

	/**
	 * Delete a {@link Version} by id and {@link User}
	 * @param id the id uuid format of the {@link Version}
	 * @param user the logged in {@link User}
	 * @return true if the {@link Version} is deleted successfully
	 */
	public boolean delete(String id, User user) {
		if (!repository.existsById(UUID.fromString(id))) {
			throw new VersionNotFoundException(id);
		}
		Version version = findById(id);
		Record record = version.getRecord();
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("You don't have access to delete this version");
		}

		repository.deleteById(id);
		return true;
	}

}