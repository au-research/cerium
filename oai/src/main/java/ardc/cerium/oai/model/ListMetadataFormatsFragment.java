package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

public class ListMetadataFormatsFragment {

	@JsonProperty("metadataFormat")
	@JacksonXmlElementWrapper(useWrapping = false)

	private List<MetadataFormatFragment> listMetadataFormats = new ArrayList<MetadataFormatFragment>();

	public void setMetadataFormat(String metadataPrefix, String schema, String metadataNamespace) {
		MetadataFormatFragment newFormat = new MetadataFormatFragment();
		newFormat.setFormat(metadataPrefix, schema, metadataNamespace);
		listMetadataFormats.add(newFormat);
	}

	public List<MetadataFormatFragment> getMetadataFormat() {
		return this.listMetadataFormats;
	}

}
