package com.example.studentsbot;

import com.example.studentsbot.db.UserVerification;
import com.example.studentsbot.directory.DirectoryLister;
import com.example.studentsbot.entity.Users;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StudentsBot extends TelegramLongPollingBot {
    private final UserVerification userVerification;
    private final Map<Long, String> lastKnownPaths = new ConcurrentHashMap<>();
    private final TelegramBotProperties telegramBotProperties;


    private static final Logger logger = LogManager.getLogger(StudentsBot.class);
    private static final Logger userActions = LogManager.getLogger("USER_ACTIONS");
    private static final Logger authLogger = LogManager.getLogger("AUTH_LOGGER");
    private static final Logger fileAccessLogger = LogManager.getLogger("FILE_ACCESS_LOGGER");
    private static final Logger navigationLogger = LogManager.getLogger("NAVIGATION_LOGGER");

    private static final Long ADMIN_ID = 973792032L;
    private DirectoryLister lister;


    public StudentsBot(UserVerification userVerification, TelegramBotProperties telegramBotProperties) {
        super(telegramBotProperties.getBot().getToken());
        this.userVerification = userVerification;
        this.telegramBotProperties = telegramBotProperties;
        this.lister = new DirectoryLister(telegramBotProperties.getDirectories().getBasePath());

    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String userName = message.getFrom().getUserName();
            logger.debug("Message from {}: {}", message.getFrom().getId(), message.getText());

            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start") || text.equals("Start")) {
                    requestPhoneNumber(chatId);
                }
            } else if (message.hasContact()) {
                Contact contact = message.getContact();
                String phoneNumber = contact.getPhoneNumber();
                if (userVerification.isUserValid(chatId, phoneNumber)) {
                    try {
                        authLogger.info(phoneNumber,userName,"Authorization");
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("✅ Доступ разрешен")
                                .replyMarkup(new ReplyKeyboardRemove(true))
                                .build());
                        sendPrivateDirectoryMenu(chatId, userName);
                    } catch (TelegramApiException e) {
                        logger.error("Ошибка при отправке сообщения", e);
                    }
                } else {
                    try {
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("❌ Доступ запрещен\nОбратитесь к администратору")
                                .replyMarkup(new ReplyKeyboardRemove(true))
                                .build());
                        authLogger.info(phoneNumber,userName,"No Authorization");
                    } catch (TelegramApiException e) {
                        logger.error("Ошибка при отправке сообщения", e);
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
            logger.debug("Callback from {}: {}",
                    update.getCallbackQuery().getFrom().getId(),
                    update.getCallbackQuery().getData());
        }
    }

    private void handleMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getUserName();

        if (messageText.equals("/start")) {
            // Уведомление администратора
            notifyAdminAboutStart(chatId, userName);
            // Отправка меню пользователю

        }
    }

    private void notifyAdminAboutStart(long userChatId, String userName) {
        String adminMessage = String.format(
                "Пользователь @%s (ID: %d) запустил бота",
                userName != null ? userName : "unknown",
                userChatId
        );
        SendMessage message = new SendMessage();
        message.setChatId(telegramBotProperties.getBot().getAdminId().toString());
        message.setText(adminMessage);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        userActions.info("Запустил" + userName + userChatId);
    }

    private void editDirectoryMenu(long chatId, int messageId, String userName) {
        String newPath = lister.getCurrentPath();
        String currentDirName = new File(newPath).getName();

        // Проверяем, действительно ли нужно обновлять сообщение
        if (lastKnownPaths.getOrDefault(chatId, "").equals(newPath)) {
            return; // Не обновляем, если путь не изменился
        }
        lastKnownPaths.put(chatId, newPath);

        InlineKeyboardMarkup markup = createDirectoryKeyboard(chatId);
        String newText = lister.isRootDirectory()
                ? "Основная директория"
                : "Директория: " + currentDirName;

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            if (!e.getMessage().contains("message is not modified")) {
                logger.error("Ошибка при редактировании меню: " + e.getMessage());
            }
        }
    }

    private InlineKeyboardMarkup createDirectoryKeyboard(long chatId) {
        Optional<Users> userOptional = userVerification.getUser(chatId);
        if (userOptional.isEmpty()) return null;

        Users user = userOptional.get();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (!lister.isRootDirectory()) {
            rows.add(createButtonRow("⬆️ На уровень выше", ".."));
        }

        for (DirectoryLister.DirectoryItem item : lister.getDirectoryContents()) {
            if (item.isDirectory()) {
                String fullPath = lister.getCurrentPath() + File.separator + item.getName();
                if (!hasAccessToPath(user, fullPath)) {
                    continue; // Пропускаем папки без доступа
                }
            }

            String callbackData = item.isDirectory() ? "dir:" + item.getName() : "file:" + item.getName();
            rows.add(createButtonRow(item.getEmoji() + " " + item.getName(), callbackData));
        }

        return new InlineKeyboardMarkup(rows);
    }


    private void requestPhoneNumber(Long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📱 Нажмите кнопку, чтобы поделиться номером:");

        // Настройка клавиатуры
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setOneTimeKeyboard(true); // Исчезнет после нажатия
        keyboardMarkup.setResizeKeyboard(true);  // Автоматический размер кнопки

        // Создаем кнопку "Отправить номер"
        KeyboardButton phoneButton = new KeyboardButton("📲 Отправить номер");
        phoneButton.setRequestContact(true); // Запрашиваем контакт

        // Добавляем кнопку в клавиатуру
        KeyboardRow row = new KeyboardRow();
        row.add(phoneButton);
        keyboardMarkup.setKeyboard(List.of(row));

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String userName = update.getCallbackQuery().getFrom().getUserName();
        navigationLogger.info("User {} action: {}", chatId, callbackData);

        Optional<Users> userOptional = userVerification.getUser(chatId);
        if (userOptional.isEmpty()) {
            sendTextMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        Users user = userOptional.get();
        String currentPath = lister.getCurrentPath();
        String requiredRole = getRequiredRoleForPath(currentPath);

        if (callbackData.equals("..")) {
            String previousPath = lister.getCurrentPath();
            lister.navigateToParent();

            // Проверяем, изменился ли путь
            if (!previousPath.equals(lister.getCurrentPath())) {
                editDirectoryMenu(chatId, messageId, userName);
            }
        }

        // Проверяем доступ для текущей папки
        if (!hasAccessToPath(user, currentPath)) {
            sendTextMessage(chatId, "❌ У вас нет доступа к этой папке");
            return;
        }

        // Обработка навигации...
        if (callbackData.equals("..")) {
            String newPath = String.valueOf(lister.navigateToParent());
            if (!hasAccessToPath(user, newPath)) {
                sendTextMessage(chatId, "❌ У вас нет доступа к этой папке");
                return;
            }
            editDirectoryMenu(chatId, messageId, userName);
        }


        userActions.info("Перешел в @{} (ID: {}): {}",
                userName != null ? userName : "unknown",
                chatId,
                callbackData);

        if (callbackData.equals("..")) {
            lister.navigateToParent();
            editDirectoryMenu(chatId, messageId, userName);
        } else if (callbackData.startsWith("dir:")) {
            String dirName = callbackData.substring(4);
            fileAccessLogger.debug("User {} accessing dir: {}", chatId, dirName);
            lister.navigateToSubdirectory(dirName);
            editDirectoryMenu(chatId, messageId, userName);
        } else if (callbackData.startsWith("file:")) {
            String fileName = callbackData.substring(5);
            sendFile(chatId, fileName);
        }
    }

    private boolean hasAccessToPath(Users user, String path) {
        String userRole = user.getRole().getName();
        String requiredRole = getRequiredRoleForPath(path);

        // Админ имеет доступ ко всем папкам
        if ("Admin".equals(userRole)) {
            return true;
        }

        // Если для папки не требуется определенная роль - доступ разрешен
        if (requiredRole == null) {
            return true;
        }

        // Проверяем соответствие роли
        return requiredRole.equals(userRole);
    }


    private String getRequiredRoleForPath(String path) {
        String normalizedPath = path.replace("\\", "/");

        if (normalizedPath.contains("/Barmen")) {
            return "Barmen"; // Точное соответствие с названием роли в БД
        } else if (normalizedPath.contains("/Waiter")) {
            return "Waiter";
        } else if (normalizedPath.contains("/Admin")) {
            return "Admin";
        }
        return null;
    }

    private void sendFile(long chatId, String fileName) {
        fileAccessLogger.info("File request by {}: {}", chatId, fileName);
        try {
            File fileToSend = new File(lister.getCurrentPath(), fileName);

            // Проверка существования файла
            if (!fileToSend.exists()) {
                sendTextMessage(chatId, "Файл не найден: " + fileName);
                logger.error("Файл не найден: " + fileName);
                return;
            }

            // Проверка, что это не директория
            if (fileToSend.isDirectory()) {
                sendTextMessage(chatId, "Это директория, а не файл: " + fileName);
                return;
            }

            // Проверка размера файла
            if (fileToSend.length() == 0) {
                sendTextMessage(chatId, "Файл пустой: " + fileName);
                return;
            }

            String mimeType = Files.probeContentType(fileToSend.toPath());

            if (mimeType == null) {
                // Стандартная отправка как документ
                execute(new SendDocument(Long.toString(chatId), new InputFile(fileToSend)));
                return;
            }

            // Оптимизация для разных типов файлов
            if (mimeType.startsWith("image/")) {
                execute(new SendPhoto(Long.toString(chatId), new InputFile(fileToSend)));
                logger.info("Sending file {} to chat {}", fileName, chatId);
            } else if (mimeType.startsWith("video/")) {
                execute(new SendVideo(Long.toString(chatId), new InputFile(fileToSend)));
                logger.info("Sending file {} to chat {}", fileName, chatId);
            } else if (mimeType.startsWith("audio/")) {
                execute(new SendAudio(Long.toString(chatId), new InputFile(fileToSend)));
                logger.info("Sending file {} to chat {}", fileName, chatId);
            } else {
                execute(new SendDocument(Long.toString(chatId), new InputFile(fileToSend)));
                logger.info("Sending file {} to chat {}", fileName, chatId);
            }
            fileAccessLogger.debug("File sent successfully: {}", fileName);

        } catch (IOException | TelegramApiException e) {
            sendTextMessage(chatId, "Ошибка отправки файла: " + e.getMessage());
            logger.error("Failed to send file {}: {}", fileName, e.getMessage());
            e.printStackTrace();
        }
    }

    private List<InlineKeyboardButton> createButtonRow(String text, String callbackData) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
        return List.of(button);
    }


    private void sendTextMessage(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке текстового сообщения: " + e.getMessage());
            e.printStackTrace();
            logger.error("Ошибка при отправке текстового сообщения: ", e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);  // Автоматически подгонять размер
        keyboard.setOneTimeKeyboard(false); // Клавиатура не будет скрываться после нажатия

        // Создаем список строк кнопок
        List<KeyboardRow> rows = new ArrayList<>();

        // Первая (и единственная) строка с кнопкой "Start"
        KeyboardRow row = new KeyboardRow();
        row.add("🔄 Start");
        rows.add(row);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void handleAccess(long chatId) {
        Optional<String> role = userVerification.getUserRole(chatId);

        if (role.isEmpty()) {
            sendTextMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        String directoryPath;
        switch (role.get()) {
            case "Admin":
                directoryPath = "C:/path/to/admin";
                break;
            case "BARMEN":
                directoryPath = "C:/path/to/barmen";
                break;
            case "WAITER":
                directoryPath = "C:/path/to/waiter";
                break;
            default:
                sendTextMessage(chatId, "❌ У вас нет доступа");
                return;
        }
    }

    private void sendPrivateDirectoryMenu(long chatId, String userName) {
        logger.info("Opening directory menu for {} (ID: {})", userName, chatId);
        Optional<Users> userOptional = userVerification.getUser(chatId);
        if (userOptional.isEmpty()) {
            sendTextMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        Users user = userOptional.get();
        String roleName = user.getRole().getName();
        String directoryPath = getBasePathForRole(roleName);

        if (directoryPath == null) {
            sendTextMessage(chatId, "❌ У вас нет доступа");
            return;
        }

        this.lister = new DirectoryLister(directoryPath);
        InlineKeyboardMarkup markup = createDirectoryKeyboard(chatId);

        if ("Admin".equals(user.getRole().getName())) {
            directoryPath = "C:/Users/Serge/Documents/Bot/directories";
        } else if ("Barmen".equals(user.getRole().getName())) {
            directoryPath = "C:/Users/Serge/Documents/Bot/directories/Barmen";
        } else if ("Waiter".equals(user.getRole().getName())) {
            directoryPath = "C:/Users/Serge/Documents/Bot/directories/Waiter";
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Доступные файлы");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке меню директории", e);
            logger.error("Failed to send menu to {}: {}", chatId, e.getMessage());
        }
    }

    private String getBasePathForRole(String roleName) {
        String basePath = telegramBotProperties.getDirectories().getBasePath();
        switch(roleName) {
            case "Admin": return basePath;
            case "Barmen": return basePath + "/Barmen";
            case "Waiter": return basePath + "/Waiter";
            default: return null;
        }
    }

    @Override
    public String getBotToken() {
        return telegramBotProperties.getBot().getToken();
    }

    @Override
    public String getBotUsername() {
        return telegramBotProperties.getBot().getUsername();
    }
}
