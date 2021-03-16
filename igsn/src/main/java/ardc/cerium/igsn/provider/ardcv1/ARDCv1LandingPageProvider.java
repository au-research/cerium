package ardc.cerium.igsn.provider.ardcv1;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import ardc.cerium.core.common.provider.LandingPageProvider;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ardc.cerium.core.common.model.Schema;

import ardc.cerium.core.common.util.XMLUtil;

@SuppressWarnings("unused")
public class ARDCv1LandingPageProvider implements LandingPageProvider {

	/**
	 * Finds the landingPage of 1 IGSN record in ARDCv1 schema
	 * @param content The xml content of the ARDC v1 version
	 * @return The landing page (a url) as String
	 */
	@Override
	public String get(String content) {
		String landingPageValue = "";
		String xpath = "/resources/resource/landingPage";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > 0) {
				landingPageValue = l.item(0).getFirstChild().getNodeValue();
			}
		}
		catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return landingPageValue;
	}

}