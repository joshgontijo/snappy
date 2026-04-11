package io.joshworks.snappy.http;

import io.joshworks.snappy.handler.HandlerUtil;

public abstract class Interceptor {

    private final String url;
    private final boolean wildcard;

    protected Interceptor(String url) {
        url = HandlerUtil.parseUrl(url);
        this.wildcard = url.endsWith(HandlerUtil.WILDCARD);
        this.url = wildcard ? url.substring(0, url.length() - 1) : url;
    }

    public String url() {
        return url;
    }

    public boolean match(String url) {
        return wildcard ? url.startsWith(this.url) : url.equals(this.url);
    }

}
