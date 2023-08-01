package com.nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FileChannelServerApplication {

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        Selector selector = Selector.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(8081));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功");

        while (true) {
            if (selector.select(1000) == 0) {
                continue;
            }

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    System.out.println("连接就绪" + LocalTime.now().toString());
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    FileChannel fileChannel = FileChannel.open(Paths.get("文件1.txt"), StandardOpenOption.READ);
                    fileChannel.transferTo(0, fileChannel.size(), socketChannel);
                    fileChannel.close();
                    System.out.println("连接结束" + LocalTime.now().toString());
                }
                iterator.remove();
            }
        }
    }
}
