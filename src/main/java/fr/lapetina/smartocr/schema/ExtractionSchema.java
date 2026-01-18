package fr.lapetina.smartocr.schema;

import java.util.Objects;

/**
 * Opaque wrapper for a JSON schema used as extraction instructions for the LLM.
 * The schema is treated as raw instructions and is not validated or parsed.
 */
public final class ExtractionSchema {

    private final String rawJson;

    private ExtractionSchema(String rawJson) {
        this.rawJson = rawJson;
    }

    /**
     * Creates an ExtractionSchema from a raw JSON schema string.
     *
     * @param json the raw JSON schema string
     * @return a new ExtractionSchema instance
     * @throws NullPointerException if json is null
     * @throws IllegalArgumentException if json is blank
     */
    public static ExtractionSchema fromString(String json) {
        Objects.requireNonNull(json, "json must not be null");
        if (json.isBlank()) {
            throw new IllegalArgumentException("json must not be blank");
        }
        return new ExtractionSchema(json);
    }

    /**
     * Returns the raw JSON schema string.
     *
     * @return the raw JSON schema
     */
    public String rawJson() {
        return rawJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractionSchema that)) return false;
        return rawJson.equals(that.rawJson);
    }

    @Override
    public int hashCode() {
        return rawJson.hashCode();
    }

    @Override
    public String toString() {
        return "ExtractionSchema[rawJson=" + rawJson + "]";
    }
}
