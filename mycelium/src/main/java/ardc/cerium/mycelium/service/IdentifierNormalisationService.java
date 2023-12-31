package ardc.cerium.mycelium.service;


import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.rifcs.model.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * This service normalises Identifiers and their types based on RDA's specs
 *
 */

@Service
public class IdentifierNormalisationService {

    /**
     * returns a standard representation for any given Identifier
     * this is needed to match Identifiers that are identical but using a different form
     * eg: all these identifiers are identical for type="doi"
     * DOI:10.234/455
     * http://doi.org/10.234/455
     * https://doi.org/10.234/455
     * 10.234/455
     * @param identifier (@link Identifier)
     * @return a normalised Identifier (@link Identifier)
     */
    @NotNull
    @Contract("null -> fail")
    public static Identifier getNormalisedIdentifier(Identifier identifier) throws ContentNotSupportedException {

        if(identifier == null || identifier.getValue() == null || identifier.getValue().isEmpty()){
            throw new ContentNotSupportedException("Identifier must have a value");
        }
        String type = getNormalisedIdentifierType(identifier);
        identifier.setType(type);
        String value = identifier.getValue().trim();
        switch (type) {
            case "doi":
                // if it's a valid DOI eg. there is a string that starts with 10.
                if (value.contains("10.")) {
                    // upper case DOI values they are case-insensitive
                    value = value.toUpperCase(Locale.ROOT);
                    value = value.substring(value.indexOf("10."));
                }
                break;
            case "orcid":
                // ORCID is 19 character long with 4 sets of 4-digit numbers
                if (StringUtils.countMatches(value, "-") == 3) {
                    value = value.substring(value.indexOf("-") - 4, value.indexOf("-") + 15);
                }
                break;

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
                break;
            case "purl":
                if (value.contains("purl.org")) {
                    value = "https://" + value.substring(value.indexOf("purl.org"));
                }
                break;
            case "ror":
                if (value.contains("ror.org")) {
                    value = value.substring(value.indexOf("ror.org/"));
                }else{
                    value = String.format("ror.org/%s", value);
                }
                break;
            case "au-anl:peau":
                if (value.contains("nla.party-")) {
                    value = value.substring(value.indexOf("nla.party-"));
                }else if(!value.startsWith("https://") && !value.startsWith("http://")){
                    value = "nla.party-" + value;
                }
                break;
            case "igsn":
                // upper case IGSN values they are case-insensitive
                value = value.toUpperCase(Locale.ROOT);
                if (value.contains("10273/")) {
                    value = value.substring(value.indexOf("10273/") + 6);
                }
                else if (value.contains("IGSN.ORG/")) {
                    value = value.substring(value.indexOf("IGSN.ORG/") + 9);
                }
                break;
            default:
                value = value.replaceFirst("(^https?://)", "");
        }
        identifier.setValue(value);
        return identifier;
    }


    /**
     * trying to best guess the more specific IdentifierType based on the Identifier value
     * or a regular missmatch from
     * eg: uri with value http://doi.org/10.5412 should be changed to doi
     * every identifier type is lower-cased
     * @param identifier (@link Identifier)
     * @return string the assumed type of the given Identifier
     */
    public static String getNormalisedIdentifierType(Identifier identifier) throws ContentNotSupportedException{
        if(identifier.getType() == null || identifier.getType().isEmpty()){
            throw new ContentNotSupportedException("Identifier must have a value");
        }
        String type = identifier.getType().trim().toLowerCase(Locale.ROOT);
        String value = identifier.getValue().toUpperCase(Locale.ROOT);

        if (value.contains("NLA.PARTY-") || type.equals("nla.party")){
            return "au-anl:peau";
        }

        if (value.contains("HDL.HANDLE.NET/10273/") || value.contains("10273/") || value.contains("IGSN.ORG")) {
            return "igsn";
        }

        if (value.contains("10.") && value.contains("DOI")) {
            return "doi";
        }

        if (value.contains("ORCID.ORG") && StringUtils.countMatches(value, "-") == 3) {
            return "orcid";
        }
        if (value.contains("ROR.ORG")) {
            return "ror";
        }
        if (value.contains("HANDLE.") || value.contains("HDL:")) {
            if (StringUtils.countMatches(value, "HTTP:") > 1 || type.contains("raid")) {
                // unable to confirm it's a handle
                return type;
            }
            return "handle";
        }

        if (value.contains("PURL.ORG")) {
            return "purl";
        }


        return type;
    }

}
