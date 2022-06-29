package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DCIRelationChangeSideEffect extends SideEffect{

    public String affectedRegistryObjectId;

}
