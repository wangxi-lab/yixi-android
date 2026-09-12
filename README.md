# 一息（Yixi for Android）

打开干扰类应用前，先呼吸十秒。

这是一个面向隐私敏感用户的、本地运行的 Android 应用，灵感来自 [一息](https://github.com/Defiabell/yixi)。Android 版本通过无障碍服务获知当前窗口所属应用，在目标应用打开时显示一次 10 秒呼吸练习。

## 隐私边界

- 没有 `INTERNET` 或网络状态权限。
- 没有使用情况访问、悬浮窗、存储或通知权限。
- 无障碍服务声明 `canRetrieveWindowContent=false`，不能读取界面文字或控件树。
- 无障碍服务声明 `canPerformGestures=false`，不能代替用户点击或滑动。
- 只对用户选中的包名做匹配；非目标应用包名不会写入数据库。
- 配置和统计只保存在 Android 应用沙箱，且不参与云备份或设备迁移。
- 原始统计默认只保留 30 天，也可在应用内立即清空。

唯一申请的普通权限是 `VIBRATE`，用于呼吸结束时的短促触觉提示。

## 当前 MVP

- 从系统启动器入口枚举可启动应用，不使用 `QUERY_ALL_PACKAGES`。
- 选择需要守护的应用。
- 检测目标应用窗口切换并显示无障碍专用覆盖层。
- 4 秒吸气、2 秒屏息、4 秒呼气。
- 完成后先显示“算了”，延迟 800 毫秒显示“继续打开”。
- “继续打开”后对该应用放行 90 秒，避免立即重复拦截。
- 本地记录触发时间、等待时长和用户选择。
- 首页提供 realme UI 7.0 授权与保活诊断提示。

## 构建

要求 Android Studio（JDK 17）、Android SDK 36。仓库的 GitHub Actions 会运行：

```bash
gradle checkPrivacyManifest testDebugUnitTest lintDebug assembleDebug
bash scripts/check-apk-permissions.sh app/build/outputs/apk/debug/app-debug.apk
```

CI 构建产物中会提供 `yixi-debug-apk`。首次安装后，请到系统无障碍设置中启用“一息 · 应用打开提醒”。

## realme UI 7.0 测试重点

不同机型菜单命名可能略有变化，建议重点检查：

1. 无障碍服务可以正常启用，且系统详情页显示服务不能读取窗口内容。
2. 电池/应用耗电管理中允许一息后台活动。
3. 锁屏解锁、清理最近任务、长时间待机后仍能触发。
4. 来电、权限弹窗、系统设置和安装器不会被拦截。

## 安全说明

应用采用故障放行策略：覆盖层创建失败或服务异常时不会阻止目标应用继续打开。当前数据库依赖 Android 应用沙箱和系统文件级加密保护；尚未加入独立 SQLCipher 数据库密钥。

## 许可证

MIT
