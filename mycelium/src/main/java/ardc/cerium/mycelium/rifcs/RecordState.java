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

	public String registryObjectKey;

	private String title;

	private String registryObjectClass;

	private String registryObjectType;

	private String group;

	private Vertex origin;

	private String dataSourceId;

	private Collection<Vertex> identical = new ArrayList<>();

	private Collection<Relationship> outbounds = new ArrayList<>();

	private Collection<Vertex> identifiers = new ArrayList<>();

}
