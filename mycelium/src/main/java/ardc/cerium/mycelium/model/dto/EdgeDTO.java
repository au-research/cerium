package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EdgeDTO {

	private String type;

	private String description;

	private String url;

	private String origin;

	private boolean isReverse;

	private boolean isImplicit;

	private boolean isPublic = true;

	private boolean isInternal = true;

}
