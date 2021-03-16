package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.TitleProvider;
import ardc.cerium.core.common.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("unused")
public class ARDCv1TitleProvider implements TitleProvider {

	/**
	 * retrieve the resourceTitle of an IGSN record in ARDC v1 schema
	 * @param content The xml content of the ARDC v1 version
	 * @return The resourceTitle as String
	 */
	@Override
	public String get(String content) {
		String title = null;
		try {
			NodeList nodeList = XMLUtil.getXPath(content, "//resourceTitle");
			Node resourceTitleNode = nodeList.item(0);
			title = resourceTitleNode.getTextContent();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return title;
	}

}
