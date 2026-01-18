package fr.lapetina.smartocr.api;

import fr.lapetina.smartocr.pipeline.ExtractionStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDocumentParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCHEMA = "{\"type\": \"object\"}";

    @Mock
    private ExtractionStrategy mockStrategy;

    private DefaultDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultDocumentParser(mockStrategy);
    }

    @Test
    void parseImage_validInput_delegatesToStrategy() throws Exception {
        byte[] image = new byte[]{1, 2, 3};
        JsonNode expectedResult = MAPPER.readTree("{\"result\": \"success\"}");
        when(mockStrategy.extract(any(), any(), eq(SCHEMA))).thenReturn(expectedResult);

        JsonNode result = parser.parseImage(image, SCHEMA);

        assertEquals(expectedResult, result);
        verify(mockStrategy).extract(
                eq(Optional.of(image)),
                eq(Optional.empty()),
                eq(SCHEMA)
        );
    }

    @Test
    void parseText_validInput_delegatesToStrategy() throws Exception {
        String text = "sample text";
        JsonNode expectedResult = MAPPER.readTree("{\"result\": \"success\"}");
        when(mockStrategy.extract(any(), any(), eq(SCHEMA))).thenReturn(expectedResult);

        JsonNode result = parser.parseText(text, SCHEMA);

        assertEquals(expectedResult, result);
        verify(mockStrategy).extract(
                eq(Optional.empty()),
                eq(Optional.of(text)),
                eq(SCHEMA)
        );
    }

    @Test
    void parse_bothInputsEmpty_throwsParserException() {
        assertThrows(ParserException.class, () ->
                parser.parse(Optional.empty(), Optional.empty(), SCHEMA));
    }

    @Test
    void parse_nullImage_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                parser.parse(null, Optional.of("text"), SCHEMA));
    }

    @Test
    void parse_nullText_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                parser.parse(Optional.of(new byte[]{1}), null, SCHEMA));
    }

    @Test
    void parse_nullSchema_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                parser.parse(Optional.of(new byte[]{1}), Optional.empty(), null));
    }

    @Test
    void parse_blankSchema_throwsParserException() {
        assertThrows(ParserException.class, () ->
                parser.parse(Optional.of(new byte[]{1}), Optional.empty(), "   "));
    }

    @Test
    void constructor_nullStrategy_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new DefaultDocumentParser(null));
    }

    @Test
    void parse_strategyThrowsOcrException_propagates() {
        when(mockStrategy.extract(any(), any(), any()))
                .thenThrow(new OcrException("OCR failed"));

        OcrException exception = assertThrows(OcrException.class, () ->
                parser.parseImage(new byte[]{1}, SCHEMA));

        assertTrue(exception.getMessage().contains("OCR failed"));
    }

    @Test
    void parse_strategyThrowsExtractionException_propagates() {
        when(mockStrategy.extract(any(), any(), any()))
                .thenThrow(new ExtractionException("Extraction failed"));

        ExtractionException exception = assertThrows(ExtractionException.class, () ->
                parser.parseText("text", SCHEMA));

        assertTrue(exception.getMessage().contains("Extraction failed"));
    }

    @Test
    void builder_defaultConfiguration_createsParser() {
        DefaultDocumentParser parser = DefaultDocumentParser.builder().build();

        assertNotNull(parser);
    }

    @Test
    void builder_customUrl_createsParser() {
        DefaultDocumentParser parser = DefaultDocumentParser.builder()
                .ollamaBaseUrl("http://custom:11434")
                .build();

        assertNotNull(parser);
    }

    @Test
    void builder_customModels_createsParser() {
        DefaultDocumentParser parser = DefaultDocumentParser.builder()
                .visionModel("llava")
                .textModel("mistral")
                .build();

        assertNotNull(parser);
    }
}
