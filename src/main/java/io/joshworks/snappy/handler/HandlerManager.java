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

import io.joshworks.snappy.http.ConnegHandler;
import io.joshworks.snappy.http.ExceptionMapper;
import io.joshworks.snappy.http.HttpDispatcher;
import io.joshworks.snappy.http.InterceptorHandler;
import io.joshworks.snappy.http.Interceptors;
import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by josh on 3/11/17.
 */
public class HandlerManager {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private static final String WS_UPGRADE_HEADER_VALUE = "websocket";

    //chain of responsibility
    public static HttpHandler createRootHandler(
            List<MappedEndpoint> mappedEndpoints,
            Interceptors interceptors,
            ExceptionMapper exceptionMapper,
            String basePath,
            boolean httpTracer) {

        final RoutingHandler routingRestHandler = new HttpRootHandler(false);
        final PathTemplateHandler websocketHandler = Handlers.pathTemplate(false);
        HttpHandler staticHandler = null;


        for (MappedEndpoint me : mappedEndpoints) {

            InterceptorHandler interceptor = new InterceptorHandler(me.handler, interceptors.requestInterceptors(), interceptors.responseInterceptors());

            if (MappedEndpoint.Type.REST.equals(me.type)) {
                HttpHandler httpDispatcher =
                        new BlockingHandler(
                                new HttpDispatcher(
                                        new ConnegHandler(
                                                interceptor, me.mediaTypes
                                        ),
                                        exceptionMapper
                                )
                        );

                String endpointPath = endpointPath(basePath, me);

                HttpHandler httpHandler = wrapCompressionHandler(httpDispatcher, me);
                routingRestHandler.add(me.method, endpointPath, httpHandler);
            }
            if (MappedEndpoint.Type.SSE.equals(me.type)) {
                HttpHandler httpHandler = wrapCompressionHandler(interceptor, me);

                String endpointPath = endpointPath(basePath, me);
                routingRestHandler.add(me.method, endpointPath, httpHandler);
            }
            if (MappedEndpoint.Type.WS.equals(me.type)) {
                HttpHandler httpHandler = wrapCompressionHandler(interceptor, me);
                websocketHandler.add(me.url, httpHandler);
            }
            if (MappedEndpoint.Type.STATIC.equals(me.type)) {
                staticHandler = wrapCompressionHandler(me.handler, me);
            }
        }

        HttpHandler handler = resolveHandlers(routingRestHandler, websocketHandler, staticHandler, mappedEndpoints);

        handler = wrapRootInterceptorHandler(handler, interceptors);
        handler = wrapServerName(handler);
        handler = wrapRequestDump(handler, httpTracer);

        return Handlers.gracefulShutdown(handler);
    }

    private static String endpointPath(String basePath, MappedEndpoint me) {
        return HandlerUtil.BASE_PATH.equals(basePath) ? me.url : basePath + me.url;
    }

    private static HttpHandler wrapRootInterceptorHandler(HttpHandler original, Interceptors interceptors) {
        if (interceptors.rootRequestInterceptors().isEmpty()) {
            return original;
        }
        //No point in having response root interceptor since the response will be already completed
        return new InterceptorHandler(original, interceptors.rootRequestInterceptors(), new ArrayList<>());
    }

    private static HttpHandler wrapCompressionHandler(HttpHandler original, MappedEndpoint endpoint) {
        if (MappedEndpoint.Type.REST.equals(endpoint.type)) {
            return defaultCompressionHandler(original);
        } else if (MappedEndpoint.Type.SSE.equals(endpoint.type)) {
            return defaultCompressionHandler(original);
        } else {
            logger.warn("Content encoding not implemented for {} endpoints, response will not be compressed for '{}'.", endpoint.type, endpoint.url);
            return original;
        }
    }

    private static HttpHandler defaultCompressionHandler(HttpHandler handler) {
        return new EncodingHandler.Builder().build(new HashMap<>()).wrap(handler);
    }

    private static HttpHandler wrapRequestDump(HttpHandler original, boolean httpTracer) {
        return httpTracer ? Handlers.requestDump(original) : original;
    }

    private static HttpHandler wrapServerName(HttpHandler original) {
        return new ServerNameHandler(original);
    }

    private static HttpHandler resolveHandlers(HttpHandler rest, HttpHandler ws, HttpHandler file, List<MappedEndpoint> mappedEndpoints) {

        PredicateHandler websocketRestResolved = Handlers.predicate(value -> {
            HeaderValues upgradeHeader = value.getRequestHeaders().get(Headers.UPGRADE);
            return upgradeHeader != null && upgradeHeader.stream().anyMatch(v -> v.equalsIgnoreCase(WS_UPGRADE_HEADER_VALUE));
        }, ws, rest);

        if (file != null) {
            String[] mappedServices = HandlerUtil.removePathTemplate(mappedEndpoints);
            Predicate mappedPredicate = Predicates.prefixes(mappedServices);
            return Handlers.predicate(mappedPredicate, websocketRestResolved, file);
        }
        return websocketRestResolved;
    }


}
