package com.relatopro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportDialog(
    templates: List<TemplateEntity>,
    onDismiss: () -> Unit,
    onTemplateSelected: (Long) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Novo Relatório",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selecione um modelo base para iniciar:",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (templates.isEmpty()) {
                Text("Nenhum modelo disponível. Crie um modelo primeiro.")
            } else {
                templates.forEach { template ->
                    Card(
                        onClick = { onTemplateSelected(template.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(template.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${template.version}", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
