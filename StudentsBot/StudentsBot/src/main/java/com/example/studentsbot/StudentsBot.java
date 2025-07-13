package com.example.studentsbot;

import com.example.studentsbot.db.UserVerification;
import com.example.studentsbot.directory.DirectoryLister;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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


public class StudentsBot extends TelegramLongPollingBot {
    private final UserVerification userVerification;


    private static final Logger logger = LogManager.getLogger(StudentsBot.class);
    private static final Logger userActions = LogManager.getLogger("USER_ACTIONS");
    private static final Logger userAuth = LogManager.getLogger("USER_AUTH");

    private static final Long ADMIN_ID = 973792032L;
    private DirectoryLister lister;

    public StudentsBot(UserVerification userVerification) {
        super("7574057992:AAHJy_MnNppVALrz0tIPPEr_zZd8k0OJmSA");
        this.userVerification = userVerification;
        this.lister = new DirectoryLister("C:Users/Serge/Documents/Bot/directories");
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        String userName = message.getFrom().getUserName();

        if (message.hasText()) {
            String text = message.getText();

            if (text.equals("/start") || text.equals("Start")) {
                requestPhoneNumber(chatId); // Запрашиваем номер
            }
        } else if (message.hasContact()) {
            // Обработка номера телефона
            Contact contact = message.getContact();
            String phoneNumber = contact.getPhoneNumber();


            if (userVerification.isUserValid(chatId, phoneNumber)) {
                try {
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ Доступ разрешен")
                            .replyMarkup(new ReplyKeyboardRemove(true))
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                sendPrivateDirectoryMenu(chatId, userName);
            } else {
                try {
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("❌ Доступ запрещен\nОбратитесь к администратору")
                            .replyMarkup(new ReplyKeyboardRemove(true))
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
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
        message.setChatId(ADMIN_ID.toString());
        message.setText(adminMessage);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        userActions.info("Запустил" + userName + userChatId);
    }


    private void sendPrivateDirectoryMenu(long chatId, String userName) {
        InlineKeyboardMarkup markup = createDirectoryKeyboard();
        String safeUserName = (userName != null) ? userName : "unknown";

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Основная директория");
        message.setReplyMarkup(markup);

        // Устанавливаем параметр, чтобы сообщение было видно только отправителю
        message.setReplyToMessageId(null);
        userActions.info("Пользователь {} (@{}) перешел в {}",
                chatId,
                safeUserName,
                lister.getCurrentPath());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void editDirectoryMenu(long chatId, int messageId, String userName) {
        InlineKeyboardMarkup markup = createDirectoryKeyboard();
        String currentDirName = new File(lister.getCurrentPath()).getName();
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        if (lister.isRootDirectory()) {
            editMessage.setText("Основная директория");
        } else {
            editMessage.setText("Директория: " + currentDirName);
        }
        editMessage.setReplyMarkup(markup);

        userActions.info("Пользователь " + chatId + userName != null ? userName : "unknown" + " перешел в " + lister.getCurrentPath());
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при редактировании меню: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup createDirectoryKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "На уровень выше" (если не в корневой директории)
        if (!lister.isRootDirectory()) {
            rows.add(createButtonRow("⬆️ В зад", ".."));
        }

        // Список папок и файлов
        for (DirectoryLister.DirectoryItem item : lister.getDirectoryContents()) {
            String callbackData = item.isDirectory() ? "dir:" + item.getName() : "file:" + item.getName();
            rows.add(createButtonRow(item.getEmoji() + " " + item.getName(), callbackData));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
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
        int messageId = update.getCallbackQuery().getMessage().getMessageId(); // Получаем ID сообщения
        String userName = update.getCallbackQuery().getFrom().getUserName();

        userActions.info("Перешел в @{} (ID: {}): {}",
                userName != null ? userName : "unknown",
                chatId,
                callbackData);


        if (callbackData.equals("..")) {
            lister.navigateToParent();
        } else if (callbackData.startsWith("dir:")) {
            String dirName = callbackData.substring(4);
            lister.navigateToSubdirectory(dirName);
        } else if (callbackData.startsWith("file:")) {
            String fileName = callbackData.substring(5);
            sendFile(chatId, fileName);
            return; // Не обновляем меню после отправки файла
        }

        // Редактируем текущее сообщение вместо отправки нового
        editDirectoryMenu(chatId, update.getCallbackQuery().getMessage().getMessageId(), userName);
    }

    private void sendFile(long chatId, String fileName) {
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

    @Override
    public String getBotToken() {
        return "7574057992:AAHJy_MnNppVALrz0tIPPEr_zZd8k0OJmSA";
    }

    @Override
    public String getBotUsername() {
        return "korzhovvbot";
    }
}
