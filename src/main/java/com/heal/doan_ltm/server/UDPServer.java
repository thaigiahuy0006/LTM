package com.heal.doan_ltm.server;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import java.sql.*;

public class UDPServer extends JFrame {

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lblStatus;

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
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        new Thread(this::startUDPServer).start();
    }

    private void refreshOnlineUsers() {
        try (Connection c = getConn();
             PreparedStatement s = c.prepareStatement("SELECT username, fullname, ip, port FROM users WHERE port > 0 AND ip IS NOT NULL")) {
            ResultSet rs = s.executeQuery();
            tableModel.setRowCount(0);
            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("username"),
                        rs.getString("fullname"),
                        rs.getString("ip"),
                        rs.getInt("port")
                });
                count++;
            }
            lblStatus.setText("Server P2P hoạt động ở cổng 8080 | Số người đang Online: " + count);
            lblStatus.setForeground(new Color(0, 104, 255));
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
                    if (msgParts.length >= 4) saveMessage(msgParts[1], msgParts[2], msgParts[3]);
                    res = "OK";
                }
                else if (req.startsWith("SAVEGROUPMSG;;;")) {
                    String[] msgParts = req.split(";;;", 4);
                    if (msgParts.length >= 4) saveGroupMessage(Integer.parseInt(msgParts[1]), msgParts[2], msgParts[3]);
                    res = "OK";
                }
                else if (parts[0].equals("GETGROUPMSG") && parts.length >= 2) {
                    res = getGroupMessages(Integer.parseInt(parts[1]));
                }
                else if (parts[0].equals("CREATEGROUP") && parts.length >= 4) {
                    res = createGroup(parts[1], parts[2], parts[3]);
                }
                else if (parts[0].equals("GETMSG") && parts.length >= 3) res = getMessages(parts[1], parts[2]);
                else if (parts[0].equals("LOGIN") && parts.length >= 3) res = login(parts[1], parts[2]);
                else if (parts[0].equals("REGISTER") && parts.length >= 7) res = register(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
                else if (parts[0].equals("UPDATE") && parts.length >= 4) res = update(parts[1], parts[2], parts[3]);
                else if (parts[0].equals("ONLINE") && parts.length >= 2) {
                    updateOnline(parts[1], p.getAddress().getHostAddress(), p.getPort());
                    res = "OK";
                }
                else if (parts[0].equals("SEARCH") && parts.length >= 2) res = search(parts[1]);
                else if (parts[0].equals("ADD") && parts.length >= 3) res = addFriend(parts[1], parts[2]);
                else if (parts[0].equals("ACCEPT") && parts.length >= 3) res = acceptFriend(parts[1], parts[2]);
                else if (parts[0].equals("DECLINE") && parts.length >= 3) res = declineFriend(parts[1], parts[2]);
                else if (parts[0].equals("UNFRIEND") && parts.length >= 3) res = unfriend(parts[1], parts[2]);
                else if (parts[0].equals("GETDATA") && parts.length >= 2) res = getData(parts[1]);

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
        } catch (Exception e) { e.printStackTrace(); }
        return "FAIL";
    }

    private static void saveGroupMessage(int groupId, String sender, String content) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("INSERT INTO group_messages(group_id, sender, content) VALUES(?,?,?)")) {
            s.setInt(1, groupId); s.setString(2, sender); s.setString(3, content);
            s.executeUpdate();
        } catch (Exception e) {}
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
        } catch (Exception e) {}
        return sb.toString();
    }

    private static void saveMessage(String sender, String receiver, String content) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("INSERT INTO messages(sender, receiver, content) VALUES(?,?,?)")) {
            s.setString(1, sender); s.setString(2, receiver); s.setString(3, content);
            s.executeUpdate();
        } catch (Exception e) {}
    }

    private static String getMessages(String u1, String u2) {
        StringBuilder sb = new StringBuilder("MSGDATA;;;");
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("SELECT sender, content FROM messages WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY id ASC")) {
            s.setString(1, u1); s.setString(2, u2); s.setString(3, u2); s.setString(4, u1);
            ResultSet rs = s.executeQuery();
            while(rs.next()) {
                sb.append(rs.getString("sender")).append("::").append(rs.getString("content")).append("||");
            }
        } catch (Exception e) {}
        return sb.toString();
    }

    private static String login(String u, String p) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("SELECT fullname, avatar FROM users WHERE username=? AND password=?")) {
            s.setString(1, u); s.setString(2, p); ResultSet rs = s.executeQuery();
            if (rs.next()) return "SUCCESS;;;" + (rs.getString("fullname")!=null?rs.getString("fullname"):"") + ";;;" + (rs.getString("avatar")!=null?rs.getString("avatar"):"");
        } catch (Exception e) {} return "FAIL";
    }

    private static String register(String u, String p, String n, String e, String ph, String g) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("INSERT INTO users(username, password, fullname, email, phone, gender) VALUES(?,?,?,?,?,?)")) {
            s.setString(1, u); s.setString(2, p); s.setString(3, n); s.setString(4, e); s.setString(5, ph); s.setString(6, g);
            s.executeUpdate(); return "SUCCESS";
        } catch (Exception ex) { return "FAIL"; }
    }

    private static String update(String u, String n, String a) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("UPDATE users SET fullname=?, avatar=? WHERE username=?")) {
            s.setString(1, n); s.setString(2, a); s.setString(3, u); s.executeUpdate(); return "SUCCESS";
        } catch (Exception e) {} return "FAIL";
    }

    private static void updateOnline(String u, String ip, int port) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("UPDATE users SET ip=?, port=? WHERE username=?")) {
            s.setString(1, ip); s.setInt(2, port); s.setString(3, u); s.executeUpdate();
        } catch (Exception e) {}
    }

    private static String search(String p) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("SELECT username, fullname FROM users WHERE phone=? OR username=?")) {
            s.setString(1, p); s.setString(2, p); ResultSet rs = s.executeQuery();
            if(rs.next()) return "SEARCHRES;;;" + rs.getString("username") + ";;;" + rs.getString("fullname");
        } catch (Exception e) {} return "NOTFOUND";
    }

    private static String addFriend(String u1, String u2) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("INSERT INTO friends(user1, user2, status) VALUES(?,?,0)")) {
            s.setString(1, u1); s.setString(2, u2); s.executeUpdate(); return "ADD_OK";
        } catch (Exception e) {} return "FAIL";
    }

    private static String acceptFriend(String u1, String u2) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("UPDATE friends SET status=1 WHERE user1=? AND user2=?")) {
            s.setString(1, u1); s.setString(2, u2); s.executeUpdate(); return "ACCEPT_OK";
        } catch (Exception e) {} return "FAIL";
    }

    private static String declineFriend(String u1, String u2) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("DELETE FROM friends WHERE user1=? AND user2=? AND status=0")) {
            s.setString(1, u1); s.setString(2, u2); s.executeUpdate(); return "DECLINE_OK";
        } catch (Exception e) {} return "FAIL";
    }

    private static String unfriend(String u1, String u2) {
        try (Connection c = getConn(); PreparedStatement s = c.prepareStatement("DELETE FROM friends WHERE (user1=? AND user2=?) OR (user1=? AND user2=?)")) {
            s.setString(1, u1); s.setString(2, u2);
            s.setString(3, u2); s.setString(4, u1);
            s.executeUpdate(); return "UNFRIEND_OK";
        } catch (Exception e) {} return "FAIL";
    }

    private static String getData(String u) {
        StringBuilder sb = new StringBuilder("DATA;;;");
        try (Connection c = getConn()) {
            // 1. Danh sách bạn bè
            PreparedStatement s1 = c.prepareStatement("SELECT u.username, u.fullname, u.ip, u.port, u.avatar FROM users u JOIN friends f ON (f.user1=u.username OR f.user2=u.username) WHERE (f.user1=? OR f.user2=?) AND f.status=1 AND u.username!=?");
            s1.setString(1, u); s1.setString(2, u); s1.setString(3, u);
            ResultSet r1 = s1.executeQuery();
            while(r1.next()) {
                String avtStr = r1.getString("avatar");
                if (avtStr == null || avtStr.isEmpty()) avtStr = "null";
                sb.append(r1.getString(1)).append(":")
                        .append(r1.getString(2)).append(":")
                        .append(r1.getString(3)).append(":")
                        .append(r1.getInt(4)).append(":")
                        .append(avtStr).append("|");
            }
            sb.append("###");

            // 2. Danh sách lời mời kết bạn
            PreparedStatement s2 = c.prepareStatement("SELECT u.username, u.fullname FROM users u JOIN friends f ON f.user1=u.username WHERE f.user2=? AND f.status=0");
            s2.setString(1, u);
            ResultSet r2 = s2.executeQuery();
            while(r2.next()) sb.append(r2.getString(1)).append(":").append(r2.getString(2)).append("|");
            sb.append("###");

            // 3. Danh sách nhóm riêng mà user tham gia
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
                    sb.append(rMem.getString(1)).append("^")
                            .append(rMem.getString(2)).append("^")
                            .append(rMem.getString(3)).append("^")
                            .append(rMem.getInt(4)).append(",");
                }
                sb.append("|");
            }
        } catch (Exception e) {}
        return sb.toString();
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new UDPServer().setVisible(true));
    }
}