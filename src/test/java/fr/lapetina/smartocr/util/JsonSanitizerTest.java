package fr.lapetina.smartocr.util;

import fr.lapetina.smartocr.util.JsonSanitizer.JsonSanitizationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonSanitizerTest {

    @Test
    void sanitize_validJson_returnsJsonNode() throws JsonSanitizationException {
        String json = "{\"name\": \"test\", \"value\": 123}";

        JsonNode result = JsonSanitizer.sanitize(json);

        assertEquals("test", result.get("name").asText());
        assertEquals(123, result.get("value").asInt());
    }

    @Test
    void sanitize_jsonInMarkdownBlock_extractsJson() throws JsonSanitizationException {
        String response = "```json\n{\"name\": \"test\"}\n```";

        JsonNode result = JsonSanitizer.sanitize(response);

        assertEquals("test", result.get("name").asText());
    }

    @Test
    void sanitize_jsonWithLeadingText_extractsJson() throws JsonSanitizationException {
        String response = "Here is the result:\n{\"name\": \"test\"}";

        JsonNode result = JsonSanitizer.sanitize(response);

        assertEquals("test", result.get("name").asText());
    }

    @Test
    void sanitize_jsonWithTrailingText_extractsJson() throws JsonSanitizationException {
        String response = "{\"name\": \"test\"}\nI hope this helps!";

        JsonNode result = JsonSanitizer.sanitize(response);

        assertEquals("test", result.get("name").asText());
    }

    @Test
    void sanitize_nestedJson_extractsCorrectly() throws JsonSanitizationException {
        String json = "{\"outer\": {\"inner\": \"value\"}}";

        JsonNode result = JsonSanitizer.sanitize(json);

        assertEquals("value", result.get("outer").get("inner").asText());
    }

    @Test
    void sanitize_jsonWithBracesInStrings_handlesCorrectly() throws JsonSanitizationException {
        String json = "{\"text\": \"contains {braces} inside\"}";

        JsonNode result = JsonSanitizer.sanitize(json);

        assertEquals("contains {braces} inside", result.get("text").asText());
    }

    @Test
    void sanitize_nullInput_throwsException() {
        assertThrows(JsonSanitizationException.class, () -> JsonSanitizer.sanitize(null));
    }

    @Test
    void sanitize_blankInput_throwsException() {
        assertThrows(JsonSanitizationException.class, () -> JsonSanitizer.sanitize("   "));
    }

    @Test
    void sanitize_noJsonFound_throwsException() {
        assertThrows(JsonSanitizationException.class, () -> JsonSanitizer.sanitize("no json here"));
    }

    @Test
    void sanitize_invalidJson_throwsException() {
        assertThrows(JsonSanitizationException.class, () -> JsonSanitizer.sanitize("{invalid}"));
    }

    @Test
    void isValidJson_validJson_returnsTrue() {
        assertTrue(JsonSanitizer.isValidJson("{\"key\": \"value\"}"));
    }

    @Test
    void isValidJson_invalidJson_returnsFalse() {
        assertFalse(JsonSanitizer.isValidJson("{invalid}"));
    }

    @Test
    void isValidJson_null_returnsFalse() {
        assertFalse(JsonSanitizer.isValidJson(null));
    }

    @Test
    void isValidJson_blank_returnsFalse() {
        assertFalse(JsonSanitizer.isValidJson("   "));
    }

    @Test
    void sanitize_jsonArray_extractsCorrectly() throws JsonSanitizationException {
        String json = "[{\"id\": 1}, {\"id\": 2}]";

        JsonNode result = JsonSanitizer.sanitize(json);

        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    void sanitize_nullValuesInJson_preservesNull() throws JsonSanitizationException {
        String json = "{\"name\": null, \"value\": 123}";

        JsonNode result = JsonSanitizer.sanitize(json);

        assertTrue(result.get("name").isNull());
        assertEquals(123, result.get("value").asInt());
    }
}
