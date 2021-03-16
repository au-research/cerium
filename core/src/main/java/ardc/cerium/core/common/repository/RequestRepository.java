package ardc.cerium.core.common.repository;

import ardc.cerium.core.common.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, String>, JpaSpecificationExecutor<Request> {

	Optional<Request> findById(UUID id);

	boolean existsById(UUID id);

}
