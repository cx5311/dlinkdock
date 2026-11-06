package com.example.dockoverlay;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** 参考迪友桌面 z3/n.java：只渲染 enabled 项，按 order 排列；间隔项作为普通 item 参与布局。 */
public class DockAdapter extends RecyclerView.Adapter<DockAdapter.VH> {
    public interface OnClick { void onClick(DockItem item); }
    private final List<DockItem> items;
    private final OnClick click;
    private final float density;

    public DockAdapter(List<DockItem> items, OnClick click, float density) {
        this.items = items; this.click = click; this.density = density;
    }
    public List<DockItem> items() { return items; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11);
        tv.setBackgroundColor(Color.parseColor("#66000000"));
        int w = 30;
        if (viewType == DockItem.TYPE_SPACE_SMALL) w = 15;
        else if (viewType == DockItem.TYPE_SPLIT_LINE) w = 10;
        else if (viewType == DockItem.TYPE_ICON) w = 46;
        tv.setLayoutParams(new RecyclerView.LayoutParams((int)(w*density), (int)(30*density)));
        return new VH(tv);
    }
    @Override public int getItemViewType(int pos) { return items.get(pos).type; }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        DockItem it = items.get(pos);
        String t = it.label;
        if (it.type == DockItem.TYPE_SPLIT_LINE) t = "|";
        else if (it.type != DockItem.TYPE_ICON) t = "";
        else if (t.length() > 4) t = t.substring(0, 4);
        h.tv.setText(t);
        h.tv.setContentDescription(it.label);
        h.tv.setOnClickListener(v -> { if (click != null) click.onClick(it); });
    }
    @Override public int getItemCount() { return items.size(); }
    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        VH(View v) { super(v); tv = (TextView) v; }
    }
}
