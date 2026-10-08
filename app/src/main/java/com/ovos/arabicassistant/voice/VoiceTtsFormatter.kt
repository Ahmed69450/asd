package com.ovos.arabicassistant.voice

import java.util.regex.Pattern

/**
 * مُعقّم ومطهّر النصوص الصوتي لمحرّك Piper TTS
 * يقوم بإزالة الماركداون والرموز التعبيرية وروابط الويب ووسوم HTML،
 * ويترجم الرموز الحسابية إلى كلمات عربية صريحة لضمان نطق فصيح وسلس للسائق.
 */
object VoiceTtsFormatter {

    // وسوم HTML
    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]+>")

    // روابط الماركداون: [نص الرابط](url) -> نص الرابط
    private val MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\([^\\)]+\\)")

    // روابط الويب المباشرة
    private val URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+")

    // وسوم الماركداون: عناوين، خطوط عريضة، مائلة، أكواد
    private val MARKDOWN_SYMBOLS_PATTERN = Pattern.compile("(?m)^#{1,6}\\s*|[*_~`#>]")

    // الرموز التعبيرية (Emojis) بجميع نطاقات اليونيكود
    private val EMOJI_PATTERN = Pattern.compile(
        "[\\x{1F600}-\\x{1F64F}]|" + // Emoticons
        "[\\x{1F300}-\\x{1F5FF}]|" + // Misc Symbols and Pictographs
        "[\\x{1F680}-\\x{1F6FF}]|" + // Transport and Map
        "[\\x{1F700}-\\x{1F77F}]|" + // Alchemical
        "[\\x{1F780}-\\x{1F7FF}]|" + // Geometric Shapes Extended
        "[\\x{1F800}-\\x{1F8FF}]|" + // Supplemental Arrows-C
        "[\\x{1F900}-\\x{1F9FF}]|" + // Supplemental Symbols and Pictographs
        "[\\x{1FA00}-\\x{1FA6F}]|" + // Chess Symbols
        "[\\x{1FA70}-\\x{1FAFF}]|" + // Symbols and Pictographs Extended-A
        "[\\x{2600}-\\x{26FF}]|" +   // Misc symbols
        "[\\x{2700}-\\x{27BF}]|" +   // Dingbats
        "[\\x{D83C}-\\x{DBFF}\\x{DC00}-\\x{DFFF}]"
    )

    // المسافات وفواصل الأسطر المتكررة
    private val WHITESPACE_PATTERN = Pattern.compile("[\\r\\n\\t]+")
    private val MULTI_SPACE_PATTERN = Pattern.compile("\\s{2,}")

    /**
     * تنقية النص الخام وتجهيزه للنطق الصوتي
     */
    fun sanitize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. استبدال روابط الماركداون بالنص الظاهر فقط
        text = MARKDOWN_LINK_PATTERN.matcher(text).replaceAll("$1")

        // 2. إزالة وسوم HTML
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ")

        // 3. إزالة عناوين الويب والروابط الصريحة
        text = URL_PATTERN.matcher(text).replaceAll(" ")

        // 4. ترجمة الرموز الخاصة والحسابية إلى كلمات عربية صريحة قبل حذف الرموز
        text = text.replace("°C", " درجة مئوية ")
            .replace("℃", " درجة مئوية ")
            .replace("%", " بالمئة ")
            .replace("&", " و ")
            .replace("+", " زائد ")
            .replace(" - ", " ناقص ")
            .replace(" -", " ناقص ")

        // 5. إزالة محارف الماركداون المتبقية
        text = MARKDOWN_SYMBOLS_PATTERN.matcher(text).replaceAll("")

        // 6. إزالة الرموز التعبيرية
        text = EMOJI_PATTERN.matcher(text).replaceAll("")

        // 7. تحويل الأسطر الجديدة والمسافات البيضاء إلى مسافات فردية
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ")
        text = MULTI_SPACE_PATTERN.matcher(text).replaceAll(" ")

        return text.trim()
    }
}
