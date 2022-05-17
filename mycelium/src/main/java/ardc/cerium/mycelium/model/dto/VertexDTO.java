package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class VertexDTO {

    private String identifier;

    private String identifierType;

    private List<String> labels;

    private String objectType;

    private String objectClass;

    private String url;

    private String title;

    private String group;

    private String dataSourceId;

    private Date createdAt;

    private Date updatedAt;

    private String status;

    private Map<String, Object> meta;

}
