package ardc.cerium.mycelium.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class IdentifierUtil {

    public static String getUrl(String identifierValue, String identifierType){
        switch (identifierType) {

            case "doi":
                return String.format("https://doi.org/%s", identifierValue);
            case "orcid":
                return String.format("http://orcid.org/%s", identifierValue);
            case "handle":
                return String.format("http://hdl.handle.net/%s", identifierValue);
            case "purl":
                if (identifierValue.contains("http")) {
                    return identifierValue;
                }
            case "AU-ANL:PEAU":
                return String.format("http://nla.gov.au/%s", identifierValue);
            case "igsn":
                return String.format("http://igsn.org/%s", identifierValue);
            case "uri":
                return String.format("http://%s", identifierValue);
            case "ark":
                if (identifierValue.contains("ark:")) {
                    return String.format("http://%s", identifierValue);
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
                    return String.format("http://,%s", identifierValue);
                }
                return String.format("http://isni.org/,%s", identifierValue);
            case "raid":
                identifierValue = identifierValue.toLowerCase(Locale.ROOT);
                if(identifierValue.contains("hdl:")){
                    identifierValue = identifierValue.substring(identifierValue.indexOf("hdl:") + 4);
                }
                else if(identifierValue.contains("http")){
                    try {
                        URL url = new URL(identifierValue);
                        identifierValue = url.getPath().substring(1);
                    }catch(MalformedURLException ignored){

                    }
                }
                else if(identifierValue.contains("handle.")){
                    try {
                        URL url = new URL("https://" + identifierValue);
                        identifierValue = url.getPath().substring(1);
                    }catch(MalformedURLException ignored){

                    }
                }
                return String.format("http://hdl.handle.net/,%s", identifierValue);
            default:
                return null;
        }
    }
}
