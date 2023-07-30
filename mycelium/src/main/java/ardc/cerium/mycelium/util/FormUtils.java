package ardc.cerium.mycelium.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class FormUtils {

    public static List<String> getListFromString(String importedRecordIds){
        List<String> idList = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        if(importedRecordIds == null){
            return idList;
        }
        try{
            idList = mapper.readValue(importedRecordIds, new TypeReference<List<String>>(){});
            return idList;
        }catch (JsonProcessingException e){
            return idList;
        }
    }


}
