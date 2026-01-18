package fr.lapetina.smartocr.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for sanitizing and validating JSON from LLM responses.
 * Handles common LLM misbehaviors like wrapping JSON in markdown or adding explanatory text.
 */
public final class JsonSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern MARKDOWN_JSON_BLOCK = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JSON_OBJECT = Pattern.compile(
            "\\{[\\s\\S]*}",
            Pattern.DOTALL
    );

    private static final Pattern JSON_ARRAY = Pattern.compile(
            "\\[[\\s\\S]*]",
            Pattern.DOTALL
    );

    private JsonSanitizer() {
        // Utility class
    }

    /**
     * Sanitizes and parses a raw LLM response into a valid JsonNode.
     * <p>
     * Handles:
     * <ul>
     *   <li>Markdown code blocks (```json ... ```)</li>
     *   <li>Leading/trailing non-JSON text</li>
     *   <li>Whitespace trimming</li>
     * </ul>
     *
     * @param rawResponse the raw response from the LLM
     * @return the parsed JsonNode
     * @throws JsonSanitizationException if no valid JSON can be extracted
     */
    public static JsonNode sanitize(String rawResponse) throws JsonSanitizationException {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new JsonSanitizationException("Response is null or blank");
        }

        String extracted = extractJson(rawResponse.trim());
        return parseAndValidate(extracted, rawResponse);
    }

    /**
     * Checks if the given string contains valid JSON.
     *
     * @param json the string to check
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static String extractJson(String response) throws JsonSanitizationException {
        // Try markdown code block first
        Matcher blockMatcher = MARKDOWN_JSON_BLOCK.matcher(response);
        if (blockMatcher.find()) {
            String content = blockMatcher.group(1).trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        // Find the first JSON structure (object or array)
        Matcher objectMatcher = JSON_OBJECT.matcher(response);
        Matcher arrayMatcher = JSON_ARRAY.matcher(response);

        boolean hasObject = objectMatcher.find();
        boolean hasArray = arrayMatcher.find();

        if (hasObject && hasArray) {
            // Use whichever appears first
            if (objectMatcher.start() < arrayMatcher.start()) {
                return findBalancedJson(response, objectMatcher.start(), '{', '}');
            } else {
                return findBalancedJson(response, arrayMatcher.start(), '[', ']');
            }
        } else if (hasObject) {
            return findBalancedJson(response, objectMatcher.start(), '{', '}');
        } else if (hasArray) {
            return findBalancedJson(response, arrayMatcher.start(), '[', ']');
        }

        String preview = response.length() > 200 ? response.substring(0, 200) + "..." : response;
        throw new JsonSanitizationException("No JSON structure found in response. Preview: " + preview);
    }

    private static String findBalancedJson(String text, int start, char open, char close)
            throws JsonSanitizationException {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
        }

        throw new JsonSanitizationException("Unbalanced JSON structure");
    }

    private static JsonNode parseAndValidate(String json, String originalResponse)
            throws JsonSanitizationException {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new JsonSanitizationException(
                    "Invalid JSON: " + e.getOriginalMessage() + "\nExtracted: " + json,
                    e
            );
        }
    }

    /**
     * Exception thrown when JSON sanitization fails.
     */
    public static class JsonSanitizationException extends Exception {
        public JsonSanitizationException(String message) {
            super(message);
        }

        public JsonSanitizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
