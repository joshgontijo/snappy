package io.joshworks.snappy.http;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

public class HeaderUtils {

    static String extractAuthorizationValue(String type, HeaderMap headers) {
        HeaderValues values = headers.get(Headers.AUTHORIZATION_STRING);
        if (values == null || values.isEmpty()) {
            return null;
        }

        for (String value : values) {
            String[] parts = value.split(" ");
            if (parts.length == 2 && parts[0].trim().equals(type)) {
                String valTrimmed = parts[1].trim();
                return valTrimmed.isEmpty() ? null : valTrimmed;
            }
        }
        return null;
    }

    static String userAgent(HeaderMap headers) {
        HeaderValues userAgent = headers.get(Headers.USER_AGENT);
        if (userAgent != null && !userAgent.isEmpty()) {
            return userAgent.getFirst();
        }
        return null;
    }

    static MediaType contentType(HeaderMap headers) {
        HeaderValues contentType = headers.get(Headers.CONTENT_TYPE);
        if (contentType != null && !contentType.isEmpty()) {
            return MediaType.valueOf(contentType.getFirst());
        }
        return MediaType.WILDCARD_TYPE;
    }
}
