package icu.takeneko.omms.crystal.rcon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import icu.takeneko.omms.crystal.config.Config;
import icu.takeneko.omms.crystal.util.UtilKt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class RconListener extends RconBase {
    private static final Logger logger = UtilKt.createLogger("RconListener", false);
    private final ServerSocket listener;
    private final String password;
    private final List<RconServer> clients = new ArrayList<>();

    private RconListener(ServerSocket listener, String password) {
        super("RCON Listener");
        this.listener = listener;
        this.password = password;
    }

    private void removeStoppedClients() {
        this.clients.removeIf((client) -> !client.isRunning());
    }

    public void run() {
        try {
            while(this.running) {
                try {
                    Socket socket = this.listener.accept();
                    RconServer rconServer = new RconServer(this.password, socket);
                    rconServer.start();
                    this.clients.add(rconServer);
                    this.removeStoppedClients();
                } catch (SocketTimeoutException var7) {
                    this.removeStoppedClients();
                } catch (IOException var8) {
                    if (this.running) {
                        logger.info("IO exception: ", var8);
                    }
                }
            }
        } finally {
            this.closeSocket(this.listener);
        }

    }

    @Nullable
    public static RconListener create() {
        String hostname = "0.0.0.0";
        int rconPort = Config.config.getRconServerPort();//rcon port
        if (0 < rconPort && 65535 >= rconPort) {
            String rconPassword = Config.config.getRconServerPassword();
            if (rconPassword.isEmpty()) {
                logger.warn("No rcon password set in server.properties, rcon disabled!");
                return null;
            } else {
                try {
                    ServerSocket serverSocket = new ServerSocket(rconPort, 0, InetAddress.getByName(hostname));
                    serverSocket.setSoTimeout(500);
                    RconListener rconListener = new RconListener(serverSocket, rconPassword);
                    if (!rconListener.start()) {
                        return null;
                    } else {
                        logger.info("RCON running on {}:{}", hostname, rconPort);
                        return rconListener;
                    }
                } catch (IOException e) {
                    logger.warn("Unable to initialise RCON on {}:{}", hostname, rconPort, e);
                    return null;
                }
            }
        } else {
            logger.warn("Invalid rcon port {} found in server.properties, rcon disabled!", rconPort);
            return null;
        }
    }

    public void stop() {
        this.running = false;
        this.closeSocket(this.listener);
        super.stop();
        for (RconServer client : clients) {
            if (client.isRunning()) {
                client.stop();
            }
        }
        this.clients.clear();
    }

    private void closeSocket(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            logger.warn("Failed to close socket", e);
        }

    }
}
