package io.joshworks.snappy.http.body;

public class BodyReadException extends RuntimeException {
    public BodyReadException(Throwable cause) {
        super("Failed to read body", cause);
    }
}
