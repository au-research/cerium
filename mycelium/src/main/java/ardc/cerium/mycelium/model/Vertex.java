package ardc.cerium.mycelium.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.util.*;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Vertex {

	@Property("identifier")
	private String identifier;

	@Property("identifierType")
	private String identifierType;

	@DynamicLabels
	private List<String> labels;

	@Property("objectType")
	private String objectType;

	@Property("objectClass")
	private String objectClass;

	@Property("public")
	private boolean isPublic = true;

	@Property("url")
	private String url;

	@Property("title")
	private String title;

	@Property("listTitle")
	private String listTitle;

	@Property("group")
	private String group;

	@Property("dataSourceId")
	private String dataSourceId;

	@Property("notes")
	private String notes;

	@Property("createdAt")
	private Date createdAt;

	@Property("updatedAt")
	private Date updatedAt;

	@Property("status")
	private String status;

	@Property("meta")
	private String meta = "{}";

	@Id
	@GeneratedValue
	private Long id;

	public Vertex(String identifier, String identifierType) {
		this.identifier = identifier;
		this.identifierType = identifierType;
		labels = new ArrayList<>();
	}

	public Vertex(String identifier, String identifierType, Status status) {
		this.identifier = identifier;
		this.identifierType = identifierType;
		this.setStatus(status);
		labels = new ArrayList<>();
	}

	public void addLabel(Label label) {
		labels.add(label.toString());
	}

    public boolean hasLabel(Label label) {
		return labels.contains(label.toString());
	}

	public void setMetaAttribute(String key, String value) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> map = objectMapper.readValue(meta, Map.class);
			map.put(key, value);
			meta = objectMapper.writeValueAsString(map);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public Object getMetaAttribute(String key) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> map = objectMapper.readValue(meta, Map.class);
			return map.get(key);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return null;
	}

	public enum Label {
		Vertex, RegistryObject, Identifier, DataSource, Cluster, DRAFT, PUBLISHED
	}

	public void setStatus(Status status) {
		this.status = status.name();
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public enum Status {
		DRAFT,PUBLISHED
	}
}
