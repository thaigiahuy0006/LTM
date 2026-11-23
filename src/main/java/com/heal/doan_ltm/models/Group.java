package com.heal.doan_ltm.models;

import java.util.ArrayList;
import java.util.List;

public class Group {
    public int id;
    public String name;
    public List<Friend> members = new ArrayList<>();

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }
}