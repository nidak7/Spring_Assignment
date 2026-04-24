package com.viral.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<List> botCommentWithCooldownScript() {
        return buildScript(
                """
                local newCount = redis.call('INCR', KEYS[1])
                if newCount > tonumber(ARGV[1]) then
                  redis.call('DECR', KEYS[1])
                  return {0, newCount - 1, tonumber(redis.call('GET', KEYS[2]) or '0')}
                end

                local cooldownSet = redis.call('SET', KEYS[3], '1', 'NX', 'EX', tonumber(ARGV[3]))
                if not cooldownSet then
                  redis.call('DECR', KEYS[1])
                  return {-1, newCount - 1, tonumber(redis.call('GET', KEYS[2]) or '0')}
                end

                local newScore = redis.call('INCRBY', KEYS[2], tonumber(ARGV[2]))
                return {1, newCount, newScore}
                """,
                List.class
        );
    }

    @Bean
    public RedisScript<List> botCommentWithoutCooldownScript() {
        return buildScript(
                """
                local newCount = redis.call('INCR', KEYS[1])
                if newCount > tonumber(ARGV[1]) then
                  redis.call('DECR', KEYS[1])
                  return {0, newCount - 1, tonumber(redis.call('GET', KEYS[2]) or '0')}
                end

                local newScore = redis.call('INCRBY', KEYS[2], tonumber(ARGV[2]))
                return {1, newCount, newScore}
                """,
                List.class
        );
    }

    @Bean
    public RedisScript<Long> botCommentRollbackScript() {
        return buildScript(
                """
                local currentCount = tonumber(redis.call('GET', KEYS[1]) or '0')
                if currentCount > 0 then
                  redis.call('DECR', KEYS[1])
                end

                local currentScore = tonumber(redis.call('GET', KEYS[2]) or '0')
                local decrement = tonumber(ARGV[1])
                if currentScore <= decrement then
                  redis.call('SET', KEYS[2], '0')
                else
                  redis.call('INCRBY', KEYS[2], -decrement)
                end

                if tonumber(ARGV[2]) == 1 then
                  redis.call('DEL', KEYS[3])
                end

                return 1
                """,
                Long.class
        );
    }

    @Bean
    public RedisScript<Long> viralityRollbackScript() {
        return buildScript(
                """
                local currentScore = tonumber(redis.call('GET', KEYS[1]) or '0')
                local decrement = tonumber(ARGV[1])
                if currentScore <= decrement then
                  redis.call('SET', KEYS[1], '0')
                else
                  redis.call('INCRBY', KEYS[1], -decrement)
                end
                return 1
                """,
                Long.class
        );
    }

    @Bean
    public RedisScript<List> notificationDecisionScript() {
        return buildScript(
                """
                local cooldownSet = redis.call('SET', KEYS[1], '1', 'NX', 'EX', tonumber(ARGV[2]))
                if cooldownSet then
                  return {1, 0}
                end

                local size = redis.call('RPUSH', KEYS[2], ARGV[1])
                return {0, size}
                """,
                List.class
        );
    }

    private <T> RedisScript<T> buildScript(String scriptText, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(resultType);
        return script;
    }
}
