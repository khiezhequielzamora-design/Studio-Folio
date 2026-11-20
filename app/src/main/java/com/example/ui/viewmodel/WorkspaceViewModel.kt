package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SourceExtractor
import com.example.data.StudyAiService
import com.example.data.StudyRepository
import com.example.model.ArtifactType
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.Notebook
import com.example.model.SourceType
import com.example.model.StudyArtifact
import com.example.model.StudySource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val repository: StudyRepository,
    private val aiService: StudyAiService,
    private val sourceExtractor: SourceExtractor,
    private val currentUserId: String,
    private val notebookId: String
) : ViewModel() {

    val notebookState: StateFlow<UiState<Notebook?>> =
        repository.observeNotebook(currentUserId, notebookId)
            .map<Notebook?, UiState<Notebook?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing notebook", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load notebook."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val sourcesState: StateFlow<UiState<List<StudySource>>> =
        repository.observeSources(currentUserId, notebookId)
            .map<List<StudySource>, UiState<List<StudySource>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing sources", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load sources."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val artifactsState: StateFlow<UiState<List<StudyArtifact>>> =
        repository.observeArtifacts(currentUserId, notebookId)
            .map<List<StudyArtifact>, UiState<List<StudyArtifact>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing artifacts", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load study studio."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val messagesState: StateFlow<UiState<List<ChatMessage>>> =
        repository.observeMessages(currentUserId, notebookId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing messages", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load chat history."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _importingMessage = MutableStateFlow<String?>(null)
    val importingMessage: StateFlow<String?> = _importingMessage.asStateFlow()

    private val _streamingResponse = MutableStateFlow<String?>(null)
    val streamingResponse: StateFlow<String?> = _streamingResponse.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _generatingArtifactType = MutableStateFlow<ArtifactType?>(null)
    val generatingArtifactType: StateFlow<ArtifactType?> = _generatingArtifactType.asStateFlow()

    private val _isGeneratingOverview = MutableStateFlow(false)
    val isGeneratingOverview: StateFlow<Boolean> = _isGeneratingOverview.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    fun dismissError() {
        _errorBanner.value = null
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            _importingMessage.value = "Extracting study content from file…"
            _errorBanner.value = null
            try {
                val extracted = sourceExtractor.extractFromDocumentUri(uri)
                val currentCount = (sourcesState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L
                repository.addSource(
                    notebookId = notebookId,
                    title = extracted.title,
                    sourceType = extracted.sourceType,
                    mimeType = extracted.mimeType,
                    content = extracted.content,
                    currentSourceCount = currentCount
                ).onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save extracted source."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Document import failed", e)
                _errorBanner.value = e.localizedMessage ?: "Failed to import document."
            } finally {
                _importingMessage.value = null
            }
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            _importingMessage.value = "Reading notes and diagrams from photo…"
            _errorBanner.value = null
            try {
                val extracted = sourceExtractor.extractFromImageUri(uri)
                val currentCount = (sourcesState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L
                repository.addSource(
                    notebookId = notebookId,
                    title = extracted.title,
                    sourceType = extracted.sourceType,
                    mimeType = extracted.mimeType,
                    content = extracted.content,
                    currentSourceCount = currentCount
                ).onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save image notes."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image import failed", e)
                _errorBanner.value = e.localizedMessage ?: "Failed to analyze photo."
            } finally {
                _importingMessage.value = null
            }
        }
    }

    fun addManualNote(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) {
            _errorBanner.value = "Both note title and content are required."
            return
        }
        viewModelScope.launch {
            _importingMessage.value = "Saving note…"
            _errorBanner.value = null
            try {
                val currentCount = (sourcesState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L
                repository.addSource(
                    notebookId = notebookId,
                    title = title,
                    sourceType = SourceType.NOTE,
                    mimeType = "text/plain",
                    content = content,
                    currentSourceCount = currentCount
                ).onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save note."
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _importingMessage.value = null
            }
        }
    }

    fun toggleSourceIncluded(source: StudySource) {
        viewModelScope.launch {
            try {
                repository.toggleSourceIncluded(notebookId, source.id, !source.included)
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "Could not update source toggle."
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun updateSource(sourceId: String, title: String, content: String) {
        viewModelScope.launch {
            try {
                repository.updateSourceContent(notebookId, sourceId, title, content)
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "Failed to update source."
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            try {
                val currentCount = (sourcesState.value as? UiState.Success)?.data?.size?.toLong() ?: 1L
                repository.deleteSource(notebookId, sourceId, currentCount)
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "Failed to delete source."
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun generateOverview() {
        val activeSources = activeSourcesList()
        if (activeSources.isEmpty()) {
            _errorBanner.value = "Add or enable at least one source first."
            return
        }
        viewModelScope.launch {
            _isGeneratingOverview.value = true
            _errorBanner.value = null
            try {
                val overview = aiService.generateNotebookOverview(activeSources)
                if (overview.isNotBlank()) {
                    repository.updateNotebookOverview(notebookId, overview)
                        .onFailure { err ->
                            _errorBanner.value = err.localizedMessage ?: "Failed to save overview."
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Overview failed", e)
                _errorBanner.value = e.localizedMessage ?: "Failed to generate notebook overview."
            } finally {
                _isGeneratingOverview.value = false
            }
        }
    }

    fun sendChatMessage(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty() || _isChatGenerating.value) return
        val activeSources = activeSourcesList()
        if (activeSources.isEmpty()) {
            _errorBanner.value = "Select or upload at least one source so StudyFolio can ground its answer."
            return
        }

        viewModelScope.launch {
            _isChatGenerating.value = true
            _streamingResponse.value = ""
            _errorBanner.value = null
            try {
                val saveUserResult = repository.saveChatMessage(
                    notebookId = notebookId,
                    role = ChatRole.USER,
                    text = cleanQuestion,
                    citations = emptyList()
                )
                saveUserResult.onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save message."
                    return@launch
                }

                val history = (messagesState.value as? UiState.Success)?.data.orEmpty()
                var finalAnswer = ""
                aiService.askNotebookStream(cleanQuestion, activeSources, history)
                    .collect { partial ->
                        finalAnswer = partial
                        _streamingResponse.value = partial
                    }

                val citations = aiService.extractCitedTitles(finalAnswer, activeSources)
                repository.saveChatMessage(
                    notebookId = notebookId,
                    role = ChatRole.ASSISTANT,
                    text = finalAnswer,
                    citations = citations
                ).onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save assistant reply."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed", e)
                _errorBanner.value = e.localizedMessage ?: "Failed to generate response."
            } finally {
                _streamingResponse.value = null
                _isChatGenerating.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            try {
                repository.clearChatHistory(notebookId)
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "Failed to clear chat history."
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun generateArtifact(artifactType: ArtifactType, onGenerated: (String) -> Unit = {}) {
        if (_generatingArtifactType.value != null) return
        val activeSources = activeSourcesList()
        if (activeSources.isEmpty()) {
            _errorBanner.value = "Add or enable at least one source in Sources first."
            return
        }
        val notebookTitle = (notebookState.value as? UiState.Success)?.data?.title ?: "Study Notebook"

        viewModelScope.launch {
            _generatingArtifactType.value = artifactType
            _errorBanner.value = null
            try {
                val (title, payload) = aiService.generateStudioArtifact(
                    artifactType = artifactType,
                    notebookTitle = notebookTitle,
                    sources = activeSources
                )
                val result = repository.saveArtifact(
                    notebookId = notebookId,
                    artifactType = artifactType,
                    title = title,
                    payloadJson = payload,
                    sourceTitles = activeSources.map { it.title }
                )
                result.onSuccess { artifactId ->
                    onGenerated(artifactId)
                }.onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to save ${artifactType.label}."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Artifact generation failed", e)
                _errorBanner.value = e.localizedMessage ?: "Could not generate ${artifactType.label}."
            } finally {
                _generatingArtifactType.value = null
            }
        }
    }

    fun deleteArtifact(artifactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteArtifact(notebookId, artifactId)
                    .onFailure { err ->
                        _errorBanner.value = err.localizedMessage ?: "Failed to delete artifact."
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun activeSourcesList(): List<StudySource> {
        val all = (sourcesState.value as? UiState.Success)?.data.orEmpty()
        return all.filter { it.included }
    }

    private companion object {
        const val TAG = "WorkspaceViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
