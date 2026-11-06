package com.example.dockoverlay;

import android.content.Context;
import android.provider.Settings;

/**
 * 隐藏原厂导航栏 + 【完整恢复】。
 * 重要：迪友桌面只有 settings put global policy_control，没有读取原值/恢复命令。
 * 本类在改动前把原值持久化，恢复时逐条写回；无备份时用 delete 兜底；恢复后读回复核。
 */
public final class SystemBarGuard {
    public static final String KEY_POLICY = "policy_control";
    private static final String EXEMPT =
        ",com.byd.carsettings,com.byd.autovoice,com.byd.bluetoothcall,com.byd.customserver";
    private SystemBarGuard() {}

    /** 改动前读取并保存所有会被修改的系统键值。 */
    public static void capture(Context c, DockPrefs p, ShellExecutor sh) {
        String policy = sh.isConnected() ? sh.exec("settings get global " + KEY_POLICY) : "";
        boolean existed = policy != null && !policy.isEmpty()
            && !"null".equalsIgnoreCase(policy.trim());
        String acc = Settings.Secure.getString(c.getContentResolver(), "enabled_accessibility_services");
        int accEnabled = Settings.Secure.getInt(c.getContentResolver(), "accessibility_enabled", 0);
        p.saveBackup(existed, existed ? policy.trim() : "", acc, accEnabled);
    }

    public static boolean hide(Context c, DockPrefs p, ShellExecutor sh) {
        if (!sh.isConnected()) return false;
        if (!p.hasBackup()) capture(c, p, sh);          // 必须先备份
        String pkg = c.getPackageName();
        String r = sh.exec("settings put global " + KEY_POLICY + " immersive.full=" + pkg + EXEMPT);
        return r != null;
    }

    /** 完整恢复：有原值写回原值，无原值删除键；同时恢复无障碍相关键；最后读回复核。 */
    public static boolean restore(Context c, DockPrefs p, ShellExecutor sh) {
        if (!sh.isConnected()) return false;
        if (p.hasBackup()) {
            if (p.backupPolicyExisted()) {
                sh.exec("settings put global " + KEY_POLICY + " " + p.backupPolicyValue());
            } else {
                sh.exec("settings delete global " + KEY_POLICY);
            }
            sh.exec("settings put secure enabled_accessibility_services " + p.backupAccServices());
            sh.exec("settings put secure accessibility_enabled " + p.backupAccEnabled());
        } else {
            sh.exec("settings delete global " + KEY_POLICY);   // 兜底
        }
        p.setHideSystemNav(false);
        p.clearBackup();
        return verify(sh, p);
    }

    /** 复核：恢复后 policy_control 应等于原值或已不存在。 */
    public static boolean verify(ShellExecutor sh, DockPrefs p) {
        String now = sh.exec("settings get global " + KEY_POLICY);
        String expected = p.backupPolicyExisted() ? p.backupPolicyValue() : "null";
        return now != null && (now.trim().equalsIgnoreCase(expected)
            || now.trim().equalsIgnoreCase("null"));
    }

    /** 兜底命令（供 README / PC 端 adb 使用，App 数据被清时仍可恢复）。 */
    public static String fallbackCommand() {
        return "adb shell settings delete global policy_control";
    }
}
