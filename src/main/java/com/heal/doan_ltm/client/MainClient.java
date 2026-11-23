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
import java.awt.datatransfer.StringSelection;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    private boolean isGroupConferenceCall = false;

    private RoundAvatar avatarLabel, targetAvatarHeader;
    private JLabel lblTarget, lblTargetStatus;
    private JTextField txtChat, txtSidebarSearch;
    private JPanel chatPanel;
    private FlatButton btnCall, btnVideoHeader, btnToggleInfo, btnGroupMembers, btnSend, btnRecord, btnImg, btnFile, btnIcon, btnShowRequests;

    // Thanh tin nhắn đã ghim trên đầu khung chat
    private JPanel pinnedBannerPanel;
    private JLabel lblPinnedTitle, lblPinnedPreview;
    private FlatButton btnPinnedList, btnUnpinBanner;
    private List<PinnedMessageItem> currentPinnedMessages = new ArrayList<>();

    // Quản lý từng bong bóng tin nhắn để Thu hồi / Ghim / Cuộn tới
    private Map<String, ChatBubbleEntry> messageBubbles = new LinkedHashMap<>();
    private final AtomicInteger msgSeq = new AtomicInteger(0);
    private int legacyCounter = 0;

    private static class PinnedMessageItem {
        final String msgId;
        final String senderName;
        final String preview;
        PinnedMessageItem(String msgId, String senderName, String preview) {
            this.msgId = msgId;
            this.senderName = senderName;
            this.preview = preview;
        }
    }

    private static class ChatBubbleEntry {
        final String msgId;
        final String senderUsername;
        final String senderName;
        final String previewText;
        final String copyableText;
        final boolean isMe;
        boolean isRecalled;
        final JPanel wrapperRow;
        final JPanel bubbleHolder;
        FlatButton btnMore;
        SharedMediaItem linkedMedia;
        SharedFileItem linkedFile;

        ChatBubbleEntry(String msgId, String senderUsername, String senderName, String previewText,
                        String copyableText, boolean isMe, boolean isRecalled, JPanel wrapperRow, JPanel bubbleHolder) {
            this.msgId = msgId;
            this.senderUsername = senderUsername;
            this.senderName = senderName;
            this.previewText = previewText;
            this.copyableText = copyableText;
            this.isMe = isMe;
            this.isRecalled = isRecalled;
            this.wrapperRow = wrapperRow;
            this.bubbleHolder = bubbleHolder;
        }
    }

    private JPanel pnlReqList, pnlConversationList, pnlSearchActions;
    private FlatButton btnCloseSearch;
    private boolean isSearchMode = false;
    private boolean filterOnlineOnly = false;
    private JLabel lblTabAll, lblTabOnline;
    private JDialog requestDialog;
    private JPanel pnlAddFriendResult;

    // Khung "Thông tin hội thoại", "Kho lưu trữ" & "Thành viên" bên phải giống Zalo PC
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

    // Thành phần bên trong thẻ "Thành viên" (giống hệt ảnh mẫu Zalo PC)
    private JLabel lblMembersSectionTitle;
    private JPanel pnlGroupMembersList;

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
        btnGroupMembers.setPreferredSize(new Dimension(112, 36));
        btnGroupMembers.setVisible(false);
        btnGroupMembers.addActionListener(e -> openGroupMembersDrawer());

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

        // Thanh Tin nhắn đã ghim (ẩn mặc định, hiện khi có tin nhắn ghim)
        pinnedBannerPanel = createPinnedBannerPanel();

        JPanel topChatStack = new JPanel();
        topChatStack.setLayout(new BoxLayout(topChatStack, BoxLayout.Y_AXIS));
        topChatStack.add(headerPanel);
        topChatStack.add(pinnedBannerPanel);

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

        centerChatColumn.add(topChatStack, BorderLayout.NORTH);
        centerChatColumn.add(chatScroll, BorderLayout.CENTER);
        centerChatColumn.add(chatBottomPanel, BorderLayout.SOUTH);

        // =====================================================================
        // CỘT 4: KHUNG "THÔNG TIN HỘI THOẠI", "KHO LƯU TRỮ" & "THÀNH VIÊN" BÊN PHẢI
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

    // =========================================================================
    // THANH TIN NHẮN ĐÃ GHIM & XỬ LÝ THU HỒI / GHIM TIN NHẮN (NÚT ...)
    // =========================================================================
    private JPanel createPinnedBannerPanel() {
        JPanel banner = new JPanel(new BorderLayout(10, 0));
        banner.setBackground(new Color(245, 249, 255));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(212, 228, 255)),
                new EmptyBorder(6, 14, 6, 12)
        ));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        banner.setVisible(false);

        JLabel pinIcon = new JLabel(new ZaloIcon(ZaloIcon.Type.PIN, 18, ZALO_BLUE));

        JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 1));
        textCol.setOpaque(false);
        textCol.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblPinnedTitle = new JLabel("Tin nhắn đã ghim");
        lblPinnedTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPinnedTitle.setForeground(ZALO_BLUE);

        lblPinnedPreview = new JLabel("");
        lblPinnedPreview.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPinnedPreview.setForeground(ZALO_TEXT_DARK);

        textCol.add(lblPinnedTitle);
        textCol.add(lblPinnedPreview);
        textCol.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!currentPinnedMessages.isEmpty()) {
                    scrollToAndHighlightMessage(currentPinnedMessages.get(0).msgId);
                }
            }
        });

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightBtns.setOpaque(false);

        btnPinnedList = new FlatButton("Danh sách (1)", Color.WHITE, ZALO_BLUE, 6);
        btnPinnedList.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnPinnedList.setPreferredSize(new Dimension(96, 26));
        btnPinnedList.addActionListener(e -> showPinnedListDialog());

        btnUnpinBanner = new FlatButton("Bỏ ghim", new Color(230, 238, 252), ZALO_TEXT_DARK, 6);
        btnUnpinBanner.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnUnpinBanner.setPreferredSize(new Dimension(64, 26));
        btnUnpinBanner.addActionListener(e -> {
            if (!currentPinnedMessages.isEmpty()) {
                unpinMessage(currentPinnedMessages.get(0).msgId);
            }
        });

        rightBtns.add(btnPinnedList);
        rightBtns.add(btnUnpinBanner);

        banner.add(pinIcon, BorderLayout.WEST);
        banner.add(textCol, BorderLayout.CENTER);
        banner.add(rightBtns, BorderLayout.EAST);
        return banner;
    }

    private void refreshPinnedBanner() {
        if (pinnedBannerPanel == null) return;
        if (currentPinnedMessages.isEmpty()) {
            pinnedBannerPanel.setVisible(false);
        } else {
            PinnedMessageItem top = currentPinnedMessages.get(0);
            int count = currentPinnedMessages.size();
            lblPinnedTitle.setText(count > 1 ? "Tin nhắn đã ghim (" + count + ")" : "Tin nhắn đã ghim");
            lblPinnedPreview.setText(top.senderName + ": " + top.preview);
            btnPinnedList.setText("Danh sách (" + count + ")");
            btnPinnedList.setVisible(count > 1);
            pinnedBannerPanel.setVisible(true);
        }
        pinnedBannerPanel.revalidate();
        pinnedBannerPanel.repaint();
    }

    private void showPinnedListDialog() {
        if (currentPinnedMessages.isEmpty()) return;
        JDialog dlg = new JDialog(this, "Danh sách tin nhắn đã ghim (" + currentPinnedMessages.size() + ")", true);
        dlg.setSize(450, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        list.setBorder(new EmptyBorder(10, 12, 10, 12));

        for (PinnedMessageItem item : new ArrayList<>(currentPinnedMessages)) {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(ZALO_HOVER_ITEM);
            card.setBorder(new EmptyBorder(10, 12, 10, 12));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

            JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
            info.setOpaque(false);
            JLabel lblS = new JLabel(item.senderName);
            lblS.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblS.setForeground(ZALO_BLUE);

            JLabel lblP = new JLabel(item.preview);
            lblP.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lblP.setForeground(ZALO_TEXT_DARK);
            info.add(lblS);
            info.add(lblP);

            JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
            acts.setOpaque(false);

            FlatButton btnGo = new FlatButton("Xem", ZALO_BLUE, Color.WHITE, 6);
            btnGo.setFont(new Font("SansSerif", Font.BOLD, 11));
            btnGo.setPreferredSize(new Dimension(56, 28));
            btnGo.addActionListener(ev -> {
                dlg.dispose();
                scrollToAndHighlightMessage(item.msgId);
            });

            FlatButton btnDel = new FlatButton("Bỏ ghim", Color.WHITE, new Color(220, 53, 69), 6);
            btnDel.setFont(new Font("SansSerif", Font.BOLD, 11));
            btnDel.setPreferredSize(new Dimension(68, 28));
            btnDel.addActionListener(ev -> {
                unpinMessage(item.msgId);
                dlg.dispose();
                if (!currentPinnedMessages.isEmpty()) showPinnedListDialog();
            });

            acts.add(btnGo);
            acts.add(btnDel);

            card.add(info, BorderLayout.CENTER);
            card.add(acts, BorderLayout.EAST);

            list.add(card);
            list.add(Box.createVerticalStrut(8));
        }

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        dlg.add(sp, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void scrollToAndHighlightMessage(String msgId) {
        ChatBubbleEntry entry = messageBubbles.get(msgId);
        if (entry == null || entry.wrapperRow == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy vị trí tin nhắn này trong khung chat hiện tại.");
            return;
        }
        Rectangle bounds = entry.wrapperRow.getBounds();
        chatPanel.scrollRectToVisible(bounds);
        entry.wrapperRow.setOpaque(true);
        entry.wrapperRow.setBackground(new Color(255, 243, 196));
        entry.wrapperRow.repaint();

        Timer flashTimer = new Timer(1600, e -> {
            entry.wrapperRow.setOpaque(false);
            entry.wrapperRow.setBackground(null);
            entry.wrapperRow.repaint();
        });
        flashTimer.setRepeats(false);
        flashTimer.start();
    }

    private String generateMessageId() {
        return System.currentTimeMillis() + "_" + msgSeq.incrementAndGet();
    }

    private String getCanonicalPinnedConvKey() {
        if (isMulticastMode) return "LAN";
        if (currentGroupId != -1) return "GRP_" + currentGroupId;
        if (currentTargetUser != null) {
            String a = currentUser;
            String b = currentTargetUser;
            return "USR_" + (a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a);
        }
        return null;
    }

    private boolean isMessagePinned(String msgId) {
        for (PinnedMessageItem item : currentPinnedMessages) {
            if (item.msgId.equals(msgId)) return true;
        }
        return false;
    }

    private void showBubbleActionMenu(Component invoker, int x, int y, String msgId) {
        ChatBubbleEntry entry = messageBubbles.get(msgId);
        if (entry == null) return;

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230), 1, true));

        if (!entry.isRecalled) {
            boolean alreadyPinned = isMessagePinned(msgId);
            JMenuItem itemPin = new JMenuItem(alreadyPinned ? "Bỏ ghim tin nhắn" : "Ghim tin nhắn");
            itemPin.setFont(new Font("SansSerif", Font.PLAIN, 13));
            itemPin.setIcon(new ZaloIcon(ZaloIcon.Type.PIN, 15, ZALO_BLUE));
            itemPin.addActionListener(ev -> {
                if (alreadyPinned) unpinMessage(msgId);
                else pinMessage(entry);
            });
            menu.add(itemPin);

            if (entry.copyableText != null && !entry.copyableText.isEmpty()) {
                JMenuItem itemCopy = new JMenuItem("Sao chép tin nhắn");
                itemCopy.setFont(new Font("SansSerif", Font.PLAIN, 13));
                itemCopy.addActionListener(ev -> {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(entry.copyableText), null);
                });
                menu.add(itemCopy);
            }

            if (entry.isMe) {
                menu.addSeparator();
                JMenuItem itemRecall = new JMenuItem("Thu hồi tin nhắn");
                itemRecall.setFont(new Font("SansSerif", Font.BOLD, 13));
                itemRecall.setForeground(new Color(220, 53, 69));
                itemRecall.addActionListener(ev -> recallMessage(msgId));
                menu.add(itemRecall);
            }
        } else {
            JMenuItem itemInfo = new JMenuItem("Tin nhắn đã được thu hồi");
            itemInfo.setEnabled(false);
            menu.add(itemInfo);
        }

        menu.show(invoker, x, y);
    }

    private void attachBubbleContextMenu(JComponent target, String msgId) {
        MouseAdapter menuAdapter = new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                showBubbleActionMenu(e.getComponent(), e.getX(), e.getY(), msgId);
            }

            @Override
            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }

            @Override
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
        };
        attachMouseListenerRecursive(target, menuAdapter);
    }

    private void attachMouseListenerRecursive(Component comp, MouseAdapter adapter) {
        comp.addMouseListener(adapter);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                attachMouseListenerRecursive(child, adapter);
            }
        }
    }

    private void pinMessage(ChatBubbleEntry entry) {
        if (entry == null || entry.isRecalled) return;
        String senderDisplay = entry.isMe ? currentFullname : (entry.senderName != null && !entry.senderName.isEmpty() ? entry.senderName : entry.senderUsername);
        String cleanPreview = entry.previewText.replace(";;;", " ").replace("||", " ").replace("^", " ").replace("\n", " ").trim();
        if (cleanPreview.length() > 90) cleanPreview = cleanPreview.substring(0, 90) + "...";

        applyPinLocally(entry.msgId, senderDisplay, cleanPreview);

        String convKey = getCanonicalPinnedConvKey();
        if (convKey != null && !isMulticastMode) {
            sendToServer("PINMSG;;;" + convKey + ";;;" + entry.msgId + ";;;" + senderDisplay + ";;;" + cleanPreview);
        }

        // Đồng bộ realtime qua P2P / Group / Multicast
        try {
            if (isMulticastMode && mcSocket != null) {
                byte[] dt = ("PIN_MC:::" + entry.msgId + ":::" + senderDisplay + ":::" + cleanPreview).getBytes("UTF-8");
                mcSocket.send(new DatagramPacket(dt, dt.length, mcGroup, 9999));
            } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                String payload = currentGroupId + ":::PIN:::" + entry.msgId + ":::" + senderDisplay + ":::" + cleanPreview;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 14;
                System.arraycopy(b, 0, pkt, 1, b.length);
                for (Friend m : groups.get(currentGroupId).members) {
                    if (!m.username.equals(currentUser) && m.isOnline()) {
                        udpSocket.send(new DatagramPacket(pkt, pkt.length, InetAddress.getByName(m.ip), m.port));
                    }
                }
            } else if (currentTargetUser != null && friends.containsKey(currentTargetUser)) {
                String payload = "PIN:::" + entry.msgId + ":::" + senderDisplay + ":::" + cleanPreview;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 13;
                System.arraycopy(b, 0, pkt, 1, b.length);
                sendP2P(friends.get(currentTargetUser), pkt);
            }
        } catch (Exception e) {}
    }

    private void unpinMessage(String msgId) {
        applyUnpinLocally(msgId);

        String convKey = getCanonicalPinnedConvKey();
        if (convKey != null && !isMulticastMode) {
            sendToServer("UNPINMSG;;;" + convKey + ";;;" + msgId);
        }

        try {
            if (isMulticastMode && mcSocket != null) {
                byte[] dt = ("UNPIN_MC:::" + msgId).getBytes("UTF-8");
                mcSocket.send(new DatagramPacket(dt, dt.length, mcGroup, 9999));
            } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                String payload = currentGroupId + ":::UNPIN:::" + msgId;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 14;
                System.arraycopy(b, 0, pkt, 1, b.length);
                for (Friend m : groups.get(currentGroupId).members) {
                    if (!m.username.equals(currentUser) && m.isOnline()) {
                        udpSocket.send(new DatagramPacket(pkt, pkt.length, InetAddress.getByName(m.ip), m.port));
                    }
                }
            } else if (currentTargetUser != null && friends.containsKey(currentTargetUser)) {
                String payload = "UNPIN:::" + msgId;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 13;
                System.arraycopy(b, 0, pkt, 1, b.length);
                sendP2P(friends.get(currentTargetUser), pkt);
            }
        } catch (Exception e) {}
    }

    private void applyPinLocally(String msgId, String senderName, String preview) {
        currentPinnedMessages.removeIf(p -> p.msgId.equals(msgId));
        currentPinnedMessages.add(0, new PinnedMessageItem(msgId, senderName, preview));
        refreshPinnedBanner();
    }

    private void applyUnpinLocally(String msgId) {
        currentPinnedMessages.removeIf(p -> p.msgId.equals(msgId));
        refreshPinnedBanner();
    }

    private void recallMessage(String msgId) {
        ChatBubbleEntry entry = messageBubbles.get(msgId);
        if (entry == null || !entry.isMe || entry.isRecalled) return;

        applyRecallLocally(msgId);

        try {
            if (isMulticastMode && mcSocket != null) {
                byte[] dt = ("RECALL_MC:::" + msgId).getBytes("UTF-8");
                mcSocket.send(new DatagramPacket(dt, dt.length, mcGroup, 9999));
            } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                sendToServer("RECALLGROUPMSG;;;" + currentGroupId + ";;;" + currentUser + ";;;" + msgId);
                String payload = currentGroupId + ":::" + msgId;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 12;
                System.arraycopy(b, 0, pkt, 1, b.length);
                for (Friend m : groups.get(currentGroupId).members) {
                    if (!m.username.equals(currentUser) && m.isOnline()) {
                        udpSocket.send(new DatagramPacket(pkt, pkt.length, InetAddress.getByName(m.ip), m.port));
                    }
                }
            } else if (currentTargetUser != null) {
                sendToServer("RECALLMSG;;;" + currentUser + ";;;" + currentTargetUser + ";;;" + msgId);
                if (friends.containsKey(currentTargetUser)) {
                    byte[] b = msgId.getBytes("UTF-8");
                    byte[] pkt = new byte[b.length + 1];
                    pkt[0] = 11;
                    System.arraycopy(b, 0, pkt, 1, b.length);
                    sendP2P(friends.get(currentTargetUser), pkt);
                }
            }
        } catch (Exception e) {}
    }

    private void applyRecallLocally(String msgId) {
        applyUnpinLocally(msgId);
        ChatBubbleEntry entry = messageBubbles.get(msgId);
        if (entry == null || entry.isRecalled) return;
        entry.isRecalled = true;

        if (entry.btnMore != null) {
            entry.btnMore.setVisible(false);
        }
        if (entry.linkedMedia != null) {
            currentSharedImages.remove(entry.linkedMedia);
            refreshInfoDrawer();
        }
        if (entry.linkedFile != null) {
            currentSharedFiles.remove(entry.linkedFile);
            refreshInfoDrawer();
        }

        entry.bubbleHolder.removeAll();
        BubblePanel recalledBubble = new BubblePanel(
                entry.isMe ? new Color(238, 244, 255) : new Color(248, 249, 250),
                new Color(216, 222, 230)
        );
        JLabel lblRecalled = new JLabel("Tin nhắn đã được thu hồi");
        lblRecalled.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblRecalled.setForeground(new Color(130, 138, 150));
        recalledBubble.add(lblRecalled, BorderLayout.CENTER);

        entry.bubbleHolder.add(recalledBubble);
        entry.bubbleHolder.revalidate();
        entry.bubbleHolder.repaint();
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

    private void openGroupMembersDrawer() {
        if (currentGroupId == -1 || !groups.containsKey(currentGroupId)) return;
        if (!infoDrawerPanel.isVisible()) {
            infoDrawerPanel.setVisible(true);
            btnToggleInfo.setBackground(ZALO_ACTIVE_ITEM);
            btnToggleInfo.setIcon(new ZaloIcon(ZaloIcon.Type.SIDEBAR_RIGHT, 20, ZALO_BLUE));
        }
        refreshGroupMembersCard();
        infoDrawerCardLayout.show(infoDrawerPanel, "MEMBERS");
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    // =========================================================================
    // TẠO KHUNG "THÔNG TIN HỘI THOẠI", "KHO LƯU TRỮ" & "THÀNH VIÊN" GIỐNG ZALO PC
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

        // Khối 2: Thông tin kết nối & Thành viên / Nhóm chung
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
        lblInfoSub2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblInfoSub2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentGroupId != -1) openGroupMembersDrawer();
            }
        });

        metaCard.add(lblInfoSub1);
        metaCard.add(Box.createVerticalStrut(12));
        metaCard.add(lblInfoSub2);

        // Khối 3: Ảnh/Video đã chia sẻ
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

        // ---------------------------------------------------------------------
        // THẺ 3: THÀNH VIÊN NHÓM (GIỐNG HỆT ẢNH MẪU ZALO PC)
        // ---------------------------------------------------------------------
        JPanel membersCardView = createGroupMembersDrawerCard();

        drawerRoot.add(infoCardView, "INFO");
        drawerRoot.add(archiveCardView, "ARCHIVE");
        drawerRoot.add(membersCardView, "MEMBERS");
        return drawerRoot;
    }

    private JPanel createGroupMembersDrawerCard() {
        JPanel membersView = new JPanel(new BorderLayout());
        membersView.setBackground(Color.WHITE);

        // Header: Nút quay lại "<" + tiêu đề "Thành viên"
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(Color.WHITE);
        navBar.setPreferredSize(new Dimension(0, 66));
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ZALO_DIVIDER),
                new EmptyBorder(0, 8, 0, 14)
        ));

        FlatButton btnBack = new FlatButton("‹", new Color(0, 0, 0, 0), ZALO_TEXT_DARK, 8);
        btnBack.setFont(new Font("SansSerif", Font.PLAIN, 28));
        btnBack.setPreferredSize(new Dimension(38, 38));
        btnBack.setToolTipText("Quay lại Thông tin hội thoại");
        btnBack.addActionListener(e -> showInfoDrawerMain());

        JLabel lblTitle = new JLabel("Thành viên", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setForeground(ZALO_TEXT_DARK);

        JLabel dummyRight = new JLabel();
        dummyRight.setPreferredSize(new Dimension(38, 38));

        navBar.add(btnBack, BorderLayout.WEST);
        navBar.add(lblTitle, BorderLayout.CENTER);
        navBar.add(dummyRight, BorderLayout.EAST);

        // Body cuộn
        JPanel body = new DrawerBodyPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Nút "Thêm thành viên" lớn ở đầu giống hệt ảnh Zalo PC
        FlatButton btnAddMember = new FlatButton("  Thêm thành viên", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnAddMember.setIcon(new ZaloIcon(ZaloIcon.Type.ADD_FRIEND, 18, ZALO_TEXT_DARK));
        btnAddMember.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAddMember.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAddMember.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnAddMember.setPreferredSize(new Dimension(0, 38));
        btnAddMember.addActionListener(e -> showAddGroupMemberDialog());

        // Hàng tiêu đề "Danh sách thành viên (X)" + nút "..."
        JPanel listHeaderRow = new JPanel(new BorderLayout());
        listHeaderRow.setOpaque(false);
        listHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        listHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        listHeaderRow.setBorder(new EmptyBorder(12, 2, 6, 2));

        lblMembersSectionTitle = new JLabel("Danh sách thành viên (0)");
        lblMembersSectionTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblMembersSectionTitle.setForeground(ZALO_TEXT_DARK);

        JLabel lblDots = new JLabel("•••");
        lblDots.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDots.setForeground(ZALO_ICON_GRAY);

        listHeaderRow.add(lblMembersSectionTitle, BorderLayout.WEST);
        listHeaderRow.add(lblDots, BorderLayout.EAST);

        pnlGroupMembersList = new JPanel();
        pnlGroupMembersList.setLayout(new BoxLayout(pnlGroupMembersList, BoxLayout.Y_AXIS));
        pnlGroupMembersList.setOpaque(false);
        pnlGroupMembersList.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Nút "Rời nhóm" ngay dưới danh sách thành viên
        FlatButton btnLeaveGroup = new FlatButton("  Rời nhóm", new Color(255, 238, 240), new Color(220, 53, 69), 8);
        btnLeaveGroup.setIcon(new ZaloIcon(ZaloIcon.Type.NAV_LOGOUT, 16, new Color(220, 53, 69)));
        btnLeaveGroup.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLeaveGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLeaveGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnLeaveGroup.setPreferredSize(new Dimension(0, 38));
        btnLeaveGroup.addActionListener(e -> confirmLeaveCurrentGroup());

        body.add(btnAddMember);
        body.add(listHeaderRow);
        body.add(pnlGroupMembersList);
        body.add(Box.createVerticalStrut(14));
        body.add(btnLeaveGroup);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        membersView.add(navBar, BorderLayout.NORTH);
        membersView.add(scroll, BorderLayout.CENTER);
        return membersView;
    }
    private void confirmLeaveCurrentGroup() {
        if (currentGroupId == -1 || !groups.containsKey(currentGroupId)) return;
        Group g = groups.get(currentGroupId);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn rời khỏi nhóm \"" + g.name + "\" không?",
                "Rời nhóm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        int leavingGroupId = currentGroupId;
        sendToServer("LEAVEGROUP;;;" + leavingGroupId + ";;;" + currentUser);
        groups.remove(leavingGroupId);
        pinnedConversations.remove("GRP_" + leavingGroupId);
        mutedConversations.remove("GRP_" + leavingGroupId);

        currentGroupId = -1;
        currentTargetUser = null;
        btnGroupMembers.setVisible(false);
        txtChat.setEnabled(false);
        btnSend.setEnabled(false);
        btnIcon.setEnabled(false);
        btnCall.setEnabled(false);
        btnVideoHeader.setEnabled(false);
        btnRecord.setEnabled(false);
        btnImg.setEnabled(false);
        btnFile.setEnabled(false);

        lblTarget.setText("Chào mừng đến với Z-Voice");
        lblTargetStatus.setText("Chọn bạn bè hoặc tạo nhóm để bắt đầu trò chuyện");
        lblTargetStatus.setForeground(new Color(110, 118, 130));
        targetAvatarHeader.setImage(null);
        targetAvatarHeader.setAvatarText("Z");
        targetAvatarHeader.setBackground(ZALO_BLUE);
        targetAvatarHeader.setOnlineStatus(false, false);

        chatPanel.removeAll();
        chatPanel.revalidate();
        chatPanel.repaint();
        resetConversationViewState();
        showInfoDrawerMain();
        refreshInfoDrawer();

        lastDataSync = "";
        sendToServer("GETDATA;;;" + currentUser);
        renderConversationList();
    }

    private void refreshGroupMembersCard() {
        if (pnlGroupMembersList == null || lblMembersSectionTitle == null) return;
        pnlGroupMembersList.removeAll();

        if (currentGroupId == -1 || !groups.containsKey(currentGroupId)) {
            lblMembersSectionTitle.setText("Danh sách thành viên (0)");
            pnlGroupMembersList.revalidate();
            pnlGroupMembersList.repaint();
            return;
        }

        Group g = groups.get(currentGroupId);
        lblMembersSectionTitle.setText("Danh sách thành viên (" + g.members.size() + ")");

        for (int i = 0; i < g.members.size(); i++) {
            Friend m = g.members.get(i);
            boolean isMe = m.username.equals(currentUser);
            String displayName = isMe ? "Bạn" : m.name;
            String roleText;
            if (i == 0) {
                roleText = isMe ? "Trưởng nhóm • Đang hoạt động" : (m.isOnline() ? "Trưởng nhóm • Đang hoạt động" : "Trưởng nhóm");
            } else {
                roleText = (isMe || m.isOnline()) ? "Đang hoạt động" : "Ngoại tuyến";
            }

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(7, 4, 7, 4));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            String initStr = (m.name != null && !m.name.isEmpty()) ? m.name.substring(0, 1).toUpperCase() : "?";
            RoundAvatar av = new RoundAvatar(initStr);
            av.setPreferredSize(new Dimension(40, 40));
            av.setBackground(i == 0 ? new Color(0, 150, 136) : ZALO_BLUE);
            av.setForeground(Color.WHITE);
            av.setFont(new Font("SansSerif", Font.BOLD, 15));
            av.setOnlineStatus(true, isMe || m.isOnline());

            if (isMe && currentAvatarBase64 != null && !currentAvatarBase64.isEmpty()) {
                try {
                    byte[] imgBytes = Base64.getDecoder().decode(currentAvatarBase64);
                    av.setImage(ImageIO.read(new ByteArrayInputStream(imgBytes)));
                    av.setAvatarText("");
                } catch (Exception ex) {}
            } else if (avatarCache.containsKey(m.username)) {
                av.setImage(avatarCache.get(m.username));
                av.setAvatarText("");
            }

            JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 1));
            textCol.setOpaque(false);

            JLabel lblName = new JLabel(displayName);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblName.setForeground(ZALO_TEXT_DARK);

            JLabel lblRole = new JLabel(roleText);
            lblRole.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblRole.setForeground((isMe || m.isOnline()) ? new Color(46, 160, 95) : new Color(130, 138, 150));

            textCol.add(lblName);
            textCol.add(lblRole);

            row.add(av, BorderLayout.WEST);
            row.add(textCol, BorderLayout.CENTER);

            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { row.setBackground(ZALO_HOVER_ITEM); }
                @Override
                public void mouseExited(MouseEvent e) { row.setBackground(Color.WHITE); }
            });

            pnlGroupMembersList.add(row);
        }

        pnlGroupMembersList.revalidate();
        pnlGroupMembersList.repaint();
    }

    private void showAddGroupMemberDialog() {
        if (currentGroupId == -1 || !groups.containsKey(currentGroupId)) return;
        Group g = groups.get(currentGroupId);
        Set<String> existingUsernames = new HashSet<>();
        for (Friend m : g.members) existingUsernames.add(m.username);

        List<Friend> candidates = new ArrayList<>();
        for (Friend f : friends.values()) {
            if (!existingUsernames.contains(f.username)) candidates.add(f);
        }

        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tất cả bạn bè của bạn đều đã có mặt trong nhóm này!", "Thêm thành viên", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(this, "Thêm thành viên vào \"" + g.name + "\"", true);
        dlg.setSize(380, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(Color.WHITE);

        JLabel lblHint = new JLabel("Chọn bạn bè muốn mời thêm vào nhóm:");
        lblHint.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblHint.setForeground(ZALO_TEXT_DARK);
        lblHint.setBorder(new EmptyBorder(14, 16, 10, 16));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        Map<String, JCheckBox> cbs = new LinkedHashMap<>();
        for (Friend f : candidates) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(8, 16, 8, 16));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            RoundAvatar av = new RoundAvatar(f.name.substring(0, 1).toUpperCase());
            av.setPreferredSize(new Dimension(36, 36));
            av.setBackground(ZALO_BLUE);
            av.setForeground(Color.WHITE);
            if (avatarCache.containsKey(f.username)) {
                av.setImage(avatarCache.get(f.username));
                av.setAvatarText("");
            }

            JCheckBox cb = new JCheckBox(f.name + " (@" + f.username + ")");
            cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
            cb.setOpaque(false);
            cbs.put(f.username, cb);

            row.add(av, BorderLayout.WEST);
            row.add(cb, BorderLayout.CENTER);
            listPanel.add(row);
        }

        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, ZALO_DIVIDER));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setBackground(Color.WHITE);
        FlatButton btnCancel = new FlatButton("Hủy", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnCancel.setPreferredSize(new Dimension(76, 34));
        btnCancel.addActionListener(e -> dlg.dispose());

        FlatButton btnOk = new FlatButton("Xác nhận", ZALO_BLUE, Color.WHITE, 8);
        btnOk.setPreferredSize(new Dimension(96, 34));
        btnOk.addActionListener(e -> {
            List<String> picked = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : cbs.entrySet()) {
                if (entry.getValue().isSelected()) {
                    picked.add(entry.getKey());
                    Friend addedF = friends.get(entry.getKey());
                    if (addedF != null) g.members.add(addedF);
                }
            }
            if (picked.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn ít nhất 1 người bạn!");
                return;
            }
            sendToServer("ADDGROUPMEMBER;;;" + g.id + ";;;" + String.join(",", picked));
            lastDataSync = "";
            sendToServer("GETDATA;;;" + currentUser);
            refreshGroupMembersCard();
            refreshInfoDrawer();
            renderConversationList();
            dlg.dispose();
        });

        bottom.add(btnCancel);
        bottom.add(btnOk);

        dlg.add(lblHint, BorderLayout.NORTH);
        dlg.add(sp, BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);
        dlg.setVisible(true);
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

    private void resetConversationViewState() {
        messageBubbles.clear();
        legacyCounter = 0;
        currentPinnedMessages.clear();
        refreshPinnedBanner();
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

        refreshGroupMembersCard();
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

    // =========================================================================
    // GIAO DIỆN CẬP NHẬT HỒ SƠ & AVATAR HIỆN ĐẠI CHUẨN ZALO PC
    // =========================================================================
    private void showProfileDialog() {
        JDialog dlg = new JDialog(this, "Cập nhật thông tin cá nhân", true);
        dlg.setSize(410, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(Color.WHITE);

        // Banner xanh trên cùng + Avatar tròn nổi bật ở giữa
        JPanel topBanner = new JPanel();
        topBanner.setLayout(new BoxLayout(topBanner, BoxLayout.Y_AXIS));
        topBanner.setBackground(new Color(242, 247, 255));
        topBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ZALO_DIVIDER),
                new EmptyBorder(22, 20, 20, 20)
        ));

        RoundAvatar previewAvatar = new RoundAvatar(currentUser.substring(0, 1).toUpperCase());
        previewAvatar.setPreferredSize(new Dimension(92, 92));
        previewAvatar.setMaximumSize(new Dimension(92, 92));
        previewAvatar.setBackground(ZALO_BLUE);
        previewAvatar.setForeground(Color.WHITE);
        previewAvatar.setFont(new Font("SansSerif", Font.BOLD, 34));
        previewAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewAvatar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        final String[] newAvatar = {currentAvatarBase64};
        if (currentAvatarBase64 != null && !currentAvatarBase64.isEmpty()) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(currentAvatarBase64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                if (img != null) {
                    previewAvatar.setImage(img);
                    previewAvatar.setAvatarText("");
                }
            } catch (Exception ex) {}
        }

        FlatButton btnPickImg = new FlatButton("  Đổi ảnh đại diện", Color.WHITE, ZALO_BLUE, 18);
        btnPickImg.setIcon(new ZaloIcon(ZaloIcon.Type.IMAGE, 16, ZALO_BLUE));
        btnPickImg.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnPickImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPickImg.setMaximumSize(new Dimension(165, 34));
        btnPickImg.setPreferredSize(new Dimension(165, 34));

        Runnable chooseAvatarAction = () -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage raw = ImageIO.read(fc.getSelectedFile());
                    if (raw == null) {
                        JOptionPane.showMessageDialog(dlg, "Tệp được chọn không phải hình ảnh hợp lệ!");
                        return;
                    }
                    // Cắt vuông chính giữa để ảnh đại diện không bị méo tỷ lệ
                    int crop = Math.min(raw.getWidth(), raw.getHeight());
                    BufferedImage square = raw.getSubimage((raw.getWidth() - crop) / 2, (raw.getHeight() - crop) / 2, crop, crop);

                    BufferedImage bimg = new BufferedImage(180, 180, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = bimg.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(square, 0, 0, 180, 180, null);
                    g2d.dispose();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bimg, "jpg", baos);
                    newAvatar[0] = Base64.getEncoder().encodeToString(baos.toByteArray());

                    previewAvatar.setImage(bimg);
                    previewAvatar.setAvatarText("");
                    previewAvatar.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Không thể đọc ảnh đại diện!");
                }
            }
        };

        btnPickImg.addActionListener(e -> chooseAvatarAction.run());
        previewAvatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { chooseAvatarAction.run(); }
        });

        topBanner.add(previewAvatar);
        topBanner.add(Box.createVerticalStrut(12));
        topBanner.add(btnPickImg);

        // Khu vực nhập thông tin
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(18, 24, 14, 24));

        JLabel lblNameTitle = new JLabel("Tên hiển thị");
        lblNameTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblNameTitle.setForeground(ZALO_TEXT_DARK);
        lblNameTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameField = new JTextField(currentFullname);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nameField.setPreferredSize(new Dimension(0, 38));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUserInfo = new JLabel("Tài khoản đăng nhập: @" + currentUser);
        lblUserInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblUserInfo.setForeground(new Color(120, 128, 140));
        lblUserInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(lblNameTitle);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(nameField);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(lblUserInfo);

        // Thanh nút bấm dưới cùng
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ZALO_DIVIDER));

        FlatButton btnCancel = new FlatButton("Hủy", ZALO_HOVER_ITEM, ZALO_TEXT_DARK, 8);
        btnCancel.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(84, 36));
        btnCancel.addActionListener(e -> dlg.dispose());

        FlatButton btnSave = new FlatButton("Cập nhật", ZALO_BLUE, Color.WHITE, 8);
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(105, 36));
        btnSave.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Tên hiển thị không được để trống!");
                return;
            }
            sendToServer("UPDATE;;;" + currentUser + ";;;" + newName + ";;;" + (newAvatar[0] == null ? "" : newAvatar[0]));
            currentFullname = newName;
            currentAvatarBase64 = newAvatar[0];
            updateAvatarDisplay();
            setTitle("Z-Voice - " + currentFullname);
            dlg.dispose();
        });

        bottomBar.add(btnCancel);
        bottomBar.add(btnSave);

        dlg.add(topBanner, BorderLayout.NORTH);
        dlg.add(formPanel, BorderLayout.CENTER);
        dlg.add(bottomBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
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

    private Friend findUserInFriendsOrGroups(String username, String fallbackIp, int fallbackPort) {
        if (username != null && friends.containsKey(username)) return friends.get(username);
        for (Group g : groups.values()) {
            for (Friend m : g.members) {
                if (m.username.equals(username)) return m;
            }
        }
        if (username != null && fallbackIp != null) {
            return new Friend(username, username, fallbackIp, fallbackPort);
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
                    String senderIp = packet.getAddress().getHostAddress();
                    int senderPort = packet.getPort();
                    Friend senderF = getFriendByIp(senderIp, senderPort);

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
                            String mediaMsgId = String.valueOf(id);
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
                                    if (type == 3) appendAudioMsg(mediaMsgId, senderF.username, senderF.name, finalData, false);
                                    else if (type == 8) appendImageMsg(mediaMsgId, senderF.username, senderF.name, finalData, false, id);
                                    else if (type == 9) appendFileMsg(mediaMsgId, senderF.username, senderF.name, finalData, false);
                                }
                            });
                            incomingData.remove(id); incomingDataReceived.remove(id);
                        }
                    }
                    // Nhận Ảnh / File / Voice trong Nhóm Chat (type 15 = Voice, 16 = Image, 17 = File)
                    else if (type == 15 || type == 16 || type == 17) {
                        ByteBuffer bb = ByteBuffer.wrap(buffer, 1, packet.getLength() - 1);
                        int gId = bb.getInt();
                        long id = bb.getLong();
                        int total = bb.getInt();
                        int offset = bb.getInt();
                        int userLen = bb.getInt();
                        byte[] uBytes = new byte[userLen];
                        bb.get(uBytes);
                        String sUser = new String(uBytes, "UTF-8");

                        int nameLen = bb.getInt();
                        byte[] nBytes = new byte[nameLen];
                        bb.get(nBytes);
                        String sName = new String(nBytes, "UTF-8");

                        int headerSize = 1 + 4 + 8 + 4 + 4 + 4 + userLen + 4 + nameLen;
                        int dataLen = packet.getLength() - headerSize;
                        if (dataLen > 0) {
                            if (!incomingData.containsKey(id)) {
                                incomingData.put(id, new byte[total]);
                                incomingDataReceived.put(id, 0);
                            }
                            byte[] fullData = incomingData.get(id);
                            System.arraycopy(buffer, headerSize, fullData, offset, dataLen);
                            int received = incomingDataReceived.get(id) + dataLen;
                            incomingDataReceived.put(id, received);

                            if (received >= total) {
                                byte[] finalData = fullData.clone();
                                String mediaMsgId = String.valueOf(id);
                                if (type == 16) {
                                    saveMediaToLocal("img_" + id + ".dat", finalData);
                                } else if (type == 17) {
                                    try {
                                        ByteBuffer fbb = ByteBuffer.wrap(finalData);
                                        int fnLen = fbb.getInt();
                                        byte[] fnBytes = new byte[fnLen];
                                        fbb.get(fnBytes);
                                        String fName = new String(fnBytes, "UTF-8");
                                        byte[] fBytes = new byte[finalData.length - 4 - fnLen];
                                        fbb.get(fBytes);
                                        saveMediaToLocal("file_" + id + "_" + fName, fBytes);
                                    } catch (Exception ex) {}
                                }

                                SwingUtilities.invokeLater(() -> {
                                    if (!isMulticastMode && currentGroupId == gId && !sUser.equals(currentUser)) {
                                        if (type == 15) appendAudioMsg(mediaMsgId, sUser, sName, finalData, false);
                                        else if (type == 16) appendImageMsg(mediaMsgId, sUser, sName, finalData, false, id);
                                        else if (type == 17) appendFileMsg(mediaMsgId, sUser, sName, finalData, false);
                                    }
                                });
                                incomingData.remove(id);
                                incomingDataReceived.remove(id);
                            }
                        }
                    }
                    else if (type == 11 && senderF != null) {
                        String targetMsgId = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        SwingUtilities.invokeLater(() -> {
                            if (!isMulticastMode && currentGroupId == -1 && currentTargetUser != null && currentTargetUser.equals(senderF.username)) {
                                applyRecallLocally(targetMsgId);
                            }
                        });
                    }
                    else if (type == 12) {
                        String raw = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        String[] p = raw.split(":::", 2);
                        if (p.length == 2) {
                            int gId = Integer.parseInt(p[0]);
                            String targetMsgId = p[1];
                            SwingUtilities.invokeLater(() -> {
                                if (!isMulticastMode && currentGroupId == gId) {
                                    applyRecallLocally(targetMsgId);
                                }
                            });
                        }
                    }
                    else if (type == 13 && senderF != null) {
                        String raw = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        SwingUtilities.invokeLater(() -> {
                            if (!isMulticastMode && currentGroupId == -1 && currentTargetUser != null && currentTargetUser.equals(senderF.username)) {
                                if (raw.startsWith("PIN:::")) {
                                    String[] p = raw.split(":::", 4);
                                    if (p.length == 4) applyPinLocally(p[1], p[2], p[3]);
                                } else if (raw.startsWith("UNPIN:::")) {
                                    String[] p = raw.split(":::", 2);
                                    if (p.length == 2) applyUnpinLocally(p[1]);
                                }
                            }
                        });
                    }
                    else if (type == 14) {
                        String raw = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        String[] p = raw.split(":::", 5);
                        if (p.length >= 3) {
                            int gId = Integer.parseInt(p[0]);
                            String action = p[1];
                            SwingUtilities.invokeLater(() -> {
                                if (!isMulticastMode && currentGroupId == gId) {
                                    if ("PIN".equals(action) && p.length == 5) applyPinLocally(p[2], p[3], p[4]);
                                    else if ("UNPIN".equals(action)) applyUnpinLocally(p[2]);
                                }
                            });
                        }
                    }
                    // Cuộc gọi nhóm đến (type 18)
                    else if (type == 18) {
                        String raw = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        String[] p = raw.split(":::", 3);
                        if (p.length == 3) {
                            int gId = Integer.parseInt(p[0]);
                            String callerUser = p[1];
                            String callerName = p[2];
                            SwingUtilities.invokeLater(() -> handleIncomingGroupCall(gId, callerUser, callerName));
                        }
                    }
                    else if (type == 4) {
                        String callerUser = senderF != null ? senderF.username : null;
                        if (callerUser == null && packet.getLength() > 1) {
                            callerUser = new String(buffer, 1, packet.getLength() - 1, "UTF-8");
                        }
                        if (callerUser != null) {
                            final String finalCaller = callerUser;
                            SwingUtilities.invokeLater(() -> handleIncomingCall(finalCaller, senderIp, senderPort));
                        }
                    }
                    else if (type == 5) SwingUtilities.invokeLater(this::handleCallAccepted);
                    else if (type == 6) SwingUtilities.invokeLater(this::handleCallDeclined);
                    else if (type == 7) SwingUtilities.invokeLater(this::handleCallEnded);
                }
            } catch (Exception e) {}
        }
    }

    private void processServerMsg(String s) {
        if (s.startsWith("FORCE_SYNC;;;")) {
            lastDataSync = "";
            sendToServer("GETDATA;;;" + currentUser);
            return;
        }
        if (s.startsWith("CREATEGROUP_OK;;;")) {
            String[] p = s.split(";;;");
            if (p.length >= 3) {
                JOptionPane.showMessageDialog(this, "Đã tạo nhóm \"" + p[2] + "\" thành công!", "Z-Voice Nhóm", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        if (s.startsWith("PINDATA;;;")) {
            String[] p = s.split(";;;", 3);
            if (p.length < 3) return;
            String activeKey = getCanonicalPinnedConvKey();
            if (activeKey == null || !activeKey.equals(p[1])) return;

            currentPinnedMessages.clear();
            String listStr = p[2];
            if (!listStr.isEmpty()) {
                for (String item : listStr.split("\\|\\|")) {
                    if (item.isEmpty()) continue;
                    String[] fields = item.split("\\^", 3);
                    if (fields.length == 3) {
                        currentPinnedMessages.add(new PinnedMessageItem(fields[0], fields[1], fields[2]));
                    }
                }
            }
            refreshPinnedBanner();
            return;
        }

        if (s.startsWith("GROUPMSGDATA;;;")) {
            String[] p = s.split(";;;", 3);
            if (p.length < 3) return;
            int gId = Integer.parseInt(p[1]);
            if (isMulticastMode || currentGroupId != gId) return;

            String data = p[2];
            chatPanel.removeAll();
            messageBubbles.clear();
            legacyCounter = 0;
            currentSharedImages.clear();
            currentSharedFiles.clear();
            currentSharedLinks.clear();
            refreshInfoDrawer();

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
            messageBubbles.clear();
            legacyCounter = 0;
            currentSharedImages.clear();
            currentSharedFiles.clear();
            currentSharedLinks.clear();
            refreshInfoDrawer();

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

                // Bật toàn bộ tính năng cho Nhóm Chat
                txtChat.setEnabled(true);
                btnSend.setEnabled(true);
                btnIcon.setEnabled(true);
                btnCall.setEnabled(true);
                btnVideoHeader.setEnabled(true);
                btnRecord.setEnabled(true);
                btnImg.setEnabled(true);
                btnFile.setEnabled(true);
                btnGroupMembers.setVisible(true);

                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();
                resetConversationViewState();
                sendToServer("GETGROUPMSG;;;" + grp.id);
                sendToServer("GETPINS;;;" + getCanonicalPinnedConvKey());

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
                    resetConversationViewState();
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
                resetConversationViewState();
                sendToServer("GETMSG;;;" + currentUser + ";;;" + currentTargetUser);
                sendToServer("GETPINS;;;" + getCanonicalPinnedConvKey());

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
        String msgId = generateMessageId();
        String wireContent = "[MID:" + msgId + "]" + messageContent;

        if (isMulticastMode) {
            try {
                String fullMsg = currentUser + ":::" + currentFullname + ":::" + wireContent;
                byte[] data = fullMsg.getBytes("UTF-8");
                if (mcSocket != null) mcSocket.send(new DatagramPacket(data, data.length, mcGroup, 9999));
            } catch (Exception e) {}
        }
        else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
            try {
                Group grp = groups.get(currentGroupId);
                String payloadStr = currentGroupId + ":::" + currentUser + ":::" + currentFullname + ":::" + wireContent;
                byte[] textBytes = payloadStr.getBytes("UTF-8");
                byte[] packetData = new byte[textBytes.length + 1];
                packetData[0] = 2;
                System.arraycopy(textBytes, 0, packetData, 1, textBytes.length);

                for (Friend member : grp.members) {
                    if (!member.username.equals(currentUser) && member.isOnline()) {
                        udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(member.ip), member.port));
                    }
                }

                sendToServer("SAVEGROUPMSG;;;" + currentGroupId + ";;;" + currentUser + ";;;" + wireContent);
                appendMsg(currentUser, currentFullname, wireContent, true);
            } catch (Exception e) {}
        }
        else {
            if (currentTargetUser == null) return;
            Friend f = friends.get(currentTargetUser);
            if (f != null && f.isOnline()) {
                try {
                    byte[] textBytes = wireContent.getBytes("UTF-8");
                    byte[] packetData = new byte[textBytes.length + 1];
                    packetData[0] = 1;
                    System.arraycopy(textBytes, 0, packetData, 1, textBytes.length);
                    udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(f.ip), f.port));

                    sendToServer("SAVEMSG;;;" + currentUser + ";;;" + f.username + ";;;" + wireContent);
                    appendMsg(currentUser, currentFullname, wireContent, true);
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

    private ChatBubbleEntry addChatBubbleWithTracking(String msgId, JComponent comp, boolean isMe,
                                                      String senderUsername, String senderName,
                                                      String previewText, String copyableText, boolean isRecalled) {
        JPanel w = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 2));
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

        // Nút "..." ngay bên cạnh bong bóng chat để bấm mở menu Ghim / Thu hồi / Sao chép
        FlatButton btnMore = new FlatButton("•••", new Color(255, 255, 255, 210), new Color(100, 110, 125), 999);
        btnMore.setFont(new Font("SansSerif", Font.BOLD, 10));
        btnMore.setPreferredSize(new Dimension(28, 24));
        btnMore.setToolTipText("Ghim, sao chép hoặc thu hồi tin nhắn");
        btnMore.setVisible(!isRecalled);
        btnMore.addActionListener(e -> showBubbleActionMenu(btnMore, 0, btnMore.getHeight() + 2, msgId));

        JPanel bubbleHolder = new JPanel(new BorderLayout());
        bubbleHolder.setOpaque(false);
        bubbleHolder.add(comp, BorderLayout.CENTER);

        if (isMe) {
            w.add(btnMore);
            w.add(bubbleHolder);
        } else {
            w.add(bubbleHolder);
            w.add(btnMore);
        }

        ChatBubbleEntry entry = new ChatBubbleEntry(
                msgId, senderUsername, senderName, previewText, copyableText, isMe, isRecalled, w, bubbleHolder
        );
        entry.btnMore = btnMore;
        messageBubbles.put(msgId, entry);
        attachBubbleContextMenu(bubbleHolder, msgId);

        chatPanel.add(w);
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollChatToBottom();
        return entry;
    }

    private void appendMsg(String senderUsername, String senderName, String rawMsg, boolean isMe) {
        String msgId;
        String msg = rawMsg;

        if (msg.startsWith("[MID:")) {
            int closeIdx = msg.indexOf(']');
            if (closeIdx != -1) {
                msgId = msg.substring(5, closeIdx);
                msg = msg.substring(closeIdx + 1);
            } else {
                msgId = "LEGACY_" + (++legacyCounter);
            }
        } else {
            msgId = "LEGACY_" + (++legacyCounter);
        }

        if ("[RECALLED]".equals(msg)) {
            BubblePanel recalledBubble = new BubblePanel(
                    isMe ? new Color(238, 244, 255) : new Color(248, 249, 250),
                    new Color(216, 222, 230)
            );
            JLabel lblRecalled = new JLabel("Tin nhắn đã được thu hồi");
            lblRecalled.setFont(new Font("SansSerif", Font.ITALIC, 13));
            lblRecalled.setForeground(new Color(130, 138, 150));
            recalledBubble.add(lblRecalled, BorderLayout.CENTER);
            addChatBubbleWithTracking(msgId, recalledBubble, isMe, senderUsername, senderName, "", "", true);
            return;
        }

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
            addChatBubbleWithTracking(msgId, pnlContent, isMe, senderUsername, senderName, "[Nhãn dán]", "", false);
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
                appendImageMsg(msgId, senderUsername, senderName, imgBytes, isMe, ts);
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
            renderHistoricalFileBubble(msgId, senderUsername, senderName, displayName, fileBytes, isMe);
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
            addChatBubbleWithTracking(msgId, bubble, isMe, senderUsername, senderName, msg, msg, false);
        } else {
            JLabel l = new JLabel(msg);
            l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
            pnlContent.add(l);
            addChatBubbleWithTracking(msgId, pnlContent, isMe, senderUsername, senderName, msg, msg, false);
        }
    }

    private void appendImageMsg(String msgId, String senderUsername, String senderName, byte[] imgData, boolean isMe, long timestampMillis) {
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
            l.setToolTipText("Nhấn để phóng to ảnh • Bấm nút ... bên cạnh để Ghim/Thu hồi");
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int idx = currentSharedImages.indexOf(mediaItem);
                        showImagePreviewDialog(Math.max(0, idx));
                    }
                }
            });

            JPanel wrapperContent = new JPanel();
            wrapperContent.setLayout(new BoxLayout(wrapperContent, BoxLayout.Y_AXIS));
            wrapperContent.setOpaque(false);
            if ((isMulticastMode || currentGroupId != -1) && !isMe && senderName != null && !senderName.isEmpty()) {
                JLabel lblName = new JLabel(senderName);
                lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblName.setForeground(new Color(110, 118, 130));
                lblName.setBorder(new EmptyBorder(0, 0, 3, 0));
                wrapperContent.add(lblName);
            }
            wrapperContent.add(l);

            BubblePanel bubble = isMe
                    ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                    : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
            bubble.add(wrapperContent, BorderLayout.CENTER);
            ChatBubbleEntry entry = addChatBubbleWithTracking(msgId, bubble, isMe, senderUsername, senderName, "[Hình ảnh]", "", false);
            entry.linkedMedia = mediaItem;
        } catch (Exception e) {}
    }

    private void appendFileMsg(String msgId, String senderUsername, String senderName, byte[] payload, boolean isMe) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(payload);
            int nameLen = bb.getInt();
            byte[] nameBytes = new byte[nameLen];
            bb.get(nameBytes);
            String fileName = new String(nameBytes, "UTF-8");
            byte[] fileData = new byte[payload.length - 4 - nameLen];
            bb.get(fileData);

            SharedFileItem fileItem = new SharedFileItem(fileName, fileData);
            currentSharedFiles.add(0, fileItem);
            refreshInfoDrawer();

            Color bg = isMe ? ZALO_SENT_BUBBLE : Color.WHITE;
            FlatButton btn = new FlatButton("  " + fileName, bg, ZALO_TEXT_DARK, 8);
            btn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 18, ZALO_BLUE));
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            btn.addActionListener(e -> saveSharedFileToDisk(fileName, fileData));

            JPanel wrapperContent = new JPanel();
            wrapperContent.setLayout(new BoxLayout(wrapperContent, BoxLayout.Y_AXIS));
            wrapperContent.setOpaque(false);
            if ((isMulticastMode || currentGroupId != -1) && !isMe && senderName != null && !senderName.isEmpty()) {
                JLabel lblName = new JLabel(senderName);
                lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblName.setForeground(new Color(110, 118, 130));
                lblName.setBorder(new EmptyBorder(0, 0, 3, 0));
                wrapperContent.add(lblName);
            }
            wrapperContent.add(btn);

            BubblePanel bubble = isMe
                    ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                    : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
            bubble.add(wrapperContent, BorderLayout.CENTER);
            ChatBubbleEntry entry = addChatBubbleWithTracking(msgId, bubble, isMe, senderUsername, senderName, "[Tệp] " + fileName, fileName, false);
            entry.linkedFile = fileItem;
        } catch (Exception e) {}
    }

    private void appendAudioMsg(String msgId, String senderUsername, String senderName, byte[] audioData, boolean isMe) {
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

        JPanel wrapperContent = new JPanel();
        wrapperContent.setLayout(new BoxLayout(wrapperContent, BoxLayout.Y_AXIS));
        wrapperContent.setOpaque(false);
        if ((isMulticastMode || currentGroupId != -1) && !isMe && senderName != null && !senderName.isEmpty()) {
            JLabel lblName = new JLabel(senderName);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblName.setForeground(new Color(110, 118, 130));
            lblName.setBorder(new EmptyBorder(0, 0, 3, 0));
            wrapperContent.add(lblName);
        }
        wrapperContent.add(btnPlay);

        BubblePanel bubble = isMe
                ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
        bubble.add(wrapperContent, BorderLayout.CENTER);
        addChatBubbleWithTracking(msgId, bubble, isMe, senderUsername, senderName, "[Tin nhắn thoại]", "", false);
    }

    private boolean autoStartVideoOnCall = false;

    // =========================================================================
    // HỆ THỐNG GỌI THOẠI & GỌI VIDEO CHO CẢ 1-1, NHÓM CHAT VÀ PHÒNG LAN
    // =========================================================================
    private void initiateCall(boolean withVideo) {
        if (isMulticastMode) {
            autoStartVideoOnCall = withVideo;
            isGroupConferenceCall = true;
            currentCallUser = "LAN_ROOM";

            try {
                byte[] dt = ("CALL_MC:::" + currentUser + ":::" + currentFullname).getBytes("UTF-8");
                if (mcSocket != null) mcSocket.send(new DatagramPacket(dt, dt.length, mcGroup, 9999));
            } catch (Exception e) {}

            pnlTargetVideo.setAvatarText("LAN");
            pnlTargetVideo.setFrame(null);
            lblCallName.setText("Phòng Họp LAN Chung");
            lblCallStatus.setText("Đang kết nối âm thanh & video mạng LAN...");
            btnAcceptCall.setVisible(false);
            btnDeclineCall.setVisible(false);
            btnEndCall.setVisible(true);
            btnToggleVideo.setVisible(true);
            cardLayout.show(rightPanel, "CALL");

            startAudioStreams();
            if (withVideo && !isVideoOn) toggleVideo();
            return;
        }

        if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
            Group g = groups.get(currentGroupId);
            autoStartVideoOnCall = withVideo;
            isGroupConferenceCall = true;
            currentCallUser = "GROUP_" + currentGroupId;

            try {
                String payload = currentGroupId + ":::" + currentUser + ":::" + currentFullname;
                byte[] b = payload.getBytes("UTF-8");
                byte[] pkt = new byte[b.length + 1];
                pkt[0] = 18;
                System.arraycopy(b, 0, pkt, 1, b.length);
                for (Friend m : g.members) {
                    if (!m.username.equals(currentUser) && m.isOnline()) {
                        udpSocket.send(new DatagramPacket(pkt, pkt.length, InetAddress.getByName(m.ip), m.port));
                    }
                }
            } catch (Exception e) {}

            pnlTargetVideo.setAvatarText(g.name.substring(0, 1).toUpperCase());
            pnlTargetVideo.setFrame(null);
            lblCallName.setText(g.name);
            lblCallStatus.setText("Cuộc gọi nhóm đang diễn ra...");
            btnAcceptCall.setVisible(false);
            btnDeclineCall.setVisible(false);
            btnEndCall.setVisible(true);
            btnToggleVideo.setVisible(true);
            cardLayout.show(rightPanel, "CALL");

            startAudioStreams();
            if (withVideo && !isVideoOn) toggleVideo();
            return;
        }

        if (currentTargetUser == null) return;
        Friend f = friends.get(currentTargetUser);
        if (f == null || !f.isOnline()) {
            JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
            return;
        }
        isGroupConferenceCall = false;
        autoStartVideoOnCall = withVideo;
        currentCallUser = currentTargetUser;

        try {
            byte[] uBytes = currentUser.getBytes("UTF-8");
            byte[] pkt = new byte[uBytes.length + 1];
            pkt[0] = 4;
            System.arraycopy(uBytes, 0, pkt, 1, uBytes.length);
            sendP2P(f, pkt);
        } catch (Exception e) {
            sendP2P(f, new byte[]{4});
        }

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

    private void handleIncomingGroupCall(int gId, String callerUser, String callerName) {
        if (currentCallUser != null) return;
        Group g = groups.get(gId);
        if (g == null) return;

        isGroupConferenceCall = true;
        autoStartVideoOnCall = false;
        currentGroupId = gId;
        currentCallUser = "GROUP_" + gId;

        pnlTargetVideo.setAvatarText(g.name.substring(0, 1).toUpperCase());
        pnlTargetVideo.setFrame(null);
        lblCallName.setText(g.name);
        lblCallStatus.setText(callerName + " đang gọi nhóm...");
        btnAcceptCall.setVisible(true);
        btnDeclineCall.setVisible(true);
        btnEndCall.setVisible(false);
        btnToggleVideo.setVisible(false);

        Toolkit.getDefaultToolkit().beep();
        cardLayout.show(rightPanel, "CALL");
    }

    private void handleIncomingMulticastCall(String callerName) {
        if (currentCallUser != null) return;
        isGroupConferenceCall = true;
        autoStartVideoOnCall = false;
        currentCallUser = "LAN_ROOM";

        pnlTargetVideo.setAvatarText("LAN");
        pnlTargetVideo.setFrame(null);
        lblCallName.setText("Phòng Họp LAN Chung");
        lblCallStatus.setText(callerName + " đang gọi trong mạng LAN...");
        btnAcceptCall.setVisible(true);
        btnDeclineCall.setVisible(true);
        btnEndCall.setVisible(false);
        btnToggleVideo.setVisible(false);

        Toolkit.getDefaultToolkit().beep();
        cardLayout.show(rightPanel, "CALL");
    }

    private void handleIncomingCall(String callerUsername, String fallbackIp, int fallbackPort) {
        if (currentCallUser != null) {
            Friend busyF = findUserInFriendsOrGroups(callerUsername, fallbackIp, fallbackPort);
            sendP2P(busyF, new byte[]{6});
            return;
        }
        isGroupConferenceCall = false;
        autoStartVideoOnCall = false;
        currentCallUser = callerUsername;
        Friend f = findUserInFriendsOrGroups(callerUsername, fallbackIp, fallbackPort);
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
        if (!isGroupConferenceCall) {
            sendP2P(friends.get(currentCallUser), new byte[]{5});
        }
        lblCallStatus.setText(isGroupConferenceCall ? "Đang tham gia cuộc gọi nhóm" : "Đang trò chuyện");
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
        if (!isGroupConferenceCall) {
            sendP2P(friends.get(currentCallUser), new byte[]{6});
        }
        closeCallUI();
    }

    private void handleCallDeclined() {
        JOptionPane.showMessageDialog(this, "Người dùng đã từ chối cuộc gọi");
        closeCallUI();
    }

    private void endCall() {
        if (currentCallUser == null) return;
        if (!isGroupConferenceCall) {
            sendP2P(friends.get(currentCallUser), new byte[]{7});
        }
        closeCallUI();
    }

    private void handleCallEnded() { closeCallUI(); }

    private void closeCallUI() {
        currentCallUser = null;
        isGroupConferenceCall = false;
        autoStartVideoOnCall = false;
        stopAudioStreams();
        stopVideo();
        btnToggleVideo.setText(" Bật Cam");
        btnToggleVideo.setBackground(ZALO_BLUE);
        btnToggleVideo.setVisible(false);
        pnlTargetVideo.setFrame(null);
        cardLayout.show(rightPanel, "CHAT");
    }

    private void sendCallPacketToActiveTargets(byte[] packetData) {
        try {
            if ("LAN_ROOM".equals(currentCallUser)) {
                if (mcSocket != null && mcGroup != null) {
                    mcSocket.send(new DatagramPacket(packetData, packetData.length, mcGroup, 9999));
                }
            } else if (currentCallUser != null && currentCallUser.startsWith("GROUP_")) {
                int gId = Integer.parseInt(currentCallUser.substring(6));
                Group g = groups.get(gId);
                if (g != null) {
                    for (Friend m : g.members) {
                        if (!m.username.equals(currentUser) && m.isOnline()) {
                            udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(m.ip), m.port));
                        }
                    }
                }
            } else if (currentCallUser != null) {
                Friend f = friends.get(currentCallUser);
                if (f != null && f.isOnline()) {
                    udpSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName(f.ip), f.port));
                }
            }
        } catch (Exception e) {}
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
                    while (isCalling && currentCallUser != null) {
                        int read = mic.read(audioBuffer, 0, audioBuffer.length);
                        if (read > 0) {
                            System.arraycopy(audioBuffer, 0, packetData, 1, read);
                            sendCallPacketToActiveTargets(packetData);
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

    // =========================================================================
    // TRUYỀN FILE / ẢNH / VOICE PHÂN MẢNH CHO CẢ 1-1, NHÓM VÀ MULTICAST LAN
    // =========================================================================
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

    private void sendFragmentedGroupData(Group grp, byte type, byte[] fullData, long msgId) {
        new Thread(() -> {
            try {
                byte[] uBytes = currentUser.getBytes("UTF-8");
                byte[] nBytes = currentFullname.getBytes("UTF-8");
                int headerSize = 1 + 4 + 8 + 4 + 4 + 4 + uBytes.length + 4 + nBytes.length;
                int maxChunk = 50000 - headerSize;
                int offset = 0;
                while (offset < fullData.length) {
                    int length = Math.min(maxChunk, fullData.length - offset);
                    ByteBuffer bb = ByteBuffer.allocate(headerSize + length);
                    bb.put(type);
                    bb.putInt(grp.id);
                    bb.putLong(msgId);
                    bb.putInt(fullData.length);
                    bb.putInt(offset);
                    bb.putInt(uBytes.length);
                    bb.put(uBytes);
                    bb.putInt(nBytes.length);
                    bb.put(nBytes);
                    bb.put(fullData, offset, length);
                    byte[] pkt = bb.array();

                    for (Friend m : grp.members) {
                        if (!m.username.equals(currentUser) && m.isOnline()) {
                            udpSocket.send(new DatagramPacket(pkt, pkt.length, InetAddress.getByName(m.ip), m.port));
                        }
                    }
                    offset += length;
                    Thread.sleep(15);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void sendFragmentedMulticastData(byte type, byte[] fullData, long msgId) {
        new Thread(() -> {
            try {
                if (mcSocket == null || mcGroup == null) return;
                byte[] uBytes = currentUser.getBytes("UTF-8");
                byte[] nBytes = currentFullname.getBytes("UTF-8");
                int headerSize = 1 + 8 + 4 + 4 + 4 + uBytes.length + 4 + nBytes.length;
                int maxChunk = 48000 - headerSize;
                int offset = 0;
                while (offset < fullData.length) {
                    int length = Math.min(maxChunk, fullData.length - offset);
                    ByteBuffer bb = ByteBuffer.allocate(headerSize + length);
                    bb.put(type);
                    bb.putLong(msgId);
                    bb.putInt(fullData.length);
                    bb.putInt(offset);
                    bb.putInt(uBytes.length);
                    bb.put(uBytes);
                    bb.putInt(nBytes.length);
                    bb.put(nBytes);
                    bb.put(fullData, offset, length);
                    byte[] pkt = bb.array();

                    mcSocket.send(new DatagramPacket(pkt, pkt.length, mcGroup, 9999));
                    offset += length;
                    Thread.sleep(15);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void sendFileOrImage(int type) {
        if (!isMulticastMode && currentGroupId == -1 && currentTargetUser == null) return;

        Friend targetFriend = null;
        if (!isMulticastMode && currentGroupId == -1) {
            targetFriend = friends.get(currentTargetUser);
            if (targetFriend == null || !targetFriend.isOnline()) {
                JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
                return;
            }
        }

        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                long msgId = System.currentTimeMillis();
                String msgIdStr = String.valueOf(msgId);

                if (type == 8) {
                    String localName = "img_" + msgId + ".dat";
                    saveMediaToLocal(localName, fileBytes);
                    String thumbB64 = createCompressedImageBase64(fileBytes);
                    String wireImg = "[MID:" + msgIdStr + "][MEDIA_IMG:" + localName + ":" + thumbB64 + "]";

                    if (isMulticastMode) {
                        sendFragmentedMulticastData((byte) 16, fileBytes, msgId);
                        appendImageMsg(msgIdStr, currentUser, currentFullname, fileBytes, true, msgId);
                    } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                        Group grp = groups.get(currentGroupId);
                        sendFragmentedGroupData(grp, (byte) 16, fileBytes, msgId);
                        sendToServer("SAVEGROUPMSG;;;" + currentGroupId + ";;;" + currentUser + ";;;" + wireImg);
                        appendImageMsg(msgIdStr, currentUser, currentFullname, fileBytes, true, msgId);
                    } else if (targetFriend != null) {
                        sendFragmentedData(targetFriend, (byte) 8, fileBytes, msgId);
                        sendToServer("SAVEMSG;;;" + currentUser + ";;;" + targetFriend.username + ";;;" + wireImg);
                        appendImageMsg(msgIdStr, currentUser, currentFullname, fileBytes, true, msgId);
                    }
                } else {
                    String safeName = file.getName().replace(":", "_").replace("|", "_").replace(";", "_");
                    String localName = "file_" + msgId + "_" + safeName;
                    saveMediaToLocal(localName, fileBytes);

                    byte[] nameBytes = safeName.getBytes("UTF-8");
                    ByteBuffer bb = ByteBuffer.allocate(4 + nameBytes.length + fileBytes.length);
                    bb.putInt(nameBytes.length);
                    bb.put(nameBytes);
                    bb.put(fileBytes);
                    byte[] payload = bb.array();
                    String wireFile = "[MID:" + msgIdStr + "][MEDIA_FILE:" + localName + ":" + safeName + "]";

                    if (isMulticastMode) {
                        sendFragmentedMulticastData((byte) 17, payload, msgId);
                        appendFileMsg(msgIdStr, currentUser, currentFullname, payload, true);
                    } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                        Group grp = groups.get(currentGroupId);
                        sendFragmentedGroupData(grp, (byte) 17, payload, msgId);
                        sendToServer("SAVEGROUPMSG;;;" + currentGroupId + ";;;" + currentUser + ";;;" + wireFile);
                        appendFileMsg(msgIdStr, currentUser, currentFullname, payload, true);
                    } else if (targetFriend != null) {
                        sendFragmentedData(targetFriend, (byte) 9, payload, msgId);
                        sendToServer("SAVEMSG;;;" + currentUser + ";;;" + targetFriend.username + ";;;" + wireFile);
                        appendFileMsg(msgIdStr, currentUser, currentFullname, payload, true);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc file");
            }
        }
    }

    private void toggleRecord() {
        if (!isMulticastMode && currentGroupId == -1 && currentTargetUser == null) return;

        Friend targetFriend = null;
        if (!isMulticastMode && currentGroupId == -1) {
            targetFriend = friends.get(currentTargetUser);
            if (targetFriend == null || !targetFriend.isOnline()) {
                JOptionPane.showMessageDialog(this, "Người này đang ngoại tuyến!");
                return;
            }
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
            long audioMsgId = System.currentTimeMillis();
            appendAudioMsg(String.valueOf(audioMsgId), currentUser, currentFullname, audioData, true);

            if (isMulticastMode) {
                sendFragmentedMulticastData((byte) 15, audioData, audioMsgId);
            } else if (currentGroupId != -1 && groups.containsKey(currentGroupId)) {
                sendFragmentedGroupData(groups.get(currentGroupId), (byte) 15, audioData, audioMsgId);
            } else if (targetFriend != null) {
                sendFragmentedData(targetFriend, (byte) 3, audioData, audioMsgId);
            }
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
                            sendCallPacketToActiveTargets(packetData);
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

        // Bật đầy đủ tính năng cho Phòng Chat LAN Chung
        txtChat.setEnabled(true);
        btnSend.setEnabled(true);
        btnIcon.setEnabled(true);
        btnCall.setEnabled(true);
        btnVideoHeader.setEnabled(true);
        btnRecord.setEnabled(true);
        btnImg.setEnabled(true);
        btnFile.setEnabled(true);
        btnGroupMembers.setVisible(false);

        chatPanel.removeAll();
        chatPanel.revalidate();
        chatPanel.repaint();
        resetConversationViewState();
        appendMsg(null, "", "Chào mừng bạn đến với phòng chat LAN chung!", false);

        try {
            mcSocket = new MulticastSocket(9999);
            mcGroup = InetAddress.getByName("224.0.0.1");
            mcSocket.joinGroup(new InetSocketAddress(mcGroup, 9999), null);

            new Thread(() -> {
                try {
                    byte[] buf = new byte[65507];
                    while (isMulticastMode && isRunning) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        mcSocket.receive(p);
                        int len = p.getLength();
                        if (len <= 0) continue;

                        int firstByte = buf[0];
                        if (firstByte == 0 && isCalling && "LAN_ROOM".equals(currentCallUser) && speaker != null) {
                            speaker.write(buf, 1, len - 1);
                            continue;
                        }
                        if (firstByte == 10 && isCalling && "LAN_ROOM".equals(currentCallUser)) {
                            byte[] imgBytes = new byte[len - 1];
                            System.arraycopy(buf, 1, imgBytes, 0, imgBytes.length);
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                                    pnlTargetVideo.setFrame(img);
                                } catch (Exception e) {}
                            });
                            continue;
                        }
                        if (firstByte == 15 || firstByte == 16 || firstByte == 17) {
                            ByteBuffer bb = ByteBuffer.wrap(buf, 1, len - 1);
                            long id = bb.getLong();
                            int total = bb.getInt();
                            int offset = bb.getInt();
                            int userLen = bb.getInt();
                            byte[] uBytes = new byte[userLen];
                            bb.get(uBytes);
                            String sUser = new String(uBytes, "UTF-8");

                            int nameLen = bb.getInt();
                            byte[] nBytes = new byte[nameLen];
                            bb.get(nBytes);
                            String sName = new String(nBytes, "UTF-8");

                            int headerSize = 1 + 8 + 4 + 4 + 4 + userLen + 4 + nameLen;
                            int dataLen = len - headerSize;
                            if (dataLen > 0) {
                                if (!incomingData.containsKey(id)) {
                                    incomingData.put(id, new byte[total]);
                                    incomingDataReceived.put(id, 0);
                                }
                                byte[] fullData = incomingData.get(id);
                                System.arraycopy(buf, headerSize, fullData, offset, dataLen);
                                int received = incomingDataReceived.get(id) + dataLen;
                                incomingDataReceived.put(id, received);

                                if (received >= total) {
                                    byte[] finalData = fullData.clone();
                                    String mediaMsgId = String.valueOf(id);
                                    SwingUtilities.invokeLater(() -> {
                                        if (isMulticastMode && !sUser.equals(currentUser)) {
                                            if (firstByte == 15) appendAudioMsg(mediaMsgId, sUser, sName, finalData, false);
                                            else if (firstByte == 16) appendImageMsg(mediaMsgId, sUser, sName, finalData, false, id);
                                            else if (firstByte == 17) appendFileMsg(mediaMsgId, sUser, sName, finalData, false);
                                        }
                                    });
                                    incomingData.remove(id);
                                    incomingDataReceived.remove(id);
                                }
                            }
                            continue;
                        }

                        String rcv = new String(p.getData(), 0, len, "UTF-8");

                        if (rcv.startsWith("CALL_MC:::")) {
                            String[] cParts = rcv.split(":::", 3);
                            if (cParts.length == 3 && !cParts[1].equals(currentUser)) {
                                SwingUtilities.invokeLater(() -> handleIncomingMulticastCall(cParts[2]));
                            }
                        } else if (rcv.startsWith("RECALL_MC:::")) {
                            String targetId = rcv.substring(12);
                            SwingUtilities.invokeLater(() -> applyRecallLocally(targetId));
                        } else if (rcv.startsWith("PIN_MC:::")) {
                            String[] pParts = rcv.split(":::", 4);
                            if (pParts.length == 4) {
                                SwingUtilities.invokeLater(() -> applyPinLocally(pParts[1], pParts[2], pParts[3]));
                            }
                        } else if (rcv.startsWith("UNPIN_MC:::")) {
                            String targetId = rcv.substring(11);
                            SwingUtilities.invokeLater(() -> applyUnpinLocally(targetId));
                        } else {
                            String[] parts = rcv.split(":::", 3);
                            if (parts.length == 3) {
                                String senderUser = parts[0];
                                String senderName = parts[1];
                                String content = parts[2];
                                boolean isMe = senderUser.equals(currentUser);
                                SwingUtilities.invokeLater(() -> appendMsg(senderUser, senderName, content, isMe));
                            }
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

    private void renderHistoricalFileBubble(String msgId, String senderUsername, String senderName, String fileName, byte[] fileData, boolean isMe) {
        SharedFileItem fileItem = new SharedFileItem(fileName, fileData);
        currentSharedFiles.add(0, fileItem);
        refreshInfoDrawer();

        Color bg = isMe ? ZALO_SENT_BUBBLE : Color.WHITE;
        FlatButton btn = new FlatButton("  " + fileName, bg, ZALO_TEXT_DARK, 8);
        btn.setIcon(new ZaloIcon(ZaloIcon.Type.FILE, 18, ZALO_BLUE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.addActionListener(e -> saveSharedFileToDisk(fileName, fileData));

        JPanel wrapperContent = new JPanel();
        wrapperContent.setLayout(new BoxLayout(wrapperContent, BoxLayout.Y_AXIS));
        wrapperContent.setOpaque(false);
        if ((isMulticastMode || currentGroupId != -1) && !isMe && senderName != null && !senderName.isEmpty()) {
            JLabel lblName = new JLabel(senderName);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblName.setForeground(new Color(110, 118, 130));
            lblName.setBorder(new EmptyBorder(0, 0, 3, 0));
            wrapperContent.add(lblName);
        }
        wrapperContent.add(btn);

        BubblePanel bubble = isMe
                ? new BubblePanel(ZALO_SENT_BUBBLE, ZALO_SENT_BORDER)
                : new BubblePanel(Color.WHITE, new Color(222, 226, 232));
        bubble.add(wrapperContent, BorderLayout.CENTER);
        ChatBubbleEntry entry = addChatBubbleWithTracking(msgId, bubble, isMe, senderUsername, senderName, "[Tệp] " + fileName, fileName, false);
        entry.linkedFile = fileItem;
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