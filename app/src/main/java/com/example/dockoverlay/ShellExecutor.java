package com.example.dockoverlay;

/** ADB shell 命令下发抽象。真机接入由 AdbClient 实现 ADB 协议后注入。 */
public interface ShellExecutor {
    String exec(String cmd);
    boolean isConnected();
}
