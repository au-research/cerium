package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.provider.VisibilityProvider;
import ardc.cerium.core.common.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("unused")
public class ARDCv1VisibilityProvider implements VisibilityProvider {

	/**
	 * try to determine if an IGSN record in ARDC v1 schema is visible
	 * @param content The xml content of the ARDC v1 version
	 * @return the first 'isPublic' element's content
	 */
	@Override
	public boolean get(String content) {
		boolean visible = false;
		// assume if the isPublic not set or ommited the record is visible
		try {
			NodeList nodeList = XMLUtil.getXPath(content, "//isPublic");
			if (nodeList.getLength() > 0) {
				Node isPublicNode = nodeList.item(0);
				String isPublic = isPublicNode.getTextContent();
				if (isPublic.toLowerCase().equals("true")) {
					visible = true;
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return visible;
	}

}