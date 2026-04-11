package io.joshworks.snappy.it;

import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.Unirest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.basicAuthSecured;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.group;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static io.joshworks.snappy.http.Response.ok;

public class SecuredTest {

    private static final String SERVER_URL = "http://localhost:9000";
    private static final String USER = "josh1";
    private static final String PASSWORD = "abc-123";

    @BeforeClass
    public static void setup() {

        basicAuthSecured("/secured/*", (user, psw) -> USER.equals(user) && PASSWORD.equals(psw));

        group("secured", () -> {
            get("users", req -> ok());
        });

        group("unsecured", () -> {
            get("users", req -> ok());
        });


        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void securedPathWithoutHeader() {
        HttpResponse<String> response = Unirest.get(SERVER_URL, "secured", "users").asString();
        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void securedPathWrongUser() {
        HttpResponse<String> response = Unirest.get(SERVER_URL, "secured", "users")
                .basicAuth("wrong-user", "wrong-psw")
                .asString();
        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void securedPathValidUser() {
        HttpResponse<String> response = Unirest.get(SERVER_URL, "secured", "users")
                .basicAuth(USER, PASSWORD)
                .asString();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void securedPathDifferentAuthType() {
        HttpResponse<String> response = Unirest.get(SERVER_URL, "secured", "users")
                .header("Authorization", "NotBasic abc-123")
                .asString();
        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void unsecuredPath() {
        HttpResponse<String> response = Unirest.get(SERVER_URL, "unsecured", "users").asString();
        Assert.assertEquals(200, response.getStatus());
    }

}
