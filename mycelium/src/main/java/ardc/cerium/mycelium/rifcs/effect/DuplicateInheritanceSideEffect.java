package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DuplicateInheritanceSideEffect extends SideEffect {

	private final String affectedRegistryObjectId;

}
