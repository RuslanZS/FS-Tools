package com.gametools.fstools;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class ExtensionHistory {
    private static final String PREFS_NAME = "tc_explorer_prefs";
    private static final String KEY_HISTORY = "extension_history";
    private static final int MAX_HISTORY_SIZE = 10; // Ограничение списка до 10 последних масок

    /**
     * Добавление нового расширения или маски в историю SharedPreferences
     * @param context Системный контекст приложения
     * @param extInput Строка маски, введенная пользователем (например, "png" или "*.3ds")
     */
    public static void addExtension(Context context, String extInput) {
        String cleanInput = extInput.trim();
        if (cleanInput.length() == 0) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<String> history = getHistory(context);

        // Если такая маска уже использовалась ранее, удаляем её, 
        // чтобы при повторном добавлении она поднялась в самый верх списка
        if (history.contains(cleanInput)) {
            history.remove(cleanInput);
        }

        // Вставляем новую маску на первое место (индекс 0)
        history.add(0, cleanInput);

        // Если история превысила лимит, отсекаем старые элементы
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        // Сериализуем список строк в единую строку через разделитель ";" для сохранения
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append(";");
            }
        }

        // Сохраняем данные в память устройства (метод commit() гарантирует запись на API 1+)
        prefs.edit().putString(KEY_HISTORY, sb.toString()).commit();
    }

    /**
     * Загрузка списка сохраненных масок расширений из памяти устройства
     * @param context Системный контекст приложения
     * @return Список строк (масок файлов), где на первом месте стоит последняя использованная
     */
    public static List<String> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // По умолчанию, если утилита запущена впервые, в списке всегда будет универсальный фильтр "*.*"
        String savedRaw = prefs.getString(KEY_HISTORY, "*.*"); 

        String[] tokens = savedRaw.split(";");
        List<String> list = new ArrayList<String>();

        for (String t : tokens) {
            if (t.trim().length() > 0) {
                list.add(t.trim());
            }
        }

        // Гарантируем, что базовый фильтр "*.*" всегда присутствует в списке истории
        if (!list.contains("*.*")) {
            list.add("*.*");
        }

        return list;
    }
}

