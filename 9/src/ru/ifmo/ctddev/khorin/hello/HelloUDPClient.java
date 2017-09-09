package ru.ifmo.ctddev.khorin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) throws Exception{
        HelloUDPClient client = new HelloUDPClient();
        if (args == null || args.length != 5 || args[0] == null || args[2] == null) {
            System.out.println("Wrong args");
        } else {
            try {
                client.start(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (RuntimeException e) {
                System.out.println("Wrong args");
            }
        }
    }

    public void start(final String host, final int port, final String prefix, final int requests, final int threads) {
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int threadNumber = i;
            tasks.add(() -> {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(30);
                byte[] sendBuffer = new byte[0];
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, socketAddress);
                DatagramPacket receivePacket = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                for (int j = 0; j < requests; j++) {
                    String request = prefix + threadNumber + "_" + j;
                    sendBuffer = request.getBytes("UTF-8");
                    sendPacket.setData(sendBuffer);
                    sendPacket.setLength(sendBuffer.length);
                    int errorCount = 0;
                    while (!Thread.interrupted()) {
                        try {
                            socket.send(sendPacket);
                            socket.receive(receivePacket);
                            if (!new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), "UTF-8").equals("Hello, " + request)) {
                                continue;
                            }
                            break;
                        } catch (SocketTimeoutException e) {
                            if (errorCount < 5) {
                                errorCount++;
                            } else {
                                break;
                            }
                        }
                    }

                }
                socket.close();
                return null;
            });
        }
        try {
            service.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }
}
