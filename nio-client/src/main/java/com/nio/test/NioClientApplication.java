package com.nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NioClientApplication {

    public static void main(String[] args) throws IOException {
        //单线程池
        ExecutorService single = Executors.newSingleThreadExecutor();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        Scanner scanner = new Scanner(System.in);
        socketChannel.configureBlocking(false);
        //与服务端建立连接
        socketChannel.connect(new InetSocketAddress("localhost", 8080));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        while (true) {
            if (selector.select(1000) == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                //处理连接事件
                if (selectionKey.isConnectable()) {
                    if (socketChannel.finishConnect()) {
                        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
                }
                //处理读事件
                if (selectionKey.isReadable()) {
                    readBuffer.clear();
                    int read = socketChannel.read(readBuffer);
                    byte[] readByte = new byte[read];
                    readBuffer.flip();
                    readBuffer.get(readByte);
                    System.out.println("读取到数据：" + new String(readByte));
                    //重新注册写事件
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                }
                //处理写事件
                if (selectionKey.isWritable()) {
                    //使用线程写数据到服务端
                    threadWrite(single, writeBuffer, socketChannel, scanner);
                    //取消注册写事件（不取消会造成上一次数据可能还没发送，下次进来还会继续执行
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }
            iterator.remove();
        }
    }

    static void threadWrite(ExecutorService single, ByteBuffer writeBuffer, SocketChannel socketChannel, Scanner scanner) {
        single.execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println(Thread.currentThread().getName() + "请输入：");
                String line = scanner.nextLine();
                writeBuffer.put(line.getBytes());
                writeBuffer.flip();
                //写入数据
                while (writeBuffer.hasRemaining()) {
                    socketChannel.write(writeBuffer);
                }
                writeBuffer.compact();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
