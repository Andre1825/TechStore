package com.techstore.tech_store_project.notification;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import java.util.Map;

public class SnsStockNotificationGateway implements StockNotificationGateway {
    private final SnsClient client;
    private final String topicArn;

    public SnsStockNotificationGateway(SnsClient client, String topicArn) {
        this.client = client;
        this.topicArn = topicArn;
    }

    @Override public boolean enabled() { return true; }

    @Override public void subscribe(String email, String recipientKey) {
        client.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("email").endpoint(email)
                .attributes(Map.of("FilterPolicy", "{\"recipient\":[\"" + recipientKey + "\"]}",
                        "FilterPolicyScope", "MessageAttributes"))
                .build());
    }

    @Override public void publish(String message, String recipientKey) {
        client.publish(PublishRequest.builder().topicArn(topicArn).subject("TechStore: aviso de stock bajo")
                .message(message).messageAttributes(Map.of("recipient", MessageAttributeValue.builder()
                        .dataType("String").stringValue(recipientKey).build())).build());
    }
}
