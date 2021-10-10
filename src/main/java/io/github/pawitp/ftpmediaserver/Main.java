package io.github.pawitp.ftpmediaserver;

import io.github.pawitp.ftpmediaserver.ftp.FtpServer;

public class Main {

    public static void main(String[] args) throws Exception {
        Logger.info("Starting media server");

        FtpServer server = new FtpServer(2121);
        server.start();
    }

}
