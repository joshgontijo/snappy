package io.joshworks.snappy.it;

import io.joshworks.restclient.http.Unirest;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.fail;

/**
 * Created by Josh Gontijo on 7/7/17.
 */
public class ServerListenerTest {

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void onStartListener() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            onStart(latch::countDown);

            start();

            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("onStart wasn't called");
            }

        } finally {
            stop();
        }
    }

    @Test
    public void onShutdownListener() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            onShutdown(latch::countDown);

            start();
            stop();

            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("onShutdown wasn't called");
            }

        } finally {
            stop();
        }
    }

}
