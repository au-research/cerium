package ardc.cerium.igsn.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class IGSNRequestValidationServiceTest {

    @Test
    void validate() {
    }

    @Test
     void isvalidIdentifierFormat(){
            Assert.assertTrue(IGSNRequestValidationService.isvalidIdentifierFormat("20.500.11812/XXZT1JBCSV33"));
            Assert.assertTrue(IGSNRequestValidationService.isvalidIdentifierFormat("10273/XX0TUIAYLV"));
            Assert.assertFalse(IGSNRequestValidationService.isvalidIdentifierFormat("FISH"));
            Assert.assertFalse(IGSNRequestValidationService.isvalidIdentifierFormat("FISH/FISH"));
            Assert.assertFalse(IGSNRequestValidationService.isvalidIdentifierFormat("20.500.11812/XXZT1JBCSV33__...."));
            Assert.assertTrue(IGSNRequestValidationService.isvalidIdentifierFormat("20.500.11812/XXZT1VDOHF93EPI234...."));
            Assert.assertFalse(IGSNRequestValidationService.isvalidIdentifierFormat("20.500.11812/XXZT1bBWfc3aaa$"));
        }
}