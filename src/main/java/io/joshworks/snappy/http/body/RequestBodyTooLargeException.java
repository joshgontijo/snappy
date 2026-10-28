package io.joshworks.snappy.http.body;

import java.io.IOException;

/**
 * Thrown when the decompressed request body exceeds the configured maximum entity size.
 * The compressed wire size is already limited by Undertow, so this guards against
 * compression bombs (small compressed body expanding to a very large body).
 */
public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException(long maxBytes) {
        super("Request body exceeds the maximum allowed decompressed size of " + maxBytes + " bytes");
    }
}
