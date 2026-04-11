package io.joshworks.snappy.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NioClient {

    public static void main(String[] args) throws Exception {
        new NioClient().startClient();
    }

    public void startClient() throws Exception {

        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 9999);
        try (SocketChannel client = SocketChannel.open(hostAddress)) {
            System.out.println("Client started");

            Scanner scanner = new Scanner(System.in);

            String message;
            while(!(message = scanner.nextLine()).equals("exit")) {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                client.write(buffer);
            }

        }

    }
}