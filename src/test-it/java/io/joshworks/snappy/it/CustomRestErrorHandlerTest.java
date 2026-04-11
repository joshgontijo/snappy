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
import io.undertow.util.Headers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Josh Gontijo on 3/15/17.
 */
public class CustomRestErrorHandlerTest {

    private static final CustomExceptionBody exceptionBody_1 = new CustomExceptionBody("123", 123L);
    private static final CustomExceptionBody exceptionBody_2 = new CustomExceptionBody("456", 456L);
    private static final int responseStatus_1 = 401;
    private static final int responseStatus_2 = 405;

    @BeforeClass
    public static void setup() {
        exception(Exception.class, (e, req) -> Response.withStatus(responseStatus_1).body(exceptionBody_1));
        exception(UnsupportedOperationException.class, (e, req) -> Response.withStatus(responseStatus_2).body(exceptionBody_2));
        //custom media type
        exception(NumberFormatException.class, (e, req) -> Response.withStatus(responseStatus_2).body(exceptionBody_2, MediaType.TEXT_PLAIN_TYPE));
        exception(NumberFormatException.class, (e, req) -> Response.withStatus(responseStatus_2).body(exceptionBody_2, MediaType.TEXT_PLAIN_TYPE));

        get("/custom-handler-1", req -> {
            throw new RuntimeException("Some error");
        });

        get("/custom-handler-2", req -> {
            throw new UnsupportedOperationException("Some other error");
        });

        get("/custom-handler-3-mediaType", req -> {
            throw new NumberFormatException("Some error with custom media type");
        });

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }


    @Test
    public void exceptionThrown() throws Exception {
        HttpResponse<CustomExceptionBody> response = Unirest.get("http://localhost:9000/custom-handler-1").asObject(CustomExceptionBody.class);

        Assert.assertEquals(responseStatus_1, response.getStatus());

        CustomExceptionBody body = response.body();
        assertNotNull(body);
        assertEquals(exceptionBody_1.getUuid(), body.getUuid());
        assertEquals(exceptionBody_1.getTime(), body.getTime());
    }

    @Test
    public void exceptionThrownExactMatch() throws Exception {
        HttpResponse<CustomExceptionBody> response = Unirest.get("http://localhost:9000/custom-handler-2").asObject(CustomExceptionBody.class);

        Assert.assertEquals(responseStatus_2, response.getStatus());

        CustomExceptionBody body = response.body();
        assertNotNull(body);
        assertEquals(exceptionBody_2.getUuid(), body.getUuid());
        assertEquals(exceptionBody_2.getTime(), body.getTime());
    }

    @Test
    public void exceptionProvidedMediaType() throws Exception {
        HttpResponse<String> response = Unirest.get("http://localhost:9000/custom-handler-3-mediaType")
                .asString();

        Assert.assertEquals(responseStatus_2, response.getStatus());
        //custom response type
        Assert.assertEquals(1, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).size());
        Assert.assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().get(Headers.CONTENT_TYPE.toString()).get(0));

        assertNotNull(response.body());
        assertTrue(response.body().contains(exceptionBody_2.getUuid()));
        assertTrue(response.body().contains("" + exceptionBody_2.getTime()));
    }

    private static class CustomExceptionBody {
        private String uuid;
        private long time;

        public CustomExceptionBody(String uuid, long time) {

            this.uuid = uuid;
            this.time = time;
        }


        public String getUuid() {
            return uuid;
        }

        public long getTime() {
            return time;
        }

        //this is used to compare string result from the text/plain media type
        @Override
        public String toString() {
            return "CustomExceptionBody{" +
                    "uuid='" + uuid + '\'' +
                    ", time=" + time +
                    '}';
        }
    }

}
