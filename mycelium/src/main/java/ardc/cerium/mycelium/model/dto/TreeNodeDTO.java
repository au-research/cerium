package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TreeNodeDTO {

	private String identifier;

	private String identifierType;

	private String parentId;

	private String title;

	private String url;

	private String objectClass;

	private String objectType;

	private Integer childrenCount = 0;

	private List<TreeNodeDTO> children = new ArrayList<>();

}
