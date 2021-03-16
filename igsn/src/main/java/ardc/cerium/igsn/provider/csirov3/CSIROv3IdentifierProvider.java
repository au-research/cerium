package ardc.cerium.igsn.provider.csirov3;

import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.util.XMLUtil;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSIROv3IdentifierProvider implements IdentifierProvider {

	/**
	 * Finds the resourceIdentifier of an IGSN record with ARDC v1 schema
	 * @param content XML String of one ARDCv1 Resource document
	 * @return The resourceIdentifier as String
	 */
	private String prefix = "";

	@Override
	public String get(String content) {
		String identifierValue = "";

		String xpath = "/resources/resource/resourceIdentifier";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > 0) {
				identifierValue = String.format("%s%s", prefix, l.item(0).getFirstChild().getNodeValue()).toUpperCase();
			}
		}
		catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return identifierValue.toUpperCase();
	}

	@Override
	public String get(String content, int position) {
		String identifierValue = "";
		if(position < 0) return null;
		String xpath = "/resources/resource/resourceIdentifier";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > position) {
				identifierValue = String.format("%s%s", prefix, l.item(position).getFirstChild().getNodeValue()).toUpperCase();
			}
		}
		catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return identifierValue.toUpperCase();
	}

	/**
	 * @param content XML String of the ARDCv1 Resource(s) document
	 * @return a List is identifier values in the given document
	 */
	@Override
	public List<String> getAll(String content) {
		List<String> identifiers = new ArrayList<String>();
		String xpath = "/resources/resource/resourceIdentifier";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			for (int i = 0; i < l.getLength(); i++) {
				identifiers.add(String.format("%s%s", prefix, l.item(i).getFirstChild().getNodeValue()).toUpperCase());
			}
		}
		catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return identifiers;
	}

	@Override
	public void setPrefix(String prefix) {
		// add a paths separator if not present
		if(!prefix.endsWith("/")){
			this.prefix = prefix + "/";
		}
		else{
			this.prefix = prefix;
		}

	}

}
