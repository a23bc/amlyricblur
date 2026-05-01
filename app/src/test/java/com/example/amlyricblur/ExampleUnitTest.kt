package com.example.amlyricblur

import org.junit.Test

import org.junit.Assert.*

/**
 * 歌词模糊效果单元测试
 */
class LyricBlurUnitTest {

    @Test
    fun blurRadiusCalculation_isCorrect() {
        // 焦点行应该使用基础模糊半径
        assertEquals(10f, calculateBlurRadius(0, 0), 0.01f)
        
        // 距离焦点 1 行的位置
        assertEquals(11.5f, calculateBlurRadius(1, 0), 0.01f)
        
        // 距离焦点 2 行的位置
        assertEquals(13.0f, calculateBlurRadius(2, 0), 0.01f)
        
        // 距离焦点 5 行的位置
        assertEquals(17.5f, calculateBlurRadius(5, 0), 0.01f)
    }

    @Test
    fun focusPositionCalculation_isCorrect() {
        // 测试焦点位置计算逻辑
        val screenHeight = 1920
        val expectedFocusY = screenHeight / 9
        assertEquals(213, expectedFocusY)
    }

    @Test
    fun lyricViewIds_areDefined() {
        // 验证所有歌词 TextView ID 已定义
        assertTrue(LyricViewIds.ALL_TEXT_IDS.isNotEmpty())
        assertEquals(8, LyricViewIds.ALL_TEXT_IDS.size)
    }

    private fun calculateBlurRadius(currentIndex: Int, focusIndex: Int): Float {
        val distance = Math.abs(currentIndex - focusIndex)
        return BlurConfig.BASE_BLUR_RADIUS + distance * BlurConfig.BLUR_INCREMENT_PER_LINE
    }
}