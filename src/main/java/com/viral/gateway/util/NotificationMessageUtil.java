package com.viral.gateway.util;

import java.util.List;

public final class NotificationMessageUtil {

    private static final String BOT_PREFIX = "BOT";

    private NotificationMessageUtil() {
    }

    public static String buildActorPayload(String actorName, boolean actorBot) {
        String safeActorName = (actorName == null || actorName.isBlank()) ? "unknown" : actorName;
        return (actorBot ? BOT_PREFIX + "|" : "") + safeActorName;
    }

    public static String buildSummary(List<String> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return "Summarized Push Notification: No pending interactions.";
        }

        String actorName = extractActorName(payloads.get(0));
        int others = payloads.size() - 1;

        if (others <= 0) {
            return "Summarized Push Notification: Bot " + actorName + " interacted with your posts.";
        }

        return "Summarized Push Notification: Bot " + actorName + " and " + others
                + " others interacted with your posts.";
    }

    public static Long extractUserId(String pendingKey) {
        String[] parts = pendingKey.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid pending notification key: " + pendingKey);
        }
        return Long.parseLong(parts[1]);
    }

    private static String extractActorName(String payload) {
        String safePayload = payload == null ? "" : payload.trim();
        if (safePayload.isEmpty()) {
            return "unknown";
        }

        String[] parts = safePayload.split("\\|", 2);
        if (parts.length == 2 && !parts[1].isBlank()) {
            return parts[1].trim();
        }

        return safePayload;
    }
}
