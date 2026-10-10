package com.duolingo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.ai.IngestionSourceType
import com.duolingo.app.model.ResourceIngestionItem
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun AiImporterScreen(viewModel: AppViewModel) {
    val ingestedItems by viewModel.ingestedItems.collectAsState()
    val progressText by viewModel.ingestionProgressText.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()

    var inputUrlOrPath by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(IngestionSourceType.YOUTUBE_VIDEO) }

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = DuoPurple,
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🤖 Transcripteur IA de Ressources",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Importez des fichiers PDF ou des liens YouTube pour générer automatiquement des leçons et quiz Duolingo (Maths, C++, TCF Canada).",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Type de ressource", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = selectedType == IngestionSourceType.YOUTUBE_VIDEO,
                            onClick = { selectedType = IngestionSourceType.YOUTUBE_VIDEO },
                            label = "▶️ Vidéo YouTube"
                        )
                        FilterChip(
                            selected = selectedType == IngestionSourceType.PDF_DOCUMENT,
                            onClick = { selectedType = IngestionSourceType.PDF_DOCUMENT },
                            label = "📄 Fichier PDF"
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("2. Lien YouTube ou Chemin du Fichier PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputUrlOrPath,
                        onValueChange = { inputUrlOrPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (selectedType == IngestionSourceType.YOUTUBE_VIDEO)
                                    "https://www.youtube.com/watch?v=..."
                                else
                                    "C:/documents/cours_proba.pdf"
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    if (progressText.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DuoPurple)
                            Spacer(Modifier.width(10.dp))
                            Text(progressText, color = DuoPurple, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (inputUrlOrPath.isNotBlank()) {
                                coroutineScope.launch {
                                    viewModel.importResourceFromAi(inputUrlOrPath, selectedType)
                                    inputUrlOrPath = ""
                                }
                            }
                        },
                        enabled = inputUrlOrPath.isNotBlank() && progressText.isEmpty(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DuoPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GÉNÉRER LA LEÇON IA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Historique des Ingestions IA", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
        }

        items(ingestedItems) { item ->
            IngestedItemRow(item = item)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(1.5.dp, if (selected) DuoPurple else Color.LightGray, RoundedCornerShape(20.dp)),
        color = if (selected) Color(0FFF3E8FF) else Color.White,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            color = if (selected) DuoPurple else Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun IngestedItemRow(item: ResourceIngestionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (item.sourceType == IngestionSourceType.YOUTUBE_VIDEO) "▶️" else "📄", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(item.sourceUrlOrPath, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = Color(0FFE6F4FE),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "${item.generatedLessonsCount} leçon(s)",
                    color = DuoBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
