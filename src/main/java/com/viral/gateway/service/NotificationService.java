package com.viral.gateway.service;

import com.viral.gateway.entity.Post;
import com.viral.gateway.repository.BotRepository;
import com.viral.gateway.util.NotificationMessageUtil;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Duration NOTIFICATION_COOLDOWN = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;
    private final BotRepository botRepository;
    private final RedisScript<List> notificationDecisionScript;

    public NotificationService(
            StringRedisTemplate stringRedisTemplate,
            BotRepository botRepository,
            @Qualifier("notificationDecisionScript") RedisScript<List> notificationDecisionScript
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.botRepository = botRepository;
        this.notificationDecisionScript = notificationDecisionScript;
    }

    public void handlePostInteractionNotification(Post post, Long actorId, boolean actorBot) {
        if (!actorBot || Boolean.TRUE.equals(post.getIsAuthorBot())) {
            return;
        }

        Long targetUserId = post.getAuthorId();
        String actorName = botRepository.findById(actorId)
                .map(bot -> bot.getName())
                .orElse("unknown");

        String payload = NotificationMessageUtil.buildActorPayload(actorName, true);
        List<?> result = stringRedisTemplate.execute(
                notificationDecisionScript,
                List.of(notificationCooldownKey(targetUserId), pendingNotificationKey(targetUserId)),
                payload,
                String.valueOf(NOTIFICATION_COOLDOWN.toSeconds())
        );

        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Notification decision script failed");
        }

        long status = toLong(result.get(0));
        if (status == 1L) {
            log.info("Push Notification Sent to User");
        }
    }

    public List<String> findPendingNotificationKeys() {
        List<String> keys = stringRedisTemplate.execute((RedisCallback<List<String>>) connection -> {
            Set<String> collectedKeys = new LinkedHashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match("user:*:pending_notifs")
                    .count(500)
                    .build();

            try (var cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    collectedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }

            return new ArrayList<>(collectedKeys);
        });

        return keys == null ? Collections.emptyList() : keys;
    }

    public void flushPendingNotifications(String pendingKey) {
        Long userId = NotificationMessageUtil.extractUserId(pendingKey);
        List<String> payloads = stringRedisTemplate.opsForList().range(pendingKey, 0, -1);

        if (payloads == null || payloads.isEmpty()) {
            stringRedisTemplate.delete(pendingKey);
            return;
        }

        String summary = NotificationMessageUtil.buildSummary(payloads);
        log.info(summary);
        log.debug("Cleared pending notifications for user {}", userId);
        stringRedisTemplate.delete(pendingKey);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String notificationCooldownKey(Long userId) {
        return "user:" + userId + ":notif_cooldown";
    }

    private String pendingNotificationKey(Long userId) {
        return "user:" + userId + ":pending_notifs";
    }
}
