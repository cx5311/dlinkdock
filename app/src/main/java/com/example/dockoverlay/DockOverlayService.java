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
        adapter = new DockAdapter(list, this::onItemClick, getResources().getDisplayMetrics().density);
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
        int h = (int) (30 * getResources().getDisplayMetrics().density);
        p.height = h;                                                      // dp_30
        p.y = dm.heightPixels - h;
        int f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
              | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
              | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
              | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
              | WindowManager.LayoutParams.FLAG_FULLSCREEN
              | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (!edit) f |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;      // 常态点击穿透
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
        if (DockCatalog.ACTION_SETTINGS.equals(item.id)) {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        DockAccessibilityService s = DockAccessibilityService.get();
        if (s != null) {
            if ("back".equals(item.id)) s.performGlobalAction(1);
            else if ("home".equals(item.id)) s.performGlobalAction(2);
        }
        // 空调等车控项：S09 接 CarDataProvider / BYDAutoAcDevice
    }

    @Override public void onDestroy() {
        if (dockView != null && wm != null) { wm.removeView(dockView); dockView = null; }
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
