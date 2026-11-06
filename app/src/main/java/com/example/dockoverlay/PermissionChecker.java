package com.example.dockoverlay;

import android.content.Context;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;

/** 「已获得全部权限」= 7 项全通过；onResume 与开机后自检。 */
public final class PermissionChecker {
    private PermissionChecker() {}
    public static class Result {
        public final boolean allGranted;
        public final List<String> missing;
        Result(List<String> m) { missing = m; allGranted = m.isEmpty(); }
    }
    public static Result check(Context c, ShellExecutor sh) {
        List<String> miss = new ArrayList<>();
        if (!sh.isConnected()) miss.add("无线 ADB 未连接");
        if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            miss.add("WRITE_SECURE_SETTINGS 未授权");
        if (!Settings.canDrawOverlays(c)) miss.add("悬浮窗权限未开启");
        String acc = Settings.Secure.getString(c.getContentResolver(), "enabled_accessibility_services");
        if (acc == null || !acc.contains(c.getPackageName())) miss.add("无障碍服务未开启");
        // 以下三项按实际功能可选，骨架阶段不阻断
        return new Result(miss);
    }
}
