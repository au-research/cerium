package ardc.cerium.core.common.service;

import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.EmbargoRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmbargoService {

	private final EmbargoRepository repository;
	private final RecordRepository recordRepository;

	public EmbargoService(EmbargoRepository repository, RecordRepository recordRepository) {
		this.repository = repository;
		this.recordRepository = recordRepository;
	}

	/**
	 * Return List of embargo that have reached their embargoEnd date
	 * @param date to compare the embargoEnd to
	 * @return The Lists of Embargo to end
	 */
	public List<Embargo> findAllEmbargoToEnd(Date date) {
		return repository.findAllByEmbargoEndLessThanEqual(date);
	}

	/**
	 * For each embargo in the list make record visible and delete the embargo
	 */
	@Scheduled(fixedRate = 60 * 60 * 1000)
	public void endEmbargoList() {
		List<Embargo> embargoList = findAllEmbargoToEnd(new Date());
		for(Embargo endEmbargo: embargoList) {
			Record record = endEmbargo.getRecord();
			record.setVisible(true);
			recordRepository.save(record);
			delete(endEmbargo.getId());
		}
	}


	/**
	 * Deletes an embargo completely from the repository
	 * @param id the id of the embargo to be deleted
	 * @return true if the record is deleted
	 */
	public boolean delete(UUID id) {
		Embargo embargo = findById(id.toString());
		repository.delete(embargo);
		return true;
	}

	/**
	 * Permanently delete the embargo
	 * @param record the record of the Embargo to search for
	 * @return Optional Embargo
	 */
	public Embargo findByRecord(Record record) {
		Optional<Embargo> embargo =  repository.findByRecord(record);
		return embargo.orElse(null);
	}

	/**
	 * Saves and persist a {@link Embargo}. Generates the UUID for the record
	 * @param embargo The {@link Embargo} to save and flush
	 * @return The {@link Embargo} persisted
	 */
	public Embargo save(Embargo embargo) {
		return repository.saveAndFlush(embargo);
	}

	/**
	 * Find a embargo by id todo unit test
	 * @param id String representation of a uuid
	 * @return the record if it exists, null if not
	 */
	public Embargo findById(String id) {
		Optional<Embargo> opt = repository.findById(UUID.fromString(id));

		return opt.orElse(null);
	}

}
