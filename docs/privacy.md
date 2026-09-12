# 隐私核验清单

每次发布前需要同时通过源码清单检查和最终 APK 检查：

- 不存在联网与网络状态权限。
- 不存在使用情况访问权限。
- 不存在全包查询权限。
- 不存在普通悬浮窗权限。
- 不存在外部存储权限。
- `canRetrieveWindowContent` 为 `false`。
- `canPerformGestures` 为 `false`。
- `allowBackup` 为 `false`，数据提取规则排除全部应用数据。
- 依赖列表中没有广告、统计、崩溃上报或推送 SDK。

CI 的 `checkPrivacyManifest` 检查源码清单，`scripts/check-apk-permissions.sh` 检查依赖合并后的最终 APK。
