package fr.lapetina.smartocr.ollama;

import fr.lapetina.smartocr.api.OcrException;
import fr.lapetina.smartocr.util.PromptTemplates;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * OCR service that extracts plain text from images using Ollama's vision model.
 */
public class VisionOcrService {

    private static final String DEFAULT_MODEL = "llama3.2-vision";

    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("```[a-z]*\\n?|```");
    private static final Pattern MARKDOWN_BOLD = Pattern.compile("\\*\\*|__");
    private static final Pattern MARKDOWN_ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)");
    private static final Pattern MARKDOWN_HEADERS = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);
    private static final Pattern MARKDOWN_LINKS = Pattern.compile("\\[([^]]+)]\\([^)]+\\)");
    private static final Pattern MARKDOWN_INLINE_CODE = Pattern.compile("`([^`]+)`");

    private final LlmClient client;
    private final String model;

    /**
     * Creates a VisionOcrService with the specified LLM client and model.
     *
     * @param client the LLM client to use
     * @param model  the vision model name
     */
    public VisionOcrService(LlmClient client, String model) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    /**
     * Creates a VisionOcrService with the specified LLM client and default model.
     *
     * @param client the LLM client to use
     */
    public VisionOcrService(LlmClient client) {
        this(client, DEFAULT_MODEL);
    }

    /**
     * Extracts plain text from an image using OCR.
     *
     * @param image the image data
     * @return the extracted plain text
     * @throws OcrException if OCR fails
     */
    public String extractText(byte[] image) {
        Objects.requireNonNull(image, "image must not be null");

        if (image.length == 0) {
            throw new OcrException("image must not be empty");
        }

        try {
            String response = client.sendVisionPrompt(model, PromptTemplates.buildOcrPrompt(), image);
            return stripMarkdown(response);
        } catch (OllamaException e) {
            throw new OcrException("OCR failed: " + e.getMessage(), e);
        }
    }

    private String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;
        result = MARKDOWN_CODE_BLOCK.matcher(result).replaceAll("");
        result = MARKDOWN_BOLD.matcher(result).replaceAll("");
        result = MARKDOWN_ITALIC.matcher(result).replaceAll("");
        result = MARKDOWN_HEADERS.matcher(result).replaceAll("");
        result = MARKDOWN_LINKS.matcher(result).replaceAll("$1");
        result = MARKDOWN_INLINE_CODE.matcher(result).replaceAll("$1");

        return result.trim();
    }
}
