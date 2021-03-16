package ardc.cerium.oai;

import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
public class UserTest {

	@Test
	public void belongsToDataCenter() {
		// given a User with several datacenters
		User user = TestHelper.mockUser();
		DataCenter dataCenter = new DataCenter(UUID.randomUUID());
		user.setDataCenters(Arrays.asList(new DataCenter(UUID.randomUUID()), dataCenter));

		// when check if the user belongs to that datacenters
		assertThat(user.belongsToDataCenter(dataCenter.getId())).isTrue();
		assertThat(user.belongsToDataCenter(UUID.randomUUID())).isFalse();

		// empty datacenters
		assertThat(TestHelper.mockUser().belongsToDataCenter(dataCenter.getId())).isFalse();
	}

	@Test
	void hasAllocation() {
		// given a User with several Allocation
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		user.setAllocations(Arrays.asList(new Allocation(UUID.randomUUID()), allocation));

		// when check if the user has an allocation
		assertThat(user.hasAllocation(allocation.getId())).isTrue();
		assertThat(user.hasAllocation(UUID.randomUUID())).isFalse();

		// empty allocation case
		assertThat(TestHelper.mockUser().hasAllocation(allocation.getId())).isFalse();
	}

	@Test
	void getAllocationById() {
		// given a User with several Allocation
		User user = TestHelper.mockUser();
		Allocation expected = new Allocation(UUID.randomUUID());
		user.setAllocations(Arrays.asList(new Allocation(UUID.randomUUID()), expected));

		// when getByAllocationId
		Allocation actual = user.getAllocationById(expected.getId());

		// actual is provided
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		// empty case
		assertThat(TestHelper.mockUser().getAllocationById(expected.getId())).isNull();
	}

}