package com.example.dockoverlay;

import java.util.List;

/** 把可见列表的顺序写回全量列表（隐藏项保持不变），实现「项开关 + 排序」的统一持久化。 */
public final class DockOrderHelper {
    private DockOrderHelper() {}
    public static void applyVisibleOrder(DockPrefs prefs, List<DockItem> visible) {
        List<DockItem> all = prefs.loadAll();
        for (int i = 0; i < visible.size(); i++) {
            DockItem v = visible.get(i);
            for (DockItem a : all) {
                if (a.id.equals(v.id)) { a.order = i; a.enabled = true; break; }
            }
        }
        for (DockItem a : all) {
            boolean in = false;
            for (DockItem v : visible) if (v.id.equals(a.id)) { in = true; break; }
            if (!in) a.enabled = false;
        }
        prefs.saveAll(all);
    }
}
