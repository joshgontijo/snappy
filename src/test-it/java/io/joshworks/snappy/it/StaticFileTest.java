package io.joshworks.snappy.it;

import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.MediaType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Josh Gontijo on 7/7/17.
 */
public class StaticFileTest {

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void returnsIndexPage() {
        try {
            staticFiles("/pages");
            start();

            HttpResponse<String> response = Unirest.get("http://localhost:9000/pages").asString();
            Assert.assertEquals(200, response.getStatus());

            List<String> contentTypes = response.getHeaders().get("Content-Type");
            assertNotNull(contentTypes);
            assertTrue(contentTypes.size() >= 1);
            assertEquals(MediaType.TEXT_HTML, contentTypes.get(0));

        } finally {
            stop();
        }
    }

    @Test
    public void customFolder() {
        try {
            staticFiles("/pages", "someFolder");
            start();

            HttpResponse<String> response = Unirest.get("http://localhost:9000/pages").asString();

            Assert.assertEquals(200, response.getStatus());

            List<String> contentTypes = response.getHeaders().get("Content-Type");
            assertNotNull(contentTypes);
            assertTrue(contentTypes.size() >= 1);
            assertEquals(MediaType.TEXT_HTML, contentTypes.get(0));

        } finally {
            stop();
        }
    }
}
