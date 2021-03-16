package ardc.cerium.drvs.provider;

import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VocabService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.common.util.XMLUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.w3c.dom.Element;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VocabService.class })
class DOIMetadataQualityProviderTest {

    @Autowired
    VocabService vocabService;

    @Test
    void get() throws IOException {
        /**
         * 		rules.put("E01", false);
         * 		rules.put("E02", false);
         * 		rules.put("E03", false);
         * 		rules.put("E04", false);
         * 		rules.put("E05", false);
         * 		rules.put("E06", false);
         * 		rules.put("V01", false);
         * 		rules.put("V02", false);
         * 		rules.put("V03", false);
         * 		rules.put("V04", false);
         * 		rules.put("V05", false);
         * 		rules.put("V06", false);
         * 		rules.put("V07", false);
         */
        DOIMetadataQualityProvider doiMetadataQualityProvider = new DOIMetadataQualityProvider();
        doiMetadataQualityProvider.setVocabService(vocabService);
        // a DataCite XML that passes all rules
        String xml = Helpers.readFile("src/test/resources/xml/sample_datacite_xml2.xml");
        String localIdentifier = "llapax";
        doiMetadataQualityProvider.setLocalIdentifierValue(localIdentifier);
        Element domElement = XMLUtil.getDomDocument(xml);
        Boolean result;
        result = doiMetadataQualityProvider.testRule("E01", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("E02", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("E03", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("E04", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("E05", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("E06", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V01", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V02", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V03", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V04", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V05", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V06", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("V07", domElement);
        Assert.assertTrue(result);
        result = doiMetadataQualityProvider.testRule("ZZZ", domElement);
        Assert.assertFalse(result);
    }
}