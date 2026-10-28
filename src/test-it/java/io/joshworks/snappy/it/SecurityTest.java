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

package io.joshworks.snappy.it;

import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.MediaType;
import io.joshworks.snappy.http.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static io.joshworks.snappy.SnappyServer.cors;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.maxEntitySize;
import static io.joshworks.snappy.SnappyServer.post;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static io.joshworks.snappy.parser.MediaTypes.consumes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for the HTTP hardening changes.
 */
public class SecurityTest {

    private static final String SERVER_URL = "http://localhost:9000";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9000;
    private static final int MAX_ENTITY_SIZE = 1024 * 1024;

    @BeforeClass
    public static void setup() {
        maxEntitySize(MAX_ENTITY_SIZE);
        get("/hello", req -> Response.ok("hello").type("txt"));
        get("/host", req -> Response.ok(req.host()).type("txt"));
        get("/multipart", req -> Response.ok(String.valueOf(req.multiPartBody().partNames())).type("txt"),
                consumes(MediaType.MULTIPART_FORM_DATA));
        post("/echo", req -> Response.ok(req.body().asString()).type("txt"));
        get("/error", req -> {
            throw new RuntimeException("secret-detail");
        });
        cors();
        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void malformedHostIsRejected() throws Exception {
        String response = sendRaw("GET /host HTTP/1.1\r\nHost: evil.example:notaport\r\nConnection: close\r\n\r\n");
        assertTrue(response, response.startsWith("HTTP/1.1 400"));
    }

    @Test
    public void absoluteUriHostMismatchIsRejected() throws Exception {
        String response = sendRaw("GET http://evil.example/host HTTP/1.1\r\nHost: 127.0.0.1:9000\r\nConnection: close\r\n\r\n");
        assertTrue(response, response.startsWith("HTTP/1.1 400"));
    }

    @Test
    public void leadingSpaceHeaderIsRejected() throws Exception {
        String response = sendRaw("GET /host HTTP/1.1\r\n Host: attacker.example\r\nConnection: close\r\n\r\n");
        assertTrue(response, response.startsWith("HTTP/1.1 400"));
    }

    @Test
    public void multipartBodyIsNotParsedOnGet() throws Exception {
        String boundary = "----snappy-security-test";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"a\"\r\n\r\n"
                + "1\r\n"
                + "--" + boundary + "--\r\n";
        String request = "GET /multipart HTTP/1.1\r\n"
                + "Host: " + HOST + ":" + PORT + "\r\n"
                + "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n"
                + "Content-Length: " + body.getBytes(StandardCharsets.ISO_8859_1).length + "\r\n"
                + "Connection: close\r\n\r\n"
                + body;

        String response = sendRaw(request);
        assertTrue(response, response.contains("200 OK"));
        assertTrue(response, response.contains("[]"));
        assertFalse(response, response.contains("[a]"));
    }

    @Test
    public void gzipBombIsRejectedByDecompressedLimit() throws Exception {
        byte[] payload = new byte[2 * 1024 * 1024];
        Arrays.fill(payload, (byte) 'A');

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(payload);
        }

        HttpResponse<String> response = Unirest.post(SERVER_URL + "/echo")
                .header("Content-Type", "text/plain")
                .header("Content-Encoding", "gzip")
                .body(compressed.toByteArray())
                .asString();

        assertEquals(413, response.getStatus());
    }

    @Test
    public void corsDoesNotSendCredentialsWithWildcard() {
        HttpResponse<String> response = Unirest.options(SERVER_URL + "/hello")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .asString();

        assertEquals(200, response.getStatus());
        assertEquals(List.of("*"), response.getHeaders().get("Access-Control-Allow-Origin"));
        List<String> credentials = response.getHeaders().get("Access-Control-Allow-Credentials");
        assertTrue(credentials == null || credentials.isEmpty());
    }

    @Test
    public void unexpectedErrorsAreNotLeaked() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/error").asString();

        assertEquals(500, response.getStatus());
        assertTrue(response.body().contains("Internal Server Error"));
        assertFalse(response.body().contains("secret-detail"));
    }

    @Test
    public void serverHeaderDoesNotDiscloseVersionAndAddsNosniff() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/hello").asString();

        assertEquals(List.of("snappy"), response.getHeaders().get("Server"));
        assertEquals(List.of("nosniff"), response.getHeaders().get("X-Content-Type-Options"));
    }

    private static String sendRaw(String request) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 3000);
            socket.setSoTimeout(3000);

            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ByteArrayOutputStream response = new ByteArrayOutputStream();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                response.write(buffer, 0, read);
            }
            return response.toString(StandardCharsets.ISO_8859_1);
        }
    }
}
