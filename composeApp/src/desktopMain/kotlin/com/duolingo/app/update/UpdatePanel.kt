package com.duolingo.app.update

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.ui.Graphite
import com.duolingo.app.ui.White

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpdatePanel(state: UpdateUiState, onCheck: () -> Unit, onAutomatic: (Boolean) -> Unit, onApply: () -> Unit) {
    Surface(color = White, elevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Version ${state.version}", modifier = Modifier.padding(top = 16.dp), color = Graphite, fontSize = 13.sp)
                if (state.installed) {
                    Row {
                        Checkbox(state.automatic, onAutomatic)
                        Text("Mises à jour auto", modifier = Modifier.padding(top = 16.dp), fontSize = 13.sp)
                    }
                    TextButton(onCheck, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Vérifier les mises à jour") }
                    if (state.ready) TextButton(onApply, Modifier.heightIn(min = 48.dp)) { Text("Installer et redémarrer") }
                }
            }
            if (state.message.isNotBlank()) Text(state.message, modifier = Modifier.padding(bottom = 8.dp), color = Graphite, fontSize = 13.sp)
        }
    }
}
