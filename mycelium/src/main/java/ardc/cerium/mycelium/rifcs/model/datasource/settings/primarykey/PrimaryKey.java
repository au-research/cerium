package ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode
public class PrimaryKey implements Serializable {

	private String key;

	private String relationTypeFromCollection;

	private String relationTypeFromParty;

	private String relationTypeFromActivity;

	private String relationTypeFromService;

}
