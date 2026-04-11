package io.joshworks.snappy.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("Duplicates")
public class TcpServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final InetSocketAddress serverSocket;
    private Selector selector;
    private final AtomicBoolean stopped = new AtomicBoolean();

    private final Queue<SocketChannel> requests = new LinkedBlockingQueue<>();


    public TcpServer(String host, int port) throws IOException {
        this.serverSocket = new InetSocketAddress(host, port);
    }


    public static void main(String[] args) throws Exception {
        TcpServer server = new TcpServer("localhost", 9999);
        Thread thread = new Thread(server);
        thread.setName("accept");
        thread.start();
        thread.join();
    }

    private void startServer() {
        try {
            this.selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            // retrieve server socket and bind to port
            serverChannel.socket().bind(serverSocket);
            serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {
        startServer();

        try {
            logger.info("Waiting for incoming requests...");
            while (!stopped.get()) {
                selector.select();
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    // this is necessary to prevent the same key from coming up
                    // again the next time around.
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        onConnect(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in server event loop", e);
            throw new RuntimeException(e);
        }
    }

    public void onConnect(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        logger.info("Connected to: {}", remoteAddr);

        // register channel with selector for further IO
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        int bufferSize = 1024;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int numRead = channel.read(buffer);

        // Check for connection close before attempting to read data
        if (numRead == -1) {
            Socket readSocket = channel.socket();
            SocketAddress remoteAddr = readSocket.getRemoteSocketAddress();
            logger.info("Connection closed by client: {}", remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        // Flip buffer to prepare for reading (limit = position, position = 0)
        buffer.flip();

        // Need at least 4 bytes for the frame length header
        if (buffer.remaining() < Integer.BYTES) {
            logger.warn("Received partial frame header ({} bytes), discarding", buffer.remaining());
            return;
        }

        int size = buffer.getInt();

        if (size > bufferSize) {
            // Message is bigger than the buffer — discard and log
            logger.warn("Message size {} exceeds buffer size {}, discarding frame", size, bufferSize);
            return;
        } else if (buffer.remaining() < size) {
            // Partial data — full frame hasn't arrived yet
            // TODO: implement proper frame reassembly (buffer partial frames per channel)
            logger.warn("Partial frame: expected {} bytes, got {} bytes, discarding", size, buffer.remaining());
            return;
        }

        byte[] data = new byte[size];
        buffer.get(data);
        logger.debug("Received: {}", new String(data, StandardCharsets.UTF_8));
    }

}