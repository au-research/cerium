package ardc.cerium.mycelium.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelationLookupEntry {

    @CsvBindByName(column = "relationType")
    private String relationType;

    @CsvBindByName(column = "reverseRelationType")
    private String reverseRelationType;

    @CsvBindByName(column = "defaultText")
    private String defaultText;

    @CsvBindByName(column = "collectionText")
    private String collectionText;

    @CsvBindByName(column = "partyText")
    private String partyText;

    @CsvBindByName(column = "serviceText")
    private String serviceText;

    @CsvBindByName(column = "activityText")
    private String activityText;

}
