package com.example.dockoverlay;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

/** App 唯一主页面：设置主页（素排版）。第 19 项「设置」即跳转到此。 */
public class SettingsActivity extends android.app.Activity {
    private static final String PKG = "com.example.dockoverlay";

    /** PC 端一次性授权命令（临时通道；S09 补完 App 内 ADB 协议栈后可不再需要）。 */
    private static final String PC_GRANT =
        "adb connect 192.168.186.95:5555\n"
        + "adb shell pm grant " + PKG + " android.permission.WRITE_SECURE_SETTINGS\n"
        + "adb shell appops set " + PKG + " SYSTEM_ALERT_WINDOW allow\n"
        + "adb shell settings get secure enabled_accessibility_services\n"
        + "adb shell settings put secure enabled_accessibility_services "
        + PKG + "/.DockAccessibilityService\n"
        + "adb shell settings put secure accessibility_enabled 1";

    private LinearLayout root;
    private DockPrefs prefs;
    private final AdbClient adb = new AdbClient();
    private TextView adbStatus;
    private TextView backupStatus;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        root = findViewById(R.id.settings_root);
        prefs = new DockPrefs(this);
        build();
    }
    @Override protected void onResume() {
        super.onResume();
        updateAdbStatus();
        updateBackupStatus();
    }
    private void section(String title) {
        TextView t = new TextView(this);
        t.setText(title); t.setTextSize(16); t.setTextColor(Color.BLACK);
        t.setPadding(0, 24, 0, 8);
        root.addView(t);
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        l.setPadding(0, 8, 0, 8);
        return l;
    }
    private Switch addSwitch(String title, String desc, boolean checked,
            android.widget.CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout l = row();
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView a = new TextView(this); a.setText(title); a.setTextSize(15); a.setTextColor(Color.BLACK);
        TextView d = new TextView(this); d.setText(desc); d.setTextSize(11); d.setTextColor(Color.GRAY);
        txt.addView(a); txt.addView(d);
        Switch s = new Switch(this); s.setChecked(checked); s.setOnCheckedChangeListener(listener);
        l.addView(txt); l.addView(s);
        root.addView(l);
        return s;
    }
    private Button addButton(String text, View.OnClickListener l) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setOnClickListener(l);
        root.addView(b);
        return b;
    }
    private TextView addText(String s) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(12); t.setTextColor(Color.DKGRAY);
        t.setPadding(0, 6, 0, 6);
        root.addView(t);
        return t;
    }

    private void build() {
        section("1  连接与授权");
        final EditText host = new EditText(this);
        host.setText(prefs.adbHost() + ":" + prefs.adbPort());
        host.setInputType(InputType.TYPE_CLASS_TEXT);
        host.setHint("无线 ADB 地址，默认 localhost:5555");
        root.addView(host);
        addButton("连接无线 ADB 并授权", v -> {
            String[] hp = host.getText().toString().trim().split(":");
            String h = hp[0];
            int p = 5555;
            try { if (hp.length > 1) p = Integer.parseInt(hp[1]); } catch (NumberFormatException ignored) { }
            prefs.setAdb(h, p);
            adbStatus.setText("正在连接…");
            final String hh = h; final int pp = p;
            new Thread(() -> {
                adb.connect(hh, pp);
                runOnUiThread(this::updateAdbStatus);
            }).start();
        });
        adbStatus = addText("");
        addButton("打开系统无障碍设置", v ->
            openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS, "无障碍"));
        addButton("打开悬浮窗权限设置", v -> openOverlaySettings());

        section("1.5  PC 端一次性授权（临时通道）");
        addText("车机与电脑连同一 WiFi 后，在电脑命令行执行以下命令。权限一次授予、持久生效，之后无需电脑。");
        addText(PC_GRANT).setTextIsSelectable(true);
        addButton("复制授权命令", v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("adb", PC_GRANT));
            adbStatus.setText("授权命令已复制；粘贴到电脑命令行执行前请先确认车机 IP");
        });
        addText("注意：enabled_accessibility_services 原值若非空，须把本服务追加在原值后（用 : 分隔），切勿覆盖车机原有无障碍服务。");

        section("2  导航栏显示");
        addSwitch("隐藏原厂导航栏", "通过 ADB 下发 policy_control；改动前自动备份原值",
            prefs.isHideSystemNav(), (btn, on) -> {
                if (on) {
                    boolean ok = SystemBarGuard.hide(this, prefs, adb);
                    prefs.setHideSystemNav(ok);
                    if (!ok) {
                        btn.setChecked(false);
                        adbStatus.setText("隐藏失败：App 内 ADB 授权通道尚未接入（S09），请先用上方 PC 端命令授权");
                    }
                } else {
                    doRestore();
                }
            });
        // 独立一键恢复：与开关状态无关，随时可点
        addButton("【一键恢复原厂导航栏】", v -> doRestore());
        backupStatus = addText("");
        addText("PC 端兜底命令：" + SystemBarGuard.fallbackCommand());

        section("3  导航项管理");
        addSwitch("锁定导航栏布局", "开启=禁止拖动防误触；关闭=编辑模式，长按图标左右拖动排序",
            prefs.isLockDock(), (btn, on) -> {
                prefs.setLockDock(on);
                startService(new Intent(this, DockOverlayService.class)
                    .setAction(DockOverlayService.ACTION_EDIT).putExtra("edit", !on));
            });
        List<DockItem> all = prefs.loadAll();
        for (DockItem item : all) {
            final DockItem it = item;
            addSwitch(it.label, it.enabled ? "已显示" : "已隐藏", it.enabled, (btn, on) -> {
                List<DockItem> list = prefs.loadAll();
                for (DockItem x : list) if (x.id.equals(it.id)) x.enabled = on;
                prefs.saveAll(list);
                startService(new Intent(this, DockOverlayService.class)
                    .setAction(DockOverlayService.ACTION_REFRESH));
            });
        }
        addButton("恢复默认项与顺序", v -> {
            prefs.resetItems();
            startService(new Intent(this, DockOverlayService.class)
                .setAction(DockOverlayService.ACTION_REFRESH));
            recreate();
        });

        section("4  空调与车控");
        addSwitch("显示左右温度分区", "空调状态模型：17–33 共 17 档", true, (btn, on) -> { });
        addText("车控数据：第一期使用假数据，BYDAutoAcDevice 待 S09 真机接入");

        section("5  高级");
        addSwitch("开机自启", "BOOT_COMPLETED / QUICKBOOT_POWERON", true, (btn, on) -> { });
        addText("车型：宋 PLUS DMi 2021 款　控制器：13.6　DiLink 3.0");
        addText("版本 0.1.1（S09 前置修复：ADB 状态诚实化 / 权限页降级 / PC 授权通道）");
    }

    /** 悬浮窗权限页：必须带 package: URI，且车机定制 ROM 可能没有该页面，需降级。 */
    private void openOverlaySettings() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            adbStatus.setText("车机未提供悬浮窗权限页面；请用上方 PC 端命令授权：appops set "
                + PKG + " SYSTEM_ALERT_WINDOW allow");
        }
    }
    private void openSystemSettings(String action, String label) {
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            adbStatus.setText("车机未提供「" + label + "」设置页面；请用上方 PC 端命令授权。");
        }
    }

    private void doRestore() {
        boolean ok = SystemBarGuard.restore(this, prefs, adb);
        if (ok) adbStatus.setText("原厂导航栏已恢复");
        else adbStatus.setText("恢复未完成：ADB 授权通道未接入或车机未连接，可用 PC 端兜底命令 "
            + SystemBarGuard.fallbackCommand());
        updateBackupStatus();
    }
    private void updateAdbStatus() {
        PermissionChecker.Result r = PermissionChecker.check(this, adb);
        String s;
        if (r.allGranted) s = "已获得全部权限";
        else {
            StringBuilder sb = new StringBuilder("未获得全部权限，缺少：");
            for (String m : r.missing) sb.append(m).append("；");
            s = sb.toString();
        }
        if (!adb.canExec()) {
            s += "\n说明：无线 ADB 端口已连通，但 App 内授权通道（ADB 协议握手 + RSA 认证）尚未接入（S09）；"
               + "端口连通 ≠ 已授权，本按钮暂不会下发任何命令。请先用上方 PC 端命令一次性授权。";
        }
        if (adbStatus != null) adbStatus.setText(s);
    }
    private void updateBackupStatus() {
        if (backupStatus == null) return;
        backupStatus.setText(prefs.hasBackup()
            ? "原值已备份（隐藏前自动保存），恢复时会写回原值"
            : "当前无原值备份（未修改过系统设置）");
    }
}
