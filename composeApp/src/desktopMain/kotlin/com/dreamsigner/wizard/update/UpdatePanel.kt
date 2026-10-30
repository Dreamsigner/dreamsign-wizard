package com.dreamsigner.wizard.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpdatePanel(state: UpdateUiState, onCheck: () -> Unit, onAutomatic: (Boolean) -> Unit, onApply: () -> Unit) {
    Surface(color = White, shape = LearningShape) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dreamsign Wizard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Version ${state.version}", color = Graphite)
            }
            if (state.installed) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(value = state.automatic, role = Role.Checkbox, onValueChange = onAutomatic),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(state.automatic, onCheckedChange = null)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mises à jour auto", fontWeight = FontWeight.Bold)
                        Text("Télécharger les nouvelles versions et les installer à la fermeture.", color = Graphite, fontSize = 14.sp)
                    }
                }
            }
            if (state.message.isNotBlank()) Row(
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = ActionRed, strokeWidth = 2.dp)
                Text(state.message, color = if (state.requiresAttention) ActionRed else Graphite, modifier = Modifier.weight(1f))
            }
            if (state.installed) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.ready) LearningButton("Installer et redémarrer", onApply, enabled = !state.busy)
                OutlinedButton(onCheck, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape) {
                    Text("Vérifier les mises à jour")
                }
            }
        }
    }
}
