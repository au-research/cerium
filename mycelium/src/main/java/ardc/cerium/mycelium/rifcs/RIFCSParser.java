package ardc.cerium.mycelium.rifcs;

import ardc.cerium.core.common.transform.XSLTransformer;
import ardc.cerium.mycelium.rifcs.model.RegistryObjects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Parsing RIFCS
 *
 * @author Minh Duc Nguyen
 */
public class RIFCSParser {

	private static final String path = "xslt/rifcs_fix_non_group.xsl";

	/**
	 * Parse a rifcs XML payload into deserialized {@link RegistryObjects}
	 * @param rifcs the XML payload
	 * @return {@link RegistryObjects} deserialized based on the XML
	 */
	public static RegistryObjects parse(String rifcs) {

		// due to rifcs can have non grouped attributes, this XSLT will put them next to
		// each other
		// todo remove the transform when upgrading jackson to 2.12.2 (spring boot 2.5)
		String fixedRifcs = XSLTransformer.transform(path, rifcs, null);

		XmlMapper xmlMapper = new XmlMapper();
		try {
			return xmlMapper.readValue(fixedRifcs, RegistryObjects.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}

	}

}
