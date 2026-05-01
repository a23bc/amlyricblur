package com.example.amlyricblur

import android.graphics.BlurMaskFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Apple Music 歌词模糊效果 Xposed 模块
 * 为歌词列表添加基于位置的模糊效果，焦点行清晰，其他行根据距离渐变模糊
 */
class Main : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return

        val recyclerViewClass = XposedHelpers.findClass(RECYCLER_VIEW_CLASS_NAME, lpparam.classLoader)
        
        XposedBridge.hookAllMethods(recyclerViewClass, METHOD_ON_LAYOUT, createRecyclerViewHook())
    }

    /**
     * 创建 RecyclerView 的布局钩子
     */
    private fun createRecyclerViewHook(): XC_MethodHook {
        return object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val recyclerView = param.thisObject as ViewGroup
                recyclerView.post { applyBlurEffect(recyclerView) }
            }
        }
    }

    /**
     * 对 RecyclerView 中的歌词应用模糊效果
     */
    private fun applyBlurEffect(recyclerView: ViewGroup) {
        val lyricLines = collectLyricLines(recyclerView)
        if (lyricLines.isEmpty()) return

        val focusIndex = calculateFocusIndex(lyricLines, recyclerView.height)

        lyricLines.forEachIndexed { index, line ->
            val isFocused = index == focusIndex
            val blurRadius = calculateBlurRadius(index, focusIndex)
            applyBlurToLyricLine(line, isFocused, blurRadius)
        }
    }

    /**
     * 收集所有歌词行视图
     */
    private fun collectLyricLines(recyclerView: ViewGroup): List<View> {
        return buildList {
            for (i in 0 until recyclerView.childCount) {
                add(recyclerView.getChildAt(i))
            }
        }
    }

    /**
     * 计算焦点行的索引（最接近屏幕指定位置的行）
     */
    private fun calculateFocusIndex(lines: List<View>, screenHeight: Int): Int {
        val focusY = screenHeight / BlurConfig.FOCUS_POSITION_RATIO
        val sortedLines = lines.sortedBy { Math.abs(it.top - focusY) }
        return lines.indexOf(sortedLines.first())
    }

    /**
     * 根据与焦点的距离计算模糊半径
     */
    private fun calculateBlurRadius(currentIndex: Int, focusIndex: Int): Float {
        val distance = Math.abs(currentIndex - focusIndex)
        return BlurConfig.BASE_BLUR_RADIUS + distance * BlurConfig.BLUR_INCREMENT_PER_LINE
    }

    /**
     * 对歌词行中的所有 TextView 应用模糊效果
     */
    private fun applyBlurToLyricLine(line: View, isFocused: Boolean, blurRadius: Float) {
        traverseViews(line) { textView ->
            textView.paint.maskFilter = if (!isFocused) {
                BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            } else {
                null
            }
            textView.invalidate()
        }
    }

    /**
     * 深度优先遍历视图树，对所有歌词 TextView 执行操作
     */
    private fun traverseViews(root: View, action: (TextView) -> Unit) {
        when (root) {
            is TextView -> {
                if (root.id in LyricViewIds.ALL_TEXT_IDS) {
                    action(root)
                }
            }
            is ViewGroup -> {
                for (i in 0 until root.childCount) {
                    traverseViews(root.getChildAt(i), action)
                }
            }
        }
    }

    companion object {
        private const val TARGET_PACKAGE = "com.apple.android.music"
        private const val RECYCLER_VIEW_CLASS_NAME = "androidx.recyclerview.widget.RecyclerView"
        private const val METHOD_ON_LAYOUT = "onLayout"
    }

    init {
        // 验证配置常量
        require(BlurConfig.BASE_BLUR_RADIUS > 0) { "基础模糊半径必须大于 0" }
        require(BlurConfig.BLUR_INCREMENT_PER_LINE > 0) { "模糊增量必须大于 0" }
        require(BlurConfig.FOCUS_POSITION_RATIO > 1) { "焦点位置比例必须大于 1" }
    }
}