package io.joshworks.snappy.sse;

import io.joshworks.snappy.http.Request;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicLong;

public class SseContext extends Request {

    private static final Logger logger = LoggerFactory.getLogger(SseContext.class);
    private static final HttpString LAST_EVENT_ID = new HttpString("Last-Event-ID");

    final ServerSentEventConnection connection;
    private final SseBroadcaster broadcaster;
    private final long since = System.currentTimeMillis();
    private final AtomicLong sent = new AtomicLong();
    private Runnable closeTask;

    SseContext(ServerSentEventConnection connection, HttpServerExchange exchange, SseBroadcaster broadcaster) {
        super(exchange);
        this.connection = connection;
        this.broadcaster = broadcaster;
    }

    public SseContext send(String data) {
        return send(data, null);
    }

    public SseContext send(String data, ServerSentEventConnection.EventCallback callback) {
        connection.send(data, callback);
        sent.incrementAndGet();
        return this;
    }

    public SseContext send(EventData eventData) {
        return send(eventData, null);
    }

    public SseContext send(EventData eventData, ServerSentEventConnection.EventCallback callback) {
        connection.send(eventData.data, eventData.event, eventData.id, callback);
        sent.incrementAndGet();
        return this;
    }

    public SseContext broadcast(String group, String data) {
        broadcaster.broadcast(group, data);
        return this;
    }

    public SseContext broadcast(String group, EventData eventData) {
        broadcaster.broadcast(group, eventData);
        return this;
    }

    public SseContext joinGroup(String group) {
        broadcaster.joinGroup(group, this);
        return this;
    }

    public SseContext leaveGroup(String group) {
        broadcaster.leaveGroup(group, this);
        return this;
    }

    /**
     * Sets the keep alive time in milliseconds. If this is larger than zero a ':' message will be sent this often
     * (assuming there is no activity) to keep the connection alive.
     * <p>
     * The spec recommends a value of 15000 (15 seconds).
     *
     * @param keepAliveTime The time in milliseconds between keep alive messaged
     */
    public SseContext keepAlive(long keepAliveTime) {
        connection.setKeepAliveTime(keepAliveTime);
        return this;
    }

    /**
     * Sends the 'retry' message to the client, instructing it how long to wait before attempting a reconnect.
     *
     * @param retry The retry time in milliseconds
     */
    public SseContext sendRetry(long retry) {
        connection.sendRetry(retry);
        return this;
    }

    /**
     * Returns the keep alive time of this connection.
     *
     * @return The keep alive time in milliseconds
     */
    public long keepAlive() {
        return connection.getKeepAliveTime();
    }

    public String lastEventId() {
        return connection.getRequestHeaders().getLast(LAST_EVENT_ID);
    }

    /**
     * The connection time
     */
    public long since() {
        return since;
    }

    /**
     * The number of messages sent
     */
    public long sent() {
        return sent.get();
    }

    /**
     * Returns whether this channel is open, it does not guarantee immediate effect after closing a connection.
     */
    public boolean isOpen() {
        return connection.isOpen();
    }

    /**
     * Closes this channel.
     *
     * <p> After a channel is closed, any further attempt to invoke I/O
     * operations upon it will cause a {@link ClosedChannelException} to be
     * thrown.
     *
     * <p> If this channel is already closed then invoking this method has no
     * effect.
     *
     * <p> This method may be invoked at any time.  If some other thread has
     * already invoked it, however, then another invocation will block until
     * the first invocation is complete, after which it will return without
     * effect. </p>
     */
    public void forceClose() {
        try {
            connection.close();
        } catch (IOException e) {
            logger.warn("Error while closing SSE connection: {}", e.getMessage());
        }
    }

    /**
     * Execute a graceful shutdown once all data has been sent
     */
    public void close() {
        connection.shutdown();
    }

    /**
     * Connection close callback, it might not be executed if the by the time this is set the connections has already been closed
     */
    public void onClose(Runnable closeTask) {
        this.closeTask = closeTask;
    }

    void onClose() {
        if (closeTask != null) {
            closeTask.run();
        }
    }

}
