package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Notebook
import com.example.model.SourceType
import com.example.model.StudySource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesTab(
    notebook: Notebook?,
    sources: List<StudySource>,
    importingMessage: String?,
    isGeneratingOverview: Boolean,
    onUploadDocumentClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onAddManualNote: (String, String) -> Unit,
    onToggleSource: (StudySource) -> Unit,
    onUpdateSource: (String, String, String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onGenerateOverview: () -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var inspectingSource by remember { mutableStateOf<StudySource?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ImportActionButton(
                    icon = Icons.Filled.UploadFile,
                    label = stringResource(R.string.add_file),
                    onClick = onUploadDocumentClick,
                    enabled = importingMessage == null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("upload_file_button")
                )
                ImportActionButton(
                    icon = Icons.Filled.AddPhotoAlternate,
                    label = stringResource(R.string.add_photo),
                    onClick = onPickImageClick,
                    enabled = importingMessage == null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_photo_button")
                )
                ImportActionButton(
                    icon = Icons.Filled.EditNote,
                    label = stringResource(R.string.add_note),
                    onClick = { showNoteDialog = true },
                    enabled = importingMessage == null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("write_note_button")
                )
            }
        }

        if (importingMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = importingMessage,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (sources.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Notebook Briefing",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            TextButton(
                                onClick = onGenerateOverview,
                                enabled = !isGeneratingOverview,
                                modifier = Modifier.testTag("generate_overview_button")
                            ) {
                                if (isGeneratingOverview) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (notebook?.summaryOverview.isNullOrBlank()) "Summarize" else "Refresh"
                                    )
                                }
                            }
                        }

                        if (!notebook?.summaryOverview.isNullOrBlank()) {
                            Text(
                                text = notebook!!.summaryOverview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "Tap Summarize to generate an instant executive overview and core takeaways from your active sources.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (sources.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_sources),
                        contentDescription = "No sources uploaded yet",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(220.dp)
                            .height(155.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Text(
                        text = "Add your learning materials",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Upload PDF modules, PowerPoint slides (.pptx), Word docs (.docx), lecture whiteboard photos, or type your own study notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                val activeCount = sources.count { it.included }
                Text(
                    text = "UPLOADED SOURCES ($activeCount/${sources.size} active in AI context)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(sources, key = { it.id }) { source ->
                SourceItemCard(
                    source = source,
                    onClick = { inspectingSource = source },
                    onToggle = { onToggleSource(source) },
                    onDelete = { onDeleteSource(source.id) }
                )
            }
        }
    }

    if (showNoteDialog) {
        AddNoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { title, body ->
                onAddManualNote(title, body)
                showNoteDialog = false
            }
        )
    }

    inspectingSource?.let { src ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var editMode by remember(src.id) { mutableStateOf(false) }
        var draftTitle by remember(src.id) { mutableStateOf(src.title) }
        var draftContent by remember(src.id) { mutableStateOf(src.content) }

        ModalBottomSheet(
            onDismissRequest = { inspectingSource = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editMode) "Edit Source" else src.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { editMode = !editMode }) {
                        Text(if (editMode) "Preview" else "Edit")
                    }
                }

                if (editMode) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { if (it.length <= 160) draftTitle = it },
                        label = { Text("Source Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { if (it.length <= 78000) draftContent = it },
                        label = { Text("Extracted Content / Notes") },
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { inspectingSource = null }) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateSource(src.id, draftTitle, draftContent)
                                inspectingSource = null
                            },
                            enabled = draftTitle.isNotBlank() && draftContent.isNotBlank()
                        ) {
                            Text("Save Changes")
                        }
                    }
                } else {
                    Text(
                        text = "${SourceType.fromKey(src.sourceType).badge} • ${src.wordCount} words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = src.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SourceItemCard(
    source: StudySource,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val type = SourceType.fromKey(source.sourceType)
    val icon = when (type) {
        SourceType.PDF -> Icons.Filled.PictureAsPdf
        SourceType.SLIDES -> Icons.Filled.Slideshow
        SourceType.IMAGE -> Icons.Filled.Image
        SourceType.NOTE -> Icons.Filled.EditNote
        SourceType.DOC -> Icons.Filled.Description
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (source.included) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (source.included) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("source_card_${source.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = source.included,
                onCheckedChange = { onToggle() }
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.badge,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${type.badge} • ${source.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (source.excerpt.isNotBlank()) {
                    Text(
                        text = source.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove source",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Lecture Note", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 160) title = it },
                    label = { Text("Note title") },
                    placeholder = { Text("e.g., Week 4 - Enzymes & Kinetics") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_note_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 78000) content = it },
                    label = { Text("Paste or type study notes") },
                    placeholder = { Text("Enter key definitions, lecture bullet points, or pasted textbook sections…") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_note_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.testTag("save_manual_note_button")
            ) {
                Text("Add Source")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
