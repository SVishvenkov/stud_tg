package com.example.studentsbot;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramBotProperties {

    private Bot bot = new Bot();
    private Directories directories = new Directories();

    public Bot getBot() {
        return bot;
    }

    public Directories getDirectories() {
        return directories;
    }


    @Getter
    @Data
    public static class Bot {
        private String token;
        private String username;
        private Long adminId;
        private String kipUrl;

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public Long getAdminId() {
            return adminId;
        }

        public String getKipUrl() {return  kipUrl;}

    }

    @Getter
    @Data
    public static class Directories {
        private String basePath;

        public String getBasePath() {
            return basePath;
        }
    }
}
