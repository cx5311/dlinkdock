package com.example.dockoverlay;

public class DockItem {
    public static final int TYPE_ICON = 0;
    public static final int TYPE_SPACE_BIG = 1;    // 30dp，参考 nav_free_item_big_space
    public static final int TYPE_SPACE_SMALL = 2;  // 15dp
    public static final int TYPE_SPLIT_LINE = 3;   // 10dp 竖线

    public final String id;
    public final String label;
    public int type;
    public boolean enabled;
    public int order;

    public DockItem(String id, String label, int type, boolean enabled, int order) {
        this.id = id; this.label = label; this.type = type;
        this.enabled = enabled; this.order = order;
    }
    public String encode() { return id + ":" + type + ":" + (enabled ? 1 : 0) + ":" + order; }
    public static DockItem decode(String s, String label) {
        String[] p = s.split(":");
        return new DockItem(p[0], label, Integer.parseInt(p[1]), "1".equals(p[2]), Integer.parseInt(p[3]));
    }
}
