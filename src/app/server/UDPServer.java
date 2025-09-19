package app.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.*;

public class UDPServer {
    private static final int PORT = 12345;
    private static final int BUFFER = 65507;
    private static final int CHUNK_SIZE = 1400; // tránh fragmentation qua Internet
    private static final String STORAGE_DIR = "storage";

    private static final Map<String, RandomAccessFile> openFiles = new ConcurrentHashMap<>();
    private static final Map<String, Integer> expectedChunks = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(STORAGE_DIR));
        Files.createDirectories(Paths.get("data"));
        DBHelper.init();

        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("UDP server listening on port " + PORT);

        byte[] buf = new byte[BUFFER];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            // copy payload bytes (length-limited)
            byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            InetAddress clientAddr = packet.getAddress();
            int clientPort = packet.getPort();

            int headerEnd = indexOf(data, (byte)'\n');
            if (headerEnd < 0) {
                sendAck(socket, clientAddr, clientPort, "ERR|NO_HEADER");
                continue;
            }

            String header = new String(data, 0, headerEnd, StandardCharsets.UTF_8).trim();
            String[] parts = header.split("\\|");
            String type = parts[0];

            try {
                switch (type) {
                    case "META": {
                        // META|filename|totalChunks|filesize|username
                        String filename = parts[1];
                        int totalChunks = Integer.parseInt(parts[2]);
                        long filesize = Long.parseLong(parts[3]);
                        String username = parts.length >=5 ? parts[4] : "unknown";

                        String key = username + "::" + filename;
                        Path out = Paths.get(STORAGE_DIR, System.currentTimeMillis() + "_" + sanitize(filename));
                        RandomAccessFile raf = new RandomAccessFile(out.toFile(), "rw");
                        openFiles.put(key, raf);
                        expectedChunks.put(key, totalChunks);

                        // insert metadata
                        try (Connection conn = DBHelper.getConnection();
                             PreparedStatement p = conn.prepareStatement(
                                     "INSERT INTO files(filename, stored_path, filesize, total_chunks, uploader) VALUES(?,?,?,?,?)")) {
                            p.setString(1, filename);
                            p.setString(2, out.toString());
                            p.setLong(3, filesize);
                            p.setInt(4, totalChunks);
                            p.setString(5, username);
                            p.executeUpdate();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        System.out.println("META from " + username + " -> " + filename + " chunks=" + totalChunks + " store=" + out);
                        sendAck(socket, clientAddr, clientPort, "META-ACK|" + filename);
                        break;
                    }

                    case "CHUNK": {
                        // CHUNK|seq|filename|username\n + raw bytes
                        int seq = Integer.parseInt(parts[1]);
                        String filename = parts[2];
                        String username = parts[3];
                        String key = username + "::" + filename;
                        RandomAccessFile raf = openFiles.get(key);
                        if (raf == null) {
                            sendAck(socket, clientAddr, clientPort, "ERR|NO_META|" + filename);
                        } else {
                            int chunkOffset = headerEnd + 1;
                            byte[] chunk = Arrays.copyOfRange(data, chunkOffset, data.length);
                            long offset = (long) seq * CHUNK_SIZE;
                            raf.seek(offset);
                            raf.write(chunk);
                            sendAck(socket, clientAddr, clientPort, "CHUNK-ACK|" + filename + "|" + seq);
                        }
                        break;
                    }

                    case "END": {
                        // END|filename|username
                        String filename = parts[1];
                        String username = parts[2];
                        String key = username + "::" + filename;
                        RandomAccessFile raf = openFiles.remove(key);
                        expectedChunks.remove(key);
                        if (raf != null) {
                            raf.close();
                            System.out.println("Completed: " + filename + " from " + username);
                            sendAck(socket, clientAddr, clientPort, "END-ACK|" + filename);
                        } else {
                            sendAck(socket, clientAddr, clientPort, "ERR|NO_META|" + filename);
                        }
                        break;
                    }

                    case "LOGIN": {
                        // LOGIN|username|password
                        String username = parts[1];
                        String password = parts[2];
                        boolean ok = UserDAO.login(username, password);
                        sendAck(socket, clientAddr, clientPort, ok ? "LOGIN-OK" : "LOGIN-FAIL");
                        break;
                    }

                    case "REGISTER": {
                        String username = parts[1];
                        String password = parts[2];
                        boolean ok = UserDAO.register(username, password);
                        sendAck(socket, clientAddr, clientPort, ok ? "REGISTER-OK" : "REGISTER-FAIL");
                        break;
                    }

                    default:
                        sendAck(socket, clientAddr, clientPort, "ERR|UNKNOWN");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                sendAck(socket, clientAddr, clientPort, "ERR|EX|" + ex.getMessage());
            }
        }
    }

    private static void sendAck(DatagramSocket s, InetAddress addr, int port, String msg) {
        try {
            byte[] b = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(b, b.length, addr, port);
            s.send(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int indexOf(byte[] data, byte target) {
        for (int i = 0; i < data.length; i++) if (data[i] == target) return i;
        return -1;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
}
