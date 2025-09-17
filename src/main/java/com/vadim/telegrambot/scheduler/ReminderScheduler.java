package com.vadim.telegrambot.scheduler;

import java.time.LocalDate;

import com.vadim.telegrambot.service.PaymentService;
import com.vadim.telegrambot.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.service.ReminderBot;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final ReminderBot bot;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        for (Subscription subscription : subscriptionService.listAllSubscriptions()) {

            LocalDate due = today.withDayOfMonth(subscription.getDayOfMonth());
            if (today.isAfter(due)) {
                due = due.plusMonths(1);
            }

            String textTomorrow = "Завтра оплата «" + subscription.getName() + "» — " + subscription.getPrice() + " ₽";
            String textToday = "Сегодня оплата «" + subscription.getName() + "» — " + subscription.getPrice() + " ₽";

            if (today.equals(due.minusDays(1))) {
                broadcast(subscription, textTomorrow);
            } else if (today.equals(due)) {
                broadcast(subscription, textToday);
            }
        }
    }

    @Scheduled(cron = "0 5 0 1 * *")
    public void scheduleMonthlyPayments() {
        paymentService.generatePaymentsForCurrentMonth();
    }

    private void broadcast(Subscription subscription, String text) {
        subscription.getUsers().forEach(user -> bot.sendText(user.getTelegramId(), text));
    }
}
