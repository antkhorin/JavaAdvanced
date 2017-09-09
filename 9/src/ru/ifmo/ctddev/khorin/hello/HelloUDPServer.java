package ru.ifmo.ctddev.khorin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {

    private static byte[] hello;
    private Queue<ExecutorService> services = new ConcurrentLinkedDeque<>();
    private Queue<DatagramSocket> sockets = new ConcurrentLinkedDeque<>();
    private boolean close;

    public HelloUDPServer() {
        try {
            hello = "Hello, ".getBytes("UTF-8");
        } catch (Exception ignored) {

        }
    }

    public static void main(String[] args) {
        HelloUDPServer server = new HelloUDPServer();
        if (args == null || args.length != 2) {
            System.out.println("Wrong args");
        } else {
            try {
                server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (RuntimeException e) {
                System.out.println("Wrong args");
            }
        }
    }

    public void start(final int port, final int threads) {
        if (!close) {
            ExecutorService service = Executors.newFixedThreadPool(threads);
            services.add(service);
            try {
                DatagramSocket socket = new DatagramSocket(port);
                sockets.add(socket);
                socket.setSoTimeout(1000);
                for (int i = 0; i < threads; i++) {
                    service.submit(() -> {
                        try {
                            byte[] buffer = new byte[socket.getReceiveBufferSize()];
                            System.arraycopy(hello, 0, buffer, 0, hello.length);
                            DatagramPacket receivePacket = new DatagramPacket(buffer, hello.length, socket.getReceiveBufferSize() - hello.length);
                            while (!Thread.interrupted() && !close) {
                                try {
                                    socket.receive(receivePacket);
                                } catch (SocketTimeoutException e) {
                                    continue;
                                }
                                socket.send(new DatagramPacket(receivePacket.getData(), receivePacket.getLength() + hello.length, receivePacket.getSocketAddress()));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        close = true;
        services.forEach(ExecutorService::shutdown);
        sockets.forEach(DatagramSocket::close);
    }
}
