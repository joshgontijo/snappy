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
import io.joshworks.snappy.handler.UnsupportedMediaType;
import io.joshworks.snappy.parser.MediaTypes;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class ConnegHandler extends ChainHandler {

    public static final AttachmentKey<NegotiatedMediaType> NEGOTIATED_MEDIA_TYPE = AttachmentKey.create(NegotiatedMediaType.class);
    private MediaTypes consumes;
    private MediaTypes produces;

    public ConnegHandler(HttpHandler next, MediaTypes... mimeTypes) {
        super(next);
        initTypes(mimeTypes);
        consumes = consumes == null ? MediaTypes.DEFAULT_CONSUMES : consumes;
        produces = produces == null ? MediaTypes.DEFAULT_PRODUCES : produces;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        MediaType consumesType = matchConsumesMime(exchange);
        if (consumesType == null) {
            invalidMediaType(exchange, Headers.CONTENT_TYPE);
            return;
        }

        MediaType producesType = matchProducesMime(exchange);
        if (producesType == null) {
            invalidMediaType(exchange, Headers.ACCEPT);
            return;
        }

        exchange.putAttachment(NEGOTIATED_MEDIA_TYPE, new NegotiatedMediaType(consumesType, producesType));
        next.handleRequest(exchange);
    }

    private void initTypes(MediaTypes... mimeTypes) {
        for (MediaTypes type : mimeTypes) {
            if (MediaTypes.Context.CONSUMES.equals(type.getContext())) {
                if (consumes != null) {
                    consumes.addAll(type);
                } else {
                    consumes = type;
                }
            } else {
                if (produces != null) {
                    produces.addAll(type);
                } else {
                    produces = type;
                }
            }
        }
    }

    private void invalidMediaType(HttpServerExchange exchange, HttpString headerName) throws UnsupportedMediaType {
        HeaderValues headerValues = exchange.getRequestHeaders().get(headerName);
        exchange.setStatusCode(StatusCodes.UNSUPPORTED_MEDIA_TYPE);
        throw UnsupportedMediaType.unsuportedMediaType(headerValues, produces);
    }

    private MediaType matchProducesMime(HttpServerExchange exchange) {
        HeaderValues acceptHeader = exchange.getRequestHeaders().get(Headers.ACCEPT);
        boolean hasAcceptHeader = acceptHeader != null && !acceptHeader.isEmpty();
        if (hasAcceptHeader) {
            return produces.match(acceptHeader);
        }
        //if no Accept header is specified, the first specified by user OR default is used
        return produces.getDefaultType();
    }

    private MediaType matchConsumesMime(HttpServerExchange exchange) {
        HeaderValues bodyContentType = exchange.getRequestHeaders().get(Headers.CONTENT_TYPE);
        boolean hasContentType = bodyContentType != null && !bodyContentType.isEmpty();
        if (hasContentType) {
            return consumes.match(bodyContentType);
        }
        //If no content type is specified, json is used
        return consumes.getDefaultType();
    }

    public static class NegotiatedMediaType {
        public final MediaType consumes;
        public final MediaType produces;

        NegotiatedMediaType(MediaType consumes, MediaType produces) {
            this.consumes = consumes;
            this.produces = produces;
        }
    }
}
