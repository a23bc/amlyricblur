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

        val rvClazz = XposedHelpers.findClass(
            "androidx.recyclerview.widget.RecyclerView",
            lpparam.classLoader
        )
        XposedBridge.hookAllMethods(rvClazz, "onLayout", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val rv = param.thisObject as ViewGroup
                rv.post {
                    val lyrics = mutableListOf<Pair<Int, TextView>>() // (topY, TextView)
                    for (i in 0 until rv.childCount) {
                        val child = rv.getChildAt(i) as? ViewGroup ?: continue
                        val tv = child.findViewById<View>(0x7f0a06fe) as? TextView
                            ?: child.findViewById(0x7f0a06fc) as? TextView
                            ?: continue
                        lyrics += Pair(child.top, tv)
                    }
                    if (lyrics.isEmpty()) return@post

                    // ✅ 如果所有行文本相同，认为是“视觉上一行歌词”，全部清晰
                    val uniqueLyrics = lyrics.map { it.second.text.toString() }.distinct()
                    if (uniqueLyrics.size == 1) {
                        lyrics.forEach { (_, tv) ->
                            tv.paint.maskFilter = null
                            tv.invalidate()
                        }
                        return@post
                    }

                    val focusY = rv.height / 6

                    val sorted = lyrics.sortedBy { kotlin.math.abs(it.first - focusY) }
                    val centerIndex = sorted.indexOfFirst { it.first == sorted.first().first }

                    sorted.forEachIndexed { pos, (_, tv) ->
                        val offset = pos - centerIndex
                        val baseBlur = kotlin.math.abs(offset)

                        val blurLevel = when {
                            offset == 0 -> 0f
                            offset == 1 -> baseBlur + 1f
                            offset < 0  -> baseBlur + 1f
                            else        -> baseBlur.toFloat()
                        }

                        tv.paint.maskFilter = if (blurLevel > 0f) {
                            BlurMaskFilter(blurLevel * 2.5f, BlurMaskFilter.Blur.NORMAL)
                        } else null
                        tv.invalidate()
                    }
                }
            }
        })
    }
}
