package com.audihelp.app.domain.model

import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.TrialResponse

/**
 * Hearing acuity scoring — analogous to visual 20/20.
 *
 * Each of the 6 standard frequencies (250–8000 Hz) is scored 0–10:
 *   • Level score (0–7): how quiet a tone the person could detect
 *       -80 to -70 dBFS → 7 pts  (normal)
 *       -70 to -60      → 6 pts  (near-normal)
 *       -60 to -50      → 5 pts  (mild loss)
 *       -50 to -40      → 4 pts
 *       -40 to -30      → 3 pts  (moderate)
 *       -30 to -20      → 2 pts
 *       -20 to -10      → 1 pt   (severe)
 *        -10 to  0      → 0 pts  (profound)
 *   • Response bonus (0–3):
 *       HEAR_CLEARLY → 3, BARELY_HEAR → 2, FEEL → 1, NOTHING → 0
 *
 * Maximum per frequency: 10 pts.
 * 6 frequencies × 10 = 60 pts total.
 * Reported as X/20 (scaled) so it maps intuitively like vision tests.
 *
 * Overall category:
 *   18–20 Normal  |  15–17 Mild  |  11–14 Moderate
 *   7–10 Severe   |  1–6 Profound  |  0 Deaf
 */

private const val MAX_SCORE_PER_FREQ = 10
private const val FREQ_COUNT = 6
private const val RAW_MAX = MAX_SCORE_PER_FREQ * FREQ_COUNT   // 60
private const val REPORTED_MAX = 20

enum class HearingCategory(val label: String, val description: String) {
    NORMAL    ("Normal",    "Hears all frequencies at quiet levels"),
    MILD      ("Mild loss", "Difficulty with soft speech or distant sounds"),
    MODERATE  ("Moderate loss", "Difficulty with normal conversation"),
    SEVERE    ("Severe loss", "Only loud sounds detected; speech is very unclear"),
    PROFOUND  ("Profound loss", "Barely detects very loud sounds"),
    DEAF      ("Deaf",      "No detectable hearing at standard test levels"),
}

data class FrequencyScore(
    val frequencyHz: Int,
    val levelDbfs: Float,
    val response: TrialResponse,
    val points: Int,           // 0–10
    val maxPoints: Int = MAX_SCORE_PER_FREQ,
)

data class EarScore(
    val ear: EarSide,
    val frequencies: List<FrequencyScore>,
    val rawScore: Int,          // 0–60
    val score: Int,             // 0–20 (scaled)
    val maxScore: Int = REPORTED_MAX,
    val category: HearingCategory,
    val percentageByFrequency: Map<Int, Int>,  // freq → 0-100%
)

data class HearingAcuityScore(
    val right: EarScore?,
    val left: EarScore?,
    val overall: EarScore?,     // average of both ears if both present
) {
    /** Display string like "14/20" for the given ear, or overall if null. */
    fun displayScore(ear: EarSide? = null): String {
        val s = when (ear) {
            EarSide.RIGHT -> right
            EarSide.LEFT  -> left
            else          -> overall ?: right ?: left
        } ?: return "—"
        return "${s.score}/$REPORTED_MAX"
    }

    fun category(ear: EarSide? = null): HearingCategory {
        val s = when (ear) {
            EarSide.RIGHT -> right
            EarSide.LEFT  -> left
            else          -> overall ?: right ?: left
        } ?: return HearingCategory.DEAF
        return s.category
    }
}

// ── Calculator ────────────────────────────────────────────────────────────────

fun calculateHearingAcuity(thresholds: List<HearingThreshold>): HearingAcuityScore {
    val byEar = thresholds.groupBy { it.ear }
    val right = byEar[EarSide.RIGHT]?.let { scoreEar(EarSide.RIGHT, it) }
    val left  = byEar[EarSide.LEFT]?.let  { scoreEar(EarSide.LEFT,  it) }

    val overall = when {
        right != null && left != null -> {
            val avgRaw = (right.rawScore + left.rawScore) / 2
            buildEarScore(EarSide.BOTH, avgRaw, mergeFrequencies(right, left))
        }
        right != null -> right
        left  != null -> left
        else          -> null
    }

    return HearingAcuityScore(right = right, left = left, overall = overall)
}

private fun scoreEar(ear: EarSide, thresholds: List<HearingThreshold>): EarScore {
    val freqScores = thresholds.sortedBy { it.frequencyHz }.map { t ->
        val levelPts = when {
            t.levelDbfs <= -70f -> 7
            t.levelDbfs <= -60f -> 6
            t.levelDbfs <= -50f -> 5
            t.levelDbfs <= -40f -> 4
            t.levelDbfs <= -30f -> 3
            t.levelDbfs <= -20f -> 2
            t.levelDbfs <= -10f -> 1
            else                -> 0
        }
        val responsePts = when (t.response) {
            TrialResponse.HEAR_CLEARLY -> 3
            TrialResponse.BARELY_HEAR  -> 2
            TrialResponse.FEEL         -> 1
            TrialResponse.NOTHING      -> 0
        }
        FrequencyScore(
            frequencyHz = t.frequencyHz,
            levelDbfs   = t.levelDbfs,
            response    = t.response,
            points      = levelPts + responsePts,
        )
    }
    val rawScore = freqScores.sumOf { it.points }
    return buildEarScore(ear, rawScore, freqScores)
}

private fun buildEarScore(ear: EarSide, rawScore: Int, freqScores: List<FrequencyScore>): EarScore {
    val scaled = (rawScore.toFloat() / RAW_MAX * REPORTED_MAX).toInt().coerceIn(0, REPORTED_MAX)
    val category = when {
        scaled >= 18 -> HearingCategory.NORMAL
        scaled >= 15 -> HearingCategory.MILD
        scaled >= 11 -> HearingCategory.MODERATE
        scaled >= 7  -> HearingCategory.SEVERE
        scaled >= 1  -> HearingCategory.PROFOUND
        else         -> HearingCategory.DEAF
    }
    val pctByFreq = freqScores.associate { fs ->
        fs.frequencyHz to (fs.points.toFloat() / fs.maxPoints * 100).toInt()
    }
    return EarScore(
        ear = ear,
        frequencies = freqScores,
        rawScore = rawScore,
        score = scaled,
        category = category,
        percentageByFrequency = pctByFreq,
    )
}

private fun mergeFrequencies(right: EarScore, left: EarScore): List<FrequencyScore> {
    val allFreqs = (right.frequencies.map { it.frequencyHz } +
                   left.frequencies.map { it.frequencyHz }).toSortedSet()
    val rMap = right.frequencies.associateBy { it.frequencyHz }
    val lMap = left.frequencies.associateBy { it.frequencyHz }
    return allFreqs.map { freq ->
        val r = rMap[freq]
        val l = lMap[freq]
        when {
            r != null && l != null -> r.copy(points = (r.points + l.points) / 2)
            r != null -> r
            else      -> l!!
        }
    }
}
