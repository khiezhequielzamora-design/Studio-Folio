package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ArtifactType
import com.example.model.StudyArtifact
import com.example.model.StudySource
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StudioTab(
    sources: List<StudySource>,
    artifacts: List<StudyArtifact>,
    generatingArtifactType: ArtifactType?,
    onGenerateArtifact: (ArtifactType) -> Unit,
    onOpenArtifact: (StudyArtifact) -> Unit,
    onDeleteArtifact: (String) -> Unit
) {
    val activeCount = sources.count { it.included }
    val canGenerate = activeCount > 0 && generatingArtifactType == null

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Study Studio",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (activeCount > 0) {
                        "Transform $activeCount active ${if (activeCount == 1) "source" else "sources"} into interactive revision tools."
                    } else {
                        "Upload or enable at least one source in Sources to unlock generation."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GeneratorActionCard(
                type = ArtifactType.AUDIO_OVERVIEW,
                title = "Audio Discussion",
                subtitle = "Two AI hosts unpack your lesson in a natural podcast-style dialogue with voice playback.",
                icon = Icons.Filled.GraphicEq,
                accentColor = MaterialTheme.colorScheme.secondary,
                isLoading = generatingArtifactType == ArtifactType.AUDIO_OVERVIEW,
                enabled = canGenerate,
                onClick = { onGenerateArtifact(ArtifactType.AUDIO_OVERVIEW) }
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactGeneratorCard(
                    type = ArtifactType.STUDY_GUIDE,
                    title = "Study Guide",
                    subtitle = "Concepts & self-test",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isLoading = generatingArtifactType == ArtifactType.STUDY_GUIDE,
                    enabled = canGenerate,
                    onClick = { onGenerateArtifact(ArtifactType.STUDY_GUIDE) },
                    modifier = Modifier.weight(1f)
                )
                CompactGeneratorCard(
                    type = ArtifactType.SUMMARY,
                    title = "Summary Brief",
                    subtitle = "High-yield overview",
                    icon = Icons.Filled.Summarize,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    isLoading = generatingArtifactType == ArtifactType.SUMMARY,
                    enabled = canGenerate,
                    onClick = { onGenerateArtifact(ArtifactType.SUMMARY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactGeneratorCard(
                    type = ArtifactType.FLASHCARDS,
                    title = "Flashcards",
                    subtitle = "Interactive flip deck",
                    icon = Icons.Filled.Style,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isLoading = generatingArtifactType == ArtifactType.FLASHCARDS,
                    enabled = canGenerate,
                    onClick = { onGenerateArtifact(ArtifactType.FLASHCARDS) },
                    modifier = Modifier.weight(1f)
                )
                CompactGeneratorCard(
                    type = ArtifactType.QUIZ,
                    title = "Practice Exam",
                    subtitle = "Timed MCQ check",
                    icon = Icons.Filled.Quiz,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    isLoading = generatingArtifactType == ArtifactType.QUIZ,
                    enabled = canGenerate,
                    onClick = { onGenerateArtifact(ArtifactType.QUIZ) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SAVED STUDY MATERIALS (${artifacts.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (artifacts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Generated study guides, flashcards, practice exams, and audio discussions will be saved here for offline review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(artifacts, key = { it.id }) { artifact ->
                SavedArtifactCard(
                    artifact = artifact,
                    onClick = { onOpenArtifact(artifact) },
                    onDelete = { onDeleteArtifact(artifact.id) }
                )
            }
        }
    }
}

@Composable
private fun GeneratorActionCard(
    type: ArtifactType,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("generate_${type.key.lowercase()}_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoading) "Generating $title…" else title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactGeneratorCard(
    type: ArtifactType,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("generate_${type.key.lowercase()}_button")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Column {
                Text(
                    text = if (isLoading) "Creating…" else title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SavedArtifactCard(
    artifact: StudyArtifact,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val type = ArtifactType.fromKey(artifact.artifactType)
    val icon = when (type) {
        ArtifactType.SUMMARY -> Icons.Filled.Summarize
        ArtifactType.STUDY_GUIDE -> Icons.AutoMirrored.Filled.MenuBook
        ArtifactType.FLASHCARDS -> Icons.Filled.Style
        ArtifactType.QUIZ -> Icons.Filled.Quiz
        ArtifactType.AUDIO_OVERVIEW -> Icons.Filled.GraphicEq
    }
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val timeLabel = artifact.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("artifact_card_${artifact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.label,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = artifact.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$timeLabel • ${artifact.sourceTitles.size} sources",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete study material",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
