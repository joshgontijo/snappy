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
import io.joshworks.snappy.http.body.RequestBodyTooLargeException;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipException;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class ExceptionMapper {

    private final Map<Class<? extends Exception>, ErrorHandler> mappers = new ConcurrentHashMap<>();


    private static final String INTERNAL_ERROR_MESSAGE = "Internal Server Error";

    // Never expose internal exception class names or messages to clients. Details are logged
    // by HttpEntrypoint; the correlation id can be used to find the matching server log entry.
    private static final ErrorHandler<Exception> fallbackInternalError = (e, req) ->
            Response.internalServerError()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .body(new ExceptionResponse(e.id, INTERNAL_ERROR_MESSAGE));

    private static final ErrorHandler<RequestBodyTooLargeException> bodyTooLarge = (e, req) ->
            Response.withStatus(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
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
        mappers.put(RequestBodyTooLargeException.class, bodyTooLarge);
    }

    private static ErrorHandler<IOException> ioExceptionHandler() {
        return (ioex, req) -> {
            try {
                if (ioex.exception instanceof RequestBodyTooLargeException) {
                    return tooLarge(ioex.id, ioex.exception);
                }
                // "UT000020" is Undertow's request-too-large message code
                String msg = ioex.exception.getMessage();
                if (msg != null && (msg.contains("UT000020") || msg.contains("request entity too large"))) {
                    return tooLarge(ioex.id, ioex.exception);
                }
            } catch (Exception ignored) {
            }
            return fallbackInternalError.apply(new ErrorContext<>(ioex.id, ioex.exception), req);
        };
    }

    private static ErrorHandler<BodyReadException> bodyReadException() {
        return (brex, req) -> {
            try {
                Throwable cause = brex.exception.getCause();
                if (cause == null) {
                    return fallbackInternalError.apply(new ErrorContext<>(brex.id, brex.exception), req);
                }
                if (cause instanceof RequestBodyTooLargeException) {
                    return tooLarge(brex.id, (Exception) cause);
                }
                if (cause instanceof ZipException) {
                    return Response.badRequest()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .body(new ExceptionResponse(brex.id, "Invalid request body"));
                }
                // "UT000020" is Undertow's request-too-large message code
                String msg = cause.getMessage();
                if (msg != null && (msg.contains("UT000020") || msg.contains("request entity too large"))) {
                    return tooLarge(brex.id, (Exception) cause);
                }
            } catch (Exception e) {
                return Response.internalServerError();
            }
            return fallbackInternalError.apply(new ErrorContext<>(brex.id, brex.exception), req);
        };
    }

    private static Response tooLarge(String id, Exception exception) {
        return Response.withStatus(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .body(new ExceptionResponse(id, exception.getMessage()));
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