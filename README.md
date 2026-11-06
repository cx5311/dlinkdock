# DockOverlay — 最小工程骨架（S08）

车型：宋 PLUS DMi 2021 款 / 控制器 13.6 / DiLink 3.0；车机已开无线 ADB。

## 编译
本机已具备 Gradle 8.14 + AGP 8.13.1 + androidx 缓存，可离线构建：
```
set GRADLE_USER_HOME=D:/dev/gradle-home
D:/dev/gradle-home/wrapper/dists/gradle-8.14-all/8mguqc37c200i71ledpgw8n5m/gradle-8.14/bin/gradle.bat --offline -p . assembleDebug
```
产物：`app\build\outputs\apk\debug\app-debug.apk`

## 已实现
- 唯一主页面 `SettingsActivity`（设置主页，5 分区，素排版）
- 17 项导航项（移除 17/18/20；5/14 默认关；19 跳设置主页）
- 每项单独开关 + 锁定/编辑拖拽排序（`RecyclerView wrap_content` 双向居中，项增减自动重排）
- 悬浮栏精确 LayoutParams + 常态 NOT_TOUCHABLE 点击穿透 / 编辑态可交互
- 无线 ADB 连接按钮 + 7 项权限校验 + 「已获得全部权限」提示
- **完整恢复原厂导航栏**：改动前备份原值、独立一键恢复按钮、恢复后读回复核、开机自检兜底

## 完整恢复（重要）
- 设置页常驻「一键恢复原厂导航栏」按钮，与隐藏开关状态无关，随时可点。
- 隐藏前自动 `settings get global policy_control` 并持久化原值（含无障碍相关键）。
- 恢复时：有原值 → `settings put ... <原值>`；无原值 → `settings delete global policy_control`。
- 恢复后 `settings get` 复核；App 数据被清时可用 PC 端兜底：
  `adb shell settings delete global policy_control`
- 进 4S / OTA 前务必先点恢复。

## PC 端一次性授权（当前 0.1.1 的可用通道）

车机无线 ADB 已开、车机 IP `192.168.186.95:5555`。**电脑必须与车机在同一局域网**（当前电脑在
`192.168.111.x`、车机在 `192.168.186.x`，不同网段，需让电脑连车机所在的 WiFi/热点）。
连上后在电脑命令行执行：

```sh
adb connect 192.168.186.95:5555
adb shell pm grant com.example.dockoverlay android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.example.dockoverlay SYSTEM_ALERT_WINDOW allow
adb shell settings get secure enabled_accessibility_services
adb shell settings put secure enabled_accessibility_services com.example.dockoverlay/.DockAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

> ⚠️ `enabled_accessibility_services` 原值若非空，须用 `:` 把本服务**追加**到原值后，不要覆盖车机原有无障碍服务。
> 权限一次授予、持久生效（除非清除 App 数据 / 恢复出厂），之后无需电脑。
> 车机若弹出「是否允许 USB 调试」，勾选「一律允许」并点允许。

复核：`adb shell appops get com.example.dockoverlay SYSTEM_ALERT_WINDOW`、
`adb shell dumpsys package com.example.dockoverlay | grep WRITE_SECURE_SETTINGS`。

## 已知限制（S09 真机接入）
- `AdbClient` 目前只完成 **TCP 端口连通**；ADB 协议 CNXN/AUTH/OPEN/WRTE 握手、RSA 公钥认证与 shell 下发仍是 TODO。
  **端口连通 ≠ 已授权**：`canExec()` 恒为 false，`exec()` 返回 null，`SystemBarGuard` 的 hide/restore 会安全地直接失败，
  不会假装成功、不会写假备份、不会误删 `policy_control` 原值。
- 设置页「打开悬浮窗权限设置」在 0.1.1 已带 `package:` URI 并加 try/catch 降级；车机定制 ROM 若无该页面，会提示改用 PC 端 `appops set`。
- `CarDataProvider` 为假数据；BYDAutoAcDevice 的 12 setter 待接。
- Service 为真机前台服务改造预留；targetSdk 34 后台启动限制需 S09 处理。
- 控制器 13.6 的常量、事件码、`setAcTemperature` 参数语义待实测。
