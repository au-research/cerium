package ardc.cerium.core.common.repository;

import ardc.cerium.core.common.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hibernate.annotations.QueryHints.READ_ONLY;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;

@Repository
public interface RecordRepository extends JpaRepository<Record, String>, JpaSpecificationExecutor<Record> {

	List<Record> findByCreatorID(UUID creatorID);

	Optional<Record> findById(UUID id);

	boolean existsById(UUID id);

	@Query(value = "SELECT r FROM Record r WHERE r.creatorID = ?1 OR r.ownerID = ?1")
	List<Record> findOwned(UUID owner);

	@Query(value = "SELECT r FROM Record r WHERE r.allocationID = ?1")
	List<Record> findByAllocationID(UUID allocationID);

	List<Record> findByTypeAndAllocationID(String type, UUID allocationID);

	@Query(value = "SELECT r FROM Record r WHERE r.creatorID = ?1 OR r.ownerID = ?1 OR r.allocationID IN ?2")
	List<Record> findOwned(UUID owner, List<UUID> allocationIDs, Pageable pageable);

	Page<Record> findAllByVisibleIsTrue(Pageable pageable);

	Page<Record> findById(UUID id, Pageable pageable);

	Page<Record> findAllByTitleNull(Pageable pageable);

	@Query(value = "SELECT MIN(modifiedAt) FROM Record")
	Date findEarliest();

	@QueryHints(value = { @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
			@QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = READ_ONLY, value = "true") })
	@Query("select record from Record record")
	Stream<Record> getAll();

	List<Record> findAllByType(String type);

}
