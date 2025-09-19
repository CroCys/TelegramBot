package com.vadim.telegrambot.scheduler;

import java.time.LocalDate;
import java.util.List;

import com.vadim.telegrambot.model.Payment;
import com.vadim.telegrambot.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.vadim.telegrambot.service.ReminderBot;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final PaymentService paymentService;
    private final ReminderBot bot;

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Moscow")
    public void sendReminders() {
        LocalDate today = LocalDate.now();

        List<Payment> payments = paymentService.findAllDebtors();

        for (Payment payment : payments) {
            LocalDate due = LocalDate.of(payment.getYear(), payment.getMonth(), payment.getSubscription().getDayOfMonth());

            if (today.equals(due.minusDays(1))) {
                send(payment, "Привет " + payment.getUser().getFirstName() + "! Напоминаю: завтра оплата «%s» — %s ₽");
            } else if (today.equals(due)) {
                send(payment, "Привет " + payment.getUser().getFirstName() + "! Напоминаю: сегодня оплата «%s» — %s ₽");
            } else if (today.isAfter(due)) {
                send(payment, "Привет " + payment.getUser().getFirstName() + "! Напоминаю: просрочен платеж «%s» — %s ₽. Оплати пожалуйста \uD83E\uDD72");
            }
        }
    }

    private void send(Payment payment, String template) {
        String text = String.format(template, payment.getSubscription().getName(), payment.getSubscription().getPrice());
        bot.sendText(payment.getUser().getTelegramId(), text);
    }

    @Scheduled(cron = "0 5 0 1 * *")
    void scheduleMonthlyPayments() {
        paymentService.generatePaymentsForCurrentMonth();
    }
}
