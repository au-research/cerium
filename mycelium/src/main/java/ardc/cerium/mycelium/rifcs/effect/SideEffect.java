package ardc.cerium.mycelium.rifcs.effect;

import ardc.cerium.core.common.entity.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SideEffect {

    private Request request;

    abstract public void handle();
}
