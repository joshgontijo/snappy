package io.joshworks.snappy.rest;

import io.joshworks.snappy.Exchange;
import io.joshworks.snappy.handler.HandlerUtil;

import java.util.function.Consumer;

/**
 * Created by Josh Gontijo on 3/18/17.
 */
public class Interceptor {
    private final String url;
    private final Type type;
    private final Consumer<Exchange> exchange;
    private final boolean wildcard;
    public Interceptor(Type type, String url, Consumer<Exchange> exchange) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Interceptor url cannot be null or empty");
        }
        if (!url.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + url);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (occurrences(url, HandlerUtil.WILDCARD.charAt(0)) > 1) {
            throw new IllegalArgumentException("Multiple wildcards were found, this is not supported: " + url);
        }

        this.type = type;
        this.exchange = exchange;
        this.wildcard = url.endsWith(HandlerUtil.WILDCARD);
        this.url = wildcard ? url.substring(0, url.length() - 1) : url;
    }

    private static int occurrences(String str, char val) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == val) {
                count++;
            }
        }
        return count;
    }

    public void intercept(Exchange restExchange) throws Exception {
        this.exchange.accept(restExchange);
    }

    public boolean match(Type type, String url) {
        return this.type.equals(type) && (wildcard ? url.startsWith(this.url) : url.equals(this.url));
    }

    public enum Type {
        BEFORE, AFTER
    }
}
