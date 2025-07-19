package com.example.amlyricblur

import android.graphics.BlurMaskFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Main : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.apple.android.music") return

        // 所有官方歌词 TextView ID
        val allTextIds = listOf(
            0x7f0a06fc,  // song_lyrics_line
            0x7f0a06fe,  // song_lyrics_word
            0x7f0d01c1,  // lyrics_line
            0x7f0d01c2,  // lyrics_line_instrumental
            0x7f0d01c3,  // lyrics_line_karaoke
            0x7f0d01c4,  // lyrics_line_static
            0x7f0d01c8,  // lyrics_word_karaoke
            0x7f0d01c9   // lyrics_word_karaoke_bg
        )

        val rvClazz = XposedHelpers.findClass(
            "androidx.recyclerview.widget.RecyclerView",
            lpparam.classLoader
        )
        XposedBridge.hookAllMethods(rvClazz, "onLayout", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val rv = param.thisObject as ViewGroup
                rv.post {
                    val lines = mutableListOf<View>()
                    for (i in 0 until rv.childCount) {
                        val child = rv.getChildAt(i)
                        lines += child
                    }
                    if (lines.isEmpty()) return@post

                    val screenH = rv.height
                    val focusY = screenH / 9
                    val sorted = lines.sortedBy { kotlin.math.abs(it.top - focusY) }
                    val focusIndex = sorted.indexOfFirst { it.top == sorted.first().top }

                    sorted.forEachIndexed { pos, line ->
                        val active = pos == focusIndex
                        dfs(line) { tv ->
                            if (tv.id in allTextIds) {
                                tv.paint.maskFilter = if (!active) {
                                    BlurMaskFilter(10f + kotlin.math.abs(pos - focusIndex) * 1.5f,
                                        BlurMaskFilter.Blur.NORMAL)
                                } else null
                                tv.invalidate()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun dfs(root: View, action: (TextView) -> Unit) {
        if (root is TextView && root.id in listOf(
                0x7f0a06fc, 0x7f0a06fe,
                0x7f0d01c1, 0x7f0d01c2, 0x7f0d01c3, 0x7f0d01c4,
                0x7f0d01c8, 0x7f0d01c9
            )) {
            action(root)
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) dfs(root.getChildAt(i), action)
        }
    }
}