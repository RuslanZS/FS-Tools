package com.gametools.fstools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.List;
import java.util.Set;

public class FileListAdapter extends BaseAdapter {

    private final Context context;
    private final List<File> filesList;
    private final Set<File> selectedFiles;

    // Конструктор принимает контекст, список элементов текущей папки и ссылку на множество выделенных файлов
    public FileListAdapter(Context context, List<File> filesList, Set<File> selectedFiles) {
        this.context = context;
        this.filesList = filesList;
        this.selectedFiles = selectedFiles;
    }

    @Override
    public int getCount() {
        return filesList != null ? filesList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return filesList != null ? filesList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Оптимизированный паттерн ViewHolder для быстрой прокрутки списков без подвисаний на старых API
    private static class ViewHolder {
        LinearLayout layoutSelectZone;
        LinearLayout layoutContentZone;
        CheckBox cbSelect;
        TextView tvFileName;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Надуваем макет строки из вашего Шаблона 2 (используя правильный ID file_picker_item)
            convertView = LayoutInflater.from(context).inflate(R.layout.file_picker_item, parent, false);

            holder = new ViewHolder();
            holder.layoutSelectZone = (LinearLayout) convertView.findViewById(R.id.layoutSelectZone);
            holder.layoutContentZone = (LinearLayout) convertView.findViewById(R.id.layoutContentZone);
            holder.cbSelect = (CheckBox) convertView.findViewById(R.id.cbSelect);
            holder.tvFileName = (TextView) convertView.findViewById(R.id.tvFileName);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final File currentFile = filesList.get(position);

        // Логика отображения элемента "Назад" (..)
        if (currentFile.getName().equals("..")) {
            holder.tvFileName.setText(R.string.folder_up); // Подгружаем текст из strings.xml
            holder.layoutSelectZone.setVisibility(View.GONE); // Скрываем чекбокс для системной строки ".."

            // Чтобы клик по всей строке ".." возвращал назад, дублируем его на контентную зону
            holder.layoutContentZone.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Эмулируем клик по элементу ListView для навигации вверх
						if (parent instanceof android.widget.ListView) {
							android.widget.ListView listView = (android.widget.ListView) parent;
							if (listView.getOnItemClickListener() != null) {
								listView.getOnItemClickListener().onItemClick(listView, v, position, listView.getItemIdAtPosition(position));
							}
						}
					}
				});
        } else {
            // Отображение стандартного файла или папки
            holder.tvFileName.setText(currentFile.getName());
            holder.layoutSelectZone.setVisibility(View.VISIBLE); // Возвращаем видимость чекбокса

            // Синхронизируем визуальное состояние чекбокса с нашей коллекцией выделения
            holder.cbSelect.setChecked(selectedFiles.contains(currentFile));

            // ОБРАБОТКА КЛИКА ПО ЛЕВОЙ ЗОНЕ (Выделение чекбоксом)
            holder.layoutSelectZone.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selectedFiles.contains(currentFile)) {
							selectedFiles.remove(currentFile);
						} else {
							selectedFiles.add(currentFile);
						}
						// Моментально обновляем интерфейс, чтобы галочка переключилась
						notifyDataSetChanged();
					}
				});

            // ОБРАБОТКА КЛИКА ПО ПРАВОЙ ЗОНЕ (Вход в папку)
            holder.layoutContentZone.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Перенаправляем клик в стандартный обработчик OnItemClickListener внутри PanelViewHolder
						if (parent instanceof android.widget.ListView) {
							android.widget.ListView listView = (android.widget.ListView) parent;
							if (listView.getOnItemClickListener() != null) {
								listView.getOnItemClickListener().onItemClick(listView, v, position, listView.getItemIdAtPosition(position));
							}
						}
					}
				});
        }

        return convertView;
    }
}

