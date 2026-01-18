package fr.lapetina.smartocr.pipeline;

import fr.lapetina.smartocr.TestImageGenerator;
import fr.lapetina.smartocr.api.ParserException;
import fr.lapetina.smartocr.ollama.LlmClient;
import fr.lapetina.smartocr.ollama.StructuredExtractionService;
import fr.lapetina.smartocr.ollama.VisionOcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParsingPipelineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCHEMA = "{\"type\": \"object\"}";

    @Mock
    private LlmClient mockClient;

    private ParsingPipeline pipeline;
    private VisionOcrService ocrService;
    private StructuredExtractionService extractionService;

    @BeforeEach
    void setUp() {
        ocrService = new VisionOcrService(mockClient, "llama3.2-vision");
        extractionService = new StructuredExtractionService(mockClient, "llama3.2");
        pipeline = new ParsingPipeline(ocrService, extractionService);
    }

    @Test
    void extract_imageProvided_performsOcrThenExtraction() throws Exception {
        byte[] image = new byte[]{1, 2, 3};
        String ocrText = "Extracted OCR text";

        // Mock vision prompt for OCR
        when(mockClient.sendVisionPrompt(eq("llama3.2-vision"), any(), eq(image)))
                .thenReturn(ocrText);
        // Mock text prompt for extraction
        when(mockClient.sendPrompt(eq("llama3.2"), any()))
                .thenReturn("{\"name\": \"test\"}");

        JsonNode result = pipeline.extract(Optional.of(image), Optional.empty(), SCHEMA);

        assertNotNull(result);
        assertEquals("test", result.get("name").asText());
        verify(mockClient).sendVisionPrompt(eq("llama3.2-vision"), any(), eq(image));
        verify(mockClient).sendPrompt(eq("llama3.2"), any());
    }

    @Test
    void extract_textProvided_skipsOcr() throws Exception {
        String text = "Direct text input";

        when(mockClient.sendPrompt(eq("llama3.2"), any()))
                .thenReturn("{\"name\": \"test\"}");

        JsonNode result = pipeline.extract(Optional.empty(), Optional.of(text), SCHEMA);

        assertNotNull(result);
        assertEquals("test", result.get("name").asText());
        verify(mockClient, never()).sendVisionPrompt(any(), any(), any());
        verify(mockClient).sendPrompt(eq("llama3.2"), any());
    }

    @Test
    void extract_bothProvided_imageTakesPrecedence() throws Exception {
        byte[] image = new byte[]{1, 2, 3};
        String text = "Direct text (should be ignored)";
        String ocrText = "OCR extracted text";

        when(mockClient.sendVisionPrompt(eq("llama3.2-vision"), any(), eq(image)))
                .thenReturn(ocrText);
        when(mockClient.sendPrompt(eq("llama3.2"), any()))
                .thenReturn("{\"name\": \"test\"}");

        JsonNode result = pipeline.extract(Optional.of(image), Optional.of(text), SCHEMA);

        assertNotNull(result);
        verify(mockClient).sendVisionPrompt(eq("llama3.2-vision"), any(), eq(image));
    }

    @Test
    void extract_neitherProvided_throwsParserException() {
        assertThrows(ParserException.class, () ->
                pipeline.extract(Optional.empty(), Optional.empty(), SCHEMA));
    }

    @Test
    void extract_emptyImageArray_throwsParserException() {
        assertThrows(ParserException.class, () ->
                pipeline.extract(Optional.of(new byte[0]), Optional.empty(), SCHEMA));
    }

    @Test
    void extract_blankText_throwsParserException() {
        assertThrows(ParserException.class, () ->
                pipeline.extract(Optional.empty(), Optional.of("   "), SCHEMA));
    }

    @Test
    void extract_nullImage_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                pipeline.extract(null, Optional.of("text"), SCHEMA));
    }

    @Test
    void extract_nullText_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                pipeline.extract(Optional.of(new byte[]{1}), null, SCHEMA));
    }

    @Test
    void extract_nullSchema_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                pipeline.extract(Optional.of(new byte[]{1}), Optional.empty(), null));
    }

    @Nested
    @DisplayName("Tests with TestImageGenerator")
    class TestImageGeneratorTests {

        @Test
        @DisplayName("Invoice image passes through pipeline correctly")
        void extract_invoiceImage_processesCorrectly() throws Exception {
            byte[] invoiceImage = TestImageGenerator.createInvoiceImage(
                    "INV-2024-001",
                    "2024-03-15",
                    "$500.00",
                    "Test Vendor Inc."
            );
            String ocrText = "INVOICE\nTest Vendor Inc.\nInvoice Number: INV-2024-001\nDate: 2024-03-15\nTotal Amount: $500.00";
            String extractedJson = """
                    {"invoiceNumber": "INV-2024-001", "date": "2024-03-15", "totalAmount": 500.00, "vendorName": "Test Vendor Inc."}
                    """;
            String invoiceSchema = """
                    {
                        "type": "object",
                        "properties": {
                            "invoiceNumber": {"type": "string"},
                            "date": {"type": "string"},
                            "totalAmount": {"type": "number"},
                            "vendorName": {"type": "string"}
                        }
                    }
                    """;

            when(mockClient.sendVisionPrompt(eq("llama3.2-vision"), any(), eq(invoiceImage)))
                    .thenReturn(ocrText);
            when(mockClient.sendPrompt(eq("llama3.2"), any()))
                    .thenReturn(extractedJson);

            JsonNode result = pipeline.extract(Optional.of(invoiceImage), Optional.empty(), invoiceSchema);

            assertNotNull(result);
            assertEquals("INV-2024-001", result.get("invoiceNumber").asText());
            assertEquals("2024-03-15", result.get("date").asText());
            assertEquals(500.00, result.get("totalAmount").asDouble(), 0.01);
            assertEquals("Test Vendor Inc.", result.get("vendorName").asText());

            // Verify image bytes were passed correctly
            ArgumentCaptor<byte[]> imageCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(mockClient).sendVisionPrompt(eq("llama3.2-vision"), any(), imageCaptor.capture());
            assertArrayEquals(invoiceImage, imageCaptor.getValue());
        }

        @Test
        @DisplayName("Receipt image passes through pipeline correctly")
        void extract_receiptImage_processesCorrectly() throws Exception {
            byte[] receiptImage = TestImageGenerator.createReceiptImage(
                    "Coffee Shop",
                    new String[]{"Latte         $4.50", "Muffin        $3.00"},
                    "$7.50",
                    "2024-03-15 10:30"
            );
            String ocrText = "Coffee Shop\n2024-03-15 10:30\nLatte $4.50\nMuffin $3.00\nTOTAL: $7.50";
            String extractedJson = """
                    {"storeName": "Coffee Shop", "total": 7.50, "items": [{"name": "Latte", "price": 4.50}, {"name": "Muffin", "price": 3.00}]}
                    """;
            String receiptSchema = """
                    {
                        "type": "object",
                        "properties": {
                            "storeName": {"type": "string"},
                            "total": {"type": "number"},
                            "items": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "price": {"type": "number"}}}}
                        }
                    }
                    """;

            when(mockClient.sendVisionPrompt(eq("llama3.2-vision"), any(), eq(receiptImage)))
                    .thenReturn(ocrText);
            when(mockClient.sendPrompt(eq("llama3.2"), any()))
                    .thenReturn(extractedJson);

            JsonNode result = pipeline.extract(Optional.of(receiptImage), Optional.empty(), receiptSchema);

            assertNotNull(result);
            assertEquals("Coffee Shop", result.get("storeName").asText());
            assertEquals(7.50, result.get("total").asDouble(), 0.01);
            assertTrue(result.get("items").isArray());
            assertEquals(2, result.get("items").size());
        }

        @Test
        @DisplayName("Simple text image passes through pipeline correctly")
        void extract_simpleTextImage_processesCorrectly() throws Exception {
            byte[] textImage = TestImageGenerator.createImageWithText(
                    "Customer: Jane Doe\nEmail: jane@example.com"
            );
            String ocrText = "Customer: Jane Doe\nEmail: jane@example.com";
            String extractedJson = """
                    {"customerName": "Jane Doe", "email": "jane@example.com"}
                    """;
            String customerSchema = """
                    {
                        "type": "object",
                        "properties": {
                            "customerName": {"type": "string"},
                            "email": {"type": "string"}
                        }
                    }
                    """;

            when(mockClient.sendVisionPrompt(eq("llama3.2-vision"), any(), eq(textImage)))
                    .thenReturn(ocrText);
            when(mockClient.sendPrompt(eq("llama3.2"), any()))
                    .thenReturn(extractedJson);

            JsonNode result = pipeline.extract(Optional.of(textImage), Optional.empty(), customerSchema);

            assertNotNull(result);
            assertEquals("Jane Doe", result.get("customerName").asText());
            assertEquals("jane@example.com", result.get("email").asText());
        }

        @Test
        @DisplayName("Generated image is valid PNG data")
        void createImageWithText_generatesValidPngData() {
            byte[] image = TestImageGenerator.createImageWithText("Test");

            assertNotNull(image);
            assertTrue(image.length > 0);
            // PNG magic bytes
            assertEquals((byte) 0x89, image[0]);
            assertEquals((byte) 0x50, image[1]); // 'P'
            assertEquals((byte) 0x4E, image[2]); // 'N'
            assertEquals((byte) 0x47, image[3]); // 'G'
        }

        @Test
        @DisplayName("Custom dimensions create different sized images")
        void createImageWithText_customDimensions_createsDifferentSizes() {
            byte[] smallImage = TestImageGenerator.createImageWithText("Test", 100, 50);
            byte[] largeImage = TestImageGenerator.createImageWithText("Test", 800, 600);

            assertNotNull(smallImage);
            assertNotNull(largeImage);
            // Larger images should generally produce more bytes
            assertTrue(largeImage.length > smallImage.length,
                    "Large image should have more bytes than small image");
        }
    }
}
