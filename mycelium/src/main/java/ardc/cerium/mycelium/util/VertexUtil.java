package ardc.cerium.mycelium.util;

import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.doi.ContentNegotiationClient;
import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.orcid.PublicOrcidClient;
import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import ardc.cerium.ror.PublicRorClient;
import ardc.cerium.ror.schema.ror.json.RorRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Locale;

@Slf4j
public class VertexUtil {

	/**
	 * Normalise the Vertex Identifier value and type
	 *
	 * @param vertex the {@link Vertex} to normalise
	 */
	public static void normalise(Vertex vertex) {

		String value = vertex.getIdentifier();
		String type = vertex.getIdentifierType();
		vertex.setMetaAttribute("rawIdentifierValue", value);
		vertex.setMetaAttribute("rawIdentifierType", type);

		// normalise the type first and use that to normalise the value
		String normalisedType = getNormalisedIdentifierType(value, type);
		String normalisedValue = getNormalisedIdentifierValue(value, normalisedType);

		// sets the normalised value and type to the vertex
		vertex.setIdentifier(normalisedValue);
		vertex.setIdentifierType(normalisedType);
	}

	/**
	 * Obtain the normalised value of an identifier given the original value and the (normalised) type
	 *
	 * @param value the value of the identifier
	 * @param type the type of the identifier
	 * @return the normalised identifier value
	 */
	private static String getNormalisedIdentifierValue(String value, String type) {
		switch (type) {
		case "doi":
			// if it's a valid DOI eg there is a string that starts with 10.
			if (value.contains("10.")) {
				// upper case DOI values they are case insensitive
				value = value.toUpperCase(Locale.ROOT);
				value = value.substring(value.indexOf("10."));
			}
			return value;
		case "orcid":
			// ORCID is 19 character long with 4 sets of 4 digit numbers
			if (StringUtils.countMatches(value, "-") == 3) {
				value = value.substring(value.indexOf("-") - 4, value.indexOf("-") + 15);
			}
			return value;
		case "raid":
		case "handle":
			value = value.toLowerCase(Locale.ROOT);
			if (value.contains("hdl:")) {
				value = value.substring(value.indexOf("hdl:") + 4);
			}
			else if (value.contains("http")) {
				try {
					URL url = new URL(value);
					value = url.getPath().substring(1);
				}
				catch (MalformedURLException ignored) {

				}
			}
			else if (value.contains("handle.")) {
				try {
					URL url = new URL("https://" + value);
					value = url.getPath().substring(1);
				}
				catch (MalformedURLException ignored) {

				}
			}
			return value;
		case "purl":
			if (value.contains("purl.org")) {
				value = "https://" + value.substring(value.indexOf("purl.org"));
			}
			return value;
		case "ror":
			if (value.contains("ror.org")) {
				value = value.substring(value.indexOf("ror.org/") + 8);
			}
			return value;
		case "AU-ANL:PEAU":
			if (value.contains("nla.party-")) {
				value = value.substring(value.indexOf("nla.party-"));
			}else if(!value.startsWith("https://") && !value.startsWith("http://")){
				value = "nla.party-" + value;
			}
			return value;
		case "igsn":
			// upper case IGSN values they are case insensitive
			value = value.toUpperCase(Locale.ROOT);
			if (value.contains("10273/")) {
				value = value.substring(value.indexOf("10273/") + 6);
			}
			else if (value.contains("IGSN.ORG/")) {
				value = value.substring(value.indexOf("IGSN.ORG/") + 9);
			}
			return value;
		default:
			return value.replaceFirst("(^https?://)", "");
		}
	}

	/**
	 * Obtain the normalised type given the original value and the type
	 *
	 * Business rule applied to recognised supported identifiers
	 * @param identifierValue the value of the identifier
	 * @param identifierType the type of the identifier
	 * @return the normalised type of the identifier
	 */
	public static String getNormalisedIdentifierType(String identifierValue, String identifierType) {

		// uppercase the value for easy sub-string comparison


		if (identifierType == null || identifierType.isEmpty()) {
			throw new ContentNotSupportedException("Identifier Type must have a value");
		}

		if (identifierValue == null || identifierValue.isEmpty()) {
			throw new ContentNotSupportedException("Identifier must have a value");
		}

		String value = identifierValue.toUpperCase(Locale.ROOT);

		if (identifierType.toLowerCase(Locale.ROOT).equals("nla.party")) {
			return "AU-ANL:PEAU";
		}

		if (value.contains("HDL.HANDLE.NET/10273/")) {
			return "igsn";
		}

		if (value.contains("10.") && value.contains("DOI")) {
			return "doi";
		}

		if (value.contains("ORCID.ORG") && StringUtils.countMatches(value, "-") == 3) {
			return "orcid";
		}

		if (value.contains("HANDLE.") || value.contains("HDL:")) {
			if (StringUtils.countMatches(value, "HTTP:") > 1 || identifierType.contains("raid")) {
				// unable to confirm it's a handle
				return identifierType;
			}
			return "handle";
		}

		if (value.contains("PURL.ORG")) {
			return "purl";
		}

		if (value.contains("NLA.PARTY-")) {
			return "AU-ANL:PEAU";
		}

		return identifierType;
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
				String title = orcidRecord.getPerson().getName().getFullName();
				if (vertex.getTitle() != null && !vertex.getTitle().isBlank()) {
					vertex.setMetaAttribute("rawTitle", vertex.getTitle());
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
		else if (identifierType.equals("ror")) {
			// todo check identifierValue to match a ROR regex first
			try {
				PublicRorClient client = new PublicRorClient();
				RorRecord rorRecord = client.resolve(identifierValue);
				String title = rorRecord.getName();
				if (vertex.getTitle() != null && !vertex.getTitle().isBlank()) {
					vertex.setMetaAttribute("rawTitle", vertex.getTitle());
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
