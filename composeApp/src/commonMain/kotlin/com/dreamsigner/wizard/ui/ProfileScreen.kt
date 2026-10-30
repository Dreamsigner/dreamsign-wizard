package com.dreamsigner.wizard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.model.UserStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(stats: UserStats, persistenceStatus: String = "État de la sauvegarde non disponible.") {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 800.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (wide) 32.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Mon parcours", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
            Text("Les petits progrès font les grandes habitudes.", color = Graphite, fontSize = 16.sp)
            Surface(color = White, shape = LearningShape) {
                FlowRow(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatCard(LearningSymbol.COMPLETED, "Leçons terminées", "${stats.completedLessonIds.size}", Ink)
                    StatCard(LearningSymbol.XP, "XP accumulés", "${stats.xp}", Ink)
                    StatCard(LearningSymbol.FLAME, "Jours de série", "${stats.streakDays}", Ink)
                    StatCard(LearningSymbol.HEART, "Vies restantes", "${stats.hearts}/${stats.maxHearts}", ActionRed)
                }
            }
            Surface(color = White, shape = LearningShape) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ma progression", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(persistenceStatus, fontSize = 16.sp)
                    if (stats.textModeCompletedLessonIds.isNotEmpty()) Text("${stats.textModeCompletedLessonIds.size} atelier(s) audio terminé(s) en mode lecture. Ces tentatives correspondent à la compréhension écrite.", color = Graphite)
                    Text("Les XP récompensent la première réussite d’une leçon. Les révisions restent accessibles pour entretenir tes acquis.", color = Graphite, fontSize = 16.sp)
                    Text("Une erreur retire un cœur. Même à zéro, tu peux continuer à t’entraîner et à corriger tes réponses.", color = Graphite, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(icon: LearningSymbol, title: String, value: String, color: Color) {
    Column(Modifier.widthIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelIcon(icon)
        Text(title, color = Graphite, fontSize = 14.sp)
        Text(value, color = color, fontSize = 32.sp)
    }
}
