package com.example.studentsbot.quiz;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestLoader {
    private final Path testsDir;
    private final Map<String, Path> nameToFile = new HashMap<>();

    public TestLoader(Path testsDir) throws IOException {
        this.testsDir = testsDir;
        reload();
    }

    public void reload() throws IOException {
        nameToFile.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testsDir, "*.txt")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.endsWith(".txt")) name = name.substring(0, name.length() - 4);
                nameToFile.put(name, p);
            }
        }
    }

    public Set<String> availableTests() {
        return nameToFile.keySet();
    }

    public List<Question> loadTest(String name) throws IOException {
        Path p = nameToFile.get(name);
        if (p == null) throw new IllegalArgumentException("Тест не найден: " + name);
        return TestParser.parse(p);
    }
}