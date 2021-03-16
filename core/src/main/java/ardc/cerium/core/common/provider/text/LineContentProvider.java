package ardc.cerium.core.common.provider.text;
import ardc.cerium.core.common.provider.IdentifierProvider;
import java.util.*;

public class LineContentProvider implements IdentifierProvider {

    /**
     * Finds the resourceIdentifier of an IGSN record with ARDC v1 schema
     * @param content a list of identifiers from a text file
     * @return The resourceIdentifier as String
     */
    @Override
    public String get(String content) {
        String[] lines = content.split("\\r?\\n");
        return lines[0].toUpperCase();
    }

    /**
     * Finds the resourceIdentifier of an IGSN record with ARDC v1 schema
     * @param content a list of identifiers from a text file
     * @param position int the position of the identifier to retrieve
     * @return The Upper cased resourceIdentifier as String
     */
    @Override
    public String get(String content, int position) {
        String[] lines = content.split("\\r?\\n");
        if(position < 0 || lines.length <= position) return null;
        return lines[position].toUpperCase();
    }

    /**
     * @param content a list of identifiers from a text file
     * @return a List is identifier values (Upper cased) from the given document
     */
    @Override
    public List<String> getAll(String content) {
        List<String> identifiers = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            identifiers.add(line.toUpperCase());
        }
        return identifiers;
    }

    @Override
    public void setPrefix(String prefix) {
        // prefix is provided in the content
    }

}
