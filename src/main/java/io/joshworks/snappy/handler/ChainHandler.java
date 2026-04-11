package io.joshworks.snappy.handler;

import io.undertow.server.HttpHandler;

/**
 * Created by Josh Gontijo on 10/15/17.
 */
public abstract class ChainHandler implements HttpHandler {

    protected final HttpHandler next;

    public ChainHandler(HttpHandler next) {
        this.next = next;
    }
}
