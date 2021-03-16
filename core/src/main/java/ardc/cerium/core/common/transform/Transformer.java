package ardc.cerium.core.common.transform;

import ardc.cerium.core.common.entity.Version;

import java.util.Map;

public interface Transformer {

	Version transform(Version version);


	Transformer setParam(String key, String value);

	/**
	 * @return the parameters
	 */
	Map<String, String> getParams();

}
