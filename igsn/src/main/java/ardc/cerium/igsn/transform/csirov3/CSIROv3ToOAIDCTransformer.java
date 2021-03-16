package ardc.cerium.igsn.transform.csirov3;

import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.XSLTransformer;

import java.util.HashMap;
import java.util.Map;

public class CSIROv3ToOAIDCTransformer implements Transformer {

	private static final String path = "xslt/csiro_v3_to_oai_dc.xsl";

	private static final String targetSchemaID = SchemaService.OAIDC;

	private Map<String, String> parameters = new HashMap<>();

	@Override
	public Version transform(Version version) {

		String resultDocument = XSLTransformer.transform(path, new String(version.getContent()), new HashMap<>());

		if (resultDocument == null)
			return null;

		Version resultVersion = new Version();
		resultVersion.setSchema(targetSchemaID);
		resultVersion.setCurrent(true);
		resultVersion.setRecord(version.getRecord());
		resultVersion.setContent(resultDocument.getBytes());

		// resulting version should have the same request ID as the original version
		resultVersion.setRequestID(version.getRequestID());

		return resultVersion;
	}

	@Override
	public Transformer setParam(String key, String value) {
		return null;
	}

	@Override
	public Map<String, String> getParams() {
		return null;
	}

}
