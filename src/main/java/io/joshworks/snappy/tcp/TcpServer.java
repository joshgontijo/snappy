package io.joshworks.snappy.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
            System.out.println("Waiting incoming requests...");
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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void onConnect(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);

        // register channel with selector for further IO
//        requests.add(channel);
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        int bufferSize = 1024;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int numRead = channel.read(buffer);

        int size = buffer.getInt();
        if(size > bufferSize) { //bigger than buffer

        } else if(size < buffer.remaining()) { //partial data, nesds to buffer and wait for next message

        }


        if (numRead == -1) {
            Socket readSocket = channel.socket();
            SocketAddress remoteAddr = readSocket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println("Got: " + new String(data));
    }

}


