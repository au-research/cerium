package ardc.cerium.mycelium.rifcs;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import ardc.cerium.mycelium.util.CSVUtil;
import org.springframework.cache.annotation.Cacheable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RelationshipsLookUpService {

    private List<RelationLookupEntry> entries;

    /**
     * RelationshipsLookUpService loads a CVS file containing all known and defined relationships
     *
     */
    RelationshipsLookUpService(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String resourceName = "relations.csv";
            Path path = Path.of(Objects.requireNonNull(classLoader.getResource(resourceName)).getPath());
            entries = CSVUtil.readCSV(path);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    /**
     * getEntry find an entry with the given direct relationType
     * @param typeValue a relationship type as (@link String) from the supported list from rifcs vocabs
     * @return (@link RelationLookupEntry) if an entry was found otherwise null given
     */
    @Cacheable
    public RelationLookupEntry getEntry(String typeValue){
        // remove all non word characters and lowercase the value
        typeValue = typeValue.replaceAll("\\W", "").toLowerCase(Locale.ROOT);
        for(RelationLookupEntry entry:entries){
            // make search case insensitive
            if(entry.getRelationType().toLowerCase(Locale.ROOT).equals(typeValue)){
                return entry;
            }
        }
        return null;
    }

}
