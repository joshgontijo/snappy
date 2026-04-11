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

import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by josh on 3/12/17.
 */
public class MethodNotAllowedTest {

    @BeforeClass
    public static void setup() {
        basePath("/v1");
        get("/sample", req -> Response.ok());
        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void validGet() {
        int responseStatus = Unirest.get("http://localhost:9000/v1/sample").asString().getStatus();
        assertEquals(200, responseStatus);
    }

    @Test
    public void invalidPut() {
        int responseStatus = Unirest.put("http://localhost:9000/v1/sample").asString().getStatus();
        assertEquals(405, responseStatus);
    }

    @Test
    public void invalidPost() {
        int responseStatus = Unirest.post("http://localhost:9000/v1/sample").asString().getStatus();
        assertEquals(405, responseStatus);
    }

    @Test
    public void invalidDelete() {
        int responseStatus = Unirest.delete("http://localhost:9000/v1/sample").asString().getStatus();
        assertEquals(405, responseStatus);
    }

}
