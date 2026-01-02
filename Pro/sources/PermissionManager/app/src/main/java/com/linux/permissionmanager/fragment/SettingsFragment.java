package com.linux.permissionmanager.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.linux.permissionmanager.R;
import com.linux.permissionmanager.ActivityResultId;
import com.linux.permissionmanager.AppSettings;
import com.linux.permissionmanager.MainActivity;
import com.linux.permissionmanager.bridge.NativeBridge;
import com.linux.permissionmanager.model.AppUpdateInfo;
import com.linux.permissionmanager.update.AppUpdateManager;
import com.linux.permissionmanager.utils.DialogUtils;
import com.linux.permissionmanager.utils.UrlIntentUtils;
import com.linux.permissionmanager.utils.ThemeUtils;

public class SettingsFragment extends Fragment {
    private Activity mActivity;
    private String mRootKey = "";

    private MaterialSwitch mCkboxEnableBootFailProtect;
    private Button mBtnTestSkrootBasics;
    private Button mBtnTestSkrootDefaultModule;
    private MaterialSwitch mCkboxEnableSkrootLog;
    private Button mBtnShowSkrootLog;
    private TextView mTvAboutVer;
    private TextView mTvLink;

    private Button mBtnSetBg;
    private Button mBtnPickBg;
    private Button mBtnClearBg;
    private TextView mTvBgMusicStatus;
    private Button mBtnPickBgMusic;
    private Button mBtnClearBgMusic;
    private MaterialSwitch mSwitchShowLyrics;
    private MaterialSwitch mSwitchAdaptiveBg;
    private TextView mTvLyricFileStatus;
    private Button mBtnPickLyricFile;
    private Button mBtnClearLyricFile;
    private TextView mTvLogSavePath;
    private Button mBtnPickLogPath;
    private Button mBtnClearLogPath;
    private com.google.android.material.slider.Slider mCardAlphaSlider;
    private TextView mTvCardAlphaValue;
    private com.google.android.material.slider.Slider mBgAlphaSlider;
    private TextView mTvBgAlphaValue;
    private LinearLayout mThemeColorsContainer;

    // Update component
    private LinearLayout mTvUpdateBlock;
    private TextView mTvUpdateFound;
    private TextView mTvUpdateChangelog;
    private Button mTvUpdateDownload;
    private AppUpdateManager mUpdateManager;

    public SettingsFragment(Activity activity) {
        mActivity = activity;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCkboxEnableBootFailProtect = view.findViewById(R.id.enable_boot_fail_protect_ckbox);
        mBtnTestSkrootBasics = view.findViewById(R.id.test_skroot_basics_btn);
        mBtnTestSkrootDefaultModule = view.findViewById(R.id.test_skroot_default_module_btn);
        mCkboxEnableSkrootLog = view.findViewById(R.id.enable_skroot_log_ckbox);
        mBtnShowSkrootLog = view.findViewById(R.id.show_skroot_log_btn);
        mTvAboutVer = view.findViewById(R.id.about_ver_tv);
        mTvLink = view.findViewById(R.id.link_tv);

        mBtnSetBg = view.findViewById(R.id.set_bg_url_btn);
        mBtnPickBg = view.findViewById(R.id.pick_bg_file_btn);
        mBtnClearBg = view.findViewById(R.id.clear_bg_btn);
        mTvBgMusicStatus = view.findViewById(R.id.bg_music_status_tv);
        mBtnPickBgMusic = view.findViewById(R.id.pick_bg_music_btn);
        mBtnClearBgMusic = view.findViewById(R.id.clear_bg_music_btn);
        mSwitchShowLyrics = view.findViewById(R.id.show_lyrics_switch);
        mSwitchAdaptiveBg = view.findViewById(R.id.adaptive_bg_switch);
        mTvLyricFileStatus = view.findViewById(R.id.lyric_file_status_tv);
        mBtnPickLyricFile = view.findViewById(R.id.pick_lyric_file_btn);
        mBtnClearLyricFile = view.findViewById(R.id.clear_lyric_file_btn);
        mTvLogSavePath = view.findViewById(R.id.log_save_path_tv);
        mBtnPickLogPath = view.findViewById(R.id.pick_log_path_btn);
        mBtnClearLogPath = view.findViewById(R.id.clear_log_path_btn);
        mCardAlphaSlider = view.findViewById(R.id.card_alpha_slider);
        mTvCardAlphaValue = view.findViewById(R.id.card_alpha_value_tv);
        mBgAlphaSlider = view.findViewById(R.id.bg_alpha_slider);
        mTvBgAlphaValue = view.findViewById(R.id.bg_alpha_value_tv);
        mThemeColorsContainer = view.findViewById(R.id.theme_colors_container);

        // Update component
        mTvUpdateBlock = view.findViewById(R.id.core_update_block);
        mTvUpdateFound = view.findViewById(R.id.core_update_found_tv);
        mTvUpdateChangelog = view.findViewById(R.id.core_update_changelog_tv);
        mTvUpdateDownload = view.findViewById(R.id.core_update_download_tv);
        initSettingsControl();
    }

