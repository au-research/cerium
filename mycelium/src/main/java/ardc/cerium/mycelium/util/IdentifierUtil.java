package ardc.cerium.mycelium.util;

public class IdentifierUtil {

	/**
	 * For normalised Identifier values
     *
	 * @param identifierValue the value of the identifier
	 * @param identifierType the type of the identifier
	 * @return a url representation for the identifier
	 */
    public static String getUrl(String identifierValue, String identifierType){
        switch (identifierType) {

            case "doi":
                return String.format("https://doi.org/%s", identifierValue);
            case "orcid":
                return String.format("https://orcid.org/%s", identifierValue);
            case "raid":
            case "handle":
                return String.format("https://hdl.handle.net/%s", identifierValue);
            case "purl":
                if (identifierValue.contains("http")) {
                    return identifierValue;
                }
            case "AU-ANL:PEAU":
                return String.format("https://nla.gov.au/%s", identifierValue);
            case "igsn":
                return String.format("https://igsn.org/%s", identifierValue);
            case "uri":
            case "url":
                return String.format("https://%s", identifierValue);
            case "ark":
                if (identifierValue.contains("ark:")) {
                    return String.format("https://%s", identifierValue);
                }
            case "scopusID":
                if (identifierValue.contains("www.scopus.com/")){
                    return String.format("https://,%s", identifierValue);
                }
                return String.format("https://www.scopus.com/authid/detail.uri?authorId=%s", identifierValue);
            case "grid":
                return String.format("https://%s", identifierValue);
            case "isni":
                if (identifierValue.contains("isni.org/")){
                    return String.format("https://,%s", identifierValue);
                }
                return String.format("https://isni.org/,%s", identifierValue);
            default:
                return null;
        }
    }
}
