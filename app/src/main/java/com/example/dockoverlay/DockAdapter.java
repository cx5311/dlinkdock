package com.example.dockoverlay;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 参考迪友桌面 z3/n.java + navigation_bar_default_view.xml：只渲染 enabled 项，按 order 排列。
 * 图标直接使用迪友桌面 nav_common_* 系列 vector（b=深色图标配浅底，w=白色图标配深底）。
 */
public class DockAdapter extends RecyclerView.Adapter<DockAdapter.VH> {
    public interface OnClick { void onClick(DockItem item); }
    private final List<DockItem> items;
    private final OnClick click;
    private final float density;
    private boolean dark = false;
    private final int barHeightDp;

    public DockAdapter(List<DockItem> items, OnClick click, float density, int barHeightDp) {
        this.items = items; this.click = click; this.density = density;
        this.barHeightDp = barHeightDp;
    }
    public List<DockItem> items() { return items; }
    public void setDark(boolean d) { dark = d; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(parent.getContext());
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        // 参考迪友桌面：item 宽=栏高、图标 1:1 占栏高 0.8；此处整体放大并把间隔拉开
        int w = (int)(barHeightDp * 1.35);              // 48dp 栏高 -> 64dp item
        if (viewType == DockItem.TYPE_SPACE_SMALL) w = w / 2;
        else if (viewType == DockItem.TYPE_SPLIT_LINE) w = w / 3;
        iv.setLayoutParams(new RecyclerView.LayoutParams((int)(w*density), (int)(barHeightDp*density)));
        int px = (int)(w * 0.22 * density), py = (int)(barHeightDp * 0.10 * density);
        iv.setPadding(px, py, px, py);
        return new VH(iv);
    }
    @Override public int getItemViewType(int pos) { return items.get(pos).type; }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        DockItem it = items.get(pos);
        if (it.type == DockItem.TYPE_ICON) {
            h.iv.setImageResource(iconOf(it.id, dark));
            h.iv.setContentDescription(it.label);
        } else if (it.type == DockItem.TYPE_SPLIT_LINE) {
            h.iv.setImageResource(dark
                ? R.drawable.nav_free_item_split_line_w_icon
                : R.drawable.nav_free_item_split_line_b_icon);
            h.iv.setContentDescription("");
        } else {
            h.iv.setImageDrawable(null);
        }
        h.iv.setOnClickListener(v -> {
            if (click != null && it.type == DockItem.TYPE_ICON) click.onClick(it);
        });
    }
    @Override public int getItemCount() { return items.size(); }

    /** 17 项 → 迪友桌面图标；深浅两套（对称项复用同一箭头，靠位置区分左右）。 */
    static int iconOf(String id, boolean d) {
        switch (id) {
            case "back": return d ? R.drawable.nav_common_back_w_icon : R.drawable.nav_common_back_b_icon;
            case "home": return d ? R.drawable.nav_common_home_w_icon : R.drawable.nav_common_home_b_icon;
            case "menu": return d ? R.drawable.nav_common_menu_w_icon : R.drawable.nav_common_menu_b_icon;
            case "ac":   return d ? R.drawable.nav_common_ac_switch_w_icon : R.drawable.nav_common_ac_switch_b_icon;
            case "left_seat":
                return d ? R.drawable.nav_common_left_seat_vent_w_icon : R.drawable.nav_common_left_seat_vent_b_icon;
            case "right_seat":
                return d ? R.drawable.nav_common_right_seat_vent_w_icon : R.drawable.nav_common_right_seat_vent_b_icon;
            case "left_temp_down":
            case "right_temp_down":
            case "wind_down":
                return d ? R.drawable.nav_common_left_arrow_w_icon : R.drawable.nav_common_left_arrow_b_icon;
            case "left_temp_up":
            case "right_temp_up":
            case "wind_up":
                return d ? R.drawable.nav_common_right_arrow_w_icon : R.drawable.nav_common_right_arrow_b_icon;
            case "wind_mode":
                return d ? R.drawable.nav_common_ac_wind_mode_0_w_icon : R.drawable.nav_common_ac_wind_mode_0_b_icon;
            case "wind_level":
                return d ? R.drawable.nav_common_ac_wind_level_3_w_icon : R.drawable.nav_common_ac_wind_level_3_b_icon;
            case "cycle_mode":
                return d ? R.drawable.nav_common_cycle_mode_inner_w_icon : R.drawable.nav_common_cycle_mode_inner_b_icon;
            case "lock_screen":
                return d ? R.drawable.nav_common_lock_screen_w_icon : R.drawable.nav_common_lock_screen_b_icon;
            case "settings":
                return d ? R.drawable.nav_common_setup_w_icon : R.drawable.nav_common_setup_b_icon;
            default:
                return d ? R.drawable.nav_common_setup_w_icon : R.drawable.nav_common_setup_b_icon;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView iv;
        VH(View v) { super(v); iv = (ImageView) v; }
    }
}
