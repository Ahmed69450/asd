package com.ovos.arabicassistant.data.ai.engine_a

/**
 * تمثيل متجه التضمين الدلالي لنوايا السيارة (384D Anchor Vector)
 */
data class AnchorVector(
    val intent: String,
    val text: String,
    val vector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnchorVector

        if (intent != other.intent) return false
        if (text != other.text) return false
        if (!vector.contentEquals(other.vector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intent.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
