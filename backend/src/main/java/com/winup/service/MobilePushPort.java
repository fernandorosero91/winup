package com.winup.service;

public interface MobilePushPort {
    void push(String userId, String title, String body);
}
