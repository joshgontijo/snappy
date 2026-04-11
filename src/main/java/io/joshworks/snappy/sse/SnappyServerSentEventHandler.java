/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.joshworks.snappy.sse;

import io.joshworks.snappy.http.ApplicationException;
import io.undertow.UndertowLogger;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelExceptionHandler;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.util.Map;

/**
 * @author Stuart Douglas
 * Modified version of {@link io.undertow.server.handlers.sse.ServerSentEventHandler}
 */
public class SnappyServerSentEventHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(SnappyServerSentEventHandler.class);
    private final SseHandler handler;
    //manages all the connections
    private final SseBroadcaster broadcaster;


    public SnappyServerSentEventHandler(SseHandler handler, SseBroadcaster broadcaster) {
        this.handler = handler;
        this.broadcaster = broadcaster;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream; charset=UTF-8");
        exchange.setPersistent(false);
        final StreamSinkChannel sink = exchange.getResponseChannel();
        if (!sink.flush()) {
            sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                @Override
                public void handleEvent(StreamSinkChannel channel) {
                    handleConnect(channel, exchange);
                }
            }, new ChannelExceptionHandler<StreamSinkChannel>() {
                @Override
                public void handleException(StreamSinkChannel channel, IOException exception) {
                    IoUtils.safeClose(exchange.getConnection());
                }
            }));
            sink.resumeWrites();
        } else {
            //dispatches to worker thread
            exchange.dispatch(new Runnable() {
                @Override
                public void run() {
                    handleConnect(sink, exchange);
                }
            });
        }
    }

    private void handleConnect(StreamSinkChannel channel, HttpServerExchange exchange) {
        UndertowLogger.REQUEST_LOGGER.debugf("Opened SSE connection to %s", exchange);
        final ServerSentEventConnection connection = new ServerSentEventConnection(exchange, channel);
        PathTemplateMatch pt = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);

        final SseContext context = new SseContext(connection, exchange, broadcaster);

        if (pt != null) {
            for (Map.Entry<String, String> p : pt.getParameters().entrySet()) {
                connection.setParameter(p.getKey(), p.getValue());
            }
        }
        broadcaster.add(context);
        connection.addCloseTask(new ChannelListener<ServerSentEventConnection>() {
            @Override
            public void handleEvent(ServerSentEventConnection channel) {
                broadcaster.remove(connection);
                try {
                    context.onClose();
                } catch (Exception e) {
                    logger.warn("Error while running SSE connection close listener: {}", e.getMessage());
                }
            }
        });
        handleConnected(context);
    }

    private void handleConnected(SseContext context) {
        try {
            handler.accept(context);
        } catch (ApplicationException e) {
            logger.error("Error while handling SSE connection, closing", e.original);
            context.close();
        } catch (Exception e) {
            logger.error("Unexpected internal server error while handling SSE connection, closing", e);
            context.close();
        }
    }
}
