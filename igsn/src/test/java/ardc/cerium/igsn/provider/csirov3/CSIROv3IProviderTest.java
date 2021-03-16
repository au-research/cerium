package ardc.cerium.igsn.provider.csirov3;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.FragmentProvider;
import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class CSIROv3IProviderTest {

    @Autowired
    SchemaService service;

    @Test
    public void extractIdentifierFromCSIROv3() throws Exception {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
        IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
        provider.setPrefix("10273");
        String identifierValue = provider.get(xml);
        assertEquals("10273/CSTSTDOCO1", identifierValue);
        provider.setPrefix("10273/");
        identifierValue = provider.get(xml);
        assertEquals("10273/CSTSTDOCO1", identifierValue);
    }


    @Test
    void get_fragments() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String xml = Helpers.readFile("src/test/resources/xml/sample_csirov3_batch.xml");
        FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
        String first = fProvider.get(xml, 0);
        assertTrue(first.contains("<resourceTitle>A title worthy for FIRST kings</resourceTitle>"));
        String third = fProvider.get(xml, 2);
        assertTrue(
                third.contains("<resourceTitle>A title worthy for THIRD kings"));
    }

    @Test
    void getfragmentCount() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String xml = Helpers.readFile("src/test/resources/xml/sample_csirov3_batch.xml");
        FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
        int fragCounter = fProvider.getCount(xml);
        assertTrue(fragCounter == 3);
    }

    @Test
    void get_fragmentOnSameDocumentConsistentResult() throws IOException {
        Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
        String original = Helpers.readFile("src/test/resources/xml/sample_csirov3_batch.xml");

        FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
                Metadata.Fragment);
        String firstFragmentOnce = fragmentProvider.get(original, 0);
        String firstFragmentAgain = fragmentProvider.get(original, 0);

        assertThat(firstFragmentOnce).isEqualTo(firstFragmentAgain);
    }
}