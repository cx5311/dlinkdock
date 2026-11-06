package com.example.dockoverlay;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 参考迪友桌面 z3/n.java + navigation_bar_default_view.xml：只渲染 enabled 项，按 order 排列。
 * 图标使用迪友桌面 nav_common_* 系列 vector（b=深色图标配浅底，w=白色图标配深底）。
 * 0.1.4：按迪友布局在「温度 − / 温度 +」之间插入温度数字项。
 */
public class DockAdapter extends RecyclerView.Adapter<DockAdapter.VH> {
    public interface OnClick { void onClick(DockItem item); }
    private final List<DockItem> items;
    private final OnClick click;
    private final float density;
    private final int barHeightDp;
    private boolean dark = false;
    private int leftTemp = 22, rightTemp = 22;

    public DockAdapter(List<DockItem> items, OnClick click, float density, int barHeightDp) {
        this.items = items; this.click = click; this.density = density;
        this.barHeightDp = barHeightDp;
    }
    public List<DockItem> items() { return items; }
    public void setDark(boolean d) { dark = d; }
    public void setTemperatures(int l, int r) { leftTemp = l; rightTemp = r; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int w = (int)(barHeightDp * 1.35);
        if (viewType == DockItem.TYPE_SPACE_SMALL) w = w / 2;
        else if (viewType == DockItem.TYPE_SPLIT_LINE) w = w / 3;
        else if (viewType == DockItem.TYPE_TEMPERATURE) w = (int)(barHeightDp * 1.15);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
            (int)(w * density), (int)(barHeightDp * density));

        if (viewType == DockItem.TYPE_TEMPERATURE) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, barHeightDp * 0.34f);
            tv.setTextColor(ThemeHelper.titleColor(dark));
            return new VH(tv, null, tv);
        }
        ImageView iv = new ImageView(parent.getContext());
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int px = (int)(w * 0.22 * density), py = (int)(barHeightDp * 0.10 * density);
        iv.setPadding(px, py, px, py);
        return new VH(iv, iv, null);
    }
    @Override public int getItemViewType(int pos) { return items.get(pos).type; }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        DockItem it = items.get(pos);
        if (it.type == DockItem.TYPE_TEMPERATURE) {
            int t = "_temp_right".equals(it.id) ? rightTemp : leftTemp;
            h.tv.setText(t + "°C");
            h.tv.setTextColor(ThemeHelper.titleColor(dark));
            h.tv.setContentDescription(it.label + " " + t + " 度");
            h.itemView.setOnClickListener(null);
            return;
        }
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
        h.itemView.setOnClickListener(v -> {
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
        final View itemView;
        final ImageView iv;
        final TextView tv;
        VH(View v, ImageView iv, TextView tv) { super(v); this.itemView = v; this.iv = iv; this.tv = tv; }
    }
}
