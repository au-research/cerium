package ardc.cerium.mycelium.rifcs;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
public class RecordState {

	private String registryObjectId;

	private String title;

	private String group;

	private Vertex origin;

	private Collection<Vertex> identical = new ArrayList<>();

	private Collection<Relationship> outbounds = new ArrayList<>();

}
