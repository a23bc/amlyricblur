# AMLyricBlur

Apple Music 歌词逐行模糊 Xposed 模块。对当前高亮歌词行以外的歌词施加渐进式模糊效果，突出当前演唱行。

## 功能

- 逐行模糊：高亮行清晰显示，非高亮行按距离递增模糊
- 非对称模糊层级（参考 iOS 27）：
  - 高亮行：无模糊
  - 下一行（即将演唱）：blur=12
  - 再下一行：blur=16
  - 三行及以后：blur=20
  - 上一行（已演唱）：blur=16
  - 再上一行：blur=20
- 创作者行同步模糊：歌词末尾的创作者行跟随最后一行歌词的模糊深度
- 等待球阶段自动模糊：歌曲开头等待动画出现时，所有歌词行即刻施加模糊
- 双行高亮支持（同时高亮多行时均不模糊）
- 滑动歌词时自动解除模糊，方便阅读非高亮行歌词
- 高亮行不在可视区域时自动保持解除模糊状态，避免无意义的模糊效果

## 环境要求

- Android 12+（API 31）
- Apple Music 6.5.0
- Xposed / LSPosed 框架

## 安装

1. 从 [Releases](https://github.com/a23bc/amlyricblur/releases) 下载 APK
2. 安装到设备
3. 在 LSPosed 中启用模块，作用域选择 `com.apple.android.music`
4. 强制停止并重新打开 Apple Music

## 工作原理

### 歌词高亮检测

模块通过两种方式获取当前高亮歌词行：

1. **DexFile 扫描（主要）**：运行时扫描 Apple Music APK 的 DEX 文件，查找接收 `LyricsLineVector` 参数的方法。该方法回调提供完整的高亮行列表，能准确支持双行/多行高亮场景。扫描通过 `dalvik.system.DexFile` API 直接完成，无需额外依赖。
2. **ViewModel Hook（后备）**：当 DexFile 扫描未安装时，通过 Hook `PlayerLyricsViewModel` 的 `setCurrentHighlightedLine(I)V` 获取单行高亮。`lineId=0`（等待球阶段）不受 DexFile hook 安装状态门控，确保等待球期间歌词正确模糊。

此外还 Hook 了 `notifyWordHighlight(IIIZ)V` 以支持逐字歌词的高亮累加。

### 模糊计算

非对称模糊，上一行（已演唱）比下一行（即将演唱）模糊更深：

```
下方（前方）：模糊值 = min(12 + (距离 - 1) × 4, 20)
上方（后方）：模糊值 = min(16 + (距离 - 1) × 4, 20)
```

### 创作者行

歌词末尾的创作者行（`lyrics_report_concern` 布局）不是歌词行，不会被识别为歌词。模块检测 RecyclerView 最后一个 child 是否为非歌词行，如果是则跟随最后一个歌词行的模糊深度。

### 滑动交互

- 通过 `OnTouchListener` 检测用户手指触摸状态
- 结合 `ViewTreeObserver.OnScrollChangedListener` 检测滚动事件
- 仅在用户手动滑动时清除模糊，程序化滚动（切歌自动定位）不触发清除
- 松手后，若高亮行不在屏幕内则保持解除模糊，直到高亮行重新可见

## 构建

```bash
# 需要 JDK 17 和 Android SDK
./gradlew assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/example/amlyricblur/
  └── HookEntry.kt      # Xposed Hook 入口及模糊逻辑
```

## 依赖

- `de.robv.android.xposed:api:82` — Xposed Framework API

## 已知问题

- 模糊通过 `View.setRenderEffect` 实现，依赖 Android 12+ 的 RenderEffect API
- `getChildAdapterPosition(View)` 在 6.5.0 中已移除，改用 `RecyclerView.M(View)` 静态方法获取 adapter position

## License

MIT
