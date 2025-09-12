package com.vadim.telegrambot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.model.User;
import com.vadim.telegrambot.repository.SubscriptionRepository;
import com.vadim.telegrambot.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional
    public List<Subscription> listAllTemplates() {
        return subscriptionRepository.findAll();
    }

    @Transactional
    public boolean subscribe(User user, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new IllegalArgumentException("Шаблон не найден: " + subscriptionId));
        boolean added = user.getSubscriptions().add(subscription);
        if (added) {
            userRepository.save(user);
            paymentService.createPayment(user, subscription);
        }
        return added;
    }

    @Transactional
    public boolean unsubscribe(User user, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new IllegalArgumentException("Шаблон не найден: " + subscriptionId));
        boolean removed = user.getSubscriptions().remove(subscription);
        if (removed) {
            userRepository.save(user);
            paymentService.deletePayment(user, subscription);
        }
        return removed;
    }
}
