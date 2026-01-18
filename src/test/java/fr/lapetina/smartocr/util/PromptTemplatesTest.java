package fr.lapetina.smartocr.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplatesTest {

    @Test
    void buildOcrPrompt_containsOcrEngineIdentity() {
        String prompt = PromptTemplates.buildOcrPrompt();

        assertTrue(prompt.contains("OCR engine"), "Prompt should identify as OCR engine");
    }

    @Test
    void buildOcrPrompt_containsPreservationInstructions() {
        String prompt = PromptTemplates.buildOcrPrompt();

        assertTrue(prompt.contains("numbers"), "Prompt should mention preserving numbers");
        assertTrue(prompt.contains("dates"), "Prompt should mention preserving dates");
        assertTrue(prompt.contains("currency"), "Prompt should mention preserving currency");
    }

    @Test
    void buildOcrPrompt_containsProhibitions() {
        String prompt = PromptTemplates.buildOcrPrompt();

        assertTrue(prompt.contains("NOT summarize"), "Prompt should prohibit summarization");
        assertTrue(prompt.contains("NOT interpret"), "Prompt should prohibit interpretation");
        assertTrue(prompt.contains("NOT extract fields"), "Prompt should prohibit field extraction");
    }

    @Test
    void buildOcrPrompt_requestsPlainTextOnly() {
        String prompt = PromptTemplates.buildOcrPrompt();

        assertTrue(prompt.contains("plain text only"), "Prompt should request plain text only");
    }

    @Test
    void buildExtractionPrompt_containsDataExtractionIdentity() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("data extraction engine"), "Prompt should identify as data extraction engine");
    }

    @Test
    void buildExtractionPrompt_containsJsonOnlyRule() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("ONLY valid JSON"), "Prompt should require JSON only output");
    }

    @Test
    void buildExtractionPrompt_containsNullForMissingRule() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("null"), "Prompt should mention using null for missing values");
    }

    @Test
    void buildExtractionPrompt_containsNoHallucinationRule() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("NOT guess") || prompt.contains("NOT hallucinate"),
                "Prompt should prohibit hallucination");
    }

    @Test
    void buildExtractionPrompt_containsIso8601Rule() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("ISO-8601"), "Prompt should require ISO-8601 dates");
    }

    @Test
    void buildExtractionPrompt_containsNoMarkdownRule() {
        String prompt = PromptTemplates.buildExtractionPrompt("{}", "text");

        assertTrue(prompt.contains("NOT include markdown"), "Prompt should prohibit markdown");
    }

    @Test
    void buildExtractionPrompt_includesSchema() {
        String schema = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
        String prompt = PromptTemplates.buildExtractionPrompt(schema, "text");

        assertTrue(prompt.contains(schema), "Prompt should include the schema");
    }

    @Test
    void buildExtractionPrompt_includesText() {
        String text = "Sample document text to extract from";
        String prompt = PromptTemplates.buildExtractionPrompt("{}", text);

        assertTrue(prompt.contains(text), "Prompt should include the text");
    }

    @Test
    void buildExtractionPrompt_nullSchema_throwsException() {
        assertThrows(NullPointerException.class, () ->
                PromptTemplates.buildExtractionPrompt(null, "text"));
    }

    @Test
    void buildExtractionPrompt_nullText_throwsException() {
        assertThrows(NullPointerException.class, () ->
                PromptTemplates.buildExtractionPrompt("{}", null));
    }
}
