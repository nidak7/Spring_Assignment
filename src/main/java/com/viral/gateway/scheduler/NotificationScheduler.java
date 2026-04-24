package com.viral.gateway.scheduler;

import com.viral.gateway.service.NotificationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 300000)
    public void flushPendingNotifications() {
        List<String> pendingKeys = notificationService.findPendingNotificationKeys();
        for (String pendingKey : pendingKeys) {
            try {
                notificationService.flushPendingNotifications(pendingKey);
            } catch (RuntimeException exception) {
                log.error("Failed to flush pending notifications for key {}", pendingKey, exception);
            }
        }
    }
}
