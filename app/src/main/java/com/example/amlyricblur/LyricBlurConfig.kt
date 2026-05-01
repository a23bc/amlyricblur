package com.example.amlyricblur

/**
 * 官方歌词 TextView 的资源 ID 常量
 */
object LyricViewIds {
    val ALL_TEXT_IDS = setOf(
        0x7f0a06fc,  // song_lyrics_line
        0x7f0a06fe,  // song_lyrics_word
        0x7f0d01c1,  // lyrics_line
        0x7f0d01c2,  // lyrics_line_instrumental
        0x7f0d01c3,  // lyrics_line_karaoke
        0x7f0d01c4,  // lyrics_line_static
        0x7f0d01c8,  // lyrics_word_karaoke
        0x7f0d01c9   // lyrics_word_karaoke_bg
    )
}

/**
 * 模糊效果配置常量
 */
object BlurConfig {
    const val BASE_BLUR_RADIUS = 10f
    const val BLUR_INCREMENT_PER_LINE = 1.5f
    const val FOCUS_POSITION_RATIO = 9  // 屏幕高度的 1/9 处作为焦点
}
