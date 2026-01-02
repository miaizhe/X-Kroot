package com.linux.permissionmanager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.linux.permissionmanager.bridge.NativeBridge;
import com.linux.permissionmanager.fragment.SuAuthFragment;
import com.linux.permissionmanager.fragment.HomeFragment;
import com.linux.permissionmanager.fragment.SettingsFragment;
import com.linux.permissionmanager.fragment.SkrModFragment;
import com.linux.permissionmanager.utils.DialogUtils;
import com.linux.permissionmanager.utils.FileUtils;
import com.linux.permissionmanager.utils.GetAppListPermissionHelper;
import com.linux.permissionmanager.utils.GetSdcardPermissionsHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.linux.permissionmanager.utils.BackgroundMusicManager;
import com.linux.permissionmanager.utils.ThemeUtils;
import androidx.palette.graphics.Palette;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class MainActivity extends AppCompatActivity {
    private String mRootKey = "";
    private BottomNavigationView mBottomNavigation;
    private MenuItem mMainMenu;
    private MenuItem mMusicMenu;
    private MenuItem mRefreshBgMenu;
    public HomeFragment mHomeFragm = null;
    public SuAuthFragment mSuAuthFragm = null;
    public SkrModFragment mSkrModFragm = null;
    public SettingsFragment mSettingsFragm = null;
    
    private final BackgroundMusicManager.OnStateChangedListener musicStateListener = isPlaying -> {
        runOnUiThread(this::updateMusicMenuIcon);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_main);
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Handle insets for toolbar and bottom navigation
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            androidx.core.graphics.Insets statusInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusInsets.top, 0, 0);
            return insets;
        });

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            androidx.core.graphics.Insets navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, navInsets.bottom);
            return insets;
        });

        // Ensure background colors are updated for the current theme
        updateBackground();
        
        mRootKey = AppSettings.getString("rootKey", mRootKey);
        checkGetAppListPermission();
        showInputRootKeyDlg();
        setupFragment();

        // Apply theme color
        ThemeUtils.applyTheme(this);

        // Auto play background music
        String bgMusicUriStr = AppSettings.getString("background_music_uri", "");
        if (!bgMusicUriStr.isEmpty()) {
            try {
                BackgroundMusicManager.getInstance(this).play(Uri.parse(bgMusicUriStr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        BackgroundMusicManager.getInstance(this).setOnStateChangedListener(musicStateListener);
    }

    public void updateBackgroundAlpha() {
        View overlay = findViewById(R.id.main_background_overlay);
        if (overlay != null) {
            float alpha = AppSettings.getFloat("background_alpha", 0.5f);
            overlay.setAlpha(alpha);
        }
    }

    public void updateBackground() {
        String bgPath = AppSettings.getString("background_path", "");
        View iv = findViewById(R.id.main_background_iv);
        View overlay = findViewById(R.id.main_background_overlay);
        View toolbar = findViewById(R.id.toolbar);
        View nav = findViewById(R.id.bottom_navigation);

        if (mBottomNavigation == null && nav instanceof BottomNavigationView) {
            mBottomNavigation = (BottomNavigationView) nav;
        }
        
        updateMenuVisibility();
        
        // Handle system bar icons color based on theme
        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        
        // Get background color from theme to ensure it matches light/dark mode
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        int backgroundColor = typedValue.data;
        
        if (bgPath.isEmpty()) {
            iv.setVisibility(View.GONE);
            overlay.setVisibility(View.GONE);
            
            int finalBgColor = backgroundColor;
            if (AppSettings.getBoolean("adaptive_background", false)) {
                finalBgColor = ThemeUtils.getAdaptiveBackgroundColor(this, ThemeUtils.getThemeColor());
            }
            
            getWindow().getDecorView().setBackgroundColor(finalBgColor);
            if (toolbar != null) toolbar.setBackgroundColor(finalBgColor);
            if (nav != null) nav.setBackgroundColor(finalBgColor);
            
            // In light mode with white background, icons should be dark
            controller.setAppearanceLightStatusBars(!isDarkMode);
            controller.setAppearanceLightNavigationBars(!isDarkMode);
        } else {
            iv.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
            
            float alpha = AppSettings.getFloat("background_alpha", 0.5f);
            overlay.setAlpha(alpha);
            
            if (toolbar != null) toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            if (nav != null) nav.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            
            // When showing background image, we usually want light icons because of the dark overlay
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
            
            Log.d("MainActivity", "Loading background from: " + bgPath);
            
            if (isDestroyed() || isFinishing()) return;

            com.bumptech.glide.load.engine.DiskCacheStrategy strategy = com.bumptech.glide.load.engine.DiskCacheStrategy.ALL;
            if (bgPath.startsWith("http")) {
                strategy = com.bumptech.glide.load.engine.DiskCacheStrategy.NONE;
            }

            Glide.with(this)
                    .asBitmap()
                    .load(bgPath.trim())
                    .diskCacheStrategy(strategy)
                    .skipMemoryCache(bgPath.startsWith("http"))
                    .centerCrop()
                    .timeout(60000)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Log.d("MainActivity", "Glide bitmap load success");
                            ((android.widget.ImageView) iv).setImageBitmap(resource);
                            
                            // Extract color from image if adaptive background is enabled
                            if (AppSettings.getBoolean("adaptive_background", false)) {
                                Palette.from(resource).generate(palette -> {
                                    if (palette != null) {
                                        // Prefer Vibrant or Muted colors
                                        int color = palette.getVibrantColor(
                                            palette.getMutedColor(
                                                palette.getDominantColor(ThemeUtils.DEFAULT_PRIMARY_COLOR)
                                            )
                                        );
                                        
                                        Log.d("MainActivity", "Extracted color: " + String.format("#%06X", (0xFFFFFF & color)));
                                        
                                        // Apply the extracted color as theme color
                                        ThemeUtils.setThemeColor(color);
                                        ThemeUtils.applyTheme(MainActivity.this);
                                        
                                        // Notify fragments to refresh theme
                                        if (mHomeFragm != null && mHomeFragm.isAdded()) {
                                            mHomeFragm.refreshTheme();
                                        }
                                        if (mSettingsFragm != null && mSettingsFragm.isAdded()) {
                                            mSettingsFragm.refreshAllFragmentsTheme();
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.e("MainActivity", "Glide load failed");
                        }
                    });
        }
    }

    private void showInputRootKeyDlg() {
        Handler inputCallback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String text = (String)msg.obj;
                mRootKey = text;
                AppSettings.setString("rootKey", mRootKey);
                mHomeFragm.setRootKey(mRootKey);
                mSuAuthFragm.setRootKey(mRootKey);
                mSkrModFragm.setRootKey(mRootKey);
                mSettingsFragm.setRootKey(mRootKey);
                super.handleMessage(msg);
            }
        };
        DialogUtils.showInputDlg(this, mRootKey,"请输入ROOT权限的KEY", null, inputCallback, null);
    }

    private void checkGetAppListPermission() {
        if(GetAppListPermissionHelper.getPermissions(this)) return;
        DialogUtils.showCustomDialog(
                this,"权限申请","请授予读取APP列表权限，再重新打开",null,"确定",
                (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                },null, null);
    }

    private void setupFragment() {
        mBottomNavigation = findViewById(R.id.bottom_navigation);
        mHomeFragm = new HomeFragment(this);
        mSuAuthFragm = new SuAuthFragment(this);
        mSkrModFragm = new SkrModFragment(this);
        mSettingsFragm = new SettingsFragment(this);

        // Pass root key to fragments if it exists
        if (mRootKey != null && !mRootKey.isEmpty()) {
            mHomeFragm.setRootKey(mRootKey);
            mSuAuthFragm.setRootKey(mRootKey);
            mSkrModFragm.setRootKey(mRootKey);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, mHomeFragm)
                .commit();
        mBottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int checkedId = item.getItemId();
            if (checkedId == R.id.rb_home) {
                selectedFragment = mHomeFragm;
            } else if (checkedId == R.id.rb_su_auth) {
                selectedFragment = mSuAuthFragm;
            } else if (checkedId == R.id.rb_skr_mod) {
                selectedFragment = mSkrModFragm;
            } else if (checkedId == R.id.rb_settings) {
                selectedFragment = mSettingsFragm;
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                        .replace(R.id.frame_layout, selectedFragment)
                        .commitNow();
                updateMenuVisibility();
                return true;
            }
            return false;
        });
    }

    private void updateMenuVisibility() {
        if (mBottomNavigation == null) return;
        int checkedId = mBottomNavigation.getSelectedItemId();
        
        if (mMainMenu != null) {
            mMainMenu.setVisible(false);
        }

        if (mRefreshBgMenu != null) {
            String bgPath = AppSettings.getString("background_path", "");
            mRefreshBgMenu.setVisible(!bgPath.isEmpty());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMainMenu = menu.findItem(R.id.action_add);
        mMusicMenu = menu.findItem(R.id.action_music_control);
        mRefreshBgMenu = menu.findItem(R.id.action_refresh_bg);
        updateMusicMenuIcon();
        updateMenuVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            View anchor = findViewById(R.id.toolbar).findViewById(R.id.action_add);
            if (anchor == null) anchor = findViewById(R.id.toolbar);
            showMainPopupMenu(anchor);
            return true;
        } else if (item.getItemId() == R.id.action_refresh_bg) {
            updateBackground();
            return true;
        } else if (item.getItemId() == R.id.action_music_control) {
            BackgroundMusicManager manager = BackgroundMusicManager.getInstance(this);
            if (manager.isPlaying()) {
                manager.pause();
            } else {
                manager.resume();
                // If resume didn't work (e.g. no current URI in manager), try loading from settings
                if (!manager.isPlaying()) {
                    String bgMusicUriStr = AppSettings.getString("background_music_uri", "");
                    if (!bgMusicUriStr.isEmpty()) {
                        try {
                            manager.play(Uri.parse(bgMusicUriStr));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMusicMenuIcon() {
        if (mMusicMenu == null) return;
        BackgroundMusicManager manager = BackgroundMusicManager.getInstance(this);
        if (manager.isPlaying()) {
            mMusicMenu.setIcon(R.drawable.ic_pause);
            mMusicMenu.setTitle("暂停");
        } else {
            mMusicMenu.setIcon(R.drawable.ic_play_arrow);
            mMusicMenu.setTitle("播放");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BackgroundMusicManager.getInstance(this).setOnStateChangedListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onChooseFileActivityResult(requestCode, resultCode, data);
        onChooseBgImageActivityResult(requestCode, resultCode, data);
    }

    private void showMainPopupMenu(View v) {
        int checkedId = mBottomNavigation.getSelectedItemId();
        if(checkedId == R.id.rb_su_auth) onShowSuAuthMainPopupMenu(v);
        if(checkedId == R.id.rb_skr_mod)  onShowSkrModMainPopupMenu(v);
    }

    public void onShowSuAuthMainPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.popup_su_auth_main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.add_su_auth) {
                mSuAuthFragm.onShowSelectAddSuAuthList();
            } else if (itemId == R.id.clear_su_auth) {
                mSuAuthFragm.onClearSuAuth();
            }
            return true;
        });

        popupMenu.show();
    }

    public void onShowSkrModMainPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.popup_skr_mod_main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.add_skr_mod) chooseFile();
            return true;
        });
        popupMenu.show();
    }

    private void chooseFile() {
        if(!GetSdcardPermissionsHelper.getPermissions(this, this, this.getPackageName())) {
            DialogUtils.showNeedPermissionDialog(this);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        startActivityForResult(intent, ActivityResultId.REQUEST_CODE_CHOOSE_FILE);
    }

    private void onChooseFileActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityResultId.REQUEST_CODE_CHOOSE_FILE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String filePath = FileUtils.getRealPathFromURI(this, uri);
            if (filePath == null) {
                Log.e("SkrModFragment", "Invalid file path");
                return;
            }
            Log.d("SkrModFragment", "Add skr module file path: " + filePath);
            mSkrModFragm.onAddSkrMod(filePath);
        }
    }

    private void onChooseBgImageActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityResultId.REQUEST_CODE_CHOOSE_BG_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String filePath = FileUtils.getRealPathFromURI(this, uri);
            if (filePath == null) {
                Log.e("MainActivity", "Invalid image path");
                return;
            }
            AppSettings.setString("background_path", filePath);
            updateBackground();
        }
    }

}