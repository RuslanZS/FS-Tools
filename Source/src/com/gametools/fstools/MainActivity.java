// Блок 1 (строки 1-70)
package com.gametools.fstools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 201;

    private ViewPager viewPager;
    private PanelViewHolder leftPanel;
    private PanelViewHolder rightPanel;
    private PanelViewHolder activePanel;

    private TextView tvLeftHeader, tvRightHeader, tvStatus, tvConsoleLog;
    private Button btnCopy, btnMove, btnDelete;
    private LinearLayout layoutProgress;
    private ProgressBar progressBar;
    private AutoCompleteTextView etExtension;

    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvLeftHeader = (TextView) findViewById(R.id.tvLeftPathHeader);
        tvRightHeader = (TextView) findViewById(R.id.tvRightPathHeader);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        
        tvConsoleLog = new TextView(this); 

        layoutProgress = (LinearLayout) findViewById(R.id.layoutProgressBlock);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnCopy = (Button) findViewById(R.id.btnTcCopy);
        btnMove = (Button) findViewById(R.id.btnTcMove);
        btnDelete = (Button) findViewById(R.id.btnTcDelete);
        
        etExtension = new AutoCompleteTextView(this);

        setupUiHandler();
        buildPanelsLogic();
        setupTcOperations();
        checkStoragePermissions();
    }
// Блок 2 (строки 71-140)
    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            String writePermission = "android.permission.WRITE_EXTERNAL_STORAGE";
            String readPermission = "android.permission.READ_EXTERNAL_STORAGE";
            if (checkSelfPermission(writePermission) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(readPermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{writePermission, readPermission}, REQ_PERMISSIONS);
            }
        }
    }

    private void buildPanelsLogic() {
        PanelViewHolder.OnPathChangedListener panelListener = new PanelViewHolder.OnPathChangedListener() {
            @Override
            public void onPathChanged(File newPath) {
                if (leftPanel != null && newPath.getAbsolutePath().equals(leftPanel.getCurrentDir().getAbsolutePath())) {
                    tvLeftHeader.setText("Левая: " + newPath.getAbsolutePath());
                } else if (rightPanel != null) {
                    tvRightHeader.setText("Правая: " + newPath.getAbsolutePath());
                }
            }

            @Override
            public void onPanelFocused(PanelViewHolder panel) {
                activePanel = panel;
                tvLeftHeader.setTextColor(activePanel == leftPanel ? 0xFFFFFFFF : 0xFF555555);
                tvRightHeader.setTextColor(activePanel == rightPanel ? 0xFFFFFFFF : 0xFF555555);
            }
        };

        leftPanel = new PanelViewHolder(this, new File("/sdcard"), panelListener);
        rightPanel = new PanelViewHolder(this, new File("/sdcard/Download"), panelListener);
        activePanel = leftPanel;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FrameLayout containerLeft = (FrameLayout) findViewById(R.id.containerLeftPanel);
            FrameLayout containerRight = (FrameLayout) findViewById(R.id.containerRightPanel);
            if (containerLeft != null && containerRight != null) {
                containerLeft.addView(leftPanel.getRootView());
                containerRight.addView(rightPanel.getRootView());
            }
        } else {
            viewPager = (ViewPager) findViewById(R.id.panelViewPager);
            if (viewPager != null) {
                PagerAdapter panelAdapter = new PagerAdapter() {
                    @Override public int getCount() { return 2; }
                    @Override public boolean isViewFromObject(View v, Object o) { return v == o; }
                    @Override public Object instantiateItem(ViewGroup c, int p) {
                        View v = (p == 0) ? leftPanel.getRootView() : rightPanel.getRootView();
                        c.addView(v); return v;
                    }
                    @Override public void destroyItem(ViewGroup c, int p, Object o) { c.removeView((View) o); }
                };
                viewPager.setAdapter(panelAdapter);
                viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        activePanel = (position == 0) ? leftPanel : rightPanel;
                        tvLeftHeader.setTextColor(position == 0 ? 0xFFFFFFFF : 0xFF555555);
                        tvRightHeader.setTextColor(position == 1 ? 0xFFFFFFFF : 0xFF555555);
                    }
                });
            }
        }
    }
