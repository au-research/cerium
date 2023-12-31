package ardc.cerium.mycelium.service;

import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.rifcs.model.Identifier;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@Import(IdentifierNormalisationService.class)
class IdentifierNormalisationServiceTest {

    @Test
    void testDOIs(){
        HashMap<String, String> testCase = new HashMap<String, String>();
        List<HashMap> testCases = new ArrayList<HashMap>();
        testCase.put("value","DOI:10.234/455");
        testCase.put("type","doi");
        testCase.put("expectedValue","10.234/455");
        testCase.put("expectedType","doi");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","http://doi.org/10.234/455");
        testCase.put("type","doi");
        testCase.put("expectedValue","10.234/455");
        testCase.put("expectedType","doi");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","https://doi.org/10.234/455");
        testCase.put("type","url");
        testCase.put("expectedValue","10.234/455");
        testCase.put("expectedType","doi");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","https://doi.org/10.234/455");
        testCase.put("type","uri");
        testCase.put("expectedValue","10.234/455");
        testCase.put("expectedType","doi");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","10.234/455");
        testCase.put("type","doi");
        testCase.put("expectedValue","10.234/455");
        testCase.put("expectedType","doi");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","1.234/455");
        testCase.put("type","fish");
        testCase.put("expectedValue","1.234/455");
        testCase.put("expectedType","fish");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","http://doi.org/1.234/455");
        testCase.put("type","url");
        testCase.put("expectedValue","doi.org/1.234/455");
        testCase.put("expectedType","url");
        testCases.add(testCase);
        testCase = new HashMap<String, String>();
        testCase.put("value","http://hdl.handle.net/2328.1/1134");
        testCase.put("type","raid");
        testCase.put("expectedValue","2328.1/1134");
        testCase.put("expectedType","raid");
        testCases.add(testCase);
        Identifier identifier = new Identifier();
        String newValue = null;
        String newType = null;
        for(HashMap<String, String> testcase:testCases){
            identifier.setValue(testcase.get("value"));
            identifier.setType(testcase.get("type"));
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getType()).isEqualTo(testcase.get("expectedType"));
            assertThat(identifier.getValue()).isEqualTo(testcase.get("expectedValue"));
        }
    }

    @Test
    void testNullValues(){
        Collection tests = Arrays.asList(new String[][] {
                {null,null},
        });
        Identifier identifier = new Identifier();
        for(Object test:tests){
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            Assert.assertThrows(ContentNotSupportedException.class, () -> IdentifierNormalisationService.getNormalisedIdentifier(identifier));
        }
    }

    @Test
    void testORCID(){
        Collection tests = Arrays.asList(new String[][] {
                { "http://http://orcid.org/0000-0001-7212-0667","uri","0000-0001-7212-0667","orcid"},
                { "http://orcid.org/0000-0002-9539-5716","url","0000-0002-9539-5716","orcid"},
                { "https://orcid.org/0000-0002-9539-5716","url","0000-0002-9539-5716","orcid"},
                { "https://orcid.org/0000-0002-9539-5716/userInfo.csv","url","0000-0002-9539-5716","orcid"},
                { "0000-0002-9539-5716","orcid","0000-0002-9539-5716","orcid"},
                { "http://orcid.org/index.php","url","orcid.org/index.php","url"},
                { "http://forcid.org/9539-5716","url","forcid.org/9539-5716","url"}
        });
        Identifier identifier = new Identifier();
        for(Object test:tests){
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }


    @Test
    void testHandles() {
        Collection tests = Arrays.asList(new String[][]{
                {"http://handle.westernsydney.edu.au:8081/1959.7/512474", "uri", "1959.7/512474", "handle"},
                {"hdl:1959.7/512474", "handle", "1959.7/512474", "handle"},
                {"hdl:1959.7/512474", "global", "1959.7/512474", "handle"},
                {"hdl.handle.net/1959.7/512474", "url", "1959.7/512474", "handle"},
                {"https://hdl.handle.net/1959.7/512474", "uri", "1959.7/512474", "handle"},
                {"http://hdl.handle.net/1959.7/512474", "handle", "1959.7/512474", "handle"},
                {"1959.7/512474", "handle", "1959.7/512474", "handle"},
                {"http://researchdata.ands.org.au/view/?key=http://hdl.handle.net/1959.14/201435", "uri",
                        "researchdata.ands.org.au/view/?key=http://hdl.handle.net/1959.14/201435", "uri"}
        });
        Identifier identifier = new Identifier();
        for (Object test : tests) {
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }


    @Test
    void testPURLS() {
        Collection tests = Arrays.asList(new String[][]{
                {"http://purl.org/au-research/grants/nhmrc/GNT1002592","uri","https://purl.org/au-research/grants/nhmrc/GNT1002592","purl"},
                {"http://purl.org/au-research/grants/nhmrc/GNT1002592","purl","https://purl.org/au-research/grants/nhmrc/GNT1002592","purl"},
                {"https://purl.org/au-research/grants/nhmrc/GNT1002592","global","https://purl.org/au-research/grants/nhmrc/GNT1002592","purl"},
                {"https://purl.org/au-research/grants/nhmrc/GNT1002592","url","https://purl.org/au-research/grants/nhmrc/GNT1002592","purl"}        });
        Identifier identifier = new Identifier();
        for (Object test : tests) {
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }


    @Test
    void testNLAParties(){
        Collection tests = Arrays.asList(new String[][]{
                {"http://nla.gov.au/nla.party-1692395","uri","nla.party-1692395","au-anl:peau"},
                {"http://nla.gov.au/nla.party-1692394","nla-party","nla.party-1692394","au-anl:peau"},
                {"http://nla.gov.au/nla.party-1692393","AU-VANDS","nla.party-1692393","au-anl:peau"},
                {"nla.gov.au/nla.party-1692396","AU-QGU","nla.party-1692396","au-anl:peau"},
                {"https://nla.gov.au/nla.party-1692397","AU-QUT","nla.party-1692397","au-anl:peau"},
                {"http://nla.gov.au/nla.party-1692398","nla.party","nla.party-1692398","au-anl:peau"},
                {"nla.party-1692399","AU-ANL:PEAU","nla.party-1692399","au-anl:peau"},
                {"nla.party-1692390","AU-QGU","nla.party-1692390","au-anl:peau"},
                {"16923952","NLA.PARTY","nla.party-16923952","au-anl:peau"}
       });
        Identifier identifier = new Identifier();
        for (Object test : tests) {
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }


    @Test
    void testIGSN(){
        Collection tests = Arrays.asList(new String[][] {
                {"http://igsn.org/AU1243", "IGSN", "AU1243", "igsn"},
                {"https://igsn.org/AU1244", "url", "AU1244", "igsn"},
                {"hdl.handle.net/10273/AU1245", "handle", "AU1245", "igsn"},
                {"10273/AU1246", "igsn", "AU1246", "igsn"},
                {"au1247", "igsn", "AU1247", "igsn"},
                {"https://igsn.org/AU1248", "uri", "AU1248", "igsn"}
        });
        Identifier identifier = new Identifier();
        for(Object test:tests){
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }


    @Test
    void testROR(){
        Collection tests = Arrays.asList(new String[][] {
                {"0402tt118", "ROR", "ror.org/0402tt118", "ror"},
                {"ror.org/0402tt118", "ror", "ror.org/0402tt118", "ror"},
                {"http://ror.org/0402tt118", "url", "ror.org/0402tt118", "ror"},
                {"https://ror.org/0402tt118", "ROR", "ror.org/0402tt118", "ror"}
        });
        Identifier identifier = new Identifier();
        for(Object test:tests){
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }
    @Test
    void testRemovalOfhttpprotocol(){
        Collection tests = Arrays.asList(new String[][] {
                {"http://geoserver-123.aodn.org.au/geoserver/ncwms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities",
                        "url","geoserver-123.aodn.org.au/geoserver/ncwms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities","url"},
                {"https://geoserver.imas.utas.edu.au/geoserver/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities","uri",
                        "geoserver.imas.utas.edu.au/geoserver/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities","uri"},
                {"http://google.com","local","google.com","local"},
                {"fish.org","global","fish.org","global"},
                {"https://fish.org?url=http://google.com","noidea","fish.org?url=http://google.com","noidea"},
                {"fish.org?url=http://google.com","uri","fish.org?url=http://google.com","uri"}
        });
        Identifier identifier = new Identifier();
        for(Object test:tests){
            String[] testcase = (String[]) test;
            identifier.setValue(testcase[0]);
            identifier.setType(testcase[1]);
            identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase[2]);
            assertThat(identifier.getType()).isEqualTo(testcase[3]);
        }
    }

}