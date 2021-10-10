package io.github.pawitp.ftpmediaserver.ftp;

import io.github.pawitp.ftpmediaserver.Logger;
import io.github.pawitp.ftpmediaserver.media.FileMediaSource;
import io.github.pawitp.ftpmediaserver.media.Media;
import io.github.pawitp.ftpmediaserver.media.MediaSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handles an FTP control channel
 */
public class FtpControlThread implements Runnable {
    private final SocketChannel socketChannel;
    private SocketAddress socketAddress;
    private BufferedReader reader;
    private Writer writer;
    private ServerSocketChannel dataServerSocketChannel;
    private Thread currentDataThread;
    private FtpDataThread currentFtpDataThread;
    private MediaSource mediaSource;
    private boolean isRunning = true;
    private long restartOffset = 0;

    public FtpControlThread(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        try {
            mediaSource = new FileMediaSource();
            socketAddress = socketChannel.getRemoteAddress();
            reader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
            writer = Channels.newWriter(socketChannel, StandardCharsets.UTF_8);

            write("220 Java FTP Media Server Ready\r\n");

            while (isRunning) {
                handleCommand();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Logger.info("Closed control connection " + socketAddress);
        }
    }

    private void handleCommand() throws IOException {
        String fullCommand = reader.readLine();

        if (fullCommand != null) {
            Logger.info("Command: " + fullCommand);

            String[] commandParts = fullCommand.split(" ");
            String command = commandParts[0];
            String commandRest = fullCommand.substring(Math.min(command.length() + 1, fullCommand.length()));
            if ("FEAT".equals(command)) {
                write("211-Extensions supported\r\n MLST\r\n UTF8\r\n SIZE\r\n REST STREAM\r\n211 END\r\n");
            } else if ("USER".equals(command)) {
                write("230 User logged in\r\n");
            } else if ("EPSV ALL".equals(fullCommand)) {
                write("200 OK\r\n");
            } else if ("EPSV".equals(fullCommand)) {
                if (dataServerSocketChannel == null) {
                    dataServerSocketChannel = ServerSocketChannel.open();
                    dataServerSocketChannel.bind(new InetSocketAddress(0));
                }
                int localPort = ((InetSocketAddress) dataServerSocketChannel.getLocalAddress()).getPort();
                write("229 EPSV OK (|||" + localPort + "|)\r\n");
            } else if ("TYPE I".equals(fullCommand)) {
                write("200 Switch to \"binary\" transfer mode\r\n");
            } else if ("MLSD".equals(command)) {
                try {
                    List<Media> result = mediaSource.list(commandRest);
                    writeData(fullCommand, (ch) -> {
                        for (Media media : result) {
                            String line = "type=" + media.type + "; " + media.name + "\r\n";
                            ch.write(ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)));
                        }
                    });
                } catch (IOException e) {
                    write("550 " + e.getMessage() + "\r\n");
                }
            } else if ("SIZE".equals(command)) {
                try {
                    write("213 " + mediaSource.size(commandRest) + "\r\n");
                } catch (IOException e) {
                    write("550 " + e.getMessage() + "\r\n");
                }
            } else if ("REST".equals(command) && commandParts.length == 2) {
                restartOffset = Long.parseLong(commandParts[1]);
                write("350 Restarting at " + restartOffset + "\r\n");
            } else if ("RETR".equals(command) && commandParts.length > 1) {
                long localRestartOffset = restartOffset;
                restartOffset = 0;
                writeData(fullCommand, (ch) -> mediaSource.retrieve(commandRest, localRestartOffset, ch));
            } else if ("CWD".equals(command) && commandParts.length > 1) {
                try {
                    mediaSource.cwd(commandRest);
                    write("200 Directory changed\r\n");
                } catch (IOException e) {
                    write("550 " + e.getMessage() + "\r\n");
                }
            } else if ("ABOR".equals(command) && currentFtpDataThread != null) {
                currentFtpDataThread.abort();
                try {
                    currentDataThread.join();
                    write("226 ABOR successful\r\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isRunning = false;
                    write("421 Unknown error, closing connection\r\n");
                }
            } else if ("QUIT".equals(command)) {
                isRunning = false;
                write("221 Goodbye\r\n");
            } else {
                Logger.error("Unknown command: " + fullCommand);
                write("502 Command not allowed: " + command + "\r\n");
            }
        }
    }

    private void write(String response) throws IOException {
        writer.write(response);
        writer.flush();
    }

    private void writeData(String fullCommand, FtpDataThread.DataFunc response) throws IOException {
        SocketChannel dataSocketChannel = dataServerSocketChannel.accept();
        currentFtpDataThread = new FtpDataThread(this::write, dataSocketChannel, response);
        currentDataThread = new Thread(currentFtpDataThread);
        currentDataThread.setName("ftp-data-" + socketAddress + "-" + fullCommand);
        currentDataThread.start();
    }
}
