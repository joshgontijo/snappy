package io.joshworks.snappy.http;

import io.joshworks.snappy.parser.Parser;
import io.joshworks.snappy.parser.Parsers;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class Response {

    MediaType mediaType;
    final HeaderMap headers = new HeaderMap();
    final Map<String, Cookie> cookies = new HashMap<>();
    int status = StatusCodes.OK;

    private ResponseBody body = new EmptyBody();


    public static Response withBody(Object body) {
        Response response = new Response();
        response.body(body);
        return response;
    }

    public static Response withBody(String body) {
        Response response = new Response();
        response.body(body);
        return response;
    }

    public Response withBody(Object body, MediaType mediaType) {
        Response response = new Response();
        response.body(body, mediaType);
        return response;
    }

    public Response withBody(File file) {
        Response response = new Response();
        response.body(file);
        response.setResponseMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return response;
    }

    public Response withBody(File file, String name) {
        Response response = new Response();
        response.body(file, name);
        response.setResponseMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return response;
    }

    public Response withBody(byte[] body) {
        Response response = new Response();
        response.body(body);
        response.setResponseMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return response;
    }

    public Response withBody(ByteBuffer body) {
        Response response = new Response();
        response.body(body);
        response.setResponseMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return response;
    }

    /**
     * Transfers blocking the bytes from a given InputStream to this response. Using application/octet-stream
     *
     * @param inputStream The data to be sent
     */
    public static Response withBody(InputStream inputStream) {
        Response response = new Response();
        response.body(inputStream);
        response.setResponseMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return response;
    }

    public static Response withStatus(int status) {
        Response response = new Response();
        response.status = status;
        return response;
    }

    public static Response ok() {
        return new Response().status(StatusCodes.OK);
    }

    public static Response ok(Object body) {
        return ok().body(body).status(StatusCodes.OK);
    }

    public static Response ok(Object body, String type) {
        return ok(body).type(type);
    }

    public static Response created() {
        return new Response().status(StatusCodes.CREATED);
    }

    public static Response created(Object body) {
        return created().body(body);
    }

    public static Response created(Object body, String type) {
        return created(body).type(type);
    }

    public static Response badRequest() {
        return Response.withStatus(StatusCodes.BAD_REQUEST);
    }

    public static Response badRequest(Object body) {
        return badRequest().body(body);
    }

    public static Response badRequest(Object body, String type) {
        return badRequest().body(body).type(type);
    }

    public static Response notFound() {
        Response response = new Response();
        response.status = StatusCodes.NOT_FOUND;
        return response;
    }

    public static Response noContent() {
        Response response = new Response();
        response.status = StatusCodes.NO_CONTENT;
        return response;
    }

    public static Response internalServerError() {
        Response response = new Response();
        response.status = StatusCodes.INTERNAL_SERVER_ERROR;
        return response;
    }

    public static Response internalServerError(Exception e) {
        return internalServerError(ErrorContext.errorId(), e);
    }

    public static Response internalServerError(String errorId, Exception e) {
        return internalServerError().body(new ExceptionResponse(errorId, e == null ? "INTERNAL_SERVER_ERROR" : e.getMessage()));
    }

    public static Response unauthorized() {
        return new Response().status(StatusCodes.UNAUTHORIZED);
    }

    public static Response forbidden() {
        return new Response().status(StatusCodes.FORBIDDEN);
    }

    public static Response seeOther(URI uri) {
        requireNonNull(uri, "URI cannot be null");
        Response response = new Response();
        response.status = StatusCodes.SEE_OTHER;
        response.header(Headers.LOCATION_STRING, uri.toString());
        return response;
    }

    public static Response temporaryRedirect(URI uri) {
        requireNonNull(uri, "URI cannot be null");
        Response response = new Response();
        response.status = StatusCodes.TEMPORARY_REDIRECT;
        response.header(Headers.LOCATION_STRING, uri.toString());
        return response;
    }

    public Response header(String name, String value) {
        headers.put(HttpString.tryFromString(name), value);
        return this;
    }

    public Response header(String name, long value) {
        headers.put(HttpString.tryFromString(name), value);
        return this;
    }

    public Response cookie(Cookie cookie) {
        if (cookie != null) {
            cookies.put(cookie.getName(), cookie);
        }
        return this;
    }

    /**
     * Overrides any previous set Content-Type header value
     *
     * @param mediaType to be used in the response
     * @return The exchange
     */
    public Response type(MediaType mediaType) {
        setResponseMediaType(mediaType);
        return this;
    }

    public Response type(String mediaType) {
        setResponseMediaType(MediaType.valueOf(mediaType));
        return this;
    }

    public Response status(int status) {
        this.status = status;
        return this;
    }

    public Response body(Object response) {
        this.body = new DataBody(response);
        return this;
    }

    public Response body(String response) {
        this.body = new DataBody(response);
        return this;
    }

    public Response body(Object response, String mediaType) {
        return body(response, MediaType.valueOf(mediaType));
    }

    public Response body(Object response, MediaType mediaType) {
        type(mediaType);
        this.body = new DataBody(response);
        return this;
    }

    public Response body(File file) {
        this.body = new FileBody(file, null, new DefaultIoCallback());
        return this;
    }

    public Response body(File file, String name) {
        this.body = new FileBody(file, name, new DefaultIoCallback());
        return this;
    }

    public Response body(File file, String name, IoCallback callback) {
        this.body = new FileBody(file, name, callback);
        return this;
    }

    public Response body(InputStream is) {
        this.body = new StreamBody(is, 8196);
        return this;
    }

    public Response body(InputStream is, int bufferSize) {
        this.body = new StreamBody(is, bufferSize);
        return this;
    }

    public Response body(byte[] data) {
        return body(new ByteArrayInputStream(data));
    }

    public Response body(byte[] data, int bufferSize) {
        this.body = new StreamBody(new ByteArrayInputStream(data), bufferSize);
        return this;
    }

    public Response body(ByteBuffer data) {
        this.body = new ByteBufferBody(data);
        return this;
    }

    private void setResponseMediaTypeIfAbsent(MediaType mediaType) {
        if (mediaType == null) {
            return;
        }
        headers.put(Headers.CONTENT_TYPE, mediaType.toString());
    }

    private void setResponseMediaType(MediaType mediaType) {
        this.mediaType = requireNonNull(mediaType, "MediaType must be provided");
        headers.put(Headers.CONTENT_TYPE, mediaType.toString());
    }

    void handle(HttpServerExchange exchange) {
        ConnegHandler.NegotiatedMediaType negotiatedMediaType = exchange.getAttachment(ConnegHandler.NEGOTIATED_MEDIA_TYPE);
        MediaType negotiated = mediaType == null && negotiatedMediaType != null ? negotiatedMediaType.produces : mediaType;
        if (negotiated != null) {
            setResponseMediaType(negotiated);
        }
        for (HeaderValues header : headers) {
            exchange.getResponseHeaders().putAll(header.getHeaderName(), header);
        }

        exchange.getResponseCookies().putAll(cookies);
        exchange.setStatusCode(status);

        body.handle(exchange, this);
        if (!exchange.isComplete()) {
            exchange.endExchange();
        }
    }

    private static abstract class ResponseBody {

        abstract void handle(HttpServerExchange exchange, Response response);
    }

    private static class EmptyBody extends ResponseBody {

        @Override
        void handle(HttpServerExchange exchange, Response response) {
            //do nothing
        }
    }

    private static class DataBody extends ResponseBody {

        private final Object body;

        private DataBody(Object body) {
            this.body = body;
        }

        @Override
        void handle(HttpServerExchange exchange, Response response) {
            if (response == null || body == null) {
                return;
            }
            try {
                Parser responseParser = Parsers.getParser(response.mediaType);
                if (responseParser == null) {
                    throw new RuntimeException("Could not find Parser for type " + response.mediaType.toString());
                }
                exchange.getResponseSender().send(responseParser.writeValue(body));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class FileBody extends ResponseBody {

        private final File file;
        private final String fileName;
        private final IoCallback callback;

        private FileBody(File file, String overrideName, IoCallback callback) {
            if (file == null || !file.exists()) {
                throw new RuntimeException("Invalid file: " + file);
            }
            this.file = file;
            this.fileName = overrideName == null || overrideName.trim().isEmpty() ? file.getName() : overrideName;
            this.callback = callback;
        }

        @Override
        void handle(HttpServerExchange exchange, Response response) {
            try {
                String fileExtension = getFileExtension(file.getName());
                MediaType mimeForFile = MediaType.getMimeForFile(fileExtension);
                response.type(mimeForFile);

                HeaderValues contentDisposition = response.headers.get(Headers.CONTENT_DISPOSITION);
                if (contentDisposition == null || contentDisposition.isEmpty()) {
                    response.headers.add(Headers.CONTENT_DISPOSITION, "filename=" + fileName);
                }
                response.headers.add(Headers.CONTENT_LENGTH, file.length());

                exchange.getResponseSender().transferFrom(FileChannel.open(file.toPath()), callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String getFileExtension(String fileName) {
            String extension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
            return extension;
        }
    }

    private static class ByteBufferBody extends ResponseBody {

        private final ByteBuffer buffer;

        private ByteBufferBody(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        void handle(HttpServerExchange exchange, Response response) {
            exchange.getResponseSender().send(buffer);
        }
    }

    private static class StreamBody extends ResponseBody {

        private final InputStream is;
        private int bufferSize;

        private StreamBody(InputStream is, int bufferSize) {
            this.is = is;
            this.bufferSize = bufferSize;
        }

        @Override
        void handle(HttpServerExchange exchange, Response response) {
            try {
                OutputStream outputStream = exchange.getOutputStream();
                response.type(MediaType.APPLICATION_OCTET_STREAM);
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Error transferring data", ex);
            }
        }
    }

}
