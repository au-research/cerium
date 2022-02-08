package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class TitleChangeSideEffect extends SideEffect implements Serializable {

	private final String registryObjectId;

	private final String objectClass;

	private final String objectType;

	private final String oldTitle;

	private final String newTitle;

	private final String relationshipType;

}
