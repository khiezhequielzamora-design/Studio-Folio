package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AudioOverviewPlayer
import com.example.model.ArtifactJsonParser
import com.example.model.ArtifactType
import com.example.model.StudyArtifact
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactViewerDialog(
    artifact: StudyArtifact,
    onDismiss: () -> Unit
) {
    val type = ArtifactType.fromKey(artifact.artifactType)
    val clipboard = LocalClipboardManager.current
    var copiedBanner by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = type.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = artifact.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close viewer")
                        }
                    },
                    actions = {
                        if (type == ArtifactType.STUDY_GUIDE || type == ArtifactType.SUMMARY) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(artifact.payloadJson))
                                    copiedBanner = true
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy text")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (type) {
                    ArtifactType.SUMMARY, ArtifactType.STUDY_GUIDE -> {
                        MarkdownStudyReader(
                            content = artifact.payloadJson,
                            sourceTitles = artifact.sourceTitles,
                            copied = copiedBanner
                        )
                    }

                    ArtifactType.FLASHCARDS -> {
                        FlashcardsPlayer(payloadJson = artifact.payloadJson)
                    }

                    ArtifactType.QUIZ -> {
                        PracticeQuizPlayer(payloadJson = artifact.payloadJson)
                    }

                    ArtifactType.AUDIO_OVERVIEW -> {
                        AudioDiscussionPlayerView(payloadJson = artifact.payloadJson)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownStudyReader(
    content: String,
    sourceTitles: List<String>,
    copied: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (copied) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Copied to clipboard",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (sourceTitles.isNotEmpty()) {
            Text(
                text = "Sources: ${sourceTitles.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content.lines().forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("# ") -> {
                            Text(
                                text = trimmed.removePrefix("# "),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        trimmed.startsWith("## ") || trimmed.startsWith("### ") -> {
                            Text(
                                text = trimmed.trimStart('#', ' '),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        trimmed.isNotEmpty() -> {
                            Text(
                                text = trimmed.replace("**", ""),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardsPlayer(payloadJson: String) {
    val initialCards = remember(payloadJson) { ArtifactJsonParser.parseFlashcards(payloadJson) }
    var cards by remember(initialCards) { mutableStateOf(initialCards) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val masteredIndices = remember { mutableStateMapOf<Int, Boolean>() }

    if (cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unable to parse flashcards.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, cards.lastIndex)
    val card = cards[safeIndex]
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(stiffness = 380f),
        label = "card_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card ${safeIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${masteredIndices.values.count { it }} mastered",
                    style = MaterialTheme.typography.labelMedium,
                    color = SemanticSuccess
                )
                IconButton(
                    onClick = {
                        cards = cards.shuffled()
                        currentIndex = 0
                        isFlipped = false
                        masteredIndices.clear()
                    }
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle deck")
                }
            }
            LinearProgressIndicator(
                progress = { (safeIndex + 1).toFloat() / cards.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation <= 90f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(330.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .clip(RoundedCornerShape(28.dp))
                .clickable { isFlipped = !isFlipped }
                .testTag("flashcard_surface")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer {
                        if (rotation > 90f) rotationY = 180f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (rotation <= 90f) "PROMPT — TAP TO FLIP" else "ANSWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (rotation <= 90f) card.front else card.back,
                        style = if (rotation <= 90f) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.sourceHint.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = card.sourceHint,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        masteredIndices[safeIndex] = false
                        isFlipped = false
                        if (safeIndex < cards.lastIndex) currentIndex++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review Again")
                }
                Button(
                    onClick = {
                        masteredIndices[safeIndex] = true
                        isFlipped = false
                        if (safeIndex < cards.lastIndex) currentIndex++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Got It")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        isFlipped = false
                        if (safeIndex > 0) currentIndex--
                    },
                    enabled = safeIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous card")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }
                OutlinedButton(
                    onClick = {
                        isFlipped = false
                        if (safeIndex < cards.lastIndex) currentIndex++
                    },
                    enabled = safeIndex < cards.lastIndex
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next card")
                }
            }
        }
    }
}

@Composable
private fun PracticeQuizPlayer(payloadJson: String) {
    val questions = remember(payloadJson) { ArtifactJsonParser.parseQuiz(payloadJson) }
    val selectedAnswers = remember(payloadJson) { mutableStateMapOf<Int, Int>() }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unable to parse practice exam questions.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val answeredCount = selectedAnswers.size
    val correctCount = selectedAnswers.count { (qIdx, ansIdx) ->
        questions.getOrNull(qIdx)?.correctIndex == ansIdx
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Score: $correctCount / ${questions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$answeredCount of ${questions.size} answered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    OutlinedButton(onClick = { selectedAnswers.clear() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake")
                    }
                }
            }
        }

        itemsIndexed(questions) { qIndex, q ->
            val picked = selectedAnswers[qIndex]
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Question ${qIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium
                    )

                    q.options.forEachIndexed { optIdx, optionText ->
                        val isPicked = picked == optIdx
                        val isCorrect = q.correctIndex == optIdx
                        val borderColor = when {
                            picked == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            isCorrect -> SemanticSuccess
                            isPicked -> SemanticError
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                        val bgColor = when {
                            picked == null -> MaterialTheme.colorScheme.surface
                            isCorrect -> SemanticSuccess.copy(alpha = 0.12f)
                            isPicked -> SemanticError.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(if (isPicked || (picked != null && isCorrect)) 1.5.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = picked == null) {
                                    selectedAnswers[qIndex] = optIdx
                                }
                        ) {
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    if (picked != null) {
                        val wasRight = picked == q.correctIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = (if (wasRight) SemanticSuccess else SemanticError).copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (wasRight) "Correct!" else "Not quite — review explanation:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (wasRight) SemanticSuccess else SemanticError
                                )
                                Text(
                                    text = q.explanation,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (q.sourceHint.isNotBlank()) {
                                    Text(
                                        text = "Source: ${q.sourceHint}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDiscussionPlayerView(payloadJson: String) {
    val context = LocalContext.current
    val data = remember(payloadJson) { ArtifactJsonParser.parseAudioOverview(payloadJson) }
    val player = remember(context) { AudioOverviewPlayer(context) }

    val isReady by player.isReady.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val activeTurnIndex by player.activeTurnIndex.collectAsStateWithLifecycle()
    val playbackSpeed by player.playbackSpeed.collectAsStateWithLifecycle()
    val statusMessage by player.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(data) {
        if (data != null) {
            player.loadTurns(data.turns)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    if (data == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unable to load audio discussion script.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = data.episodeTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (data.summary.isNotBlank()) {
                    Text(
                        text = data.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledIconButton(
                            onClick = { player.togglePlayPause() },
                            enabled = isReady,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("audio_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause discussion" else "Play discussion"
                            )
                        }
                        AnimatedWaveform(isPlaying = isPlaying)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0f, 1.25f, 1.5f).forEach { speed ->
                            FilterChip(
                                selected = playbackSpeed == speed,
                                onClick = { player.setSpeed(speed) },
                                label = { Text("${speed}x") }
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(data.turns) { index, turn ->
                val isActive = index == activeTurnIndex
                val isAlex = turn.speaker.equals("Alex", ignoreCase = true)
                val speakerColor = if (isAlex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            speakerColor.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) speakerColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { player.playFromTurn(index) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = speakerColor,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Text(
                                text = turn.speaker.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = speakerColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (isActive) {
                                Text(
                                    text = "• Speaking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = speakerColor
                                )
                            }
                        }
                        Text(
                            text = turn.line,
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
private fun AnimatedWaveform(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val heights = listOf(0.4f, 0.9f, 0.6f, 1.0f, 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(28.dp)
    ) {
        heights.forEachIndexed { idx, base ->
            val anim by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = base,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 360 + idx * 90),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$idx"
            )
            val fraction = if (isPlaying) anim else 0.3f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((26f * fraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
