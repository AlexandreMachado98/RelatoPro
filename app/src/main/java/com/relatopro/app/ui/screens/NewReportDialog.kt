package com.relatopro.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.ui.theme.AppTheme

@Composable
fun NewReportDialog(
    templates: List<TemplateEntity>,
    onDismiss: () -> Unit,
    onTemplateSelected: (Long) -> Unit
) {
    val colors = AppTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Relatório", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selecione o modelo de checklist a ser utilizado:", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                if (templates.isEmpty()) {
                    Text("Nenhum modelo cadastrado.", color = colors.textSecondary)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(templates.size) { index ->
                            val template = templates[index]
                            com.relatopro.app.ui.components.animation.AnimatedListItem(index = index) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTemplateSelected(template.id) }
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(template.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                                if (!template.isGlobal) {
                                                    Text("Meu Checklist", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(template.category.ifBlank { template.description.ifBlank { "Checklist de Campo" } }, fontSize = 11.sp, color = colors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}
