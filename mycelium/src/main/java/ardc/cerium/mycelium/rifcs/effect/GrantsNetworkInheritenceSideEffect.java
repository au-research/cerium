package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GrantsNetworkInheritenceSideEffect extends SideEffect {

	public String registryObjectId;

	public String registryObjectClass;

}
