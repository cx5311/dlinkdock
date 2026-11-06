package com.example.dockoverlay;

import android.content.Intent;
import android.graphics.Color;
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

/** App 唯一主页面：设置主页（5 分区，素排版）。第 19 项「设置」即跳转到此。 */
public class SettingsActivity extends android.app.Activity {
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
        addButton("打开系统无障碍设置", v -> startActivity(
            new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        addButton("打开悬浮窗权限设置", v -> startActivity(
            new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));

        section("2  导航栏显示");
        addSwitch("隐藏原厂导航栏", "通过 ADB 下发 policy_control；改动前自动备份原值",
            prefs.isHideSystemNav(), (btn, on) -> {
                if (on) {
                    boolean ok = SystemBarGuard.hide(this, prefs, adb);
                    prefs.setHideSystemNav(ok);
                    if (!ok) { btn.setChecked(false); adbStatus.setText("请先连接无线 ADB 再隐藏原厂导航栏"); }
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
        addText("版本 0.1.0（S08 骨架）");
    }
    private void doRestore() {
        boolean ok = SystemBarGuard.restore(this, prefs, adb);
        if (ok) adbStatus.setText("原厂导航栏已恢复");
        else adbStatus.setText("恢复未完成：请先连接无线 ADB，或使用 PC 端兜底命令 "
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
        if (adbStatus != null) adbStatus.setText(s);
    }
    private void updateBackupStatus() {
        if (backupStatus == null) return;
        backupStatus.setText(prefs.hasBackup()
            ? "原值已备份（隐藏前自动保存），恢复时会写回原值"
            : "当前无原值备份（未修改过系统设置）");
    }
}
