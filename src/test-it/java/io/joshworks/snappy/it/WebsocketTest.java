package io.joshworks.snappy.it;

import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.websocket.WebsocketEndpoint;
import io.joshworks.stream.client.StreamClient;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static io.joshworks.snappy.SnappyServer.websocket;
import static org.junit.Assert.fail;

/**
 * Created by Josh Gontijo on 7/6/17.
 */
public class WebsocketTest {

    @BeforeClass
    public static void setup() throws IOException {

        websocket("/echo1", (c, m) -> WebSockets.sendText(m.getData(), c, null));
        websocket("/echo2", new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                WebSockets.sendText(message.getData(), channel, null);
            }
        });

        websocket("/echo3", new WebsocketEndpoint() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

            }

            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                WebSockets.sendText(message.getData(), channel, null);
            }
        });

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
        StreamClient.shutdown();
    }

    @Test
    public void wsEcho_endpoint1() throws Exception {
        sendRequest("/echo1");
    }

    @Test
    public void wsEcho_endpoint2() throws Exception {
        sendRequest("/echo2");
    }

    @Test
    public void wsEcho_endpoint3() throws Exception {
        sendRequest("/echo3");
    }

    private void sendRequest(String endpoint) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamClient.ws("ws://localhost:9000" + endpoint)
                .onConnect(channel -> WebSockets.sendText("yolo", channel, null))
                .onText((channel, message) -> latch.countDown())
                .onError((channel, e) -> e.printStackTrace())
                .connect();


        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Message not received");
        }
    }

}
