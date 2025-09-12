package com.vadim.telegrambot.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.repository.SubscriptionRepository;
import com.vadim.telegrambot.service.ReminderBot;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final ReminderBot bot;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        for (Subscription tpl : subscriptionRepository.findAll()) {

            LocalDate due = today.withDayOfMonth(tpl.getDayOfMonth());
            if (today.isAfter(due)) {
                due = due.plusMonths(1);
            }

            String textTomorrow = "Завтра оплата «" + tpl.getName() + "» — " + tpl.getPrice() + " ₽";
            String textToday = "Сегодня оплата «" + tpl.getName() + "» — " + tpl.getPrice() + " ₽";

            if (today.equals(due.minusDays(1))) {
                broadcast(tpl, textTomorrow);
            } else if (today.equals(due)) {
                broadcast(tpl, textToday);
            }
        }
    }

    private void broadcast(Subscription tpl, String text) {
        tpl.getUsers().forEach(user -> bot.sendText(user.getTelegramId(), text));
    }
}
