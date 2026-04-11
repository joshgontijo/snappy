package io.joshworks.snappy.http;

public class ErrorContext<T extends Exception> {

    public final String id;
    public final T exception;

    ErrorContext(String id, T exception) {
        this.id = id;
        this.exception = exception;
    }

    static String errorId() {
        return String.valueOf(System.currentTimeMillis());
    }

}
