package com.example.dockoverlay;

public class FakeCarDataProvider implements CarDataProvider {
    private int left = 22, right = 22, wind = 3;
    @Override public boolean setLeftTemperature(int i) { left = i; return true; }
    @Override public boolean setRightTemperature(int i) { right = i; return true; }
    @Override public boolean setWindLevel(int l) { wind = l; return true; }
    @Override public int getSoc() { return 50; }
}
