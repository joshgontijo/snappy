package io.joshworks.snappy.tcp.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Handler implements Runnable {
    private static final int MAXIN = 4096;
    private static final int MAXOUT = 4096;
    final SocketChannel socket;
    final SelectionKey selectionKey;
    ByteBuffer input = ByteBuffer.allocate(MAXIN);
    ByteBuffer output = ByteBuffer.allocate(MAXOUT);

    static ExecutorService pool = Executors.newFixedThreadPool(2);
    private int read;
    private int packetSize;

    Handler(Selector selector, SocketChannel c) throws IOException {
        socket = c;
        c.configureBlocking(false);
        // Optionally try first read now
        selectionKey = socket.register(selector, 0);
        selectionKey.attach(this);
        selectionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    boolean inputIsComplete() { //TODO protocol specific
        return packetSize + Integer.BYTES >= read;
    }

    boolean outputIsComplete() { //TODO protocol specific
        return true;
    }

    public void run() { // initial state is reader
        try {
            read = socket.read(input);
            if(packetSize == 0) {
                packetSize = input.getInt(0);
            }

            System.out.println("Read " + read);
            //would end of stream only, for multiple messages: inputIsComplete() || isChunkReceived()
            //and then switch to OP_WRITE inside the Processor
            if (inputIsComplete()) {
                input.flip();
                selectionKey.attach(new Sender());
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                pool.execute(new Processor(new Sender()));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Processor implements Runnable {

        private final Sender sender;

        Processor(Sender sender) {
            this.sender = sender;
        }

        private synchronized void process() throws IOException {
            input.position(Integer.BYTES);
            String message = new String(input.array(), input.position(), packetSize, StandardCharsets.UTF_8);
            System.out.println("READ: " + message);
            input.compact();

            //reply
            byte[] mBytes = ("ECHO: " + message).getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + mBytes.length);
            buffer.putInt(mBytes.length);
            buffer.put(mBytes);
            buffer.flip();

            socket.write(buffer);
        }

        public void run() {
            try {
                process();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class Sender implements Runnable {
        public void run() {
            try {
                if(!socket.isOpen() || !socket.isConnected()) {
                    selectionKey.cancel();
                    System.err.println("Socket is closed");
                }
                socket.write(output);
                output.clear();

                packetSize = 0;
                read = 0;
                selectionKey.interestOps(SelectionKey.OP_READ);
                //TODO protocol specific, if connection needs to be opened after response, then comment it out
//                if (outputIsComplete()) {
//                    selectionKey.cancel();
//                }

            } catch (Exception e) {
                e.printStackTrace();
                selectionKey.cancel();
            }
        }
    }

}
