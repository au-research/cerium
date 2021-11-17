package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class DuplicateForgoSideEffect extends SideEffect {

	private List<String> priorDuplicateIDs;

}
