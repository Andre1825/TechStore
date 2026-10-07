package com.techstore.tech_store_project.config;

import com.techstore.tech_store_project.notification.StockNotificationGateway;
import com.techstore.tech_store_project.notification.SnsStockNotificationGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import java.time.Duration;

@Configuration
public class AwsNotificationsConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "techstore.aws.sns.enabled", havingValue = "true")
    public SnsClient snsClient(@Value("${techstore.aws.region}") String region,
                               @Value("${techstore.aws.sns.topic-arn}") String topicArn) {
        if (!topicArn.matches("arn:aws:sns:" + java.util.regex.Pattern.quote(region) + ":[0-9]{12}:[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("SNS_TOPIC_ARN debe identificar un tema Standard de la región AWS_REGION.");
        }
        // La cadena predeterminada obtiene credenciales temporales del rol de EC2.
        return SnsClient.builder().region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(3))
                        .socketTimeout(Duration.ofSeconds(5)))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(6))).build();
    }

    @Bean
    @ConditionalOnProperty(name = "techstore.aws.sns.enabled", havingValue = "true")
    public StockNotificationGateway snsGateway(SnsClient client,
                                               @Value("${techstore.aws.sns.topic-arn}") String topicArn) {
        return new SnsStockNotificationGateway(client, topicArn);
    }

    @Bean
    @ConditionalOnProperty(name = "techstore.aws.sns.enabled", havingValue = "false", matchIfMissing = true)
    public StockNotificationGateway disabledSnsGateway() {
        return new StockNotificationGateway() {
            public boolean enabled() { return false; }
            public void subscribe(String email, String key) { throw new IllegalStateException("SNS desactivado."); }
            public void publish(String message, String key) { throw new IllegalStateException("SNS desactivado."); }
        };
    }

    @Bean(name = "stockAlertExecutor")
    public ThreadPoolTaskExecutor stockAlertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("stock-alert-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
