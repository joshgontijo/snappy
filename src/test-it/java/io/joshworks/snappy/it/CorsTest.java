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

import io.joshworks.restclient.http.Headers;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.Response;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static io.joshworks.snappy.SnappyServer.cors;
import static io.joshworks.snappy.SnappyServer.enableTracer;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by josh on 3/10/17.
 */
public class CorsTest {

    private static final String SERVER_URL = "http://localhost:9000";

    @BeforeClass
    public static void setup() {
        get("/a", req -> Response.withBody("A").type("txt"));
        cors();
        enableTracer();

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test //only beforeAll will be applied anyway
    public void cors_accessControl() {
        HttpResponse<String> response = Unirest.options(SERVER_URL + "/a").asString();
        Assert.assertEquals(200, response.getStatus());

        Headers headers = response.getHeaders();

        List<String> allowOrigin = headers.get("Access-Control-Allow-Origin");
        assertNotNull(allowOrigin);
        assertEquals(1, allowOrigin.size());
        assertEquals("*", allowOrigin.get(0));
    }

    @Test //only beforeAll will be applied anyway
    public void cors_allowCrendetials() {
        HttpResponse<String> response = Unirest.options(SERVER_URL + "/a").asString();
        Assert.assertEquals(200, response.getStatus());

        Headers headers = response.getHeaders();

        List<String> allowCredentials = headers.get("Access-Control-Allow-Credentials");
        assertNotNull(allowCredentials);
        assertEquals(1, allowCredentials.size());
        assertTrue(Boolean.parseBoolean(allowCredentials.get(0)));
    }

    @Test //only beforeAll will be applied anyway
    public void cors_allowedMethods() {
        HttpResponse<String> response = Unirest.options(SERVER_URL + "/a").asString();
        Assert.assertEquals(200, response.getStatus());

        Headers headers = response.getHeaders();

        List<String> allowedMethods =headers.get("Access-Control-Allow-Methods");

        assertNotNull(allowedMethods);
        assertEquals(1, allowedMethods.size());
        String headerValues = allowedMethods.get(0);

        assertTrue(headerValues.contains(Methods.GET_STRING));
        assertTrue(headerValues.contains(Methods.POST_STRING));
        assertTrue(headerValues.contains(Methods.PUT_STRING));
        assertTrue(headerValues.contains(Methods.DELETE_STRING));
        assertTrue(headerValues.contains(Methods.HEAD_STRING));
        assertTrue(headerValues.contains(Methods.OPTIONS_STRING));
    }

    @Test //only beforeAll will be applied anyway
    public void cors_allowedHeaders() {
        HttpResponse<String> response = Unirest.options(SERVER_URL + "/a").asString();
        Assert.assertEquals(200, response.getStatus());

        Headers headers = response.getHeaders();

        List<String> allowedHeaders = headers.get("Access-Control-Allow-Headers");

        assertNotNull(allowedHeaders);
        assertEquals(1, allowedHeaders.size());
        String headerValues = allowedHeaders.get(0);

        assertTrue(headerValues.contains("Accept"));
        assertTrue(headerValues.contains("X-Requested-With"));
        assertTrue(headerValues.contains("Content-Type"));
        assertTrue(headerValues.contains("Origin"));
        assertTrue(headerValues.contains("Authorization"));
        assertTrue(headerValues.contains("Access-Control-Request-Method"));
        assertTrue(headerValues.contains("Access-Control-Request-Headers"));
    }

}
