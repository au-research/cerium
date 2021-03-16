package ardc.cerium.core.common.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XMLUtilTest {

	@Test
	public void findDefaultXMLNamespace() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		String nameSpace = XMLUtil.getNamespaceURI(xml);
		assertEquals(nameSpace, "https://igsn.csiro.au/schemas/3.0");
	}

	@Test
	public void getIdentifierFromCSIROXML() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		String xpath = "/resources/resource/resourceIdentifier";
		NodeList l = XMLUtil.getXPath(xml, xpath);
		String identifier = l.item(0).getFirstChild().getNodeValue();
		assertEquals(identifier, "CSTSTDOCO1");
	}

	@Test
	public void getIdentifierFromCSIROWithPrefixXML() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_xml_prefix_cs_igsn.xml");
		String xpath = "/resources/resource/resourceIdentifier";
		NodeList l = XMLUtil.getXPath(xml, xpath);
		String identifier = l.item(0).getFirstChild().getNodeValue();
		assertEquals(identifier, "XXAA45CJ1N");
	}

	@Test
	public void getIdentifierFromARDCXML() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		String xpath = "/resources/resource/resourceIdentifier";
		NodeList l = XMLUtil.getXPath(xml, xpath);
		String identifier = l.item(0).getFirstChild().getNodeValue();
		assertEquals(identifier, "10273/XX0TUIAYLV");
	}

	@Test
	public void findprefixXMLNamespace() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_xml_prefix_cs_igsn.xml");
		String nameSpace = XMLUtil.getNamespaceURI(xml);
		assertEquals(nameSpace, "https://igsn.csiro.au/schemas/3.0");
	}

}