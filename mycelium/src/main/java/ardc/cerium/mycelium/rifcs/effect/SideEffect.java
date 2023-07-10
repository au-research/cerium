package ardc.cerium.mycelium.rifcs.effect;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public abstract class SideEffect implements Serializable {
    public String affectedRegistryObjectId;
}
