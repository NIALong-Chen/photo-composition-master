# 摄影构图大师 · Photo Composition Master

Android 拍摄前实时构图助手：本地识别人脸，给出简短的位置调整建议，并保留手动拍照与相册查看流程。

**当前：1.0 验证版（1.0.0-dev4 / versionCode 4）。** 已在 vivo X300 完成基础受控场景验证；真实人物街拍、弱光、遮挡和远距离全身场景仍待反馈。尚未作为正式 1.0 发布。

<img src="design/app-icon/approved-icon.png" width="160" alt="摄影构图大师图标">

## 文档导航

| 文档 | 用途 |
| --- | --- |
| [产品设计说明书](docs/产品设计说明书.md) | 当前功能、用户流程、交互、边界及验收状态 |
| [产品技术文档](docs/产品技术文档.md) | 实际架构、依赖、规则、生命周期与存储 |
| [开发与维护指南](docs/开发与维护指南.md) | 环境准备、构建、测试与维护方式 |
| [路线图](docs/路线图.md) | 实拍验证、精细人物 Pose、风景构图 |
| [更新记录](CHANGELOG.md) | dev1 至 dev4 的变更 |
| [原始 1.0 产品规划](APP_1.0_产品规划.md) | 原始目标，不等同于已实现功能 |
| [人脸构图验收](人脸构图版验收.md) / [相机基础验收](相机基础版验收.md) | 真机验证证据与待测项目 |
| [首次构建记录](构建记录_2026-10-10.md) | 构建环境历史记录 |
| [图标设计](design/app-icon/README.md) | 已确认图标及适配说明 |
| [历史 README](docs/history/README_整理前.md) | 早期讨论与参考项目，进度以本首页为准 |

## 已实现

- CameraX 后置预览、权限流程、手动拍照、保存至 DCIM/PhotoMaster、最近照片入口。
- 三分网格和指导开关，记忆用户选择。
- 随 APK 打包的 ML Kit 人脸检测、人脸框及单人脸部位置建议。
- 无人、多人、侧脸、太小、太近、靠边等状态处理及提示稳定化。
- 本地分析，清单移除 INTERNET 权限，断网验证通过。
- dev3 修复 X300 竖屏提示覆盖；dev4 接入确认图标，成功构建并覆盖安装。

“可以拍”表示脸部位置满足当前规则，不保证完整人体或道具未被裁切，不代表美学评分。当前没有 Pose、风景分析、语音指导、自动快门或云端模型。

## 快速构建

已验证环境：Android Studio 自带 JDK 25、Gradle 9.6.0、AGP 9.4.0、SDK Platform 37.0、Build Tools 36.0.0。Java 编译级别 17；targetSdk 36，minSdk 29。

在本机 local.properties 配置 sdk.dir，或由 Android Studio 配置 SDK，然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-debug.ps1
# 如需指定 JDK：增加 -JavaHome '你的JDK目录'
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。这是调试包；新电脑默认签名可能不同。详见维护指南。

## 仓库范围

保存源码、文档、确认图标、构建脚本和 Gradle Wrapper。APK、缓存、本机 SDK 路径、签名材料不进入 Git。

未添加开源许可证。ShutterMuse、FrameWise、LiveCapture、Aesthetic Camera 和摄影 Skill 属于研究参考，未直接接入当前运行时。后续以此仓库作为开发维护基线。
