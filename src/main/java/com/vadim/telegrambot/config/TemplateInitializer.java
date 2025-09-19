package com.vadim.telegrambot.config;

import org.springframework.stereotype.Component;

import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.repository.SubscriptionRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateInitializer {

    private final SubscriptionRepository repo;

    @PostConstruct
    public void init() {
        if (repo.count() == 0) {
            repo.save(Subscription.builder()
                    .name("VPN")
                    .price(100)
                    .dayOfMonth(19)
                    .build());
            repo.save(Subscription.builder()
                    .name("Apple")
                    .price(172)
                    .dayOfMonth(7)
                    .build());
        }
    }
}
