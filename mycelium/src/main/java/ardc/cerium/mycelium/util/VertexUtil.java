package ardc.cerium.mycelium.util;

import ardc.cerium.doi.ContentNegotiationClient;
import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.orcid.PublicOrcidClient;
import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertexUtil {

	public static void normalise(Vertex vertex) {
		// todo implement & test
		return;
	}

	/**
	 * Resolve a Vertex based on the supported identifier types
	 *
	 * Should be done after the vertex is normalised via {@link #normalise(Vertex)}. Sets
	 * the Vertex title based on the resolved value
	 * @param vertex
	 */
	public static void resolveVertex(Vertex vertex) {

		String identifierType = vertex.getIdentifierType();
		String identifierValue = vertex.getIdentifier();

		if (identifierType.equals("doi")) {
			// todo check identifierValue to match a DOI regex first
			try {
				ContentNegotiationClient client = new ContentNegotiationClient();
				CiteProcJson citeProcJson = client.resolveCiteProcJson(identifierValue);
				String title = citeProcJson.getTitle();
				vertex.setTitle(title);
			}
			catch (Exception e) {
				log.warn("Failed to resolve identifier for Vertex[identifier={}, type={}] Reason: {}", identifierValue,
						identifierType, e.getMessage());
				return;
			}
		}
		else if (identifierType.equals("orcid")) {
			// todo check identifierValue to match an ORCID regex first
			try {
				PublicOrcidClient client = new PublicOrcidClient();
				OrcidRecord orcidRecord = client.resolve(identifierValue);
				String title = orcidRecord.getPerson().getName().getFullName();
				vertex.setTitle(title);
			}
			catch (Exception e) {
				log.warn("Failed to resolve identifier for Vertex[identifier={}, type={}] Reason: {}", identifierValue,
						identifierType, e.getMessage());
				return;
			}
		}
	}

}
