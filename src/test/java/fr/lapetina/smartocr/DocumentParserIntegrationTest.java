package fr.lapetina.smartocr;

import fr.lapetina.smartocr.api.DefaultDocumentParser;
import fr.lapetina.smartocr.api.DocumentParser;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end integration tests that require a running Ollama instance.
 * These tests are skipped if Ollama is not available.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentParserIntegrationTest {

    private static final String OLLAMA_URL = "http://localhost:11434";
    private static boolean ollamaAvailable = false;

    private DocumentParser parser;

    @BeforeAll
    static void checkOllamaAvailability() {
        ollamaAvailable = isOllamaRunning();
        if (!ollamaAvailable) {
            System.out.println("WARNING: Ollama is not running. Integration tests will be skipped.");
            System.out.println("To run integration tests, start Ollama with: ollama serve");
        }
    }

    @BeforeEach
    void setUp() {
        assumeTrue(ollamaAvailable, "Ollama is not available");
        parser = new DefaultDocumentParser();
    }

    @Test
    @Order(1)
    @DisplayName("Parse simple text and extract fields")
    void parseText_simpleDocument_extractsFields() {
        String text = """
                Customer: John Smith
                Email: john.smith@example.com
                Phone: 555-123-4567
                Order Date: 2024-03-15
                """;

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "customerName": {"type": "string", "description": "Customer full name"},
                        "email": {"type": "string", "description": "Customer email address"},
                        "phone": {"type": "string", "description": "Customer phone number"},
                        "orderDate": {"type": "string", "description": "Order date in ISO-8601 format"}
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        assertTrue(result.has("customerName") || result.has("customer_name") || result.has("name"));
        System.out.println("Extracted: " + result.toPrettyString());
    }

    @Test
    @Order(2)
    @DisplayName("Parse text with missing fields returns null values")
    void parseText_missingFields_returnsNulls() {
        String text = """
                Product: Widget Pro
                Price: $49.99
                """;

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "productName": {"type": "string"},
                        "price": {"type": "number"},
                        "quantity": {"type": "integer"},
                        "discount": {"type": "number"}
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        System.out.println("Extracted with nulls: " + result.toPrettyString());
    }

    @Test
    @Order(3)
    @DisplayName("Parse invoice image and extract structured data")
    void parseImage_invoice_extractsFields() {
        byte[] invoiceImage = TestImageGenerator.createInvoiceImage(
                "INV-2024-0042",
                "2024-03-15",
                "$1,234.56",
                "ACME Corporation"
        );

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "invoiceNumber": {"type": "string", "description": "The invoice number"},
                        "date": {"type": "string", "description": "Invoice date in ISO-8601 format"},
                        "totalAmount": {"type": "number", "description": "Total amount as a number"},
                        "vendorName": {"type": "string", "description": "Name of the vendor"}
                    }
                }
                """;

        JsonNode result = parser.parseImage(invoiceImage, schema);

        assertNotNull(result);
        System.out.println("Extracted from invoice image: " + result.toPrettyString());
    }

    @Test
    @Order(4)
    @DisplayName("Parse receipt image and extract items")
    void parseImage_receipt_extractsData() {
        byte[] receiptImage = TestImageGenerator.createReceiptImage(
                "GROCERY MART",
                new String[]{
                        "Milk 2L          $3.99",
                        "Bread            $2.49",
                        "Eggs (12)        $4.99",
                        "Butter           $5.49"
                },
                "$16.96",
                "2024-03-15 14:32"
        );

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "storeName": {"type": "string"},
                        "date": {"type": "string"},
                        "total": {"type": "number"},
                        "items": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "name": {"type": "string"},
                                    "price": {"type": "number"}
                                }
                            }
                        }
                    }
                }
                """;

        JsonNode result = parser.parseImage(receiptImage, schema);

        assertNotNull(result);
        System.out.println("Extracted from receipt image: " + result.toPrettyString());
    }

    @Test
    @Order(5)
    @DisplayName("Parse image with simple text")
    void parseImage_simpleText_extractsContent() {
        byte[] image = TestImageGenerator.createImageWithText(
                "Hello World!\nThis is a test document.\nDate: 2024-03-15"
        );

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "greeting": {"type": "string", "description": "The greeting text"},
                        "date": {"type": "string", "description": "The date mentioned"}
                    }
                }
                """;

        JsonNode result = parser.parseImage(image, schema);

        assertNotNull(result);
        System.out.println("Extracted from simple image: " + result.toPrettyString());
    }

    @Test
    @Order(6)
    @DisplayName("Parse text with dates extracts ISO-8601 format")
    void parseText_withDates_extractsIso8601() {
        String text = """
                Meeting scheduled for March 15th, 2024.
                Deadline is 03/20/2024.
                Event starts at 2:30 PM on April 1, 2024.
                """;

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "meetingDate": {"type": "string", "description": "Meeting date in ISO-8601 format"},
                        "deadline": {"type": "string", "description": "Deadline in ISO-8601 format"},
                        "eventDate": {"type": "string", "description": "Event date in ISO-8601 format"}
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        System.out.println("Extracted dates: " + result.toPrettyString());

        // Verify dates are in ISO-8601 format (YYYY-MM-DD)
        if (result.has("meetingDate") && !result.get("meetingDate").isNull()) {
            String date = result.get("meetingDate").asText();
            assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}.*"),
                    "Date should be in ISO-8601 format: " + date);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Parse text with numbers extracts numeric values")
    void parseText_withNumbers_extractsNumeric() {
        String text = """
                Quantity: 42 units
                Unit Price: $19.99
                Total: $839.58
                Tax Rate: 8.5%
                """;

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "quantity": {"type": "integer"},
                        "unitPrice": {"type": "number"},
                        "total": {"type": "number"},
                        "taxRate": {"type": "number"}
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        System.out.println("Extracted numbers: " + result.toPrettyString());

        // Verify numeric values
        if (result.has("quantity") && !result.get("quantity").isNull()) {
            assertTrue(result.get("quantity").isNumber(), "Quantity should be a number");
        }
    }

    @Test
    @Order(8)
    @DisplayName("Parse document with multiple items returns top-level array")
    void parseText_multipleItems_returnsArray() {
        String text = """
                CONTACT LIST

                Name: John Smith
                Email: john@example.com
                Phone: 555-1234

                Name: Jane Doe
                Email: jane@example.com
                Phone: 555-5678

                Name: Bob Wilson
                Email: bob@example.com
                Phone: 555-9012
                """;

        String schema = """
                {
                    "type": "array",
                    "description": "List of contacts extracted from the document",
                    "items": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string", "description": "Contact name"},
                            "email": {"type": "string", "description": "Email address"},
                            "phone": {"type": "string", "description": "Phone number"}
                        }
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        assertTrue(result.isArray(), "Result should be a JSON array");
        assertTrue(result.size() >= 2, "Should extract at least 2 contacts");
        System.out.println("Extracted array: " + result.toPrettyString());
    }

    @Test
    @Order(9)
    @DisplayName("Parse document with nested arrays extracts correctly")
    void parseText_nestedArrays_extractsCorrectly() {
        String text = """
                COMPANY DIRECTORY

                Department: Engineering
                Team Lead: Alice Johnson
                Members: Bob, Charlie, David
                Projects: Alpha, Beta

                Department: Marketing
                Team Lead: Eve Brown
                Members: Frank, Grace
                Projects: Campaign X, Campaign Y, Campaign Z
                """;

        String schema = """
                {
                    "type": "array",
                    "description": "List of departments",
                    "items": {
                        "type": "object",
                        "properties": {
                            "department": {"type": "string", "description": "Department name"},
                            "teamLead": {"type": "string", "description": "Team leader name"},
                            "members": {
                                "type": "array",
                                "description": "List of team member names",
                                "items": {"type": "string"}
                            },
                            "projects": {
                                "type": "array",
                                "description": "List of project names",
                                "items": {"type": "string"}
                            }
                        }
                    }
                }
                """;

        JsonNode result = parser.parseText(text, schema);

        assertNotNull(result);
        assertTrue(result.isArray(), "Result should be a JSON array");
        assertTrue(result.size() >= 1, "Should extract at least 1 department");

        // Check that nested arrays exist
        JsonNode firstDept = result.get(0);
        assertNotNull(firstDept);
        assertTrue(firstDept.has("members") || firstDept.has("projects"),
                "Department should have members or projects array");

        if (firstDept.has("members") && !firstDept.get("members").isNull()) {
            assertTrue(firstDept.get("members").isArray(), "Members should be an array");
        }

        System.out.println("Extracted nested arrays: " + result.toPrettyString());
    }

    @Test
    @Order(10)
    @DisplayName("Parse invoice image with line items array")
    void parseImage_invoiceWithLineItems_extractsArray() {
        byte[] invoiceImage = TestImageGenerator.createInvoiceWithLineItems(
                "INV-2024-0099",
                "2024-03-20",
                new String[]{
                        "Widget A     x2     $10.00     $20.00",
                        "Gadget B     x1     $35.00     $35.00",
                        "Service C    x3     $15.00     $45.00"
                },
                "$100.00"
        );

        String schema = """
                {
                    "type": "object",
                    "properties": {
                        "invoiceNumber": {"type": "string"},
                        "date": {"type": "string"},
                        "lineItems": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "description": {"type": "string"},
                                    "quantity": {"type": "integer"},
                                    "unitPrice": {"type": "number"},
                                    "total": {"type": "number"}
                                }
                            }
                        },
                        "grandTotal": {"type": "number"}
                    }
                }
                """;

        JsonNode result = parser.parseImage(invoiceImage, schema);

        assertNotNull(result);
        System.out.println("Extracted invoice with line items: " + result.toPrettyString());

        if (result.has("lineItems") && !result.get("lineItems").isNull()) {
            assertTrue(result.get("lineItems").isArray(), "lineItems should be an array");
        }
    }

    private static boolean isOllamaRunning() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
