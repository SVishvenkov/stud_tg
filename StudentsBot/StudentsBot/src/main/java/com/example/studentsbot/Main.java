package com.example.studentsbot;

import com.example.studentsbot.db.UserVerification;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@SpringBootApplication
public class Main {
    public static void main(String[] args) throws TelegramApiException {

        ApplicationContext context = SpringApplication.run(Main.class, args);
        UserVerification userVerification = context.getBean(UserVerification.class);
        StudentsBot bot = new StudentsBot(userVerification);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
