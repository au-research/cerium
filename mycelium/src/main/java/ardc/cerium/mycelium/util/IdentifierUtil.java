package ardc.cerium.mycelium.util;

public class IdentifierUtil {

    public static String getUrl(String identifierValue, String identifierType){
        switch (identifierType) {

            case "doi":
                return String.format("https://doi.org/%s",identifierValue);
            default:
                return null;
        }
    }
}
