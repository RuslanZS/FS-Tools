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

public class Packer {

    public interface OnPackListener {
        void onLogReceived(String msg);
        void onProgressUpdated(int progress, String currentFile);
        void onPackFinished(boolean success, String errorMsg);
    }

    public static void copySelectedItems(List<String> selectedPaths, String targetBasePath, String maskInput, boolean keepEmptyDirs, int opType, OnPackListener listener) {
        if (selectedPaths == null || selectedPaths.isEmpty()) {
            listener.onPackFinished(false, "Список выделенных элементов пуст");
            return;
        }

        // Разбираем маску ввода (поддерживает усложненные маски и списки через запятую)
        String[] rawTokens = maskInput.split(",");
        List<Pattern> compiledPatterns = new ArrayList<Pattern>();
        for (String token : rawTokens) {
            String trimmed = token.trim();
            if (trimmed.length() > 0) {
                compiledPatterns.add(compileMaskToPattern(trimmed));
            }
        }
        if (compiledPatterns.isEmpty()) {
            compiledPatterns.add(compileMaskToPattern("*.*"));
        }

        try {
            for (int i = 0; i < selectedPaths.size(); i++) {
                File item = new File(selectedPaths.get(i));
                if (!item.exists()) continue;

                if (item.isFile()) {
                    // Если это одиночный файл, проверяем, подходит ли он под маску
                    if (isFileMatchesPatterns(item, compiledPatterns)) {
                        File destFile = new File(targetBasePath, item.getName());
                        copyFileUsingChannel(item, destFile);
                        if (opType == 2) item.delete(); // Если это перемещение (OP_MOVE = 2)
                        listener.onLogReceived("Обработан файл: " + item.getName());
                    }
                } else if (item.isDirectory()) {
                    // Если это выделенная галочкой папка (например, Materials)
                    File baseSourceDir = item.getParentFile(); 
                    File targetDir = new File(targetBasePath);
                    
                    listener.onLogReceived("Фильтрация папки " + item.getName() + " по маске: " + maskInput);
                    copyDirStructureRecursive(baseSourceDir, item, targetDir, compiledPatterns, keepEmptyDirs, opType, listener);
                }
                
                int progress = (int) (((float) (i + 1) / selectedPaths.size()) * 100);
                listener.onProgressUpdated(progress, item.getName());
            }
            listener.onPackFinished(true, null);
        } catch (IOException e) {
            e.printStackTrace();
            listener.onPackFinished(false, e.getMessage());
        }
    }

    private static void copyDirStructureRecursive(File baseSourceDir, File currentSourceDir, File baseTargetDir, List<Pattern> patterns, boolean keepEmptyDirs, int opType, OnPackListener listener) throws IOException {
        File[] files = currentSourceDir.listFiles();
        if (files == null) return;

        // Если подпапка пустая и включен флаг сохранения структуры пустых подпапок
        if (files.length == 0 && keepEmptyDirs) {
            String relativePath = currentSourceDir.getAbsolutePath().substring(baseSourceDir.getAbsolutePath().length());
            File emptyTarget = new File(baseTargetDir, relativePath);
            if (!emptyTarget.exists()) emptyTarget.mkdirs();
            return;
        }

        for (File file : files) {
            String relativePath = file.getAbsolutePath().substring(baseSourceDir.getAbsolutePath().length());
            File destTarget = new File(baseTargetDir, relativePath);

            if (file.isDirectory()) {
                // Если сохраняем структуру пустых папок, создаем директорию превентивно перед входом
                if (keepEmptyDirs) {
                    if (!destTarget.exists()) destTarget.mkdirs();
                }
                copyDirStructureRecursive(baseSourceDir, file, baseTargetDir, patterns, keepEmptyDirs, opType, listener);
            } else if (file.isFile() && isFileMatchesPatterns(file, patterns)) {
                // Файл подошел под маску — создаем для него структуру родительских папок на лету
                File destParent = destTarget.getParentFile();
                if (destParent != null && !destParent.exists()) {
                    destParent.mkdirs();
                }
                
                copyFileUsingChannel(file, destTarget);
                if (opType == 2) file.delete(); // Удаляем оригинал при перемещении
                listener.onLogReceived("Скопирован по маске: " + relativePath);
            }
        }
    }

    // Компилятор масок в RegEx с игнорированием регистра символов
    private static Pattern compileMaskToPattern(String mask) {
        String cleanMask = mask.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        for (int i = 0; i < cleanMask.length(); i++) {
            char c = cleanMask.charAt(i);
            if (c == '*') sb.append(".*");
            else if (c == '?') sb.append(".");
            else if ("\\^$.|?+()[]{}NewLine".indexOf(c) != -1) sb.append("\\").append(c);
            else sb.append(c);
        }
        sb.append("$");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static boolean isFileMatchesPatterns(File file, List<Pattern> patterns) {
        String name = file.getName();
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) return true;
        }
        return false;
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
