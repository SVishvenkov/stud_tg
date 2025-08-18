package com.example.studentsbot.quiz;

import java.util.List;

public class Question {
    private final String text;
    private final List<String> options;
    private final String correct;
    private final String correctAnswer;


    public Question(String text, List<String> options, String correct, String correctAnswer) {
        this.text = text;
        this.options = options;
        this.correct = correct;
        this.correctAnswer = correctAnswer;
    }

    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public String getCorrect() { return correct; }

}