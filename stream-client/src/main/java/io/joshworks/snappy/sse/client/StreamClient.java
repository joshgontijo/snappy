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

package io.joshworks.snappy.sse.client;

import io.joshworks.snappy.sse.client.sse.EventData;
import io.joshworks.snappy.sse.client.sse.SSEConnection;
import io.joshworks.snappy.sse.client.sse.SseClientCallback;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Created by Josh Gontijo on 4/2/17.
 */
public final class StreamClient {

    private static final Logger logger = LoggerFactory.getLogger(StreamClient.class);

    private static final String CLIENT_WORKER_NAME = "client-worker";

    private static OptionMap options = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 5)
            .set(Options.TCP_NODELAY, true)
            .set(Options.WORKER_NAME, CLIENT_WORKER_NAME)
            .set(Options.KEEP_ALIVE, true)
            .getMap();

    private static StreamClient INSTANCE;

    private final XnioWorker worker;


    private StreamClient(XnioWorker worker) {
        this.worker = worker;
    }

    public static void configure(OptionMap options) {
        if (INSTANCE != null) {
            logger.warn("StreamClient already in use, configuration will have no effect");
            return;
        }
        StreamClient.options = options;
    }

    public synchronized static void close() {
        if (INSTANCE != null) {
            logger.info("Shutting down StreamClient workers");
            INSTANCE.worker.shutdownNow();
            INSTANCE = null;
        }
    }

    private static XnioWorker getWorker() {
        if (INSTANCE == null) {
            synchronized (StreamClient.class) {
                if (INSTANCE == null) {
                    XnioWorker workers = createWorkers();
                    INSTANCE = new StreamClient(workers);
                }
            }
        }
        return INSTANCE.worker;
    }

    private static XnioWorker createWorkers() {
        try {
            return Xnio.getInstance().createWorker(options);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static WebSocketChannel connectWS(String url, ChannelListener<? super WebSocketChannel> clientEndpoint) {
        try {
            WebSocketChannel webSocketChannel = new WebSocketClient.ConnectionBuilder(
                    getWorker(),
                    new DefaultByteBufferPool(false, 2048),
                    URI.create(url))
                    .connect().get();


            webSocketChannel.getReceiveSetter().set(clientEndpoint);
            webSocketChannel.resumeReceives();
            return webSocketChannel;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SSEConnection connect(String url, Consumer<EventData> callback) {
        return connect(url, new SseClientCallback() {
            @Override
            public void onEvent(EventData data) {
                callback.accept(data);
            }
        }, null);
    }

    public static SSEConnection connect(String url, Consumer<EventData> callback, String lastEventId) {
        return connect(url, new SseClientCallback() {
            @Override
            public void onEvent(EventData data) {
                callback.accept(data);
            }
        }, lastEventId);
    }

    public static SSEConnection connect(String url, SseClientCallback callback) {
        return connect(url, callback, null);
    }

    public static SSEConnection connect(String url, SseClientCallback callback, String lastEventId) {
        SSEConnection connection = new SSEConnection(url, callback, getWorker());
        connection.connect(lastEventId);
        return connection;
    }

}