package com.viral.gateway.config;

import com.viral.gateway.entity.Bot;
import com.viral.gateway.entity.User;
import com.viral.gateway.repository.BotRepository;
import com.viral.gateway.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedReferenceData(UserRepository userRepository, BotRepository botRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.saveAll(List.of(
                        new User("alice", true),
                        new User("bob", false),
                        new User("carol", true)
                ));
                log.info("Seeded sample users with generated ids");
            }

            if (botRepository.count() == 0) {
                botRepository.saveAll(List.of(
                        new Bot("newsbot", "News bot that comments on trending topics"),
                        new Bot("helperbot", "Helpful bot that answers user questions"),
                        new Bot("financebot", "Finance bot focused on money and markets")
                ));
                log.info("Seeded sample bots with generated ids");
            }
        };
    }
}
