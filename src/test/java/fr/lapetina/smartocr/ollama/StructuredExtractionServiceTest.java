package fr.lapetina.smartocr.ollama;

import fr.lapetina.smartocr.api.ExtractionException;
import fr.lapetina.smartocr.util.PromptTemplates;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructuredExtractionServiceTest {

    private static final String SCHEMA = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
    private static final String TEXT = "Sample document text";

    @Mock
    private LlmClient mockClient;

    private StructuredExtractionService service;

    @BeforeEach
    void setUp() {
        service = new StructuredExtractionService(mockClient, "llama3.2");
    }

    @Test
    void extract_validInput_returnsJsonNode() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("{\"name\": \"test\"}");

        JsonNode result = service.extract(TEXT, SCHEMA);

        assertNotNull(result);
        assertEquals("test", result.get("name").asText());
    }

    @Test
    void extract_usesCorrectPrompt() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("{\"name\": \"test\"}");

        service.extract(TEXT, SCHEMA);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockClient).sendPrompt(any(), promptCaptor.capture());

        String capturedPrompt = promptCaptor.getValue();
        String expectedPrompt = PromptTemplates.buildExtractionPrompt(SCHEMA, TEXT);
        assertEquals(expectedPrompt, capturedPrompt);
    }

    @Test
    void extract_usesConfiguredModel() {
        String customModel = "custom-text-model";
        StructuredExtractionService customService = new StructuredExtractionService(mockClient, customModel);
        when(mockClient.sendPrompt(any(), any())).thenReturn("{\"name\": \"test\"}");

        customService.extract(TEXT, SCHEMA);

        verify(mockClient).sendPrompt(eq(customModel), any());
    }

    @Test
    void extract_jsonInMarkdownBlock_extractsCorrectly() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("```json\n{\"name\": \"test\"}\n```");

        JsonNode result = service.extract(TEXT, SCHEMA);

        assertEquals("test", result.get("name").asText());
    }

    @Test
    void extract_jsonWithSurroundingText_extractsCorrectly() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("Here is the result:\n{\"name\": \"test\"}");

        JsonNode result = service.extract(TEXT, SCHEMA);

        assertEquals("test", result.get("name").asText());
    }

    @Test
    void extract_nullValues_preservesNull() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("{\"name\": null}");

        JsonNode result = service.extract(TEXT, SCHEMA);

        assertTrue(result.get("name").isNull());
    }

    @Test
    void extract_nullText_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.extract(null, SCHEMA));
    }

    @Test
    void extract_nullSchema_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.extract(TEXT, null));
    }

    @Test
    void extract_blankText_throwsExtractionException() {
        assertThrows(ExtractionException.class, () -> service.extract("   ", SCHEMA));
    }

    @Test
    void extract_blankSchema_throwsExtractionException() {
        assertThrows(ExtractionException.class, () -> service.extract(TEXT, "   "));
    }

    @Test
    void extract_invalidJsonResponse_throwsExtractionException() {
        when(mockClient.sendPrompt(any(), any())).thenReturn("not valid json");

        assertThrows(ExtractionException.class, () -> service.extract(TEXT, SCHEMA));
    }

    @Test
    void extract_clientThrowsException_throwsExtractionException() {
        when(mockClient.sendPrompt(any(), any())).thenThrow(new OllamaException("Connection failed"));

        ExtractionException exception = assertThrows(ExtractionException.class, () ->
                service.extract(TEXT, SCHEMA));

        assertTrue(exception.getMessage().contains("Extraction failed"));
    }

    @Test
    void constructor_nullClient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new StructuredExtractionService(null, "model"));
    }

    @Test
    void constructor_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new StructuredExtractionService(mockClient, null));
    }
}
