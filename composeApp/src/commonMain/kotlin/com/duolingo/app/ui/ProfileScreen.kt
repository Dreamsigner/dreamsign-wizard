package com.duolingo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.model.UserStats

@Composable
fun ProfileScreen(stats: UserStats) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = DuoGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎓", fontSize = 40.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Mon profil", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("C++, Algorithmes, Mathématiques & TCF Canada", fontSize = 14.sp, color = Color.Gray)

                Spacer(Modifier.height(20.dp))
                Divider()
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCard(icon = "🔥", title = "Série (Jours)", value = "${stats.streakDays}", color = DuoYellow)
                    StatCard(icon = "⚡", title = "XP Total", value = "${stats.xp}", color = DuoBlue)
                    StatCard(icon = "❤️", title = "Vies", value = "${stats.hearts}/${stats.maxHearts}", color = DuoRed)
                }
            }
        }

        // État de sauvegarde locale
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐙", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("État de la sauvegarde", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "La progression reste en mémoire pendant cette session et se réinitialise à la fermeture.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                Text("Synchronisation cloud non configurée.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatCard(icon: String, title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(title, fontSize = 12.sp, color = Color.Gray)
    }
}
