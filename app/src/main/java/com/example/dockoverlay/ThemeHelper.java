package com.example.dockoverlay;

import android.content.Context;
import android.content.res.Configuration;

/** 深浅模式：跟随系统 / 强制浅色 / 强制深色。设置页与悬浮底栏共用同一判定。 */
public final class ThemeHelper {
    public static final int FOLLOW = 0;
    public static final int LIGHT = 1;
    public static final int DARK = 2;
    private ThemeHelper() {}

    public static boolean isSystemNight(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isDark(Context c, DockPrefs p) {
        int m = p.themeMode();
        if (m == LIGHT) return false;
        if (m == DARK) return true;
        return isSystemNight(c);
    }

    public static int barBackground(boolean dark)  { return dark ? 0xE62A2A2A : 0xE6F7F7F7; }
    public static int windowBackground(boolean d)  { return d ? 0xFF1E1E1E : 0xFFFFFFFF; }
    public static int titleColor(boolean dark)     { return dark ? 0xFFFFFFFF : 0xFF000000; }
    public static int descColor(boolean dark)      { return dark ? 0xFFAAAAAA : 0xFF757575; }
}
