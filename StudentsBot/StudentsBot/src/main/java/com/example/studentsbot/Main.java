package com.example.studentsbot;

import com.example.studentsbot.db.UserVerification;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Map;


@SpringBootApplication
public class Main {

public static void main(String[] args) {
//    try {
//        ApplicationContext context = SpringApplication.run(Main.class, args);
//        // Получаем бота из Spring контекста
//        StudentsBot bot = context.getBean(StudentsBot.class);
//        // Регистрируем бота
//        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
//        botsApi.registerBot(bot);
//        System.out.println("Bot started successfully!");
//
//    }
//


    try {
        // Инициализируем SpringApplication с настройкой внешнего файла конфигурации
        SpringApplication app = new SpringApplication(Main.class);
        String configLocation = "file:C:/Users/Serge/Dropbox/conf.yml";
        app.setDefaultProperties(Map.of("spring.config.location", configLocation /*"debug", "true"*/));

        // Запускаем приложение и получаем контекст
        ApplicationContext context = app.run(args);

        // Получаем бота из Spring контекста
        StudentsBot bot = context.getBean(StudentsBot.class);

        // Регистрируем бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        System.out.println("Bot started successfully!");

    }    catch (TelegramApiException e) {
        System.err.println("Failed to start bot: " + e.getMessage());
        e.printStackTrace();
    }
}


}
