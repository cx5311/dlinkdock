package com.example.dockoverlay;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;

/** 参考迪友桌面 b4/b.java：仅左右拖动、禁滑动删除；长按拖动由锁定开关控制。 */
public class DockDragCallback extends ItemTouchHelper.SimpleCallback {
    private final DockAdapter adapter;
    private final DockPrefs prefs;
    private final Runnable onSaved;

    public DockDragCallback(DockAdapter adapter, DockPrefs prefs, Runnable onSaved) {
        super(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
        this.adapter = adapter; this.prefs = prefs; this.onSaved = onSaved;
    }
    @Override public boolean isLongPressDragEnabled() { return !prefs.isLockDock(); }
    @Override public boolean onMove(@NonNull RecyclerView rv,
            @NonNull RecyclerView.ViewHolder src, @NonNull RecyclerView.ViewHolder dst) {
        int from = src.getAdapterPosition(), to = dst.getAdapterPosition();
        if (from < 0 || to < 0 || from >= adapter.items().size() || to >= adapter.items().size()) return false;
        // 温度数字是布局元素（参考迪友固定 TextView），不参与拖拽
        if (adapter.items().get(from).type == DockItem.TYPE_TEMPERATURE
                || adapter.items().get(to).type == DockItem.TYPE_TEMPERATURE) return false;
        Collections.swap(adapter.items(), from, to);
        adapter.notifyItemMoved(from, to);
        return true;
    }
    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) { }
    @Override public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        super.clearView(rv, vh);
        DockOrderHelper.applyVisibleOrder(prefs, adapter.items());
        if (onSaved != null) onSaved.run();
    }
}
