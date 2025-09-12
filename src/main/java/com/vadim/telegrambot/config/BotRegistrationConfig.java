package com.vadim.telegrambot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.vadim.telegrambot.service.ReminderBot;

@Configuration
public class BotRegistrationConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(ReminderBot reminderBot) throws Exception {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(reminderBot);
        return api;
    }
}
