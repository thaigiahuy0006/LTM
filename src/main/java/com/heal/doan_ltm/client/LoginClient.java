package com.heal.doan_ltm.client;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.heal.doan_ltm.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.*;

public class LoginClient extends JFrame {
    public static final String SERVER_IP = "192.168.1.4";

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JTextField txtLoginUser;
    private JPasswordField txtLoginPass;

    private JTextField txtRegUser, txtRegName, txtRegEmail, txtRegPhone;
    private JPasswordField txtRegPass;
    private JComboBox<String> cbRegGender;

    static {
        FlatLightLaf.setup();
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
    }

    public LoginClient() {
        setTitle("Z-Voice - Đăng nhập tài khoản");
        setSize(460, 670);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Màu nền xanh nhạt đặc trưng của màn hình đăng nhập Zalo PC
        getContentPane().setBackground(new Color(229, 239, 255));

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setOpaque(false);

        mainContainer.add(createLoginPanel(), "LOGIN");
        mainContainer.add(createRegisterPanel(), "REGISTER");

        setLayout(new GridBagLayout());
        add(mainContainer);
    }

    private JPanel createLoginPanel() {
        RoundedPanel panel = new RoundedPanel(18, Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(40, 38, 40, 38));
        panel.setPreferredSize(new Dimension(390, 510));

        JLabel lblLogo = new JLabel("Z-Voice", SwingConstants.CENTER);
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 38));
        lblLogo.setForeground(new Color(0, 104, 255));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Đăng nhập tài khoản Z-Voice để kết nối", SwingConstants.CENTER);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSub.setForeground(new Color(110, 118, 130));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtLoginUser = new JTextField();
        txtLoginUser.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Số điện thoại hoặc tên tài khoản");
        txtLoginPass = new JPasswordField();
        txtLoginPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mật khẩu");
        txtLoginPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        JButton btnLogin = createPrimaryButton("Đăng nhập với mật khẩu");
        btnLogin.addActionListener(e -> login());
        txtLoginPass.addActionListener(e -> login());

        JLabel lblSwitchToReg = new JLabel("Chưa có tài khoản? Đăng ký ngay");
        lblSwitchToReg.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSwitchToReg.setForeground(new Color(0, 104, 255));
        lblSwitchToReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSwitchToReg.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSwitchToReg.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainContainer, "REGISTER"); }
        });

        panel.add(lblLogo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblSub);
        panel.add(Box.createVerticalStrut(38));
        panel.add(createInputGroup("Tài khoản", txtLoginUser));
        panel.add(Box.createVerticalStrut(18));
        panel.add(createInputGroup("Mật khẩu", txtLoginPass));
        panel.add(Box.createVerticalStrut(35));
        panel.add(btnLogin);
        panel.add(Box.createVerticalStrut(22));
        panel.add(lblSwitchToReg);

        return panel;
    }

    private JPanel createRegisterPanel() {
        RoundedPanel panel = new RoundedPanel(18, Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(28, 36, 28, 36));
        panel.setPreferredSize(new Dimension(390, 600));

        JLabel lblTitle = new JLabel("Đăng ký tài khoản mới", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(0, 104, 255));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtRegUser = new JTextField();
        txtRegPass = new JPasswordField();
        txtRegPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        txtRegName = new JTextField();
        txtRegPhone = new JTextField();
        txtRegEmail = new JTextField();
        cbRegGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cbRegGender.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        formPanel.add(createInputGroup("Tên đăng nhập *", txtRegUser)); formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInputGroup("Mật khẩu *", txtRegPass)); formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInputGroup("Tên hiển thị Zalo *", txtRegName)); formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInputGroup("Số điện thoại *", txtRegPhone)); formPanel.add(Box.createVerticalStrut(12));

        JPanel emailGenderPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        emailGenderPanel.setOpaque(false);
        emailGenderPanel.add(createInputGroup("Email", txtRegEmail));
        emailGenderPanel.add(createInputGroup("Giới tính", cbRegGender));
        emailGenderPanel.setMaximumSize(new Dimension(400, 65));
        formPanel.add(emailGenderPanel);

        JButton btnRegister = createPrimaryButton("Tạo tài khoản");
        btnRegister.addActionListener(e -> register());

        JLabel lblSwitchToLogin = new JLabel("Đã có tài khoản? Quay lại đăng nhập");
        lblSwitchToLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSwitchToLogin.setForeground(new Color(0, 104, 255));
        lblSwitchToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSwitchToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSwitchToLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainContainer, "LOGIN"); }
        });

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(formPanel);
        panel.add(Box.createVerticalStrut(24));
        panel.add(btnRegister);
        panel.add(Box.createVerticalStrut(16));
        panel.add(lblSwitchToLogin);

        return panel;
    }

    private JPanel createInputGroup(String labelText, JComponent inputField) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        group.setMaximumSize(new Dimension(400, 66));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(40, 48, 60));
        group.add(label, BorderLayout.NORTH);

        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setPreferredSize(new Dimension(300, 40));
        group.add(inputField, BorderLayout.CENTER);
        return group;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setBackground(new Color(0, 104, 255));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(400, 44));
        return btn;
    }

    private void login() {
        if (txtLoginUser.getText().trim().isEmpty() || new String(txtLoginPass.getPassword()).trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            String request = "LOGIN;;;" + txtLoginUser.getText().trim() + ";;;" + new String(txtLoginPass.getPassword());
            byte[] sendData = request.getBytes("UTF-8");
            socket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_IP), 8080));

            byte[] receiveData = new byte[65507];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3000);
            socket.receive(receivePacket);

            String[] parts = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8").split(";;;", -1);
            if ("SUCCESS".equals(parts[0])) {
                String fn = parts.length > 1 ? parts[1] : "";
                String av = parts.length > 2 ? parts[2] : "";
                new MainClient(txtLoginUser.getText().trim(), fn, av).setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu", "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SocketTimeoutException ste) {
            JOptionPane.showMessageDialog(this, "Server chưa bật! Hãy chạy UDPServer.java trước.", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối máy chủ", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void register() {
        String user = txtRegUser.getText().trim();
        String pass = new String(txtRegPass.getPassword());
        String name = txtRegName.getText().trim();
        String phone = txtRegPhone.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ các trường có dấu (*)", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            String req = "REGISTER;;;" + user + ";;;" + pass + ";;;" + name + ";;;" + txtRegEmail.getText().trim() + ";;;" + phone + ";;;" + cbRegGender.getSelectedItem();
            byte[] sendData = req.getBytes("UTF-8");
            socket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_IP), 8080));

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3000);
            socket.receive(receivePacket);

            if ("SUCCESS".equals(new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8"))) {
                JOptionPane.showMessageDialog(this, "Tạo tài khoản thành công! Vui lòng đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                txtLoginUser.setText(user);
                cardLayout.show(mainContainer, "LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại!", "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối máy chủ", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginClient().setVisible(true));
    }
}