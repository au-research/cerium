package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.XMLUtil;
import ardc.cerium.mycelium.harvester.RDAHarvester;
import ardc.cerium.mycelium.model.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class RDAHarvestingService {

    private static final Logger log = LoggerFactory.getLogger(RDAHarvestingService.class);

    @Autowired
    RecordService recordService;

    @Autowired
    VersionService versionService;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
	MyceliumService myceliumService;

    public void harvest(String host) {
        int limit = 100;
        int offset = 0;

        RDAHarvester harvester = new RDAHarvester(host);

        RegistryObject[] registryObjects = harvester.harvestRecords(limit, offset);
        while(registryObjects.length > 0) {

            for (RegistryObject registryObject : registryObjects) {
                log.info("Harvesting Record: {}", registryObject.getRegistryObjectId());
                String rifcs = harvester.harvestRIFCS(registryObject.getRegistryObjectId());

                // todo use Supplier<T> pattern to yield registryObject and rifcs?
                //saveFile(registryObject, rifcs);

                try {
                    myceliumService.ingest(rifcs);
                } catch (Exception e) {
                    log.error("Failed to ingest {} due to {}", registryObject.getRegistryObjectId(), e.getMessage());
                }

            }

            // next page
            offset += limit;
            registryObjects = harvester.harvestRecords(limit, offset);
        }
    }

    public void saveFile(RegistryObject ro, String rifcs) {
        String filePath = String.format("%s/%s.xml", applicationProperties.getDataPath(), ro.getRegistryObjectId().toString());
        log.info("Writing rifcs to {}", filePath);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(filePath);
            writer.write(rifcs);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ingest(RegistryObject ro, String xml) {
        Record record = new Record();
        record.setType("RegistryObject");
        recordService.save(record);

        Version version = new Version();
        version.setRecord(record);
        version.setContent(xml.getBytes(StandardCharsets.UTF_8));
        version.setHash(VersionService.getHash(xml));
        versionService.save(version);
    }

    public void ingest(String rifcs) {

        // extract key
        String key;
        try {
            NodeList list = XMLUtil.getXPath(rifcs, "//key");
            Node keyNode = list.item(0);
            key = keyNode.getFirstChild().getNodeValue();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        // find existing record with the same key

        // if exists -> update record
        // if not exists -> create record
        // todo persist Record.type=RegistryObject
        // todo persist Identifier.type=ro:key

        // extract identifiers -> more identifiers
        try {
            NodeList identifierNodeList = XMLUtil.getXPath(rifcs, "//identifier");
            for(int i = 0; i < identifierNodeList.getLength(); i++) {
                Node identifierNode = identifierNodeList.item(i);
                String type = identifierNode.getAttributes().getNamedItem("type").getNodeValue();
                String value = identifierNode.getFirstChild().getNodeValue();

                // todo persists Identifier.type=type, value=value, record=record
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }


        // extract relatedObject -> edges
        try {
            NodeList relatedObjectNodeList = XMLUtil.getXPath(rifcs, "//relatedObject");
            for(int i = 0; i < relatedObjectNodeList.getLength(); i++) {
                Node relatedObjectNode = relatedObjectNodeList.item(i);
                NodeList relatedObjectNodeChildNodes = relatedObjectNode.getChildNodes();
                for (int j = 0; j < relatedObjectNodeChildNodes.getLength(); j++) {
                    String nodeName = relatedObjectNodeChildNodes.item(0).getNodeName();
                    String nodeValue = relatedObjectNodeChildNodes.item(0).getNodeValue();
                    System.out.println(nodeName);
                    System.out.println(nodeValue);
				}
                System.out.println(relatedObjectNodeChildNodes);
                // todo persists Identifier.type=type, value=value, record=record
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        // extract relatedInfo -> more nodes and edges
        // relatedInfo.identifiers become nodes -> neo4j
        // relatedInfo.relation becomes edges

    }

}
