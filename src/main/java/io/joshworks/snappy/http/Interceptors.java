/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.snappy.http;

import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Created by Josh Gontijo on 3/23/17.
 */
public class Interceptors {

    private final List<RequestInterceptor> requestInterceptors = new LinkedList<>();
    private final List<ResponseInterceptor> responseInterceptors = new LinkedList<>();

    private final List<RequestInterceptor> rootRequestInterceptors = new LinkedList<>();

    public void add(RequestInterceptor interceptor) {
        requestInterceptors.add(interceptor);
    }

    public void add(ResponseInterceptor interceptor) {
        responseInterceptors.add(interceptor);
    }

    public void addRoot(RequestInterceptor interceptor) {
        rootRequestInterceptors.add(interceptor);
    }

    public List<RequestInterceptor> requestInterceptors() {
        return requestInterceptors;
    }

    public List<ResponseInterceptor> responseInterceptors() {
        return responseInterceptors;
    }

    public List<RequestInterceptor> rootRequestInterceptors() {
        return rootRequestInterceptors;
    }

    public static RequestInterceptor cors() {
        return new RequestInterceptor("/*", (req) -> {
            if (req.method().equalsIgnoreCase(Methods.OPTIONS_STRING)) {
                Response response = Response.ok()
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Credentials", "true")
                        .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                        .header("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Authorization, Access-Control-Request-Method, Access-Control-Request-Headers");
                req.abortWith(response);
            }
        });
    }

    public static RequestInterceptor secured(String pattern, BiPredicate<String, String> handler) {
        return new RequestInterceptor(pattern, (req) -> {
            HeaderValues values = req.headers(Headers.AUTHORIZATION_STRING);
            if (values == null || values.isEmpty()) {
                req.abortWith(Response.unauthorized());
                return;
            }

            for (String value : values) {
                String[] parts = value.split(" ");
                if (parts.length == 2) {
                    String keyTrimmed = parts[0].trim();
                    String valTrimmed = parts[1].trim();
                    String type = keyTrimmed.isEmpty() ? null : keyTrimmed;
                    String val = valTrimmed.isEmpty() ? null : valTrimmed;
                    if (type != null && val != null && handler.test(type, val)) {
                        return;
                    }
                }
            }
            req.abortWith(Response.unauthorized());
        });
    }

    public static RequestInterceptor basicAuthentication(String pattern, BiPredicate<String, String> userPswAuthenticator) {
        return secured(pattern, (type, value) -> {
            if (!type.equals("Basic")) {
                return false;
            }
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            String[] split = decoded.split(":");
            return userPswAuthenticator.test(split[0], split[1]);
        });
    }

}
