package io.joshworks.snappy.tcp.reactor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioClient {

    public static void main(String[] args) throws IOException {

        InetSocketAddress address = new InetSocketAddress("localhost", 9090);
        try (SocketChannel client = SocketChannel.open(address)) {

            System.out.println("Client started");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            new Thread(() -> {
                ByteBuffer readBuffer = ByteBuffer.allocate(4096);
                int packetSize = 0;
                int packetRead = 0;
                while (true) {
                    try {
                        int read = client.read(readBuffer);
                        System.out.println("READ " + read);
                        packetRead += read;
                        if (read == -1) {
                            System.out.println("Closing client");
                            client.close();
                            break;
                        }
                        if (read > 0) {
                            if (packetSize == 0) {
                                packetSize = readBuffer.getInt(0);
                            }
                            if (packetRead >= packetSize) {
                                System.out.println("Received " + read);
                                System.out.println(new String(readBuffer.array()));
                                readBuffer.compact();
                                //TODO reread buffer if not empty afterwards
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }

            }).start();


            String message;
            while (!(message = reader.readLine()).equals("exit")) {
                byte[] mBytes = message.getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + mBytes.length);
                buffer.putInt(mBytes.length);
                buffer.put(mBytes);
                buffer.flip();
                client.write(buffer);
            }
//
//            System.out.println("Response: " + new String(read.array(), 0, read.position()));

        }
    }
}