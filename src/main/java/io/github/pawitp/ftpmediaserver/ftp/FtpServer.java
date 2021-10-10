package io.github.pawitp.ftpmediaserver.ftp;

import io.github.pawitp.ftpmediaserver.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Main server class - accepts connection and spawn a new thread per connection.
 */
public class FtpServer {

    private final int port;

    public FtpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        Logger.info("Listening on port " + port);

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            Logger.info("Accepted control connection from " + socketChannel.getRemoteAddress());

            // Handle connection on new thread
            Thread thread = new Thread(new FtpControlThread(socketChannel));
            thread.setName("ftp-control-" + socketChannel.getRemoteAddress());
            thread.start();
        }
    }

}
