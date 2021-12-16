package ardc.cerium.mycelium.util;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.mycelium.model.AdditionalRelation;
import ardc.cerium.mycelium.model.DataSource;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import ardc.cerium.mycelium.rifcs.model.BaseRegistryObjectClass;
import ardc.cerium.mycelium.rifcs.model.RegistryObjects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.solr.core.query.result.Cursor;
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

import static ardc.cerium.mycelium.service.MyceliumRequestService.IMPORT_REQUEST_TYPE;

/**
creates a List containing Json payload from any set of XML containing registryObject[s]

 */
public class TestHelper {

    public static List<String> getJSONImportPayload(String rifcses, List<String> titles, ardc.cerium.mycelium.rifcs.model.datasource.DataSource dataSource, List<AdditionalRelation> additionalRelations, int startingIDIncrement) {
        List<String> result = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        // convert DataSource model
        if (dataSource == null) {
            dataSource = TestHelper.mockDataSource();
        }
        DataSource ds = new DataSource();
        ds.setKey(dataSource.getId());
        ds.setId(Long.valueOf(dataSource.getId()));
        ds.setTitle(dataSource.getTitle());

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(rifcses));
            Document doc = builder.parse(inputSource);
            NodeList nList = doc.getElementsByTagName("registryObject");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                RegistryObject ro = new RegistryObject();
                ro.setRifcs(new String(Base64.getEncoder().encode(wrapRifcs(getString(nNode)).getBytes())));
                ro.setKey(getKey(nNode));
                String objectClass = getClass(nNode);
                ro.setClassification(objectClass);
                ro.setGroup(getGroup(nNode));
                String objectType = getType(nNode);
                ro.setType(objectType);
                ro.setStatus("PUBLISHED");
                ro.setDataSource(ds);
                ro.setRegistryObjectId((long) i+1 + startingIDIncrement);
                ro.setTitle(
                        titles == null || i >= titles.size()
                                ? String.format("%s %s autogen title #%s", objectClass, objectType, i)
                                : titles.get(i)
                );
              	ro.setAdditionalRelations(additionalRelations != null
						? additionalRelations.stream().toArray(AdditionalRelation[]::new) : new AdditionalRelation[0]);
                result.add(mapper.writeValueAsString(ro));
            }
        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<String> buildJsonPackages(String xmlSource) {
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

    public static String wrapRifcs(String rifcs) {
        return "<registryObjects xmlns=\"http://ands.org.au/standards/rif-cs/registryObjects\" xmlns:extRif=\"http://ands.org.au/standards/rif-cs/extendedRegistryObjects\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd\">" + rifcs + "</registryObjects>";
    }

    public static ardc.cerium.mycelium.rifcs.model.datasource.DataSource mockDataSource() {
        ardc.cerium.mycelium.rifcs.model.datasource.DataSource dataSource = new ardc.cerium.mycelium.rifcs.model.datasource.DataSource();
        dataSource.setId("1");
        dataSource.setTitle("Test DataSource");
        return dataSource;
    }

    public static RequestDTO mockRequestDTO(String type) {
        RequestDTO dto = new RequestDTO();
        dto.setType(type);
        return dto;
    }

    public static List<RelationshipDocument> cursorToList(Cursor<RelationshipDocument> cursor) {
        List<RelationshipDocument> result = new ArrayList<>();
        while (cursor.hasNext()) {
            result.add(cursor.next());
        }
        return result;
    }

}