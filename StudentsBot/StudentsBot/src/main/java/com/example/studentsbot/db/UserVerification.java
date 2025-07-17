package com.example.studentsbot.db;

import com.example.studentsbot.entity.Users;
import com.example.studentsbot.repositories.UserRepositories;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

//import static jdk.internal.org.jline.utils.Log.info;

@Service
@RequiredArgsConstructor
public class UserVerification {
    private static final Logger log = LoggerFactory.getLogger(UserVerification.class);
    private final UserRepositories userRep;

    public boolean isUserValid(long chatId, String userNumber) {
        String normalizedPhone = normalizePhone(userNumber);
        log.info("Checking access: chatId={}, normalizedPhone={}", chatId, normalizedPhone);
        boolean exists = userRep.existsByChatIdAndUserNumber(chatId, normalizedPhone);
        log.info("Result: {}", exists);
        return exists;
    }

    public Optional<Users> getUser(Long chatId) {
        return userRep.findByChatId(chatId);
    }

    // Новый метод для проверки роли пользователя
    public boolean hasRole(Long chatId, String roleName) {
        return userRep.existsByChatIdAndRole_Name(chatId, roleName);
    }

    // Новый метод для получения роли пользователя
    public Optional<String> getUserRole(Long chatId) {
        return userRep.findByChatId(chatId)
                .map(user -> user.getRole().getName());
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("8")) {
            return "7" + digits.substring(1);
        }
        return digits.startsWith("7") ? digits : "7" + digits;
    }
}
