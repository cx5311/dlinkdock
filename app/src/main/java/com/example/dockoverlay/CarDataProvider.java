package com.example.dockoverlay;

/** 车控数据抽象。第一期用 FakeCarDataProvider，S09 再接 BYDAutoAcDevice。 */
public interface CarDataProvider {
    boolean setLeftTemperature(int index);   // 17..33，索引语义待 S09 实测
    boolean setRightTemperature(int index);
    boolean setWindLevel(int level);
    int getSoc();
}
