package ardc.cerium.core.common.transform;

import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class XSLTransformer {

	/**
	 * Using XSLT to transform a String into another String with the provided parameter
	 * map
	 * @param schemaPath path to the schema
	 * @param xml XML String
	 * @param parameters a Map of xslt parameters
	 * @return String result of the transformation
	 */
	public static String transform(String schemaPath, String xml, Map<String, String> parameters) {
		try {
			// setup document source from the provided xml
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder builder;
			builder = documentBuilderFactory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
			DOMSource DOMSource = new DOMSource(document);

			// setup transformer factory with the xslt file at the provided path
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer;

			Source xslt = new StreamSource(new ClassPathResource(schemaPath).getInputStream());
			transformer = factory.newTransformer(xslt);

			// set parameters if any
			if (parameters != null) {
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					if(entry.getValue() != null){
						transformer.setParameter(entry.getKey(), entry.getValue());
					}
				}
			}

			// setup input stream for the transformer
			StringWriter stringWriter = new StringWriter();
			StreamResult resultStream = new StreamResult(stringWriter);

			// do the transform
			transformer.transform(DOMSource, resultStream);

			// the result is available via the StringWriter
			return stringWriter.toString();

		}
		catch (SAXException | ParserConfigurationException | IOException | TransformerException e) {
			e.printStackTrace();
			return null;
		}
	}

}
