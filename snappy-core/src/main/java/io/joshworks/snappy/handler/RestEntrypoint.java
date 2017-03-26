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

package io.joshworks.snappy.handler;

import io.joshworks.snappy.rest.ExceptionMapper;
import io.joshworks.snappy.rest.RestExchange;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static io.joshworks.snappy.SnappyServer.*;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class RestEntrypoint implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final Consumer<RestExchange> endpoint;
    private final ExceptionMapper exceptionMapper;

    public RestEntrypoint(Consumer<RestExchange> endpoint, ExceptionMapper exceptionMapper) {
        this.endpoint = endpoint;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        RestExchange restExchange = new RestExchange(exchange);
        try {
            if (!exchange.isResponseComplete()) {
                endpoint.accept(restExchange);
            }
        }  catch (Exception e) {
            HttpString requestMethod = exchange.getRequestMethod();
            String requestPath = exchange.getRequestPath();
            logger.error("Exception was thrown from " + requestMethod + " " + requestPath, e);
            exceptionMapper.getOrFallback(e).onException(e, restExchange);
            exchange.endExchange();

        }
    }

}
