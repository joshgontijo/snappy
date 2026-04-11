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

import io.joshworks.snappy.handler.ChainHandler;
import io.joshworks.snappy.handler.HandlerUtil;
import io.joshworks.snappy.handler.UnsupportedMediaType;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by Josh Gontijo on 3/5/17.
 * <p>
 * This should be the higher level Rest class, ideally this is wrapped in the Error handler which will return
 * handler exceptions thrown by the endpoints
 */
public class HttpDispatcher extends ChainHandler {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    static AttachmentKey<Response> RESPONSE = AttachmentKey.create(Response.class);
    static AttachmentKey<RequestContext> REQUEST = AttachmentKey.create(RequestContext.class);

    private final ExceptionMapper exceptionMapper;

    public HttpDispatcher(HttpHandler next, ExceptionMapper exceptionMapper) {
        super(next);
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        RequestContext requestContext = new RequestContext(exchange);
        exchange.putAttachment(REQUEST, requestContext);
        try {
            this.next.handleRequest(exchange);
            Response response = exchange.getAttachment(RESPONSE);
            if (response != null) { //TODO ASYNC ?
                response.handle(exchange);
            }

        } catch (UnsupportedMediaType connex) {
            String errorId = ErrorContext.errorId();
            logger.error(HandlerUtil.exceptionMessageTemplate(errorId, exchange, "Unsupported media type " + connex.headerValues + " supported types: " + connex.types));
            sendErrorResponse(errorId, exchange, connex);
            exchange.endExchange();

        } catch (Exception e) { //Should not happen (server error)
            String errorId = ErrorContext.errorId();
            logger.error(HandlerUtil.exceptionMessageTemplate(errorId, exchange, "Server error"), e);
            sendErrorResponse(errorId, exchange, e);
        }
    }

    private <T extends Exception> void sendErrorResponse(String errorId, HttpServerExchange exchange, T ex) {
        try {
            Response response = exceptionMapper.apply(errorId, ex, new Request(exchange));
            response.handle(exchange);
        } catch (Exception handlingError) {
            logger.error("Exception was thrown when executing handler, body will be null", handlingError);
        }
    }

}
