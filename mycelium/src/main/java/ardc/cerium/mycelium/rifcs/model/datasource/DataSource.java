package ardc.cerium.mycelium.rifcs.model.datasource;

import ardc.cerium.mycelium.rifcs.model.datasource.settings.PrimaryKeySetting;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class DataSource {

	private String id;

	private String title;

	private PrimaryKeySetting primaryKeySetting;

}
