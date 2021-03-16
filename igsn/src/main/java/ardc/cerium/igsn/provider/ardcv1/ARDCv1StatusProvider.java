package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.provider.StatusProvider;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ARDCv1StatusProvider implements StatusProvider {

	@Override
	public String get(Record record) {

		// reserved when the IGSN identifier is in Reserved state
		Identifier igsn = record.getIdentifiers().stream()
				.filter((identifier -> identifier.getType().equals(Identifier.Type.IGSN))).findFirst().orElse(null);
		if (igsn != null && igsn.getStatus().equals(Identifier.Status.RESERVED)) {
			return Status.Reserved.toString();
		}

		// Destroyed|Deprecated|Registered|Updated from the latest version logDate
		Version ardcv1 = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(SchemaService.ARDCv1)).findFirst().orElse(null);
		if (ardcv1 == null) {
			// this shouldn't be happening but the record doesn't have an ARDCv1 current
			// version (reserved?)
			return Status.Unknown.toString();
		}

		// obtain eventType
		String content = new String(ardcv1.getContent());
		String eventType;
		try {
			NodeList nodeList = XMLUtil.getXPath(content, "//logDate");
			Node logDateNode = nodeList.item(0);
			Element elem = (Element) logDateNode;
			eventType = elem.getAttribute("eventType");
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return Status.Unknown.toString();
		}

		// conditional returns based on the eventType
		switch(eventType) {
			case "registered":
			case "updated":
				return Status.Registered.toString();
			case "destroyed":
				return Status.Destroyed.toString();
			case "deprecated":
				return Status.Deprecated.toString();
			default:
				return Status.Unknown.toString();
		}
	};

	public static enum Status {

		Reserved, Registered, Destroyed, Deprecated, Unknown

	}

}
