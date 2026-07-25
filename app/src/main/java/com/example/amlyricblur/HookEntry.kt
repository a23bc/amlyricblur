package com.example.amlyricblur

import android.animation.ValueAnimator
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import java.util.WeakHashMap

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "AMLyricBlur"
        private const val PKG = "com.apple.android.music"
        private const val BLUR_BASE = 12f
        private const val BLUR_STEP = 4f
        private const val BLUR_MAX = 20f
    }

    private val highlightedLineIds = mutableSetOf<Int>()
    private val viewBlurValues = WeakHashMap<View, Float>()
    private val viewAnimators = WeakHashMap<View, ValueAnimator>()

    private var getChildCountMethod: Method? = null
    private var getChildAtMethod: Method? = null
    private var setRenderEffectMethod: Method? = null
    private var createBlurEffectMethod: Method? = null
    private var getAdapterPositionFromView: Method? = null

    private var recyclerView: Any? = null
    private var lyricsRootView: View? = null
    private var isUserScrolling = false
    private val scrollHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != PKG) return
        initReflectionCache(lpparam.classLoader)
        hookLyricsFragment(lpparam.classLoader)
        hookViewModel(lpparam.classLoader)
    }

    private fun initReflectionCache(cl: ClassLoader) {
        try {
            val rvClass = cl.loadClass("androidx.recyclerview.widget.RecyclerView")
            getChildCountMethod = ViewGroup::class.java.getMethod("getChildCount")
            getChildAtMethod = ViewGroup::class.java.getMethod("getChildAt", Int::class.javaPrimitiveType)

            for (m in rvClass.declaredMethods) {
                if (java.lang.reflect.Modifier.isStatic(m.modifiers)
                    && m.parameterTypes.size == 1
                    && m.parameterTypes[0] == View::class.java
                    && m.returnType == Int::class.javaPrimitiveType
                ) {
                    getAdapterPositionFromView = m
                    Log.i(TAG, "Found RecyclerView.${m.name}(View)")
                    break
                }
            }

            setRenderEffectMethod = View::class.java.getMethod(
                "setRenderEffect",
                Class.forName("android.graphics.RenderEffect")
            )
            createBlurEffectMethod = Class.forName("android.graphics.RenderEffect")
                .getMethod(
                    "createBlurEffect",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                    Shader.TileMode::class.java
                )
            Log.i(TAG, "Reflection OK")
        } catch (t: Throwable) {
            Log.e(TAG, "Reflection failed", t)
        }
    }

    private fun hookLyricsFragment(cl: ClassLoader) {
        try {
            val cls = cl.loadClass("com.apple.android.music.player.fragment.PlayerLyricsViewFragment")
            XposedBridge.hookAllMethods(cls, "onCreateView", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val result = param.result as? View ?: return
                    lyricsRootView = result
                    Log.i(TAG, "onCreateView hooked")
                    Handler(Looper.getMainLooper()).postDelayed({ findRecyclerView(result) }, 500)
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Fragment hook failed: ${t.message}")
        }
    }

    private fun hookViewModel(cl: ClassLoader) {
        try {
            val vmClass = cl.loadClass("com.apple.android.music.player.viewmodel.PlayerLyricsViewModel")
            Log.i(TAG, "Found VM")

            for (m in vmClass.declaredMethods) {
                val p = m.parameterTypes
                if (p.size == 4 && p[0] == Int::class.javaPrimitiveType && p[3] == Boolean::class.javaPrimitiveType) {
                    Log.i(TAG, "Hooking: " + m.name + "(IIIZ)")
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val lineId = param.args[0] as Int
                            val isBg = param.args[3] as Boolean
                            Log.d(TAG, ">>> word lineId=$lineId bg=$isBg")
                            if (!isBg && lineId > 0) {
                                synchronized(highlightedLineIds) {
                                    highlightedLineIds.add(lineId)
                                }
                                scheduleBlurUpdate()
                            }
                        }
                    })
                }
            }

            for (m in vmClass.declaredMethods) {
                val p = m.parameterTypes
                if (p.size == 1 && p[0] == Int::class.javaPrimitiveType && m.returnType == Void.TYPE) {
                    Log.i(TAG, "Hooking: " + m.name + "(I)")
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val lineId = param.args[0] as Int
                            Log.d(TAG, ">>> line lineId=$lineId")
                            synchronized(highlightedLineIds) {
                                highlightedLineIds.clear()
                                highlightedLineIds.add(lineId)
                            }
                            scheduleBlurUpdate()
                        }
                    })
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "VM hook failed: ${t.message}")
        }
    }

    private fun findRecyclerView(view: View) {
        if (recyclerView != null) return
        try {
            val rv = findRVInHierarchy(view)
            if (rv != null) {
                recyclerView = rv
                Log.i(TAG, "RV FOUND")
                attachScrollListener(rv)

            } else {
                Handler(Looper.getMainLooper()).postDelayed({ findRecyclerView(view) }, 1000)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "findRV error", t)
        }
    }

    private fun findRVInHierarchy(view: View): Any? {
        if (view.javaClass.name == "androidx.recyclerview.widget.RecyclerView") return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findRVInHierarchy(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    private fun scheduleBlurUpdate() {
        Handler(Looper.getMainLooper()).postDelayed({
            try { applyBlur() } catch (t: Throwable) { Log.e(TAG, "Blur failed", t) }
        }, 200)
    }

    private fun attachScrollListener(rv: Any) {
        try {
            val view = rv as View
            view.setOnTouchListener { _, event ->
                isUserScrolling = event.action != MotionEvent.ACTION_CANCEL
                    && event.action != MotionEvent.ACTION_UP
                false
            }
            view.viewTreeObserver.addOnScrollChangedListener { onScrollDetected() }
            Log.i(TAG, "Scroll listener attached")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach scroll listener", t)
        }
    }

    private fun onScrollDetected() {
        if (isUserScrolling) clearAllBlur()
    }

    private fun clearAllBlur() {
        val rv = getRv() ?: return
        val gcm = getChildCountMethod ?: return
        val gca = getChildAtMethod ?: return
        val childCount = gcm.invoke(rv) as Int
        for (i in 0 until childCount) {
            val child = gca.invoke(rv, i) as? View ?: continue
            if (!isLyricsLine(child)) continue
            viewBlurValues[child] = 0f
            viewAnimators[child]?.cancel()
            setRenderEffectMethod?.invoke(child, null)
        }
        Log.d(TAG, "clearAllBlur: done")
    }

    private fun getRv(): Any? {
        val rv = recyclerView ?: return null
        val gcm = getChildCountMethod ?: return null
        val count = try { gcm.invoke(rv) as Int } catch (_: Throwable) { -1 }
        if (count > 0) return rv
        // stale reference, re-find from lyricsRootView
        val root = lyricsRootView ?: return null
        val fresh = findRVInHierarchy(root)
        if (fresh != null) {
            recyclerView = fresh
            Log.i(TAG, "RV re-found")
            return fresh
        }
        return null
    }

    private fun applyBlur() {
        val rv = getRv() ?: return
        val gcm = getChildCountMethod ?: return
        val gca = getChildAtMethod ?: return

        val childCount = gcm.invoke(rv) as Int
        val hlIds = synchronized(highlightedLineIds) { highlightedLineIds.toSet() }

        var hasBouncingBall = false
        for (i in 0 until childCount) {
            val child = gca.invoke(rv, i) as? View ?: continue
            if (!isLyricsLine(child)) {
                hasBouncingBall = true
                break
            }
        }

        val shouldBlur = hlIds.isNotEmpty() || hasBouncingBall

        if (hlIds.isNotEmpty()) {
            var hasVisibleHighlight = false
            for (i in 0 until childCount) {
                val child = gca.invoke(rv, i) as? View ?: continue
                if (!isLyricsLine(child)) continue
                if (getAdapterPosition(child) in hlIds) {
                    hasVisibleHighlight = true
                    break
                }
            }
            if (!hasVisibleHighlight) {
                clearAllBlur()
                Log.d(TAG, "applyBlur: no highlight in viewport, cleared")
                return
            }
        }

        Log.d(TAG, "applyBlur: children=$childCount hl=$hlIds ball=$hasBouncingBall")

        for (i in 0 until childCount) {
            val child = gca.invoke(rv, i) as? View ?: continue

            if (!isLyricsLine(child)) continue

            val adapterPos = getAdapterPosition(child)
            val isHighlighted = adapterPos in hlIds

            val targetBlur = if (!shouldBlur || isHighlighted) {
                0f
            } else {
                val minDist = hlIds.minOf { Math.abs(adapterPos - it) }
                (BLUR_BASE + (minDist - 1) * BLUR_STEP).coerceAtMost(BLUR_MAX)
            }
            Log.d(TAG, "  [$i] pos=$adapterPos hl=$isHighlighted blur=$targetBlur")
            animateBlur(child, targetBlur)
        }
    }

    private fun isLyricsLine(view: View): Boolean {
        if (view !is ViewGroup) return false
        if (hasDescendantOfType(view, ImageView::class.java)) return false
        return true
    }

    private fun hasDescendantOfType(view: View, cls: Class<*>): Boolean {
        if (cls.isInstance(view)) return true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (hasDescendantOfType(view.getChildAt(i), cls)) return true
            }
        }
        return false
    }

    private fun getAdapterPosition(child: View): Int {
        val method = getAdapterPositionFromView ?: return -1
        return try {
            method.invoke(null, child) as Int
        } catch (t: Throwable) {
            Log.w(TAG, "RecyclerView.M(view) failed", t)
            -1
        }
    }

    private fun animateBlur(view: View, targetBlur: Float) {
        val current = viewBlurValues[view] ?: 0f
        if (current == targetBlur) return
        viewAnimators[view]?.cancel()
        ValueAnimator.ofFloat(current, targetBlur).apply {
            duration = 300
            addUpdateListener { anim ->
                try {
                    val v = anim.animatedValue as Float
                    if (v <= 0f) {
                        setRenderEffectMethod?.invoke(view, null)
                    } else {
                        val effect = createBlurEffectMethod?.invoke(null, v, v, Shader.TileMode.MIRROR)
                        setRenderEffectMethod?.invoke(view, effect)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Render err: ${t.message}", t)
                }
            }
            viewBlurValues[view] = targetBlur
            viewAnimators[view] = this
            start()
        }
    }
}
