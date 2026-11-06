package com.example.dockoverlay;

import android.content.Context;
import android.provider.Settings;

/**
 * 保证本 App 的无障碍服务始终在 enabled_accessibility_services 中。
 *
 * 真机实测：覆盖安装、车机第三方桌面、系统配置变化都会把我们的服务从列表移除，
 * 且 TYPE_ACCESSIBILITY_OVERLAY 只有「已启用的无障碍服务所属 UID」才能添加窗口。
 * App 已通过 PC 端 adb 获得 WRITE_SECURE_SETTINGS，可自行写回（追加而非覆盖）。
 */
public final class AccessibilityGuard {
    private static final String KEY_LIST = "enabled_accessibility_services";
    private static final String KEY_ENABLED = "accessibility_enabled";
    private AccessibilityGuard() {}

    public static String serviceName(Context c) {
        return c.getPackageName() + "/.DockAccessibilityService";
    }

    public static boolean isEnabled(Context c) {
        String cur = Settings.Secure.getString(c.getContentResolver(), KEY_LIST);
        return cur != null && cur.contains(serviceName(c));
    }

    /** 追加本服务（保留原有服务，用 : 分隔），并确保 accessibility_enabled=1。 */
    public static boolean ensureEnabled(Context c) {
        if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return isEnabled(c);
        if (isEnabled(c)) {
            Settings.Secure.putInt(c.getContentResolver(), KEY_ENABLED, 1);
            return true;
        }
        String cur = Settings.Secure.getString(c.getContentResolver(), KEY_LIST);
        String base = (cur == null || cur.isEmpty() || "null".equalsIgnoreCase(cur.trim())) ? "" : cur.trim();
        String next = base.isEmpty() ? serviceName(c) : base + ":" + serviceName(c);
        boolean ok = Settings.Secure.putString(c.getContentResolver(), KEY_LIST, next);
        Settings.Secure.putInt(c.getContentResolver(), KEY_ENABLED, 1);
        return ok && isEnabled(c);
    }
}
