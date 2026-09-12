# 架构说明

## 事件链路

1. `AppWatchAccessibilityService` 只订阅 `TYPE_WINDOW_STATE_CHANGED`。
2. 事件中只提取 `packageName` 和本机时间；非目标包名只参与瞬时判断，不保存。
3. `PackageRuleMatcher` 排除一息本身和安全关键系统包。
4. 目标应用进入前台且不在 90 秒放行窗口时，创建一条未完成记录。
5. `BreathingOverlayController` 使用 `TYPE_ACCESSIBILITY_OVERLAY` 显示 10 秒练习。
6. 用户选择“算了”或“继续打开”后，本地 SQLite 记录被补全。
7. 覆盖层创建失败时记录 `FAIL_OPEN`，立即放行且不计入放下率。

## 数据

`interventions` 表只包含：目标包名、触发/结束时间、等待秒数和结果。没有窗口标题、文字、控件、截图、账号标识或设备标识。

应用选择保存在私有 `SharedPreferences`。统计存放在私有 SQLite 数据库。应用清单关闭 Android 备份和设备迁移。

## 包结构

- `accessibility/`：无障碍服务入口。
- `intervention/`：包名规则、放行窗口、呼吸覆盖层。
- `data/`：本地配置、服务健康状态和统计数据库。
- `domain/`：领域模型。
- `platform/`：系统启动器应用查询。
- `ui/`：Compose 主题与界面。
