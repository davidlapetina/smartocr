package fr.lapetina.smartocr.api;

/**
 * Runtime exception thrown when structured extraction fails.
 */
public class ExtractionException extends ParserException {

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
