package ardc.cerium.drvs.provider;

import ardc.cerium.core.common.provider.MetadataQualityProvider;
import ardc.cerium.core.common.service.VocabService;
import ardc.cerium.core.common.util.Orcid;
import ardc.cerium.core.common.util.XMLUtil;
import ardc.cerium.drvs.model.CollectionValidationSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DOIMetadataQualityProvider implements MetadataQualityProvider {

	private static final Logger logger = LoggerFactory.getLogger(DOIMetadataQualityProvider.class);

	/**
	 * A String containing only those characters which are used in the base 32
	 * representation of ROR values.
	 */
	private static final String ROR_CHARS = "0123456789abcdefghjkmnpqrstvwxyz";

	/**
	 * Basic regular expression for RORs, just for making sure that a value to be
	 * validated has the correct length and only uses the correct characters. The first
	 * character must be 0; the next six characters must be valid base 32, and the final
	 * two characters must be decimal digits.
	 */
	private static final String ROR_REGEX = "0[" + ROR_CHARS + "]{6}" + "[0-9]{2}";

	/** Compiled version of the regular expression for RORs. */
	private static final Pattern ROR_PATTERN = Pattern.compile(ROR_REGEX);

	/**
	 * ROR validation code sourced from:
	 * https://git.ands.org.au/projects/RV/repos/vocabs-registry/browse/src/main/java/au/org/ands/vocabs/registry/api/validation/ValidationUtils.java#752
	 */
	private HashMap<String, Boolean> rules;

	private VocabService vocabService;

	private String localIdentifierValue;

	public static boolean isValidROR(String ror) {
		// An explicit null check is required first, as matcher() throws
		// an NPE on a null actual parameter.
		if (ror == null) {
			return false;
		}

		if (ror.startsWith("https://ror.org/")) {
			ror = ror.substring(16);
		}

		if (!ROR_PATTERN.matcher(ror).matches()) {
			return false;
		}
		// The value is the correct syntax. Now attempt to decode it.
		// Discard the leading 0, and break up into the value proper
		// and the checksum.
		String rorIdString = ror.substring(1, 7);
		String checksumString = ror.substring(7);
		long intValue = 0;
		for (int i = 0; i < rorIdString.length(); i++) {
			intValue = (intValue << 5) + ROR_CHARS.indexOf(rorIdString.charAt(i));
		}
		// logger.info("intValue: " + intValue);
		int checksumInt = Integer.parseInt(checksumString);
		long remainder = 98 - ((intValue * 100) % 97);
		// logger.info("Computed remainder: " + remainder);
		return remainder == checksumInt;
	}

	@Override
	public String get(String content, String localIdentifierValue) {
		this.localIdentifierValue = localIdentifierValue;
		String summary = null;
		CollectionValidationSummary collectionValidationSummary = new CollectionValidationSummary();
		rules = collectionValidationSummary.getRules();
		Element domElement = XMLUtil.getDomDocument(content);
		if (domElement == null) {
			collectionValidationSummary.setStatus(CollectionValidationSummary.Status.DOINOTFOUND);
		}
		else {
			for (Map.Entry<String, Boolean> rule : rules.entrySet()) {
				Boolean result = testRule(rule.getKey(), domElement);
				collectionValidationSummary.setResult(rule.getKey(), result);
			}
		}
		// test for all
		// generate a quality report
		try {
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			summary = ow.writeValueAsString(collectionValidationSummary);
			logger.debug(String.format("Quality report %s", summary));
			logger.info(String.format("generated Quality report for local ID: %s", localIdentifierValue));
		}
		catch (Exception e) {
			logger.error("Unable to generate Validation report");
		}

		return summary;
	}

	public void setLocalIdentifierValue(String localIdentifierValue){
		this.localIdentifierValue = localIdentifierValue;
	}

	public Boolean testRule(String ruleKey, Element domElement) {
		Boolean result = false;
		switch (ruleKey) {
		case "E01":
			// At least 1 size element exists and the text value contains "TB".
			try {
				// At least 1 alternateIdentifier value in the DOI metadata matches the
				// local ID from the submission metadata for the collection.
				String elementName = "size";
				NodeList alternateIdentifiers = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < alternateIdentifiers.getLength(); i++) {
					Node alternateIdentifier = alternateIdentifiers.item(i);
					if (alternateIdentifier.getFirstChild().getNodeValue().contains("TB")) {
						return true;
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "E02":
			try {
				// At least 1 alternateIdentifier value in the DOI metadata matches the
				// local ID from the submission metadata for the collection.
				String elementName = "alternateIdentifier";
				NodeList alternateIdentifiers = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < alternateIdentifiers.getLength(); i++) {
					Node alternateIdentifier = alternateIdentifiers.item(i);
					if (alternateIdentifier.getFirstChild().getNodeValue().equals(this.localIdentifierValue)) {
						return true;
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "E03":
			try {
				// At least 1 alternateIdentifier value in the DOI metadata matches the
				// local ID from the submission metadata for the collection.
				String elementName = "subject";
				NodeList alternateIdentifiers = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < alternateIdentifiers.getLength(); i++) {
					Node alternateIdentifier = alternateIdentifiers.item(i);
					if (isValidFORCode(alternateIdentifier.getFirstChild().getNodeValue().trim())) {
						return true;
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "E04":
			// None. If the XML validates against the DataCIte schema at least 1 title
			// must exist.
			result = true;
			break;
		case "E05":
			// At least 1 description element exists and is not empty
			try {
				String elementName = "description";
				NodeList descriptions = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < descriptions.getLength(); i++) {
					Node description = descriptions.item(i);
					if (!description.getFirstChild().getNodeValue().trim().isEmpty()) {
						return true;
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "E06":
			// Publisher value exists and contains ROR identifier:
			String regEx = "[,\\s\\-=\\?]";
			try {
				String elementName = "publisher";
				NodeList publishers = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < publishers.getLength(); i++) {
					Node publisher = publishers.item(i);
					if(publisher.getFirstChild() != null && !publisher.getFirstChild().getNodeValue().trim().isEmpty()) {
						String[] publisherContent = publisher.getTextContent().split(regEx);
						for(String s : publisherContent){
							if (isValidROR(s)) {
								return true;
							}
						}
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "V01":
			// None. Harvesting from DataCite so DOI should be valid.
			result = true;
			break;
		case "V02":
			// None. Should be the same URL associated with the DOI
			result = true;
			break;
		case "V03":
			// At least one creator element exists with a nameIdentifier element which has
			// a text value
			// AND
			// a nameIdentifierScheme attribute with a value of either "ORCID" or "ROR"
			/**
			 *
			 * <creator> <creatorName nameType="Personal">Miller, Elizabeth</creatorName>
			 * <givenName>Elizabeth</givenName> <familyName>Miller</familyName>
			 * <nameIdentifier schemeURI="http://orcid.org/" nameIdentifierScheme=
			 * "ORCID">0000-0001-5000-0007</nameIdentifier>
			 * <affiliation>DataCite</affiliation> </creator>
			 *
			 */
			try {
				String elementName = "creator";
				NodeList creators = domElement.getElementsByTagName(elementName);
				boolean gotValidNamedIdentifier = false;
				for (int i = 0; i < creators.getLength(); i++) {
					Node creator = creators.item(i);
					NodeList childNodes = creator.getChildNodes();
					for (int j = 0; j < childNodes.getLength(); j++) {
						Node childNode = childNodes.item(j);
						if (childNode.getNodeName().equals("nameIdentifier")) {
							// OR at least one rights attribute has a value
							NamedNodeMap nameIdentifierAttributes = childNode.getAttributes();
							for (int k = 0; k < nameIdentifierAttributes.getLength(); k++) {
								Node attribute = nameIdentifierAttributes.item(k);
								if (!gotValidNamedIdentifier && attribute.getNodeName().equals("nameIdentifierScheme")
										&& attribute.getNodeValue().trim().toUpperCase().equals("ORCID")) {
									gotValidNamedIdentifier = isValidORCID(childNode.getFirstChild().getNodeValue());
								}
								if (!gotValidNamedIdentifier && attribute.getNodeName().equals("nameIdentifierScheme")
										&& attribute.getNodeValue().trim().toUpperCase().equals("ROR")) {
									gotValidNamedIdentifier = isValidROR(childNode.getFirstChild().getNodeValue());
								}
							}
						}
					}
				}
				return gotValidNamedIdentifier;
			}
			catch (Exception e) {
				return false;
			}
		case "V04":
			// At least 1 awardNumber exists (in FundingReference element)

			try {
				String elementName = "fundingReference";
				NodeList fundingReferences = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < fundingReferences.getLength(); i++) {
					Node fundingReference = fundingReferences.item(i);
					NodeList childNodes = fundingReference.getChildNodes();
					for (int j = 0; j < childNodes.getLength(); j++) {
						Node childNode = childNodes.item(j);
						if (childNode.getNodeName().equals("awardNumber") && childNode.getFirstChild() != null
								&& !childNode.getFirstChild().getNodeValue().trim().isEmpty()) {
							return true;
						}
					}
				}
			}
			catch (Exception e) {
				return false;
			}
			// OR
			// At least 1 relatedIdentifier element exists
			try {
				String elementName = "relatedIdentifier";
				NodeList relatedIdentifiers = domElement.getElementsByTagName(elementName);
				if(relatedIdentifiers.getLength() > 0) {
							return true;
				}
			}
			catch (Exception e) {
				return false;
			}
			return false;
		case "V05":
			// At least 1 Rights element exists that contains a text value OR at least one
			// rights attribute has a value.
			try {
				String elementName = "rights";
				NodeList rightss = domElement.getElementsByTagName(elementName);
				for (int i = 0; i < rightss.getLength(); i++) {
					Node rights = rightss.item(i);
					if (rights.getFirstChild() != null && !rights.getFirstChild().getNodeValue().trim().isEmpty()) {
						return true;
					}
					// OR at least one rights attribute has a value
					NamedNodeMap rightsAttributes = rights.getAttributes();
					for (int j = 0; j < rightsAttributes.getLength(); j++) {
						Node attribute = rightsAttributes.item(j);
						if (!attribute.getNodeValue().trim().isEmpty()) {
							return true;
						}
					}
				}
				return false;
			}
			catch (Exception e) {
				return false;
			}
		case "V06":
			result = true;
			break;
		case "V07":
			result = true;
			break;
		}

		return result;
	}

	public void setVocabService(VocabService vocabService) {
		this.vocabService = vocabService;
	}

	private boolean isValidFORCode(String codeValue) {
		return vocabService.isValidNotation(codeValue);
	}

	private boolean isValidORCID(String orcid) {
		return Orcid.isValid(orcid);
	}

}
