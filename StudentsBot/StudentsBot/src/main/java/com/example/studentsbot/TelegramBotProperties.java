package com.example.studentsbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramBotProperties {

    private Bot bot = new Bot();
    private Directories directories = new Directories();

    @Data
    public static class Bot {
        private String token;
        private String username;
        private Long adminId;
    }

    @Data
    public static class Directories {
        private String basePath;
    }
}
