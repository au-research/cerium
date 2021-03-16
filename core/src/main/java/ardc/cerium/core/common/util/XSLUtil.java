package ardc.cerium.core.common.util;

import org.json.JSONArray;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

/**
 * XSLUtils a collection of functions that are much simpler in Java than XSLT
 */
public class XSLUtil {

	/**
	 * @param mixedCase String containing mixed case characters
	 * @return String upper cased
	 */
	public static String toUpperCase(String mixedCase) {
		return mixedCase.toUpperCase();
	}

	/**
	 * @param mixedCase String containing mixed case characters
	 * @return String lower cased
	 */
	public static String toLowerCase(String mixedCase) {
		return mixedCase.toLowerCase();
	}

	/**
	 * https://stackoverflow.com/questions/18898773/java-escape-json-string
	 * @param raw input string that may contains unescaped string that is invalid JSON str
	 * @return an escaped string
	 */
	public static String escapeJsonString(String raw) {
		String escaped = raw;
		escaped = escaped.replace("\\", "\\\\");
		escaped = escaped.replace("\"", "\\\"");
		escaped = escaped.replace("\b", "\\b");
		escaped = escaped.replace("\f", "\\f");
		escaped = escaped.replace("\n", "\\n");
		escaped = escaped.replace("\r", "\\r");
		escaped = escaped.replace("\t", "\\t");
		// TODO: escape other non-printing characters using uXXXX notation
		return escaped;
	}

	public static String createJsonArray(NodeList nodeList){
		JSONArray result = new JSONArray();
		int length = nodeList.getLength();
		for(int i = 0 ; i < length; i++)
		{
			result.put(XSLUtil.escapeJsonString(nodeList.item(i).getNodeValue()));
		}
		return result.toString();
	}

	public static String createJsonArray(NodeIterator iterator){
		JSONArray result = new JSONArray();
		Node n;
		while( (n = iterator.nextNode()) != null)
		{
			result.put(XSLUtil.escapeJsonString(n.getNodeValue()));
		}
		return result.toString();
	}

}
