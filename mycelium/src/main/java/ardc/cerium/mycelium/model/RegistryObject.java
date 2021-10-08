package ardc.cerium.mycelium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RegistryObject {

    @JsonProperty("registryObjectId")
    private Long registryObjectId;

    private String rifcs;

    private String status;

    private String key;

    private String title;

    @JsonProperty("list_title")
    private String listTitle;

    private String slug;

    private String portalUrl;

    private String group;

    private String type;

    @JsonProperty("class")
    private String classification;

    @JsonProperty("dataSource")
    private DataSource dataSource;

    @JsonProperty("additionalRelations")
    private AdditionalRelation[] additionalRelations;

}

