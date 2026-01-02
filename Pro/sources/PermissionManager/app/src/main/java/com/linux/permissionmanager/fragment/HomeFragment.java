package com.linux.permissionmanager.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ValueAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.linux.permissionmanager.AppSettings;
import com.linux.permissionmanager.R;
import com.linux.permissionmanager.bridge.NativeBridge;
import com.linux.permissionmanager.utils.ClipboardUtils;
import com.linux.permissionmanager.utils.DialogUtils;
import com.linux.permissionmanager.utils.ThemeUtils;

import com.linux.permissionmanager.utils.BackgroundMusicManager;
import com.linux.permissionmanager.utils.LyricUtils;
import android.net.Uri;
import android.widget.TextView;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private Activity mActivity;
    private String mRootKey = "";
    private String lastInputCmd = "id";
    private String lastInputRootExecPath = "";

    private EditText console_edit;
    private View mLyricCard;
    private TextView mLyricTv;
    private TextView mSkrootStatusVal;
    private TextView mKernelVerVal;
    private TextView mModuleCountVal;
    private TextView mSuAppCountVal;
    private List<LyricUtils.LyricEntry> mLyrics;
    private String mCurrentLyricUriStr = "";

    private View mMusicVisualizerContainer;
    private View[] mMusicBars;
    private java.util.List<ValueAnimator> mVisualizerAnimators = new java.util.ArrayList<>();

    private final Handler mLyricUpdateHandler = new Handler();
    private final Runnable mLyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateLyrics();
            mLyricUpdateHandler.postDelayed(this, 500);
        }
    };

    public HomeFragment(Activity activity) {
        mActivity = activity;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootKey = AppSettings.getString("rootKey", mRootKey);
        lastInputCmd = AppSettings.getString("lastInputCmd", lastInputCmd);
        lastInputRootExecPath = AppSettings.getString("lastInputRootExecPath", lastInputRootExecPath);

        Button install_skroot_env_btn = view.findViewById(R.id.install_skroot_env_btn);
        Button uninstall_skroot_env_btn = view.findViewById(R.id.uninstall_skroot_env_btn);
        Button test_root_btn = view.findViewById(R.id.test_root_btn);
        Button run_root_cmd_btn = view.findViewById(R.id.run_root_cmd_btn);
        Button root_exec_process_btn = view.findViewById(R.id.root_exec_process_btn);
        Button implant_app_btn = view.findViewById(R.id.implant_app_btn);
        Button copy_info_btn = view.findViewById(R.id.copy_info_btn);
        Button clean_info_btn = view.findViewById(R.id.clean_info_btn);
        console_edit = view.findViewById(R.id.console_edit);
        mLyricCard = view.findViewById(R.id.lyric_card);
        mLyricTv = view.findViewById(R.id.lyric_tv);
        mSkrootStatusVal = view.findViewById(R.id.skroot_status_val);
        mKernelVerVal = view.findViewById(R.id.kernel_ver_val);
        mModuleCountVal = view.findViewById(R.id.module_count_val);
        mSuAppCountVal = view.findViewById(R.id.su_app_count_val);

        mMusicVisualizerContainer = view.findViewById(R.id.music_visualizer_container);
        mMusicBars = new View[20];
        for (int i = 0; i < 20; i++) {
            int resId = getResources().getIdentifier("music_bar_" + (i + 1), "id", mActivity.getPackageName());
            mMusicBars[i] = view.findViewById(resId);
        }

        install_skroot_env_btn.setOnClickListener(this);
        uninstall_skroot_env_btn.setOnClickListener(this);
        test_root_btn.setOnClickListener(this);
        run_root_cmd_btn.setOnClickListener(this);
        root_exec_process_btn.setOnClickListener(this);
        implant_app_btn.setOnClickListener(this);
        copy_info_btn.setOnClickListener(this);
        clean_info_btn.setOnClickListener(this);

        ThemeUtils.applyToViewTree(view, ThemeUtils.getThemeColor());
    }

    public void setRootKey(String rootKey) {
        this.mRootKey = rootKey;
        showSkrootStatus();
        updateKernelInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLyricUpdateHandler.post(mLyricUpdateRunnable);
        updateAllCardsAlpha(getView());
        updateKernelInfo();
        
        int themeColor = ThemeUtils.getThemeColor();
        ThemeUtils.applyToViewTree(getView(), themeColor);
        
        // 更新律动条颜色
        if (mMusicBars != null) {
            for (View bar : mMusicBars) {
                if (bar != null) {
                    bar.setBackgroundColor(themeColor);
                }
            }
        }
        
        updateMusicVisualizerVisibility();
    }

    private void updateKernelInfo() {
        if (mSkrootStatusVal == null) return;

        // 1. Root 状态
        String curState = NativeBridge.getSkrootEnvState(mRootKey);
        if (curState.contains("Running")) {
            mSkrootStatusVal.setText("运行中");
            mSkrootStatusVal.setTextColor(ThemeUtils.getThemeColor());
        } else if (curState.contains("NotInstalled")) {
            mSkrootStatusVal.setText("未安装");
            mSkrootStatusVal.setTextColor(android.graphics.Color.GRAY);
        } else {
            mSkrootStatusVal.setText("故障");
            mSkrootStatusVal.setTextColor(android.graphics.Color.RED);
        }

        // 2. 内核版本 - 仅显示系统 Linux 内核版本
        String osVer = System.getProperty("os.version");
        if (osVer != null && !osVer.isEmpty()) {
            mKernelVerVal.setText(osVer);
        } else {
            mKernelVerVal.setText("-");
        }

        // 3. 模块数量
        try {
            String jsonAll = NativeBridge.getSkrootModuleList(mRootKey, false);
            org.json.JSONArray modules = new org.json.JSONArray(jsonAll);
            mModuleCountVal.setText(String.valueOf(modules.length()));
        } catch (Exception e) {
            mModuleCountVal.setText("0");
        }

        // 4. 授权应用数量
        try {
            String jsonSu = NativeBridge.getSuAuthList(mRootKey);
            org.json.JSONArray suApps = new org.json.JSONArray(jsonSu);
            mSuAppCountVal.setText(String.valueOf(suApps.length()));
        } catch (Exception e) {
            mSuAppCountVal.setText("0");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLyricUpdateHandler.removeCallbacks(mLyricUpdateRunnable);
        if (mMusicVisualizerContainer != null) {
            mMusicVisualizerContainer.removeCallbacks(mVisualizerRunnable);
        }
        cancelVisualizerAnimations();
    }

    /**
     * 立即刷新主题颜色，供外部调用（如 MainActivity 刷新背景时）
     */
    public void refreshTheme() {
        if (getView() == null) return;
        int themeColor = ThemeUtils.getThemeColor();
        ThemeUtils.applyToViewTree(getView(), themeColor);
        
        // 特别确保律动条颜色更新
        if (mMusicBars != null) {
            for (View bar : mMusicBars) {
                if (bar != null) {
                    bar.setBackgroundColor(themeColor);
                }
            }
        }
    }

    private void updateLyrics() {
        boolean showLyrics = AppSettings.getBoolean("show_lyrics", false);
        String lyricUriStr = AppSettings.getString("lyric_file_uri", "");

        if (!showLyrics || lyricUriStr.isEmpty()) {
            mLyricCard.setVisibility(View.GONE);
            return;
        }

        mLyricCard.setVisibility(View.VISIBLE);

        // Load lyrics if URI changed or not loaded
        if (!lyricUriStr.equals(mCurrentLyricUriStr) || mLyrics == null) {
            mCurrentLyricUriStr = lyricUriStr;
            try {
                mLyrics = LyricUtils.parseLrc(mActivity, Uri.parse(lyricUriStr));
                if (mLyrics.isEmpty()) {
                    mLyricTv.setText("歌词解析为空或文件错误");
                }
            } catch (Exception e) {
                mLyricTv.setText("歌词加载失败");
                mLyrics = null;
            }
        }

        if (mLyrics != null && !mLyrics.isEmpty()) {
            BackgroundMusicManager musicManager = BackgroundMusicManager.getInstance(mActivity);
            if (musicManager.isPlaying()) {
                long currentPos = musicManager.getCurrentPosition();
                String currentLyric = LyricUtils.getCurrentLyric(mLyrics, currentPos);
                if (currentLyric.isEmpty()) {
                    mLyricTv.setText("...");
                } else {
                    mLyricTv.setText(currentLyric);
                }
            } else {
                mLyricTv.setText("音乐已暂停");
            }
        }
    }

    private void updateMusicVisualizerVisibility() {
        if (mMusicVisualizerContainer == null) return;

        boolean enabled = AppSettings.getBoolean("show_music_visualizer", false);
        String musicUri = AppSettings.getString("background_music_uri", "");
        BackgroundMusicManager musicManager = BackgroundMusicManager.getInstance(mActivity);

        boolean shouldShow = enabled && !musicUri.isEmpty() && musicManager.isPlaying();
        mMusicVisualizerContainer.setVisibility(shouldShow ? View.VISIBLE : View.GONE);

        if (shouldShow) {
            animateMusicBars();
        }
    }

    private void cancelVisualizerAnimations() {
        for (ValueAnimator animator : mVisualizerAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        mVisualizerAnimators.clear();
    }

    private void animateMusicBars() {
        if (mMusicBars == null || mMusicBars.length == 0) return;

        // 取消之前的动画以确保平滑切换
        cancelVisualizerAnimations();

        int minHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        int maxHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

        for (View bar : mMusicBars) {
            if (bar == null) continue;

            int currentHeight = bar.getHeight();
            if (currentHeight <= 0) currentHeight = minHeight;
            int targetHeight = minHeight + (int) (Math.random() * (maxHeight - minHeight));

            ValueAnimator animator = ValueAnimator.ofInt(currentHeight, targetHeight);
            animator.setDuration(450); // 调整为 450ms，在慢与快之间取得平衡
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = bar.getLayoutParams();
                if (lp != null) {
                    lp.height = val;
                    bar.setLayoutParams(lp);
                }
            });
            animator.start();
            mVisualizerAnimators.add(animator);
        }

        mMusicVisualizerContainer.removeCallbacks(mVisualizerRunnable);
        mMusicVisualizerContainer.postDelayed(mVisualizerRunnable, 450); // 延迟时间与动画时长匹配
    }

    private final Runnable mVisualizerRunnable = new Runnable() {
        @Override
        public void run() {
            updateMusicVisualizerVisibility();
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.install_skroot_env_btn:
                onClickInstallSkrootEnvBtn();
                break;
            case R.id.uninstall_skroot_env_btn:
                onClickUninstallSkrootEnvBtn();
                break;
            case R.id.test_root_btn:
                appendConsoleMsg(NativeBridge.testRoot(mRootKey));
                break;
            case R.id.run_root_cmd_btn:
                showInputRootCmdDlg();
                break;
            case R.id.implant_app_btn:
                break;
            case R.id.root_exec_process_btn:
                showInputRootExecProcessPathDlg();
                break;
            case R.id.copy_info_btn:
                copyConsoleMsg();
                break;
            case R.id.clean_info_btn:
                cleanConsoleMsg();
                break;
            default:
                break;
        }
    }
    private void showSkrootStatus() {
        String curState = NativeBridge.getSkrootEnvState(mRootKey);
        String installedVer = NativeBridge.getInstalledSkrootEnvVersion(mRootKey);
        String sdkVer = NativeBridge.getSdkVersion();

        if(curState.indexOf("NotInstalled") != -1) {
            appendConsoleMsg("X-KRoot环境未安装！");
        } else if(curState.indexOf("Fault") != -1) {
            appendConsoleMsg("X-KRoot环境出现故障，核心版本：" + installedVer);
        } else if(curState.indexOf("Running") != -1) {
            if (sdkVer.equals(installedVer)) {  
                appendConsoleMsg("X-KRoot环境运行中，核心版本：" + installedVer);
            } else {
                appendConsoleMsg("X-KRoot环境运行中，核心版本：" + installedVer + "，版本太低，请升级！");
                appendConsoleMsg("升级方法：重新点击“安装环境”按钮。");
            }
        }
    }

    private void showInputRootCmdDlg() {
        Handler inputCallback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String text = (String)msg.obj;
                lastInputCmd = text;
                AppSettings.setString("lastInputCmd", lastInputCmd);
                appendConsoleMsg(text + "\n" + NativeBridge.runRootCmd(mRootKey, text));
                super.handleMessage(msg);
            }
        };
        DialogUtils.showInputDlg(mActivity, lastInputCmd, "请输入ROOT命令", null, inputCallback, null);
    }

    private void onClickInstallSkrootEnvBtn() {
        appendConsoleMsg(NativeBridge.installSkrootEnv(mRootKey));
    }

    private void onClickUninstallSkrootEnvBtn() {
        DialogUtils.showCustomDialog(
                mActivity,
                "确认",
                "确定要卸载X-KRoot环境吗？这会同时清空 SU 授权列表和删除已安装的模块",
                null,
                "确定", (dialog, which) -> {
                    dialog.dismiss();
                    appendConsoleMsg(NativeBridge.uninstallSkrootEnv(mRootKey));
                },
                "取消", (dialog, which) -> {
                    dialog.dismiss();
                }
        );
    }

    private void showInputRootExecProcessPathDlg() {
        Handler inputCallback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String text = (String)msg.obj;

                lastInputRootExecPath = text;
                AppSettings.setString("lastInputRootExecPath", lastInputRootExecPath);
                appendConsoleMsg(text + "\n" + NativeBridge.rootExecProcessCmd(mRootKey, text));
                super.handleMessage(msg);
            }
        };
        Handler helperCallback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                DialogUtils.showMsgDlg(mActivity,"帮助", "请将JNI可执行文件放入/data内任意目录并且赋予777权限，如/data/app/com.xx，然后输入文件路径，即可直接执行，如：\n/data/com.xx/aaa\n", null);
                super.handleMessage(msg);
            }
        };
        DialogUtils.showInputDlg(mActivity, lastInputRootExecPath, "请输入Linux可运行文件的位置", "指导", inputCallback, helperCallback);
        DialogUtils.showMsgDlg(mActivity,"提示", "本功能是以ROOT身份直接运行程序，可避免产生su、sh等多余驻留后台进程，能最大程度上避免侦测", null);
    }

    private void appendConsoleMsg(String msg) {
        StringBuffer txt = new StringBuffer();
        txt.append(console_edit.getText().toString());
        if (txt.length() != 0) txt.append("\n");
        txt.append(msg);
        txt.append("\n");
        console_edit.setText(txt.toString());
        console_edit.setSelection(txt.length());
    }

    private void copyConsoleMsg() {
        ClipboardUtils.copyText(mActivity, console_edit.getText().toString());
        Toast.makeText(mActivity, "复制成功", Toast.LENGTH_SHORT).show();
    }

    private void cleanConsoleMsg() {
        console_edit.setText("");
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
}
