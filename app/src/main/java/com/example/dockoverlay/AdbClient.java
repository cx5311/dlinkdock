package com.example.dockoverlay;

import java.io.IOException;
import java.net.Socket;

/**
 * 无线 ADB 连接骨架（S09 真机接入）。
 * 参考迪友桌面 com.smg.adb.ADBCmd：mServer="localhost:5555"，
 * 自生成 privKey/pubKey 做 ADB RSA 认证，Socket + AdbConnection.connect(10000)，
 * echo '1' 心跳 + 最多 3 次重连。
 * TODO(真机)：实现 ADB 协议 CNXN/AUTH/OPEN/WRTE 握手后，exec() 才能下发 shell。
 */
public final class AdbClient implements ShellExecutor {
    private volatile boolean connected = false;
    private volatile String lastError = "";

    public boolean connect(String host, int port) {
        lastError = "";
        for (int attempt = 0; attempt < 3; attempt++) {
            try (Socket s = new Socket()) {
                s.connect(new java.net.InetSocketAddress(host, port), 3000);
                connected = true;
                return true;
            } catch (IOException e) {
                lastError = "连接失败，请检查ADB调试是否开启（" + e.getMessage() + "）";
            }
        }
        connected = false;
        return false;
    }
    public String lastError() { return lastError; }
    public void disconnect() { connected = false; }
    @Override public boolean isConnected() { return connected; }

    /**
     * S09 之前恒为 false：当前只完成 TCP 端口连通，ADB 协议
     * CNXN/AUTH/OPEN/WRTE + RSA 公钥认证尚未实现，无法下发任何 shell 命令。
     * 端口连通 ≠ 已授权，禁止据此判定授权成功。
     */
    @Override public boolean canExec() { return false; }

    @Override public String exec(String cmd) {
        // TODO(S09)：通过 AdbStream 打开 shell: 通道并读取输出。
        // 必须返回 null 表示「命令未执行」。返回空串会被 SystemBarGuard.hide()
        // 误判为执行成功（r != null），进而假装隐藏成功、写入假备份。
        return null;
    }
}
