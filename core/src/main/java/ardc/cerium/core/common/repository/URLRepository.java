package ardc.cerium.core.common.repository;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.common.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface URLRepository extends JpaRepository<URL, String> {

	Optional<URL> findById(UUID id);

	boolean existsById(UUID id);

	Optional<URL> findByRecord(Record record);

}
