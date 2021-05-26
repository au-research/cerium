package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.rifcs.model.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import(IdentifierNormalisationService.class)
class IdentifierNormalisationServiceTest {

    @Autowired
    private IdentifierNormalisationService identifierNormalisationService;

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
        testCase.put("expectedValue","http://doi.org/1.234/455");
        testCase.put("expectedType","url");
        testCases.add(testCase);
        Identifier identifier = new Identifier();
        String newValue = null;
        String newType = null;
        for(HashMap<String, String> testcase:testCases){
            identifier.setValue(testcase.get("value"));
            identifier.setType(testcase.get("type"));
            identifier = identifierNormalisationService.getNormalisedIdentifier(identifier);
            assertThat(identifier.getValue()).isEqualTo(testcase.get("expectedValue"));
            assertThat(identifier.getType()).isEqualTo(testcase.get("expectedType"));
        }
    }


}