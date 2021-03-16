package ardc.cerium.core.common.repository;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, String>, JpaSpecificationExecutor<Identifier> {

	Optional<Identifier> findById(UUID id);

	boolean existsById(UUID id);

	void deleteById(UUID id);

	Identifier findFirstByValueIgnoreCaseAndType(String value, Identifier.Type type);

	Identifier findByValueAndTypeAndDomain(String value, Identifier.Type type, String domain);

	Identifier findFirstByRecordAndType(Record record, Identifier.Type type);

	boolean existsByTypeAndValue(Identifier.Type type, String value);

}
