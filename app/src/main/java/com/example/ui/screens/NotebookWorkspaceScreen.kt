package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.StudyArtifact
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WorkspaceViewModel

enum class WorkspaceTab(val labelRes: Int) {
    SOURCES(R.string.tab_sources),
    CHAT(R.string.tab_chat),
    STUDIO(R.string.tab_studio)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookWorkspaceScreen(
    viewModel: WorkspaceViewModel,
    onBack: () -> Unit
) {
    val notebookState by viewModel.notebookState.collectAsStateWithLifecycle()
    val sourcesState by viewModel.sourcesState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()

    val importingMessage by viewModel.importingMessage.collectAsStateWithLifecycle()
    val streamingResponse by viewModel.streamingResponse.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val generatingArtifactType by viewModel.generatingArtifactType.collectAsStateWithLifecycle()
    val isGeneratingOverview by viewModel.isGeneratingOverview.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(WorkspaceTab.SOURCES) }
    var activeViewerArtifact by remember { mutableStateOf<StudyArtifact?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importImage(uri)
        }
    }

    val notebook = (notebookState as? UiState.Success)?.data
    val sources = (sourcesState as? UiState.Success)?.data.orEmpty()
    val artifacts = (artifactsState as? UiState.Success)?.data.orEmpty()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("workspace_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to library"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = notebook?.title ?: "Notebook",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val activeCount = sources.count { it.included }
                            Text(
                                text = "${sources.size} sources • $activeCount active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        WorkspaceTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        WorkspaceTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (errorBanner != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::dismissError) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    val isInitialLoading =
                        notebookState is UiState.Loading || sourcesState is UiState.Loading

                    if (isInitialLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (selectedTab) {
                            WorkspaceTab.SOURCES -> {
                                SourcesTab(
                                    notebook = notebook,
                                    sources = sources,
                                    importingMessage = importingMessage,
                                    isGeneratingOverview = isGeneratingOverview,
                                    onUploadDocumentClick = {
                                        docPickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "text/plain",
                                                "text/markdown",
                                                "text/csv"
                                            )
                                        )
                                    },
                                    onPickImageClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onAddManualNote = viewModel::addManualNote,
                                    onToggleSource = viewModel::toggleSourceIncluded,
                                    onUpdateSource = viewModel::updateSource,
                                    onDeleteSource = viewModel::deleteSource,
                                    onGenerateOverview = viewModel::generateOverview
                                )
                            }

                            WorkspaceTab.CHAT -> {
                                ChatTab(
                                    sources = sources,
                                    messages = messages,
                                    streamingResponse = streamingResponse,
                                    isGenerating = isChatGenerating,
                                    onSendMessage = viewModel::sendChatMessage,
                                    onClearChat = viewModel::clearChat
                                )
                            }

                            WorkspaceTab.STUDIO -> {
                                StudioTab(
                                    sources = sources,
                                    artifacts = artifacts,
                                    generatingArtifactType = generatingArtifactType,
                                    onGenerateArtifact = { type ->
                                        viewModel.generateArtifact(type) { createdId ->
                                            val latest = (viewModel.artifactsState.value as? UiState.Success)
                                                ?.data
                                                ?.firstOrNull { it.id == createdId }
                                            if (latest != null) {
                                                activeViewerArtifact = latest
                                            }
                                        }
                                    },
                                    onOpenArtifact = { activeViewerArtifact = it },
                                    onDeleteArtifact = viewModel::deleteArtifact
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeViewerArtifact?.let { artifact ->
        ArtifactViewerDialog(
            artifact = artifact,
            onDismiss = { activeViewerArtifact = null }
        )
    }
}

private fun tabIcon(tab: WorkspaceTab, selected: Boolean) = when (tab) {
    WorkspaceTab.SOURCES -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    WorkspaceTab.CHAT -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
    WorkspaceTab.STUDIO -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
}
