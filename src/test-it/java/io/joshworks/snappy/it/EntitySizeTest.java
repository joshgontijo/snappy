package io.joshworks.snappy.it;

import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.Json;
import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.http.MediaType;
import io.joshworks.snappy.http.Request;
import io.joshworks.snappy.http.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.maxEntitySize;
import static io.joshworks.snappy.SnappyServer.maxMultipartSize;
import static io.joshworks.snappy.SnappyServer.post;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static io.joshworks.snappy.parser.MediaTypes.consumes;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Josh Gontijo on 7/6/17.
 */
public class EntitySizeTest {

    private static final String SERVER_URL = "http://localhost:9000";

    private static final String MESSAGE = "0123456789";
    private static final long MAX_ENTITY_SIZE = MESSAGE.length() - 1;
    private static final long MULTIPART_MAX_ENTITY_SIZE = MESSAGE.length() - 2;


    @BeforeClass
    public static void setup() {

        maxEntitySize(MAX_ENTITY_SIZE);
        maxMultipartSize(MULTIPART_MAX_ENTITY_SIZE);
        post("/form", EntitySizeTest::printAndReturn, consumes(MediaType.APPLICATION_FORM_URLENCODED));
        post("/multipart", EntitySizeTest::printAndReturn, consumes(MediaType.MULTIPART_FORM_DATA));

        post("/plain", req -> {
            System.out.println(req.body().asString());
            return Response.ok();
        });

        start();
    }

    private static Response printAndReturn(Request req) {
        System.out.println(req.multiPartBody().partNames());
        return Response.ok();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void limitFormParam() {

        HttpResponse<Json> response = Unirest.post(SERVER_URL + "/form")
                .field("message", MESSAGE)
                .asJson();

        Assert.assertEquals(413, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().getObject().getLong("id") > 0); //just to cause a call to getLong
    }

    @Test
    public void withFormLimit() {

        HttpResponse<Json> response = Unirest.post(SERVER_URL + "/form")
                .field("message", MESSAGE.substring(0, MESSAGE.length() - 5))
                .asJson();

        System.out.println(response.asString());

        Assert.assertEquals(413, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().getObject().getLong("id") > 0); //just to cause a call to getLong
    }

    @Test
    public void limitPlainText() {

        HttpResponse<Json> response = Unirest.post(SERVER_URL + "/plain")
                .body(MESSAGE)
                .asJson();

        Assert.assertEquals(413, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().getObject().getLong("id") > 0); //just to cause a call to getLong
    }

}
