package io.joshworks.snappy.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import static java.util.Objects.requireNonNull;

public class RequestContext extends Request {

    Response response;

    RequestContext(HttpServerExchange exchange) {
        super(exchange);
    }

    public void abortWith(Response response) {
        this.response = requireNonNull(response, "Response must not be null");
    }

    public RequestContext header(String name, String value) {
        exchange.getRequestHeaders().put(HttpString.tryFromString(name), value);
        return this;
    }

    public RequestContext header(String name, long value) {
        exchange.getRequestHeaders().put(HttpString.tryFromString(name), value);
        return this;
    }

    public RequestContext cookie(Cookie cookie) {
        if (cookie != null) {
            exchange.getRequestCookies().put(cookie.getName(), cookie);
        }
        return this;
    }

    public RequestContext type(MediaType mediaType) {
        return type(requireNonNull(mediaType, "MediaType must not be null").toString());
    }

    public RequestContext type(String mediaType) {
        exchange.getRequestHeaders().put(Headers.CONTENT_TYPE, mediaType);
        return this;
    }

}
