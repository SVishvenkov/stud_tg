package com.example.studentsbot.quiz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestParser {
    public static List<Question> parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Question> questions = new ArrayList<>();

        String questionText = null;
        List<String> options = null;
        String correct = null;
        String correctAnswer = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (questionText != null) {
                    questions.add(new Question(questionText, options, correct,correctAnswer));
                    questionText = null; options = null; correct = null;
                }
                continue;
            }
            if (line.startsWith("Вопрос:")) {
                questionText = line.substring("Вопрос:".length()).trim();
            } else if (line.startsWith("Варианты:")) {
                String opts = line.substring("Варианты:".length()).trim();
                options = Arrays.stream(opts.split(","))
                        .map(String::trim)
                        .toList();
            } else if (line.startsWith("Правильный:")) {
                correct = line.substring("Правильный:".length()).trim();
            }
        }
        if (questionText != null) {
            questions.add(new Question(questionText, options, correct, correctAnswer));
        }
        return questions;
    }

    public static boolean isLikelyTest(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            boolean hasQuestion = false, hasOptions = false, hasCorrect = false;
            int count = 0;
            for (String raw : lines) {
                String line = raw.trim();
                if (line.startsWith("Вопрос:")) hasQuestion = true;
                else if (line.startsWith("Варианты:")) hasOptions = true;
                else if (line.startsWith("Правильный:")) hasCorrect = true;
                if (hasQuestion && hasOptions && hasCorrect) return true;
                if (++count >= 30) break; // ограничим чтение
            }
        } catch (Exception ignored) {}
        return false;
    }
}