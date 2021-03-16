package ardc.cerium.core.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to work with <a href="http://orcid.org">ORCID</a> identifiers.
 *
 * @author Kyle Michel
 */
public class Orcid {

    /**
     * Get the ID string.
     *
     * @return String with the ID for this object.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Generate a {@link Orcid} object from an input string. This will use regular expression to try to determine
     * the format of the input string and extract the id.
     *
     * @param input String to convert into and ORCID id.
     * @return New {@link Orcid} object with the value extracted from the input string.
     * @throws IllegalArgumentException if the input string is not a valid ORCID string.
     */
    public static Orcid valueOf(final String input) {
        final Matcher matcher = ORCID_PATTERN.matcher(input);
        if (matcher.matches()) {
            return new Orcid(matcher.group(0) + "-" + matcher.group(1) + "-"
                    + matcher.group(2) + "-" + matcher.group(3));
        }
        else {
            throw new IllegalArgumentException("Input string is not a valid ORCID: " + input);
        }
    }

    /**
     * Determine whether an input string is a valid ORCID identifier string.
     *
     * @param input String to check as an ORCID identifier.
     * @return True if the input string is a valid ORCID identifier.
     */
    public static boolean isValid(final String input) {
        try {
            valueOf(input);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Constructor.
     *
     * @param id String with the formatted ORCID id.
     */
    private Orcid(final String id) {
        this.id = id;
    }

    /** String with the value of the ORCID id. */
    private final String id;

    /** Regular expression to check whether an input string is a valid ORCID id. */
    private static final String ORCID_REGEX =
            "^\\s*(?:(?:https?://)?orcid.org/)?([0-9]{4})\\-?([0-9]{4})\\-?([0-9]{4})\\-?([0-9]{4})\\s*$";

    /** Regular expression to check whether an input string is a valid ORCID id. */
    private static final Pattern ORCID_PATTERN = Pattern.compile(ORCID_REGEX);
}