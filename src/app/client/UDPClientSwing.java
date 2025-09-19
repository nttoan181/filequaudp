package app.client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPClientSwing extends JFrame {
    private JTextArea logArea;
    private JTextField txtHost, txtPort, txtUser;
    private JPasswordField txtPass;
    private JButton btnRegister, btnLogin, btnChoose, btnSend;
    private File selectedFile;
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private int serverPort = 12345;

    private static final int CHUNK_SIZE = 1400;
    private static final int TIMEOUT_MS = 5000;
    private static final int RETRIES = 4;

    public UDPClientSwing() throws Exception {
        super("📤 Ứng dụng Truyền File UDP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // căn giữa màn hình
        setLayout(new BorderLayout(10,10));

        // ====== Panel thông tin kết nối & tài khoản ======
        JPanel top = new JPanel(new GridLayout(2,4,8,8));
        top.setBorder(new TitledBorder("🔑 Thông tin kết nối & Tài khoản"));

        txtHost = new JTextField("127.0.0.1");
        txtPort = new JTextField("12345");
        txtUser = new JTextField("user1");
        txtPass = new JPasswordField("pass");

        top.add(new JLabel("Máy chủ:")); top.add(txtHost);
        top.add(new JLabel("Cổng:")); top.add(txtPort);
        top.add(new JLabel("Người dùng:")); top.add(txtUser);
        top.add(new JLabel("Mật khẩu:")); top.add(txtPass);
        add(top, BorderLayout.NORTH);

        // ====== Panel log hoạt động (ở giữa màn hình) ======
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(new TitledBorder("📜 Nhật ký hoạt động"));
        add(scroll, BorderLayout.CENTER);

        // ====== Panel nút chức năng (ở dưới cùng) ======
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottom.setBorder(new TitledBorder("⚙️ Chức năng"));

        btnRegister = new JButton("📝 Đăng ký");
        btnLogin = new JButton("🔓 Đăng nhập");
        btnChoose = new JButton("📂 Chọn file");
        btnSend = new JButton("🚀 Gửi file");
        btnSend.setEnabled(false);

        Dimension btnSize = new Dimension(140, 45);
        btnRegister.setPreferredSize(btnSize);
        btnLogin.setPreferredSize(btnSize);
        btnChoose.setPreferredSize(btnSize);
        btnSend.setPreferredSize(btnSize);

        bottom.add(btnRegister);
        bottom.add(btnLogin);
        bottom.add(btnChoose);
        bottom.add(btnSend);
        add(bottom, BorderLayout.SOUTH);

        // ====== Gắn sự kiện ======
        btnRegister.addActionListener(e -> doRegister());
        btnLogin.addActionListener(e -> doLogin());
        btnChoose.addActionListener(e -> chooseFile());
        btnSend.addActionListener(e -> {
            new Thread(() -> {
                try {
                    sendFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log("❌ Lỗi gửi file: " + ex.getMessage());
                }
            }).start();
        });

        // ====== Khởi tạo socket ======
        socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT_MS);
    }

    // ====== Các hàm xử lý ======
    private void doRegister() {
        try {
            prepareAddr();
            String username = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            String msg = "REGISTER|" + username + "|" + pass + "\n";
            sendSimple(msg.getBytes(StandardCharsets.UTF_8));
            String resp = recvSimple();
            log("📩 Phản hồi Đăng ký: " + resp);
        } catch (Exception e) {
            e.printStackTrace();
            log("❌ Đăng ký thất bại: " + e.getMessage());
        }
    }

    private void doLogin() {
        try {
            prepareAddr();
            String username = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            String msg = "LOGIN|" + username + "|" + pass + "\n";
            sendSimple(msg.getBytes(StandardCharsets.UTF_8));
            String resp = recvSimple();
            log("📩 Phản hồi Đăng nhập: " + resp);
            if ("LOGIN-OK".equals(resp)) {
                btnSend.setEnabled(true);
                log("✅ Đăng nhập thành công, bạn có thể gửi file.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("❌ Đăng nhập thất bại: " + e.getMessage());
        }
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            log("📂 Đã chọn file: " + selectedFile.getAbsolutePath());
        }
    }

    private void sendFile() throws Exception {
        if (selectedFile == null) { log("⚠️ Chưa chọn file nào!"); return; }
        prepareAddr();
        String username = txtUser.getText().trim();
        long filesize = selectedFile.length();
        int totalChunks = (int)((filesize + CHUNK_SIZE - 1) / CHUNK_SIZE);
        String meta = "META|" + selectedFile.getName() + "|" + totalChunks + "|" + filesize + "|" + username + "\n";

        boolean metaOk = sendWithRetry(meta.getBytes(StandardCharsets.UTF_8), "META-ACK");
        if (!metaOk) { log("❌ Gửi META thất bại."); return; }

        try (FileInputStream fis = new FileInputStream(selectedFile)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int n;
            int seq = 0;
            while ((n = fis.read(buffer)) != -1) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String header = "CHUNK|" + seq + "|" + selectedFile.getName() + "|" + username + "\n";
                baos.write(header.getBytes(StandardCharsets.UTF_8));
                baos.write(buffer, 0, n);
                byte[] packet = baos.toByteArray();

                boolean ok = false;
                for (int attempt = 0; attempt < RETRIES; attempt++) {
                    sendSimple(packet);
                    String ack = recvSimple();
                    if (ack.startsWith("CHUNK-ACK|" + selectedFile.getName() + "|" + seq)) {
                        ok = true;
                        break;
                    } else {
                        log("⚠️ Mảnh " + seq + " lần thử " + (attempt+1) + " phản hồi: " + ack);
                    }
                }
                if (!ok) {
                    log("❌ Mảnh " + seq + " thất bại sau nhiều lần thử.");
                    return;
                }
                seq++;
                log("📤 Đã gửi mảnh " + seq + "/" + totalChunks);
            }
        }

        String end = "END|" + selectedFile.getName() + "|" + username + "\n";
        boolean endOk = sendWithRetry(end.getBytes(StandardCharsets.UTF_8), "END-ACK");
        log(endOk ? "✅ File đã gửi thành công." : "❌ Kết thúc không được xác nhận.");
    }

    private boolean sendWithRetry(byte[] data, String expectPrefix) throws IOException {
        for (int i = 0; i < RETRIES; i++) {
            sendSimple(data);
            String resp = recvSimple();
            if (resp.startsWith(expectPrefix)) return true;
            log("⚠️ Thử lần " + (i+1) + " nhưng phản hồi: " + resp);
        }
        return false;
    }

    private void prepareAddr() throws UnknownHostException {
        serverAddr = InetAddress.getByName(txtHost.getText().trim());
        serverPort = Integer.parseInt(txtPort.getText().trim());
    }

    private void sendSimple(byte[] b) throws IOException {
        DatagramPacket p = new DatagramPacket(b, b.length, serverAddr, serverPort);
        socket.send(p);
    }

    private String recvSimple() {
        byte[] buf = new byte[8192];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(p);
            return new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).trim();
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (IOException e) {
            e.printStackTrace();
            return "ERR:" + e.getMessage();
        }
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> logArea.append(s + "\n"));
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                new UDPClientSwing().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
