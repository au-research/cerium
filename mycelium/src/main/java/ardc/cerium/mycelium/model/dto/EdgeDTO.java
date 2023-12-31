package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
public class EdgeDTO {

	private Long id;

	private String type;

	private String description;

	private String url;

	private String notes;

	private String origin;

	private boolean isReverse = false;

	private boolean isDuplicate = false;

	private boolean isPublic = true;

	private boolean isInternal = true;

	private Date updatedAt;

	private Date createdAt;
}
