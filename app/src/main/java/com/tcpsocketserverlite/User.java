package com.tcpsocketserverlite;

public class User {
    String username;
    String ip;
    long lastActionTime;

    public User(String username, String ip, long lastActionTime) {
        this.username = username;
        this.ip = ip;
        this.lastActionTime = lastActionTime;
    }
}
