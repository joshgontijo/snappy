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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.group;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;

/**
 * Created by Josh Gontijo on 3/17/17.
 */
public class GroupTest {

    private static final String basePath = "http://localhost:9000";

    @BeforeClass
    public static void setup() {

        get("/a", req -> Response.withBody(req.path()).type("txt"));
        group("/groupA", () -> {
            get("/", req -> Response.withBody(req.path()).type("txt"));
            get("/b", req -> Response.withBody(req.path()).type("txt"));
            get("/c", req -> Response.withBody(req.path()).type("txt"));

            group("/groupB", () -> {
                get("/d", req -> Response.withBody(req.path()).type("txt"));
            });

            group("/{param}", () -> {
                get("/e", req -> Response.withBody(req.path()).type("txt"));
            });
        });

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void noGroup() throws Exception {
        assertPath("/a");
    }

    @Test
    public void withRootPath() throws Exception {
        assertPath("/groupA");
    }

    @Test
    public void firstLevelGroup() throws Exception {
        assertPath("/groupA/b");
    }

    @Test
    public void multipleNestedGroups() throws Exception {
        assertPath("/groupA/groupB/d");
    }

    @Test
    public void withGroupParam() throws Exception {
        assertPath("/groupA/YOLO/e");
    }

    private void assertPath(String path) throws Exception {
        HttpResponse<String> response = Unirest.get(basePath + path).asString();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(path, response.body());
    }


}