// Блок 3 (строки 141-210)
    private void setupTcOperations() {
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PanelViewHolder sourcePanel = activePanel;
                PanelViewHolder targetPanel = (activePanel == leftPanel) ? rightPanel : leftPanel;
                showTcDialog(FileCoreEngine.OP_COPY, sourcePanel, targetPanel);
            }
        });

        btnMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PanelViewHolder sourcePanel = activePanel;
                PanelViewHolder targetPanel = (activePanel == leftPanel) ? rightPanel : leftPanel;
                showTcDialog(FileCoreEngine.OP_MOVE, sourcePanel, targetPanel);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTcDialog(FileCoreEngine.OP_DELETE, activePanel, null);
            }
        });
    }

    private void showTcDialog(final int opType, final PanelViewHolder srcPanel, final PanelViewHolder dstPanel) {
        final java.util.Set<File> selectedFiles = srcPanel.getSelectedFiles();
        final boolean hasSelection = !selectedFiles.isEmpty();
        
        final String srcPath = srcPanel.getCurrentDir().getAbsolutePath();
        final String dstPath = (dstPanel != null) ? dstPanel.getCurrentDir().getAbsolutePath() : "";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        String opName = "Копировать";
        if (opType == FileCoreEngine.OP_MOVE) opName = "Переместить";
        if (opType == FileCoreEngine.OP_DELETE) opName = "Удалить";

        if (hasSelection) {
            builder.setTitle(opName + " из выделенных элементов (" + selectedFiles.size() + " шт.) в:");
        } else {
            builder.setTitle(opName + " из \"" + srcPanel.getCurrentDir().getName() + "\":");
        }

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(25, 25, 25, 25);

        final EditText etTargetInput = new EditText(this);
        if (opType != FileCoreEngine.OP_DELETE) {
            etTargetInput.setText(dstPath);
            dialogLayout.addView(etTargetInput);
        }
// Блок 4 (строки 211-280)
        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText("Только файлы типа (маска):");
        tvTypeLabel.setPadding(0, 15, 0, 5);
        dialogLayout.addView(tvTypeLabel);

        final AutoCompleteTextView etMaskInput = new AutoCompleteTextView(this);
        etMaskInput.setText("*.*");
        
        List<String> historyList = ExtensionHistory.getHistory(this);
        ArrayAdapter<String> historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, historyList);
        etMaskInput.setAdapter(historyAdapter);
        etMaskInput.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { etMaskInput.showDropDown(); }
        });
        dialogLayout.addView(etMaskInput);

        final CheckBox cbIncludeEmptyDirs = new CheckBox(this);
        if (opType != FileCoreEngine.OP_DELETE) {
            cbIncludeEmptyDirs.setText("Копировать пустые подпапки");
            cbIncludeEmptyDirs.setChecked(false);
            cbIncludeEmptyDirs.setPadding(0, 15, 0, 0);
            dialogLayout.addView(cbIncludeEmptyDirs);
        }

        builder.setView(dialogLayout);

        builder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String finalTarget = etTargetInput.getText().toString().trim();
                String finalMask = etMaskInput.getText().toString().trim();
                boolean keepEmptyDirs = cbIncludeEmptyDirs.isChecked();

                if (finalMask.length() > 0 && !finalMask.equals("*.*") && !finalMask.equals("*")) {
                    ExtensionHistory.addExtension(MainActivity.this, finalMask);
                }

                if (hasSelection) {
                    if (opType == FileCoreEngine.OP_DELETE) {
                        runThreadedGroupDelete(selectedFiles, finalMask);
                    } else {
                        runThreadedGroupOperation(opType, selectedFiles, finalTarget, finalMask, keepEmptyDirs);
                    }
                } else {
                    executeFileEngine(opType, srcPath, finalTarget, finalMask, keepEmptyDirs);
                }
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
// Блок 5 (строки 281-355)
    private void executeFileEngine(final int opType, final String src, final String dst, final String mask, final boolean keepEmptyDirs) {
        layoutProgress.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileCoreEngine.startOperation(src, dst, mask, opType, keepEmptyDirs, new FileCoreEngine.OnFileOperationListener() {
                    @Override public void onLogReceived(String msg) { uiHandler.sendMessage(uiHandler.obtainMessage(1, msg)); }
                    @Override public void onProgressUpdated(int progress, String currentFile) { uiHandler.sendMessage(uiHandler.obtainMessage(2, progress, 0, currentFile)); }
                    @Override public void onOperationFinished(boolean success, String errorMsg) { uiHandler.sendMessage(uiHandler.obtainMessage(3, success ? 1 : 0, 0, errorMsg)); }
                });
            }
        }).start();
    }

    private void runThreadedGroupOperation(final int opType, final java.util.Set<File> files, final String targetPath, final String mask, final boolean keepEmptyDirs) {
        layoutProgress.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> paths = new ArrayList<String>();
                for (File f : files) { paths.add(f.getAbsolutePath()); }

                Packer.copySelectedItems(paths, targetPath, mask, keepEmptyDirs, opType, new Packer.OnPackListener() {
                    @Override public void onLogReceived(String msg) { uiHandler.sendMessage(uiHandler.obtainMessage(1, msg)); }
                    @Override public void onProgressUpdated(int p, String f) { uiHandler.sendMessage(uiHandler.obtainMessage(2, p, 0, f)); }
                    @Override public void onPackFinished(boolean s, String err) { uiHandler.sendMessage(uiHandler.obtainMessage(3, s ? 1 : 0, 0, err)); }
                });
            }
        }).start();
    }

    private void runThreadedGroupDelete(final java.util.Set<File> files, final String mask) {
        layoutProgress.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                // Преобразуем коллекцию выделенных папок в массив для последовательного удаления
                List<String> paths = new ArrayList<String>();
                for (File f : files) { paths.add(f.getAbsolutePath()); }
                
                // Для удаления по маске внутри конкретных папок переиспользуем FileCoreEngine
                for (String path : paths) {
                    FileCoreEngine.startOperation(path, null, mask, FileCoreEngine.OP_DELETE, false, new FileCoreEngine.OnFileOperationListener() {
                        @Override public void onLogReceived(String msg) { uiHandler.sendMessage(uiHandler.obtainMessage(1, msg)); }
                        @Override public void onProgressUpdated(int progress, String currentFile) { }
                        @Override public void onOperationFinished(boolean success, String errorMsg) { }
                    });
                    count++;
                    int progress = (int) (((float) count / paths.size()) * 100);
                    uiHandler.sendMessage(uiHandler.obtainMessage(2, progress, 0, "Очистка папок..."));
                }
                uiHandler.sendMessage(uiHandler.obtainMessage(3, 1, 0, null));
            }
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnCopy.setEnabled(enabled);
        btnMove.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
    }

    private void setupUiHandler() {
        uiHandler = new Handler(android.os.Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1: break;
                    case 2:
                        progressBar.setProgress(msg.arg1);
                        tvStatus.setText("Обработка: " + msg.obj);
                        break;
                    case 3:
                        layoutProgress.setVisibility(View.GONE);
                        setButtonsEnabled(true);
                        tvStatus.setText("Ожидание...");
                        if (leftPanel != null) leftPanel.refreshDir();
                        if (rightPanel != null) rightPanel.refreshDir();
                        
                        if (msg.arg1 == 1) {
                            Toast.makeText(MainActivity.this, "Операция завершена!", Toast.LENGTH_SHORT).show();
                        } else if (msg.obj != null) {
                            Toast.makeText(MainActivity.this, "Ошибка: " + msg.obj, Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        };
    }
}
