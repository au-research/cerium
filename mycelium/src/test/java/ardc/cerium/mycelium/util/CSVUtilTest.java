package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CSVUtilTest {


    @Test
    void loadTest(){
        Path csvFilePath = Paths.get("src","main", "resources", "relations.csv");
        try {
            List<RelationLookupEntry> relationsList =  CSVUtil.readCSV(csvFilePath);
            assertThat(relationsList.size()).isGreaterThan(20);
        }
        catch (FileNotFoundException e){
            System.err.println(e.getMessage());
        }
    }


}