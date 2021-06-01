package ardc.cerium.mycelium.rifcs;


import ardc.cerium.mycelium.rifcs.model.Identifier;
import org.apache.commons.lang3.StringUtils;
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
    public static Identifier getNormalisedIdentifier(Identifier identifier){
        String type = getNormalisedIdentifierType(identifier);
        String value = identifier.getValue().trim();
        switch (type)
        {
            case "doi":
                // if it's a valid DOI eg there is a string that starts with 10.
                // upper case DOI values they are case insensitive
                value = value.toUpperCase(Locale.ROOT);
                if(value.contains("10.")){
                    value = value.substring(value.indexOf("10."));
                }
            break;
            case "orcid":
                // ORCID is 19 character long with 4 sets of 4 digit numbers
                if(StringUtils.countMatches(value, "-") == 3 ){
                    value = value.substring(value.indexOf("-") - 4, value.indexOf("-") + 15);
                }
            break;
            case "handle":
                value = value.toLowerCase(Locale.ROOT);
                if(value.contains("hdl:")){
                    value = value.substring(value.indexOf("hdl:") + 4);
                }
                else if(value.contains("http")){
                    try {
                        URL url = new URL(value);
                        value = url.getPath().substring(1);
                    }catch(MalformedURLException ignored){

                    }
                }
                else if(value.contains("handle.")){
                    try {
                        URL url = new URL("https://" + value);
                        value = url.getPath().substring(1);
                    }catch(MalformedURLException ignored){

                    }
                }
            break;
            case "purl":
                if(value.contains("purl.org")){
                    value = "https://" + value.substring(value.indexOf("purl.org"));
                }
            break;
            case "AU-ANL:PEAU":
                if(value.contains("nla.party-")){
                    value = value.substring(value.indexOf("nla.party-"));
                }else{
                    value = "nla.party-" + value;
                }
            break;
            case "igsn":
                // upper case IGSN values they are case insensitive
                value = value.toUpperCase(Locale.ROOT);
                if(value.contains("10273/")){
                    value = value.substring(value.indexOf("10273/") + 6);
                }
                else if(value.contains("IGSN.ORG/")){
                    value = value.substring(value.indexOf("IGSN.ORG/") + 9);
                }
            break;
            default:
                value = value.replaceFirst("(^https?://)", "");
        }
        identifier.setType(type);
        identifier.setValue(value);
        return identifier;
    }


    /**
     * trying to best guess the more specific IdentifierType based on the Identifier value
     * or a regular missmatch from
     * eg: uri with value http://doi.org/10.5412 should be changed to doi
     * @param identifier (@link Identifier)
     * @return string the assumed type of the given Identifier
     */
    private static String getNormalisedIdentifierType(Identifier identifier){
        String type = identifier.getType().trim();
        String value = identifier.getValue().toUpperCase(Locale.ROOT);
        if(type.toLowerCase(Locale.ROOT).equals("nla.party")){
            return "AU-ANL:PEAU";
        }
        if(value.contains("HDL.HANDLE.NET/10273/")){
            return "igsn";
        }
        if(value.contains("10.")  && value.contains("DOI")){
            return "doi";
        }
        if(value.contains("ORCID.ORG") && StringUtils.countMatches(value, "-") == 3){
            return "orcid";
        }
        if(value.contains("HANDLE.")  || value.contains("HDL:")){
            if(StringUtils.countMatches(value, "HTTP:") > 1){
                // unable to confirm it's a handle
                return type;
            }
            return "handle";
        }
        if(value.contains( "PURL.ORG")){
            return "purl";
        }
        if(value.contains("NLA.PARTY-")){
            return "AU-ANL:PEAU";
        }

        return type;
    }

}
