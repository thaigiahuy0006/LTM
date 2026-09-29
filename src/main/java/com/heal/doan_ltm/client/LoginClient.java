package com.heal.doan_ltm.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.*;

public class LoginClient extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // UI Đăng nhập
    private JTextField txtLoginUser;
    private JPasswordField txtLoginPass;

    // UI Đăng ký
    private JTextField txtRegUser, txtRegName, txtRegEmail, txtRegPhone;
    private JPasswordField txtRegPass;
    private JComboBox<String> cbRegGender;

    static {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) {}
    }

    public LoginClient() {
        setTitle("Z-Voice P2P - Đăng Nhập");
        setSize(450, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(232, 234, 237));

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setOpaque(false);

        mainContainer.add(createLoginPanel(), "LOGIN");
        mainContainer.add(createRegisterPanel(), "REGISTER");

        setLayout(new GridBagLayout());
        add(mainContainer);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(40, 35, 40, 35));
        panel.setPreferredSize(new Dimension(380, 500));

        JLabel lblLogo = new JLabel("Z-Voice", SwingConstants.CENTER);
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 36));
        lblLogo.setForeground(new Color(0, 104, 255));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Đăng nhập bằng tài khoản", SwingConstants.CENTER);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblSub.setForeground(Color.GRAY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtLoginUser = new JTextField();
        txtLoginPass = new JPasswordField();

        JButton btnLogin = createPrimaryButton("Đăng nhập");
        btnLogin.addActionListener(e -> login());

        JLabel lblSwitchToReg = new JLabel("Chưa có tài khoản? Đăng ký ngay");
        lblSwitchToReg.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSwitchToReg.setForeground(new Color(0, 104, 255));
        lblSwitchToReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSwitchToReg.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSwitchToReg.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainContainer, "REGISTER"); }
        });

        panel.add(lblLogo);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblSub);
        panel.add(Box.createVerticalStrut(40));
        panel.add(createInputGroup("Số điện thoại / Tài khoản", txtLoginUser));
        panel.add(Box.createVerticalStrut(20));
        // SỬ DỤNG KHUNG CÓ NÚT BẬT TẮT MẬT KHẨU
        panel.add(createPasswordInputGroup("Mật khẩu", txtLoginPass));
        panel.add(Box.createVerticalStrut(40));
        panel.add(btnLogin);
        panel.add(Box.createVerticalStrut(20));
        panel.add(lblSwitchToReg);

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));
        panel.setPreferredSize(new Dimension(380, 600));

        JLabel lblTitle = new JLabel("Đăng Ký", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(new Color(0, 104, 255));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtRegUser = new JTextField(); txtRegPass = new JPasswordField();
        txtRegName = new JTextField(); txtRegPhone = new JTextField();
        txtRegEmail = new JTextField();
        cbRegGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cbRegGender.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cbRegGender.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        formPanel.add(createInputGroup("Tài khoản *", txtRegUser)); formPanel.add(Box.createVerticalStrut(15));
        // SỬ DỤNG KHUNG CÓ NÚT BẬT TẮT MẬT KHẨU
        formPanel.add(createPasswordInputGroup("Mật khẩu *", txtRegPass)); formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(createInputGroup("Tên hiển thị *", txtRegName)); formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(createInputGroup("Số điện thoại *", txtRegPhone)); formPanel.add(Box.createVerticalStrut(15));

        JPanel emailGenderPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        emailGenderPanel.setBackground(Color.WHITE);
        emailGenderPanel.add(createInputGroup("Email", txtRegEmail));
        emailGenderPanel.add(createInputGroup("Giới tính", cbRegGender));
        emailGenderPanel.setMaximumSize(new Dimension(400, 65));
        formPanel.add(emailGenderPanel);

        JButton btnRegister = createPrimaryButton("Tạo tài khoản");
        btnRegister.addActionListener(e -> register());

        JLabel lblSwitchToLogin = new JLabel("Đã có tài khoản? Đăng nhập");
        lblSwitchToLogin.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSwitchToLogin.setForeground(new Color(0, 104, 255));
        lblSwitchToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSwitchToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSwitchToLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainContainer, "LOGIN"); }
        });

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(25));
        panel.add(formPanel);
        panel.add(Box.createVerticalStrut(30));
        panel.add(btnRegister);
        panel.add(Box.createVerticalStrut(20));
        panel.add(lblSwitchToLogin);

        return panel;
    }

    // TẠO Ô NHẬP LIỆU THƯỜNG
    private JPanel createInputGroup(String labelText, JComponent inputField) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
        group.setBackground(Color.WHITE);
        group.setMaximumSize(new Dimension(400, 65));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(50, 50, 50));
        group.add(label, BorderLayout.NORTH);

        inputField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        inputField.setPreferredSize(new Dimension(300, 40));
        if (inputField instanceof JTextField || inputField instanceof JPasswordField) {
            ((JComponent) inputField).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
        }
        group.add(inputField, BorderLayout.CENTER);

        return group;
    }

    // TẠO Ô NHẬP PASSWORD CÓ NÚT BẬT/TẮT
    private JPanel createPasswordInputGroup(String labelText, JPasswordField passField) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
        group.setBackground(Color.WHITE);
        group.setMaximumSize(new Dimension(400, 65));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(50, 50, 50));
        group.add(label, BorderLayout.NORTH);

        JPanel passWrapper = new JPanel(new BorderLayout());
        passWrapper.setBackground(Color.WHITE);
        passWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        passWrapper.setPreferredSize(new Dimension(300, 40));

        passField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        passField.setBorder(null); // Xóa viền gốc vì đã có viền ngoài
        passField.setEchoChar('•');

        // Nút con mắt
        JButton btnToggle = new JButton("👁");
        btnToggle.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnToggle.setBackground(Color.WHITE);
        btnToggle.setBorder(null);
        btnToggle.setFocusPainted(false);
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggle.setPreferredSize(new Dimension(35, 40));

        btnToggle.addActionListener(e -> {
            if (passField.getEchoChar() == '•') {
                passField.setEchoChar((char) 0); // Hiện pass
                btnToggle.setText("🙈");
            } else {
                passField.setEchoChar('•'); // Che pass
                btnToggle.setText("👁");
            }
        });

        passWrapper.add(passField, BorderLayout.CENTER);
        passWrapper.add(btnToggle, BorderLayout.EAST);

        group.add(passWrapper, BorderLayout.CENTER);
        return group;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(new Color(0, 104, 255));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(400, 45));
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
            socket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName("172.26.52.71"), 8080));

            byte[] receiveData = new byte[65507];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3000);
            socket.receive(receivePacket);

            String[] parts = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8").split(";;;", -1);
            if ("SUCCESS".equals(parts[0])) {
                String fn = parts.length > 1 ? parts[1] : "";
                String av = parts.length > 2 ? parts[2] : "";

                // MỞ MAINCLIENT SAU KHI ĐĂNG NHẬP
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
            socket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName("172.26.52.71"), 8080));

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3000);
            socket.receive(receivePacket);

            if ("SUCCESS".equals(new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8"))) {
                JOptionPane.showMessageDialog(this, "Tạo tài khoản thành công! Vui lòng đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                txtLoginUser.setText(user);
                cardLayout.show(mainContainer, "LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại. Nếu không phải, hãy kiểm tra lỗi màu đỏ bên màn hình UDPServer!", "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối máy chủ", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new LoginClient().setVisible(true)); }
}