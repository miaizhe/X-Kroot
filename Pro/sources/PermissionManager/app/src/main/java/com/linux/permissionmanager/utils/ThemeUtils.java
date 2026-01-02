package com.linux.permissionmanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.linux.permissionmanager.AppSettings;
import com.linux.permissionmanager.R;

public class ThemeUtils {
    public static final int DEFAULT_PRIMARY_COLOR = Color.parseColor("#6750A4");

    public static int getThemeColor() {
        return AppSettings.getInt("theme_color", DEFAULT_PRIMARY_COLOR);
    }

    public static void setThemeColor(int color) {
        AppSettings.setInt("theme_color", color);
    }

    public static void applyTheme(Activity activity) {
        int color = getThemeColor();
        applyToWindow(activity, color);
        
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null) {
            applyToViewTree(rootView, color);
        }
    }

    public static void applyToWindow(Activity activity, int color) {
        Window window = activity.getWindow();
        
        // Update status bar and navigation bar if they are not transparent (Edge-to-Edge handles this differently)
        // In this app, MainActivity uses transparent bars with Edge-to-Edge.
        
        // Update Toolbar if found
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Only update background if no custom background image is set
            String bgPath = AppSettings.getString("background_path", "");
            if (bgPath.isEmpty()) {
                // In Material 3, we usually want a surface color for toolbar, but we can tint it
                // toolbar.setBackgroundColor(getSurfaceColorWithTint(color));
            }
        }

        // Update BottomNavigationView
        BottomNavigationView nav = activity.findViewById(R.id.bottom_navigation);
        if (nav != null) {
            ColorStateList csl = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                    color,
                    Color.GRAY
                }
            );
            nav.setItemIconTintList(csl);
            nav.setItemTextColor(csl);
            // Material 3 indicator color
            nav.setItemRippleColor(ColorStateList.valueOf(color & 0x20FFFFFF));
        }
    }

    public static void applyToViewTree(View view, int color) {
        ColorStateList csl = ColorStateList.valueOf(color);
        ColorStateList tonalCsl = ColorStateList.valueOf(color & 0x20FFFFFF);

        if (view instanceof MaterialButton) {
            MaterialButton btn = (MaterialButton) view;
            
            // 检查按钮的样式（描边还是填充）
            boolean isOutlined = btn.getStrokeWidth() > 0;
            
            if (isOutlined) {
                // 描边按钮：描边和图标用主题色，背景保持透明
                btn.setStrokeColor(csl);
                btn.setRippleColor(tonalCsl);
                if (btn.getIcon() != null) btn.setIconTint(csl);
                
                // 文字通常也用主题色（如果原来不是白色的）
                if (btn.getCurrentTextColor() != Color.WHITE) {
                    btn.setTextColor(color);
                }
            } else {
                // 填充按钮：背景用主题色，文字和图标用白色
                if (btn.getBackgroundTintList() != null && btn.getBackgroundTintList().getDefaultColor() != Color.TRANSPARENT) {
                    btn.setBackgroundTintList(csl);
                }
                btn.setTextColor(Color.WHITE);
                if (btn.getIcon() != null) btn.setIconTint(ColorStateList.valueOf(Color.WHITE));
                btn.setRippleColor(ColorStateList.valueOf(Color.WHITE & 0x40FFFFFF));
            }
        } else if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            // 更新卡片的描边颜色
            card.setStrokeColor(csl);
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            // 排除控制台
            if (tv.getId() != R.id.console_edit) {
                // 检查是否是主色调文本（基于 ID 或原始颜色）
                int textColor = tv.getTextColors().getDefaultColor();
                if (isPrimaryColored(tv, textColor)) {
                    tv.setTextColor(color);
                }
                
                if (tv.getLinkTextColors() != null) {
                    tv.setLinkTextColor(color);
                }
            }
        } else if (view instanceof MaterialSwitch) {
            MaterialSwitch sw = (MaterialSwitch) view;
            sw.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{color, Color.WHITE}
            ));
            sw.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{color & 0x40FFFFFF, Color.LTGRAY}
            ));
        } else if (view instanceof Slider) {
            Slider slider = (Slider) view;
            slider.setThumbTintList(csl);
            slider.setTrackTintList(csl);
            slider.setHaloTintList(tonalCsl);
        } else if (view instanceof CheckBox) {
            ((CheckBox) view).setButtonTintList(csl);
        } else if (view instanceof RadioButton) {
            ((RadioButton) view).setButtonTintList(csl);
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            // 仅对非应用图标且没有背景图性质的 ImageView 应用染色
            // 排除应用图标 (app_icon_iv)、系统背景以及主界面自定义背景图
            if (iv.getId() != R.id.app_icon_iv && 
                iv.getId() != android.R.id.background &&
                iv.getId() != R.id.main_background_iv) {
                
                // 如果 ImageView 的 ID 包含 "btn" 或 "icon"，或者它在特定的布局中，则应用染色
                String idName = "";
                try {
                    idName = view.getResources().getResourceEntryName(view.getId());
                } catch (Exception ignored) {}
                
                if (idName.contains("btn") || idName.contains("icon") || iv.getDrawable() == null) {
                    iv.setImageTintList(csl);
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToViewTree(vg.getChildAt(i), color);
            }
        }
    }

    private static boolean isPrimaryColored(TextView tv, int color) {
        // 关键 ID 检查
        int id = tv.getId();
        if (id == R.id.link_tv || id == R.id.core_update_found_tv || 
            id == R.id.lyric_tv || id == R.id.app_name_tv || id == R.id.name_tv) {
            return true;
        }
        
        // 颜色相似度检查（如果颜色接近默认主色）
        return isColorSimilar(color, DEFAULT_PRIMARY_COLOR);
    }

    private static boolean isColorSimilar(int color1, int color2) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        int threshold = 40;
        return Math.abs(r1 - r2) < threshold && 
               Math.abs(g1 - g2) < threshold && 
               Math.abs(b1 - b2) < threshold;
    }
    
    public static int getAdaptiveBackgroundColor(Context context, int themeColor) {
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        if (isDarkMode) {
            // Dark mode adaptive background: very dark version of the theme color
            return Color.argb(255, 
                (int)(Color.red(themeColor) * 0.05 + 18 * 0.95),
                (int)(Color.green(themeColor) * 0.05 + 18 * 0.95),
                (int)(Color.blue(themeColor) * 0.05 + 18 * 0.95));
        } else {
            // Light mode adaptive background: very light version of the theme color
            return Color.argb(255, 
                (int)(Color.red(themeColor) * 0.05 + 245 * 0.95),
                (int)(Color.green(themeColor) * 0.05 + 245 * 0.95),
                (int)(Color.blue(themeColor) * 0.05 + 245 * 0.95));
        }
    }

    private static int getSurfaceColorWithTint(int color) {
        // Simple surface tint logic: mix surface color with a bit of primary color
        return Color.argb(255, 
            (int)(Color.red(color) * 0.05 + 255 * 0.95),
            (int)(Color.green(color) * 0.05 + 255 * 0.95),
            (int)(Color.blue(color) * 0.05 + 255 * 0.95));
    }
}
