package com.example.studentsbot.quiz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserSession {
    private final long chatId;
    private final String testName;
    private final List<Question> questions;
    private final Map<Integer, String> answers = new HashMap<>();
    private int currentIndex = 0;
    private final String userPhoneNumber;

    public UserSession(long chatId, String testName, List<Question> questions, String userPhoneNumber) {
        this.chatId = chatId;
        this.testName = testName;
        this.questions = questions;
        this.userPhoneNumber = userPhoneNumber;
    }

    public boolean hasNext() {
        return currentIndex < questions.size();
    }

    public Question currentQuestion() {
        return questions.get(currentIndex);
    }

    public void answerCurrent(String answer) {
        answers.put(currentIndex, answer);
        currentIndex++;
    }



    public long getchatId() {
        return chatId;
    }

    public String buildReport() {

        StringBuilder sb = new StringBuilder();
        sb.append("Тест: ").append(testName).append("\n");
        sb.append("Пользователь номер: ").append(userPhoneNumber != null ? userPhoneNumber : "неизвестен").append("\n\n");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String given = answers.getOrDefault(i, "(нет ответа)");
            boolean ok = q.getCorrect().equalsIgnoreCase(given);
            sb.append(i + 1).append(". ").append(q.getText()).append("\n");
            sb.append("   Ответ: ").append(given);
            if (ok) sb.append(" ✅\n");
            else sb.append(" ❌ (правильно: ").append(q.getCorrect()).append(")\n");
        }
        return sb.toString();
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getCorrectAnswers() {
        int correctCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            String userAnswer = answers.get(i);
            String correctAnswer = questions.get(i).getCorrect();
            if (userAnswer != null && userAnswer.equalsIgnoreCase(correctAnswer)) {
                correctCount++;
            }
        }
        return correctCount;
    }
}