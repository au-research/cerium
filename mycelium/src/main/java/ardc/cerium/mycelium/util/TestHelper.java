package ardc.cerium.mycelium.util;
import ardc.cerium.mycelium.model.DataSource;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import ardc.cerium.mycelium.rifcs.model.BaseRegistryObjectClass;
import ardc.cerium.mycelium.rifcs.model.RegistryObjects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
creates a List containing Json payload from any set of XML containing registryObject[s]

 */
public class TestHelper {

    static List<String> buildJsonPackages(String xmlSource) {
        RegistryObject ro = new RegistryObject();
        List<String> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        long registryObjectId = 100000;
        DataSource datasource = new DataSource();
        datasource.setKey("99");
        datasource.setTitle("Test DataSource");
        datasource.setId(11111L);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlSource));
            Document doc = builder.parse(inputSource);
            NodeList nList = doc.getElementsByTagName("registryObject");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                String rifcs = getString(nNode);
                ro.setRifcs(new String(Base64.getEncoder().encode(rifcs.getBytes())));
                ro.setKey(getKey(nNode));
                ro.setClassification(getClass(nNode));
                ro.setGroup(getGroup(nNode));
                ro.setType(getType(nNode));
                ro.setStatus("PUBLISHED");
                ro.setDataSource(datasource);
                ro.setRegistryObjectId(registryObjectId++);
                ro.setTitle("not yet implemented");
                result.add(mapper.writeValueAsString(ro));
            }
        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String getString(Node node) throws TransformerException {
        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }

    public static String getKey(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals("key")) {
                return children.item(i).getFirstChild().getNodeValue();
            }
        }
        return "";
    }

    public static String getClass(Node node) {

        Set<String> classSet = Set.of("party","collection","activity","service");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (classSet.contains(children.item(i).getNodeName())) {
                return children.item(i).getNodeName();
            }
        }
        return "";
    }

    public static String getType(Node node) {

        Set<String> classSet = Set.of("party","collection","activity","service");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (classSet.contains(children.item(i).getNodeName())) {
                NamedNodeMap attributes = children.item(i).getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    if (attributes.item(j).getNodeName().equals("type")) {
                        return attributes.item(j).getNodeValue();
                    }
                }
            }
        }
        return "";
    }


    public static String getGroup(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.item(i).getNodeName().equals("group")) {
                return attributes.item(i).getNodeValue();
            }
        }
        return "";
    }

}