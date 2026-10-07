package com.techstore.tech_store_project.notification;

public interface StockNotificationGateway {
    boolean enabled();
    void subscribe(String email, String recipientKey);
    void publish(String message, String recipientKey);
}
