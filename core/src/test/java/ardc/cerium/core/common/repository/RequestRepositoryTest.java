package ardc.cerium.core.common.repository;

import ardc.cerium.core.common.repository.RequestRepository;
import ardc.cerium.core.common.entity.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class RequestRepositoryTest {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	RequestRepository repository;

	@Test
	void findById() {
		Request request = new Request();
		repository.save(request);

		UUID id = request.getId();

		Optional<Request> dbFound = repository.findById(id);

		assertThat(dbFound.isPresent()).isTrue();
		Request found = dbFound.get();
		assertThat(found).isInstanceOf(Request.class);
		assertThat(found.getId()).isEqualTo(request.getId());
	}

	@Test
	void existsById() {
		assertThat(repository.existsById(UUID.randomUUID())).isFalse();

		Request request = new Request();
		repository.save(request);
		UUID id = request.getId();

		assertThat(repository.existsById(id)).isTrue();
	}

}