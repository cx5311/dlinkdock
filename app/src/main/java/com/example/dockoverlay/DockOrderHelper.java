package com.example.dockoverlay;

import java.util.List;

/** 把可见列表的顺序写回全量列表（隐藏项保持不变），实现「项开关 + 排序」的统一持久化。 */
public final class DockOrderHelper {
    private DockOrderHelper() {}
    public static void applyVisibleOrder(DockPrefs prefs, List<DockItem> visible) {
        List<DockItem> all = prefs.loadAll();
        int order = 0;
        for (DockItem v : visible) {
            if (v.id.startsWith("_")) continue;   // 温度数字等布局元素不参与持久化
            for (DockItem a : all) {
                if (a.id.equals(v.id)) { a.order = order; a.enabled = true; break; }
            }
            order++;
        }
        for (DockItem a : all) {
            boolean in = false;
            for (DockItem v : visible) if (v.id.equals(a.id)) { in = true; break; }
            if (!in) a.enabled = false;
        }
        prefs.saveAll(all);
    }
}
