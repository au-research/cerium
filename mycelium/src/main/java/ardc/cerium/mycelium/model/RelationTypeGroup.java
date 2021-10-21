package ardc.cerium.mycelium.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class RelationTypeGroup {

	public String relation;

	private List<Vertex.Label> labels;

	private String objectClass;

	private String objectType;

	private int count;

}
