package com.example.studentsbot.directory;

import com.example.studentsbot.StudentsBot;
import com.example.studentsbot.TelegramBotProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DirectoryLister {
    private static final Logger logger = LogManager.getLogger(StudentsBot.class);
    private Path currentPath;
    private final Path rootPath;
//    private final TelegramBotProperties properties;


    public DirectoryLister(@Value("${telegram.directories.base_path}") String rootDirectory) {
     this.rootPath = Paths.get(rootDirectory)
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

    public FileContent getFileContent(String fileName) throws IOException {
        Path filePath = currentPath.resolve(fileName).normalize();
        File file = filePath.toFile();

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = getMimeType(file.getName());
        }


        if (mimeType.startsWith("text/") || mimeType.equals("application/json")) {
            String content = Files.readString(filePath);
            return new FileContent(file.getName(), content, FileContent.ContentType.TEXT);
        } else if (mimeType.startsWith("image/")) {
            return new FileContent(file.getName(), filePath.toString(), FileContent.ContentType.IMAGE);
        } else if (mimeType.startsWith("video/")) {
            return new FileContent(file.getName(), filePath.toString(), FileContent.ContentType.VIDEO);
        } else if (mimeType.startsWith("audio/")) {
            return new FileContent(file.getName(), filePath.toString(), FileContent.ContentType.AUDIO);
        } else {
            return new FileContent(file.getName(), filePath.toString(), FileContent.ContentType.FILE);
        }
    }

    public static class FileContent {
        private final String fileName;
        private final String content;
        private final ContentType contentType;

        public FileContent(String fileName, String content, ContentType contentType) {
            this.fileName = fileName;
            this.content = content;
            this.contentType = contentType;
        }

        public String getFileName() { return fileName; }
        public String getContent() { return content; }
        public ContentType getContentType() { return contentType; }

        public enum ContentType {
            TEXT, IMAGE, VIDEO, AUDIO, FILE
        }
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "txt": return "text/plain";
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mov": return "video/quicktime";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "json": return "application/json";
            case "csv": return "text/csv";
            case "xml": return "application/xml";
            default: return "application/octet-stream";
        }
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
