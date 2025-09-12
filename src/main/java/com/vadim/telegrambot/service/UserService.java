package com.vadim.telegrambot.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.model.User;
import com.vadim.telegrambot.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(Long telegramId,
            String username,
            String firstName,
            String lastName) {
        Optional<User> opt = userRepository.findByTelegramId(telegramId);
        if (opt.isPresent()) {
            return opt.get();
        }
        User user = User.builder()
                .telegramId(telegramId)
                .username("@" + username)
                .firstName(firstName)
                .lastName(lastName)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public void updateUser(Update update) {
        Long telegramId;
        String username;
        String firstName;
        String lastName;

        if (update.hasMessage()) {
            telegramId = update.getMessage().getFrom().getId();
            username = update.getMessage().getFrom().getUserName();
            firstName = update.getMessage().getFrom().getFirstName();
            lastName = update.getMessage().getFrom().getLastName();
        } else if (update.hasCallbackQuery()) {
            telegramId = update.getCallbackQuery().getFrom().getId();
            username = update.getCallbackQuery().getFrom().getUserName();
            firstName = update.getCallbackQuery().getFrom().getFirstName();
            lastName = update.getCallbackQuery().getFrom().getLastName();
        } else {
            return;
        }

        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setUsername(username != null ? "@" + username : null);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            userRepository.save(user);
        });
    }

    @Transactional
    public Set<Subscription> getSubscriptions(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + telegramId));
        return user.getSubscriptions();
    }
}
