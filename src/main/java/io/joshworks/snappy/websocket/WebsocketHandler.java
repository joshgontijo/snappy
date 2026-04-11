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

package io.joshworks.snappy.websocket;

import io.joshworks.snappy.http.InterceptorHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;

/**
 * Created by Josh Gontijo on 3/19/17.
 */
public class WebsocketHandler implements HttpHandler {

    private WebSocketProtocolHandshakeHandler websocket;

    public WebsocketHandler(AbstractReceiveListener endpoint, InterceptorHandler interceptorHandler) {
        websocket = Handlers.websocket((exchange, channel) -> {
            channel.getReceiveSetter().set(endpoint);
            channel.resumeReceives();
        });
    }

    public WebsocketHandler(WebSocketConnectionCallback connectionCallback) {
        websocket = Handlers.websocket(connectionCallback);
    }

    public WebsocketHandler(WebsocketEndpoint websocketEndpoint) {
        websocket = Handlers.websocket((exchange, channel) -> {
            websocketEndpoint.onConnect(exchange, channel);

            channel.getReceiveSetter().set(websocketEndpoint);
            channel.resumeReceives();
        });
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        websocket.handleRequest(exchange);
    }
}
