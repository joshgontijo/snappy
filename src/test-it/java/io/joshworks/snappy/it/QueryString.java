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
import io.joshworks.restclient.http.Json;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;

/**
 * Created by josh on 3/10/17.
 */
public class QueryString {

    private static final String SERVER_URL = "http://localhost:9000";

    @BeforeClass
    public static void setup() {

        get("/echoQuery/{path}", req -> Response.withBody(req.queryParameters()));
        get("/echoQuery", req -> Response.withBody(req.queryParameters()));
        get("/echoPath/{path}", req -> Response.withBody(req.pathParameters()));

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }


    @Test
    public void queryParam() throws Exception {
        String value = "queryValue";
        HttpResponse<Json> response = Unirest.get(SERVER_URL + "/echoQuery")
                .queryString("q", value)
                .asJson();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(1, response.body().getObject().keySet().size());
        Assert.assertEquals(value, response.body().getObject().getJSONArray("q").get(0));
    }

    @Test
    public void queryParamWithPath() throws Exception {
        String value = "queryValue";
        HttpResponse<Json> response = Unirest.get(SERVER_URL + "/echoQuery/{path}")
                .queryString("q", value)
                .routeParam("path", "anotherValue")
                .asJson();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(1, response.body().getObject().keySet().size());
        Assert.assertEquals(value, response.body().getObject().getJSONArray("q").get(0));
    }

    @Test
    public void pathParameters() throws Exception {
        String value = "pathValue";
        HttpResponse<Json> response = Unirest.get(SERVER_URL + "/echoPath/{path}")
                .routeParam("path", value)
                .asJson();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(1, response.body().getObject().keySet().size());
        Assert.assertEquals(value, response.body().getObject().getString("path"));
    }


}
