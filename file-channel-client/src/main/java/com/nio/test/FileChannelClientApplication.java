package com.nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileChannelClientApplication {

    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8081));
        FileChannel fileChannel = FileChannel.open(Paths.get("拷贝.txt"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        fileChannel.transferFrom(socketChannel, 0, Long.MAX_VALUE);
        fileChannel.force(false);
    }
}
