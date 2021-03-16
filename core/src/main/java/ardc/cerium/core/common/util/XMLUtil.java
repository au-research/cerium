package ardc.cerium.core.common.util;

import ardc.cerium.core.exception.ContentNotSupportedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import org.xmlunit.builder.Input;

//import org.xmlunit.diff.ComparisonControllers;
//import org.xmlunit.diff.DefaultNodeMatcher;
//import org.xmlunit.diff.Diff;
//import org.xmlunit.diff.ElementSelectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class XMLUtil {

	public static NodeList getXPath(String xml, String xpath)
			throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
		InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document xmlDocument = builder.parse(xmlStream);

		XPath xPath = XPathFactory.newInstance().newXPath();
		return (NodeList) xPath.compile(xpath).evaluate(xmlDocument, XPathConstants.NODESET);
	}

	public static Element getDomDocument(String xml){
		Element element = null;
		try {
			InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(xmlStream);
			element = document.getDocumentElement();
		}
		catch (Exception e){
			return null;
		}
		return element;
	}

	public static String getNamespaceURI(String xml) throws ContentNotSupportedException {
		String nameSpace = "";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
			Document doc = builder.parse(xmlStream);
			Element root = doc.getDocumentElement();
			String rootPrefix = root.getPrefix();
			// the default namespace (no prefix)
			if (rootPrefix == null)
				rootPrefix = "xmlns";

			NamedNodeMap attributes = root.getAttributes();
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					Node node = attributes.item(i);
					if (node.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/")
							&& node.getLocalName().equals(rootPrefix)) {
						nameSpace = node.getNodeValue();
					}

				}
			}
		}
		catch (Exception e) {
			throw new ContentNotSupportedException(e.getMessage());
		}
		if (nameSpace.equals("")) {
			throw new ContentNotSupportedException("Namespace unavailable");
		}
		return nameSpace;
	}

	public static boolean compareRegistrationMetadata(byte[] currentContent, byte[] newContent) {
		boolean different = true;
		return different;
	}

}
