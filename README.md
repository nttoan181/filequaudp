# 🎓 Faculty of Information Technology (DaiNam University)

## 📂 TRUYỀN FILE QUA UDP VỚI ĐĂNG NHẬP VÀ LƯU CSDL SQLITE

<div align="center">
    <p>
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="FITDNU Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)  
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)  
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

---

## 📖 1. Giới thiệu

**Đề tài**: Truyền file qua giao thức UDP sử dụng Java Swing, có đăng nhập/đăng ký người dùng và lưu trữ thông tin bằng SQLite.  

**Mục tiêu**:  
- Xây dựng ứng dụng Client–Server truyền file qua UDP.  
- Người dùng phải **đăng nhập/đăng ký** trước khi gửi hoặc tải file.  
- Server nhận file và lưu vào **thư mục lưu trữ** đồng thời ghi log vào **database SQLite**.  

Ứng dụng mô phỏng giúp sinh viên hiểu rõ:  
- Cách sử dụng UDP trong Java.  
- Quản lý dữ liệu người dùng và file bằng cơ sở dữ liệu.  
- Kết hợp giao diện Swing + JDBC SQLite.  

---

## 📌 2. Công nghệ sử dụng

### 2.1. Java
- Ngôn ngữ hướng đối tượng, đa nền tảng.  
- Hỗ trợ mạnh về Socket, I/O, giao diện Swing.  

### 2.2. UDP Socket
- `DatagramSocket` và `DatagramPacket`.  
- Client chia nhỏ file thành nhiều gói tin, gửi cho Server.  
- Server ghép gói, lưu file đầy đủ.  

### 2.3. SQLite + JDBC
- CSDL nhẹ, nhúng trực tiếp vào ứng dụng.  
- Bảng `users`: quản lý tài khoản đăng nhập.  
- Bảng `files`: quản lý file gửi/nhận.  
- Truy cập qua JDBC (`DBHelper.java`, `UserDAO.java`).  

### 2.4. Mô hình Client – Server
- **Client**: giao diện Swing → chọn file → gửi qua UDP.  
- **Server**: nhận file → lưu → ghi log DB.  
- Có chức năng đăng nhập/đăng ký người dùng.  

---

## 💻 3. Các thành phần chính

- **UDPClient.java** → giao diện Swing cho người dùng (chọn file, gửi file, xem log).  
- **UDPServer.java** → chạy trên cổng UDP, nhận và lưu file.  
- **DBHelper.java** → quản lý kết nối SQLite.  
- **UserDAO.java** → xử lý đăng nhập, đăng ký.  
- **FileDAO.java** (tuỳ chọn) → quản lý log file gửi/nhận.  

📌 **Quy trình hoạt động**:  
1. Người dùng đăng nhập.  
2. Client chọn file, gửi đến Server.  
3. Server nhận gói tin, lưu file vào thư mục.  
4. Ghi log (username, filename, timestamp) vào SQLite.  

---

## ⚙️ 4. Các bước cài đặt

1. **Tạo Project**  
   - Eclipse / IntelliJ → New Java Project → UDPFileTransfer.  

2. **Thêm mã nguồn**  
   - Copy các file:  
     `UDPClient.java`, `UDPServer.java`, `DBHelper.java`, `UserDAO.java` vào `src/`.  

3. **Thêm SQLite JDBC Driver**  
   - Tải `sqlite-jdbc-x.x.x.jar`.  
   - Eclipse → Project → Build Path → Add External JARs → chọn file `.jar`.  

4. **Tạo database**  
   - Chạy `DBHelper.java` → sẽ tự tạo file `storage.db` với bảng `users`, `files`.  

5. **Chạy chương trình**  
   - Chạy `UDPServer.java` trước.  
   - Chạy `UDPClient.java` → đăng nhập → chọn file → gửi đến server.  

---


## 📞 5. Liên hệ
- 💌 Email: tattoan181@gmail.com  
- ☎️ SĐT: 0976987556  

© 2025 AIoTLab, Faculty of Information Technology, DaiNam University. All rights reserved.  
