package fr.lapetina.smartocr.ollama;

import fr.lapetina.smartocr.api.OcrException;
import fr.lapetina.smartocr.util.PromptTemplates;
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
class VisionOcrServiceTest {

    @Mock
    private LlmClient mockClient;

    private VisionOcrService service;

    @BeforeEach
    void setUp() {
        service = new VisionOcrService(mockClient, "llama3.2-vision");
    }

    @Test
    void extractText_validImage_returnsText() {
        byte[] image = new byte[]{1, 2, 3};
        String expectedText = "Extracted text from image";
        when(mockClient.sendVisionPrompt(any(), any(), any())).thenReturn(expectedText);

        String result = service.extractText(image);

        assertEquals(expectedText, result);
    }

    @Test
    void extractText_usesCorrectPrompt() {
        byte[] image = new byte[]{1, 2, 3};
        when(mockClient.sendVisionPrompt(any(), any(), any())).thenReturn("text");

        service.extractText(image);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockClient).sendVisionPrompt(any(), promptCaptor.capture(), any());

        String capturedPrompt = promptCaptor.getValue();
        assertEquals(PromptTemplates.buildOcrPrompt(), capturedPrompt);
    }

    @Test
    void extractText_usesConfiguredModel() {
        byte[] image = new byte[]{1, 2, 3};
        String customModel = "custom-vision-model";
        VisionOcrService customService = new VisionOcrService(mockClient, customModel);
        when(mockClient.sendVisionPrompt(any(), any(), any())).thenReturn("text");

        customService.extractText(image);

        verify(mockClient).sendVisionPrompt(eq(customModel), any(), any());
    }

    @Test
    void extractText_stripsMarkdownCodeBlocks() {
        byte[] image = new byte[]{1, 2, 3};
        when(mockClient.sendVisionPrompt(any(), any(), any()))
                .thenReturn("```text\nExtracted content\n```");

        String result = service.extractText(image);

        assertEquals("Extracted content", result);
    }

    @Test
    void extractText_stripsMarkdownBold() {
        byte[] image = new byte[]{1, 2, 3};
        when(mockClient.sendVisionPrompt(any(), any(), any()))
                .thenReturn("**bold text**");

        String result = service.extractText(image);

        assertEquals("bold text", result);
    }

    @Test
    void extractText_stripsMarkdownHeaders() {
        byte[] image = new byte[]{1, 2, 3};
        when(mockClient.sendVisionPrompt(any(), any(), any()))
                .thenReturn("## Header\nContent");

        String result = service.extractText(image);

        assertEquals("Header\nContent", result);
    }

    @Test
    void extractText_nullImage_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.extractText(null));
    }

    @Test
    void extractText_emptyImage_throwsOcrException() {
        assertThrows(OcrException.class, () -> service.extractText(new byte[0]));
    }

    @Test
    void extractText_clientThrowsException_throwsOcrException() {
        byte[] image = new byte[]{1, 2, 3};
        when(mockClient.sendVisionPrompt(any(), any(), any()))
                .thenThrow(new OllamaException("Connection failed"));

        OcrException exception = assertThrows(OcrException.class, () ->
                service.extractText(image));

        assertTrue(exception.getMessage().contains("OCR failed"));
    }

    @Test
    void constructor_nullClient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new VisionOcrService(null, "model"));
    }

    @Test
    void constructor_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new VisionOcrService(mockClient, null));
    }
}
