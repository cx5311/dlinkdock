package com.example.dockoverlay;

import java.util.ArrayList;
import java.util.List;

/** 用户 2026-11-06 12:50 定稿：移除 17/18/20，共 17 项；5/14 初始默认关；19 跳设置主页。 */
public final class DockCatalog {
    public static final String ACTION_SETTINGS = "settings";
    private DockCatalog() {}

    private static void add(List<DockItem> l, String id, String label, boolean on, int order) {
        l.add(new DockItem(id, label, DockItem.TYPE_ICON, on, order));
    }
    public static List<DockItem> defaults() {
        List<DockItem> l = new ArrayList<>();
        add(l, "back", "返回", true, 0);
        add(l, "home", "主页", true, 1);
        add(l, "menu", "菜单／应用列表", true, 2);
        add(l, "ac", "空调开关／面板", true, 3);
        add(l, "left_seat", "主驾座椅通风／加热", false, 4);   // 默认关
        add(l, "left_temp_down", "左温度 −", true, 5);
        add(l, "left_temp_up", "左温度 +", true, 6);
        add(l, "wind_down", "风量 −", true, 7);
        add(l, "wind_mode", "出风模式", true, 8);
        add(l, "wind_level", "风量档位", true, 9);
        add(l, "wind_up", "风量 +", true, 10);
        add(l, "right_temp_down", "右温度 −", true, 11);
        add(l, "right_temp_up", "右温度 +", true, 12);
        add(l, "right_seat", "副驾座椅通风／加热", false, 13); // 默认关
        add(l, "cycle_mode", "内外循环", true, 14);
        add(l, "lock_screen", "锁屏／息屏", true, 15);
        add(l, ACTION_SETTINGS, "设置", true, 16);
        return l;
    }
    public static String labelOf(String id) {
        for (DockItem i : defaults()) if (i.id.equals(id)) return i.label;
        return id;
    }
}
