// Блок 1 (строки 1-70)
package com.gametools.fstools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileCoreEngine {

    public interface OnFileOperationListener {
        void onLogReceived(String msg);
        void onProgressUpdated(int progress, String currentFile);
        void onOperationFinished(boolean success, String errorMsg);
    }

    private static int totalFilesProcessed = 0;
    private static int totalFilesCount = 0;

    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;

    public static void startOperation(String sourcePath, String targetPath, String extensionInput, int operationType, boolean keepEmptyDirs, OnFileOperationListener listener) {
        File sourceDir = new File(sourcePath);
        File targetDir = targetPath != null ? new File(targetPath) : null;

        String[] rawTokens = extensionInput.split(",");
        List<Pattern> compiledPatterns = new ArrayList<Pattern>();

        for (String token : rawTokens) {
            String trimmed = token.trim();
            if (trimmed.length() > 0) {
                compiledPatterns.add(compileMaskToPattern(trimmed));
            }
        }

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            listener.onOperationFinished(false, "Исходная папка не существует");
            return;
        }

        if (compiledPatterns.isEmpty()) {
            compiledPatterns.add(compileMaskToPattern("*.*"));
        }

        try {
            listener.onLogReceived("Сканирование дерева папок по сложной маске...");

            totalFilesCount = countMatchingFiles(sourceDir, compiledPatterns);
            totalFilesProcessed = 0;

            if (totalFilesCount == 0 && !keepEmptyDirs) {
                listener.onLogReceived("Файлы, соответствующие маске \"" + extensionInput + "\", не найдены.");
                listener.onOperationFinished(true, null);
                return;
            }

            listener.onLogReceived("Найдено файлов для обработки: " + totalFilesCount);

            processRecursive(sourceDir, sourceDir, targetDir, compiledPatterns, operationType, keepEmptyDirs, listener);

            listener.onOperationFinished(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onOperationFinished(false, e.getMessage());
        }
    }
// Блок 2 (строки 71-140)
    private static Pattern compileMaskToPattern(String mask) {
        String cleanMask = mask.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("^"); 

        for (int i = 0; i < cleanMask.length(); i++) {
            char c = cleanMask.charAt(i);
            if (c == '*') {
                sb.append(".*"); 
            } else if (c == '?') {
                sb.append(".");  
            } else if ("\\^$.|?+()[]{}NewLine".indexOf(c) != -1) {
                sb.append("\\").append(c);
            } else {
                sb.append(c);
            }
        }

        sb.append("$"); 

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static int countMatchingFiles(File currentDir, List<Pattern> patterns) {
        int count = 0;
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countMatchingFiles(file, patterns);
                } else if (isFileMatchesPatterns(file, patterns)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void processRecursive(File baseSourceDir, File currentSourceDir, File baseTargetDir, List<Pattern> patterns, int opType, boolean keepEmptyDirs, OnFileOperationListener listener) throws IOException {
        File[] files = currentSourceDir.listFiles();
        if (files == null) return;

        if (files.length == 0 && keepEmptyDirs && opType != OP_DELETE) {
            String relativePath = currentSourceDir.getAbsolutePath().substring(baseSourceDir.getAbsolutePath().length());
            File emptyTargetDir = new File(baseTargetDir, relativePath);
            if (!emptyTargetDir.exists()) {
                emptyTargetDir.mkdirs();
                listener.onLogReceived("Создана пустая папка структуры: " + relativePath);
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (keepEmptyDirs && opType != OP_DELETE) {
                    String relativePath = file.getAbsolutePath().substring(baseSourceDir.getAbsolutePath().length());
                    File targetSubDir = new File(baseTargetDir, relativePath);
                    if (!targetSubDir.exists()) {
                        targetSubDir.mkdirs();
                    }
                }
                processRecursive(baseSourceDir, file, baseTargetDir, patterns, opType, keepEmptyDirs, listener);
// Блок 3 (строки 141-210)
            } else if (isFileMatchesPatterns(file, patterns)) {

                String relativePathWithFile = file.getAbsolutePath().substring(baseSourceDir.getAbsolutePath().length());

                if (opType == OP_DELETE) {
                    if (file.delete()) {
                        totalFilesProcessed++;
                        sendProgress(progressPercent(), file.getName(), "Удален: " + relativePathWithFile, listener);
                    }
                } else {
                    File destFile = new File(baseTargetDir, relativePathWithFile);

                    if (destFile.exists()) {
                        destFile = resolveCollision(destFile);
                    }

                    File destParent = destFile.getParentFile();
                    if (destParent != null && !destParent.exists()) {
                        destParent.mkdirs();
                    }

                    copyFileUsingChannel(file, destFile);

                    if (opType == OP_MOVE) {
                        file.delete();
                    }

                    totalFilesProcessed++;
                    String actionName = (opType == OP_MOVE) ? "Перемещен: " : "Скопирован: ";
                    sendProgress(progressPercent(), file.getName(), actionName + relativePathWithFile, listener);
                }
            }
        }
    }

    private static boolean isFileMatchesPatterns(File file, List<Pattern> patterns) {
        String name = file.getName(); 

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                return true; 
            }
        }
        return false;
    }

    private static File resolveCollision(File destFile) {
        File parent = destFile.getParentFile();
        String name = destFile.getName();
        String baseName = name;
        String ext = "";

        int dotIdx = name.lastIndexOf('.');
        if (dotIdx != -1) {
            baseName = name.substring(0, dotIdx);
            ext = name.substring(dotIdx);
        }

        int counter = 1;
        File newFile = destFile;
        while (newFile.exists()) {
            newFile = new File(parent, baseName + "_" + counter + ext);
            counter++;
        }
        return newFile;
    }
// Блок 4 (строки 211-238)
    private static int progressPercent() {
        if (totalFilesCount == 0) return 100;
        return (int) (((float) totalFilesProcessed / totalFilesCount) * 100);
    }

    private static void sendProgress(int progress, String file, String log, OnFileOperationListener listener) {
        listener.onProgressUpdated(progress, file);
        listener.onLogReceived(log);
    }

    private static void copyFileUsingChannel(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null) sourceChannel.close();
            if (destChannel != null) destChannel.close();
        }
    }
}

