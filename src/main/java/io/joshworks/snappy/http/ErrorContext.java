package io.joshworks.snappy.http;

import java.util.UUID;

public class ErrorContext<T extends Exception> {

    public final String id;
    public final T exception;

    ErrorContext(String id, T exception) {
        this.id = id;
        this.exception = exception;
    }

    static String errorId() {
        return UUID.randomUUID().toString();
    }

}