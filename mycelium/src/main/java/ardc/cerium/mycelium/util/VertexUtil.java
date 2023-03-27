package ardc.cerium.mycelium.util;

import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.doi.ContentNegotiationClient;
import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.model.Identifier;
import ardc.cerium.mycelium.service.IdentifierNormalisationService;
import ardc.cerium.orcid.PublicOrcidClient;
import ardc.cerium.orcid.schema.orcid.json.Biography;
import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import ardc.cerium.ror.PublicRorClient;
import ardc.cerium.ror.schema.ror.json.RorRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
public class VertexUtil {

	/**
	 * Normalise the Vertex Identifier value and type
	 *
	 * @param vertex the {@link Vertex} to normalise
	 */
	public static void normalise(Vertex vertex) {
		Identifier identifier = new Identifier();
		identifier.setValue(vertex.getIdentifier());
		identifier.setType(vertex.getIdentifierType());
		vertex.setMetaAttribute("rawIdentifierValue", identifier.getValue());
		vertex.setMetaAttribute("rawIdentifierType", identifier.getType());
		// normalise Identifier
		identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
		// sets the normalised value and type to the vertex
		vertex.setIdentifier(identifier.getValue());
		vertex.setIdentifierType(identifier.getType());
	}





	/**
	 * Resolve a Vertex based on the supported identifier types
	 *
	 * Should be done after the vertex is normalised via {@link #normalise(Vertex)}. Sets
	 * the Vertex title based on the resolved value
	 * @param vertex the {@link Vertex} to perform resolution on
	 */
	public static void resolveVertex(Vertex vertex) {

		String identifierType = vertex.getIdentifierType();
		String identifierValue = vertex.getIdentifier();

		if (identifierType.equals("doi")) {
			// todo check identifierValue to match a DOI regex first
			// todo index the vertex in solr instead of neo4j
			try {
				ContentNegotiationClient client = new ContentNegotiationClient();
				CiteProcJson citeProcJson = client.resolveCiteProcJson(identifierValue);
				String title = citeProcJson.getTitle();
				String publisher = citeProcJson.getPublisher();
				String type = citeProcJson.getType();
				String doi_abstract = citeProcJson.getDoi_abstract();
				String source = citeProcJson.getSource();
				if (vertex.getTitle() != null && !vertex.getTitle().isBlank()) {
					vertex.setMetaAttribute("rawTitle", vertex.getTitle());
				}
				if(publisher != null && !publisher.isBlank()){
					vertex.setMetaAttribute("publisher", publisher);
				}
				if(type != null && !type.isBlank()){
					vertex.setMetaAttribute("type", type);
				}
				if(doi_abstract != null && !doi_abstract.isBlank()){
					vertex.setMetaAttribute("abstract", doi_abstract);
				}
				if(source != null && !source.isBlank()){
					vertex.setMetaAttribute("source", source);
				}
				vertex.setTitle(title);
			}
			catch (Exception e) {
				log.warn("Failed to resolve identifier for Vertex[identifier={}, type={}] Reason: {}", identifierValue,
						identifierType, e.getMessage());
				return;
			} finally{
				vertex.setMetaAttribute("lastResolved", Instant.now().toString());
			}
		}
		else if (identifierType.equals("orcid")) {
			// todo check identifierValue to match an ORCID regex first
			try {
				PublicOrcidClient client = new PublicOrcidClient();
				OrcidRecord orcidRecord = client.resolve(identifierValue);
				if(orcidRecord.getPerson() != null){
					String title = orcidRecord.getPerson().getName().getFullName();
					Biography biography = orcidRecord.getPerson().getBiography();
					if (vertex.getTitle() != null && !vertex.getTitle().isBlank()) {
						vertex.setMetaAttribute("rawTitle", vertex.getTitle());
					}
					if (biography!= null && !biography.getContent().isBlank()) {
						vertex.setMetaAttribute("biography", biography.getContent());
					}
					vertex.setTitle(title);
				}

			}
			catch (Exception e) {
				e.printStackTrace();
				log.warn("Failed to resolve identifier for Vertex[identifier={}, type={}] Reason: {}", identifierValue,
						identifierType, e.getMessage());

				return;
			} finally{
				vertex.setMetaAttribute("lastResolved", Instant.now().toString());
			}
		}
		else if (identifierType.equals("ror")) {
			// todo check identifierValue to match a ROR regex first
			try {
				PublicRorClient client = new PublicRorClient();
				RorRecord rorRecord = client.resolve(identifierValue);
				String title = rorRecord.getName();
				List types = rorRecord.getTypes();
				List links = rorRecord.getLinks();
				String country = rorRecord.getCountry().getCountryName();
				if (vertex.getTitle() != null && !vertex.getTitle().isBlank()) {
					vertex.setMetaAttribute("rawTitle", vertex.getTitle());
				}
				if (country != null && !country.isBlank()) {
					vertex.setMetaAttribute("country", country);
				}
				if (types != null && !types.isEmpty()) {
					String result = (String) types.stream()
							.map(n -> String.valueOf(n))
							.collect(Collectors.joining(", "));
					vertex.setMetaAttribute("types",result);
				}
				if (links != null && !links.isEmpty()) {
					String result = (String) links.stream()
							.map(n -> String.valueOf(n))
							.collect(Collectors.joining(", "));
					vertex.setMetaAttribute("links",result);
				}
				vertex.setTitle(title);
			}
			catch (Exception e) {
				log.warn("Failed to resolve identifier for Vertex[identifier={}, type={}] Reason: {}", identifierValue,
						identifierType, e.getMessage());
				return;
			} finally{
				vertex.setMetaAttribute("lastResolved", Instant.now().toString());
			}
		}
	}
}
