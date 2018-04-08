package dk.pfrandsen.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

/**
 * Utility to remove/replace prefix in object keys in json tree.
 */
public final class NSMap {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private NSMap() {
    }

    /**
     * Copy JSON tree while removing prefix from all object keys that starts with given prefix. If removing prefix from
     * key results in a name clash with another key in the same object node, then the last key seen will win
     * (other key/value is lost). In case of key clash expect unpredictable results in terms of which value is lost.
     *
     * @param src source json tree
     * @param prefix prefix to remove from object keys
     * @return copy of source tree with prefix removed from object keys
     */
    public static JsonNode transform(JsonNode src, String prefix) {
        return transform(src, prefix, null);
    }

    /**
     * Copy JSON tree while replacing prefix from all object keys that starts with given prefix. If replacing prefix in
     * key results in a name clash with another key in the same object node, then the last key seen will win
     * (other key/value is lost). In case of key clash expect unpredictable results in terms of which value is lost.
     *
     * @param src source json tree
     * @param prefix prefix to replace in object keys
     * @param replace string to replace prefix with
     * @return copy of source tree with prefix replaced in object keys
     */
    public static JsonNode transform(JsonNode src, String prefix, String replace) {
        if (src == null) {
            return null;
        }
        if (!src.isContainerNode()) {
            return src.deepCopy();
        }
        if (src.isArray()) {
            ArrayNode arr = MAPPER.createArrayNode();
            Iterator<JsonNode> iter = src.iterator();
            while (iter.hasNext()) {
                JsonNode t = transform(iter.next(), prefix, replace);
                arr.add(t);
            }
            return arr;
        }
        if (src.isObject()) {
            ObjectNode obj = MAPPER.createObjectNode();
            Iterator<String> iter = src.fieldNames();
            while (iter.hasNext()) {
                String nodeName = iter.next();
                JsonNode node = src.path(nodeName);
                obj.set(map(prefix, replace, nodeName), transform(node, prefix, replace));
            }
            return obj;
        }
        // should never get here
        return null;
    }

    /**
     * Remove prefix from string.
     *
     * @param prefix prefix to remove from string
     * @param value string to remove prefix from
     * @return value if value is null or does not start with prefix, else value with prefix removed
     */
    public static String rm(String prefix, String value) {
        if (prefix == null || prefix.length() == 0 || value == null) {
            return value;
        }
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /**
     * Replace prefix in string.
     *
     * @param prefix prefix to be replaced (if null or empty value is not changed)
     * @param replace string to replace prefix with (if null or empty prefix will be removed and not replaced)
     * @param value string to replace prefix in
     * @return value if value is null or does not start with prefix, else value with prefix replaced
     */
    public static String map(String prefix, String replace, String value) {
        if (prefix == null || prefix.length() == 0 || value == null) {
            return value;
        }
        if (replace == null || replace.length() == 0) {
            return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        }
        return value.startsWith(prefix) ? (replace + value.substring(prefix.length())) : value;
    }

}
