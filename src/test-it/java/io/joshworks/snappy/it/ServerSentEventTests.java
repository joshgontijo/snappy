package io.joshworks.snappy.it;

import io.joshworks.restclient.http.Unirest;
import io.joshworks.stream.client.StreamClient;
import io.joshworks.stream.client.sse.SSEConnection;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.joshworks.snappy.SnappyServer.sse;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by Josh Gontijo on 7/6/17.
 */
public class ServerSentEventTests {

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
        StreamClient.shutdown();
    }

    @Test
    public void handler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        String message = "yolo";
        try {
            sse("/sse", sse -> {
                sse.send(message);
            });
            start();

            StreamClient.sse("http://localhost:9000/sse")
                    .onEvent(e -> {
                        receivedMessage.set(e.data);
                        latch.countDown();
                    }).connect();

            if (!latch.await(5, TimeUnit.SECONDS)) {
                fail("No message received");
            }
            assertEquals(message, receivedMessage.get());

        } finally {
            stop();
        }
    }

    @Test
    public void on_close_is_called_when_channel_is_closed() throws Exception {
        long keepAlive = 1000;
        CountDownLatch latch = new CountDownLatch(1);
        try {
            sse("/sse", sse -> {
                sse.keepAlive(keepAlive);
                sse.onClose(latch::countDown);
                new Thread(() -> {
                    while (sse.isOpen()) {
                        sse.send(UUID.randomUUID().toString());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            });
            start();

            SSEConnection connection = StreamClient.sse("http://localhost:9000/sse")
                    .onEvent(System.out::println)
                    .connect();
            connection.close();

            if (!latch.await(keepAlive * 3, TimeUnit.MILLISECONDS)) {
                fail("Close not called");
            }
        } finally {
            stop();
        }
    }

}
