package ardc.cerium.mycelium.rifcs.model.datasource.settings;

import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class PrimaryKeySetting extends DataSourceSetting {

	private List<PrimaryKey> primaryKeys = new ArrayList<>();

}
