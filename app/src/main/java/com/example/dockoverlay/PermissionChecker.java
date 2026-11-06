package com.example.dockoverlay;

import android.content.Context;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;

/** 「已获得全部权限」= 三项核心权限全部通过；设置页 onResume 与开机后自检。 */
public final class PermissionChecker {
    private PermissionChecker() {}
    public static class Result {
        public final boolean allGranted;
        public final List<String> missing;
        Result(List<String> m) { missing = m; allGranted = m.isEmpty(); }
    }

    /**
     * 0.1.2：权限状态与「App 内无线 ADB 通道」解耦。
     * 权限可由 PC 端 adb 一次性授予并持久生效（pm grant / appops / settings put），
     * 不应因 App 内 Socket 未连接而误报「缺少权限」。sh 参数保留给 S09 查通道状态，本方法不依赖它。
     */
    public static Result check(Context c, ShellExecutor sh) {
        List<String> miss = new ArrayList<>();
        if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            miss.add("WRITE_SECURE_SETTINGS 未授权");
        if (!Settings.canDrawOverlays(c)) miss.add("悬浮窗权限未开启");
        String acc = Settings.Secure.getString(c.getContentResolver(), "enabled_accessibility_services");
        if (acc == null || !acc.contains(c.getPackageName())) miss.add("无障碍服务未开启");
        return new Result(miss);
    }
}
