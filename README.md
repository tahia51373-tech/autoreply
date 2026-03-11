# AutoReply - 自动回复助手

一个本地的 Android 自动回复应用，无需联网。

## 功能

- 监听通知栏消息（支持微信、QQ、短信）
- 根据关键词自动回复
- 完全本地运行，不联网
- 轻量级，低配置手机也能用

## 编译环境要求

### 必需：
1. **JDK 17** - [下载地址](https://www.oracle.com/java/technologies/downloads/#java17)
2. **Android SDK** - 命令行工具即可

### 可选（推荐）：
- **Android Studio** - 完整 IDE，但比较吃资源
- 或者只用 **VS Code** + 命令行编译

## 编译步骤

### 方法 1：使用 Gradle 命令行（轻量推荐）

```bash
# 1. 进入项目目录
cd AutoReply

# 2. 编译 Debug 版本
gradlew.bat assembleDebug
# 或者在 Linux/Mac 上：
# ./gradlew assembleDebug

# 3. APK 输出位置：
# app/build/outputs/apk/debug/app-debug.apk
```

### 方法 2：使用 Android Studio

1. 打开 Android Studio
2. File → Open → 选择 AutoReply 文件夹
3. 等待 Gradle 同步完成
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

## 安装使用

1. 将 APK 传到手机
2. 安装应用
3. 打开应用 → 点击"开启服务"
4. 在无障碍设置中找到 AutoReply 并开启
5. 添加回复规则（关键词 + 回复内容）

## 回复规则示例

| 关键词 | 回复内容 |
|--------|----------|
| 在吗 | 在的，什么事？ |
| 开会 | 稍后回复你 |
| 忙 | 现在有点忙，晚点说 |

## 注意事项

1. **需要无障碍权限** - 这是 Android 系统要求的，用于读取通知内容
2. **应用需要保持后台运行** - 不要强行关闭
3. **部分手机需要额外设置** - 有些手机需要允许自启动、后台运行等权限
4. **微信/QQ 可能有限制** - 部分版本的通知可能无法完整读取

## 项目结构

```
AutoReply/
├── app/
│   ├── src/main/
│   │   ├── java/com/autoreply/
│   │   │   ├── MainActivity.kt      # 主界面
│   │   │   ├── AutoReplyService.kt  # 无障碍服务
│   │   │   └── RuleAdapter.kt       # 规则列表适配器
│   │   ├── res/                      # 资源文件
│   │   └── AndroidManifest.xml       # 权限配置
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 低配置电脑编译建议

你的电脑配置（J1900 + 8GB）编译这个项目应该没问题：

1. **关闭其他程序** - 编译时尽量关闭浏览器等占用内存的程序
2. **使用命令行** - 比 Android Studio 轻量很多
3. **首次编译会慢** - Gradle 首次需要下载依赖，后续会快很多
4. **可以远程编译** - 如果实在卡，可以用 GitHub Actions 云编译

## 后续改进方向

- 支持更多应用
- 更智能的回复（可以接入本地小模型）
- 定时回复
- 回复历史记录

## 许可证

个人学习使用，请勿用于商业或作弊用途。
