package fr.lapetina.smartocr.util;

import java.util.Objects;

/**
 * Centralized prompt templates for LLM interactions.
 * All prompts are deterministic and entity-agnostic.
 */
public final class PromptTemplates {

    /**
     * OCR prompt for extracting text from images.
     * Semantically equivalent to PRD Section 7 normative prompt.
     */
    private static final String OCR_PROMPT = """
            You are an OCR engine.

            Extract ALL readable text from the provided image.
            Preserve original wording, numbers, dates, reference numbers, and currency values.
            Do NOT summarize.
            Do NOT interpret.
            Do NOT extract fields.
            Do NOT add explanations.

            Return plain text only.""";

    /**
     * Extraction prompt template for structured data extraction.
     * Semantically equivalent to PRD Section 8 normative prompt.
     */
    private static final String EXTRACTION_PROMPT_TEMPLATE = """
            You are a data extraction engine. Your output must be ONLY a JSON object, nothing else.

            You are given:
            1. A JSON schema describing fields to extract
            2. A block of unstructured text

            Rules:
            - Output MUST start with { and end with }
            - Return ONLY valid JSON - no text before or after
            - Use exactly the field names from the schema
            - If a value is not found, use null
            - Do NOT guess or hallucinate values
            - Dates must be ISO-8601 format (YYYY-MM-DD)
            - Numbers must be numeric (no currency symbols)
            - Do NOT include explanations or comments
            - Do NOT include markdown
            - NEVER respond with anything other than a JSON object

            Schema:
            %s

            Text:
            %s

            Respond with JSON only:""";

    private PromptTemplates() {
        // Utility class
    }

    /**
     * Builds the OCR prompt for text extraction from images.
     *
     * @return the OCR prompt
     */
    public static String buildOcrPrompt() {
        return OCR_PROMPT;
    }

    /**
     * Builds the extraction prompt for structured data extraction.
     *
     * @param schemaJson the JSON schema defining the extraction structure
     * @param text       the text to extract from
     * @return the formatted extraction prompt
     */
    public static String buildExtractionPrompt(String schemaJson, String text) {
        Objects.requireNonNull(schemaJson, "schemaJson must not be null");
        Objects.requireNonNull(text, "text must not be null");

        return String.format(EXTRACTION_PROMPT_TEMPLATE, schemaJson, text);
    }
}
