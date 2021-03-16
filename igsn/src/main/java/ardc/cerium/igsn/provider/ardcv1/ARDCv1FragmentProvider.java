package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.provider.FragmentProvider;
import ardc.cerium.core.common.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

@SuppressWarnings("unused")
public class ARDCv1FragmentProvider implements FragmentProvider {

	/***
	 * the resources container for ARDCv1 IGSN resource(s)
	 *
	 */
	private static final String container = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<resources xmlns=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc\"" +
			" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
			" xsi:schemaLocation=\"https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc" +
			" https://identifiers.ardc.edu.au/igsn-schema/descriptive/1.0/resource.xsd\">" + "</resources>";

	/**
	 * returns 1 resource elements from a given resources content at the given position
	 * @param content the entire content of the payload that contains one or many
	 * resource(s)
	 * @param position int the position of the resource element
	 * @return an XML String as an ARDC v1 resource document
	 */
	@Override
	public String get(String content, int position) {
		String fragment = "";

		String xpath = "/resources/resource";

		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			if (l.getLength() > 0) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(container));
				Document doc = builder.parse(is);
				Node e = doc.importNode(l.item(position), true);
				doc.getElementsByTagName("resources").item(0).appendChild(e);
				TransformerFactory transFactory = TransformerFactory.newInstance();
				Transformer transformer = transFactory.newTransformer();
				StringWriter buffer = new StringWriter();
				transformer.transform(new DOMSource(doc), new StreamResult(buffer));
				fragment = buffer.toString();
			}
		}
		catch (TransformerException | XPathExpressionException | ParserConfigurationException | IOException
				| SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fragment;
	}

	/**
	 * @param content String containing the entire IGSNv1 payload that contains 1 or many
	 * resource(s)
	 * @return (int) the number of resource elements in the payload
	 */
	@Override
	public int getCount(String content) {
		int count = 0;
		String xpath = "/resources/resource";
		try {
			NodeList l = XMLUtil.getXPath(content, xpath);
			count = l.getLength();
		}
		catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return count;
	}

}
