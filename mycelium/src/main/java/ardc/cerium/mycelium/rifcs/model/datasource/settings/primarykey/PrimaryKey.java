package ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class PrimaryKey {

	private String key;

	private String relationTypeFromCollection;

	private String relationTypeFromParty;

	private String relationTypeFromActivity;

	private String relationTypeFromService;

}
