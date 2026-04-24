package com.viral.gateway.service;

import com.viral.gateway.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(RedisGuardrailService.class);
    private static final long BOT_COMMENT_VIRALITY_INCREMENT = 1L;
    private static final long HUMAN_COMMENT_VIRALITY_INCREMENT = 50L;
    private static final long HUMAN_LIKE_VIRALITY_INCREMENT = 20L;
    private static final long MAX_BOT_COMMENTS = 100L;
    private static final Duration BOT_TO_HUMAN_COOLDOWN = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List> botCommentWithCooldownScript;
    private final RedisScript<List> botCommentWithoutCooldownScript;
    private final RedisScript<Long> botCommentRollbackScript;
    private final RedisScript<Long> viralityRollbackScript;

    public RedisGuardrailService(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("botCommentWithCooldownScript") RedisScript<List> botCommentWithCooldownScript,
            @Qualifier("botCommentWithoutCooldownScript") RedisScript<List> botCommentWithoutCooldownScript,
            @Qualifier("botCommentRollbackScript") RedisScript<Long> botCommentRollbackScript,
            @Qualifier("viralityRollbackScript") RedisScript<Long> viralityRollbackScript
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.botCommentWithCooldownScript = botCommentWithCooldownScript;
        this.botCommentWithoutCooldownScript = botCommentWithoutCooldownScript;
        this.botCommentRollbackScript = botCommentRollbackScript;
        this.viralityRollbackScript = viralityRollbackScript;
    }

    public BotCommentReservation reserveBotComment(Long postId, Long botId, Long targetHumanUserId) {
        List<?> result;
        if (targetHumanUserId != null) {
            result = stringRedisTemplate.execute(
                    botCommentWithCooldownScript,
                    List.of(botCountKey(postId), viralityScoreKey(postId), cooldownKey(botId, targetHumanUserId)),
                    String.valueOf(MAX_BOT_COMMENTS),
                    String.valueOf(BOT_COMMENT_VIRALITY_INCREMENT),
                    String.valueOf(BOT_TO_HUMAN_COOLDOWN.toSeconds())
            );
        } else {
            result = stringRedisTemplate.execute(
                    botCommentWithoutCooldownScript,
                    List.of(botCountKey(postId), viralityScoreKey(postId)),
                    String.valueOf(MAX_BOT_COMMENTS),
                    String.valueOf(BOT_COMMENT_VIRALITY_INCREMENT)
            );
        }

        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Bot guardrail script failed");
        }

        long status = toLong(result.get(0));
        long botCount = toLong(result.get(1));
        long viralityScore = toLong(result.get(2));

        if (status == 0L) {
            throw new RateLimitExceededException("Maximum bot comment cap of 100 reached for this post.");
        }
        if (status == -1L) {
            throw new RateLimitExceededException("Bot cooldown is active for this user.");
        }

        return new BotCommentReservation(botCount, viralityScore);
    }

    public long reserveHumanComment(Long postId) {
        return incrementVirality(postId, HUMAN_COMMENT_VIRALITY_INCREMENT);
    }

    public long reserveHumanLike(Long postId) {
        return incrementVirality(postId, HUMAN_LIKE_VIRALITY_INCREMENT);
    }

    public void rollbackBotComment(Long postId, Long botId, Long targetHumanUserId) {
        stringRedisTemplate.execute(
                botCommentRollbackScript,
                List.of(botCountKey(postId), viralityScoreKey(postId), targetHumanUserId != null ? cooldownKey(botId, targetHumanUserId) : "guardrail:noop"),
                String.valueOf(BOT_COMMENT_VIRALITY_INCREMENT),
                String.valueOf(targetHumanUserId != null ? 1 : 0)
        );
    }

    public void rollbackHumanComment(Long postId) {
        rollbackVirality(postId, HUMAN_COMMENT_VIRALITY_INCREMENT);
    }

    public void rollbackHumanLike(Long postId) {
        rollbackVirality(postId, HUMAN_LIKE_VIRALITY_INCREMENT);
    }

    public long getViralityScore(Long postId) {
        Object value = stringRedisTemplate.opsForValue().get(viralityScoreKey(postId));
        return readLongValue(value, viralityScoreKey(postId));
    }

    public long getBotCount(Long postId) {
        Object value = stringRedisTemplate.opsForValue().get(botCountKey(postId));
        return readLongValue(value, botCountKey(postId));
    }

    private long incrementVirality(Long postId, long delta) {
        Long updatedValue = stringRedisTemplate.opsForValue().increment(viralityScoreKey(postId), delta);
        if (updatedValue == null) {
            throw new IllegalStateException("Virality increment failed");
        }
        return updatedValue;
    }

    private void rollbackVirality(Long postId, long delta) {
        stringRedisTemplate.execute(
                viralityRollbackScript,
                List.of(viralityScoreKey(postId)),
                String.valueOf(delta)
        );
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private long readLongValue(Object value, String key) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof CharSequence sequence) {
            String text = sequence.toString().trim();
            if (text.isEmpty()) {
                return 0L;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException exception) {
                log.warn("Unable to parse Redis numeric value for key {}", key, exception);
                return 0L;
            }
        }
        log.warn("Unsupported Redis numeric type {} for key {}", value.getClass().getName(), key);
        return 0L;
    }

    private String viralityScoreKey(Long postId) {
        return "post:" + postId + ":virality_score";
    }

    private String botCountKey(Long postId) {
        return "post:" + postId + ":bot_count";
    }

    private String cooldownKey(Long botId, Long humanUserId) {
        return "cooldown:bot_" + botId + ":human_" + humanUserId;
    }

    public record BotCommentReservation(long botCount, long viralityScore) {
    }
}
