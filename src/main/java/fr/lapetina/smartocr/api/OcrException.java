package fr.lapetina.smartocr.api;

/**
 * Runtime exception thrown when OCR processing fails.
 */
public class OcrException extends ParserException {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
