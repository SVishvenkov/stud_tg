package com.example.studentsbot.directory;

import com.example.studentsbot.StudentsBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryLister {
    private static final Logger logger = LogManager.getLogger(StudentsBot.class);
    private Path currentPath;
    private final Path rootPath;


    public DirectoryLister(String rootDirectory) {
     this.rootPath = Paths.get("/home/sergey/tg_directories/")
                           .toAbsolutePath()
                           .normalize();

        // Проверяем доступ
        File dir = rootPath.toFile();
        if (!dir.exists()) {
            System.err.println("Папка не существует! Создаём...");
            logger.error("Папка не существует! Создаём...");

            dir.mkdirs();
        }
        if (!dir.canRead()) {
            System.err.println("Нет прав на чтение папки!");
            logger.error("Нет прав на чтение папки!");
        }

        this.currentPath = this.rootPath;
        System.out.println("Успешно инициализирован путь: " + this.rootPath);
    }

    public DirectoryLister() {
        this(".");
    }

    public String getCurrentPath() {
        return currentPath.toString();
    }



    public boolean navigateToParent() {
        if (currentPath.equals(rootPath)) {
            return false;
        }
        currentPath = currentPath.getParent().normalize();
        return true;
    }

    public void navigateToSubdirectory(String dirName) {
        Path newPath = currentPath.resolve(dirName).normalize();
        if (newPath.startsWith(rootPath)) {
            currentPath = newPath;
        }
    }

    public List<String> getSubDirectories() {
        File currentDir = currentPath.toFile();
        System.out.println("Чтение папок в: " + currentDir.getAbsolutePath());

        File[] dirs = currentDir.listFiles(File::isDirectory);
        if (dirs == null) {
            System.err.println("Ошибка доступа! Проверьте права.");
            logger.error("Ошибка доступа! Проверьте права.");
            return List.of();
        }

        return Arrays.stream(dirs)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public List<DirectoryItem> getDirectoryContents() {
        File currentDir = currentPath.toFile();
        File[] items = currentDir.listFiles();

        if (items == null) {
            return List.of();
        }

        return Arrays.stream(items)
                .map(file -> {
                    String emoji = getEmojiForFile(file);
                    return new DirectoryItem(file.getName(), emoji, file.isDirectory());
                })
                .collect(Collectors.toList());
    }
    private String getEmojiForFile(File file) {
        if (file.isDirectory()) {
            return "📁";
        }

        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")) {
            return "📄";
        } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")) {
            return "🖼️";
        } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov")) {
            return "🎬";
        } else if (name.endsWith(".mp3") || name.endsWith(".wav")) {
            return "🎵";
        } else {
            return "📄";
        }
    }

    public static class DirectoryItem {
        private final String name;
        private final String emoji;
        private final boolean isDirectory;

        public DirectoryItem(String name, String emoji, boolean isDirectory) {
            this.name = name;
            this.emoji = emoji;
            this.isDirectory = isDirectory;
        }
        public String getName() { return name; }
        public String getEmoji() { return emoji; }
        public boolean isDirectory() { return isDirectory; }
    }

    public void resetToRoot() {
        this.currentPath = this.rootPath;
    }

    public boolean isRootDirectory() {
        return currentPath.equals(rootPath);
    }
}
