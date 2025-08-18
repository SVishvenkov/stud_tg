package com.example.studentsbot.quiz;

import com.example.studentsbot.db.UserVerification;
import com.example.studentsbot.entity.Users;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuizManager {
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final TelegramLongPollingBot bot;
    private final long adminChatId;
    private final UserVerification userVerification;

    public QuizManager(TelegramLongPollingBot bot, long adminChatId, UserVerification userVerification) {
        this.bot = bot;
        this.adminChatId = adminChatId;
        this.userVerification = userVerification;
    }

    public void startCustomSession(UserSession session) {
        long chatId = session.getchatId();
        sessions.put(chatId, session);
        sendQuestion(chatId, session);
    }

    public boolean hasSession(long chatId) {
        return sessions.containsKey(chatId);
    }

    public void handleCallback(CallbackQuery callback) {
        long chatId = callback.getFrom().getId();
        String data = callback.getData();
        String phoneUser = userVerification.getUser(chatId)
                .map(Users::getUserNumber)
                .orElse("Неизвестно");


        UserSession session = sessions.get(chatId);
        if (session == null) {
            answerCallback(callback, "Тест не запущен. Выберите файл-тест в папке.");
            return;
        }

        session.answerCurrent(data);
        editPreviousQuestion(callback, data);

        if (session.hasNext()) {
            sendQuestion(chatId, session);
        } else {
            int total = session.getQuestions().size();
            int correct = session.getCorrectAnswers();
            int percent = (int) Math.round((correct * 100.0) / total);

            String percentText = "Процент верных ответов: " + percent + "%";

            sendText(chatId, percentText + "\nТест завершён, спасибо! Результаты отправлены админу.");
            String report = percentText + "\n" + session.buildReport();

            sendText(adminChatId, report);
            sendText(chatId,report);
            sessions.remove(chatId);
        }
    }

    private void sendQuestion(long chatId, UserSession session) {
        Question q = session.currentQuestion();
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(q.getText());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String opt : q.getOptions()) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(opt);
            btn.setCallbackData(opt);
            rows.add(Collections.singletonList(btn));
        }
        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editPreviousQuestion(CallbackQuery callback, String chosen) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(callback.getFrom().getId()));
        edit.setMessageId(callback.getMessage().getMessageId());
        edit.setText("Вы выбрали: " + chosen);
        try {
            bot.execute(edit);
        } catch (TelegramApiException ignored) {}
    }

    private void answerCallback(CallbackQuery cb, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(cb.getId());
        answer.setText(text);
        try {
            bot.execute(answer);
        } catch (TelegramApiException ignored) {}
    }

    private void sendText(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}