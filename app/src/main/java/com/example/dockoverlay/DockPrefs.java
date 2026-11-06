package com.example.dockoverlay;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DockPrefs {
    private static final String P = "dock_prefs";
    public static final String K_ITEMS = "nav_items";
    public static final String K_LOCK = "lock_dock";
    public static final String K_HIDE_SYS_NAV = "hide_system_nav";
    public static final String K_ADB_HOST = "adb_host";
    public static final String K_ADB_PORT = "adb_port";
    // 完整恢复：改动前的系统原值，持久化保存
    public static final String K_BACK_POLICY_EXISTED = "bak_policy_existed";
    public static final String K_BACK_POLICY_VALUE = "bak_policy_value";
    public static final String K_BACK_ACC_SERVICES = "bak_acc_services";
    public static final String K_BACK_ACC_ENABLED = "bak_acc_enabled";
    public static final String K_BACK_SAVED = "bak_saved";

    private final SharedPreferences sp;
    public DockPrefs(Context c) { sp = c.getApplicationContext().getSharedPreferences(P, Context.MODE_PRIVATE); }

    public boolean isLockDock() { return sp.getBoolean(K_LOCK, true); }   // 默认锁定 ON
    public void setLockDock(boolean v) { sp.edit().putBoolean(K_LOCK, v).apply(); }
    public boolean isHideSystemNav() { return sp.getBoolean(K_HIDE_SYS_NAV, false); }
    public void setHideSystemNav(boolean v) { sp.edit().putBoolean(K_HIDE_SYS_NAV, v).apply(); }
    public String adbHost() { return sp.getString(K_ADB_HOST, "localhost"); }
    public int adbPort() { return sp.getInt(K_ADB_PORT, 5555); }
    public void setAdb(String h, int p) { sp.edit().putString(K_ADB_HOST, h).putInt(K_ADB_PORT, p).apply(); }

    public List<DockItem> visibleItems() {
        List<DockItem> all = loadAll();
        List<DockItem> v = new ArrayList<>();
        for (DockItem i : all) if (i.enabled) v.add(i);
        Collections.sort(v, Comparator.comparingInt(o -> o.order));
        return v;
    }
    public List<DockItem> loadAll() {
        String s = sp.getString(K_ITEMS, null);
        List<DockItem> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return DockCatalog.defaults();
        for (String part : s.split(",")) {
            if (part.isEmpty()) continue;
            String[] p = part.split(":");
            out.add(DockItem.decode(part, DockCatalog.labelOf(p[0])));
        }
        return out;
    }
    public void saveAll(List<DockItem> items) {
        StringBuilder sb = new StringBuilder();
        for (DockItem i : items) { if (sb.length() > 0) sb.append(','); sb.append(i.encode()); }
        sp.edit().putString(K_ITEMS, sb.toString()).apply();
    }
    public void resetItems() { sp.edit().remove(K_ITEMS).apply(); }

    public boolean hasBackup() { return sp.getBoolean(K_BACK_SAVED, false); }
    public void saveBackup(boolean existed, String policy, String accServices, int accEnabled) {
        sp.edit().putBoolean(K_BACK_SAVED, true)
            .putBoolean(K_BACK_POLICY_EXISTED, existed)
            .putString(K_BACK_POLICY_VALUE, policy == null ? "" : policy)
            .putString(K_BACK_ACC_SERVICES, accServices == null ? "" : accServices)
            .putInt(K_BACK_ACC_ENABLED, accEnabled).apply();
    }
    public boolean backupPolicyExisted() { return sp.getBoolean(K_BACK_POLICY_EXISTED, false); }
    public String backupPolicyValue() { return sp.getString(K_BACK_POLICY_VALUE, ""); }
    public String backupAccServices() { return sp.getString(K_BACK_ACC_SERVICES, ""); }
    public int backupAccEnabled() { return sp.getInt(K_BACK_ACC_ENABLED, 0); }
    public void clearBackup() { sp.edit().remove(K_BACK_SAVED).apply(); }
}
