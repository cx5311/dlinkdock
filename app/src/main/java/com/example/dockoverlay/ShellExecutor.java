package com.example.dockoverlay;

/** ADB shell 命令下发抽象。真机接入由 AdbClient 实现 ADB 协议后注入。 */
public interface ShellExecutor {
    String exec(String cmd);
    boolean isConnected();
    /** 是否具备真实 shell 下发能力。仅 TCP 端口连通不算，必须完成 ADB 协议握手与 RSA 认证。 */
    boolean canExec();
}
