package com.example.dockoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** 悬浮底栏：参数来自 S05 实测（y4/c、y4/f、a5/d）。0.1.2 起为前台服务，适配 Android 10 后台限制。 */
public class DockOverlayService extends Service {
    private static final String TAG = "DockOverlay";
    public static final String ACTION_REFRESH = "com.example.dockoverlay.REFRESH";
    public static final String ACTION_EDIT = "com.example.dockoverlay.EDIT";
    private static final String CHANNEL_ID = "dock_overlay";
    private static final int NOTIF_ID = 1;

    private WindowManager wm;
    private View dockView;
    private DockPrefs prefs;
    private boolean editMode = false;
    private DockAdapter adapter;
    private int lastBarHeight = 0;
    // 0.1.3：车控本地状态（S09 接 BYDAutoAcDevice 后替换为真实读写）
    private int leftTemp = 22, rightTemp = 22, wind = 3;
    private int windMode = 0, leftSeat = 0, rightSeat = 0;
    private boolean cycleInner = true;

    @Override public void onCreate() {
        super.onCreate();
        prefs = new DockPrefs(this);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        startAsForeground();   // startForegroundService 后必须尽快调用
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "底栏服务", NotificationManager.IMPORTANCE_MIN);
            ch.setDescription("保持底部导航栏运行");
            nm.createNotificationChannel(ch);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, SettingsActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("底栏已启用")
            .setContentText("点击打开底栏设置")
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
        startForeground(NOTIF_ID, n);
        Log.d(TAG, "foreground service started");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) refresh();
        if (intent != null && ACTION_EDIT.equals(intent.getAction()))
            setEditMode(intent.getBooleanExtra("edit", false));
        else if (dockView == null) build();
        return START_STICKY;
    }

    private void build() {
        if (dockView != null) return;
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "canDrawOverlays=false, skip addView");
            return;
        }
        dockView = LayoutInflater.from(this).inflate(R.layout.dock_bar, null);
        RecyclerView rv = dockView.findViewById(R.id.dock_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<DockItem> list = prefs.visibleItems();
        lastBarHeight = prefs.barHeightDp();
        adapter = new DockAdapter(list, this::onItemClick,
            getResources().getDisplayMetrics().density, lastBarHeight);
        boolean dark = ThemeHelper.isDark(this, prefs);
        adapter.setDark(dark);
        rv.setAdapter(adapter);
        new ItemTouchHelper(new DockDragCallback(adapter, prefs, null)).attachToRecyclerView(rv);
        dockView.setBackgroundColor(ThemeHelper.barBackground(dark));
        WindowManager.LayoutParams p = buildLp(false);
        wm.addView(dockView, p);
        Log.d(TAG, "addView OK: items=" + list.size() + " h=" + p.height + " y=" + p.y);
    }

    private WindowManager.LayoutParams buildLp(boolean edit) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;      // 2038
        p.format = PixelFormat.TRANSLUCENT;                                // -3
        p.gravity = Gravity.TOP;                                           // 48，配合 y 贴底
        p.width = WindowManager.LayoutParams.MATCH_PARENT;
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        int h = (int) (prefs.barHeightDp() * getResources().getDisplayMetrics().density);
        p.height = h;                                                      // 默认 48dp，可调 40/48/56
        p.y = dm.heightPixels - h;
        int f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
              | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
              | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
              | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
              | WindowManager.LayoutParams.FLAG_FULLSCREEN
              | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        // 0.1.3：底栏按钮必须可点击，常态不再加 NOT_TOUCHABLE。
        // 仅靠 NOT_TOUCH_MODAL 保证「栏外」触摸穿透到下层界面，栏内正常响应。
        p.flags = f;
        return p;
    }

    /** 编辑态切换：去掉/加回 NOT_TOUCHABLE（参考 b5/e.java）。 */
    public void setEditMode(boolean edit) {
        if (dockView == null) { build(); if (dockView == null) return; }
        editMode = edit;
        wm.updateViewLayout(dockView, buildLp(edit));
        refresh();
    }

    public void refresh() {
        if (dockView == null) { build(); return; }
        if (prefs.barHeightDp() != lastBarHeight) {     // 栏高变化需重建 item 尺寸
            wm.removeView(dockView);
            dockView = null;
            build();
            return;
        }
        boolean dark = ThemeHelper.isDark(this, prefs);
        dockView.setBackgroundColor(ThemeHelper.barBackground(dark));
        if (adapter != null) { adapter.setDark(dark); adapter.notifyDataSetChanged(); }
        wm.updateViewLayout(dockView, buildLp(editMode));   // 旋转屏 / 深浅切换后重算 y
    }

    /** 系统深浅模式切换时即时刷新底栏（「跟随系统」选项下生效）。 */
    @Override public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged night="
            + (newConfig.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK));
        refresh();
    }

    private void onItemClick(DockItem item) {
        Log.d(TAG, "click: " + item.id);
        switch (item.id) {
            case "settings":                    // 第 19 项：打开 App 设置主页
                startActivity(new Intent(this, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            case "back":                        // GLOBAL_ACTION_BACK
                globalAction(1, "返回"); return;
            case "home":                        // GLOBAL_ACTION_HOME
                globalAction(2, "主页"); return;
            case "menu":                        // GLOBAL_ACTION_RECENTS（车机菜单近似）
                globalAction(3, "菜单／最近任务"); return;
            case "lock_screen":                 // GLOBAL_ACTION_LOCK_SCREEN (API 28+)
                globalAction(8, "锁屏"); return;
            case "ac":
                openAcPanel(); return;
            default:
                carAction(item);
        }
    }

    private void globalAction(int action, String label) {
        DockAccessibilityService s = DockAccessibilityService.get();
        if (s == null) { toast("无障碍服务未运行，" + label + "不可用"); return; }
        boolean ok = s.performGlobalAction(action);
        Log.d(TAG, "globalAction " + action + " -> " + ok);
        if (!ok) toast(label + "执行失败");
    }

    /** 空调面板：优先比亚迪空调应用，其次车机设置，都没有则给出明确提示。 */
    private void openAcPanel() {
        android.content.pm.PackageManager pm = getPackageManager();
        Intent i = pm.getLaunchIntentForPackage("com.byd.airconditioning");
        if (i == null) i = pm.getLaunchIntentForPackage("com.byd.carsettings");
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception e) { toast("空调面板拉起失败：" + e.getMessage()); }
        } else {
            toast("空调开关／面板（未找到车机空调应用，S09 接 BYDAutoAcDevice）");
        }
    }

    /** 车控项：先给本地状态 + 可见反馈；S09 换成 BYDAutoAcDevice 的真实读写与回调。 */
    private void carAction(DockItem it) {
        String msg;
        switch (it.id) {
            case "left_temp_down":  leftTemp = Math.max(17, leftTemp - 1); msg = "左温度 " + leftTemp + "℃"; break;
            case "left_temp_up":    leftTemp = Math.min(33, leftTemp + 1); msg = "左温度 " + leftTemp + "℃"; break;
            case "right_temp_down": rightTemp = Math.max(17, rightTemp - 1); msg = "右温度 " + rightTemp + "℃"; break;
            case "right_temp_up":   rightTemp = Math.min(33, rightTemp + 1); msg = "右温度 " + rightTemp + "℃"; break;
            case "wind_down":       wind = Math.max(0, wind - 1); msg = "风量 " + wind + " 档"; break;
            case "wind_up":         wind = Math.min(7, wind + 1); msg = "风量 " + wind + " 档"; break;
            case "wind_level":      msg = "当前风量 " + wind + " 档"; break;
            case "wind_mode":       windMode = (windMode + 1) % 4; msg = "出风模式 " + (windMode + 1); break;
            case "cycle_mode":      cycleInner = !cycleInner; msg = cycleInner ? "内循环" : "外循环"; break;
            case "left_seat":       leftSeat = (leftSeat + 1) % 4; msg = "主驾座椅 " + leftSeat + " 档"; break;
            case "right_seat":      rightSeat = (rightSeat + 1) % 4; msg = "副驾座椅 " + rightSeat + " 档"; break;
            default:                msg = it.label + "（车控接入待 S09）"; break;
        }
        toast(msg);
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroy() {
        if (dockView != null && wm != null) { wm.removeView(dockView); dockView = null; }
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
