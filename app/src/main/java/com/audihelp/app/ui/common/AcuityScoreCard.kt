package com.audihelp.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.model.EarScore
import com.audihelp.app.domain.model.HearingAcuityScore
import com.audihelp.app.domain.model.HearingCategory

@Composable
fun AcuityScoreCard(
    score: HearingAcuityScore,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Hearing Acuity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            // Overall score bubble + category
            score.overall?.let { overall ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreBubble(score = overall.score, max = overall.maxScore, category = overall.category)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            overall.category.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor(overall.category),
                        )
                        Text(
                            overall.category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Per-ear breakdown
            val hasRight = score.right != null
            val hasLeft = score.left != null
            if (hasRight || hasLeft) {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Per Ear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    score.right?.let { EarScoreColumn(it, Modifier.weight(1f)) }
                    score.left?.let  { EarScoreColumn(it, Modifier.weight(1f)) }
                }
            }

            // Per-frequency breakdown (use overall or whichever ear is available)
            val freqSource = score.overall ?: score.right ?: score.left
            freqSource?.let { src ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("By Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                src.frequencies.forEach { fs ->
                    val pct = src.percentageByFrequency[fs.frequencyHz] ?: 0
                    val freqLabel = if (fs.frequencyHz >= 1000) "${fs.frequencyHz / 1000}kHz" else "${fs.frequencyHz}Hz"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            freqLabel,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(44.dp),
                        )
                        // Progress bar
                        Box(
                            Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(pct / 100f)
                                    .height(10.dp)
                                    .background(frequencyBarColor(pct)),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$pct%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            fs.response.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(80.dp),
                        )
                    }
                }
            }

            // Early warning note for non-deaf users
            val cat = score.overall?.category ?: score.right?.category ?: score.left?.category
            if (cat != null && cat != HearingCategory.NORMAL) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                EarlyWarningNote(cat)
            }
        }
    }
}

@Composable
private fun ScoreBubble(score: Int, max: Int, category: HearingCategory) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(categoryColor(category).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = categoryColor(category),
            )
            Text(
                "/$max",
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor(category),
            )
        }
    }
}

@Composable
private fun EarScoreColumn(earScore: EarScore, modifier: Modifier = Modifier) {
    val label = when (earScore.ear) {
        EarSide.RIGHT -> "Right ear"
        EarSide.LEFT  -> "Left ear"
        EarSide.BOTH  -> "Both"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${earScore.score}/${earScore.maxScore}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = categoryColor(earScore.category),
            )
            Text(
                earScore.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor(earScore.category),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EarlyWarningNote(category: HearingCategory) {
    val (icon, text) = when (category) {
        HearingCategory.MILD ->
            "⚠" to "Mild loss detected. Early sign — consider scheduling a clinical audiogram for a baseline record."
        HearingCategory.MODERATE ->
            "⚠" to "Moderate loss. Speech in noisy environments is likely difficult. Clinical evaluation recommended."
        HearingCategory.SEVERE ->
            "🔴" to "Severe loss. Conversation without amplification is very difficult. Audiologist consultation advised."
        HearingCategory.PROFOUND ->
            "🔴" to "Profound loss. Very limited hearing remains. Hearing aids or cochlear implant evaluation recommended."
        HearingCategory.DEAF ->
            "🔴" to "No detectable hearing at standard test levels. Clinical evaluation required."
        HearingCategory.NORMAL -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            .padding(10.dp),
    ) {
        Text("$icon  ", style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun categoryColor(category: HearingCategory): Color = when (category) {
    HearingCategory.NORMAL   -> Color(0xFF2E7D32)
    HearingCategory.MILD     -> Color(0xFF558B2F)
    HearingCategory.MODERATE -> Color(0xFFF9A825)
    HearingCategory.SEVERE   -> Color(0xFFE65100)
    HearingCategory.PROFOUND -> Color(0xFFC62828)
    HearingCategory.DEAF     -> Color(0xFF6A1B9A)
}

private fun frequencyBarColor(pct: Int): Color = when {
    pct >= 80 -> Color(0xFF2E7D32)
    pct >= 60 -> Color(0xFF558B2F)
    pct >= 40 -> Color(0xFFF9A825)
    pct >= 20 -> Color(0xFFE65100)
    else      -> Color(0xFFC62828)
}
