# 用户签名与 nightly 构建

采用与 [UlinoyaPed/fcitx5-android](https://github.com/UlinoyaPed/fcitx5-android/blob/main/.github/workflows/build-apks.yml) 相同的 Secret 名称和密码约定：

| Repository Secret | 内容 | 工作流环境变量 |
| --- | --- | --- |
| `SIGNING_KEY` | keystore 文件的 Base64 内容 | `SIGN_KEY_BASE64` |
| `KEY_ALIAS` | 文件中的密钥 alias | `SIGN_KEY_ALIAS` |
| `KEY_PASSWORD` | keystore 和密钥的共同密码 | `SIGN_KEY_PWD` |

在 [Attendance 的 Actions Secrets 页面](https://github.com/UlinoyaPed/Attendance/settings/secrets/actions)分别创建上述三个 Repository Secrets。GitHub 仓库 Secret 不会自动从 fcitx5-android 共享过来。请直接在 GitHub 或本机已授权的 GitHub CLI 中输入密码，勿放入源码、issue 或聊天。

## 本机生成和上传

可用 JDK 的 keytool 交互生成，密码由提示输入：

```bash
keytool -genkeypair -keystore attendance-release.keystore -storetype PKCS12 \
  -alias attendance -keyalg RSA -keysize 3072 -validity 10000
```

此示例的 alias 是 `attendance`，PKCS12 使用同一密码。已有密钥也可使用，只要 keystore 与密钥密码相同。

使用本机有 Secrets 写权限的 `gh` 登录后上传：

```bash
base64 < attendance-release.keystore | tr -d '\r\n' | gh secret set SIGNING_KEY --repo UlinoyaPed/Attendance
gh secret set KEY_ALIAS --repo UlinoyaPed/Attendance
gh secret set KEY_PASSWORD --repo UlinoyaPed/Attendance
```

后两条交互式输入，不在命令参数中写密码。也可在 GitHub 网页创建三个 Secret。若需要生成 Base64 文件后粘贴，请在仓库之外保存：

```bash
base64 < attendance-release.keystore | tr -d '\r\n' > attendance-signing-base64.txt
```

Base64 是编码而非加密；keystore、密码与此文本需妥善保存。将相同 keystore 长期用于后续更新，已安装的其他签名版本不能由新签名直接覆盖升级。

## 构建和发布

配置完成后，在 Actions 中手动运行 **Build and publish nightly** 的 master 分支，或推送 master。

1. 执行 lint、单元测试和 R8 Release 构建，保留 `Attendance-nightly-unsigned.apk`。
2. 三个 Secrets 全部存在时，临时解码 keystore，用共同密码签名，执行 `apksigner verify`，成功后才将 `Attendance-nightly.apk` 和公开的 `SIGNING-CERTIFICATE.txt` 指纹加入 artifact。
3. 生成各 APK 和证书指纹文件的 SHA-256；发布至 nightly prerelease，签名 APK 可直接安装。
4. 三个 Secrets 均未配置时保持未签名流程；部分配置、错误密码、错误 alias、损坏的 keystore 或验签失败都会让构建失败，保留上一份 nightly。

Secrets 只在 master 的签名步骤环境中提供。私钥置于 runner 临时目录，设置私有权限，退出时清理；私钥和密码不进入 Gradle 缓存、Actions artifact、仓库或 Release。工作流只发布 APK、公开证书指纹与校验值。

如果撤销全部签名 Secrets，后续未签名 nightly 会移除上一提交遗留的签名 APK / 指纹附件，避免将旧包误当成当前版本。

当前连接缺少 GitHub Actions Secrets 接口权限时，代码推送仍可完成，但实际 Secrets 需要由用户本机或 GitHub 网页录入。配置正确与否以真实签名步骤和 APK 验签结果为准。
