# 固定发布签名配置

Android 只允许使用同一证书签名的 APK 覆盖升级。GitHub Actions 临时生成的 Debug 证书每次可能不同，因此预览版不能依赖 Debug 签名发布。

## 1. 在本机生成签名密钥

请在安全目录运行以下命令，并妥善离线备份生成的 `yixi-release.jks`。丢失后无法继续为现有用户提供升级包。

```bash
keytool -genkeypair -v \
  -keystore yixi-release.jks \
  -storetype JKS \
  -alias yixi \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

不要把密钥文件、密码或 Base64 内容提交到仓库，也不要在 Issue、聊天或构建日志中发送。

## 2. 转换为 Base64

Windows PowerShell：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("yixi-release.jks")) | Set-Clipboard
```

Linux：

```bash
base64 -w0 yixi-release.jks
```

## 3. 配置 GitHub Actions Secrets

进入仓库 `Settings → Secrets and variables → Actions → New repository secret`，分别创建：

| Secret | 内容 |
| --- | --- |
| `YIXI_KEYSTORE_BASE64` | 密钥文件的完整 Base64 内容 |
| `YIXI_KEYSTORE_PASSWORD` | 生成密钥时设置的 keystore 密码 |
| `YIXI_KEY_ALIAS` | `yixi` |
| `YIXI_KEY_PASSWORD` | 生成密钥时设置的 key 密码 |

配置完成后重新运行失败的 `Publish signed release` 工作流。工作流不会输出密码，会在结束时删除临时密钥文件，并在签名缺失时停止发布。

## 4. 首次切换说明

`v0.1` 和 `v0.2` 使用了临时 Debug 签名，无法升级到固定签名版本。安装第一个固定签名版本前仍需卸载旧版一次；从固定签名版本开始，后续版本即可直接覆盖升级并保留本地统计。
