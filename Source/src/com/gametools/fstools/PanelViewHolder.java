package com.gametools.fstools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PanelViewHolder {
    private final View rootView;
    private final ListView listView;
    private final List<File> filesList;
    private final FileListAdapter adapter;
    private File currentDir;
    private final OnPathChangedListener pathListener;

    // Множество для хранения файлов и папок, выделенных вручную через чекбоксы
    private final Set<File> selectedFiles = new HashSet<File>();

    // Интерфейс для связи панели с MainActivity
    public interface OnPathChangedListener {
        void onPathChanged(File newPath);
        void onPanelFocused(PanelViewHolder panel);
    }

    public PanelViewHolder(Context context, File initialDir, final OnPathChangedListener listener) {
        this.pathListener = listener;
        this.currentDir = initialDir.exists() ? initialDir : new File("/sdcard");
        this.filesList = new ArrayList<File>();

        // Инфлейтим разметку проводника из шаблона file_picker.xml
        rootView = LayoutInflater.from(context).inflate(R.layout.file_picker, null);
        listView = (ListView) rootView.findViewById(R.id.listViewFiles);

        // Инициализируем адаптер, передавая туда список файлов и хранилище выделения
        adapter = new FileListAdapter(context, filesList, selectedFiles);
        listView.setAdapter(adapter);

        setupListClick();
        refreshDir();

        // Передача фокуса активной панели при клике или скролле списка
        listView.setOnTouchListener(new android.view.View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, android.view.MotionEvent event) {
					if (listener != null) {
						listener.onPanelFocused(PanelViewHolder.this);
					}
					return false;
				}
			});
    }

    // Возвращает корневую разметку панели для добавления во ViewPager или FrameLayout
    public View getRootView() { 
        return rootView; 
    }

    // Возвращает текущую директорию данной панели
    public File getCurrentDir() { 
        return currentDir; 
    }

    // Возвращает список элементов, выделенных чекбоксами
    public Set<File> getSelectedFiles() { 
        return selectedFiles; 
    }

    // Очистить ручное выделение элементов
    public void clearSelection() { 
        selectedFiles.clear(); 
        adapter.notifyDataSetChanged(); 
    }

    // Метод обновления списка файлов и папок с умной сортировкой
    public void refreshDir() {
        filesList.clear();
        selectedFiles.clear(); // Полностью сбрасываем старое выделение при переходе в другую папку

        File[] files = currentDir.listFiles();
        List<File> dirs = new ArrayList<File>();
        List<File> fls = new ArrayList<File>();

        // Разделяем элементы на две группы для раздельной сортировки
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    dirs.add(file);
                } else {
                    fls.add(file);
                }
            }
        }

        // Стандартный компаратор для сортировки по алфавиту без учета регистра
        Comparator<File> alphabetComparator = new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        };

        // Сортируем папки и файлы по отдельности
        Collections.sort(dirs, alphabetComparator);
        Collections.sort(fls, alphabetComparator);

        // Формируем результирующий список элементов для отображения
        // 1. Ссылка на шаг назад, если у текущей папки есть родительский каталог
        if (currentDir.getParentFile() != null) {
            filesList.add(new File(currentDir, ".."));
        }

        // 2. Все отсортированные папки (вверху списка)
        filesList.addAll(dirs);

        // 3. Все отсортированные файлы (строго под папками)
        filesList.addAll(fls);

        adapter.notifyDataSetChanged();
        listView.setSelection(0); // Скроллим список в самый верх

        if (pathListener != null) {
            pathListener.onPathChanged(currentDir);
        }
    }

    // Обработка стандартных одиночных кликов по элементам списка для навигации
    private void setupListClick() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					File selected = filesList.get(position);

					if (selected.getName().equals("..")) {
						// Переход вверх по дереву каталогов
						currentDir = currentDir.getParentFile();
						refreshDir();
					} else if (selected.isDirectory()) {
						// Вход внутрь выбранной директории
						currentDir = selected;
						refreshDir();
					}
				}
			});
    }
}

