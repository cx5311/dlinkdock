package com.example.dockoverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(a)) return;
        DockPrefs p = new DockPrefs(c);
        // 安全兜底：若开关已关但存在原值备份，开机自动完整恢复原厂导航栏
        if (!p.isHideSystemNav() && p.hasBackup()) {
            AdbClient adb = new AdbClient();
            if (adb.connect(p.adbHost(), p.adbPort())) SystemBarGuard.restore(c, p, adb);
        }
        c.startForegroundService(new Intent(c, DockOverlayService.class)
            .setAction(DockOverlayService.ACTION_REFRESH));
    }
}
