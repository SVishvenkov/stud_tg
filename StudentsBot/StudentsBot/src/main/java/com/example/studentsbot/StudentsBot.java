package com.example.studentsbot;

import com.example.studentsbot.db.UserVerification;
import com.example.studentsbot.directory.DirectoryLister;
import com.example.studentsbot.entity.Users;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
import com.example.studentsbot.quiz.*;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StudentsBot extends TelegramLongPollingBot {
    private final UserVerification userVerification;
    private final Map<Long, String> lastKnownPaths = new ConcurrentHashMap<>();
    private final TelegramBotProperties telegramBotProperties;
    private final Map<Long, Integer> lastMenuMessageId = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> menuPromptSent = new ConcurrentHashMap<>();
    private final QuizManager quizManager;


    private static final Logger logger = LogManager.getLogger(StudentsBot.class);
    private static final Logger userActions = LogManager.getLogger("USER_ACTIONS");
    private static final Logger authLogger = LogManager.getLogger("AUTH_LOGGER");
    private static final Logger fileAccessLogger = LogManager.getLogger("FILE_ACCESS_LOGGER");
    private static final Logger navigationLogger = LogManager.getLogger("NAVIGATION_LOGGER");

    private DirectoryLister lister;

    private final Map<Long, Integer> lastMessageIds = new HashMap<>();
    private final Map<Long, String> lastKnownsPaths = new HashMap<>();
    private final Map<Long, String> lastKnownTexts = new HashMap<>();
    private final Map<Long, InlineKeyboardMarkup> lastKnownMarkups = new HashMap<>();


    @Autowired
    public StudentsBot(UserVerification userVerification, TelegramBotProperties telegramBotProperties, DirectoryLister lister) {
        super(telegramBotProperties.getBot().getToken());
        this.userVerification = userVerification;
        this.telegramBotProperties = telegramBotProperties;
        this.lister = lister;
        long adminId = telegramBotProperties.getBot().getAdminId();
        this.quizManager = new QuizManager(this, adminId,userVerification);

    }


    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String userName = message.getFrom().getUserName();
            logger.debug("Message from {}: {}", message.getFrom().getId(), message.getText());

            InlineKeyboardMarkup markup = createDirectoryKeyboard(chatId);
            if (message.hasText()) {
                String text = message.getText();

                if ("КИП".equals(text)) {
                    sendKipLink(chatId);
                    return;
                }

                if (text.equals("/start") || text.equals("Start")) {
                    requestPhoneNumber(chatId);
                    notifyAdminAboutStart(chatId, userName);
                } else if (text.equals("📁 Меню")) {
                    sendPrivateDirectoryMenu(chatId, userName, markup);
                }


            } else if (message.hasContact()) {
                Contact contact = message.getContact();
                String phoneNumber = contact.getPhoneNumber();
                if (userVerification.isUserValid(chatId, phoneNumber)) {
                    try {
                        authLogger.info(phoneNumber, userName, "Authorization");
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("✅ Доступ разрешен")
                                .replyMarkup(new ReplyKeyboardRemove(true))
                                .protectContent(true)
                                .build());
                        sendPrivateDirectoryMenu(chatId, userName, markup);
                        sendMenuButton(chatId,true);
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
                        authLogger.info(phoneNumber, userName, "No Authorization");
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

    private void sendKipLink(long chatId) {
        String kipUrl = telegramBotProperties.getBot().getKipUrl();

        if (kipUrl == null || kipUrl.isBlank()) {
            logger.error("KIP URL не задан в настройках!");
            return;
        }

        if (!kipUrl.startsWith("http://") && !kipUrl.startsWith("https://")) {
            logger.error("KIP URL имеет неверный формат: " + kipUrl);
            return;
        }

        logger.info("KIP URL: " + kipUrl);
        InlineKeyboardButton linkButton = InlineKeyboardButton.builder()
                .text("Открыть КИП")
                .url(kipUrl)
                .build();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(List.of(linkButton)));

        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text("🔗 Ссылка на КИП:")
                    .replyMarkup(markup)
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке ссылки КИП: " + e.getMessage());
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

    private void hideMenuIfPresent(long chatId) {
        Integer menuMsgId = lastMenuMessageId.get(chatId);
        if (menuMsgId == null) return;

        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(menuMsgId);
        edit.setReplyMarkup(null); // прячет inline-клавиатуру
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            logger.warn("Не удалось скрыть меню: {}", e.getMessage());
        }
        lastMenuMessageId.remove(chatId);
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

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        closeRow.add(InlineKeyboardButton.builder()
                .text("❌ Закрыть меню")
                .callbackData("close_menu")
                .build());
        rows.add(closeRow);

        if (!lister.isRootDirectory()) {
            rows.add(createButtonRow("⬆️ На уровень выше", ".."));
        }

        for (DirectoryLister.DirectoryItem item : lister.getDirectoryContents()) {
            String fullPath = lister.getCurrentPath() + File.separator + item.getName();

            if (item.isDirectory()) {
                if (!hasAccessToPath(user, fullPath)) continue;
                rows.add(createButtonRow(item.getEmoji() + " " + item.getName(), "dir:" + item.getName()));
            } else {
                // Проверяем .txt на ссылку
                if (item.getName().toLowerCase().endsWith(".txt")) {
                    try {
                        String content = Files.readString(Path.of(fullPath)).trim();
                        if (isValidUrl(content)) {
                            rows.add(List.of(
                                    InlineKeyboardButton.builder()
                                            .text(item.getEmoji() + " " + removeExtension(item.getName()))
                                            .url(content) // вместо callbackData
                                            .build()
                            ));
                            continue;
                        }
                    } catch (IOException e) {
                        logger.error("Не удалось прочитать файл: " + fullPath, e);
                    }
                }

                // Обычный файл
                rows.add(createButtonRow(item.getEmoji() + " " + removeExtension(item.getName()), "file:" + item.getName()));
            }
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

    private void sendMenuButton(long chatId, boolean forceText) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("📁 Меню"));

        // Если Франчайзи — добавляем кнопку КИП
        boolean isFranchisee = userVerification.getUser(chatId)
                .map(u -> "Франчайзи/Собственники".equalsIgnoreCase(u.getRole().getName()))
                .orElse(false);

        if (isFranchisee) {
            KeyboardButton kipButton = new KeyboardButton("КИП");
            row.add(kipButton);
        }

        keyboardMarkup.setKeyboard(List.of(row));

        String text;
        if (forceText || !Boolean.TRUE.equals(menuPromptSent.get(chatId))) {
            text = "Нажмите кнопку, чтобы открыть меню";
            menuPromptSent.put(chatId, true);
        } else {
            text = "\u200B";
        }

        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(keyboardMarkup)
                .protectContent(true)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке кнопки меню: " + e.getMessage());
        }
    }


    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String userName = update.getCallbackQuery().getFrom().getUserName();
        long userId = update.getCallbackQuery().getFrom().getId();


        navigationLogger.info("User {} action: {}", chatId, callbackData);
        Optional<Users> userOptional = userVerification.getUser(chatId);
        if (userOptional.isEmpty()) {
            sendTextMessage(chatId, "❌ Пользователь не найден");
            return;
        }
        Users user = userOptional.get();

        String currentPath = lister.getCurrentPath();

        // 1. Если клик по .txt и файл выглядит как тест — запускаем тест
        if (callbackData.startsWith("file:")) {
            String fileName = callbackData.substring(5);
            File maybe = new File(currentPath + File.separator + fileName);
            if (maybe.exists() && fileName.toLowerCase().endsWith(".txt")) {
                Path testFile = maybe.toPath();
                if (com.example.studentsbot.quiz.TestParser.isLikelyTest(testFile)) {
                    String testName = fileName.substring(0, fileName.lastIndexOf('.'));
                    try {
                        hideMenuIfPresent(chatId);
                        List<com.example.studentsbot.quiz.Question> questions = com.example.studentsbot.quiz.TestParser.parse(testFile);
                        String phoneNumber = userVerification.getUser(userId)
                                .map(Users::getUserNumber)
                                .orElse("неизвестен");
                        com.example.studentsbot.quiz.UserSession session = new com.example.studentsbot.quiz.UserSession(userId, testName, questions,phoneNumber);
                        quizManager.startCustomSession(session);
                    } catch (Exception e) {
                        sendTextMessage(chatId, "Не удалось загрузить тест: " + e.getMessage());
                    }
                    return;
                }
            }
        }

        // 2. Если уже есть активная сессия — делегируем ответ
        if (quizManager.hasSession(userId)) {
            quizManager.handleCallback(update.getCallbackQuery());
            return;
        }

        // 3. Обычная навигация / файл / директория
        if (callbackData.equals("..")) {
            boolean moved = lister.navigateToParent();
            if (moved) {
                editDirectoryMenu(chatId, messageId, userName);
            }
            return;
        }

        if (!hasAccessToPath(user, currentPath)) {
            sendTextMessage(chatId, "❌ У вас нет доступа к этой папке");
            return;
        }

        if ("close_menu".equals(callbackData)) {
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(String.valueOf(chatId));
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(null);
            try {
                execute(editMarkup);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при закрытии меню: " + e.getMessage());
            }
            return;
        }

        userActions.info("Перешел в @{} (ID: {}): {}", userName != null ? userName : "unknown", chatId, callbackData);

        if (callbackData.startsWith("dir:")) {
            String dirName = callbackData.substring(4);
            fileAccessLogger.debug("User {} accessing dir: {}", chatId, dirName);
            lister.navigateToSubdirectory(dirName);
            editDirectoryMenu(chatId, messageId, userName);
        } else if (callbackData.startsWith("file:")) {
            String fileName = callbackData.substring(5);
            sendFile(String.valueOf(chatId), fileName);
        }
    }

    private boolean hasAccessToPath(Users user, String path) {
        String userRole = user.getRole().getName();
        String requiredRole = getRequiredRoleForPath(path);

        // Админ имеет доступ ко всем папкам
        if ("Управляющий".equals(userRole) || "Франчайзи/Собственники".equals(userRole)) {
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

        if (normalizedPath.contains("/Старшая хозяйка")) {
            return "Старшая хозяйка"; // Точное соответствие с названием роли в БД
        } else if (normalizedPath.contains("/Хозяйка")) {
            return "Хозяйка";
        } else if (normalizedPath.contains("/Управляющий")) {
            return "Управляющий";
        } else if (normalizedPath.contains("/Повар")) {
            return "Повар";
        }
        return null;
    }

    private void sendFile(String chatId, String fileName) {
        long parsedChatId = Long.parseLong(chatId);
        hideMenuIfPresent(parsedChatId);
        fileAccessLogger.info("File request by {}: {}", chatId, fileName);
        try {
            DirectoryLister.FileContent fileContent = lister.getFileContent(fileName);
            if (fileContent == null) {
                sendTextMessage(Long.parseLong(chatId), "❌ Файл не найден или недоступен");
                return;
            }

            switch (fileContent.getContentType()) {
                case TEXT:
                    sendTextContent(Long.parseLong(chatId), fileContent);
                    break;
                case IMAGE:
                    SendPhoto photo = new SendPhoto();
                    photo.setChatId(chatId);
                    photo.setPhoto(new InputFile(new File(fileContent.getContent())));
                    photo.setProtectContent(true);
                    execute(photo);
                    break;
                case VIDEO:
                    SendVideo video = new SendVideo();
                    video.setChatId(chatId);
                    video.setVideo(new InputFile(new File(fileContent.getContent())));
                    video.setSupportsStreaming(true);
                    video.setProtectContent(true);
                    execute(video);
                    break;
                case AUDIO:
                    execute(new SendAudio(chatId, new InputFile(new File(fileContent.getContent()))));
                    break;
                case FILE:
                default:
                    if (fileContent.getContent().endsWith(".pdf")) {
                        sendPdfAsText(chatId, new File(fileContent.getContent()));
                    }else if (fileContent.getContent().endsWith(".docx")) {
                        sendDocxAsText(chatId, new File(fileContent.getContent()));
                    }
                    else {
                        execute(new SendDocument(chatId, new InputFile(new File(fileContent.getContent()))));
                    }
                    break;
            }
        } catch (IOException e) {
            sendTextMessage(Long.parseLong(chatId), "❌ Ошибка чтения файла: " + e.getMessage());
            logger.error("Ошибка чтения файла", e);
        } catch (TelegramApiException e) {
            sendTextMessage(Long.parseLong(chatId), "❌ Ошибка отправки файла: " + e.getMessage());
            logger.error("Ошибка отправки файла", e);
        }
    }

    private void sendDocxAsText(String chatId, File docxFile) {
        hideMenuIfPresent(Long.parseLong(chatId));
        try (FileInputStream fis = new FileInputStream(docxFile)) {
            XWPFDocument document = new XWPFDocument(fis);
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String text = extractor.getText();

            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "\n\n... (файл слишком большой, показаны первые 4000 символов)";
            }

            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("📄 DOCX-текст из " + docxFile.getName() + ":\n" + text)
                    .protectContent(true)
                    .build());
        } catch (Exception e) {
            try {
                execute(new SendDocument(chatId, new InputFile(docxFile)));
            } catch (TelegramApiException ex) {
                sendTextMessage(Long.parseLong(chatId), "❌ Ошибка обработки DOCX: " + ex.getMessage());
            }
        }
    }

    private void sendTextContent(long chatId, DirectoryLister.FileContent fileContent) throws TelegramApiException {
        hideMenuIfPresent(chatId);
        String content = fileContent.getContent();
        if (content.length() > 4000) {
            for (int i = 0; i < content.length(); i += 4000) {
                String part = content.substring(i, Math.min(i + 4000, content.length()));
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("📄 " + fileContent.getFileName() + " (часть " + (i / 4000 + 1) + "):\n" + part)
                        .protectContent(true)
                        .build());
            }
        } else {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("📄 " + fileContent.getFileName() + ":\n" + content)
                    .protectContent(true)
                    .build());
        }
    }

    private void sendPdfAsText(String chatId, File pdfFile) {
        hideMenuIfPresent(Long.parseLong(chatId));
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "\n\n... (файл слишком большой, показаны первые 4000 символов)";
            }

            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("📄 PDF-текст из " + pdfFile.getName() + ":\n" + text)
                    .protectContent(true)
                    .build());
        } catch (Exception e) {
            try {
                execute(new SendDocument(chatId.toString(), new InputFile(pdfFile)));
            } catch (TelegramApiException ex) {
                sendTextMessage(Long.parseLong(chatId), "❌ Ошибка обработки PDF: " + ex.getMessage());
            }
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
            message.setProtectContent(true);
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке текстового сообщения: " + e.getMessage());
            e.printStackTrace();
            logger.error("Ошибка при отправке текстового сообщения: ", e.getMessage());
        }
    }

    private void sendPrivateDirectoryMenu(long chatId, String text, InlineKeyboardMarkup markup) {
        Integer existingMessageId = lastMessageIds.get(chatId);

        // Получаем текущий путь
        String newPath = lister.getCurrentPath();
        String oldPath = lastKnownsPaths.get(chatId);
        boolean isSamePath = oldPath != null && oldPath.equals(newPath);

        String oldText = lastKnownTexts.get(chatId);
        boolean isSameText = oldText != null && oldText.equals(text);

        InlineKeyboardMarkup oldMarkup = lastKnownMarkups.get(chatId);
        boolean isSameMarkup = oldMarkup != null && oldMarkup.equals(markup);

        if (isSamePath && isSameText && isSameMarkup) {
            logger.debug("📂 Меню для {} не изменилось — пропуск", chatId);
            return;
        }

        lastKnownPaths.put(chatId, newPath);
        lastKnownTexts.put(chatId, text);
        lastKnownMarkups.put(chatId, markup);

        if (existingMessageId != null && existingMessageId > 0) {
            try {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(existingMessageId);
                edit.setText(text);
                edit.setReplyMarkup(markup);
                edit.setParseMode("HTML");
                execute(edit);
                return; // Успешно — выходим
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("message is not modified")) {
                    logger.warn("⚠ Не удалось отредактировать меню {}: {}", chatId, e.getMessage());
                }
            }
        }

        try {
            SendMessage send = new SendMessage();
            send.setChatId(String.valueOf(chatId));
            send.setText(text);
            send.setReplyMarkup(markup);
            send.setParseMode("HTML");
            Message message = execute(send);
            lastMessageIds.put(chatId, message.getMessageId());
        } catch (TelegramApiException e) {
            logger.error("❌ Ошибка при отправке меню {}: {}", chatId, e.getMessage());
        }
    }

    private String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? filename : filename.substring(0, lastDot);
    }
    private boolean isValidUrl(String text) {
        try {
            new java.net.URL(text).toURI();
            return true;
        } catch (Exception e) {
            return false;
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
