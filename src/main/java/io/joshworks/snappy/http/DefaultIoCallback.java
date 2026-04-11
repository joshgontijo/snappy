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

package io.joshworks.snappy.http;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

import java.io.IOException;

import static io.joshworks.snappy.SnappyServer.*;

/**
 * A default callback implementation that simply ends the exchange
 *
 * @author Stuart Douglas
 * @see IoCallback#END_EXCHANGE
 */
public class DefaultIoCallback implements IoCallback {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private static final IoCallback CALLBACK = new IoCallback() {
        @Override
        public void onComplete(final HttpServerExchange exchange, final Sender sender) {
            exchange.endExchange();
        }

        @Override
        public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
            logger.error("An IOException occurred", exception);
            exchange.endExchange();
        }
    };

    public DefaultIoCallback() {

    }

    @Override
    public void onComplete(final HttpServerExchange exchange, final Sender sender) {
        sender.close(CALLBACK);
    }

    @Override
    public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
        try {
            exchange.endExchange();
        } finally {
            IoUtils.safeClose(exchange.getConnection());
        }
    }
}
