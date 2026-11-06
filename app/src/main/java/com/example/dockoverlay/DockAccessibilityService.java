package com.example.dockoverlay;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/** 按键（performGlobalAction 1/2/3/7）+ 保活；参考 S06 AutoService。 */
public class DockAccessibilityService extends AccessibilityService {
    private static DockAccessibilityService instance;
    public static DockAccessibilityService get() { return instance; }
    @Override public void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }
    @Override public boolean onUnbind(android.content.Intent i) { instance = null; return super.onUnbind(i); }
}
