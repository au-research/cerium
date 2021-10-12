package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class GrantsNetworkForgoSideEffect extends SideEffect {

	public String registryObjectId;

	public String registryObjectKey;

	public String registryObjectClass;

}
