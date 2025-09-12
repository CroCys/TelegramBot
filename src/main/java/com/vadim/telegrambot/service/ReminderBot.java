package com.vadim.telegrambot.service;

import java.util.List;
import java.util.Set;

import com.vadim.telegrambot.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.firstAdminId}")
    private int firstAdminId;
    @Value("${telegram.bot.secondAdminId}")
    private int secondAdminId;

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    private static final String BTN_SUBSCRIBE = "🛒 Подписаться";
    private static final String BTN_MY_SUBS = "📄 Мои подписки";
    private static final String BTN_DEBTORS = "📌 Должники";

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Получен апдейт: {}", update);
        userService.updateUser(update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleText(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleText(Message msg) {
        long chatId = msg.getChatId();
        switch (msg.getText()) {
            case "/start" -> sendMainMenu(chatId);
            case BTN_SUBSCRIBE -> sendTemplateList(chatId);
            case BTN_MY_SUBS -> showUserSubscriptions(chatId);
            case BTN_DEBTORS -> sendDebtors(chatId);
            default -> sendMainMenu(chatId);
        }
    }

    private void sendMainMenu(long chatId) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(BTN_SUBSCRIBE));
        row.add(new KeyboardButton(BTN_MY_SUBS));

        if (chatId == firstAdminId || chatId == secondAdminId) {
            row.add(new KeyboardButton(BTN_DEBTORS));
        }

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setKeyboard(List.of(row));
        kb.setResizeKeyboard(true);

        SendMessage msg = SendMessage.builder().chatId(String.valueOf(chatId)).text("Добро пожаловать!\nВыберите действие:").replyMarkup(kb).build();
        executeSafe(msg);
    }

    private void sendTemplateList(long chatId) {
        List<Subscription> templates = subscriptionService.listAllTemplates();

        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = templates.stream().map(t -> List.of(InlineKeyboardButton.builder().text(t.getName() + " | " + t.getPrice() + "₽" + " | " + "Дата оплаты " + t.getDayOfMonth() + "-го").callbackData("SUB_" + t.getId()).build())).toList();
        ikm.setKeyboard(rows);

        SendMessage msg = SendMessage.builder().chatId(String.valueOf(chatId)).text("📜 Выберите подписку из списка ниже:").replyMarkup(ikm).build();
        executeSafe(msg);
    }

    private void showUserSubscriptions(long chatId) {
        userService.findOrCreate(chatId, null, null, null);
        Set<Subscription> subs = userService.getSubscriptions(chatId);

        if (subs.isEmpty()) {
            executeSafe(SendMessage.builder().chatId(String.valueOf(chatId)).text("ℹ️ У вас нет подписок").build());
            return;
        }

        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = subs.stream().map(t -> List.of(InlineKeyboardButton.builder().text("❌ Отписаться от \"" + t.getName()).callbackData("UNSUB_" + t.getId()).build())).toList();
        ikm.setKeyboard(rows);

        SendMessage msg = SendMessage.builder().chatId(String.valueOf(chatId)).text("📋 Ваши подписки:").replyMarkup(ikm).build();
        executeSafe(msg);
    }

    private void sendDebtors(long chatId) {
        List<Payment> debtors = paymentService.findAllDebtors();

        if (debtors.isEmpty()) {
            executeSafe(SendMessage.builder().chatId(String.valueOf(chatId)).text("✅ Должников нет").build());
            return;
        }

        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = debtors.stream().map(p -> {
            String debtorText = p.getUser().getFirstName() + " | " + p.getUser().getUsername() + " | " + p.getSubscription().getName() + " | месяц: " + p.getMonth();
            return List.of(InlineKeyboardButton.builder().text("💰 " + debtorText).callbackData("PAY_" + p.getId()).build());
        }).toList();
        ikm.setKeyboard(rows);

        SendMessage msg = SendMessage.builder().chatId(String.valueOf(chatId)).text("📌 Список должников:").replyMarkup(ikm).build();
        executeSafe(msg);
    }

    private void handleCallback(CallbackQuery cq) {
        executeSafe(AnswerCallbackQuery.builder().callbackQueryId(cq.getId()).build());

        long chatId = cq.getMessage().getChatId();
        User user = userService.findOrCreate(chatId, cq.getFrom().getUserName(), cq.getFrom().getFirstName(), cq.getFrom().getLastName());
        String data = cq.getData();

        if (data.startsWith("SUB_")) {
            long subscriptionId = Long.parseLong(data.substring(4));
            boolean added = subscriptionService.subscribe(user, subscriptionId);
            String text = added ? "✅ Подписка успешно оформлена" : "ℹ️ Вы уже подписаны на этот сервис";
            executeSafe(SendMessage.builder().chatId(String.valueOf(chatId)).text(text).build());
        }

        if (data.startsWith("UNSUB_")) {
            long subscriptionId = Long.parseLong(data.substring(6));
            boolean removed = subscriptionService.unsubscribe(user, subscriptionId);
            String text = removed ? "❌ Вы успешно отписались от подписки" : "ℹ️ Вы не были подписаны на этот сервис";
            executeSafe(SendMessage.builder().chatId(String.valueOf(chatId)).text(text).build());
        }

        if (data.startsWith("PAY_")) {
            long paymentId = Long.parseLong(data.substring(4));
            Payment payment = paymentService.findByIdWithRelations(paymentId);

            if (payment.isPayed()) {
                executeSafe(SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("ℹ️ Оплата за этот платеж уже была зафиксирована для "
                                + payment.getUser().getFirstName() + " " + payment.getUser().getUsername())
                        .build());
            } else {
                paymentService.setPaymentStatusTrue(paymentId);
                executeSafe(SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("✅ Оплата зафиксирована для "
                                + payment.getUser().getFirstName() + " " + payment.getUser().getUsername())
                        .build());

                // --- Шаблонное сообщение пользователю ---
                String messageToUser = String.format(
                        "Привет, %s! Оплата за подписку \"%s\" за %s была успешно зафиксирована. Спасибо!",
                        payment.getUser().getFirstName(),
                        payment.getSubscription().getName(),
                        payment.getMonth()
                );

                executeSafe(SendMessage.builder()
                        .chatId(String.valueOf(payment.getUser().getTelegramId())) // id пользователя
                        .text(messageToUser)
                        .build());
            }
        }
    }

    public void sendText(Long chatId, String text) {
        SendMessage msg = SendMessage.builder().chatId(String.valueOf(chatId)).text(text).build();
        executeSafe(msg);
    }

    private void executeSafe(Object request) {
        try {
            switch (request) {
                case SendMessage msg -> execute(msg);
                case AnswerCallbackQuery ack -> execute(ack);
                default -> {
                }
            }
        } catch (TelegramApiException e) {
            log.error("⚠️ Ошибка при отправке {}: {}", request.getClass().getSimpleName(), request, e);
        }
    }
}
