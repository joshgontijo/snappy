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
import io.joshworks.snappy.http.Response;
import io.joshworks.snappy.it.util.SampleData;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static io.joshworks.snappy.SnappyServer.post;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by josh on 3/10/17.
 */
public class HttpRequestTest {

    private static final String SERVER_URL = "http://localhost:9000";

    private static final SampleData payload = new SampleData("Yolo");

    @BeforeClass
    public static void setup() {

        post("/array", req -> {
            SampleData[] sampleData = req.body().asObject(SampleData[].class);
            return Response.withBody(sampleData);
        });


        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void arrayParsing() {
        HttpResponse<SampleData[]> response = Unirest.post(SERVER_URL + "/array")
                .header("Content-Type", "application/json")
                .body(Collections.singletonList(new SampleData("Yolo")))
                .asObject(SampleData[].class);

        Assert.assertEquals(200, response.getStatus());
        SampleData[] responseBody = response.body();
        assertNotNull(responseBody);
        assertEquals(payload.value, responseBody[0].value);
    }




}
