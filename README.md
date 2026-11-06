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

## 已知限制（S09 真机接入）
- `AdbClient` 目前只完成 Socket 连接状态机；ADB 协议 CNXN/AUTH/OPEN/WRTE 握手与 shell 下发为 TODO，
  因此 `SystemBarGuard` 的 hide/restore 在真机接入前不会真正下发命令（UI 与状态机可跑通）。
- `CarDataProvider` 为假数据；BYDAutoAcDevice 的 12 setter 待接。
- Service 为真机前台服务改造预留；targetSdk 34 后台启动限制需 S09 处理。
- 控制器 13.6 的常量、事件码、`setAcTemperature` 参数语义待实测。
