package ardc.cerium.core.common.service;

import ardc.cerium.core.common.dto.URLDTO;
import ardc.cerium.core.common.dto.mapper.URLMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.URLRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class URLService {

	@Autowired
	private URLRepository repository;

	@Autowired
	private URLMapper mapper;

	@Autowired
	ValidationService validationService;

	@Autowired
	RecordService recordService;

	/**
	 * Find a url by id
	 * @param id the uuid of the URL
	 * @return the URL if it exists, null if not
	 */
	public URL findById(String id) {
		Optional<URL> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

	/**
	 * Find a url by id
	 * @param record the Record URL
	 * @return the URL if it exists, null if not
	 */
	public URL findByRecord(Record record) {
		Optional<URL> opt = repository.findByRecord(record);
		return opt.orElse(null);
	}

	/**
	 * Tell if a URL exists by id
	 * @param id the uuid of the URL
	 * @return if the uuid correlate to an existing url
	 */
	public boolean exists(String id) {
		return repository.existsById(UUID.fromString(id));
	}

	/**
	 * Retrieve all owned URLs Owned URLs are the URLs that which records the user have
	 * access to
	 *
	 * todo accept User UUID as a parameter todo update findOwned at the repository level
	 * @return a list of URLs that is owned by the user
	 */
	public List<URL> findOwned() {
		return repository.findAll();
	}

	// create
	public URL create(URL newUrl) {
		return repository.save(newUrl);
	}

	public URLDTO create(URLDTO dto, User user) {
		URL url = mapper.convertToEntity(dto);

		// validate record existence
		if (!recordService.exists(dto.getRecord().toString())) {
			throw new RecordNotFoundException(dto.getRecord().toString());
		}

		// validate record ownership
		Record record = recordService.findById(dto.getRecord().toString());
		if (!validationService.validateRecordOwnership(record, user)) {
			throw new ForbiddenOperationException("User does not have access to create URL for this record");
		}

		// defaults
		url.setRecord(record);
		url.setCreatedAt(new Date());
		url.setResolvable(false);

		// import scope to overwrite certain fields
		Allocation allocation = new Allocation(record.getAllocationID());
		if (validationService.validateAllocationScope(allocation, user, Scope.IMPORT)) {
			url.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : url.getCreatedAt());
			url.setResolvable(dto.isResolvable() ? dto.isResolvable() : url.isResolvable());
		}

		url = repository.save(url);
		return mapper.convertToDTO(url);
	}

	/**
	 * Update a URL
	 * @param url to be updated
	 * @return The url that has updated
	 */

	public URL update(@NotNull URL url) {
		url.setUpdatedAt(new Date());
		repository.save(url);
		return url;
	}

	/**
	 * Permanently delete the url
	 * @param id the uuid of the URL
	 */
	public void delete(String id) {
		repository.deleteById(id);
	}

}
