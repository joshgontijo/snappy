package io.joshworks.snappy.rest;

/**
 * Created by josh on 3/5/17.
 */
public interface RestEndpoint {

    void handle(RestExchange exchange);
}