package com.heal.doan_ltm.models;

public class Friend {
    public String username, name, ip;
    public int port;

    public Friend(String u, String n, String i, int p) {
        this.username = u;
        this.name = n;
        this.ip = i;
        this.port = p;
    }

    // Kiểm tra trạng thái Online dựa vào cổng kết nối
    public boolean isOnline() {
        return port > 0 && ip != null && !ip.isEmpty() && !ip.equals("null");
    }
}