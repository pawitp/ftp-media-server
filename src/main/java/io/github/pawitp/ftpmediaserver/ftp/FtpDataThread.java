package io.github.pawitp.ftpmediaserver.ftp;

import io.github.pawitp.ftpmediaserver.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles FTP data channel
 */
public class FtpDataThread implements Runnable {
    private final SocketChannel socketChannel;
    private final ControlWriter controlWriter;
    private final DataFunc response;
    private final AtomicBoolean isAborted = new AtomicBoolean(false);

    private SocketAddress socketAddress;
    private boolean isComplete = false;

    public interface ControlWriter {
        void write(String response) throws IOException;
    }

    public interface DataFunc {
        void doWrite(SocketChannel socketChannel) throws IOException;
    }

    public FtpDataThread(ControlWriter controlWriter, SocketChannel socketChannel, DataFunc response) {
        this.socketChannel = socketChannel;
        this.controlWriter = controlWriter;
        this.response = response;
    }

    // This is called from control thread
    public void abort() {
        isAborted.set(true);
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            try {
                socketAddress = socketChannel.getRemoteAddress();
                Logger.info("Accepted data connection from " + socketAddress);
                controlWriter.write("150 BINARY connection open\r\n");
                response.doWrite(socketChannel);
                isComplete = true;
            } finally {
                if (isComplete) {
                    controlWriter.write("226 Command completed\r\n");
                } else {
                    controlWriter.write("426 Command interrupted\r\n");
                }
            }
        } catch (IOException e) {
            // If aborted, we expect the exception
            if (!isAborted.get()) {
                // TODO: Sometimes VLC closes the stream before sending ABOR causing exception to be erroneously logged
                e.printStackTrace();
            }
        } finally {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Logger.info("Closed data connection " + socketAddress);
        }
    }
}
