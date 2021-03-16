package ardc.cerium.igsn.provider.ardcv1;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import ardc.cerium.core.common.provider.IdentifierProvider;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.*;

import ardc.cerium.core.common.util.XMLUtil;

public class ARDCv1IdentifierProvider implements IdentifierProvider {

	/**
	 * Finds the resourceIdentifier of an IGSN record with ARDC v1 schema
	 * @param content XML String of one ARDCv1 Resource document
	 * @return The resourceIdentifier as String
	 */
	@Override
	public String get(String content) {
		String identifierValue = "";

		String xpath = "/resources/resource/resourceIdentifier";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > 0) {
				identifierValue = l.item(0).getFirstChild().getNodeValue();
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
				identifierValue = l.item(position).getFirstChild().getNodeValue();
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
				identifiers.add(l.item(i).getFirstChild().getNodeValue().toUpperCase());
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
		// ardcv1 identifier contains the prefix so nothing to do here
	}


}
