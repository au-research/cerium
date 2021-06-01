package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.aspectj.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.nio.file.Path;
import java.util.List;

public class CSVUtil {

    /**
     * Parse a CSV file containing  a List of {@link RelationLookupEntry}
     * @param relationsFilePath the Path to the CSV File
     * @return a {@link List} of {@link RelationLookupEntry}
     */
    public static List<RelationLookupEntry> readCSV(Path relationsFilePath) throws FileNotFoundException {

        HeaderColumnNameMappingStrategy<RelationLookupEntry> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(RelationLookupEntry.class);

        CsvToBean<RelationLookupEntry> csvToBean = new CsvToBeanBuilder<RelationLookupEntry>(new FileReader(relationsFilePath.toFile()))
                .withMappingStrategy(strategy).withIgnoreLeadingWhiteSpace(true).build();

        return csvToBean.parse();
    }

}
