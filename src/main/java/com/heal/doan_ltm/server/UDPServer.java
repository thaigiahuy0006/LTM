package com.heal.doan_ltm.server;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServer extends JFrame {

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lblStatus;

    // Lưu thời điểm hoạt động gần nhất của từng user (ms)
    private static final ConcurrentHashMap<String, Long> lastSeenMap = new ConcurrentHashMap<>();
    // Quá 20 giây không nhận tín hiệu từ Client sẽ tự động chuyển sang Offline
    private static final long ONLINE_TIMEOUT_MS = 20_000L;

    public UDPServer() {
        setTitle("Z-Voice P2P - MÁY CHỦ TRUNG TÂM");
        setSize(720, 460);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        lblStatus = new JLabel("Đang khởi động Máy chủ UDP trên cổng 8080...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblStatus.setForeground(new Color(0, 104, 255));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(lblStatus, BorderLayout.NORTH);

        String[] columns = {"Tài khoản (Username)", "Tên hiển thị", "Địa chỉ IP", "Cổng (Port)"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Xóa trạng thái online cũ còn tồn đọng trong DB khi vừa bật Server
        resetAllUsersOffline();

        Timer timer = new Timer(3000, e -> refreshOnlineUsers());
        timer.start();

        JPanel pnlBroadcast = new JPanel(new BorderLayout(10, 5));
        pnlBroadcast.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField txtBroadcast = new JTextField();
        txtBroadcast.setFont(new Font("SansSerif", Font.PLAIN, 15));
        JButton btnBroadcast = new JButton("Phát Thông Báo (Broadcast)");
        btnBroadcast.setBackground(new Color(231, 76, 60));
        btnBroadcast.setForeground(Color.WHITE);
        btnBroadcast.setFont(new Font("SansSerif", Font.BOLD, 14));

        pnlBroadcast.add(new JLabel("Thông báo từ Server:"), BorderLayout.WEST);
        pnlBroadcast.add(txtBroadcast, BorderLayout.CENTER);
        pnlBroadcast.add(btnBroadcast, BorderLayout.EAST);
        add(pnlBroadcast, BorderLayout.SOUTH);

        btnBroadcast.addActionListener(e -> {
            String msg = "SERVER_MSG:::" + txtBroadcast.getText();
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), 8888);
                socket.send(packet);
                txtBroadcast.setText("");
                JOptionPane.showMessageDialog(this, "Đã phát Broadcast tới tất cả Client trong mạng!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        new Thread(this::startUDPServer).start();
    }

    private static void resetAllUsersOffline() {
        lastSeenMap.clear();
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("UPDATE users SET ip = NULL, port = 0")) {
            s.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void markUserActive(String username) {
        if (username != null && !username.trim().isEmpty()) {
            lastSeenMap.put(username.trim(), System.currentTimeMillis());
        }
    }

    private static boolean isUserOnline(String username) {
        if (username == null) return false;
        Long lastSeen = lastSeenMap.get(username);
        return lastSeen != null && (System.currentTimeMillis() - lastSeen) <= ONLINE_TIMEOUT_MS;
    }

    private static void cleanupTimedOutUsers(Connection c) {
        long now = System.currentTimeMillis();
        for (String expiredUser : lastSeenMap.keySet()) {
            Long lastSeen = lastSeenMap.get(expiredUser);
            if (lastSeen != null && (now - lastSeen) > ONLINE_TIMEOUT_MS) {
                lastSeenMap.remove(expiredUser);
                try (PreparedStatement ps = c.prepareStatement("UPDATE users SET ip = NULL, port = 0 WHERE username = ?")) {
                    ps.setString(1, expiredUser);
                    ps.executeUpdate();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void refreshOnlineUsers() {
        try (Connection c = getConn()) {
            cleanupTimedOutUsers(c);

            try (PreparedStatement s = c.prepareStatement("SELECT username, fullname, ip, port FROM users WHERE port > 0 AND ip IS NOT NULL")) {
                ResultSet rs = s.executeQuery();
                tableModel.setRowCount(0);
                int count = 0;
                while (rs.next()) {
                    String uname = rs.getString("username");
                    if (!isUserOnline(uname)) {
                        setOffline(uname);
                        continue;
                    }
                    tableModel.addRow(new Object[]{
                            uname,
                            rs.getString("fullname"),
                            rs.getString("ip"),
                            rs.getInt("port")
                    });
                    count++;
                }
                lblStatus.setText("Server P2P hoạt động ở cổng 8080 | Số người đang Online: " + count);
                lblStatus.setForeground(new Color(0, 104, 255));
            }
        } catch (Exception e) {
            lblStatus.setText("Lỗi kết nối Cơ sở dữ liệu XAMPP!");
            lblStatus.setForeground(Color.RED);
        }
    }

    private void startUDPServer() {
        System.out.println("====== SERVER P2P VOICE CHAT ĐANG CHẠY TRÊN CỔNG 8080 ======");
        try (DatagramSocket serverSocket = new DatagramSocket(8080)) {
            byte[] receiveData = new byte[65507];
            while (true) {
                DatagramPacket p = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(p);
                String req = new String(p.getData(), 0, p.getLength(), "UTF-8");
                String res = "FAIL";

                String[] parts = req.split(";;;", -1);

                if (req.startsWith("SAVEMSG;;;")) {
                    String[] msgParts = req.split(";;;", 4);
                    if (msgParts.length >= 4) {
                        markUserActive(msgParts[1]);
                        saveMessage(msgParts[1], msgParts[2], msgParts[3]);
                    }
                    res = "OK";
                }
                else if (req.startsWith("SAVEGROUPMSG;;;")) {
                    String[] msgParts = req.split(";;;", 4);
                    if (msgParts.length >= 4) {
                        markUserActive(msgParts[2]);
                        saveGroupMessage(Integer.parseInt(msgParts[1]), msgParts[2], msgParts[3]);
                    }
                    res = "OK";
                }
                else if (req.startsWith("RECALLMSG;;;")) {
                    String[] msgParts = req.split(";;;", 4);
                    if (msgParts.length >= 4) {
                        markUserActive(msgParts[1]);
                        recallMessage(serverSocket, msgParts[1], msgParts[2], msgParts[3]);
                    }
                    res = "OK";
                }
                else if (req.startsWith("RECALLGROUPMSG;;;")) {
                    String[] msgParts = req.split(";;;", 4);
                    if (msgParts.length >= 4) {
                        markUserActive(msgParts[2]);
                        recallGroupMessage(serverSocket, Integer.parseInt(msgParts[1]), msgParts[2], msgParts[3]);
                    }
                    res = "OK";
                }
                else if (req.startsWith("ADDGROUPMEMBER;;;")) {
                    String[] memParts = req.split(";;;", 3);
                    if (memParts.length >= 3) {
                        addGroupMembers(serverSocket, Integer.parseInt(memParts[1]), memParts[2]);
                    }
                    res = "OK";
                }
                else if (req.startsWith("LEAVEGROUP;;;")) {
                    String[] leaveParts = req.split(";;;", 3);
                    if (leaveParts.length >= 3) {
                        int groupId = Integer.parseInt(leaveParts[1]);
                        String leavingUser = leaveParts[2].trim();
                        leaveGroup(serverSocket, groupId, leavingUser);
                    }
                    res = "OK";
                }
                else if (parts[0].equals("GETGROUPMSG") && parts.length >= 2) {
                    res = getGroupMessages(Integer.parseInt(parts[1]));
                }
                else if (parts[0].equals("CREATEGROUP") && parts.length >= 4) {
                    markUserActive(parts[2]);
                    res = createGroup(parts[1], parts[2], parts[3]);
                }
                else if (parts[0].equals("GETMSG") && parts.length >= 3) {
                    markUserActive(parts[1]);
                    res = getMessages(parts[1], parts[2]);
                }
                else if (parts[0].equals("LOGIN") && parts.length >= 3) {
                    res = login(parts[1], parts[2]);
                }
                else if (parts[0].equals("REGISTER") && parts.length >= 7) {
                    res = register(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
                }
                else if (parts[0].equals("UPDATE") && parts.length >= 4) {
                    markUserActive(parts[1]);
                    res = update(parts[1], parts[2], parts[3]);
                }
                else if (parts[0].equals("ONLINE") && parts.length >= 2) {
                    updateOnline(parts[1], p.getAddress().getHostAddress(), p.getPort());
                    res = "OK";
                }
                else if (parts[0].equals("OFFLINE") && parts.length >= 2) {
                    setOffline(parts[1]);
                    res = "OK";
                }
                else if (parts[0].equals("SEARCH") && parts.length >= 2) {
                    res = search(parts[1]);
                }
                else if (parts[0].equals("ADD") && parts.length >= 3) {
                    markUserActive(parts[1]);
                    res = addFriend(parts[1], parts[2]);
                }
                else if (parts[0].equals("ACCEPT") && parts.length >= 3) {
                    markUserActive(parts[2]);
                    res = acceptFriend(parts[1], parts[2]);
                }
                else if (parts[0].equals("DECLINE") && parts.length >= 3) {
                    res = declineFriend(parts[1], parts[2]);
                }
                else if (parts[0].equals("UNFRIEND") && parts.length >= 3) {
                    res = unfriend(parts[1], parts[2]);
                }
                else if (parts[0].equals("GETDATA") && parts.length >= 2) {
                    updateOnline(parts[1], p.getAddress().getHostAddress(), p.getPort());
                    res = getData(parts[1]);
                }

                if (!res.equals("OK")) {
                    byte[] sendData = res.getBytes("UTF-8");
                    serverSocket.send(new DatagramPacket(sendData, sendData.length, p.getAddress(), p.getPort()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi không thể mở cổng 8080!", "Lỗi Server", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Connection getConn() throws Exception {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p_chat?characterEncoding=UTF8", "root", "");
    }

    private static void addGroupMembers(DatagramSocket socket, int groupId, String membersCsv) {
        if (membersCsv == null || membersCsv.trim().isEmpty()) return;
        try (Connection c = getConn()) {
            String insertSql = "INSERT IGNORE INTO group_members(group_id, username) VALUES(?, ?)";
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                for (String m : membersCsv.split(",")) {
                    String cleanUser = m.trim();
                    if (!cleanUser.isEmpty()) {
                        markUserActive(cleanUser);
                        ps.setInt(1, groupId);
                        ps.setString(2, cleanUser);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            // Báo tất cả thành viên đang online trong nhóm tải lại dữ liệu nhóm ngay lập tức
            String syncSql = "SELECT u.username, u.ip, u.port FROM users u " +
                    "JOIN group_members gm ON u.username = gm.username " +
                    "WHERE gm.group_id = ? AND u.ip IS NOT NULL AND u.port > 0";
            try (PreparedStatement psSync = c.prepareStatement(syncSql)) {
                psSync.setInt(1, groupId);
                ResultSet rs = psSync.executeQuery();
                byte[] syncBytes = "FORCE_SYNC;;;".getBytes("UTF-8");
                while (rs.next()) {
                    String mUser = rs.getString("username");
                    String ip = rs.getString("ip");
                    int port = rs.getInt("port");
                    if (isUserOnline(mUser) && ip != null && port > 0) {
                        socket.send(new DatagramPacket(syncBytes, syncBytes.length, InetAddress.getByName(ip), port));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void leaveGroup(DatagramSocket socket, int groupId, String username) {
        if (username == null || username.isEmpty()) return;
        try (Connection c = getConn()) {
            // Lấy danh sách thành viên hiện tại trong nhóm trước khi xóa để báo đồng bộ cho họ
            java.util.List<String[]> targets = new java.util.ArrayList<>();
            String listSql = "SELECT u.username, u.ip, u.port FROM users u " +
                    "JOIN group_members gm ON u.username = gm.username " +
                    "WHERE gm.group_id = ? AND u.ip IS NOT NULL AND u.port > 0";
            try (PreparedStatement psList = c.prepareStatement(listSql)) {
                psList.setInt(1, groupId);
                ResultSet rs = psList.executeQuery();
                while (rs.next()) {
                    targets.add(new String[]{
                            rs.getString("username"),
                            rs.getString("ip"),
                            String.valueOf(rs.getInt("port"))
                    });
                }
            }

            // Xóa thành viên rời nhóm khỏi bảng group_members
            try (PreparedStatement psDel = c.prepareStatement("DELETE FROM group_members WHERE group_id = ? AND username = ?")) {
                psDel.setInt(1, groupId);
                psDel.setString(2, username);
                psDel.executeUpdate();
            }

            // Nếu nhóm không còn ai thì xóa luôn nhóm và tin nhắn nhóm cho sạch DB
            int remaining = 0;
            try (PreparedStatement psCount = c.prepareStatement("SELECT COUNT(*) FROM group_members WHERE group_id = ?")) {
                psCount.setInt(1, groupId);
                ResultSet rsCount = psCount.executeQuery();
                if (rsCount.next()) remaining = rsCount.getInt(1);
            }
            if (remaining == 0) {
                try (PreparedStatement psDelMsg = c.prepareStatement("DELETE FROM group_messages WHERE group_id = ?")) {
                    psDelMsg.setInt(1, groupId);
                    psDelMsg.executeUpdate();
                }
                try (PreparedStatement psDelGrp = c.prepareStatement("DELETE FROM chat_groups WHERE id = ?")) {
                    psDelGrp.setInt(1, groupId);
                    psDelGrp.executeUpdate();
                }
            }

            // Báo FORCE_SYNC để những người còn lại cập nhật lại danh sách thành viên/nhóm ngay
            byte[] syncBytes = "FORCE_SYNC;;;".getBytes("UTF-8");
            for (String[] t : targets) {
                String mUser = t[0];
                String ip = t[1];
                int port = Integer.parseInt(t[2]);
                if (isUserOnline(mUser) && ip != null && port > 0) {
                    socket.send(new DatagramPacket(syncBytes, syncBytes.length, InetAddress.getByName(ip), port));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recallMessage(DatagramSocket socket, String sender, String receiver, String msgIdOrContent) {
        try (Connection c = getConn()) {
            String cleanId = msgIdOrContent.trim();
            if (cleanId.startsWith("[MID:")) {
                int endIdx = cleanId.indexOf(']');
                if (endIdx != -1) cleanId = cleanId.substring(5, endIdx);
            }
            String recalledValue = "[MID:" + cleanId + "][RECALLED]";

            // Cập nhật tin nhắn trong DB thành [MID:<id>][RECALLED] để khi mở lại lịch sử vẫn giữ trạng thái thu hồi
            String sqlById = "UPDATE messages SET content = ? WHERE sender = ? AND receiver = ? AND content LIKE ?";
            int updated = 0;
            try (PreparedStatement ps = c.prepareStatement(sqlById)) {
                ps.setString(1, recalledValue);
                ps.setString(2, sender);
                ps.setString(3, receiver);
                ps.setString(4, "[MID:" + cleanId + "]%");
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                String sqlFallback = "UPDATE messages SET content = ? " +
                        "WHERE id = (SELECT id FROM (SELECT id FROM messages WHERE sender=? AND receiver=? AND content=? ORDER BY id DESC LIMIT 1) AS tmp)";
                try (PreparedStatement ps = c.prepareStatement(sqlFallback)) {
                    ps.setString(1, recalledValue);
                    ps.setString(2, sender);
                    ps.setString(3, receiver);
                    ps.setString(4, msgIdOrContent);
                    ps.executeUpdate();
                }
            }

            if (isUserOnline(receiver)) {
                try (PreparedStatement psUser = c.prepareStatement("SELECT ip, port FROM users WHERE username=?")) {
                    psUser.setString(1, receiver);
                    ResultSet rs = psUser.executeQuery();
                    if (rs.next()) {
                        String ip = rs.getString("ip");
                        int port = rs.getInt("port");
                        if (ip != null && port > 0) {
                            byte[] notify = ("RECALL_NOTIFY;;;" + sender + ";;;" + cleanId).getBytes("UTF-8");
                            socket.send(new DatagramPacket(notify, notify.length, InetAddress.getByName(ip), port));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recallGroupMessage(DatagramSocket socket, int groupId, String sender, String msgIdOrContent) {
        try (Connection c = getConn()) {
            String cleanId = msgIdOrContent.trim();
            if (cleanId.startsWith("[MID:")) {
                int endIdx = cleanId.indexOf(']');
                if (endIdx != -1) cleanId = cleanId.substring(5, endIdx);
            }
            String recalledValue = "[MID:" + cleanId + "][RECALLED]";

            String sqlById = "UPDATE group_messages SET content = ? WHERE group_id = ? AND sender = ? AND content LIKE ?";
            int updated = 0;
            try (PreparedStatement ps = c.prepareStatement(sqlById)) {
                ps.setString(1, recalledValue);
                ps.setInt(2, groupId);
                ps.setString(3, sender);
                ps.setString(4, "[MID:" + cleanId + "]%");
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                String sqlFallback = "UPDATE group_messages SET content = ? " +
                        "WHERE id = (SELECT id FROM (SELECT id FROM group_messages WHERE group_id=? AND sender=? AND content=? ORDER BY id DESC LIMIT 1) AS tmp)";
                try (PreparedStatement ps = c.prepareStatement(sqlFallback)) {
                    ps.setString(1, recalledValue);
                    ps.setInt(2, groupId);
                    ps.setString(3, sender);
                    ps.setString(4, msgIdOrContent);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement psMem = c.prepareStatement(
                    "SELECT u.username, u.ip, u.port FROM users u JOIN group_members gm ON u.username = gm.username WHERE gm.group_id=? AND u.username!=?")) {
                psMem.setInt(1, groupId);
                psMem.setString(2, sender);
                ResultSet rs = psMem.executeQuery();
                byte[] notify = ("RECALL_GROUP_NOTIFY;;;" + groupId + ";;;" + sender + ";;;" + cleanId).getBytes("UTF-8");
                while (rs.next()) {
                    String mUser = rs.getString("username");
                    String ip = rs.getString("ip");
                    int port = rs.getInt("port");
                    if (isUserOnline(mUser) && ip != null && port > 0) {
                        socket.send(new DatagramPacket(notify, notify.length, InetAddress.getByName(ip), port));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String createGroup(String groupName, String creator, String membersCsv) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO chat_groups(group_name, created_by) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, groupName);
            ps.setString(2, creator);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int groupId = keys.getInt(1);
                PreparedStatement psMem = c.prepareStatement("INSERT IGNORE INTO group_members(group_id, username) VALUES(?,?)");
                psMem.setInt(1, groupId);
                psMem.setString(2, creator);
                psMem.executeUpdate();

                if (!membersCsv.isEmpty()) {
                    for (String m : membersCsv.split(",")) {
                        if (!m.trim().isEmpty()) {
                            psMem.setInt(1, groupId);
                            psMem.setString(2, m.trim());
                            psMem.executeUpdate();
                        }
                    }
                }
                return "CREATEGROUP_OK;;;" + groupId + ";;;" + groupName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "FAIL";
    }

    private static void saveGroupMessage(int groupId, String sender, String content) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("INSERT INTO group_messages(group_id, sender, content) VALUES(?,?,?)")) {
            s.setInt(1, groupId);
            s.setString(2, sender);
            s.setString(3, content);
            s.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static String getGroupMessages(int groupId) {
        StringBuilder sb = new StringBuilder("GROUPMSGDATA;;;").append(groupId).append(";;;");
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement(
                     "SELECT gm.sender, u.fullname, gm.content FROM group_messages gm LEFT JOIN users u ON gm.sender = u.username WHERE gm.group_id=? ORDER BY gm.id ASC")) {
            s.setInt(1, groupId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String fname = rs.getString("fullname") != null ? rs.getString("fullname") : sender;
                sb.append(sender).append("^").append(fname).append("::").append(rs.getString("content")).append("||");
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static void saveMessage(String sender, String receiver, String content) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("INSERT INTO messages(sender, receiver, content) VALUES(?,?,?)")) {
            s.setString(1, sender);
            s.setString(2, receiver);
            s.setString(3, content);
            s.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static String getMessages(String u1, String u2) {
        StringBuilder sb = new StringBuilder("MSGDATA;;;");
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("SELECT sender, content FROM messages WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY id ASC")) {
            s.setString(1, u1);
            s.setString(2, u2);
            s.setString(3, u2);
            s.setString(4, u1);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("sender")).append("::").append(rs.getString("content")).append("||");
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static String login(String u, String p) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("SELECT fullname, avatar FROM users WHERE username=? AND password=?")) {
            s.setString(1, u);
            s.setString(2, p);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                markUserActive(u);
                return "SUCCESS;;;" + (rs.getString("fullname") != null ? rs.getString("fullname") : "") + ";;;" + (rs.getString("avatar") != null ? rs.getString("avatar") : "");
            }
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static String register(String u, String p, String n, String e, String ph, String g) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("INSERT INTO users(username, password, fullname, email, phone, gender) VALUES(?,?,?,?,?,?)")) {
            s.setString(1, u);
            s.setString(2, p);
            s.setString(3, n);
            s.setString(4, e);
            s.setString(5, ph);
            s.setString(6, g);
            s.executeUpdate();
            return "SUCCESS";
        } catch (Exception ex) {
            return "FAIL";
        }
    }

    private static String update(String u, String n, String a) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("UPDATE users SET fullname=?, avatar=? WHERE username=?")) {
            s.setString(1, n);
            s.setString(2, a);
            s.setString(3, u);
            s.executeUpdate();
            return "SUCCESS";
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static void updateOnline(String u, String ip, int port) {
        markUserActive(u);
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("UPDATE users SET ip=?, port=? WHERE username=?")) {
            s.setString(1, ip);
            s.setInt(2, port);
            s.setString(3, u);
            s.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static void setOffline(String u) {
        if (u != null) {
            lastSeenMap.remove(u.trim());
        }
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("UPDATE users SET ip=NULL, port=0 WHERE username=?")) {
            s.setString(1, u);
            s.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static String search(String p) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("SELECT username, fullname FROM users WHERE phone=? OR username=?")) {
            s.setString(1, p);
            s.setString(2, p);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return "SEARCHRES;;;" + rs.getString("username") + ";;;" + rs.getString("fullname");
        } catch (Exception ignored) {
        }
        return "NOTFOUND";
    }

    private static String addFriend(String u1, String u2) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("INSERT INTO friends(user1, user2, status) VALUES(?,?,0)")) {
            s.setString(1, u1);
            s.setString(2, u2);
            s.executeUpdate();
            return "ADD_OK";
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static String acceptFriend(String u1, String u2) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("UPDATE friends SET status=1 WHERE user1=? AND user2=?")) {
            s.setString(1, u1);
            s.setString(2, u2);
            s.executeUpdate();
            return "ACCEPT_OK";
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static String declineFriend(String u1, String u2) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("DELETE FROM friends WHERE user1=? AND user2=? AND status=0")) {
            s.setString(1, u1);
            s.setString(2, u2);
            s.executeUpdate();
            return "DECLINE_OK";
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static String unfriend(String u1, String u2) {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("DELETE FROM friends WHERE (user1=? AND user2=?) OR (user1=? AND user2=?)")) {
            s.setString(1, u1);
            s.setString(2, u2);
            s.setString(3, u2);
            s.setString(4, u1);
            s.executeUpdate();
            return "UNFRIEND_OK";
        } catch (Exception ignored) {
        }
        return "FAIL";
    }

    private static String getData(String u) {
        StringBuilder sb = new StringBuilder("DATA;;;");
        try (Connection c = getConn()) {
            PreparedStatement s1 = c.prepareStatement("SELECT u.username, u.fullname, u.ip, u.port, u.avatar FROM users u JOIN friends f ON (f.user1=u.username OR f.user2=u.username) WHERE (f.user1=? OR f.user2=?) AND f.status=1 AND u.username!=?");
            s1.setString(1, u);
            s1.setString(2, u);
            s1.setString(3, u);
            ResultSet r1 = s1.executeQuery();
            while (r1.next()) {
                String fUser = r1.getString(1);
                boolean online = isUserOnline(fUser);
                String fIp = online ? r1.getString(3) : "null";
                int fPort = online ? r1.getInt(4) : 0;

                String avtStr = r1.getString("avatar");
                if (avtStr == null || avtStr.isEmpty()) avtStr = "null";
                sb.append(fUser).append(":")
                        .append(r1.getString(2)).append(":")
                        .append(fIp).append(":")
                        .append(fPort).append(":")
                        .append(avtStr).append("|");
            }
            sb.append("###");

            PreparedStatement s2 = c.prepareStatement("SELECT u.username, u.fullname FROM users u JOIN friends f ON f.user1=u.username WHERE f.user2=? AND f.status=0");
            s2.setString(1, u);
            ResultSet r2 = s2.executeQuery();
            while (r2.next()) sb.append(r2.getString(1)).append(":").append(r2.getString(2)).append("|");
            sb.append("###");

            PreparedStatement s3 = c.prepareStatement("SELECT g.id, g.group_name FROM chat_groups g JOIN group_members gm ON g.id = gm.group_id WHERE gm.username=? ORDER BY g.id DESC");
            s3.setString(1, u);
            ResultSet r3 = s3.executeQuery();
            while (r3.next()) {
                int gid = r3.getInt("id");
                String gname = r3.getString("group_name");
                sb.append(gid).append("~").append(gname).append("~");

                PreparedStatement sMem = c.prepareStatement("SELECT u.username, u.fullname, u.ip, u.port FROM users u JOIN group_members gm ON u.username = gm.username WHERE gm.group_id=?");
                sMem.setInt(1, gid);
                ResultSet rMem = sMem.executeQuery();
                while (rMem.next()) {
                    String mUser = rMem.getString(1);
                    boolean online = isUserOnline(mUser);
                    String mIp = online ? rMem.getString(3) : "null";
                    int mPort = online ? rMem.getInt(4) : 0;

                    sb.append(mUser).append("^")
                            .append(rMem.getString(2)).append("^")
                            .append(mIp).append("^")
                            .append(mPort).append(",");
                }
                sb.append("|");
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new UDPServer().setVisible(true));
    }
}