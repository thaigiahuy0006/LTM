package com.heal.doan_ltm.client;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.sarxos.webcam.Webcam;
import com.heal.doan_ltm.components.*;
import com.heal.doan_ltm.models.Friend;
import com.heal.doan_ltm.models.Group;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Timer;

public class MainClient extends JFrame {

    static {
        FlatLightLaf.setup();
        UIManager.put("ScrollBar.width", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 10);
    }

    private static final Color ZALO_BLUE = new Color(0, 104, 255);
    private static final Color ZALO_BLUE_DARK = new Color(0, 82, 204);
    private static final Color ZALO_SENT_BUBBLE = new Color(229, 239, 255);
    private static final Color ZALO_SENT_BORDER = new Color(195, 219, 255);
    private static final Color ZALO_ACTIVE_ITEM = new Color(229, 239, 255);
    private static final Color ZALO_HOVER_ITEM = new Color(243, 245, 246);
    private static final Color ZALO_CHAT_BG = new Color(238, 240, 241);
    private static final Color ZALO_TEXT_DARK = new Color(8, 28, 54);
    private static final Color ZALO_ICON_GRAY = new Color(75, 85, 99);
    private static final Color ZALO_DIVIDER = new Color(225, 228, 232);

    private String currentUser, currentFullname, currentAvatarBase64;
    private String currentTargetUser = null;
    private int currentGroupId = -1;
    private String currentCallUser = null;

    private RoundAvatar avatarLabel, targetAvatarHeader;
    private JLabel lblTarget, lblTargetStatus;
    private JTextField txtChat, txtSidebarSearch;
    private JPanel chatPanel;
    private FlatButton btnCall, btnVideoHeader, btnToggleInfo, btnGroupMembers, btnSend, btnRecord, btnImg, btnFile, btnIcon, btnShowRequests;

    private JPanel pnlReqList, pnlConversationList, pnlSearchActions;
    private FlatButton btnCloseSearch;
    private boolean isSearchMode = false;
    private boolean filterOnlineOnly = false;
    private JLabel lblTabAll, lblTabOnline;
    private JDialog requestDialog;
    private JPanel pnlAddFriendResult;

    // Khung "Thông tin hội thoại" & "Kho lưu trữ" bên phải giống Zalo PC
    private JPanel infoDrawerPanel;
    private CardLayout infoDrawerCardLayout;
    private RoundAvatar infoAvatar;
    private JLabel lblInfoName, lblInfoSub1, lblInfoSub2, lblMuteText, lblPinText;
    private JPanel pnlSharedImages, pnlSharedFiles, pnlSharedLinks;
    private FlatButton btnViewAllMedia;

    // Thành phần bên trong thẻ "Kho lưu trữ"
    private JPanel pnlArchiveContent;
    private JLabel lblArchiveTabMedia, lblArchiveTabFiles, lblArchiveTabLinks;
    private String activeArchiveTab = "MEDIA";

    private List<SharedMediaItem> currentSharedImages = new ArrayList<>();
    private List<SharedFileItem> currentSharedFiles = new ArrayList<>();
    private List<String> currentSharedLinks = new ArrayList<>();
    private Set<String> pinnedConversations = new HashSet<>();
    private Set<String> mutedConversations = new HashSet<>();

    private static class SharedMediaItem {
        final BufferedImage image;
        final String dateLabel;
        SharedMediaItem(BufferedImage image, String dateLabel) {
            this.image = image;
            this.dateLabel = dateLabel;
        }
    }

    private static class SharedFileItem {
        final String name;
        final byte[] data;
        SharedFileItem(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }
    }

    class DrawerBodyPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private CardLayout cardLayout;
    private JPanel rightPanel, chatContainer;
    private JLayeredPane callContainer;

    private VideoPanel pnlTargetVideo;
    private JLabel lblMyVideo;
    private JLabel lblCallName, lblCallStatus;
    private FlatButton btnAcceptCall, btnDeclineCall, btnEndCall;
    private JPanel bottomCallPanel;

    private DatagramSocket udpSocket;
    private TargetDataLine mic, recordMic;
    private SourceDataLine speaker;
    private volatile boolean isRunning = false;
    private volatile boolean isCalling = false;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream currentRecordStream;
    private Timer timer;

    private Map<String, Friend> friends = new LinkedHashMap<>();
    private Map<Integer, Group> groups = new LinkedHashMap<>();
    private Map<String, BufferedImage> avatarCache = new HashMap<>();
    private String lastDataSync = "";

    private Webcam webcam;
    private volatile boolean isVideoOn = false;
    private FlatButton btnToggleVideo;

    private Map<Long, byte[]> incomingData = new HashMap<>();
    private Map<Long, Integer> incomingDataReceived = new HashMap<>();

    private MulticastSocket mcSocket;
    private InetAddress mcGroup;
    private boolean isMulticastMode = false;
    private FlatButton btnMulticast;

    public MainClient(String user, String fullname, String avatar) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.currentUser = user;
        this.currentFullname = fullname.isEmpty() ? user : fullname;
        this.currentAvatarBase64 = avatar;

        setTitle("Z-Voice - " + this.currentFullname);
        setSize(1340, 760);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // =====================================================================
        // CỘT 1: THANH ĐIỀU HƯỚNG XANH ZALO BÊN TRÁI (64px)
        // =====================================================================
        JPanel navRail = new JPanel();
        navRail.setPreferredSize(new Dimension(64, 0));
        navRail.setBackground(ZALO_BLUE);
        navRail.setLayout(new BoxLayout(navRail, BoxLayout.Y_AXIS));
        navRail.setBorder(new EmptyBorder(18, 8, 18, 8));

