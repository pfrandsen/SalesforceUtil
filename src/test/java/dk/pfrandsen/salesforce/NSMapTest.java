package dk.pfrandsen.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.junit.Assert.*;

public class NSMapTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String loadData(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream resourceAsStream = loader.getResourceAsStream("nsmap/" + name + ".json")) {
            System.out.println("here" + resourceAsStream);
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            System.out.println("msg" + ignored.getMessage());
        }
        return null;
    }

    private String generateObject(String key, String value) {
        return "{" + quote(key) + ":" + quote(value) + "}";
    }
    private String quote(String value) {
        return "\"" + value + "\"";
    }
    @Test
    public void testString() throws IOException {
        String json = "\"a string\"";
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(rootNode, null);
        assertNotNull(transformed);
        assertTrue(transformed.isTextual());
        assertEquals("a string", transformed.asText());
    }

    @Test
    public void testSimpleObject() throws IOException {
        String json = generateObject("k", "v");
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(rootNode, null);
        assertNotNull(transformed);
        assertTrue(transformed.isObject());
        assertNotNull(transformed.path("k"));
        assertEquals("v", transformed.path("k").asText());
    }

    @Test
    public void testSimpleObjectRemovePrefix() throws IOException {
        String json = generateObject("p_k", "p_v");
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(rootNode, "p_");
        assertNotNull(transformed);
        assertTrue(transformed.isObject());
        assertNotNull(transformed.path("k"));
        assertEquals("p_v", transformed.path("k").asText());
    }

    @Test
    public void testSimpleObjectReplacePrefix() throws IOException {
        String json = generateObject("p_k", "p_v");
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(rootNode, "p_", "r_");
        assertNotNull(transformed);
        assertTrue(transformed.isObject());
        assertNotNull(transformed.path("r_k"));
        assertTrue(transformed.path("p_k").isMissingNode());
        assertTrue(transformed.path("k").isMissingNode());
        assertEquals("p_v", transformed.path("r_k").asText());
    }

    @Test
    public void testObjectRemovePrefix() throws IOException {
        String json = loadData("array1");
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(rootNode, "Pre_");
        assertNotNull(transformed);
        assertTrue(transformed.isObject());
        JsonNode a = transformed.path("records");
        assertNotNull(a);
        assertTrue(a.isArray());
        // ArrayNode arr = (ArrayNode)a;
        assertEquals(1, a.size());
        JsonNode obj = a.get(0);
        assertNotNull(obj);
        assertTrue(obj.path("Pre_Subject__c").isMissingNode());
        assertFalse(obj.path("Subject__c").isMissingNode());
        assertTrue(obj.path("Pre_Parent_Message__r").isMissingNode());
        assertFalse(obj.path("Parent_Message__r").isMissingNode());
        JsonNode pMsg = obj.path("Parent_Message__r");
        assertTrue(pMsg.path("Pre_Id__c").isMissingNode());
        assertNotNull(pMsg.path("Id__c"));
    }

    @Test
    public void testIdentityTransform() throws IOException {
        String json = loadData("array1");
        assertNotNull(json);
        JsonNode rootNode = MAPPER.readTree(json);
        assertNotNull(rootNode);
        JsonNode transformed = NSMap.transform(NSMap.transform(rootNode, "Pre_", "Other_"), "Other_", "Pre_");
    }

}
