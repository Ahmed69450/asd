package com.ovos.arabicassistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTtsFormatterTest {

    @Test
    fun `strips markdown asterisks hashes and code blocks`() {
        val input = "### درجة الحرارة **25°C** ومستوى الشحن `80%`!"
        val expected = "درجة الحرارة 25 درجة مئوية ومستوى الشحن 80 بالمئة!"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }

    @Test
    fun `strips all emojis and replaces newlines with single space`() {
        val input = "مرحباً يا سائق 👋\nالطقس مشمس اليوم ☀️\nأتمنى لك رحلة آمنة 🚗"
        val expected = "مرحباً يا سائق الطقس مشمس اليوم أتمنى لك رحلة آمنة"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }

    @Test
    fun `replaces math and special symbols with arabic words`() {
        val input = "السرعة +5 كم/س & الضغط -2"
        val expected = "السرعة زائد 5 كم/س و الضغط ناقص 2"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }

    @Test
    fun `removes html tags and markdown links`() {
        val input = "يمكنك مراجعة <a href=\"https://byd.com\">الموقع</a> أو [الرابط](https://maps.google.com) الآن."
        val expected = "يمكنك مراجعة الموقع أو الرابط الآن."
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }
}