        avatarLabel = new RoundAvatar(this.currentUser.substring(0, 1).toUpperCase());
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLabel.setPreferredSize(new Dimension(46, 46));
        avatarLabel.setMaximumSize(new Dimension(46, 46));
        avatarLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        avatarLabel.setBackground(Color.WHITE);
        avatarLabel.setForeground(ZALO_BLUE);
        avatarLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        avatarLabel.setToolTipText("Cập nhật hồ sơ (" + this.currentFullname + ")");
        avatarLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showProfileDialog(); }
        });
        updateAvatarDisplay();

        FlatButton navChat = createRailButton(ZaloIcon.Type.NAV_CHAT, "Tin nhắn & Nhóm", ZALO_BLUE_DARK);
        navChat.addActionListener(e -> deactivateMulticastMode());

        FlatButton navCreateGroup = createRailButton(ZaloIcon.Type.NAV_GROUP, "Tạo nhóm chat mới", new Color(0, 0, 0, 0));
        navCreateGroup.addActionListener(e -> showCreateGroupDialog());

        FlatButton navRequests = createRailButton(ZaloIcon.Type.NAV_BELL, "Lời mời kết bạn", new Color(0, 0, 0, 0));
        navRequests.addActionListener(e -> showRequestDialog());

        FlatButton navLan = createRailButton(ZaloIcon.Type.NAV_LAN, "Phòng Chat LAN (Multicast)", new Color(0, 0, 0, 0));
        navLan.addActionListener(e -> activateMulticastMode());

        FlatButton navLogout = createRailButton(ZaloIcon.Type.NAV_LOGOUT, "Đăng xuất", new Color(0, 0, 0, 0));
        navLogout.addActionListener(e -> logout());

        navRail.add(avatarLabel);
        navRail.add(Box.createVerticalStrut(24));
        navRail.add(navChat);
        navRail.add(Box.createVerticalStrut(10));
        navRail.add(navCreateGroup);
        navRail.add(Box.createVerticalStrut(10));
        navRail.add(navRequests);
        navRail.add(Box.createVerticalStrut(10));
        navRail.add(navLan);
        navRail.add(Box.createVerticalGlue());
        navRail.add(navLogout);

        // =====================================================================
        // CỘT 2: DANH SÁCH HỘI THOẠI CHUẨN ZALO (310px)
        // =====================================================================
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(310, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ZALO_DIVIDER));

        JPanel topSidebar = new JPanel();
        topSidebar.setLayout(new BoxLayout(topSidebar, BoxLayout.Y_AXIS));
        topSidebar.setBackground(Color.WHITE);
        topSidebar.setBorder(new EmptyBorder(14, 14, 10, 14));

        // Hàng 1: Ô Tìm kiếm + Nút Thêm bạn + Nút Tạo nhóm
        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        RoundedPanel searchBox = new RoundedPanel(8, ZALO_HOVER_ITEM);
        searchBox.setLayout(new BorderLayout(6, 0));
        searchBox.setBorder(new EmptyBorder(0, 10, 0, 8));

        JLabel lblSearchIcon = new JLabel(new ZaloIcon(ZaloIcon.Type.SEARCH, 16, new Color(120, 130, 145)));
        txtSidebarSearch = new JTextField();
        txtSidebarSearch.setOpaque(false);
        txtSidebarSearch.setBorder(null);
        txtSidebarSearch.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtSidebarSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm kiếm");

        searchBox.add(lblSearchIcon, BorderLayout.WEST);
        searchBox.add(txtSidebarSearch, BorderLayout.CENTER);

        pnlSearchActions = new JPanel(new GridLayout(1, 2, 2, 0));
        pnlSearchActions.setOpaque(false);

        FlatButton btnAddFriendTop = new FlatButton("", new Color(0, 0, 0, 0), ZALO_TEXT_DARK, 6);
        btnAddFriendTop.setIcon(new ZaloIcon(ZaloIcon.Type.ADD_FRIEND, 20, ZALO_ICON_GRAY));
        btnAddFriendTop.setPreferredSize(new Dimension(32, 32));
        btnAddFriendTop.setToolTipText("Thêm bạn");
        btnAddFriendTop.addActionListener(e -> showAddFriendDialog());

        FlatButton btnNewGroupTop = new FlatButton("", new Color(0, 0, 0, 0), ZALO_TEXT_DARK, 6);
        btnNewGroupTop.setIcon(new ZaloIcon(ZaloIcon.Type.ADD_GROUP, 20, ZALO_ICON_GRAY));
        btnNewGroupTop.setPreferredSize(new Dimension(32, 32));
        btnNewGroupTop.setToolTipText("Tạo nhóm chat");
        btnNewGroupTop.addActionListener(e -> showCreateGroupDialog());

        pnlSearchActions.add(btnAddFriendTop);
        pnlSearchActions.add(btnNewGroupTop);

        btnCloseSearch = new FlatButton("Đóng", new Color(0, 0, 0, 0), ZALO_TEXT_DARK, 6);
        btnCloseSearch.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCloseSearch.setPreferredSize(new Dimension(54, 32));
        btnCloseSearch.setVisible(false);
        btnCloseSearch.addActionListener(e -> exitSearchMode());

        JPanel rightSearchSlot = new JPanel(new BorderLayout());
        rightSearchSlot.setOpaque(false);
        rightSearchSlot.add(pnlSearchActions, BorderLayout.CENTER);
        rightSearchSlot.add(btnCloseSearch, BorderLayout.EAST);

        searchRow.add(searchBox, BorderLayout.CENTER);
        searchRow.add(rightSearchSlot, BorderLayout.EAST);

        txtSidebarSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { enterSearchMode(); }
        });
        txtSidebarSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { renderConversationList(); }
            public void removeUpdate(DocumentEvent e) { renderConversationList(); }
            public void changedUpdate(DocumentEvent e) { renderConversationList(); }
        });

        // Hàng 2: Thanh Tab "Tất cả" | "Trực tuyến" + nút "Lời mời (0)"
        JPanel tabRow = new JPanel(new BorderLayout());
        tabRow.setOpaque(false);
        tabRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tabRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 238, 242)));

        JPanel tabsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tabsLeft.setOpaque(false);

        lblTabAll = new JLabel("Tất cả");
        lblTabAll.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTabAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblTabAll.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                filterOnlineOnly = false;
                updateSidebarTabStyles();
                renderConversationList();
            }
        });

        lblTabOnline = new JLabel("Trực tuyến");
        lblTabOnline.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTabOnline.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblTabOnline.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                filterOnlineOnly = true;
                updateSidebarTabStyles();
                renderConversationList();
            }
        });
        updateSidebarTabStyles();

        tabsLeft.add(lblTabAll);
        tabsLeft.add(lblTabOnline);

        JPanel reqWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        reqWrap.setOpaque(false);
        btnShowRequests = new FlatButton("Lời mời (0)", ZALO_HOVER_ITEM, new Color(100, 110, 125), 8);
        btnShowRequests.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnShowRequests.setPreferredSize(new Dimension(84, 25));
        btnShowRequests.addActionListener(e -> showRequestDialog());
        reqWrap.add(btnShowRequests);

        tabRow.add(tabsLeft, BorderLayout.WEST);
        tabRow.add(reqWrap, BorderLayout.EAST);

        // Hàng 3: Nút Phòng Chat LAN Chung
        JPanel lanRow = new JPanel(new BorderLayout());
        lanRow.setOpaque(false);
        lanRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        lanRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        btnMulticast = new FlatButton("  Phòng Chat LAN Chung (Multicast)", new Color(235, 247, 240), new Color(25, 135, 84), 8);
        btnMulticast.setIcon(new ZaloIcon(ZaloIcon.Type.NAV_LAN, 16, new Color(25, 135, 84)));
        btnMulticast.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnMulticast.setPreferredSize(new Dimension(0, 34));
        btnMulticast.addActionListener(e -> activateMulticastMode());
        lanRow.add(btnMulticast, BorderLayout.CENTER);

        topSidebar.add(searchRow);
        topSidebar.add(Box.createVerticalStrut(8));
        topSidebar.add(tabRow);
        topSidebar.add(Box.createVerticalStrut(8));
        topSidebar.add(lanRow);

        pnlConversationList = new JPanel();
        pnlConversationList.setLayout(new BoxLayout(pnlConversationList, BoxLayout.Y_AXIS));
        pnlConversationList.setBackground(Color.WHITE);

        JScrollPane scrollFr = new JScrollPane(pnlConversationList);
        scrollFr.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 238, 242)));
        scrollFr.getVerticalScrollBar().setUnitIncrement(16);

        sidebar.add(topSidebar, BorderLayout.NORTH);
        sidebar.add(scrollFr, BorderLayout.CENTER);

        requestDialog = new JDialog(this, "Lời mời kết bạn", false);
        requestDialog.setSize(360, 420);
        requestDialog.setLocationRelativeTo(this);
        pnlReqList = new JPanel();
        pnlReqList.setLayout(new BoxLayout(pnlReqList, BoxLayout.Y_AXIS));
        pnlReqList.setBackground(Color.WHITE);
        requestDialog.add(new JScrollPane(pnlReqList));

        // =====================================================================
        // CỘT 3 & 4: KHUNG CHAT GIỮA + KHUNG "THÔNG TIN HỘI THOẠI" BÊN PHẢI
        // =====================================================================
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);

        chatContainer = new JPanel(new BorderLayout());

        JPanel centerChatColumn = new JPanel(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ZALO_DIVIDER));
        headerPanel.setPreferredSize(new Dimension(0, 66));

        JPanel headerLeftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        headerLeftWrap.setOpaque(false);

        targetAvatarHeader = new RoundAvatar("Z");
        targetAvatarHeader.setPreferredSize(new Dimension(44, 44));
        targetAvatarHeader.setBackground(ZALO_BLUE);
        targetAvatarHeader.setForeground(Color.WHITE);
        targetAvatarHeader.setFont(new Font("SansSerif", Font.BOLD, 17));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 2));
        headerText.setOpaque(false);
        lblTarget = new JLabel("Chào mừng đến với Z-Voice");
        lblTarget.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTarget.setForeground(ZALO_TEXT_DARK);
        lblTargetStatus = new JLabel("Chọn bạn bè hoặc tạo nhóm để bắt đầu trò chuyện");
        lblTargetStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTargetStatus.setForeground(new Color(110, 118, 130));
        headerText.add(lblTarget);
        headerText.add(lblTargetStatus);

        headerLeftWrap.add(targetAvatarHeader);
        headerLeftWrap.add(headerText);
        headerPanel.add(headerLeftWrap, BorderLayout.WEST);

        JPanel callPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 14));
        callPanel.setOpaque(false);

        btnGroupMembers = new FlatButton(" Thành viên", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnGroupMembers.setIcon(new ZaloIcon(ZaloIcon.Type.USERS, 18, ZALO_ICON_GRAY));
        btnGroupMembers.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGroupMembers.setPreferredSize(new Dimension(110, 36));
        btnGroupMembers.setVisible(false);
        btnGroupMembers.addActionListener(e -> showCurrentGroupMembers());

        btnCall = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 8);
        btnCall.setIcon(new ZaloIcon(ZaloIcon.Type.PHONE, 20, ZALO_ICON_GRAY));
        btnCall.setToolTipText("Cuộc gọi thoại");
        btnCall.setEnabled(false);
        btnCall.setPreferredSize(new Dimension(36, 36));

        btnVideoHeader = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 8);
        btnVideoHeader.setIcon(new ZaloIcon(ZaloIcon.Type.VIDEO, 20, ZALO_ICON_GRAY));
        btnVideoHeader.setToolTipText("Cuộc gọi video");
        btnVideoHeader.setEnabled(false);
        btnVideoHeader.setPreferredSize(new Dimension(36, 36));

        btnToggleInfo = new FlatButton("", ZALO_ACTIVE_ITEM, ZALO_BLUE, 8);
        btnToggleInfo.setIcon(new ZaloIcon(ZaloIcon.Type.SIDEBAR_RIGHT, 20, ZALO_BLUE));
        btnToggleInfo.setToolTipText("Thông tin hội thoại");
        btnToggleInfo.setPreferredSize(new Dimension(36, 36));
        btnToggleInfo.addActionListener(e -> toggleInfoDrawer());

        callPanel.add(btnGroupMembers);
        callPanel.add(btnCall);
        callPanel.add(btnVideoHeader);
        callPanel.add(btnToggleInfo);
        headerPanel.add(callPanel, BorderLayout.EAST);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(ZALO_CHAT_BG);
        chatPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel chatBottomPanel = new JPanel(new BorderLayout());
        chatBottomPanel.setBackground(Color.WHITE);
        chatBottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ZALO_DIVIDER));

        JPanel toolbarRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbarRow.setBackground(Color.WHITE);
        toolbarRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 242, 245)));

        btnIcon = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 6);
        btnIcon.setIcon(new ZaloIcon(ZaloIcon.Type.STICKER, 20, ZALO_ICON_GRAY));
        btnIcon.setToolTipText("Gửi nhãn dán & biểu cảm");

        btnImg = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 6);
        btnImg.setIcon(new ZaloIcon(ZaloIcon.Type.IMAGE, 20, ZALO_ICON_GRAY));
        btnImg.setToolTipText("Gửi hình ảnh");

        btnFile = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 6);
        btnFile.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 20, ZALO_ICON_GRAY));
        btnFile.setToolTipText("Đính kèm tệp tin");

        btnRecord = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 6);
        btnRecord.setIcon(new ZaloIcon(ZaloIcon.Type.MIC, 20, ZALO_ICON_GRAY));
        btnRecord.setToolTipText("Gửi tin nhắn thoại");

        Dimension iconDim = new Dimension(36, 32);
        for (FlatButton b : new FlatButton[]{btnIcon, btnImg, btnFile, btnRecord}) {
            b.setPreferredSize(iconDim);
            b.setEnabled(false);
            toolbarRow.add(b);
        }

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setBackground(Color.WHITE);
        inputRow.setBorder(new EmptyBorder(8, 14, 10, 14));

        txtChat = new JTextField();
        txtChat.setEnabled(false);
        txtChat.setBorder(null);
        txtChat.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtChat.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập @, tin nhắn tới người nhận...");

        btnSend = new FlatButton("", new Color(0, 0, 0, 0), ZALO_BLUE, 8);
        btnSend.setIcon(new ZaloIcon(ZaloIcon.Type.SEND, 21, ZALO_BLUE));
        btnSend.setToolTipText("Gửi tin nhắn");
        btnSend.setEnabled(false);
        btnSend.setPreferredSize(new Dimension(40, 36));

        inputRow.add(txtChat, BorderLayout.CENTER);
        inputRow.add(btnSend, BorderLayout.EAST);

        JPopupMenu stickerEmojiPopup = createStickerEmojiPopup();
        btnIcon.addActionListener(e -> stickerEmojiPopup.show(btnIcon, 0, -stickerEmojiPopup.getPreferredSize().height - 4));

        chatBottomPanel.add(toolbarRow, BorderLayout.NORTH);
        chatBottomPanel.add(inputRow, BorderLayout.CENTER);

        centerChatColumn.add(headerPanel, BorderLayout.NORTH);
        centerChatColumn.add(chatScroll, BorderLayout.CENTER);
        centerChatColumn.add(chatBottomPanel, BorderLayout.SOUTH);

        // =====================================================================
        // CỘT 4: KHUNG "THÔNG TIN HỘI THOẠI" & "KHO LƯU TRỮ" BÊN PHẢI
        // =====================================================================
        infoDrawerPanel = createConversationInfoDrawer();

        chatContainer.add(centerChatColumn, BorderLayout.CENTER);
        chatContainer.add(infoDrawerPanel, BorderLayout.EAST);

        // =====================================================================
        // GIAO DIỆN CUỘC GỌI THOẠI & VIDEO CALL
        // =====================================================================
        callContainer = new JLayeredPane() {
            @Override
            public void doLayout() {
                super.doLayout();
                int w = getWidth(), h = getHeight();
                if (pnlTargetVideo != null) pnlTargetVideo.setBounds(0, 0, w, h);
                if (lblMyVideo != null) lblMyVideo.setBounds(w - 180, 20, 160, 120);
                if (bottomCallPanel != null) bottomCallPanel.setBounds(0, h - 200, w, 200);
            }
        };
        callContainer.setOpaque(true);
        callContainer.setBackground(new Color(33, 33, 33));

        pnlTargetVideo = new VideoPanel();
        lblMyVideo = new JLabel();
        lblMyVideo.setOpaque(true);
        lblMyVideo.setBackground(Color.BLACK);
        lblMyVideo.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        lblMyVideo.setVisible(false);

        bottomCallPanel = new JPanel();
        bottomCallPanel.setLayout(new BoxLayout(bottomCallPanel, BoxLayout.Y_AXIS));
        bottomCallPanel.setOpaque(false);

        lblCallName = new JLabel("Tên người gọi");
        lblCallName.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblCallName.setForeground(Color.WHITE);
        lblCallName.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblCallStatus = new JLabel("Đang gọi...");
        lblCallStatus.setFont(new Font("SansSerif", Font.PLAIN, 16));
        lblCallStatus.setForeground(new Color(220, 220, 220));
        lblCallStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel callActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        callActionPanel.setOpaque(false);

        btnAcceptCall = new FlatButton("Nhấc máy", new Color(46, 204, 113), Color.WHITE, 25);
        btnAcceptCall.setPreferredSize(new Dimension(130, 48));
        btnDeclineCall = new FlatButton("Từ chối", new Color(231, 76, 60), Color.WHITE, 25);
        btnDeclineCall.setPreferredSize(new Dimension(130, 48));
        btnEndCall = new FlatButton("Kết thúc", new Color(231, 76, 60), Color.WHITE, 25);
        btnEndCall.setPreferredSize(new Dimension(130, 48));
        btnToggleVideo = new FlatButton(" Bật Cam", ZALO_BLUE, Color.WHITE, 25);
        btnToggleVideo.setIcon(new ZaloIcon(ZaloIcon.Type.VIDEO, 18, Color.WHITE));
        btnToggleVideo.setPreferredSize(new Dimension(135, 48));
        btnToggleVideo.setVisible(false);
        btnToggleVideo.addActionListener(e -> toggleVideo());

        callActionPanel.add(btnAcceptCall);
        callActionPanel.add(btnDeclineCall);
        callActionPanel.add(btnToggleVideo);
        callActionPanel.add(btnEndCall);

        bottomCallPanel.add(lblCallName);
        bottomCallPanel.add(Box.createVerticalStrut(8));
        bottomCallPanel.add(lblCallStatus);
        bottomCallPanel.add(Box.createVerticalStrut(18));
        bottomCallPanel.add(callActionPanel);

        callContainer.add(pnlTargetVideo, JLayeredPane.DEFAULT_LAYER);
        callContainer.add(lblMyVideo, JLayeredPane.PALETTE_LAYER);
        callContainer.add(bottomCallPanel, JLayeredPane.MODAL_LAYER);

        rightPanel.add(chatContainer, "CHAT");
        rightPanel.add(callContainer, "CALL");

        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.add(navRail, BorderLayout.WEST);
        leftWrapper.add(sidebar, BorderLayout.CENTER);

        add(leftWrapper, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        // Sự kiện
        btnCall.addActionListener(e -> initiateCall(false));
        btnVideoHeader.addActionListener(e -> initiateCall(true));
        btnAcceptCall.addActionListener(e -> acceptCall());
        btnDeclineCall.addActionListener(e -> declineCall());
        btnEndCall.addActionListener(e -> endCall());
        btnImg.addActionListener(e -> sendFileOrImage(8));
        btnFile.addActionListener(e -> sendFileOrImage(9));
        btnRecord.addActionListener(e -> toggleRecord());
        btnSend.addActionListener(e -> sendText());
        txtChat.addActionListener(e -> sendText());

        refreshInfoDrawer();
        startNetwork();
    }

    private void updateSidebarTabStyles() {
        if (!filterOnlineOnly) {
            lblTabAll.setForeground(ZALO_BLUE);
            lblTabAll.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, ZALO_BLUE),
                    new EmptyBorder(8, 2, 7, 2)
            ));
            lblTabOnline.setForeground(new Color(110, 118, 130));
            lblTabOnline.setBorder(new EmptyBorder(8, 2, 9, 2));
        } else {
            lblTabOnline.setForeground(ZALO_BLUE);
            lblTabOnline.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, ZALO_BLUE),
                    new EmptyBorder(8, 2, 7, 2)
            ));
            lblTabAll.setForeground(new Color(110, 118, 130));
            lblTabAll.setBorder(new EmptyBorder(8, 2, 9, 2));
        }
    }

    private void toggleInfoDrawer() {
        boolean next = !infoDrawerPanel.isVisible();
        infoDrawerPanel.setVisible(next);
        if (next) {
            btnToggleInfo.setBackground(ZALO_ACTIVE_ITEM);
            btnToggleInfo.setIcon(new ZaloIcon(ZaloIcon.Type.SIDEBAR_RIGHT, 20, ZALO_BLUE));
        } else {
            btnToggleInfo.setBackground(new Color(0, 0, 0, 0));
            btnToggleInfo.setIcon(new ZaloIcon(ZaloIcon.Type.SIDEBAR_RIGHT, 20, ZALO_ICON_GRAY));
        }
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void showInfoDrawerMain() {
        if (infoDrawerCardLayout != null && infoDrawerPanel != null) {
            infoDrawerCardLayout.show(infoDrawerPanel, "INFO");
        }
    }

    private void openArchiveDrawer(String tab) {
        activeArchiveTab = tab;
        updateArchiveTabStyles();
        refreshArchiveView();
        if (infoDrawerCardLayout != null && infoDrawerPanel != null) {
            infoDrawerCardLayout.show(infoDrawerPanel, "ARCHIVE");
        }
    }

    // =========================================================================
    // TẠO KHUNG "THÔNG TIN HỘI THOẠI" & "KHO LƯU TRỮ" GIỐNG HỆT ZALO PC
    // =========================================================================
    private JPanel createConversationInfoDrawer() {
        infoDrawerCardLayout = new CardLayout();
        JPanel drawerRoot = new JPanel(infoDrawerCardLayout);
        drawerRoot.setPreferredSize(new Dimension(300, 0));
        drawerRoot.setBackground(Color.WHITE);
        drawerRoot.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ZALO_DIVIDER));

        // ---------------------------------------------------------------------
        // THẺ 1: THÔNG TIN HỘI THOẠI
        // ---------------------------------------------------------------------
        JPanel infoCardView = new JPanel(new BorderLayout());
        infoCardView.setBackground(Color.WHITE);

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Color.WHITE);
        topHeader.setPreferredSize(new Dimension(0, 66));
        topHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ZALO_DIVIDER));

        JLabel lblTitle = new JLabel("Thông tin hội thoại", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(ZALO_TEXT_DARK);
        topHeader.add(lblTitle, BorderLayout.CENTER);

        JPanel body = new DrawerBodyPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ZALO_CHAT_BG);

        // Khối 1: Avatar lớn + Tên + 3 nút hành động nhanh
        JPanel profileCard = new JPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(new EmptyBorder(18, 14, 16, 14));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));

        infoAvatar = new RoundAvatar("Z");
        infoAvatar.setPreferredSize(new Dimension(62, 62));
        infoAvatar.setMaximumSize(new Dimension(62, 62));
        infoAvatar.setBackground(ZALO_BLUE);
        infoAvatar.setForeground(Color.WHITE);
        infoAvatar.setFont(new Font("SansSerif", Font.BOLD, 24));
        infoAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblInfoName = new JLabel("Z-Voice");
        lblInfoName.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblInfoName.setForeground(ZALO_TEXT_DARK);
        lblInfoName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel quickBtns = new JPanel(new GridLayout(1, 3, 6, 0));
        quickBtns.setOpaque(false);
        quickBtns.setMaximumSize(new Dimension(265, 74));

        lblMuteText = new JLabel("<html><center>Tắt thông<br>báo</center></html>", SwingConstants.CENTER);
        JPanel btnMute = createDrawerActionItem(ZaloIcon.Type.NAV_BELL, lblMuteText, () -> {
            String key = getActiveConversationKey();
            if (key == null) return;
            if (mutedConversations.contains(key)) mutedConversations.remove(key);
            else mutedConversations.add(key);
            refreshInfoDrawer();
        });

        lblPinText = new JLabel("<html><center>Ghim hội<br>thoại</center></html>", SwingConstants.CENTER);
        JPanel btnPin = createDrawerActionItem(ZaloIcon.Type.PIN, lblPinText, () -> {
            String key = getActiveConversationKey();
            if (key == null) return;
            if (pinnedConversations.contains(key)) pinnedConversations.remove(key);
            else pinnedConversations.add(key);
            refreshInfoDrawer();
            renderConversationList();
        });

        JLabel lblNewGrp = new JLabel("<html><center>Tạo nhóm<br>trò chuyện</center></html>", SwingConstants.CENTER);
        JPanel btnNewGrp = createDrawerActionItem(ZaloIcon.Type.ADD_GROUP, lblNewGrp, this::showCreateGroupDialog);

        quickBtns.add(btnMute);
        quickBtns.add(btnPin);
        quickBtns.add(btnNewGrp);

        profileCard.add(infoAvatar);
        profileCard.add(Box.createVerticalStrut(10));
        profileCard.add(lblInfoName);
        profileCard.add(Box.createVerticalStrut(14));
        profileCard.add(quickBtns);

        // Khối 2: Thông tin kết nối & Nhóm chung
        JPanel metaCard = new JPanel();
        metaCard.setLayout(new BoxLayout(metaCard, BoxLayout.Y_AXIS));
        metaCard.setBackground(Color.WHITE);
        metaCard.setBorder(new EmptyBorder(10, 14, 10, 14));
        metaCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        lblInfoSub1 = new JLabel("Chưa chọn hội thoại");
        lblInfoSub1.setIcon(new ZaloIcon(ZaloIcon.Type.CLOCK, 16, ZALO_ICON_GRAY));
        lblInfoSub1.setIconTextGap(10);
        lblInfoSub1.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblInfoSub1.setForeground(ZALO_TEXT_DARK);
        lblInfoSub1.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblInfoSub2 = new JLabel("0 nhóm chung");
        lblInfoSub2.setIcon(new ZaloIcon(ZaloIcon.Type.USERS, 16, ZALO_ICON_GRAY));
        lblInfoSub2.setIconTextGap(10);
        lblInfoSub2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblInfoSub2.setForeground(ZALO_TEXT_DARK);
        lblInfoSub2.setAlignmentX(Component.LEFT_ALIGNMENT);

        metaCard.add(lblInfoSub1);
        metaCard.add(Box.createVerticalStrut(12));
        metaCard.add(lblInfoSub2);

        // Khối 3: Ảnh/Video đã chia sẻ (lưới vuông sát nhau, không khung viền)
        JPanel mediaCard = new JPanel(new BorderLayout(0, 8));
        mediaCard.setBackground(Color.WHITE);
        mediaCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        mediaCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMediaTitle = new JLabel("Ảnh/Video");
        lblMediaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblMediaTitle.setForeground(ZALO_TEXT_DARK);

        pnlSharedImages = new JPanel(new GridLayout(0, 4, 4, 4));
        pnlSharedImages.setOpaque(false);

        btnViewAllMedia = new FlatButton("Xem tất cả", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnViewAllMedia.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnViewAllMedia.setPreferredSize(new Dimension(0, 32));
        btnViewAllMedia.setVisible(false);
        btnViewAllMedia.addActionListener(e -> openArchiveDrawer("MEDIA"));

        mediaCard.add(lblMediaTitle, BorderLayout.NORTH);
        mediaCard.add(pnlSharedImages, BorderLayout.CENTER);
        mediaCard.add(btnViewAllMedia, BorderLayout.SOUTH);

        // Khối 4: File đã chia sẻ
        JPanel fileCard = new JPanel(new BorderLayout(0, 8));
        fileCard.setBackground(Color.WHITE);
        fileCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        fileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblFileTitle = new JLabel("File");
        lblFileTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblFileTitle.setForeground(ZALO_TEXT_DARK);

        pnlSharedFiles = new JPanel();
        pnlSharedFiles.setLayout(new BoxLayout(pnlSharedFiles, BoxLayout.Y_AXIS));
        pnlSharedFiles.setOpaque(false);

        fileCard.add(lblFileTitle, BorderLayout.NORTH);
        fileCard.add(pnlSharedFiles, BorderLayout.CENTER);

        // Khối 5: Link đã chia sẻ
        JPanel linkCard = new JPanel(new BorderLayout(0, 8));
        linkCard.setBackground(Color.WHITE);
        linkCard.setBorder(new EmptyBorder(12, 14, 14, 14));
        linkCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLinkTitle = new JLabel("Link");
        lblLinkTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblLinkTitle.setForeground(ZALO_TEXT_DARK);

        pnlSharedLinks = new JPanel();
        pnlSharedLinks.setLayout(new BoxLayout(pnlSharedLinks, BoxLayout.Y_AXIS));
        pnlSharedLinks.setOpaque(false);

        linkCard.add(lblLinkTitle, BorderLayout.NORTH);
        linkCard.add(pnlSharedLinks, BorderLayout.CENTER);

        body.add(profileCard);
        body.add(Box.createVerticalStrut(8));
        body.add(metaCard);
        body.add(Box.createVerticalStrut(8));
        body.add(mediaCard);
        body.add(Box.createVerticalStrut(8));
        body.add(fileCard);
        body.add(Box.createVerticalStrut(8));
        body.add(linkCard);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        infoCardView.add(topHeader, BorderLayout.NORTH);
        infoCardView.add(scroll, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // THẺ 2: KHO LƯU TRỮ (GIỐNG HỆT ZALO PC)
        // ---------------------------------------------------------------------
        JPanel archiveCardView = createArchiveDrawerCard();

        drawerRoot.add(infoCardView, "INFO");
        drawerRoot.add(archiveCardView, "ARCHIVE");
        return drawerRoot;
    }

    private JPanel createArchiveDrawerCard() {
        JPanel archiveView = new JPanel(new BorderLayout());
        archiveView.setBackground(Color.WHITE);

        JPanel topBlock = new JPanel(new BorderLayout());
        topBlock.setBackground(Color.WHITE);
        topBlock.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ZALO_DIVIDER));

        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(Color.WHITE);
        navBar.setPreferredSize(new Dimension(0, 52));
        navBar.setBorder(new EmptyBorder(6, 8, 0, 14));

        FlatButton btnBack = new FlatButton("‹", new Color(0, 0, 0, 0), ZALO_TEXT_DARK, 8);
        btnBack.setFont(new Font("SansSerif", Font.PLAIN, 26));
        btnBack.setPreferredSize(new Dimension(36, 36));
        btnBack.setToolTipText("Quay lại Thông tin hội thoại");
        btnBack.addActionListener(e -> showInfoDrawerMain());

        JLabel lblArchiveTitle = new JLabel("Kho lưu trữ", SwingConstants.CENTER);
        lblArchiveTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblArchiveTitle.setForeground(ZALO_TEXT_DARK);

        JLabel dummyRight = new JLabel();
        dummyRight.setPreferredSize(new Dimension(36, 36));

        navBar.add(btnBack, BorderLayout.WEST);
        navBar.add(lblArchiveTitle, BorderLayout.CENTER);
        navBar.add(dummyRight, BorderLayout.EAST);

        JPanel tabBar = new JPanel(new GridLayout(1, 3, 0, 0));
        tabBar.setBackground(Color.WHITE);
        tabBar.setPreferredSize(new Dimension(0, 38));

        lblArchiveTabMedia = createArchiveTabLabel("Ảnh/Video", "MEDIA");
        lblArchiveTabFiles = createArchiveTabLabel("Files", "FILES");
        lblArchiveTabLinks = createArchiveTabLabel("Links", "LINKS");

        tabBar.add(lblArchiveTabMedia);
        tabBar.add(lblArchiveTabFiles);
        tabBar.add(lblArchiveTabLinks);

        topBlock.add(navBar, BorderLayout.NORTH);
        topBlock.add(tabBar, BorderLayout.SOUTH);

        pnlArchiveContent = new DrawerBodyPanel();
        pnlArchiveContent.setLayout(new BoxLayout(pnlArchiveContent, BoxLayout.Y_AXIS));
        pnlArchiveContent.setBackground(ZALO_CHAT_BG);

        JScrollPane archiveScroll = new JScrollPane(pnlArchiveContent);
        archiveScroll.setBorder(null);
        archiveScroll.getVerticalScrollBar().setUnitIncrement(16);
        archiveScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        archiveView.add(topBlock, BorderLayout.NORTH);
        archiveView.add(archiveScroll, BorderLayout.CENTER);
        return archiveView;
    }

    private JLabel createArchiveTabLabel(String text, String tabKey) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activeArchiveTab = tabKey;
                updateArchiveTabStyles();
                refreshArchiveView();
            }
        });
        return lbl;
    }

    private void updateArchiveTabStyles() {
        if (lblArchiveTabMedia == null) return;
        styleSingleArchiveTab(lblArchiveTabMedia, "MEDIA".equals(activeArchiveTab));
        styleSingleArchiveTab(lblArchiveTabFiles, "FILES".equals(activeArchiveTab));
        styleSingleArchiveTab(lblArchiveTabLinks, "LINKS".equals(activeArchiveTab));
    }

    private void styleSingleArchiveTab(JLabel lbl, boolean active) {
        if (active) {
            lbl.setForeground(ZALO_BLUE);
            lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ZALO_BLUE));
        } else {
            lbl.setForeground(new Color(110, 118, 130));
            lbl.setBorder(new EmptyBorder(0, 0, 2, 0));
        }
    }

    private void refreshArchiveView() {
        if (pnlArchiveContent == null) return;
        pnlArchiveContent.removeAll();

        if ("MEDIA".equals(activeArchiveTab)) {
            if (currentSharedImages.isEmpty()) {
                JPanel emptyWrap = new JPanel(new BorderLayout());
                emptyWrap.setBackground(Color.WHITE);
                emptyWrap.setBorder(new EmptyBorder(40, 14, 40, 14));
                emptyWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
                emptyWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                JLabel empty = new JLabel("Chưa có Ảnh/Video được chia sẻ", SwingConstants.CENTER);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
                empty.setForeground(new Color(140, 148, 160));
                emptyWrap.add(empty, BorderLayout.CENTER);
                pnlArchiveContent.add(emptyWrap);
            } else {
                Map<String, List<Integer>> byDate = new LinkedHashMap<>();
                for (int i = 0; i < currentSharedImages.size(); i++) {
                    String d = currentSharedImages.get(i).dateLabel;
                    byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(i);
                }

                int cellSize = 88;
                for (Map.Entry<String, List<Integer>> entry : byDate.entrySet()) {
                    JPanel daySection = new JPanel(new BorderLayout(0, 8));
                    daySection.setBackground(Color.WHITE);
                    daySection.setBorder(new EmptyBorder(12, 12, 12, 12));
                    daySection.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JLabel lblDay = new JLabel(entry.getKey());
                    lblDay.setFont(new Font("SansSerif", Font.BOLD, 13));
                    lblDay.setForeground(ZALO_TEXT_DARK);

                    JPanel grid = new JPanel(new GridLayout(0, 3, 4, 4));
                    grid.setOpaque(false);

                    List<Integer> indices = entry.getValue();
                    for (int imgIdx : indices) {
                        BufferedImage raw = currentSharedImages.get(imgIdx).image;
                        BufferedImage thumb = createSquareCoverThumbnail(raw, cellSize, 6);
                        JLabel pic = new JLabel(new ImageIcon(thumb));
                        pic.setPreferredSize(new Dimension(cellSize, cellSize));
                        pic.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        pic.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                showImagePreviewDialog(imgIdx);
                            }
                        });
                        grid.add(pic);
                    }

                    int rem = indices.size() % 3;
                    if (rem != 0) {
                        for (int k = 0; k < 3 - rem; k++) {
                            JPanel spacer = new JPanel();
                            spacer.setOpaque(false);
                            spacer.setPreferredSize(new Dimension(cellSize, cellSize));
                            grid.add(spacer);
                        }
                    }

                    int rows = (int) Math.ceil(indices.size() / 3.0);
                    int gridH = rows * cellSize + Math.max(0, rows - 1) * 4;
                    int sectionH = 24 + 8 + 18 + gridH;
                    daySection.setMaximumSize(new Dimension(Integer.MAX_VALUE, sectionH));

                    daySection.add(lblDay, BorderLayout.NORTH);
                    daySection.add(grid, BorderLayout.CENTER);

                    pnlArchiveContent.add(daySection);
                    pnlArchiveContent.add(Box.createVerticalStrut(6));
                }
            }
        } else if ("FILES".equals(activeArchiveTab)) {
            JPanel fileSection = new JPanel();
            fileSection.setLayout(new BoxLayout(fileSection, BoxLayout.Y_AXIS));
            fileSection.setBackground(Color.WHITE);
            fileSection.setBorder(new EmptyBorder(12, 12, 12, 12));
            fileSection.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (currentSharedFiles.isEmpty()) {
                JLabel empty = new JLabel("Chưa có File được chia sẻ");
                empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
                empty.setForeground(new Color(140, 148, 160));
                fileSection.add(empty);
            } else {
                for (SharedFileItem item : currentSharedFiles) {
                    FlatButton fBtn = new FlatButton(" " + item.name, ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 6);
                    fBtn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 16, ZALO_BLUE));
                    fBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
                    fBtn.setHorizontalAlignment(SwingConstants.LEFT);
                    fBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                    fBtn.addActionListener(e -> saveSharedFileToDisk(item.name, item.data));
                    fileSection.add(fBtn);
                    fileSection.add(Box.createVerticalStrut(6));
                }
            }
            pnlArchiveContent.add(fileSection);
        } else {
            JPanel linkSection = new JPanel();
            linkSection.setLayout(new BoxLayout(linkSection, BoxLayout.Y_AXIS));
            linkSection.setBackground(Color.WHITE);
            linkSection.setBorder(new EmptyBorder(12, 12, 12, 12));
            linkSection.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (currentSharedLinks.isEmpty()) {
                JLabel empty = new JLabel("Chưa có Link được chia sẻ");
                empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
                empty.setForeground(new Color(140, 148, 160));
                linkSection.add(empty);
            } else {
                for (String url : currentSharedLinks) {
                    JLabel lblUrl = new JLabel(url);
                    lblUrl.setIcon(new ZaloIcon(ZaloIcon.Type.LINK, 15, ZALO_BLUE));
                    lblUrl.setIconTextGap(8);
                    lblUrl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    lblUrl.setForeground(ZALO_BLUE);
                    lblUrl.setBorder(new EmptyBorder(5, 0, 5, 0));
                    linkSection.add(lblUrl);
                }
            }
            pnlArchiveContent.add(linkSection);
        }

        pnlArchiveContent.add(Box.createVerticalGlue());
        pnlArchiveContent.revalidate();
        pnlArchiveContent.repaint();
    }

    private BufferedImage createSquareCoverThumbnail(BufferedImage src, int size, int arc) {
        int w = src.getWidth();
        int h = src.getHeight();
        int crop = Math.min(w, h);
        int x = (w - crop) / 2;
        int y = (h - crop) / 2;
        BufferedImage cropped = src.getSubimage(x, y, crop, crop);

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0, 0, size, size, arc, arc));
        g2.drawImage(cropped, 0, 0, size, size, null);
        g2.dispose();
        return out;
    }

    private String formatDateGroup(long millis) {
        return new SimpleDateFormat("'Ngày' d 'Tháng' M").format(new Date(millis));
    }

    private long extractTimestampFromLocalName(String localName) {
        try {
            if (localName != null && localName.startsWith("img_")) {
                int dot = localName.indexOf('.');
                String num = dot != -1 ? localName.substring(4, dot) : localName.substring(4);
                return Long.parseLong(num);
            }
        } catch (Exception e) {}
        return System.currentTimeMillis();
    }

    private JPanel createDrawerActionItem(ZaloIcon.Type iconType, JLabel captionLabel, Runnable action) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setCursor(new Cursor(Cursor.HAND_CURSOR));

        FlatButton circleBtn = new FlatButton("", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 999);
        circleBtn.setIcon(new ZaloIcon(iconType, 18, ZALO_TEXT_DARK));
        circleBtn.setPreferredSize(new Dimension(38, 38));
        circleBtn.setMaximumSize(new Dimension(38, 38));
        circleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        circleBtn.addActionListener(e -> action.run());

        captionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        captionLabel.setForeground(new Color(90, 100, 115));
        captionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(circleBtn);
        box.add(Box.createVerticalStrut(5));
        box.add(captionLabel);
        return box;
    }

    private String getActiveConversationKey() {
        if (isMulticastMode) return "LAN";
        if (currentGroupId != -1) return "GRP_" + currentGroupId;
        if (currentTargetUser != null) return "USR_" + currentTargetUser;
        return null;
    }

    private void resetSharedMediaState() {
        currentSharedImages.clear();
        currentSharedFiles.clear();
        currentSharedLinks.clear();
        refreshInfoDrawer();
    }

    private void extractLinksFromText(String text) {
        if (text == null) return;
        Matcher m = Pattern.compile("(https?://\\S+|www\\.\\S+)").matcher(text);
        boolean added = false;
        while (m.find()) {
            currentSharedLinks.add(0, m.group(1));
            added = true;
        }
        if (added) refreshInfoDrawer();
    }

    private void refreshInfoDrawer() {
        if (infoDrawerPanel == null) return;

        String key = getActiveConversationKey();
        boolean isMuted = key != null && mutedConversations.contains(key);
        boolean isPinned = key != null && pinnedConversations.contains(key);

        lblMuteText.setText(isMuted ? "<html><center>Bật thông<br>báo</center></html>" : "<html><center>Tắt thông<br>báo</center></html>");
        lblMuteText.setForeground(isMuted ? ZALO_BLUE : new Color(90, 100, 115));

        lblPinText.setText(isPinned ? "<html><center>Bỏ ghim<br>hội thoại</center></html>" : "<html><center>Ghim hội<br>thoại</center></html>");
        lblPinText.setForeground(isPinned ? ZALO_BLUE : new Color(90, 100, 115));

        if (isMulticastMode) {
            infoAvatar.setImage(null);
            infoAvatar.setAvatarText("LAN");
            infoAvatar.setBackground(new Color(25, 135, 84));
            lblInfoName.setText("Phòng Chat LAN Chung");
            lblInfoSub1.setText("Multicast UDP • 224.0.0.1:9999");
            lblInfoSub2.setText("Toàn bộ máy trong mạng nội bộ");
        } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
            Group g = groups.get(currentGroupId);
            infoAvatar.setImage(null);
            infoAvatar.setAvatarText(g.name.substring(0, 1).toUpperCase());
            infoAvatar.setBackground(new Color(0, 150, 136));
            lblInfoName.setText(g.name);
            lblInfoSub1.setText("Nhóm chat riêng tư");
            lblInfoSub2.setText(g.members.size() + " thành viên trong nhóm");
        } else if (currentTargetUser != null && friends.containsKey(currentTargetUser)) {
            Friend f = friends.get(currentTargetUser);
            infoAvatar.setBackground(ZALO_BLUE);
            if (avatarCache.containsKey(f.username)) {
                infoAvatar.setImage(avatarCache.get(f.username));
                infoAvatar.setAvatarText("");
            } else {
                infoAvatar.setImage(null);
                infoAvatar.setAvatarText(f.name.substring(0, 1).toUpperCase());
            }
            lblInfoName.setText(f.name);
            lblInfoSub1.setText(f.isOnline() ? "IP P2P: " + f.ip + ":" + f.port : "Đang ngoại tuyến");

            int mutualGroups = 0;
            for (Group g : groups.values()) {
                for (Friend m : g.members) {
                    if (m.username.equals(f.username)) { mutualGroups++; break; }
                }
            }
            lblInfoSub2.setText(mutualGroups + " nhóm chung");
        } else {
            infoAvatar.setImage(null);
            infoAvatar.setAvatarText("Z");
            infoAvatar.setBackground(ZALO_BLUE);
            lblInfoName.setText("Z-Voice P2P");
            lblInfoSub1.setText("Chưa chọn hội thoại");
            lblInfoSub2.setText("0 nhóm chung");
        }

        // Cập nhật lưới Ảnh/Video
        pnlSharedImages.removeAll();
        if (currentSharedImages.isEmpty()) {
            JLabel emptyImg = new JLabel("Chưa có Ảnh/Video được chia sẻ");
            emptyImg.setFont(new Font("SansSerif", Font.PLAIN, 12));
            emptyImg.setForeground(new Color(140, 148, 160));
            pnlSharedImages.setLayout(new BorderLayout());
            pnlSharedImages.add(emptyImg, BorderLayout.CENTER);
            if (btnViewAllMedia != null) btnViewAllMedia.setVisible(false);
        } else {
            pnlSharedImages.setLayout(new GridLayout(0, 4, 4, 4));
            int limit = Math.min(8, currentSharedImages.size());
            int thumbSize = 64;
            for (int i = 0; i < limit; i++) {
                final int imgIndex = i;
                BufferedImage img = currentSharedImages.get(i).image;
                BufferedImage thumb = createSquareCoverThumbnail(img, thumbSize, 6);
                JLabel lblThumb = new JLabel(new ImageIcon(thumb));
                lblThumb.setPreferredSize(new Dimension(thumbSize, thumbSize));
                lblThumb.setCursor(new Cursor(Cursor.HAND_CURSOR));
                lblThumb.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        showImagePreviewDialog(imgIndex);
                    }
                });
                pnlSharedImages.add(lblThumb);
            }
            int rem = limit % 4;
            if (rem != 0) {
                for (int k = 0; k < 4 - rem; k++) {
                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    spacer.setPreferredSize(new Dimension(thumbSize, thumbSize));
                    pnlSharedImages.add(spacer);
                }
            }
            if (btnViewAllMedia != null) btnViewAllMedia.setVisible(true);
        }

        // Cập nhật danh sách File
        pnlSharedFiles.removeAll();
        if (currentSharedFiles.isEmpty()) {
            JLabel emptyFile = new JLabel("Chưa có File được chia sẻ");
            emptyFile.setFont(new Font("SansSerif", Font.PLAIN, 12));
            emptyFile.setForeground(new Color(140, 148, 160));
            pnlSharedFiles.add(emptyFile);
        } else {
            int limit = Math.min(5, currentSharedFiles.size());
            for (int i = 0; i < limit; i++) {
                SharedFileItem item = currentSharedFiles.get(i);
                FlatButton fBtn = new FlatButton(" " + item.name, ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 6);
                fBtn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 16, ZALO_BLUE));
                fBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
                fBtn.setHorizontalAlignment(SwingConstants.LEFT);
                fBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
                fBtn.addActionListener(e -> saveSharedFileToDisk(item.name, item.data));
                pnlSharedFiles.add(fBtn);
                pnlSharedFiles.add(Box.createVerticalStrut(4));
            }
        }

        // Cập nhật danh sách Link
        pnlSharedLinks.removeAll();
        if (currentSharedLinks.isEmpty()) {
            JLabel emptyLink = new JLabel("Chưa có Link được chia sẻ");
            emptyLink.setFont(new Font("SansSerif", Font.PLAIN, 12));
            emptyLink.setForeground(new Color(140, 148, 160));
            pnlSharedLinks.add(emptyLink);
        } else {
            int limit = Math.min(5, currentSharedLinks.size());
            for (int i = 0; i < limit; i++) {
                String url = currentSharedLinks.get(i);
                JLabel lblUrl = new JLabel(url);
                lblUrl.setIcon(new ZaloIcon(ZaloIcon.Type.LINK, 15, ZALO_BLUE));
                lblUrl.setIconTextGap(8);
                lblUrl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblUrl.setForeground(ZALO_BLUE);
                lblUrl.setBorder(new EmptyBorder(3, 0, 3, 0));
                pnlSharedLinks.add(lblUrl);
            }
        }

        refreshArchiveView();
        infoDrawerPanel.revalidate();
        infoDrawerPanel.repaint();
    }

    private FlatButton createRailButton(ZaloIcon.Type iconType, String tooltip, Color bg) {
        FlatButton btn = new FlatButton("", bg, Color.WHITE, 12);
        btn.setIcon(new ZaloIcon(iconType, 22, Color.WHITE));
        btn.setPreferredSize(new Dimension(46, 46));
        btn.setMaximumSize(new Dimension(46, 46));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setToolTipText(tooltip);
        return btn;
    }

    private JPopupMenu createStickerEmojiPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230), 1, true));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.setPreferredSize(new Dimension(320, 235));

        JPanel stickerGrid = new JPanel(new GridLayout(2, 4, 8, 8));
        stickerGrid.setBackground(Color.WHITE);
        stickerGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (String stkId : StickerPanel.STICKER_IDS) {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(Color.WHITE);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            StickerPanel sp = new StickerPanel(stkId, 64);
            cell.add(sp);
            cell.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { cell.setBackground(ZALO_HOVER_ITEM); }
                public void mouseExited(MouseEvent e) { cell.setBackground(Color.WHITE); }
                public void mouseClicked(MouseEvent e) {
                    popup.setVisible(false);
                    sendRawMessage("[STK:" + stkId + "]");
                }
            });
            stickerGrid.add(cell);
        }

        JPanel emojiGrid = new JPanel(new GridLayout(0, 6, 4, 4));
        emojiGrid.setBackground(Color.WHITE);
        emojiGrid.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] emojis = {
                "😀", "😂", "🥰", "😍", "😭", "👍",
                "❤️", "🔥", "✨", "🎉", "😢", "😡",
                "🙏", "😱", "😎", "🥳", "😴", "🤔",
                "👏", "💯", "🌹", "🍻", "👌", "😜"
        };
        Font colorEmojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 22);
        for (String em : emojis) {
            JButton btnE = new JButton(em);
            btnE.setFont(colorEmojiFont);
            btnE.setBorder(null);
            btnE.setBackground(Color.WHITE);
            btnE.setFocusPainted(false);
            btnE.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnE.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btnE.setBackground(ZALO_HOVER_ITEM); }
                public void mouseExited(MouseEvent e) { btnE.setBackground(Color.WHITE); }
            });
            btnE.addActionListener(ev -> {
                txtChat.setText(txtChat.getText() + em);
                popup.setVisible(false);
                txtChat.requestFocus();
            });
            emojiGrid.add(btnE);
        }

        tabs.addTab("Nhãn dán", stickerGrid);
        tabs.addTab("Emoji", emojiGrid);
        popup.add(tabs);
        return popup;
    }

    private void enterSearchMode() {
        if (isSearchMode) return;
        isSearchMode = true;
        pnlSearchActions.setVisible(false);
        btnCloseSearch.setVisible(true);
        renderConversationList();
    }

    private void exitSearchMode() {
        isSearchMode = false;
        txtSidebarSearch.setText("");
        btnCloseSearch.setVisible(false);
        pnlSearchActions.setVisible(true);
        renderConversationList();
    }

    private void showAddFriendDialog() {
        JDialog dialog = new JDialog(this, "Thêm bạn", true);
        dialog.setSize(390, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(16, 18, 12, 18));

        JLabel lblHint = new JLabel("Nhập số điện thoại hoặc tên tài khoản để tìm kiếm");
        lblHint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblHint.setForeground(new Color(90, 100, 115));

        JTextField txtQuery = new JTextField();
        txtQuery.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Số điện thoại hoặc username...");
        txtQuery.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtQuery.setPreferredSize(new Dimension(300, 38));

        top.add(lblHint, BorderLayout.NORTH);
        top.add(txtQuery, BorderLayout.CENTER);

        pnlAddFriendResult = new JPanel(new BorderLayout());
        pnlAddFriendResult.setBackground(Color.WHITE);
        pnlAddFriendResult.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel lblEmpty = new JLabel("Nhập thông tin và nhấn Tìm kiếm", SwingConstants.CENTER);
        lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblEmpty.setForeground(new Color(140, 148, 160));
        pnlAddFriendResult.add(lblEmpty, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 238, 242)));

        FlatButton btnCancel = new FlatButton("Hủy", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnCancel.setPreferredSize(new Dimension(85, 36));
        btnCancel.addActionListener(e -> dialog.dispose());

        FlatButton btnDoSearch = new FlatButton("Tìm kiếm", ZALO_BLUE, Color.WHITE, 8);
        btnDoSearch.setPreferredSize(new Dimension(105, 36));

        Runnable doSearchAction = () -> {
            String q = txtQuery.getText().trim();
            if (q.isEmpty()) return;
            pnlAddFriendResult.removeAll();
            JLabel searching = new JLabel("Đang tìm kiếm...", SwingConstants.CENTER);
            searching.setForeground(Color.GRAY);
            pnlAddFriendResult.add(searching, BorderLayout.CENTER);
            pnlAddFriendResult.revalidate();
            pnlAddFriendResult.repaint();
            sendToServer("SEARCH;;;" + q);
        };

        btnDoSearch.addActionListener(e -> doSearchAction.run());
        txtQuery.addActionListener(e -> doSearchAction.run());

        bottom.add(btnCancel);
        bottom.add(btnDoSearch);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(pnlAddFriendResult, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
        pnlAddFriendResult = null;
    }

    private void showCreateGroupDialog() {
        if (friends.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn cần có ít nhất 1 người bạn để tạo nhóm!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Tạo nhóm", true);
        dialog.setSize(400, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(16, 18, 12, 18));

        JLabel lblTitle = new JLabel("Đặt tên nhóm và chọn thành viên");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(ZALO_TEXT_DARK);

        JTextField txtGroupName = new JTextField();
        txtGroupName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên nhóm (VD: Nhóm Đồ Án LTM)...");
        txtGroupName.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtGroupName.setPreferredSize(new Dimension(300, 38));

        top.add(lblTitle, BorderLayout.NORTH);
        top.add(txtGroupName, BorderLayout.CENTER);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();
        for (Friend f : friends.values()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(8, 18, 8, 18));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

            RoundAvatar av = new RoundAvatar(f.name.substring(0, 1).toUpperCase());
            av.setPreferredSize(new Dimension(36, 36));
            av.setBackground(ZALO_BLUE);
            av.setForeground(Color.WHITE);
            av.setFont(new Font("SansSerif", Font.BOLD, 14));
            if (avatarCache.containsKey(f.username)) {
                av.setImage(avatarCache.get(f.username));
                av.setAvatarText("");
            }

            JCheckBox cb = new JCheckBox(f.name + " (@" + f.username + ")");
            cb.setFont(new Font("SansSerif", Font.PLAIN, 14));
            cb.setOpaque(false);
            checkBoxes.put(f.username, cb);

            row.add(av, BorderLayout.WEST);
            row.add(cb, BorderLayout.CENTER);
            listPanel.add(row);
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(230, 233, 238)));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottom.setBackground(Color.WHITE);

        FlatButton btnCancel = new FlatButton("Hủy", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnCancel.setPreferredSize(new Dimension(80, 36));
        btnCancel.addActionListener(e -> dialog.dispose());

        FlatButton btnConfirm = new FlatButton("Tạo nhóm", ZALO_BLUE, Color.WHITE, 8);
        btnConfirm.setPreferredSize(new Dimension(105, 36));
        btnConfirm.addActionListener(e -> {
            String gName = txtGroupName.getText().trim();
            if (gName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên nhóm!");
                return;
            }
            List<String> selected = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
                if (entry.getValue().isSelected()) selected.add(entry.getKey());
            }
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Hãy chọn ít nhất 1 thành viên vào nhóm!");
                return;
            }
            String csv = String.join(",", selected);
            sendToServer("CREATEGROUP;;;" + gName + ";;;" + currentUser + ";;;" + csv);
            lastDataSync = "";
            sendToServer("GETDATA;;;" + currentUser);
            dialog.dispose();
        });

        bottom.add(btnCancel);
        bottom.add(btnConfirm);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showCurrentGroupMembers() {
        if (currentGroupId == -1 || !groups.containsKey(currentGroupId)) return;
        Group g = groups.get(currentGroupId);
        StringBuilder sb = new StringBuilder("Danh sách thành viên nhóm \"" + g.name + "\":\n\n");
        for (Friend m : g.members) {
            String st = m.username.equals(currentUser) ? "Bạn (Đang hoạt động)" : (m.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
            sb.append("• ").append(m.name).append(" (@").append(m.username).append(") - ").append(st).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Thành viên nhóm", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateAvatarDisplay() {
        try {
            if (currentAvatarBase64 != null && !currentAvatarBase64.isEmpty()) {
                byte[] imgBytes = Base64.getDecoder().decode(currentAvatarBase64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                avatarLabel.setImage(img);
                avatarLabel.setAvatarText("");
            } else {
                avatarLabel.setImage(null);
                avatarLabel.setAvatarText(currentUser.substring(0, 1).toUpperCase());
            }
        } catch (Exception e) {}
    }

    private void showProfileDialog() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        JTextField nameField = new JTextField(currentFullname);
        JButton btnPickImg = new JButton("Chọn ảnh đại diện");
        final String[] newAvatar = {currentAvatarBase64};
        btnPickImg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(fc.getSelectedFile());
                    Image scaled = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                    BufferedImage bimg = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = bimg.createGraphics();
                    g2d.drawImage(scaled, 0, 0, null);
                    g2d.dispose();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bimg, "jpg", baos);
                    newAvatar[0] = Base64.getEncoder().encodeToString(baos.toByteArray());
                    JOptionPane.showMessageDialog(this, "Đã nạp ảnh");
                } catch (Exception ex) {}
            }
        });
        p.add(new JLabel("Tên hiển thị:"), BorderLayout.WEST);
        p.add(nameField, BorderLayout.CENTER);
        p.add(btnPickImg, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, p, "Cập nhật hồ sơ Zalo", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            sendToServer("UPDATE;;;" + currentUser + ";;;" + nameField.getText() + ";;;" + newAvatar[0]);
            currentFullname = nameField.getText();
            currentAvatarBase64 = newAvatar[0];
            updateAvatarDisplay();
            setTitle("Z-Voice - " + currentFullname);
        }
    }

    private void showRequestDialog() { requestDialog.setVisible(true); }

    private void startNetwork() {
        try {
            udpSocket = new DatagramSocket();
            isRunning = true;
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            speaker = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
            speaker.open(format);
            speaker.start();

            new Thread(this::listenData).start();
            new Thread(this::listenForBroadcast).start();

            timer = new Timer(3000, e -> {
                sendToServer("ONLINE;;;" + currentUser);
                sendToServer("GETDATA;;;" + currentUser);
            });
            timer.start();
            sendToServer("ONLINE;;;" + currentUser);
            sendToServer("GETDATA;;;" + currentUser);
        } catch (Exception e) {}
    }

    private void sendToServer(String msg) {
        try {
            byte[] data = msg.getBytes("UTF-8");
            udpSocket.send(new DatagramPacket(data, data.length, InetAddress.getByName(LoginClient.SERVER_IP), 8080));
        } catch (Exception e) {}
    }

    private void sendP2P(Friend f, byte[] data) {
        if (f == null || !f.isOnline()) return;
        try {
            udpSocket.send(new DatagramPacket(data, data.length, InetAddress.getByName(f.ip), f.port));
        } catch (Exception e) {}
    }

    private Friend getFriendByIp(String ip, int port) {
        for (Friend f : friends.values()) {
            if (f.ip.equals(ip) && f.port == port) return f;
        }
        for (Friend f : friends.values()) {
            if (f.ip.equals(ip)) return f;
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
                    int type = buffer[0];
                    Friend senderF = getFriendByIp(packet.getAddress().getHostAddress(), packet.getPort());

                    if (type == 0 && isCalling && speaker != null) {
                        speaker.write(buffer, 1, packet.getLength() - 1);
                    }
                    else if (type == 10 && isCalling) {
                        byte[] imgBytes = new byte[packet.getLength() - 1];
                        System.arraycopy(buffer, 1, imgBytes, 0, imgBytes.length);
                        SwingUtilities.invokeLater(() -> {
                            try {
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                                pnlTargetVideo.setFrame(img);
                            } catch (Exception e) {}
                        });
                    }
                    else if (type == 1 && senderF != null) {
                        String text = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        SwingUtilities.invokeLater(() -> {
                            if (!isMulticastMode && currentGroupId == -1 && currentTargetUser != null && currentTargetUser.equals(senderF.username)) {
                                appendMsg(senderF.username, senderF.name, text, false);
                            }
                        });
                    }
                    else if (type == 2) {
                        String raw = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        String[] p = raw.split(":::", 4);
                        if (p.length == 4) {
                            int gId = Integer.parseInt(p[0]);
                            String sUser = p[1];
                            String sName = p[2];
                            String content = p[3];
                            if (!sUser.equals(currentUser)) {
                                SwingUtilities.invokeLater(() -> {
                                    if (!isMulticastMode && currentGroupId == gId) {
                                        appendMsg(sUser, sName, content, false);
                                    }
                                });
                            }
                        }
                    }
                    else if ((type == 3 || type == 8 || type == 9) && senderF != null) {
                        ByteBuffer bb = ByteBuffer.wrap(buffer, 1, packet.getLength() - 1);
                        long id = bb.getLong(); int total = bb.getInt(); int offset = bb.getInt(); int dataLen = packet.getLength() - 17;
                        if (!incomingData.containsKey(id)) { incomingData.put(id, new byte[total]); incomingDataReceived.put(id, 0); }
                        byte[] fullData = incomingData.get(id); System.arraycopy(buffer, 17, fullData, offset, dataLen);
                        int received = incomingDataReceived.get(id) + dataLen; incomingDataReceived.put(id, received);

                        if (received >= total) {
                            byte[] finalData = fullData.clone();
                            if (type == 8) {
                                saveMediaToLocal("img_" + id + ".dat", finalData);
                            } else if (type == 9) {
                                try {
                                    ByteBuffer fbb = ByteBuffer.wrap(finalData);
                                    int nLen = fbb.getInt();
                                    byte[] nBytes = new byte[nLen];
                                    fbb.get(nBytes);
                                    String fName = new String(nBytes, "UTF-8");
                                    byte[] fBytes = new byte[finalData.length - 4 - nLen];
                                    fbb.get(fBytes);
                                    saveMediaToLocal("file_" + id + "_" + fName, fBytes);
                                } catch (Exception ex) {}
                            }

                            SwingUtilities.invokeLater(() -> {
                                if (!isMulticastMode && currentGroupId == -1 && currentTargetUser != null && currentTargetUser.equals(senderF.username)) {
                                    if (type == 3) appendAudioMsg(senderF.username, senderF.name, finalData, false);
                                    else if (type == 8) appendImageMsg(senderF.username, senderF.name, finalData, false, id);
                                    else if (type == 9) appendFileMsg(senderF.username, senderF.name, finalData, false);
                                }
                            });
                            incomingData.remove(id); incomingDataReceived.remove(id);
                        }
                    }
                    else if (type == 4 && senderF != null) SwingUtilities.invokeLater(() -> handleIncomingCall(senderF.username));
                    else if (type == 5) SwingUtilities.invokeLater(this::handleCallAccepted);
                    else if (type == 6) SwingUtilities.invokeLater(this::handleCallDeclined);
                    else if (type == 7) SwingUtilities.invokeLater(this::handleCallEnded);
                }
            } catch (Exception e) {}
        }
    }

    private void processServerMsg(String s) {
        if (s.startsWith("CREATEGROUP_OK;;;")) {
            String[] p = s.split(";;;");
            if (p.length >= 3) {
                JOptionPane.showMessageDialog(this, "Đã tạo nhóm \"" + p[2] + "\" thành công!", "Z-Voice Nhóm", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        if (s.startsWith("GROUPMSGDATA;;;")) {
            String[] p = s.split(";;;", 3);
            if (p.length < 3) return;
            int gId = Integer.parseInt(p[1]);
            if (isMulticastMode || currentGroupId != gId) return;

            String data = p[2];
            chatPanel.removeAll();
            resetSharedMediaState();
            if (!data.isEmpty()) {
                for (String m : data.split("\\|\\|")) {
                    if (m.isEmpty()) continue;
                    int idx = m.indexOf("::");
                    if (idx != -1) {
                        String senderInfo = m.substring(0, idx);
                        String content = m.substring(idx + 2);
                        String[] sParts = senderInfo.split("\\^", 2);
                        String senderUsername = sParts[0];
                        String senderName = sParts.length > 1 ? sParts[1] : senderUsername;
                        boolean isMe = senderUsername.equals(currentUser);
                        appendMsg(senderUsername, senderName, content, isMe);
                    }
                }
            }
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollChatToBottom();
            return;
        }

        if (s.startsWith("MSGDATA;;;")) {
            if (isMulticastMode || currentGroupId != -1) return;

            String data = s.substring(10);
            chatPanel.removeAll();
            resetSharedMediaState();
            if (!data.isEmpty()) {
                for (String m : data.split("\\|\\|")) {
                    if (m.isEmpty()) continue;
                    int idx = m.indexOf("::");
                    if (idx != -1) {
                        String senderUsername = m.substring(0, idx);
                        String content = m.substring(idx + 2);
                        boolean isMe = senderUsername.equals(currentUser);
                        String senderName = !isMe && friends.containsKey(senderUsername) ? friends.get(senderUsername).name : "";
                        appendMsg(senderUsername, senderName, content, isMe);
                    }
                }
            }
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollChatToBottom();
            return;
        }

        if (s.startsWith("DATA;;;")) {
            if (s.equals(lastDataSync)) return;
            lastDataSync = s;
            String[] parts = s.substring(7).split("###", -1);

            friends.clear();
            groups.clear();

            if (parts.length > 2 && !parts[2].isEmpty()) {
                for (String gStr : parts[2].split("\\|")) {
                    if (gStr.isEmpty()) continue;
                    String[] gParts = gStr.split("~", -1);
                    if (gParts.length >= 3) {
                        int gId = Integer.parseInt(gParts[0]);
                        String gName = gParts[1];
                        Group grp = new Group(gId, gName);

                        for (String memStr : gParts[2].split(",")) {
                            if (memStr.isEmpty()) continue;
                            String[] mData = memStr.split("\\^");
                            if (mData.length >= 4) {
                                grp.members.add(new Friend(mData[0], mData[1], mData[2], Integer.parseInt(mData[3])));
                            }
                        }
                        groups.put(gId, grp);
                    }
                }
            }

            if (parts.length > 0 && !parts[0].isEmpty()) {
                for (String f : parts[0].split("\\|")) {
                    if (f.isEmpty()) continue;
                    String[] d = f.split(":");
                    Friend friendObj = new Friend(d[0], d[1], d[2], Integer.parseInt(d[3]));
                    friends.put(d[0], friendObj);

                    if (d.length > 4 && !d[4].equals("null") && !d[4].isEmpty()) {
                        try {
                            byte[] imgBytes = Base64.getDecoder().decode(d[4]);
                            BufferedImage frImg = ImageIO.read(new ByteArrayInputStream(imgBytes));
                            avatarCache.put(d[0], frImg);
                        } catch (Exception ex) {}
                    } else {
                        avatarCache.remove(d[0]);
                    }
                }
            }

            renderConversationList();
            refreshInfoDrawer();

            pnlReqList.removeAll();
            int reqCount = 0;
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String[] reqs = parts[1].split("\\|");
                reqCount = reqs.length;
                for (String r : reqs) {
                    if (r.isEmpty()) continue;
                    String reqUser = r.split(":")[0];
                    String reqName = r.split(":")[1];

                    JPanel item = new JPanel(new BorderLayout(10, 0));
                    item.setBackground(Color.WHITE);
                    item.setBorder(new EmptyBorder(10, 15, 10, 15));
                    item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

                    RoundAvatar av = new RoundAvatar(reqName.substring(0, 1).toUpperCase());
                    av.setPreferredSize(new Dimension(40, 40));
                    av.setBackground(ZALO_BLUE);
                    av.setForeground(Color.WHITE);
                    av.setFont(new Font("SansSerif", Font.BOLD, 16));

                    JLabel nameLbl = new JLabel(reqName);
                    nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

                    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                    actions.setOpaque(false);
                    FlatButton btnAcc = new FlatButton("Đồng ý", ZALO_BLUE, Color.WHITE, 8);
                    FlatButton btnDec = new FlatButton("Từ chối", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
                    btnAcc.setPreferredSize(new Dimension(68, 30));
                    btnDec.setPreferredSize(new Dimension(65, 30));
                    btnAcc.addActionListener(e -> { sendToServer("ACCEPT;;;" + reqUser + ";;;" + currentUser); lastDataSync = ""; });
                    btnDec.addActionListener(e -> { sendToServer("DECLINE;;;" + reqUser + ";;;" + currentUser); lastDataSync = ""; });
                    actions.add(btnAcc);
                    actions.add(btnDec);

                    item.add(av, BorderLayout.WEST);
                    item.add(nameLbl, BorderLayout.CENTER);
                    item.add(actions, BorderLayout.EAST);
                    pnlReqList.add(item);
                }
            }
            if (reqCount > 0) {
                btnShowRequests.setText("Lời mời (" + reqCount + ")");
                btnShowRequests.setBackground(new Color(255, 235, 235));
                btnShowRequests.setForeground(new Color(220, 53, 69));
            } else {
                btnShowRequests.setText("Lời mời (0)");
                btnShowRequests.setBackground(ZALO_HOVER_ITEM);
                btnShowRequests.setForeground(new Color(100, 110, 125));
                requestDialog.setVisible(false);
            }
            pnlReqList.revalidate();
            pnlReqList.repaint();

        } else if (s.startsWith("SEARCHRES;;;")) {
            String[] p = s.split(";;;");
            if (p.length == 3) {
                String foundUser = p[1];
                String foundName = p[2];
                if (pnlAddFriendResult != null) {
                    pnlAddFriendResult.removeAll();
                    JPanel card = new JPanel(new BorderLayout(12, 0));
                    card.setBackground(ZALO_HOVER_ITEM);
                    card.setBorder(new EmptyBorder(12, 14, 12, 14));

                    RoundAvatar av = new RoundAvatar(foundName.substring(0, 1).toUpperCase());
                    av.setPreferredSize(new Dimension(44, 44));
                    av.setBackground(ZALO_BLUE);
                    av.setForeground(Color.WHITE);
                    av.setFont(new Font("SansSerif", Font.BOLD, 17));

                    JPanel info = new JPanel(new GridLayout(2, 1));
                    info.setOpaque(false);
                    JLabel lblN = new JLabel(foundName);
                    lblN.setFont(new Font("SansSerif", Font.BOLD, 14));
                    JLabel lblU = new JLabel("@" + foundUser);
                    lblU.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    lblU.setForeground(Color.GRAY);
                    info.add(lblN);
                    info.add(lblU);

                    FlatButton btnAdd = new FlatButton("Kết bạn", Color.WHITE, ZALO_BLUE, 8);
                    btnAdd.setFont(new Font("SansSerif", Font.BOLD, 13));
                    btnAdd.setPreferredSize(new Dimension(85, 32));
                    btnAdd.addActionListener(ev -> {
                        sendToServer("ADD;;;" + currentUser + ";;;" + foundUser);
                        btnAdd.setText("Đã gửi");
                        btnAdd.setEnabled(false);
                    });

                    card.add(av, BorderLayout.WEST);
                    card.add(info, BorderLayout.CENTER);
                    card.add(btnAdd, BorderLayout.EAST);

                    JPanel wrapper = new JPanel(new BorderLayout());
                    wrapper.setBackground(Color.WHITE);
                    JLabel lblTitleRes = new JLabel("Kết quả tìm kiếm");
                    lblTitleRes.setFont(new Font("SansSerif", Font.BOLD, 12));
                    lblTitleRes.setForeground(Color.GRAY);
                    lblTitleRes.setBorder(new EmptyBorder(0, 0, 8, 0));
                    wrapper.add(lblTitleRes, BorderLayout.NORTH);
                    wrapper.add(card, BorderLayout.CENTER);

                    pnlAddFriendResult.add(wrapper, BorderLayout.NORTH);
                    pnlAddFriendResult.revalidate();
                    pnlAddFriendResult.repaint();
                }
            }
        } else if (s.equals("NOTFOUND")) {
            if (pnlAddFriendResult != null) {
                pnlAddFriendResult.removeAll();
                JLabel notFound = new JLabel("Không tìm thấy tài khoản hoặc số điện thoại này!", SwingConstants.CENTER);
                notFound.setForeground(new Color(220, 53, 69));
                pnlAddFriendResult.add(notFound, BorderLayout.CENTER);
                pnlAddFriendResult.revalidate();
                pnlAddFriendResult.repaint();
            }
        }
    }

    private void renderConversationList() {
        pnlConversationList.removeAll();
        String keyword = txtSidebarSearch.getText().trim().toLowerCase();

        if (isSearchMode) {
            JLabel lblHeader = new JLabel(keyword.isEmpty() ? "  Tìm gần đây" : "  Kết quả phù hợp");
            lblHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblHeader.setForeground(ZALO_TEXT_DARK);
            lblHeader.setBorder(new EmptyBorder(10, 10, 6, 10));
            lblHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlConversationList.add(lblHeader);
        }

        // Hiển thị hội thoại được Ghim lên đầu trước
        for (Group grp : groups.values()) {
            if (!pinnedConversations.contains("GRP_" + grp.id)) continue;
            if (!keyword.isEmpty() && !grp.name.toLowerCase().contains(keyword)) continue;
            pnlConversationList.add(createGroupListItem(grp, true));
        }
        for (Friend friendObj : friends.values()) {
            if (!pinnedConversations.contains("USR_" + friendObj.username)) continue;
            if (filterOnlineOnly && !friendObj.isOnline()) continue;
            if (!keyword.isEmpty() && !friendObj.name.toLowerCase().contains(keyword) && !friendObj.username.toLowerCase().contains(keyword)) continue;
            pnlConversationList.add(createFriendListItem(friendObj, avatarCache.get(friendObj.username), true));
        }

        // Các hội thoại còn lại
        for (Group grp : groups.values()) {
            if (pinnedConversations.contains("GRP_" + grp.id)) continue;
            if (!keyword.isEmpty() && !grp.name.toLowerCase().contains(keyword)) continue;
            pnlConversationList.add(createGroupListItem(grp, false));
        }
        for (Friend friendObj : friends.values()) {
            if (pinnedConversations.contains("USR_" + friendObj.username)) continue;
            if (filterOnlineOnly && !friendObj.isOnline()) continue;
            if (!keyword.isEmpty() && !friendObj.name.toLowerCase().contains(keyword) && !friendObj.username.toLowerCase().contains(keyword)) continue;
            pnlConversationList.add(createFriendListItem(friendObj, avatarCache.get(friendObj.username), false));
        }

        pnlConversationList.revalidate();
        pnlConversationList.repaint();
    }

    private JPanel createGroupListItem(Group grp, boolean isPinned) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(10, 14, 10, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        RoundAvatar av = new RoundAvatar(grp.name.substring(0, 1).toUpperCase());
        av.setPreferredSize(new Dimension(44, 44));
        av.setBackground(new Color(0, 150, 136));
        av.setForeground(Color.WHITE);
        av.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel((isPinned ? "[Ghim] " : "") + "[Nhóm] " + grp.name);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLbl.setForeground(ZALO_TEXT_DARK);

        JLabel subLbl = new JLabel(grp.members.size() + " thành viên");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(new Color(110, 118, 130));
        info.add(nameLbl);
        info.add(subLbl);

        item.add(av, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);

        if (!isMulticastMode && currentGroupId == grp.id) {
            item.setBackground(ZALO_ACTIVE_ITEM);
            lblTargetStatus.setText(grp.members.size() + " thành viên • Nhóm chat riêng");
        }

        item.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (isSearchMode) exitSearchMode();
                deactivateMulticastMode();
                showInfoDrawerMain();
                currentTargetUser = null;
                currentGroupId = grp.id;

                targetAvatarHeader.setImage(null);
                targetAvatarHeader.setAvatarText(grp.name.substring(0, 1).toUpperCase());
                targetAvatarHeader.setBackground(new Color(0, 150, 136));
                targetAvatarHeader.setOnlineStatus(false, false);

                lblTarget.setText(grp.name);
                lblTargetStatus.setText(grp.members.size() + " thành viên • Nhóm chat riêng");
                lblTargetStatus.setForeground(new Color(0, 150, 136));

                txtChat.setEnabled(true);
                btnSend.setEnabled(true);
                btnIcon.setEnabled(true);
                btnCall.setEnabled(false);
                btnVideoHeader.setEnabled(false);
                btnRecord.setEnabled(false);
                btnImg.setEnabled(false);
                btnFile.setEnabled(false);
                btnGroupMembers.setVisible(true);

                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();
                resetSharedMediaState();
                sendToServer("GETGROUPMSG;;;" + grp.id);

                renderConversationList();
            }
            public void mouseEntered(MouseEvent e) {
                if (!item.getBackground().equals(ZALO_ACTIVE_ITEM)) item.setBackground(ZALO_HOVER_ITEM);
            }
            public void mouseExited(MouseEvent e) {
                if (!item.getBackground().equals(ZALO_ACTIVE_ITEM)) item.setBackground(Color.WHITE);
            }
        });

        return item;
    }

    private JPanel createFriendListItem(Friend friendObj, BufferedImage frImg, boolean isPinned) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(10, 14, 10, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        RoundAvatar av = new RoundAvatar(friendObj.name.substring(0, 1).toUpperCase());
        av.setPreferredSize(new Dimension(44, 44));
        av.setBackground(ZALO_BLUE);
        av.setForeground(Color.WHITE);
        av.setFont(new Font("SansSerif", Font.BOLD, 17));
        av.setOnlineStatus(true, friendObj.isOnline());

        if (frImg != null) {
            av.setImage(frImg);
            av.setAvatarText("");
        }

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel((isPinned ? "[Ghim] " : "") + friendObj.name);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLbl.setForeground(ZALO_TEXT_DARK);

        JLabel statusLbl = new JLabel(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
        statusLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLbl.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(140, 148, 160));
        info.add(nameLbl);
        info.add(statusLbl);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        rightActions.setOpaque(false);
        FlatButton btnOptions = new FlatButton("", new Color(0, 0, 0, 0), ZALO_ICON_GRAY, 10);
        btnOptions.setIcon(new ZaloIcon(ZaloIcon.Type.MORE, 18, new Color(140, 148, 160)));
        btnOptions.setPreferredSize(new Dimension(28, 28));

        JPopupMenu friendMenu = new JPopupMenu();
        JMenuItem itemUnfriend = new JMenuItem("Xóa kết bạn");
        itemUnfriend.setForeground(new Color(231, 76, 60));
        itemUnfriend.addActionListener(ev -> {
            if (JOptionPane.showConfirmDialog(this, "Xóa kết bạn với " + friendObj.name + "?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                sendToServer("UNFRIEND;;;" + currentUser + ";;;" + friendObj.username);
                if (!isMulticastMode && currentTargetUser != null && currentTargetUser.equals(friendObj.username)) {
                    currentTargetUser = null;
                    lblTarget.setText("Chào mừng đến với Z-Voice");
                    lblTargetStatus.setText("");
                    txtChat.setEnabled(false);
                    btnSend.setEnabled(false);
                    btnCall.setEnabled(false);
                    btnVideoHeader.setEnabled(false);
                    chatPanel.removeAll();
                    chatPanel.revalidate();
                    chatPanel.repaint();
                    resetSharedMediaState();
                }
                lastDataSync = "";
                sendToServer("GETDATA;;;" + currentUser);
            }
        });
        friendMenu.add(itemUnfriend);
        btnOptions.addActionListener(e -> friendMenu.show(btnOptions, -60, btnOptions.getHeight()));
        rightActions.add(btnOptions);

        item.add(av, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);
        item.add(rightActions, BorderLayout.EAST);

        if (!isMulticastMode && currentGroupId == -1 && currentTargetUser != null && currentTargetUser.equals(friendObj.username)) {
            item.setBackground(ZALO_ACTIVE_ITEM);
            lblTargetStatus.setText(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
            lblTargetStatus.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(140, 148, 160));
            targetAvatarHeader.setOnlineStatus(true, friendObj.isOnline());
        }

        item.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (isSearchMode) exitSearchMode();
                deactivateMulticastMode();
                showInfoDrawerMain();
                currentGroupId = -1;
                currentTargetUser = friendObj.username;

                targetAvatarHeader.setBackground(ZALO_BLUE);
                if (frImg != null) {
                    targetAvatarHeader.setImage(frImg);
                    targetAvatarHeader.setAvatarText("");
                } else {
                    targetAvatarHeader.setImage(null);
                    targetAvatarHeader.setAvatarText(friendObj.name.substring(0, 1).toUpperCase());
                }
                targetAvatarHeader.setOnlineStatus(true, friendObj.isOnline());

                lblTarget.setText(friendObj.name);
                lblTargetStatus.setText(friendObj.isOnline() ? "Đang hoạt động" : "Ngoại tuyến");
                lblTargetStatus.setForeground(friendObj.isOnline() ? new Color(46, 204, 113) : new Color(140, 148, 160));

                txtChat.setEnabled(true);
                btnSend.setEnabled(true);
                btnCall.setEnabled(true);
                btnVideoHeader.setEnabled(true);
                btnRecord.setEnabled(true);
                btnImg.setEnabled(true);
                btnFile.setEnabled(true);
                btnIcon.setEnabled(true);
                btnGroupMembers.setVisible(false);

                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();
                resetSharedMediaState();
                sendToServer("GETMSG;;;" + currentUser + ";;;" + currentTargetUser);

                renderConversationList();
            }
            public void mouseEntered(MouseEvent e) {
                if (!item.getBackground().equals(ZALO_ACTIVE_ITEM)) item.setBackground(ZALO_HOVER_ITEM);
            }
            public void mouseExited(MouseEvent e) {
                if (!item.getBackground().equals(ZALO_ACTIVE_ITEM)) item.setBackground(Color.WHITE);
            }
        });

        return item;
    }

    private void sendText() {
        String text = txtChat.getText().trim();
        if (text.isEmpty()) return;
        txtChat.setText("");
        sendRawMessage(text);
    }

    private void sendRawMessage(String messageContent) {
        if (isMulticastMode) {
            try {
                String fullMsg = currentUser + ":::" + currentFullname + ":::" + messageContent;
                byte[] data = fullMsg.getBytes("UTF-8");
                if (mcSocket != null) mcSocket.send(new DatagramPacket(data, data.length, mcGroup, 9999));
            } catch (Exception e) {}
        }
        else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
            try {
                Group grp = groups.get(currentGroupId);
                String payloadStr = currentGroupId + ":::" + currentUser + ":::" + currentFullname + ":::" + messageContent;
                byte[] textBytes = payloadStr.getBytes("UTF-8");
                byte[] packetData = new byte[textBytes.length + 1];
                packetData[0] = 2;
                System.arraycopy(textBytes, 0, packetData, 1, textBytes.length);

                for (Friend member : grp.members) {
                    if (!member.username.equals(currentUser) && member.isOnline()) {
                        udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(member.ip), member.port));
                    }
                }

                sendToServer("SAVEGROUPMSG;;;" + currentGroupId + ";;;" + currentUser + ";;;" + messageContent);
                appendMsg(currentUser, currentFullname, messageContent, true);
            } catch (Exception e) {}
        }
        else {
            if (currentTargetUser == null) return;
            Friend f = friends.get(currentTargetUser);
            if (f != null && f.isOnline()) {
                try {
                    byte[] textBytes = messageContent.getBytes("UTF-8");
                    byte[] packetData = new byte[textBytes.length + 1];
                    packetData[0] = 1;
                    System.arraycopy(textBytes, 0, packetData, 1, textBytes.length);
                    udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(f.ip), f.port));

                    sendToServer("SAVEMSG;;;" + currentUser + ";;;" + f.username + ";;;" + messageContent);
                    appendMsg(currentUser, currentFullname, messageContent, true);
                } catch (Exception e) {}
            } else {
                JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
            }
        }
    }

    private void scrollChatToBottom() {
        JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
    }

    private void addChatBubble(JComponent comp, boolean isMe, String senderUsername, String senderName) {
        JPanel w = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 2));
        w.setOpaque(false);
        w.setBorder(new EmptyBorder(3, 4, 3, 4));

        if (!isMe) {
            String initial = (senderName != null && !senderName.isEmpty()) ? senderName.substring(0, 1).toUpperCase() : "?";
            RoundAvatar av = new RoundAvatar(initial);
            av.setPreferredSize(new Dimension(34, 34));
            av.setBackground(ZALO_BLUE);
            av.setForeground(Color.WHITE);
            av.setFont(new Font("SansSerif", Font.BOLD, 13));

            if (senderUsername != null && avatarCache.containsKey(senderUsername)) {
                av.setImage(avatarCache.get(senderUsername));
                av.setAvatarText("");
            }
            w.add(av);
        }

        w.add(comp);
        chatPanel.add(w);
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollChatToBottom();
    }

    private void appendMsg(String senderUsername, String senderName, String msg, boolean isMe) {
        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.Y_AXIS));
        pnlContent.setOpaque(false);

        if ((isMulticastMode || currentGroupId != -1) && !isMe && senderName != null && !senderName.isEmpty()) {
            JLabel lblName = new JLabel(senderName);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblName.setForeground(new Color(110, 118, 130));
            lblName.setBorder(new EmptyBorder(0, 0, 3, 0));
            lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlContent.add(lblName);
        }

        if (msg.startsWith("[STK:") && msg.endsWith("]")) {
            String stkId = msg.substring(5, msg.length() - 1);
            pnlContent.add(new StickerPanel(stkId, 102));
            addChatBubble(pnlContent, isMe, senderUsername, senderName);
            return;
        }

        // Khôi phục Ảnh từ lịch sử chat
        if (msg.startsWith("[MEDIA_IMG:") && msg.endsWith("]")) {
            String inner = msg.substring(11, msg.length() - 1);
            int sep = inner.indexOf(':');
            String localName = sep != -1 ? inner.substring(0, sep) : inner;
            String fallbackB64 = sep != -1 ? inner.substring(sep + 1) : "";
            long ts = extractTimestampFromLocalName(localName);
            byte[] imgBytes = loadMediaFromLocal(localName);
            if (imgBytes == null && !fallbackB64.isEmpty()) {
                try { imgBytes = Base64.getDecoder().decode(fallbackB64); } catch (Exception ex) {}
            }
            if (imgBytes != null) {
                appendImageMsg(senderUsername, senderName, imgBytes, isMe, ts);
            }
            return;
        }

        // Khôi phục File từ lịch sử chat
        if (msg.startsWith("[MEDIA_FILE:") && msg.endsWith("]")) {
            String inner = msg.substring(12, msg.length() - 1);
            int sep = inner.indexOf(':');
            String localName = sep != -1 ? inner.substring(0, sep) : inner;
            String displayName = sep != -1 ? inner.substring(sep + 1) : localName;
            byte[] fileBytes = loadMediaFromLocal(localName);
            renderHistoricalFileBubble(senderUsername, senderName, displayName, fileBytes, isMe);
            return;
        }

        extractLinksFromText(msg);

        boolean isEmojiOnly = msg.length() <= 8 && !msg.matches(".*[a-zA-Z0-9\\p{L}].*");
        if (!isEmojiOnly) {
            JTextArea txtArea = new JTextArea(msg);
            txtArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            txtArea.setForeground(ZALO_TEXT_DARK);
            txtArea.setOpaque(false);
            txtArea.setEditable(false);
            txtArea.setFocusable(false);
            txtArea.setLineWrap(true);
            txtArea.setWrapStyleWord(true);
            txtArea.setBorder(null);
            txtArea.setAlignmentX(Component.LEFT_ALIGNMENT);

            int maxBubbleWidth = 340;
            FontMetrics fm = txtArea.getFontMetrics(txtArea.getFont());
            int singleLineWidth = fm.stringWidth(msg);

            if (singleLineWidth <= maxBubbleWidth && !msg.contains("\n")) {
                txtArea.setPreferredSize(new Dimension(Math.max(20, singleLineWidth + 6), fm.getHeight() + 2));
            } else {
                txtArea.setSize(new Dimension(maxBubbleWidth, Short.MAX_VALUE));
                int wrappedHeight = txtArea.getPreferredSize().height;
                txtArea.setPreferredSize(new Dimension(maxBubbleWidth, wrappedHeight + 2));
            }

            BubblePanel bubble = isMe
                    ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                    : new BubblePanel(Color.WHITE, new Color(222, 226, 232));

            pnlContent.add(txtArea);
            bubble.add(pnlContent, BorderLayout.CENTER);
            addChatBubble(bubble, isMe, senderUsername, senderName);
        } else {
            JLabel l = new JLabel(msg);
            l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
            pnlContent.add(l);
            addChatBubble(pnlContent, isMe, senderUsername, senderName);
        }
    }

    private void appendImageMsg(String senderUsername, String senderName, byte[] imgData, boolean isMe, long timestampMillis) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgData));
            if (img == null) return;
            SharedMediaItem mediaItem = new SharedMediaItem(img, formatDateGroup(timestampMillis));
            currentSharedImages.add(0, mediaItem);
            refreshInfoDrawer();

            int w = img.getWidth(), h = img.getHeight(), max = 220;
            if (w > max || h > max) {
                float ratio = Math.min((float) max / w, (float) max / h);
                w = Math.round(w * ratio);
                h = Math.round(h * ratio);
            }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            JLabel l = new JLabel(new ImageIcon(scaled));
            l.setCursor(new Cursor(Cursor.HAND_CURSOR));
            l.setToolTipText("Nhấn để phóng to ảnh");
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int idx = currentSharedImages.indexOf(mediaItem);
                    showImagePreviewDialog(Math.max(0, idx));
                }
            });

            BubblePanel bubble = isMe
                    ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                    : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
            bubble.add(l, BorderLayout.CENTER);
            addChatBubble(bubble, isMe, senderUsername, senderName);
        } catch (Exception e) {}
    }

    private void appendFileMsg(String senderUsername, String senderName, byte[] payload, boolean isMe) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(payload);
            int nameLen = bb.getInt();
            byte[] nameBytes = new byte[nameLen];
            bb.get(nameBytes);
            String fileName = new String(nameBytes, "UTF-8");
            byte[] fileData = new byte[payload.length - 4 - nameLen];
            bb.get(fileData);

            currentSharedFiles.add(0, new SharedFileItem(fileName, fileData));
            refreshInfoDrawer();

            Color bg = isMe ? ZALO_SENT_BUBBLE : Color.WHITE;
            FlatButton btn = new FlatButton("  " + fileName, bg, ZALO_TEXT_DARK, 8);
            btn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 18, ZALO_BLUE));
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            btn.addActionListener(e -> saveSharedFileToDisk(fileName, fileData));

            BubblePanel bubble = isMe
                    ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                    : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
            bubble.add(btn, BorderLayout.CENTER);
            addChatBubble(bubble, isMe, senderUsername, senderName);
        } catch (Exception e) {}
    }

    private void appendAudioMsg(String senderUsername, String senderName, byte[] audioData, boolean isMe) {
        Color bg = isMe ? ZALO_SENT_BUBBLE : Color.WHITE;
        FlatButton btnPlay = new FlatButton("  Tin nhắn thoại", bg, ZALO_BLUE, 8);
        btnPlay.setIcon(new ZaloIcon(ZaloIcon.Type.MIC, 18, ZALO_BLUE));
        btnPlay.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnPlay.addActionListener(e -> {
            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
                    SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                    sdl.open(format);
                    sdl.start();
                    sdl.write(audioData, 0, audioData.length);
                    sdl.drain();
                    sdl.close();
                } catch (Exception ex) {}
            }).start();
        });
        BubblePanel bubble = isMe
                ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
        bubble.add(btnPlay, BorderLayout.CENTER);
        addChatBubble(bubble, isMe, senderUsername, senderName);
    }

    private boolean autoStartVideoOnCall = false;

    private void initiateCall(boolean withVideo) {
        if (isMulticastMode || currentGroupId != -1) {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ gọi thoại/video trực tiếp 1-1!");
            return;
        }
        if (currentTargetUser == null) return;
        Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) {
            JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
            return;
        }
        autoStartVideoOnCall = withVideo;
        currentCallUser = currentTargetUser;
        sendP2P(f, new byte[]{4});

        pnlTargetVideo.setAvatarText(f.name.substring(0, 1).toUpperCase());
        pnlTargetVideo.setFrame(null);
        lblCallName.setText(f.name);
        lblCallStatus.setText("Đang đổ chuông...");
        btnAcceptCall.setVisible(false);
        btnDeclineCall.setVisible(false);
        btnEndCall.setVisible(true);
        btnToggleVideo.setVisible(false);

        cardLayout.show(rightPanel, "CALL");
    }

    private void handleIncomingCall(String callerUsername) {
        if (currentCallUser != null) {
            sendP2P(friends.get(callerUsername), new byte[]{6});
            return;
        }
        autoStartVideoOnCall = false;
        currentCallUser = callerUsername;
        Friend f = friends.get(currentCallUser);
        if (f == null) return;

        pnlTargetVideo.setAvatarText(f.name.substring(0, 1).toUpperCase());
        pnlTargetVideo.setFrame(null);
        lblCallName.setText(f.name);
        lblCallStatus.setText("Cuộc gọi đến...");
        btnAcceptCall.setVisible(true);
        btnDeclineCall.setVisible(true);
        btnEndCall.setVisible(false);
        btnToggleVideo.setVisible(false);

        Toolkit.getDefaultToolkit().beep();
        cardLayout.show(rightPanel, "CALL");
    }

    private void acceptCall() {
        if (currentCallUser == null) return;
        sendP2P(friends.get(currentCallUser), new byte[]{5});
        lblCallStatus.setText("Đang trò chuyện");
        btnAcceptCall.setVisible(false);
        btnDeclineCall.setVisible(false);
        btnEndCall.setVisible(true);
        startAudioStreams();
        btnToggleVideo.setVisible(true);
    }

    private void handleCallAccepted() {
        lblCallStatus.setText("Đang trò chuyện");
        startAudioStreams();
        btnToggleVideo.setVisible(true);
        if (autoStartVideoOnCall && !isVideoOn) {
            toggleVideo();
        }
    }

    private void declineCall() {
        if (currentCallUser == null) return;
        sendP2P(friends.get(currentCallUser), new byte[]{6});
        closeCallUI();
    }

    private void handleCallDeclined() {
        JOptionPane.showMessageDialog(this, "Người dùng đã từ chối cuộc gọi");
        closeCallUI();
    }

    private void endCall() {
        if (currentCallUser == null) return;
        sendP2P(friends.get(currentCallUser), new byte[]{7});
        closeCallUI();
    }

    private void handleCallEnded() { closeCallUI(); }

    private void closeCallUI() {
        currentCallUser = null;
        autoStartVideoOnCall = false;
        stopAudioStreams();
        stopVideo();
        btnToggleVideo.setVisible(false);
        pnlTargetVideo.setFrame(null);
        cardLayout.show(rightPanel, "CHAT");
    }

    private void startAudioStreams() {
        if (isCalling) return;
        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
            mic.open(format);
            mic.start();
            isCalling = true;
            new Thread(() -> {
                try {
                    byte[] audioBuffer = new byte[1024];
                    byte[] packetData = new byte[1025];
                    packetData[0] = 0;
                    Friend f = friends.get(currentCallUser);
                    if (f == null) return;
                    InetAddress addr = InetAddress.getByName(f.ip);
                    int targetPort = f.port;
                    while (isCalling && currentCallUser != null) {
                        int read = mic.read(audioBuffer, 0, audioBuffer.length);
                        if (read > 0) {
                            System.arraycopy(audioBuffer, 0, packetData, 1, read);
                            udpSocket.send(new DatagramPacket(packetData, packetData.length, addr, targetPort));
                        }
                    }
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {}
    }

    private void stopAudioStreams() {
        isCalling = false;
        if (mic != null) { mic.close(); mic = null; }
    }

    private void sendFragmentedData(Friend f, byte type, byte[] fullData, long msgId) {
        new Thread(() -> {
            try {
                InetAddress addr = InetAddress.getByName(f.ip);
                int offset = 0;
                while (offset < fullData.length) {
                    int length = Math.min(50000, fullData.length - offset);
                    ByteBuffer bb = ByteBuffer.allocate(17 + length);
                    bb.put(type);
                    bb.putLong(msgId);
                    bb.putInt(fullData.length);
                    bb.putInt(offset);
                    bb.put(fullData, offset, length);
                    udpSocket.send(new DatagramPacket(bb.array(), bb.array().length, addr, f.port));
                    offset += length;
                    Thread.sleep(15);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void sendFileOrImage(int type) {
        if (isMulticastMode || currentGroupId != -1) {
            JOptionPane.showMessageDialog(this, "Tính năng gửi File/Ảnh hiện hỗ trợ trong chat 1-1!");
            return;
        }
        if (currentTargetUser == null) return;
        Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) {
            JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
            return;
        }

        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                long msgId = System.currentTimeMillis();

                if (type == 8) {
                    String localName = "img_" + msgId + ".dat";
                    saveMediaToLocal(localName, fileBytes);
                    sendFragmentedData(f, (byte) 8, fileBytes, msgId);

                    String thumbB64 = createCompressedImageBase64(fileBytes);
                    sendToServer("SAVEMSG;;;" + currentUser + ";;;" + f.username + ";;;[MEDIA_IMG:" + localName + ":]");
                    appendImageMsg(currentUser, currentFullname, fileBytes, true, msgId);
                } else {
                    String safeName = file.getName().replace(":", "_").replace("|", "_").replace(";", "_");
                    String localName = "file_" + msgId + "_" + safeName;
                    saveMediaToLocal(localName, fileBytes);

                    byte[] nameBytes = safeName.getBytes("UTF-8");
                    ByteBuffer bb = ByteBuffer.allocate(4 + nameBytes.length + fileBytes.length);
                    bb.putInt(nameBytes.length);
                    bb.put(nameBytes);
                    bb.put(fileBytes);

                    sendFragmentedData(f, (byte) 9, bb.array(), msgId);
                    sendToServer("SAVEMSG;;;" + currentUser + ";;;" + f.username + ";;;[MEDIA_FILE:" + localName + ":" + safeName + "]");
                    appendFileMsg(currentUser, currentFullname, bb.array(), true);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc file");
            }
        }
    }

    private void toggleRecord() {
        if (isMulticastMode || currentGroupId != -1) {
            JOptionPane.showMessageDialog(this, "Tính năng ghi âm hiện hỗ trợ trong chat 1-1!");
            return;
        }
        if (currentTargetUser == null) return;
        Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) {
            JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
            return;
        }

        if (!isRecording) {
            try {
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
                recordMic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                recordMic.open(format);
                recordMic.start();
                isRecording = true;
                currentRecordStream = new ByteArrayOutputStream();
                btnRecord.setIcon(new ZaloIcon(ZaloIcon.Type.MIC, 20, new Color(231, 76, 60)));
                new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    while (isRecording) {
                        int read = recordMic.read(buffer, 0, buffer.length);
                        if (read > 0) currentRecordStream.write(buffer, 0, read);
                    }
                    recordMic.close();
                }).start();
            } catch (Exception e) {}
        } else {
            isRecording = false;
            btnRecord.setIcon(new ZaloIcon(ZaloIcon.Type.MIC, 20, ZALO_ICON_GRAY));
            byte[] audioData = currentRecordStream.toByteArray();
            appendAudioMsg(currentUser, currentFullname, audioData, true);
            sendFragmentedData(f, (byte) 3, audioData, System.currentTimeMillis());
        }
    }

    private void logout() {
        isRunning = false;
        closeCallUI();
        isRecording = false;
        deactivateMulticastMode();
        if (timer != null) timer.stop();
        if (udpSocket != null) udpSocket.close();
        if (recordMic != null) recordMic.close();
        if (speaker != null) speaker.close();
        try (DatagramSocket tmp = new DatagramSocket()) {
            byte[] dt = ("ONLINE;;;" + currentUser).getBytes("UTF-8");
            tmp.send(new DatagramPacket(dt, dt.length, InetAddress.getByName(LoginClient.SERVER_IP), 8080));
        } catch (Exception e) {}
        new LoginClient().setVisible(true);
        this.dispose();
    }

    private void toggleVideo() {
        if (!isVideoOn) {
            startVideo();
            btnToggleVideo.setText(" Tắt Cam");
            btnToggleVideo.setBackground(new Color(231, 76, 60));
        } else {
            stopVideo();
            btnToggleVideo.setText(" Bật Cam");
            btnToggleVideo.setBackground(ZALO_BLUE);
        }
    }

    private void startVideo() {
        if (webcam == null) {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy Camera trên thiết bị!");
                return;
            }
        }
        isVideoOn = true;
        lblMyVideo.setVisible(true);
        new Thread(() -> {
            try {
                Friend f = friends.get(currentCallUser);
                if (f == null) return;
                InetAddress addr = InetAddress.getByName(f.ip);
                int targetPort = f.port;

                while (isVideoOn && currentCallUser != null && webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        SwingUtilities.invokeLater(() -> {
                            Image scaled = image.getScaledInstance(160, 120, Image.SCALE_SMOOTH);
                            lblMyVideo.setIcon(new ImageIcon(scaled));
                        });

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);
                        byte[] imgBytes = baos.toByteArray();

                        if (imgBytes.length < 60000) {
                            byte[] packetData = new byte[imgBytes.length + 1];
                            packetData[0] = 10;
                            System.arraycopy(imgBytes, 0, packetData, 1, imgBytes.length);
                            udpSocket.send(new DatagramPacket(packetData, packetData.length, addr, targetPort));
                        }
                    }
                    Thread.sleep(60);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void stopVideo() {
        isVideoOn = false;
        lblMyVideo.setVisible(false);
        lblMyVideo.setIcon(null);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
        }
    }

    private void listenForBroadcast() {
        try {
            DatagramSocket broadcastSocket = new DatagramSocket(null);
            broadcastSocket.setReuseAddress(true);
            broadcastSocket.bind(new InetSocketAddress(8888));
            byte[] buffer = new byte[2048];
            while (isRunning) {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), "UTF-8");
                if (msg.startsWith("SERVER_MSG:::")) {
                    String text = msg.substring(13);
                    SwingUtilities.invokeLater(() -> {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(this, "THÔNG BÁO KHẨN TỪ SERVER:\n\n" + text, "Broadcast Message", JOptionPane.WARNING_MESSAGE);
                    });
                }
            }
        } catch (Exception e) {}
    }

    private void activateMulticastMode() {
        if (isMulticastMode) return;

        isMulticastMode = true;
        currentTargetUser = null;
        currentGroupId = -1;
        showInfoDrawerMain();

        renderConversationList();
        btnMulticast.setBackground(new Color(200, 235, 215));

        targetAvatarHeader.setImage(null);
        targetAvatarHeader.setAvatarText("LAN");
        targetAvatarHeader.setBackground(new Color(25, 135, 84));
        targetAvatarHeader.setOnlineStatus(false, false);

        lblTarget.setText("Phòng Chat LAN Chung (Multicast)");
        lblTargetStatus.setText("Tất cả thành viên trong mạng LAN đều có mặt");
        lblTargetStatus.setForeground(new Color(25, 135, 84));

        txtChat.setEnabled(true);
        btnSend.setEnabled(true);
        btnIcon.setEnabled(true);
        btnCall.setEnabled(false);
        btnVideoHeader.setEnabled(false);
        btnRecord.setEnabled(false);
        btnImg.setEnabled(false);
        btnFile.setEnabled(false);
        btnGroupMembers.setVisible(false);

        chatPanel.removeAll();
        chatPanel.revalidate();
        chatPanel.repaint();
        resetSharedMediaState();
        appendMsg(null, "", "Chào mừng bạn đến với phòng chat LAN chung!", false);

        try {
            mcSocket = new MulticastSocket(9999);
            mcGroup = InetAddress.getByName("224.0.0.1");
            mcSocket.joinGroup(new InetSocketAddress(mcGroup, 9999), null);

            new Thread(() -> {
                try {
                    byte[] buf = new byte[2048];
                    while (isMulticastMode && isRunning) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        mcSocket.receive(p);
                        String rcv = new String(p.getData(), 0, p.getLength(), "UTF-8");

                        String[] parts = rcv.split(":::");
                        if (parts.length == 3) {
                            String senderUser = parts[0];
                            String senderName = parts[1];
                            String content = parts[2];
                            boolean isMe = senderUser.equals(currentUser);
                            SwingUtilities.invokeLater(() -> appendMsg(senderUser, senderName, content, isMe));
                        }
                    }
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối mạng Multicast!");
        }
    }

    private void deactivateMulticastMode() {
        if (!isMulticastMode) return;
        isMulticastMode = false;
        btnMulticast.setBackground(new Color(235, 247, 240));
        try {
            if (mcSocket != null && mcGroup != null) {
                mcSocket.leaveGroup(new InetSocketAddress(mcGroup, 9999), null);
                mcSocket.close();
                mcSocket = null;
            }
        } catch (Exception e) {}
    }

    // =========================================================================
    // LƯU TRỮ MEDIA CỤC BỘ & TRÌNH XEM PHÓNG TO ẢNH
    // =========================================================================
    private void saveMediaToLocal(String fileName, byte[] data) {
        try {
            File dir = new File("zvoice_media");
            if (!dir.exists()) dir.mkdirs();
            Files.write(new File(dir, fileName).toPath(), data);
        } catch (Exception e) {}
    }

    private byte[] loadMediaFromLocal(String fileName) {
        try {
            File f = new File("zvoice_media", fileName);
            if (f.exists()) return Files.readAllBytes(f.toPath());
        } catch (Exception e) {}
        return null;
    }

    private String createCompressedImageBase64(byte[] rawImgBytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(rawImgBytes));
            if (src == null) return "";
            // Thu nhỏ thumbnail dự phòng xuống 96px để không bao giờ tràn gói tin UDP 64KB khi tải lịch sử
            int w = src.getWidth(), h = src.getHeight(), maxDim = 96;
            if (w > maxDim || h > maxDim) {
                float r = Math.min((float) maxDim / w, (float) maxDim / h);
                w = Math.max(1, Math.round(w * r));
                h = Math.max(1, Math.round(h * r));
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = out.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.drawImage(src.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.45f);
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(out, null, null), param);
                }
                writer.dispose();
            } else {
                ImageIO.write(out, "jpg", baos);
            }
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return b64.length() <= 3500 ? b64 : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void renderHistoricalFileBubble(String senderUsername, String senderName, String fileName, byte[] fileData, boolean isMe) {
        currentSharedFiles.add(0, new SharedFileItem(fileName, fileData));
        refreshInfoDrawer();

        Color bg = isMe ? ZALO_SENT_BUBBLE : Color.WHITE;
        FlatButton btn = new FlatButton("  " + fileName, bg, ZALO_TEXT_DARK, 8);
        btn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 18, ZALO_BLUE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.addActionListener(e -> saveSharedFileToDisk(fileName, fileData));

        BubblePanel bubble = isMe
                ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
        bubble.add(btn, BorderLayout.CENTER);
        addChatBubble(bubble, isMe, senderUsername, senderName);
    }

    private void saveSharedFileToDisk(String fileName, byte[] fileData) {
        if (fileData == null) {
            JOptionPane.showMessageDialog(this, "Tệp này được gửi từ thiết bị khác và chưa có bản lưu trên máy này.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(), fileData);
                JOptionPane.showMessageDialog(this, "Đã lưu tệp thành công!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi lưu tệp!");
            }
        }
    }

    private void showImagePreviewDialog(int startIndex) {
        if (currentSharedImages.isEmpty()) return;
        final int[] idx = {Math.max(0, Math.min(startIndex, currentSharedImages.size() - 1))};
        final double[] zoom = {1.0};

        JDialog viewer = new JDialog(this, "Xem ảnh", true);
        viewer.setSize(860, 640);
        viewer.setLocationRelativeTo(this);
        viewer.setLayout(new BorderLayout());
        viewer.getContentPane().setBackground(new Color(24, 26, 30));

        JLabel lblImg = new JLabel("", SwingConstants.CENTER);
        JScrollPane scroll = new JScrollPane(lblImg);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(24, 26, 30));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        JLabel lblCounter = new JLabel("", SwingConstants.CENTER);
        lblCounter.setForeground(Color.WHITE);
        lblCounter.setFont(new Font("SansSerif", Font.BOLD, 13));

        Runnable renderImg = () -> {
            BufferedImage raw = currentSharedImages.get(idx[0]).image;
            int maxW = 780, maxH = 500;
            double fitScale = Math.min(1.0, Math.min((double) maxW / raw.getWidth(), (double) maxH / raw.getHeight()));
            int targetW = Math.max(40, (int) Math.round(raw.getWidth() * fitScale * zoom[0]));
            int targetH = Math.max(40, (int) Math.round(raw.getHeight() * fitScale * zoom[0]));
            Image scaled = raw.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
            lblImg.setIcon(new ImageIcon(scaled));
            lblCounter.setText("Ảnh " + (idx[0] + 1) + " / " + currentSharedImages.size() + "  (" + Math.round(zoom[0] * 100) + "%)");
        };

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        controls.setBackground(new Color(34, 37, 42));

        FlatButton btnPrev = new FlatButton("◀ Trước", new Color(55, 60, 68), Color.WHITE, 8);
        FlatButton btnZoomOut = new FlatButton("Thu nhỏ (-)", new Color(55, 60, 68), Color.WHITE, 8);
        FlatButton btnReset = new FlatButton("100%", new Color(55, 60, 68), Color.WHITE, 8);
        FlatButton btnZoomIn = new FlatButton("Phóng to (+)", new Color(55, 60, 68), Color.WHITE, 8);
        FlatButton btnSaveImg = new FlatButton("Lưu ảnh", ZALO_BLUE, Color.WHITE, 8);
        FlatButton btnNext = new FlatButton("Sau ▶", new Color(55, 60, 68), Color.WHITE, 8);

        btnPrev.addActionListener(e -> {
            if (idx[0] > 0) { idx[0]--; zoom[0] = 1.0; renderImg.run(); }
        });
        btnNext.addActionListener(e -> {
            if (idx[0] < currentSharedImages.size() - 1) { idx[0]++; zoom[0] = 1.0; renderImg.run(); }
        });
        btnZoomIn.addActionListener(e -> {
            if (zoom[0] < 3.0) { zoom[0] += 0.25; renderImg.run(); }
        });
        btnZoomOut.addActionListener(e -> {
            if (zoom[0] > 0.5) { zoom[0] -= 0.25; renderImg.run(); }
        });
        btnReset.addActionListener(e -> { zoom[0] = 1.0; renderImg.run(); });
        btnSaveImg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("ZVoice_Image_" + (idx[0] + 1) + ".png"));
            if (fc.showSaveDialog(viewer) == JFileChooser.APPROVE_OPTION) {
                try {
                    ImageIO.write(currentSharedImages.get(idx[0]).image, "png", fc.getSelectedFile());
                    JOptionPane.showMessageDialog(viewer, "Đã lưu ảnh về máy!");
                } catch (Exception ex) {}
            }
        });

        controls.add(btnPrev);
        controls.add(btnZoomOut);
        controls.add(btnReset);
        controls.add(btnZoomIn);
        controls.add(btnSaveImg);
        controls.add(btnNext);
        controls.add(lblCounter);

        renderImg.run();
        viewer.add(scroll, BorderLayout.CENTER);
        viewer.add(controls, BorderLayout.SOUTH);
        viewer.setVisible(true);
    }

    class VideoPanel extends JPanel {
        private BufferedImage frame;
        private String avatarText = "?";

        public VideoPanel() {
            setOpaque(true);
            setBackground(new Color(20, 20, 20));
        }

        public void setFrame(BufferedImage img) {
            this.frame = img;
            repaint();
        }

        public void setAvatarText(String t) {
            this.avatarText = t;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (frame != null) {
                g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
            } else {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ZALO_BLUE);
                int size = 180;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2 - 40;
                g2.fillOval(x, y, size, size);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 70));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(avatarText);
                int th = fm.getAscent();
                g2.drawString(avatarText, x + (size - tw) / 2, y + (size + th) / 2 - fm.getDescent());
            }
        }
    }
}