package ardc.cerium.mycelium.rifcs;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RecordState {

	private String title;

	private String group;

	private List<Vertex> identical = new ArrayList<>();

	private List<Relationship> outbounds = new ArrayList<>();

}
