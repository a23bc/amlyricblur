# AM Lyric Blur - Apple Music 歌词模糊效果模块

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Xposed](https://img.shields.io/badge/Xposed-Module-red.svg)](https://github.com/rovo89/Xposed)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-24%2B-blue.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)

一个为 **Android 版 Apple Music** 添加动态歌词模糊效果的 Xposed 模块。通过智能识别焦点歌词行，实现类似音乐播放器的沉浸式视觉体验。

## ✨ 功能特性

- 🎯 **智能焦点识别**: 自动识别屏幕中央附近的歌词行为焦点行
- 🌫️ **渐变模糊效果**: 非焦点歌词根据与焦点行的距离呈现渐变模糊
- 📱 **原生体验**: 保持 Apple Music 原有界面风格，仅增强视觉效果
- ⚡ **性能优化**: 高效的视图遍历算法，不影响播放流畅度

## 📋 系统要求

| 项目 | 要求 |
|------|------|
| Android 版本 | Android 7.0 (API 24) 及以上 |
| Root 权限 | 需要 |
| Xposed 框架 | LSPosed / EdXposed / Xposed 官方框架 |
| 目标应用 | Apple Music for Android |

## 🚀 安装指南

### 前置条件

1. 设备已获取 Root 权限
2. 已安装并激活 Xposed 框架（推荐 LSPosed）
3. 已安装 Apple Music Android 版应用

### 安装步骤

1. **编译模块**
   ```bash
   ./gradlew assembleDebug
   ```
   编译后的 APK 位于 `app/build/outputs/apk/debug/`

2. **安装模块**
   - 将生成的 APK 安装到设备
   - 打开 LSPosed 管理器
   - 启用 "AM Lyric Blur" 模块
   - 勾选作用域为 `com.apple.android.music`
   - 重启 Apple Music 应用

## ⚙️ 配置说明

模块提供以下可配置参数（需修改源码后重新编译）：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `BASE_BLUR_RADIUS` | 10.0f | 基础模糊半径（像素） |
| `BLUR_INCREMENT_PER_LINE` | 1.5f | 每行距离增加的模糊值 |
| `FOCUS_POSITION_RATIO` | 9 | 焦点位置（屏幕高度的 1/9 处） |

修改位置：`app/src/main/java/com/example/amlyricblur/LyricBlurConfig.kt`

## 🏗️ 技术架构

```
AM Lyric Blur
├── Main.kt                 # Xposed 模块入口，钩子逻辑
├── LyricBlurConfig.kt      # 配置常量与歌词 View ID 定义
└── build.gradle            # 项目构建配置
```

### 核心原理

1. **钩子注入**: 拦截 `RecyclerView.onLayout()` 方法
2. **视图收集**: 遍历 RecyclerView 子视图获取歌词行
3. **焦点计算**: 基于屏幕位置确定当前焦点歌词行
4. **模糊渲染**: 使用 `BlurMaskFilter` 对非焦点文本应用模糊效果

## 📦 构建说明

```bash
# 克隆项目
git clone <repository-url>
cd kimi

# 使用 Gradle 构建
./gradlew assembleDebug

# 或使用 Android Studio 打开项目后直接构建
```

## 🔧 开发环境

- **IDE**: Android Studio Arctic Fox 或更高版本
- **Gradle**: 8.1.4+
- **Kotlin**: 1.9.24
- **Android SDK**: API 34

## 📝 注意事项

- 本模块仅适用于 **Apple Music Android 版**
- 不同版本的 Apple Music 可能需要调整歌词 View 的 ID 映射
- 模糊效果可能因设备性能而异，低端设备建议降低模糊半径
- 模块处于开发阶段，可能存在兼容性问题

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证

## ⚠️ 免责声明

本模块仅供学习研究使用。使用 Xposed 模块可能导致应用不稳定或违反服务条款，请自行承担风险。
