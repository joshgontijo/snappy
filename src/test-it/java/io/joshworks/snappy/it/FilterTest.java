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
import io.joshworks.snappy.http.Request;
import io.joshworks.snappy.http.Response;
import io.undertow.util.HttpString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.joshworks.snappy.SnappyServer.after;
import static io.joshworks.snappy.SnappyServer.before;
import static io.joshworks.snappy.SnappyServer.beforeAll;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by josh on 3/10/17.
 */
public class FilterTest {

    private static final String SERVER_URL = "http://localhost:9000";

    private static final AtomicReference<Request> beforeAllRef = new AtomicReference<>();

    @BeforeClass
    public static void setup() {
        get("/a", FilterTest::copyHeader);
        get("/b", FilterTest::copyHeader);
        get("/b/c", FilterTest::copyHeader);
        get("/a/c", FilterTest::copyHeader);
        get("/a/c/d", FilterTest::copyHeader);
        get("/filterAfter", req -> FilterTest.copyHeader(req).status(500).body("FOO").type(MediaType.TEXT_PLAIN_TYPE));

        beforeAll("/b", req -> req.header("FILTER-BEFORE-ALL-B", "OK"));
        beforeAll("/*", req -> {
            req.header("FILTER-BEFORE-ALL", "OK");
            beforeAllRef.set(req);
        });

        before("/a/*", req -> req.header("FILTER-1", "OK"));
        before("/a/c/*", req -> req.header("FILTER-2", "OK"));
        before("/b", req -> req.header("EXACT-FILTER", "OK"));


        after("/b", (req, res) -> res.header("FILTER-AFTER", "OK"));

        after("/filterAfter", (req, res) -> res.header("FILTER-AFTER", "OK").status(200).body("BAR"));

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Before
    public void setUp() {
        beforeAllRef.set(null);
    }

    private static Response copyHeader(Request req) {
        Response response = Response.ok();
        for (HttpString headerName : req.headers().getHeaderNames()) {
            response.header(headerName.toString(), req.header(headerName.toString()));
        }
        return response;
    }

    @Test //only beforeAll will be applied anyway
    public void noFilter() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/a").asString();
        assertEquals(200, response.getStatus());
        assertNull(response.getHeaders().get("FILTER-1"));
        assertNull(response.getHeaders().get("EXACT-FILTER"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void singleFilter() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/a/c").asString();
        assertNotNull(response.getHeaders().get("FILTER-1"));
        assertEquals(1, response.getHeaders().get("FILTER-1").size());

        assertNull(response.getHeaders().get("FILTER-2"));
        assertNull(response.getHeaders().get("EXACT-FILTER"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void multiple() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/a/c/d").asString();
        assertNotNull(response.getHeaders().get("FILTER-1"));
        assertEquals(1, response.getHeaders().get("FILTER-1").size());

        assertNotNull(response.getHeaders().get("FILTER-2"));
        assertEquals(1, response.getHeaders().get("FILTER-2").size());

        assertNull(response.getHeaders().get("EXACT-FILTER"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void exact() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/b").asString();
        assertNotNull(response.getHeaders().get("EXACT-FILTER"));
        assertEquals(1, response.getHeaders().get("EXACT-FILTER").size());

        assertNull(response.getHeaders().get("FILTER-1"));
        assertNull(response.getHeaders().get("FILTER-2"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL-B"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void exact_no_match() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/b/c").asString();
        assertNull(response.getHeaders().get("EXACT-FILTER"));
        assertNull(response.getHeaders().get("FILTER-1"));
        assertNull(response.getHeaders().get("FILTER-2"));

        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void filterAfter() throws Exception {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/b").asString();
        assertNotNull(response.getHeaders().get("EXACT-FILTER"));
        assertEquals(1, response.getHeaders().get("EXACT-FILTER").size());
        assertNull(response.getHeaders().get("FILTER-1"));
        assertNull(response.getHeaders().get("FILTER-2"));
        assertNull(response.getHeaders().get("FILTER-2"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void filter_exact_AllBefore() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/b").asString();
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL-B"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void filter_wildcard_AllBefore() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/a").asString();
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void filter_wildcard_AllBefore_nonexistentPath() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/nonexistentPath").asString();
        assertEquals(404, response.getStatus());
        assertNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

    @Test
    public void filter_wildcard_beforeAll_nonexistentPath() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/nonexistentPath").asString();
        assertEquals(404, response.getStatus());
        assertNotNull(beforeAllRef.get());
    }

    @Test
    public void filter_after_can_modify_response() {
        HttpResponse<String> response = Unirest.get(SERVER_URL + "/filterAfter").asString();
        assertEquals(200, response.getStatus());
        assertEquals("BAR", response.body());
        assertNotNull(response.getHeaders().get("FILTER-AFTER"));
        assertNotNull(response.getHeaders().get("FILTER-BEFORE-ALL"));
    }

}
