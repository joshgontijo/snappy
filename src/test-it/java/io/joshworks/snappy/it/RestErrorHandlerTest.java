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
import io.joshworks.restclient.http.RestClient;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.ExceptionResponse;
import io.joshworks.snappy.http.HttpException;
import io.joshworks.snappy.http.MediaType;
import io.joshworks.snappy.http.Response;
import io.undertow.util.Headers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.enableTracer;
import static io.joshworks.snappy.SnappyServer.exception;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class RestErrorHandlerTest {

    private static final String EXCEPTION_MESSAGE = "SOME ERROR OCCURRED";

    RestClient client;

    @BeforeClass
    public static void setup() {

        enableTracer();

        exception(CustomExceptionType.class, (e, req) -> Response.withStatus(405).body(ExceptionResponse.of(e)));

        get("/error1", req -> Response.ok());
        get("/original", req -> {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        });
        get("/restException", req -> {
            throw HttpException.badRequest(EXCEPTION_MESSAGE);
        });

        get("/customType", req -> {
            throw new CustomExceptionType();
        });

        get("/customHttpException", req -> {
            throw new CustomHttpException(501, "CUSTOM-HTTP-EXCEPTION-MESSAGE");
        });

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void unsupportedContentType() {
        HttpResponse<ExceptionResponse> response = Unirest.get("http://localhost:9000/error1")
                .header("Content-Type", "application/xml")
                .asObject(ExceptionResponse.class);

        assertEquals(415, response.getStatus());
        //default response type
        assertEquals(1, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).size());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).get(0));

        ExceptionResponse body = response.body();
        assertNotNull(body);
        assertNotNull(body.message);
    }

    @Test
    public void unsupportedAcceptedType() {
        HttpResponse<ExceptionResponse> response = Unirest.get("http://localhost:9000/error1")
                .header("Accept", "application/xml")
                .asObject(ExceptionResponse.class);

        assertEquals(415, response.getStatus());
        //default response type
        assertEquals(1, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).size());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).get(0));

        ExceptionResponse body = response.body();
        assertNotNull(body);
        assertNotNull(body.message);
    }

    @Test
    public void exceptionThrown() {
        try {
            HttpResponse<ExceptionResponse> response = Unirest.get("http://localhost:9000/original").asObject(ExceptionResponse.class);

            assertEquals(500, response.getStatus());
            //default response type
            assertEquals(1, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).size());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).get(0));

            ExceptionResponse body = response.body();
            assertNotNull(body);
            assertEquals(EXCEPTION_MESSAGE, body.message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void fromRestExceptionUtility() {
        HttpResponse<ExceptionResponse> response = Unirest.get("http://localhost:9000/restException").asObject(ExceptionResponse.class);

        assertEquals(400, response.getStatus());

        ExceptionResponse body = response.body();
        assertNotNull(body);
        assertEquals(EXCEPTION_MESSAGE, body.message);
    }

    @Test
    public void customExceptionType() {
        HttpResponse<ExceptionResponse> response = Unirest.get("http://localhost:9000/customType").asObject(ExceptionResponse.class);

        assertEquals(405, response.getStatus());
        ExceptionResponse body = response.body();
        assertNotNull(body);
        assertEquals(new CustomExceptionType().getLocalizedMessage(), body.message);
    }

    //FIXME - intermittent 500
    @Test
    public void customHttpException() {
        try (RestClient client = RestClient.builder().build()) {

            HttpResponse<ExceptionResponse> response = client.get("http://localhost:9000/customHttpException").asObject(ExceptionResponse.class);

            assertEquals(501, response.getStatus());
            ExceptionResponse body = response.body();
            assertNotNull(body);
            assertEquals("CUSTOM-HTTP-EXCEPTION-MESSAGE", body.message);
        }
    }

    public static class CustomExceptionType extends RuntimeException {
        @Override
        public String getMessage() {
            return "YOLO";
        }
    }

    public static class CustomHttpException extends HttpException {

        public CustomHttpException(int status, String message) {
            super(status, message);
        }
    }

}
