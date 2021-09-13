package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.service.MyceliumService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Executor {

	private MyceliumService myceliumService;

	public abstract void handle();

}
