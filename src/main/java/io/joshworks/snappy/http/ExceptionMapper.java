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

import io.joshworks.snappy.handler.UnsupportedMediaType;
import io.joshworks.snappy.http.body.BodyReadException;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class ExceptionMapper {

    private final Map<Class<? extends Exception>, ErrorHandler> mappers = new HashMap<>();


    private static final ErrorHandler<Exception> fallbackInternalError = (e, req) ->
            Response.internalServerError()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .body(ExceptionResponse.of(e));

    private static final ErrorHandler<HttpException> httpException = (e, req) ->
            Response.withStatus(e.exception.status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .body(ExceptionResponse.of(e));

    private static final ErrorHandler<Exception> fallbackConneg = (e, req) ->
            Response.withStatus(StatusCodes.UNSUPPORTED_MEDIA_TYPE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .body(ExceptionResponse.of(e));

    public ExceptionMapper() {
        mappers.put(HttpException.class, httpException);
        mappers.put(UnsupportedMediaType.class, fallbackConneg);
        mappers.put(Exception.class, fallbackInternalError);
        mappers.put(IOException.class, ioExceptionHandler());
        mappers.put(BodyReadException.class, bodyReadException());
    }

    private static ErrorHandler<IOException> ioExceptionHandler() {
        return (ioex, req) -> {
            try {
                //request too large
                //from UndertowMessages.MESSAGES
                if (ioex.exception.getMessage().startsWith("UT000020")) {
                    return Response.withStatus(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .body(ExceptionResponse.of(ioex));
                }
            } catch (Exception ignored) {

            }
            return fallbackInternalError.apply(new ErrorContext<>(ioex.id, ioex.exception), req);
        };
    }

    private static ErrorHandler<BodyReadException> bodyReadException() {
        return (brex, req) -> {
            try {
                if (brex.exception.getCause() == null) {
                    return fallbackInternalError.apply(new ErrorContext<>(brex.id, brex.exception), req);
                }

                //request too large
                //from UndertowMessages.MESSAGES
                if (brex.exception.getCause().getMessage().startsWith("UT000020")) {
                    return Response.withStatus(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .body(ExceptionResponse.of(new ErrorContext<>(brex.id, (Exception) brex.exception.getCause())));
                }
            } catch (Exception e) {
                return Response.internalServerError();
            }
            return Response.internalServerError();
        };
    }

    public <T extends Exception> ErrorHandler<T> getOrFallback(T ex) {
        return getOrFallback(ex, fallbackInternalError);
    }

    public <T extends Exception> Response apply(String id, T ex, Request request) {
        ErrorHandler<Exception> orFallback = getOrFallback(ex, fallbackInternalError);
        return orFallback.apply(new ErrorContext<>(id, ex), request);
    }

    public <T extends Exception> void put(Class<T> type, ErrorHandler<T> handler) {
        mappers.put(type, handler);
    }

    public <T extends Exception> ErrorHandler<T> getOrFallback(T key, ErrorHandler fallback) {
        ErrorHandler<T> errorHandler = mappers.get(key.getClass());
        if (errorHandler == null) {
            Class<? extends Throwable> mostSpecific = findMostSpecific(key.getClass());

            errorHandler = mostSpecific != null ? mappers.get(mostSpecific) : fallback;
        }
        return errorHandler;
    }

    private Class<? extends Throwable> findMostSpecific(Class<? extends Throwable> type) {
        // we'll keep a reference to the most specific one here
        Class<? extends Throwable> mostSpecific = null;
        // here we iterate over your list of types
        for (Class<? extends Throwable> tType : mappers.keySet()) {
            // well not even a subtype of tType so ignore it
            if (!tType.isAssignableFrom(type)) {
                continue;
            }

            if (mostSpecific == null || mostSpecific.isAssignableFrom(tType)) {
                mostSpecific = tType;
            }
        }
        return mostSpecific;
    }

}
