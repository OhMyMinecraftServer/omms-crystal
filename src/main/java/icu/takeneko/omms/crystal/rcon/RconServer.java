package icu.takeneko.omms.crystal.rcon;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import icu.takeneko.omms.crystal.command.CommandManager;
import icu.takeneko.omms.crystal.command.CommandSource;
import icu.takeneko.omms.crystal.command.CommandSourceStack;
import icu.takeneko.omms.crystal.permission.Permission;
import icu.takeneko.omms.crystal.util.UtilKt;
import kotlin.collections.CollectionsKt;
import org.slf4j.Logger;

public class RconServer extends RconBase {
    private static final Logger LOGGER = UtilKt.createLogger("RconBase", false);
    private boolean authenticated;
    private final Socket socket;
    private final byte[] packetBuffer = new byte[1460];
    private final String password;

    RconServer(String password, Socket socket) {
        super("RCON Client " + socket.getInetAddress());
        this.socket = socket;

        try {
            this.socket.setSoTimeout(0);
        } catch (Exception var5) {
            this.running = false;
        }

        this.password = password;
    }

    public void run() {
        while(true) {
            try {
                if (this.running) {
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(this.socket.getInputStream());
                    int packetLength = bufferedInputStream.read(this.packetBuffer, 0, 1460);
                    if (10 > packetLength) {
                        return;
                    }
                    int index = 0;
                    int k = BufferHelper.getIntLE(this.packetBuffer, 0, packetLength);
                    if (k == packetLength - 4) {
                        index += 4;
                        int l = BufferHelper.getIntLE(this.packetBuffer, index, packetLength);
                        index += 4;
                        int m = BufferHelper.getIntLE(this.packetBuffer, index);
                        index += 4;
                        switch (m) {
                            case 2:
                                if (this.authenticated) {
                                    String command = BufferHelper.getString(this.packetBuffer, index, packetLength);

                                    try {
                                        this.respond(l, executeCommand(command));
                                    } catch (Exception e) {
                                        this.respond(l, "Error executing: " + command + " (" + e.getMessage() + ")");
                                    }
                                    continue;
                                }

                                this.fail();
                                continue;
                            case 3:
                                String string = BufferHelper.getString(this.packetBuffer, index, packetLength);
                                if (!string.isEmpty() && string.equals(this.password)) {
                                    this.authenticated = true;
                                    this.respond(l, 2, "");
                                    continue;
                                }
                                this.authenticated = false;
                                this.fail();
                                continue;
                            default:
                                this.respond(l, String.format(Locale.ROOT, "Unknown request %s", Integer.toHexString(m)));
                                continue;
                        }
                    }

                    return;
                }
            } catch (IOException ignored) {
            } catch (Exception e) {
                LOGGER.error("Exception whilst parsing RCON input", e);
            } finally {
                this.close();
                LOGGER.info("Thread {} shutting down", this.description);
                this.running = false;
            }

            return;
        }
    }

    private void respond(int sessionToken, int responseType, String message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1248);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        byte[] bs = message.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(Integer.reverseBytes(bs.length + 10));
        dataOutputStream.writeInt(Integer.reverseBytes(sessionToken));
        dataOutputStream.writeInt(Integer.reverseBytes(responseType));
        dataOutputStream.write(bs);
        dataOutputStream.write(0);
        dataOutputStream.write(0);
        this.socket.getOutputStream().write(byteArrayOutputStream.toByteArray());
    }

    private void fail() throws IOException {
        this.respond(-1, 2, "");
    }

    private void respond(int sessionToken, String message) throws IOException {
        int i = message.length();

        do {
            int j = Math.min(4096, i);
            this.respond(sessionToken, 0, message.substring(0, j));
            message = message.substring(j);
            i = message.length();
        } while(0 != i);

    }

    public void stop() {
        this.running = false;
        this.close();
        super.stop();
    }

    private String executeCommand(String command) {
        var src = new CommandSourceStack(CommandSource.REMOTE, null, Permission.OWNER);
        CommandManager.INSTANCE.execute(command, src);
        return CollectionsKt.joinToString(src.getFeedbackText(), "\n", "", "", Integer.MAX_VALUE, "", (it) -> it);
    }

    private void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close socket", e);
        }
    }
}
