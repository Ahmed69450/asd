package com.ovos.arabicassistant.data.ai.engine_a

import kotlin.math.sqrt

/**
 * حاسبة تشابه جيب التمام (Cosine Similarity) فائقة السرعة
 */
object CosineSimilarity {

    fun compute(vecA: FloatArray, vecB: FloatArray): Float {
        val len = minOf(vecA.size, vecB.size)
        if (len == 0) return 0f

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in 0 until len) {
            val a = vecA[i].toDouble()
            val b = vecB[i].toDouble()
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 1e-9) {
            (dotProduct / denominator).toFloat().coerceIn(-1.0f, 1.0f)
        } else {
            0f
        }
    }
}
