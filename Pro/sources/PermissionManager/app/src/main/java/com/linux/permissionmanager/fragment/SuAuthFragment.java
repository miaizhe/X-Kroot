package com.linux.permissionmanager.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.linux.permissionmanager.R;
import com.linux.permissionmanager.AppSettings;
import com.linux.permissionmanager.adapter.SuAuthAdapter;
import com.linux.permissionmanager.bridge.NativeBridge;
import com.linux.permissionmanager.helper.SelectAppDlg;
import com.linux.permissionmanager.model.SelectAppItem;
import com.linux.permissionmanager.model.SuAuthItem;
import com.linux.permissionmanager.utils.DialogUtils;
import com.linux.permissionmanager.utils.ThemeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class SuAuthFragment extends Fragment {
    private Activity mActivity;
    private String mRootKey = "";

    private View mEmptyLayout;
    private RecyclerView mSuAuthRecyclerView;

    public SuAuthFragment(Activity activity) {
        mActivity = activity;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_su_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmptyLayout = view.findViewById(R.id.empty_layout);
        mSuAuthRecyclerView = view.findViewById(R.id.su_auth_recycler_view);
        setupSuAuthRecyclerView();
        ThemeUtils.applyToViewTree(view, ThemeUtils.getThemeColor());
    }

    public void setRootKey(String rootKey) {
        mRootKey = rootKey;
    }

    public void setupSuAuthRecyclerView() {
        String json = NativeBridge.getSuAuthList(mRootKey);
        List<SuAuthItem> skrModList = parseSuAuthList(json);
        SuAuthAdapter adapter = new SuAuthAdapter(skrModList, new SuAuthAdapter.OnItemClickListener() {
            @Override
            public void onRemoveSuAuthBtnClick(View v, SuAuthItem suAuth) {
                onRemoveSuAuth(suAuth);
            }
        });
        mSuAuthRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mSuAuthRecyclerView.setAdapter(adapter);
        mEmptyLayout.setVisibility(skrModList.size() == 0 ? View.VISIBLE : View.GONE);
        mSuAuthRecyclerView.setVisibility(skrModList.size() == 0 ? View.GONE : View.VISIBLE);
    }

    private String findAppName(List<PackageInfo> packages, String appPackageName) {
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            String packageName = packageInfo.applicationInfo.packageName;
            if(!packageName.equals(appPackageName)) continue;
            String showName = packageInfo.applicationInfo.loadLabel(mActivity.getPackageManager()).toString();
            return showName;
        }
        return "";
    }

    private Drawable findAppIcon(List<PackageInfo> packages, String appPackageName) {
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            String packageName = packageInfo.applicationInfo.packageName;
            if(!packageName.equals(appPackageName)) continue;
            Drawable icon =  packageInfo.applicationInfo.loadIcon(mActivity.getPackageManager());
            return icon;
        }
        return null;
    }

    private List<SuAuthItem> parseSuAuthList(String jsonStr) {
        List<PackageInfo> packages = mActivity.getPackageManager().getInstalledPackages(0);
        List<SuAuthItem> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String appPackageName = URLDecoder.decode(jsonObject.getString("app_package_name"), "UTF-8");
                Drawable icon = findAppIcon(packages, appPackageName);
                String appName = findAppName(packages, appPackageName);
                SuAuthItem e = new SuAuthItem(icon, appName, appPackageName);
                list.add(e);
            }
        } catch (Exception e) {
            DialogUtils.showMsgDlg(mActivity, "发生错误", jsonStr, null);
            e.printStackTrace();
        }
        return list;
    }

    public void onShowSelectAddSuAuthList() {
        Handler selectImplantAppCallback = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                SelectAppItem app = (SelectAppItem) msg.obj;
                onAddSuAuth(app);
                super.handleMessage(msg);
            }
        };
        View view = SelectAppDlg.showSelectAppDlg(mActivity, mRootKey, selectImplantAppCallback);
        CheckBox show_system_app_ckbox = view.findViewById(R.id.show_system_app_ckbox);
        CheckBox show_thirty_app_ckbox = view.findViewById(R.id.show_thirty_app_ckbox);
        show_system_app_ckbox.setChecked(false);
        show_thirty_app_ckbox.setChecked(true);
    }

    private void onAddSuAuth(SelectAppItem app) {
        String appPackageName = app.getPackageName();
        String tip = NativeBridge.addSuAuth(mRootKey, appPackageName);
        DialogUtils.showMsgDlg(mActivity, "执行结果", tip, null);
        setupSuAuthRecyclerView();
    }

    private void onRemoveSuAuth(SuAuthItem suAuth) {
        String appName = suAuth.getAppName();
        String appPackageName = suAuth.getAppPackageName();
        String showName = appName != null && !appName.isEmpty() ? appName : appPackageName;
        DialogUtils.showCustomDialog(
                mActivity,
                "确认",
                "确定要移除 " + showName +" 吗？",
                null,
                "确定", (dialog, which) -> {
                    dialog.dismiss();
                    String tip = NativeBridge.removeSuAuth(mRootKey, appPackageName);
                    DialogUtils.showMsgDlg(mActivity, "执行结果", tip, null);
                    setupSuAuthRecyclerView();
                },
                "取消", (dialog, which) -> {
                    dialog.dismiss();
                }
        );
    }

    public void onClearSuAuth() {
        DialogUtils.showCustomDialog(
                mActivity,
                "确认",
                "确定要清空 SU 授权列表吗？",
                null,
                "确定", (dialog, which) -> {
                    dialog.dismiss();
                    String tip = NativeBridge.clearSuAuthList(mRootKey);
                    DialogUtils.showMsgDlg(mActivity, "执行结果", tip, null);
                    setupSuAuthRecyclerView();
                },
                "取消", (dialog, which) -> {
                    dialog.dismiss();
                }
        );
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
        updateAllCardsAlpha(getView());
        ThemeUtils.applyToViewTree(getView(), ThemeUtils.getThemeColor());
    }
}
