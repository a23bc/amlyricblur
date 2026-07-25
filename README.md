# AMLyricBlur

Apple Music 歌词逐行模糊 Xposed 模块。对当前高亮歌词行以外的歌词施加渐进式模糊效果，突出当前演唱行。

## 功能

- 逐行模糊：高亮行清晰显示，非高亮行按距离递增模糊
- 渐进式模糊层级：
  - 高亮行：无模糊
  - 下一行：blur=12
  - 再下一行：blur=16
  - 三行及以后：blur=20
- 自动跳过等待动画（跳动的圆点指示器）
- 双行高亮支持（同时高亮多行时均不模糊）
- 滑动歌词时自动解除模糊，方便阅读非高亮行歌词
- 高亮行不在可视区域时自动清除模糊，避免无意义的模糊效果

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

通过 Hook Apple Music 的 `PlayerLyricsViewModel` 获取当前高亮歌词行的 `lineId`，结合 `RecyclerView.M(View)` 获取每个子 View 的 adapter position，精确匹配高亮行并对非高亮行施加 `RenderEffect` 模糊。

### 关键 Hook 点

| Hook 目标 | 方法签名 | 作用 |
|-----------|---------|------|
| `PlayerLyricsViewFragment` | `onCreateView` | 获取歌词界面根 View，查找 RecyclerView |
| `PlayerLyricsViewModel` | `notifyWordHighlight(IIIZ)` | 获取高亮行 lineId（支持多行高亮累加） |
| `PlayerLyricsViewModel` | `setCurrentHighlightedLine(I)` | 获取当前高亮行（切换时清空再添加） |

### 模糊计算

```
距离 = adapterPos 与最近高亮行 lineId 的差值绝对值
模糊值 = min(12 + (距离 - 1) × 4, 20)
```

### 滑动交互

- 通过 `OnTouchListener` 检测用户手指触摸状态
- 结合 `ViewTreeObserver.OnScrollChangedListener` 检测滚动事件
- 仅在用户手动滑动时清除模糊，程序化滚动（切歌自动定位）不触发清除
- 松手后模糊保持清除状态，直到下一次高亮切换或滑回高亮行

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

## 已知问题

- 模糊通过 `View.setRenderEffect` 实现，依赖 Android 12+ 的 RenderEffect API
- `getChildAdapterPosition(View)` 在 6.5.0 中已移除，改用 `RecyclerView.M(View)` 静态方法获取 adapter position

## License

MIT
