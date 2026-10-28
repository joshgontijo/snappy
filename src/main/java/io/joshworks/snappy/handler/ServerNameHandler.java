package io.joshworks.snappy.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class ServerNameHandler implements HttpHandler {

    private static final String SERVER_NAME = "snappy";

    private final HttpHandler next;

    public ServerNameHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.SERVER, SERVER_NAME);
        exchange.getResponseHeaders().put(Headers.X_CONTENT_TYPE_OPTIONS, "nosniff");
        next.handleRequest(exchange);
    }
}
