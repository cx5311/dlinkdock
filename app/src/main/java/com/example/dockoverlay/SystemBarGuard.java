package com.example.dockoverlay;

import android.content.Context;
import android.provider.Settings;

/**
 * 隐藏原厂导航栏 + 【完整恢复】。
 *
 * 0.1.3 改造：PC 端 adb 已授予 WRITE_SECURE_SETTINGS，App 可直接用 Settings API 读写，
 * 不再依赖未实现的 ADB 协议栈（ShellExecutor 保留为 S09 的备用通道）。
 * 安全约束：改动前备份原值 → 恢复时写回原值/删键 → 读回复核 → 只有成功才清备份。
 */
public final class SystemBarGuard {
    public static final String KEY_POLICY = "policy_control";
    private static final String KEY_ACC = "enabled_accessibility_services";
    private static final String KEY_ACC_EN = "accessibility_enabled";
    private static final String EXEMPT =
        ",com.byd.carsettings,com.byd.autovoice,com.byd.bluetoothcall,com.byd.customserver";
    private SystemBarGuard() {}

    private static boolean canWriteSecure(Context c) {
        return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    public static boolean available(Context c, ShellExecutor sh) {
        return canWriteSecure(c) || (sh != null && sh.canExec());
    }

    private static String readPolicy(Context c, ShellExecutor sh) {
        if (canWriteSecure(c)) return Settings.Global.getString(c.getContentResolver(), KEY_POLICY);
        if (sh != null && sh.canExec()) return sh.exec("settings get global " + KEY_POLICY);
        return null;
    }
    /** value == null 表示删除该键（SettingsProvider 对 null 值执行 delete）。 */
    private static void writePolicy(Context c, ShellExecutor sh, String value) {
        if (canWriteSecure(c)) {
            Settings.Global.putString(c.getContentResolver(), KEY_POLICY, value);
        } else if (sh != null && sh.canExec()) {
            sh.exec(value == null ? "settings delete global " + KEY_POLICY
                                  : "settings put global " + KEY_POLICY + " " + value);
        }
    }
    private static void writeSecure(Context c, ShellExecutor sh, String key, String value) {
        if (canWriteSecure(c)) Settings.Secure.putString(c.getContentResolver(), key, value);
        else if (sh != null && sh.canExec()) sh.exec("settings put secure " + key + " " + value);
    }

    /** 改动前读取并保存所有会被修改的系统键值。 */
    public static void capture(Context c, DockPrefs p, ShellExecutor sh) {
        String policy = readPolicy(c, sh);
        boolean existed = policy != null && !policy.isEmpty()
            && !"null".equalsIgnoreCase(policy.trim());
        String acc = Settings.Secure.getString(c.getContentResolver(), KEY_ACC);
        int accEn = Settings.Secure.getInt(c.getContentResolver(), KEY_ACC_EN, 0);
        p.saveBackup(existed, existed ? policy.trim() : "", acc, accEn);
    }

    public static boolean hide(Context c, DockPrefs p, ShellExecutor sh) {
        if (!available(c, sh)) return false;
        if (!p.hasBackup()) capture(c, p, sh);            // 必须先备份
        String val = "immersive.full=" + c.getPackageName() + EXEMPT;
        writePolicy(c, sh, val);
        String now = readPolicy(c, sh);                   // 写后读回复核
        return now != null && now.trim().equalsIgnoreCase(val);
    }

    /** 完整恢复：有原值写回原值，无原值删除键；同时恢复无障碍相关键；最后读回复核。 */
    public static boolean restore(Context c, DockPrefs p, ShellExecutor sh) {
        if (!available(c, sh)) return false;
        String expected;
        if (p.hasBackup()) {
            if (p.backupPolicyExisted()) {
                writePolicy(c, sh, p.backupPolicyValue());
                expected = p.backupPolicyValue();
            } else {
                writePolicy(c, sh, null);
                expected = null;
            }
            writeSecure(c, sh, KEY_ACC, p.backupAccServices());
            writeSecure(c, sh, KEY_ACC_EN, String.valueOf(p.backupAccEnabled()));
        } else {
            writePolicy(c, sh, null);                      // 兜底
            expected = null;
        }
        boolean ok = verify(c, sh, expected);             // 必须在清备份之前复核
        if (ok) {                                         // 只有真正成功才清备份，失败保留便于重试
            p.setHideSystemNav(false);
            p.clearBackup();
        }
        return ok;
    }

    public static boolean verify(Context c, ShellExecutor sh, String expected) {
        String now = readPolicy(c, sh);
        if (expected == null) return now == null || now.isEmpty() || "null".equalsIgnoreCase(now.trim());
        return now != null && now.trim().equalsIgnoreCase(expected);
    }

    /** 兜底命令（供 README / PC 端 adb 使用，App 数据被清时仍可恢复）。 */
    public static String fallbackCommand() {
        return "adb shell settings delete global policy_control";
    }
    public static String hideCommand(Context c) {
        return "adb shell settings put global policy_control immersive.full="
            + c.getPackageName() + EXEMPT;
    }
}
