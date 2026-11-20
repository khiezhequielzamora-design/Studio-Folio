package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Notebook
import com.example.model.NotebookColorToken
import com.example.ui.theme.SpineAmber
import com.example.ui.theme.SpineIndigo
import com.example.ui.theme.SpineSage
import com.example.ui.theme.SpineTeal
import com.example.ui.theme.SpineTerracotta
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    userDisplayName: String,
    onOpenNotebook: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val notebooksState by viewModel.notebooksState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var deletingNotebook by remember { mutableStateOf<Notebook?>(null) }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Welcome, ${userDisplayName.ifBlank { "Scholar" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_notebook)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_notebook_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search notebooks or subjects…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_notebooks_input")
            )

            when (val state = notebooksState) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val notebooks = state.data
                    if (notebooks.isEmpty()) {
                        EmptyNotebooksView(
                            isFiltered = searchQuery.isNotBlank(),
                            onCreateClick = { showCreateDialog = true },
                            onClearFilter = { viewModel.onSearchQueryChanged("") }
                        )
                    } else {
                        val totalSources = notebooks.sumOf { it.sourceCount }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            StatPill(label = "Notebooks", value = "${notebooks.size}")
                            StatPill(label = "Total Sources", value = "$totalSources")
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val columns = if (maxWidth >= 600.dp) 2 else 1
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(notebooks, key = { it.id }) { notebook ->
                                    NotebookCard(
                                        notebook = notebook,
                                        onClick = { onOpenNotebook(notebook.id) },
                                        onEdit = { editingNotebook = notebook },
                                        onDelete = { deletingNotebook = notebook }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NotebookEditorDialog(
            title = "Create Study Notebook",
            initialTitle = "",
            initialSubject = "",
            initialColor = NotebookColorToken.TEAL,
            busy = busy,
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, subject, color ->
                viewModel.createNotebook(title, subject, color) { createdId ->
                    showCreateDialog = false
                    onOpenNotebook(createdId)
                }
            }
        )
    }

    editingNotebook?.let { target ->
        NotebookEditorDialog(
            title = "Edit Notebook",
            initialTitle = target.title,
            initialSubject = target.subject,
            initialColor = NotebookColorToken.fromKey(target.colorToken),
            busy = busy,
            confirmLabel = "Save",
            onDismiss = { editingNotebook = null },
            onConfirm = { title, subject, color ->
                viewModel.updateNotebook(target.id, title, subject, color)
                editingNotebook = null
            }
        )
    }

    deletingNotebook?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingNotebook = null },
            title = { Text("Delete notebook?") },
            text = {
                Text(
                    "\"${target.title}\" and all its uploaded sources, study guides, flashcards, " +
                        "quizzes, and chat history will be permanently deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotebook(target.id)
                        deletingNotebook = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingNotebook = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spineColor = spineColorFor(NotebookColorToken.fromKey(notebook.colorToken))
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val updatedLabel = remember(notebook.updatedAt) {
        notebook.updatedAt?.toDate()?.let { dateFormatter.format(it) } ?: "Today"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("notebook_card_${notebook.id}")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(spineColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (notebook.subject.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = spineColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = notebook.subject.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = spineColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = notebook.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit notebook",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete notebook",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${notebook.sourceCount} ${if (notebook.sourceCount == 1L) "source" else "sources"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Updated $updatedLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotebooksView(
    isFiltered: Boolean,
    onCreateClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_sources),
            contentDescription = "Empty study shelf illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(240.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isFiltered) "No matching notebooks" else "Your study shelf is ready",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isFiltered) {
                "Try a different course name or clear your search filter."
            } else {
                "Create a notebook for a class or exam, upload your PDFs, slides, or notes, and study smarter."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (isFiltered) {
            OutlinedButton(onClick = onClearFilter) {
                Text("Clear search")
            }
        } else {
            Button(
                onClick = onCreateClick,
                modifier = Modifier.testTag("empty_create_notebook_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_notebook))
            }
        }
    }
}

@Composable
private fun NotebookEditorDialog(
    title: String,
    initialTitle: String,
    initialSubject: String,
    initialColor: NotebookColorToken,
    busy: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, NotebookColorToken) -> Unit
) {
    var name by remember { mutableStateOf(initialTitle) }
    var subject by remember { mutableStateOf(initialSubject) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 120) name = it },
                    label = { Text("Notebook title") },
                    placeholder = { Text("e.g., Organic Chemistry Exam 2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notebook_title_input")
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { if (it.length <= 60) subject = it },
                    label = { Text("Course / Subject (optional)") },
                    placeholder = { Text("e.g., CHEM 201") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notebook_subject_input")
                )
                Text(
                    text = "Cover Spine Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NotebookColorToken.entries.forEach { token ->
                        val color = spineColorFor(token)
                        val isPicked = token == selectedColor
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = if (isPicked) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            } else null,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedColor = token }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, subject, selectedColor) },
                enabled = name.isNotBlank() && !busy,
                modifier = Modifier.testTag("confirm_notebook_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        }
    )
}

fun spineColorFor(token: NotebookColorToken): Color = when (token) {
    NotebookColorToken.TEAL -> SpineTeal
    NotebookColorToken.TERRACOTTA -> SpineTerracotta
    NotebookColorToken.AMBER -> SpineAmber
    NotebookColorToken.INDIGO -> SpineIndigo
    NotebookColorToken.SAGE -> SpineSage
}
