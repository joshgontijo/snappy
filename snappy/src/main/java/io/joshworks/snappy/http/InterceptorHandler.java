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
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by Josh Gontijo on 3/19/17.
 */
public class InterceptorHandler extends ChainHandler {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;
    private final ExceptionMapper exceptionMapper = new ExceptionMapper();

    public InterceptorHandler(HttpHandler next, List<RequestInterceptor> requestInterceptors, List<ResponseInterceptor> responseInterceptors) {
        super(next);
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String url = exchange.getRequestPath();
        boolean proceed = interceptRequest(exchange, url);
        if (!proceed) {
            return;
        }

        exchange.addExchangeCompleteListener((completeExchange, nextListener) -> {
            interceptResponse(exchange, url);
        });


    }

    private boolean interceptRequest(HttpServerExchange exchange, String url) {
        RequestContext context = new RequestContext(exchange);
        for (RequestInterceptor interceptor : requestInterceptors) {
            if (!interceptor.match(url)) {
                continue;
            }
            try {
                interceptor.intercept(context);
                if (context.response != null) { //aborted
                    exchange.putAttachment(HttpDispatcher.RESPONSE, context.response);
                    context.response.handle(exchange);
                    return false;
                }
            } catch (Exception ex) {
                String message = "Error handling interceptor, request will not proceed";
                logger.error(HandlerUtil.exceptionMessageTemplate(exchange, message), ex);
                Response response = exceptionMapper.apply(ex, context);
                exchange.putAttachment(HttpDispatcher.RESPONSE, response);
                return false;
            }
        }
        return !exchange.isComplete();
    }

    private void interceptResponse(HttpServerExchange exchange, String url) {
        RequestContext requestContext = new RequestContext(exchange);
        Response response = exchange.getAttachment(HttpDispatcher.RESPONSE);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            if (!interceptor.match(url)) {
                continue;
            }
            try {
                //no response, it means HttpDispatcher hasn't been called,
                //A response is required and there's none, create one and end the request
                if (response == null) {
                    response = Response.withStatus(exchange.getStatusCode());
                    interceptor.intercept(requestContext, response);
                    response = requestContext.response == null ? response : requestContext.response;
                    response.handle(exchange);
                } else {
                    interceptor.intercept(requestContext, response);
                    response = requestContext.response == null ? response : requestContext.response;
                    exchange.putAttachment(HttpDispatcher.RESPONSE, requestContext.response);
                }

            } catch (Exception ex) {
                String message = "Error handling " + interceptor.getClass().getSimpleName() + ", request will not proceed";
                logger.error(HandlerUtil.exceptionMessageTemplate(exchange, message), ex);
                response = exceptionMapper.apply(ex, requestContext);
                exchange.putAttachment(HttpDispatcher.RESPONSE, response);
                return;
            }
        }
    }
}
