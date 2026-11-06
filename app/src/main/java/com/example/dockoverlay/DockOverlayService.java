package com.example.dockoverlay;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** 悬浮底栏：参数全部来自 S05 实测（y4/c、y4/f、a5/d）。 */
public class DockOverlayService extends Service {
    public static final String ACTION_REFRESH = "com.example.dockoverlay.REFRESH";
    public static final String ACTION_EDIT = "com.example.dockoverlay.EDIT";
    private WindowManager wm;
    private View dockView;
    private WindowManager.LayoutParams lp;
    private DockPrefs prefs;
    private boolean editMode = false;

    @Override public void onCreate() {
        super.onCreate();
        prefs = new DockPrefs(this);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) refresh();
        if (intent != null && ACTION_EDIT.equals(intent.getAction()))
            setEditMode(intent.getBooleanExtra("edit", false));
        else if (dockView == null) build();
        return START_STICKY;
    }
    private void build() {
        if (!Settings.canDrawOverlays(this) || dockView != null) return;
        dockView = LayoutInflater.from(this).inflate(R.layout.dock_bar, null);
        RecyclerView rv = dockView.findViewById(R.id.dock_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<DockItem> list = prefs.visibleItems();
        DockAdapter adapter = new DockAdapter(list, this::onItemClick, getResources().getDisplayMetrics().density);
        rv.setAdapter(adapter);
        new ItemTouchHelper(new DockDragCallback(adapter, prefs, null)).attachToRecyclerView(rv);
        lp = buildLp(false);
        wm.addView(dockView, lp);
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
        WindowManager.LayoutParams p = buildLp(edit);
        wm.updateViewLayout(dockView, p);
        refresh();
    }
    public void refresh() {
        if (dockView == null) { build(); return; }
        RecyclerView rv = dockView.findViewById(R.id.dock_recycler);
        RecyclerView.Adapter<?> a = rv.getAdapter();
        if (a != null) a.notifyDataSetChanged();   // 项增减后 wrap_content 自动重排并居中
        WindowManager.LayoutParams p = buildLp(editMode);
        wm.updateViewLayout(dockView, p);           // 旋转屏后重算 y
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
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
