package com.nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServerApplication {

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        Selector selector = Selector.open();

        //绑定端口
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        //设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //注册serverSocketChannel到selector,并关注OP_ACCEPT
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("服务端启动成功！");

        while (true) {
            //没有事件发生
            if (selector.select(1000) == 0) {
                continue;
            }
            //有事件发生，找到发生事件的Channel对应的SelectionKey合集
            Set<SelectionKey> selectorKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectorKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();

                // 发生 OP_ACCEPT 事件，处理连接请求
                if (selectionKey.isAcceptable()) {
                    System.out.println("收到连接请求");
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    // 将 socketChannel 也注册到 selector，关注 OP_READ事件，并给 socketChannel 关联 Buffer
                    socketChannel.register(selector, SelectionKey.OP_READ, new ReadWriteBuffer());
                }

                // 发生 OP_READ 事件，读客户端数据
                if (selectionKey.isReadable()) {
                    System.out.println("读取就绪");
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    //取出建立连接时添加的缓存
                    ReadWriteBuffer readWriteBuffer = (ReadWriteBuffer) selectionKey.attachment();
                    //读取数据
                    ByteBuffer readBuffer = readWriteBuffer.readBuffer;
                    int read;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((read = channel.read(readBuffer)) > 0) {
                        readBuffer.flip();
                        byte[] readByte = new byte[read];
                        readBuffer.get(readByte);
                        stringBuilder.append(new String(readByte));
                        readBuffer.clear();
                    }

                    System.out.println("msg form client:" + stringBuilder);
                    // 添加写事件
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    // 回复客户端
                    String result = "服务端已经收到消息了，谢谢！";
                    readWriteBuffer.writeBuffer.put(result.getBytes());
                    //读取不到数据就把channel关闭
                    if(read == -1) {
                        channel.close();
                    }
                }

                if (selectionKey.isWritable() && selectionKey.isValid()) {
                    System.out.println("写入就绪");
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    //取出buffer进行读取
                    ReadWriteBuffer readWriteBuffer = (ReadWriteBuffer) selectionKey.attachment();
                    readWriteBuffer.writeBuffer.flip();
                    if(readWriteBuffer.writeBuffer.hasRemaining()) {
                        channel.write(readWriteBuffer.writeBuffer);
                        System.out.println("写入数据成功给客户端");
                    } else {
                        // 注销写事件
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                        System.out.println("注销写事件");
                    }
                    // 将buffer中没有用到的数据进行迁移
                    readWriteBuffer.writeBuffer.compact();
                }

                // 手动从集合中移除当前的 selectionKey，防止重复处理事件
                iterator.remove();
            }
        }
    }

    static class ReadWriteBuffer {
        //处理读操作
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        //处理写操作
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    }
}
