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
import org.junit.Assert;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.basePath;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.group;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;

/**
 * Created by josh on 3/11/17.
 */
public class BasePathTest {

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }


    @Test
    public void getRequest() {
        try {
            basePath("/v1");
            get("/test", req -> Response.ok());
            start();

            Assert.assertEquals(200, Unirest.get("http://localhost:9000/v1/test").asString().getStatus());

        } finally {
            stop();
            Unirest.close();
        }
    }

    @Test
    public void withGroup() {
        try {
            basePath("/v1");
            group("/a", () -> get("/test", req -> Response.ok()));
            start();

            Assert.assertEquals(200, Unirest.get("http://localhost:9000/v1/a/test").asString().getStatus());

        } finally {
            stop();
            Unirest.close();
        }
    }

}