    public void setRootKey(String rootKey) {
        mRootKey = rootKey;
    }

    private void initSettingsControl() {
        mCkboxEnableBootFailProtect.setChecked(NativeBridge.isBootFailProtectEnabled(mRootKey));
        mCkboxEnableBootFailProtect.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    String tip = NativeBridge.setBootFailProtectEnabled(mRootKey, isChecked);
                    DialogUtils.showMsgDlg(mActivity, "执行结果", tip, null);
                }
        );
        mBtnTestSkrootBasics.setOnClickListener((v) -> showSelectTestSkrootBasicsDlg());
        mBtnTestSkrootDefaultModule.setOnClickListener((v) -> showSelectTestDefaultModuleDlg());

        mCkboxEnableSkrootLog.setChecked(NativeBridge.isSkrootLogEnabled(mRootKey));
        mCkboxEnableSkrootLog.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    String tip = NativeBridge.setSkrootLogEnabled(mRootKey, isChecked);
                    DialogUtils.showMsgDlg(mActivity, "执行结果", tip, null);
                }
        );
        mBtnShowSkrootLog.setOnClickListener(v -> showSkrootLogDlg());
        
        mBtnSetBg.setOnClickListener(v -> showSetBgDlg());
        mBtnPickBg.setOnClickListener(v -> pickBgFile());
        mBtnClearBg.setOnClickListener(v -> {
            AppSettings.setString("background_path", "");
            if (mActivity instanceof MainActivity) {
                ((MainActivity) mActivity).updateBackground();
            }
        });

        mBtnPickBgMusic.setOnClickListener(v -> pickBgMusic());
        mBtnClearBgMusic.setOnClickListener(v -> {
            AppSettings.setString("background_music_uri", "");
            com.linux.permissionmanager.utils.BackgroundMusicManager.getInstance(mActivity).stop();
            updateBgMusicStatus();
        });
        updateBgMusicStatus();

        mSwitchShowLyrics.setChecked(AppSettings.getBoolean("show_lyrics", false));
        mSwitchShowLyrics.setOnCheckedChangeListener((v, isChecked) -> {
            AppSettings.setBoolean("show_lyrics", isChecked);
        });

        mSwitchAdaptiveBg.setChecked(AppSettings.getBoolean("adaptive_background", false));
        mSwitchAdaptiveBg.setOnCheckedChangeListener((v, isChecked) -> {
            AppSettings.setBoolean("adaptive_background", isChecked);
            if (mActivity instanceof MainActivity) {
                ((MainActivity) mActivity).updateBackground();
            }
        });

        mBtnPickLyricFile.setOnClickListener(v -> pickLyricFile());
        mBtnClearLyricFile.setOnClickListener(v -> {
            AppSettings.setString("lyric_file_uri", "");
            updateLyricFileStatus();
        });
        updateLyricFileStatus();

        mBtnPickLogPath.setOnClickListener(v -> pickLogPath());
        mBtnClearLogPath.setOnClickListener(v -> {
            AppSettings.setString("log_save_path", "");
            updateLogPathStatus();
        });
        updateLogPathStatus();

        float currentAlpha = AppSettings.getFloat("card_alpha", 1.0f);
        mCardAlphaSlider.setValue(currentAlpha);
        mTvCardAlphaValue.setText(String.format("%.2f", currentAlpha));
        mCardAlphaSlider.addOnChangeListener((slider, value, fromUser) -> {
            AppSettings.setFloat("card_alpha", value);
            mTvCardAlphaValue.setText(String.format("%.2f", value));
            updateAllCardsAlpha(getView());
        });

        float currentBgAlpha = AppSettings.getFloat("background_alpha", 0.5f);
        mBgAlphaSlider.setValue(currentBgAlpha);
        mTvBgAlphaValue.setText(String.format("%.2f", currentBgAlpha));
        mBgAlphaSlider.addOnChangeListener((slider, value, fromUser) -> {
            AppSettings.setFloat("background_alpha", value);
            mTvBgAlphaValue.setText(String.format("%.2f", value));
            if (mActivity instanceof MainActivity) {
                ((MainActivity) mActivity).updateBackgroundAlpha();
            }
        });

        initThemeColorPicker();

        mUpdateManager = new AppUpdateManager(mActivity);
        initAboutText();
        initLink();
        initUpdateBlock();
    }

    private void initThemeColorPicker() {
        int[] colors = {
            Color.parseColor("#6750A4"), // Default Purple
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#607D8B"), // Blue Grey
            Color.parseColor("#3F51B5")  // Indigo
        };

        mThemeColorsContainer.removeAllViews();
        int currentColor = ThemeUtils.getThemeColor();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        for (int color : colors) {
            View colorView = new View(mActivity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);
            
            // Create a circular background with the color
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(color);
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            
            // Add a border if it's the current color
            if (color == currentColor) {
                gd.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()), 
                             Color.LTGRAY);
            }
            
            colorView.setBackground(gd);
            colorView.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
            
            colorView.setOnClickListener(v -> {
                ThemeUtils.setThemeColor(color);
                applyNewTheme(color);
            });
            
            mThemeColorsContainer.addView(colorView);
        }

        // Add custom color picker button
        View customColorView = new View(mActivity);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(size, size);
        customParams.setMargins(margin, margin, margin, margin);
        customColorView.setLayoutParams(customParams);

        android.graphics.drawable.GradientDrawable customGd = new android.graphics.drawable.GradientDrawable();
        customGd.setColor(Color.TRANSPARENT);
        customGd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        customGd.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), Color.GRAY);
        
        // Use a "+" icon or similar for custom color
         customColorView.setBackground(customGd);
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
             customColorView.setForeground(getResources().getDrawable(R.drawable.ic_add_white, null));
         }
        
        customColorView.setOnClickListener(v -> showCustomColorPickerDialog());
        mThemeColorsContainer.addView(customColorView);
    }

    private void applyNewTheme(int color) {
        initThemeColorPicker(); // Refresh to show selection
        if (mActivity instanceof MainActivity) {
            ThemeUtils.applyTheme(mActivity);
            ((MainActivity) mActivity).updateBackground();
            // Also need to refresh fragments
            refreshAllFragmentsTheme();
        }
    }

    private void showCustomColorPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("自定义主题色");
        
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_custom_color, null);
        EditText colorInput = view.findViewById(R.id.color_hex_input);
        View colorPreview = view.findViewById(R.id.color_preview);
        SeekBar seekBarR = view.findViewById(R.id.seekbar_r);
        SeekBar seekBarG = view.findViewById(R.id.seekbar_g);
        SeekBar seekBarB = view.findViewById(R.id.seekbar_b);
        
        int currentColor = ThemeUtils.getThemeColor();
        colorPreview.setBackgroundColor(currentColor);
        colorInput.setText(String.format("#%06X", (0xFFFFFF & currentColor)));
        
        seekBarR.setProgress(Color.red(currentColor));
        seekBarG.setProgress(Color.green(currentColor));
        seekBarB.setProgress(Color.blue(currentColor));
        
        final boolean[] isUpdating = {false};
        
        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isUpdating[0] = true;
                    int r = seekBarR.getProgress();
                    int g = seekBarG.getProgress();
                    int b = seekBarB.getProgress();
                    int newColor = Color.rgb(r, g, b);
                    colorPreview.setBackgroundColor(newColor);
                    colorInput.setText(String.format("#%06X", (0xFFFFFF & newColor)));
                    isUpdating[0] = false;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        
        seekBarR.setOnSeekBarChangeListener(seekBarListener);
        seekBarG.setOnSeekBarChangeListener(seekBarListener);
        seekBarB.setOnSeekBarChangeListener(seekBarListener);
        
        colorInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isUpdating[0]) {
                    try {
                        int color = Color.parseColor(s.toString());
                        isUpdating[0] = true;
                        colorPreview.setBackgroundColor(color);
                        seekBarR.setProgress(Color.red(color));
                        seekBarG.setProgress(Color.green(color));
                        seekBarB.setProgress(Color.blue(color));
                        isUpdating[0] = false;
                    } catch (Exception ignored) {}
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        builder.setView(view);
        builder.setPositiveButton("应用", (dialog, which) -> {
            String hex = colorInput.getText().toString();
            try {
                int color = Color.parseColor(hex);
                ThemeUtils.setThemeColor(color);
                applyNewTheme(color);
            } catch (Exception e) {
                DialogUtils.showMsgDlg(mActivity, "错误", "无效的颜色格式", null);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    public void refreshAllFragmentsTheme() {
        // This is a bit tricky, but since ThemeUtils.applyToViewTree works on any view
        // we can just apply it to the root view of the current fragment
        ThemeUtils.applyToViewTree(getView(), ThemeUtils.getThemeColor());
        // Also refresh color picker to show current selected color
        initThemeColorPicker();
    }

    private void showSelectTestSkrootBasicsDlg() {
        final String[] items = {"1.通道检查", "2.内核起始地址检查", "3.写入内存测试", "4.读取跳板测试", "5.写入跳板测试"};
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("请选择一个选项");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String item = "";
                if(which == 0) item = "Channel";
                else if(which == 1) item = "KernelBase";
                else if(which == 2) item = "WriteTest";
                else if(which == 3) item = "ReadTrampoline";
                else if(which == 4) item = "WriteTrampoline";
                String log = NativeBridge.testSkrootBasics(mRootKey, item);
                DialogUtils.showLogDialog(mActivity, log);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSelectTestDefaultModuleDlg() {
        final String[] items = {"1.ROOT 权限模块", "2.SU 重定向模块"};
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("请选择一个选项");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String defName = "";
                if(which == 0) defName = "RootBridge";
                else if(which == 1) defName = "SuRedirect";

                String log = NativeBridge.testSkrootDefaultModule(mRootKey, defName);
                DialogUtils.showLogDialog(mActivity, log);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSkrootLogDlg() {
        String log = NativeBridge.readSkrootLog(mRootKey);
        DialogUtils.showLogDialog(mActivity, log);
    }

    private void showSetBgDlg() {
        String currentBg = AppSettings.getString("background_path", "");
        Handler callback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String path = (String) msg.obj;
                if (path != null && !path.isEmpty()) {
                    AppSettings.setString("background_path", path);
                    if (mActivity instanceof MainActivity) {
                        ((MainActivity) mActivity).updateBackground();
                    }
                }
                super.handleMessage(msg);
            }
        };
        DialogUtils.showInputDlg(mActivity, currentBg, "设置背景图片", "请输入图片 URL 或本地路径", callback, null);
     }

     private void pickBgFile() {
         Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
         intent.setType("image/*");
         mActivity.startActivityForResult(intent, ActivityResultId.REQUEST_CODE_CHOOSE_BG_IMAGE);
     }

    private void pickBgMusic() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, ActivityResultId.REQUEST_CODE_CHOOSE_BG_MUSIC);
    }

    private void pickLyricFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, ActivityResultId.REQUEST_CODE_CHOOSE_LYRIC_FILE);
    }

    private void updateBgMusicStatus() {
        String uriStr = AppSettings.getString("background_music_uri", "");
        if (uriStr.isEmpty()) {
            mTvBgMusicStatus.setText("未设置");
        } else {
            try {
                Uri uri = Uri.parse(uriStr);
                String path = uri.getPath();
                if (path != null) {
                    int index = path.lastIndexOf('/');
                    if (index >= 0) {
                        path = path.substring(index + 1);
                    }
                    mTvBgMusicStatus.setText("已设置: " + Uri.decode(path));
                } else {
                    mTvBgMusicStatus.setText("已设置");
                }
            } catch (Exception e) {
                mTvBgMusicStatus.setText("已设置 (无法解析名称)");
            }
        }
    }

    private void updateLyricFileStatus() {
        String uriStr = AppSettings.getString("lyric_file_uri", "");
        if (uriStr.isEmpty()) {
            mTvLyricFileStatus.setText("未选择");
        } else {
            try {
                Uri uri = Uri.parse(uriStr);
                String path = uri.getPath();
                if (path != null) {
                    int index = path.lastIndexOf('/');
                    if (index >= 0) {
                        path = path.substring(index + 1);
                    }
                    mTvLyricFileStatus.setText("已选择: " + Uri.decode(path));
                } else {
                    mTvLyricFileStatus.setText("已选择");
                }
            } catch (Exception e) {
                mTvLyricFileStatus.setText("已选择 (无法解析名称)");
            }
        }
    }

    private void pickLogPath() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, ActivityResultId.REQUEST_CODE_CHOOSE_LOG_PATH);
    }

    private void updateLogPathStatus() {
        String path = AppSettings.getString("log_save_path", "");
        if (path.isEmpty()) {
            mTvLogSavePath.setText("默认 (内部存储)");
        } else {
            try {
                Uri uri = Uri.parse(path);
                mTvLogSavePath.setText("已设置: " + Uri.decode(uri.getLastPathSegment()));
            } catch (Exception e) {
                mTvLogSavePath.setText("已设置: " + path);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == ActivityResultId.REQUEST_CODE_CHOOSE_BG_MUSIC) {
            try {
                // Take persistable permission
                mActivity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                e.printStackTrace();
            }
            AppSettings.setString("background_music_uri", uri.toString());
            updateBgMusicStatus();
            com.linux.permissionmanager.utils.BackgroundMusicManager.getInstance(mActivity).play(uri);
        } else if (requestCode == ActivityResultId.REQUEST_CODE_CHOOSE_LOG_PATH) {
            try {
                // Take persistable permission for the directory
                mActivity.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                AppSettings.setString("log_save_path", uri.toString());
                updateLogPathStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == ActivityResultId.REQUEST_CODE_CHOOSE_LYRIC_FILE) {
            try {
                mActivity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                AppSettings.setString("lyric_file_uri", uri.toString());
                updateLyricFileStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initAboutText() {
        StringBuffer sb = new StringBuffer();
        sb.append("内置核心版本：");
        sb.append(NativeBridge.getSdkVersion());
        mTvAboutVer.setText(sb.toString());
    }

    private void initLink() {
        mTvLink.setText("https://github.com/X-KRoot/X-KRoot");
        mTvLink.setOnClickListener(v -> {
            UrlIntentUtils.openUrl(mActivity, mTvLink.getText().toString());
        });
        makeUnderline(mTvLink);
    }

    private void onDownloadChangeLogApp(AppUpdateInfo updateInfo) {
        mUpdateManager.requestAppChangelog(
                updateInfo,
                (content) -> DialogUtils.showLogDialog(mActivity, content),
                (e) -> DialogUtils.showMsgDlg(mActivity, "提示", "App 更新日志下载失败：" + e.getMessage(),null)
        );
    }

    private void initUpdateBlock() {
        mUpdateManager.requestAppUpdate(
                (info) -> {
                    if (info == null || !info.isHasNewVersion()) return;
                    mTvUpdateBlock.setVisibility(View.VISIBLE);
                    mTvUpdateFound.setText("发现新版本：" + info.getLatestVer());
                    mTvUpdateChangelog.setOnClickListener(v -> {
                        onDownloadChangeLogApp(info);
                    });
                    mTvUpdateDownload.setOnClickListener(v -> {
                        UrlIntentUtils.openUrl(mActivity, info.getDownloadUrl());
                    });
                    DialogUtils.showCustomDialog(
                            mActivity, "提示", "发现新版本：" + info.getLatestVer(),null,"确定",
                            (dialog, which) -> {
                                UrlIntentUtils.openUrl(mActivity, info.getDownloadUrl());
                                dialog.dismiss();
                            },
                            "取消",
                            (dialog, which) -> dialog.dismiss()
                    );
                },
                (e) -> {}
        );
        makeUnderline(mTvUpdateChangelog);
        makeUnderline(mTvUpdateDownload);
    }

    private void updateAllCardsAlpha(View rootView) {
        if (rootView == null) return;
        float alpha = AppSettings.getFloat("card_alpha", 1.0f);
        int alphaInt = Math.round(alpha * 255);

        if (rootView instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) rootView;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof androidx.cardview.widget.CardView) {
                    androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) child;
                    card.setAlpha(1.0f);

                    // 设置卡片阴影透明度
                    float elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
                    card.setCardElevation(alpha * elevation);

                    android.content.res.ColorStateList csl = card.getCardBackgroundColor();
                    if (csl != null) {
                        int baseColor = csl.getDefaultColor();
                        int colorWithAlpha = (baseColor & 0x00FFFFFF) | (alphaInt << 24);
                        card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorWithAlpha));
                    }
                }
                updateAllCardsAlpha(child);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBgMusicStatus();
        updateLyricFileStatus();
        updateAllCardsAlpha(getView());
        ThemeUtils.applyToViewTree(getView(), ThemeUtils.getThemeColor());
    }

    private void makeUnderline(TextView tv) {
        tv.getPaint().setFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }
}
