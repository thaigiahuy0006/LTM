package com.heal.doan_ltm.client;

import com.heal.doan_ltm.components.*;
import com.heal.doan_ltm.models.Friend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.*;
import javax.sound.sampled.*;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class MainClient extends JFrame {

    static {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) {}
    }

    private String currentUser, currentFullname, currentAvatarBase64, currentTargetUser, currentCallUser;
    private RoundAvatar avatarLabel, lblCallAvatar;
    private JLabel nameLabel, lblTarget, lblTargetStatus;
    private JTextField txtChat;
    private JPanel chatPanel;
    private FlatButton btnCall, btnSend, btnRecord, btnImg, btnFile, btnIcon, btnShowRequests;

    private JPanel pnlReqList, pnlFriendList;
    private JDialog requestDialog;

    private CardLayout cardLayout;
    private JPanel rightPanel, chatContainer, callContainer;
    private JLabel lblCallName, lblCallStatus;
    private FlatButton btnAcceptCall, btnDeclineCall, btnEndCall;

    private DatagramSocket udpSocket;
    private TargetDataLine mic, recordMic;
    private SourceDataLine speaker;
    private volatile boolean isRunning = false;
    private volatile boolean isCalling = false;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream currentRecordStream;
    private Timer timer;
    private Map<String, Friend> friends = new HashMap<>();
    private String lastDataSync = "";

    private Map<Long, byte[]> incomingData = new HashMap<>();
    private Map<Long, Integer> incomingDataReceived = new HashMap<>();

    public MainClient(String user, String fullname, String avatar) {
        this.currentUser = user; this.currentFullname = fullname.isEmpty() ? user : fullname; this.currentAvatarBase64 = avatar;
        setTitle("Z-Voice P2P - " + this.currentFullname);
        setSize(1100, 700); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout());

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(340, 0)); sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        JPanel profilePanel = new JPanel(); profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        profilePanel.setBackground(Color.WHITE); profilePanel.setBorder(new EmptyBorder(25, 20, 15, 20));

        avatarLabel = new RoundAvatar(this.currentUser.substring(0, 1).toUpperCase());
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLabel.setPreferredSize(new Dimension(70, 70)); avatarLabel.setMaximumSize(new Dimension(70, 70));
        avatarLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        avatarLabel.setBackground(new Color(0, 104, 255)); avatarLabel.setForeground(Color.WHITE);
        updateAvatarDisplay();

        nameLabel = new JLabel(this.currentFullname); nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameLabel.setForeground(new Color(30, 30, 30)); nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        FlatButton btnEditProfile = new FlatButton("Cập nhật hồ sơ", new Color(240, 242, 245), new Color(50, 50, 50), 15);
        btnEditProfile.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEditProfile.setPreferredSize(new Dimension(140, 32)); btnEditProfile.setMaximumSize(new Dimension(140, 32));
        btnEditProfile.addActionListener(e -> showProfileDialog());

        profilePanel.add(avatarLabel); profilePanel.add(Box.createVerticalStrut(10));
        profilePanel.add(nameLabel); profilePanel.add(Box.createVerticalStrut(10)); profilePanel.add(btnEditProfile);

        JPanel midPanel = new JPanel(new BorderLayout()); midPanel.setBackground(Color.WHITE);
        JPanel pnlTopMid = new JPanel(new BorderLayout()); pnlTopMid.setBackground(Color.WHITE);

        JPanel pnlSearch = new JPanel(new BorderLayout(10, 0)); pnlSearch.setBackground(Color.WHITE);
        pnlSearch.setBorder(new EmptyBorder(5, 20, 15, 20));
        JTextField txtPhone = new JTextField();
        txtPhone.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtPhone.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FlatButton btnSearch = new FlatButton("Tìm", new Color(0, 104, 255), Color.WHITE, 15);
        btnSearch.setPreferredSize(new Dimension(60, 35));
        pnlSearch.add(txtPhone, BorderLayout.CENTER); pnlSearch.add(btnSearch, BorderLayout.EAST);

        btnShowRequests = new FlatButton("    🔔 Lời mời kết bạn (0)", new Color(245, 247, 250), new Color(100, 100, 100), 0);
        btnShowRequests.setPreferredSize(new Dimension(340, 45));
        btnShowRequests.setHorizontalAlignment(SwingConstants.LEFT);
        btnShowRequests.addActionListener(e -> showRequestDialog());

        pnlTopMid.add(pnlSearch, BorderLayout.NORTH);
        pnlTopMid.add(btnShowRequests, BorderLayout.SOUTH);
        midPanel.add(pnlTopMid, BorderLayout.NORTH);

        pnlFriendList = new JPanel(); pnlFriendList.setLayout(new BoxLayout(pnlFriendList, BoxLayout.Y_AXIS)); pnlFriendList.setBackground(Color.WHITE);
        JScrollPane scrollFr = new JScrollPane(pnlFriendList); scrollFr.setBorder(null);
        midPanel.add(scrollFr, BorderLayout.CENTER);

        sidebar.add(profilePanel, BorderLayout.NORTH); sidebar.add(midPanel, BorderLayout.CENTER);

        FlatButton btnLogout = new FlatButton("Đăng xuất", new Color(255, 235, 235), new Color(232, 65, 24), 0);
        btnLogout.setPreferredSize(new Dimension(100, 45));
        btnLogout.addActionListener(e -> logout()); sidebar.add(btnLogout, BorderLayout.SOUTH);

        requestDialog = new JDialog(this, "Lời mời kết bạn", false);
        requestDialog.setSize(350, 400); requestDialog.setLocationRelativeTo(this);
        pnlReqList = new JPanel(); pnlReqList.setLayout(new BoxLayout(pnlReqList, BoxLayout.Y_AXIS)); pnlReqList.setBackground(Color.WHITE);
        requestDialog.add(new JScrollPane(pnlReqList));

        cardLayout = new CardLayout(); rightPanel = new JPanel(cardLayout);

        chatContainer = new JPanel(new BorderLayout());
        JPanel headerPanel = new JPanel(new BorderLayout()); headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setPreferredSize(new Dimension(0, 70));

        JPanel headerLeft = new JPanel(new GridLayout(2, 1));
        headerLeft.setBackground(Color.WHITE); headerLeft.setBorder(new EmptyBorder(12, 20, 12, 10));
        lblTarget = new JLabel("Chọn bạn bè để trò chuyện"); lblTarget.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTargetStatus = new JLabel(""); lblTargetStatus.setFont(new Font("SansSerif", Font.PLAIN, 13));
        headerLeft.add(lblTarget); headerLeft.add(lblTargetStatus);
        headerPanel.add(headerLeft, BorderLayout.WEST);

        JPanel callPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15)); callPanel.setBackground(Color.WHITE);
        btnCall = new FlatButton("📞", new Color(230, 245, 235), new Color(46, 204, 113), 20);
        btnCall.setFont(new Font("SansSerif", Font.PLAIN, 18));
        btnCall.setEnabled(false); btnCall.setPreferredSize(new Dimension(50, 40));
        callPanel.add(btnCall); headerPanel.add(callPanel, BorderLayout.EAST);

        chatPanel = new JPanel(); chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(244, 245, 247));
        JScrollPane chatScroll = new JScrollPane(chatPanel); chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(244, 245, 247)); bottomPanel.setBorder(new EmptyBorder(15, 20, 20, 20));

        RoundedPanel inputBg = new RoundedPanel(25, Color.WHITE);
        inputBg.setLayout(new BorderLayout(5, 5)); inputBg.setBorder(new EmptyBorder(5, 15, 5, 5));

        txtChat = new JTextField(); txtChat.setEnabled(false); txtChat.setOpaque(false); txtChat.setBorder(null); txtChat.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JPanel pnlChatActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); pnlChatActions.setOpaque(false);

        btnIcon = new FlatButton("😀", new Color(0,0,0,0), new Color(120, 120, 120), 15);
        btnImg = new FlatButton("🖼", new Color(0,0,0,0), new Color(120, 120, 120), 15);
        btnFile = new FlatButton("📁", new Color(0,0,0,0), new Color(120, 120, 120), 15);
        btnRecord = new FlatButton("🎤", new Color(0,0,0,0), new Color(120, 120, 120), 15);
        btnSend = new FlatButton("➤", new Color(0, 104, 255), Color.WHITE, 20);

        Font actionFont = new Font("SansSerif", Font.PLAIN, 18);
        btnIcon.setFont(actionFont); btnImg.setFont(actionFont); btnFile.setFont(actionFont); btnRecord.setFont(actionFont); btnSend.setFont(actionFont);

        btnIcon.setEnabled(false); btnImg.setEnabled(false); btnFile.setEnabled(false); btnRecord.setEnabled(false); btnSend.setEnabled(false);
        Dimension iconDim = new Dimension(42, 42);
        btnIcon.setPreferredSize(iconDim); btnImg.setPreferredSize(iconDim); btnFile.setPreferredSize(iconDim); btnRecord.setPreferredSize(iconDim); btnSend.setPreferredSize(iconDim);

        pnlChatActions.add(btnIcon); pnlChatActions.add(btnImg); pnlChatActions.add(btnFile); pnlChatActions.add(btnRecord); pnlChatActions.add(btnSend);

        JPopupMenu emojiMenu = new JPopupMenu(); emojiMenu.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
        JPanel emojiPanel = new JPanel(new GridLayout(0, 5, 5, 5)); emojiPanel.setBackground(Color.WHITE); emojiPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] emojis = {"😀", "😂", "😍", "😭", "👍", "❤️", "🔥", "✨", "🎉", "😢", "😡", "🙏", "😱", "😎", "💩"};
        for(String e : emojis) {
            JButton btnE = new JButton(e); btnE.setFont(new Font("SansSerif", Font.PLAIN, 24)); btnE.setBorder(null); btnE.setBackground(Color.WHITE); btnE.setFocusPainted(false); btnE.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnE.addActionListener(ev -> { txtChat.setText(txtChat.getText() + e); emojiMenu.setVisible(false); });
            emojiPanel.add(btnE);
        }
        emojiMenu.add(emojiPanel);
        btnIcon.addActionListener(e -> emojiMenu.show(btnIcon, 0, -emojiMenu.getPreferredSize().height));

        inputBg.add(txtChat, BorderLayout.CENTER); inputBg.add(pnlChatActions, BorderLayout.EAST);
        bottomPanel.add(inputBg, BorderLayout.CENTER);

        chatContainer.add(headerPanel, BorderLayout.NORTH); chatContainer.add(chatScroll, BorderLayout.CENTER); chatContainer.add(bottomPanel, BorderLayout.SOUTH);

        callContainer = new JPanel(new GridBagLayout()); callContainer.setBackground(new Color(33, 33, 33));
        GridBagConstraints g = new GridBagConstraints(); g.gridwidth = GridBagConstraints.REMAINDER; g.anchor = GridBagConstraints.CENTER; g.insets = new Insets(10, 10, 10, 10);

        lblCallAvatar = new RoundAvatar("?"); lblCallAvatar.setFont(new Font("SansSerif", Font.BOLD, 60));
        lblCallAvatar.setBackground(new Color(0, 104, 255)); lblCallAvatar.setForeground(Color.WHITE);
        lblCallAvatar.setPreferredSize(new Dimension(150, 150)); lblCallAvatar.setMaximumSize(new Dimension(150, 150));

        lblCallName = new JLabel("Tên người gọi"); lblCallName.setFont(new Font("SansSerif", Font.BOLD, 30)); lblCallName.setForeground(Color.WHITE);
        lblCallStatus = new JLabel("Đang gọi..."); lblCallStatus.setFont(new Font("SansSerif", Font.PLAIN, 18)); lblCallStatus.setForeground(new Color(180, 180, 180));

        JPanel callActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20)); callActionPanel.setOpaque(false);
        btnAcceptCall = new FlatButton("Nhấc máy", new Color(46, 204, 113), Color.WHITE, 25); btnAcceptCall.setPreferredSize(new Dimension(130, 50));
        btnDeclineCall = new FlatButton("Từ chối", new Color(231, 76, 60), Color.WHITE, 25); btnDeclineCall.setPreferredSize(new Dimension(130, 50));
        btnEndCall = new FlatButton("Kết thúc", new Color(231, 76, 60), Color.WHITE, 25); btnEndCall.setPreferredSize(new Dimension(130, 50));

        callActionPanel.add(btnAcceptCall); callActionPanel.add(btnDeclineCall); callActionPanel.add(btnEndCall);
        callContainer.add(lblCallAvatar, g); callContainer.add(lblCallName, g); callContainer.add(lblCallStatus, g); callContainer.add(callActionPanel, g);

        rightPanel.add(chatContainer, "CHAT"); rightPanel.add(callContainer, "CALL");

        add(sidebar, BorderLayout.WEST); add(rightPanel, BorderLayout.CENTER);

        btnSearch.addActionListener(e -> { if(!txtPhone.getText().trim().isEmpty()) sendToServer("SEARCH;;;" + txtPhone.getText().trim()); });
        btnCall.addActionListener(e -> initiateCall());
        btnAcceptCall.addActionListener(e -> acceptCall());
        btnDeclineCall.addActionListener(e -> declineCall());
        btnEndCall.addActionListener(e -> endCall());
        btnImg.addActionListener(e -> sendFileOrImage(8));
        btnFile.addActionListener(e -> sendFileOrImage(9));
        btnRecord.addActionListener(e -> toggleRecord());

        // Gắn sự kiện gửi Text
        btnSend.addActionListener(e -> sendText());
        txtChat.addActionListener(e -> sendText());

        startNetwork();
    }

    private void updateAvatarDisplay() {
        try {
            if (currentAvatarBase64 != null && !currentAvatarBase64.isEmpty()) {
                byte[] imgBytes = Base64.getDecoder().decode(currentAvatarBase64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                avatarLabel.setImage(img); avatarLabel.setAvatarText("");
            } else {
                avatarLabel.setImage(null); avatarLabel.setAvatarText(currentUser.substring(0, 1).toUpperCase());
            }
        } catch (Exception e) {}
    }

    private void showProfileDialog() {
        JPanel p = new JPanel(new BorderLayout(10, 10)); JTextField nameField = new JTextField(currentFullname);
        JButton btnPickImg = new JButton("Chọn ảnh"); final String[] newAvatar = {currentAvatarBase64};
        btnPickImg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(fc.getSelectedFile()); Image scaled = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                    BufferedImage bimg = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = bimg.createGraphics(); g2d.drawImage(scaled, 0, 0, null); g2d.dispose();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(); ImageIO.write(bimg, "jpg", baos);
                    newAvatar[0] = Base64.getEncoder().encodeToString(baos.toByteArray()); JOptionPane.showMessageDialog(this, "Đã nạp ảnh");
                } catch (Exception ex) {}
            }
        });
        p.add(new JLabel("Tên hiển thị:"), BorderLayout.WEST); p.add(nameField, BorderLayout.CENTER); p.add(btnPickImg, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, p, "Sửa hồ sơ", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            sendToServer("UPDATE;;;" + currentUser + ";;;" + nameField.getText() + ";;;" + newAvatar[0]);
            currentFullname = nameField.getText(); currentAvatarBase64 = newAvatar[0];
            nameLabel.setText(currentFullname); updateAvatarDisplay(); setTitle("Z-Voice P2P - " + currentFullname);
        }
    }

    private void showRequestDialog() { requestDialog.setVisible(true); }

    private void startNetwork() {
        try {
            udpSocket = new DatagramSocket(); isRunning = true;
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            speaker = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
            speaker.open(format); speaker.start();

            new Thread(this::listenData).start();
            timer = new Timer(3000, e -> { sendToServer("ONLINE;;;" + currentUser); sendToServer("GETDATA;;;" + currentUser); });
            timer.start();
        } catch (Exception e) {}
    }

    private void sendToServer(String msg) {
        try { byte[] data = msg.getBytes("UTF-8"); udpSocket.send(new DatagramPacket(data, data.length, InetAddress.getByName("172.26.52.71"), 8080)); } catch (Exception e) {}
    }

    private void sendP2P(Friend f, byte[] data) {
        if(f == null || f.ip.isEmpty() || f.port == 0) return;
        try { udpSocket.send(new DatagramPacket(data, data.length, InetAddress.getByName(f.ip), f.port)); } catch (Exception e) {}
    }

    private Friend getFriendByIp(String ip, int port) {
        for(Friend f : friends.values()) {
            if(f.ip.equals(ip)) return f;
        }
        return null;
    }

    private void listenData() {
        byte[] buffer = new byte[65507];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                if (packet.getPort() == 8080) {
                    String s = new String(buffer, 0, packet.getLength(), "UTF-8");
                    SwingUtilities.invokeLater(() -> processServerMsg(s));
                } else {
                    int type = buffer[0]; Friend senderF = getFriendByIp(packet.getAddress().getHostAddress(), packet.getPort());

                    if (type == 0 && isCalling && speaker != null) speaker.write(buffer, 1, packet.getLength() - 1);
                    else if (type == 1 && senderF != null) {
                        String text = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        SwingUtilities.invokeLater(() -> appendMsg(senderF.name, text, false));
                    } else if ((type == 3 || type == 8 || type == 9) && senderF != null) {
                        ByteBuffer bb = ByteBuffer.wrap(buffer, 1, packet.getLength() - 1);
                        long id = bb.getLong(); int total = bb.getInt(); int offset = bb.getInt(); int dataLen = packet.getLength() - 17;
                        if (!incomingData.containsKey(id)) { incomingData.put(id, new byte[total]); incomingDataReceived.put(id, 0); }
                        byte[] fullData = incomingData.get(id); System.arraycopy(buffer, 17, fullData, offset, dataLen);
                        int received = incomingDataReceived.get(id) + dataLen; incomingDataReceived.put(id, received);
                        if (received >= total) {
                            byte[] finalData = fullData.clone();
                            if(type == 3) SwingUtilities.invokeLater(() -> appendAudioMsg(senderF.name, finalData, false));
                            else if(type == 8) SwingUtilities.invokeLater(() -> appendImageMsg(senderF.name, finalData, false));
                            else if(type == 9) SwingUtilities.invokeLater(() -> appendFileMsg(senderF.name, finalData, false));
                            incomingData.remove(id); incomingDataReceived.remove(id);
                        }
                    } else if (type == 4 && senderF != null) SwingUtilities.invokeLater(() -> handleIncomingCall(senderF.username));
                    else if (type == 5) SwingUtilities.invokeLater(() -> handleCallAccepted());
                    else if (type == 6) SwingUtilities.invokeLater(() -> handleCallDeclined());
                    else if (type == 7) SwingUtilities.invokeLater(() -> handleCallEnded());
                }
            } catch (Exception e) {}
        }
    }

    private void processServerMsg(String s) {
        // --- XỬ LÝ LỊCH SỬ TIN NHẮN TỪ DATABASE ---
        if (s.startsWith("MSGDATA;;;")) {
            String data = s.substring(10);
            chatPanel.removeAll();
            if (!data.isEmpty()) {
                String[] msgs = data.split("\\|\\|");
                for (String m : msgs) {
                    if (m.isEmpty()) continue;
                    int idx = m.indexOf("::");
                    if(idx != -1) {
                        String senderUsername = m.substring(0, idx);
                        String content = m.substring(idx+2);
                        boolean isMe = senderUsername.equals(currentUser);

                        // Lấy tên hiển thị thay vì username
                        String senderName = "";
                        if (!isMe) {
                            Friend fr = friends.get(senderUsername);
                            if(fr != null) senderName = fr.name;
                        }
                        appendMsg(senderName, content, isMe);
                    }
                }
            }
            chatPanel.revalidate(); chatPanel.repaint();
            JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
            SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
            return;
        }

        if (s.startsWith("DATA;;;")) {
            if (s.equals(lastDataSync)) return;
            lastDataSync = s;
            String[] parts = s.substring(7).split("###");
            friends.clear(); pnlFriendList.removeAll();

            if (parts.length > 0 && !parts[0].isEmpty()) {
                for(String f : parts[0].split("\\|")) {
                    if(f.isEmpty()) continue;
                    String[] d = f.split(":");
                    Friend friendObj = new Friend(d[0], d[1], d[2], Integer.parseInt(d[3]));
                    friends.put(d[0], friendObj);

                    JPanel item = new JPanel(new BorderLayout(10, 0));
                    item.setBackground(Color.WHITE); item.setBorder(new EmptyBorder(10, 20, 10, 10));
                    item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

                    RoundAvatar av = new RoundAvatar(friendObj.name.substring(0, 1).toUpperCase());
                    av.setPreferredSize(new Dimension(45, 45)); av.setMinimumSize(new Dimension(45, 45)); av.setMaximumSize(new Dimension(45, 45));
                    av.setBackground(new Color(0, 104, 255)); av.setForeground(Color.WHITE); av.setFont(new Font("SansSerif", Font.BOLD, 18));

                    JPanel info = new JPanel(new GridLayout(2, 1, 0, 3)); info.setOpaque(false);
                    JLabel nameLbl = new JLabel(friendObj.name); nameLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
                    JLabel statusLbl = new JLabel(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
                    statusLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    statusLbl.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(150, 150, 150));
                    info.add(nameLbl); info.add(statusLbl);

                    JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8)); rightActions.setOpaque(false);
                    FlatButton btnOptions = new FlatButton("⋮", new Color(0,0,0,0), new Color(150, 150, 150), 15);
                    btnOptions.setFont(new Font("SansSerif", Font.BOLD, 18)); btnOptions.setPreferredSize(new Dimension(30, 30));

                    JPopupMenu friendMenu = new JPopupMenu(); JMenuItem itemUnfriend = new JMenuItem("Xóa kết bạn");
                    itemUnfriend.setForeground(new Color(231, 76, 60));
                    itemUnfriend.addActionListener(ev -> {
                        int ans = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa kết bạn với " + friendObj.name + "?", "Xóa kết bạn", JOptionPane.YES_NO_OPTION);
                        if (ans == JOptionPane.YES_OPTION) {
                            sendToServer("UNFRIEND;;;" + currentUser + ";;;" + friendObj.username);
                            if(currentTargetUser != null && currentTargetUser.equals(friendObj.username)) {
                                currentTargetUser = null; lblTarget.setText("Chọn bạn bè để trò chuyện"); lblTargetStatus.setText("");
                                txtChat.setEnabled(false); btnSend.setEnabled(false); btnCall.setEnabled(false);
                                btnRecord.setEnabled(false); btnImg.setEnabled(false); btnFile.setEnabled(false); btnIcon.setEnabled(false);
                                chatPanel.removeAll(); chatPanel.revalidate(); chatPanel.repaint();
                            }
                            friends.remove(friendObj.username); pnlFriendList.remove(item);
                            pnlFriendList.revalidate(); pnlFriendList.repaint();
                            lastDataSync = ""; sendToServer("GETDATA;;;" + currentUser);
                        }
                    });
                    friendMenu.add(itemUnfriend);
                    btnOptions.addActionListener(e -> friendMenu.show(btnOptions, -50, btnOptions.getHeight()));
                    rightActions.add(btnOptions);

                    item.add(av, BorderLayout.WEST); item.add(info, BorderLayout.CENTER); item.add(rightActions, BorderLayout.EAST);

                    item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    item.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            currentTargetUser = friendObj.username; lblTarget.setText(friendObj.name);
                            lblTargetStatus.setText(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
                            lblTargetStatus.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(170, 170, 170));
                            txtChat.setEnabled(true); btnSend.setEnabled(true); btnCall.setEnabled(true);
                            btnRecord.setEnabled(true); btnImg.setEnabled(true); btnFile.setEnabled(true); btnIcon.setEnabled(true);

                            // GỬI YÊU CẦU LOAD LỊCH SỬ KHI BẤM VÀO BẠN BÈ
                            chatPanel.removeAll(); chatPanel.revalidate(); chatPanel.repaint();
                            sendToServer("GETMSG;;;" + currentUser + ";;;" + currentTargetUser);

                            for (Component comp : pnlFriendList.getComponents()) comp.setBackground(Color.WHITE);
                            item.setBackground(new Color(240, 245, 255));
                        }
                        public void mouseEntered(MouseEvent e) { if (!item.getBackground().equals(new Color(240, 245, 255))) item.setBackground(new Color(245, 245, 245)); }
                        public void mouseExited(MouseEvent e) { if (!item.getBackground().equals(new Color(240, 245, 255))) item.setBackground(Color.WHITE); }
                    });
                    if (currentTargetUser != null && currentTargetUser.equals(friendObj.username)) {
                        item.setBackground(new Color(240, 245, 255));
                        lblTargetStatus.setText(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
                        lblTargetStatus.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(170, 170, 170));
                    }
                    pnlFriendList.add(item);
                }
            }
            pnlFriendList.revalidate(); pnlFriendList.repaint();

            pnlReqList.removeAll(); int reqCount = 0;
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String[] reqs = parts[1].split("\\|"); reqCount = reqs.length;
                for(String r : reqs) {
                    if(r.isEmpty()) continue;
                    String reqUser = r.split(":")[0]; String reqName = r.split(":")[1];

                    JPanel item = new JPanel(new BorderLayout(10, 0)); item.setBackground(Color.WHITE); item.setBorder(new EmptyBorder(10, 15, 10, 15)); item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
                    RoundAvatar av = new RoundAvatar(reqName.substring(0, 1).toUpperCase()); av.setPreferredSize(new Dimension(40, 40)); av.setBackground(new Color(230, 230, 230)); av.setForeground(Color.DARK_GRAY); av.setFont(new Font("SansSerif", Font.BOLD, 16));
                    JLabel nameLbl = new JLabel(reqName); nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

                    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); actions.setOpaque(false);
                    FlatButton btnAcc = new FlatButton("✓", new Color(46, 204, 113), Color.WHITE, 10); FlatButton btnDec = new FlatButton("✗", new Color(231, 76, 60), Color.WHITE, 10);
                    btnAcc.setPreferredSize(new Dimension(35, 30)); btnDec.setPreferredSize(new Dimension(35, 30));
                    btnAcc.addActionListener(e -> { sendToServer("ACCEPT;;;" + reqUser + ";;;" + currentUser); lastDataSync = ""; });
                    btnDec.addActionListener(e -> { sendToServer("DECLINE;;;" + reqUser + ";;;" + currentUser); lastDataSync = ""; });
                    actions.add(btnAcc); actions.add(btnDec);
                    item.add(av, BorderLayout.WEST); item.add(nameLbl, BorderLayout.CENTER); item.add(actions, BorderLayout.EAST);
                    pnlReqList.add(item);
                }
            }
            if (reqCount > 0) { btnShowRequests.setText("    🔔 Lời mời kết bạn (" + reqCount + ")"); btnShowRequests.setForeground(new Color(232, 65, 24)); }
            else { btnShowRequests.setText("    Lời mời kết bạn (0)"); btnShowRequests.setForeground(new Color(100, 100, 100)); requestDialog.setVisible(false); }
            pnlReqList.revalidate(); pnlReqList.repaint();

        } else if (s.startsWith("SEARCHRES;;;")) {
            String[] p = s.split(";;;");
            if (p.length == 3 && JOptionPane.showConfirmDialog(this, "Tìm thấy: " + p[2] + "\nBạn có muốn gửi lời mời?", "Kết bạn", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                sendToServer("ADD;;;" + currentUser + ";;;" + p[1]); JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới " + p[2]);
            }
        } else if (s.equals("NOTFOUND")) { JOptionPane.showMessageDialog(this, "Không tìm thấy người dùng này!"); }
    }

    private void initiateCall() {
        if (currentTargetUser == null) return; Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) { JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!"); return; }
        currentCallUser = currentTargetUser; sendP2P(f, new byte[]{4});
        lblCallAvatar.setAvatarText(f.name.substring(0, 1).toUpperCase()); lblCallName.setText(f.name); lblCallStatus.setText("Đang đổ chuông...");
        btnAcceptCall.setVisible(false); btnDeclineCall.setVisible(false); btnEndCall.setVisible(true); cardLayout.show(rightPanel, "CALL");
    }

    private void handleIncomingCall(String callerUsername) {
        if (currentCallUser != null) { sendP2P(friends.get(callerUsername), new byte[]{6}); return; }
        currentCallUser = callerUsername; Friend f = friends.get(currentCallUser); if (f == null) return;
        lblCallAvatar.setAvatarText(f.name.substring(0, 1).toUpperCase()); lblCallName.setText(f.name); lblCallStatus.setText("Cuộc gọi đến...");
        btnAcceptCall.setVisible(true); btnDeclineCall.setVisible(true); btnEndCall.setVisible(false); cardLayout.show(rightPanel, "CALL");
    }

    private void acceptCall() {
        if (currentCallUser == null) return; sendP2P(friends.get(currentCallUser), new byte[]{5});
        lblCallStatus.setText("Đang trò chuyện"); btnAcceptCall.setVisible(false); btnDeclineCall.setVisible(false); btnEndCall.setVisible(true); startAudioStreams();
    }

    private void handleCallAccepted() { lblCallStatus.setText("Đang trò chuyện"); startAudioStreams(); }
    private void declineCall() { if (currentCallUser == null) return; sendP2P(friends.get(currentCallUser), new byte[]{6}); closeCallUI(); }
    private void handleCallDeclined() { JOptionPane.showMessageDialog(this, "Người dùng đã từ chối cuộc gọi"); closeCallUI(); }
    private void endCall() { if (currentCallUser == null) return; sendP2P(friends.get(currentCallUser), new byte[]{7}); closeCallUI(); }
    private void handleCallEnded() { closeCallUI(); }

    private void closeCallUI() { currentCallUser = null; stopAudioStreams(); cardLayout.show(rightPanel, "CHAT"); }

    private void startAudioStreams() {
        if (isCalling) return;
        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
            mic.open(format); mic.start(); isCalling = true;
            new Thread(() -> {
                try {
                    byte[] audioBuffer = new byte[1024]; byte[] packetData = new byte[1025]; packetData[0] = 0;
                    Friend f = friends.get(currentCallUser); if (f == null) return; InetAddress addr = InetAddress.getByName(f.ip); int targetPort = f.port;
                    while (isCalling && currentCallUser != null) {
                        int read = mic.read(audioBuffer, 0, audioBuffer.length);
                        if (read > 0) { System.arraycopy(audioBuffer, 0, packetData, 1, read); udpSocket.send(new DatagramPacket(packetData, packetData.length, addr, targetPort)); }
                    }
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {}
    }

    private void stopAudioStreams() { isCalling = false; if (mic != null) { mic.close(); mic = null; } }

    private void sendFragmentedData(Friend f, byte type, byte[] fullData) {
        new Thread(() -> {
            try {
                InetAddress addr = InetAddress.getByName(f.ip); long msgId = System.currentTimeMillis(); int offset = 0;
                while (offset < fullData.length) {
                    int length = Math.min(50000, fullData.length - offset); ByteBuffer bb = ByteBuffer.allocate(17 + length);
                    bb.put(type); bb.putLong(msgId); bb.putInt(fullData.length); bb.putInt(offset); bb.put(fullData, offset, length);
                    udpSocket.send(new DatagramPacket(bb.array(), bb.array().length, addr, f.port)); offset += length; Thread.sleep(15);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void sendFileOrImage(int type) {
        if (currentTargetUser == null) return; Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) { JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!"); return; }

        JFileChooser fc = new JFileChooser();
        if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                if(type == 8) { sendFragmentedData(f, (byte)8, fileBytes); appendImageMsg("", fileBytes, true); }
                else {
                    byte[] nameBytes = file.getName().getBytes("UTF-8"); ByteBuffer bb = ByteBuffer.allocate(4 + nameBytes.length + fileBytes.length);
                    bb.putInt(nameBytes.length); bb.put(nameBytes); bb.put(fileBytes);
                    sendFragmentedData(f, (byte)9, bb.array()); appendFileMsg("", bb.array(), true);
                }
            } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi đọc file"); }
        }
    }

    private void toggleRecord() {
        if (currentTargetUser == null) return; Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) { JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!"); return; }

        if (!isRecording) {
            try {
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false); recordMic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                recordMic.open(format); recordMic.start(); isRecording = true; currentRecordStream = new ByteArrayOutputStream();
                btnRecord.setForeground(new Color(231, 76, 60));
                new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    while (isRecording) { int read = recordMic.read(buffer, 0, buffer.length); if (read > 0) currentRecordStream.write(buffer, 0, read); }
                    recordMic.close();
                }).start();
            } catch (Exception e) {}
        } else {
            isRecording = false; btnRecord.setForeground(new Color(120, 120, 120)); byte[] audioData = currentRecordStream.toByteArray();
            appendAudioMsg("", audioData, true); sendFragmentedData(f, (byte)3, audioData);
        }
    }

    private void sendText() {
        if(currentTargetUser == null || txtChat.getText().trim().isEmpty()) return; Friend f = friends.get(currentTargetUser);
        if(f != null && f.isOnline()) {
            try {
                String messageContent = txtChat.getText().trim();
                byte[] textBytes = messageContent.getBytes("UTF-8"); byte[] packetData = new byte[textBytes.length + 1]; packetData[0] = 1;
                System.arraycopy(textBytes, 0, packetData, 1, textBytes.length);
                udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(f.ip), f.port)); // Bắn P2P

                // GỬI BẢN SAO CHO MÁY CHỦ ĐỂ LƯU VÀO DATABASE
                sendToServer("SAVEMSG;;;" + currentUser + ";;;" + f.username + ";;;" + messageContent);

                appendMsg("", messageContent, true); txtChat.setText("");
            } catch (Exception e) {}
        } else JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
    }

    private void addChatBubble(JComponent comp, boolean isMe, String sender) {
        JPanel w = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT)); w.setOpaque(false); w.setBorder(new EmptyBorder(5, 5, 5, 5));
        if (!isMe && !sender.isEmpty()) {
            RoundAvatar av = new RoundAvatar(sender.substring(0, 1).toUpperCase()); av.setPreferredSize(new Dimension(35, 35));
            av.setBackground(new Color(0, 104, 255)); av.setForeground(Color.WHITE); av.setFont(new Font("SansSerif", Font.BOLD, 14)); w.add(av);
        }
        w.add(comp); chatPanel.add(w); chatPanel.revalidate(); chatPanel.repaint();
        JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
    }

    private void appendMsg(String sender, String msg, boolean isMe) {
        boolean isEmoji = msg.length() <= 8 && !msg.matches(".*[a-zA-Z0-9].*"); JLabel l = new JLabel(msg);
        if(!isEmoji) {
            l.setFont(new Font("SansSerif", Font.PLAIN, 15)); BubblePanel bubble = new BubblePanel(isMe ? new Color(0, 104, 255) : Color.WHITE);
            l.setForeground(isMe ? Color.WHITE : new Color(30, 30, 30)); bubble.add(l, BorderLayout.CENTER); addChatBubble(bubble, isMe, sender);
        } else { l.setFont(new Font("SansSerif", Font.PLAIN, 45)); addChatBubble(l, isMe, sender); }
    }

    private void appendImageMsg(String sender, byte[] imgData, boolean isMe) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgData)); int w = img.getWidth(), h = img.getHeight(), max = 220;
            if(w > max || h > max) { float ratio = Math.min((float)max/w, (float)max/h); w = Math.round(w*ratio); h = Math.round(h*ratio); }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH); JLabel l = new JLabel(new ImageIcon(scaled));
            BubblePanel bubble = new BubblePanel(isMe ? new Color(0, 104, 255) : Color.WHITE); bubble.add(l, BorderLayout.CENTER); addChatBubble(bubble, isMe, sender);
        } catch(Exception e) {}
    }

    private void appendFileMsg(String sender, byte[] payload, boolean isMe) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(payload); int nameLen = bb.getInt(); byte[] nameBytes = new byte[nameLen]; bb.get(nameBytes);
            String fileName = new String(nameBytes, "UTF-8"); byte[] fileData = new byte[payload.length - 4 - nameLen]; bb.get(fileData);
            FlatButton btn = new FlatButton("⬇ " + fileName, isMe ? new Color(0, 104, 255) : Color.WHITE, isMe ? Color.WHITE : new Color(30, 30, 30), 10);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
            btn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File(fileName));
                if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { try { Files.write(fc.getSelectedFile().toPath(), fileData); JOptionPane.showMessageDialog(this, "Đã lưu!"); } catch(Exception ex) {} }
            });
            BubblePanel bubble = new BubblePanel(isMe ? new Color(0, 104, 255) : Color.WHITE); bubble.add(btn, BorderLayout.CENTER); addChatBubble(bubble, isMe, sender);
        } catch(Exception e) {}
    }

    private void appendAudioMsg(String sender, byte[] audioData, boolean isMe) {
        FlatButton btnPlay = new FlatButton("► Tin nhắn thoại", isMe ? new Color(0, 104, 255) : Color.WHITE, isMe ? Color.WHITE : new Color(30, 30, 30), 10);
        btnPlay.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnPlay.addActionListener(e -> {
            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false); SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                    sdl.open(format); sdl.start(); sdl.write(audioData, 0, audioData.length); sdl.drain(); sdl.close();
                } catch(Exception ex) {}
            }).start();
        });
        BubblePanel bubble = new BubblePanel(isMe ? new Color(0, 104, 255) : Color.WHITE); bubble.add(btnPlay, BorderLayout.CENTER); addChatBubble(bubble, isMe, sender);
    }

    private void logout() {
        isRunning = false; closeCallUI(); isRecording = false;
        if(timer != null) timer.stop(); if(udpSocket != null) udpSocket.close(); if(recordMic != null) recordMic.close(); if(speaker != null) speaker.close();
        try (DatagramSocket tmp = new DatagramSocket()) { byte[] dt = ("ONLINE;;;" + currentUser).getBytes("UTF-8"); tmp.send(new DatagramPacket(dt, dt.length, InetAddress.getByName("172.26.52.71"), 8080)); } catch(Exception e) {}
        new LoginClient().setVisible(true); this.dispose();
    }
}