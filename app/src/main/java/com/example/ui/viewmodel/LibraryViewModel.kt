package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StudyRepository
import com.example.model.Notebook
import com.example.model.NotebookColorToken
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LibraryViewModel(
    private val repository: StudyRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val rawNotebooksState: StateFlow<UiState<List<Notebook>>> =
        repository.observeNotebooks(currentUserId)
            .map<List<Notebook>, UiState<List<Notebook>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing notebooks", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load notebooks."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val notebooksState: StateFlow<UiState<List<Notebook>>> =
        combine(rawNotebooksState, _searchQuery) { state, query ->
            if (state is UiState.Success && query.isNotBlank()) {
                val q = query.trim().lowercase()
                UiState.Success(
                    state.data.filter {
                        it.title.lowercase().contains(q) || it.subject.lowercase().contains(q)
                    }
                )
            } else {
                state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun createNotebook(
        title: String,
        subject: String,
        colorToken: NotebookColorToken,
        onCreated: (String) -> Unit
    ) {
        if (title.isBlank()) {
            _feedbackMessage.value = "Please enter a notebook title."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val result = repository.createNotebook(title, subject, colorToken)
                result.onSuccess { id ->
                    onCreated(id)
                }.onFailure { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Could not create notebook."
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _busy.value = false
            }
        }
    }

    fun updateNotebook(
        notebookId: String,
        title: String,
        subject: String,
        colorToken: NotebookColorToken
    ) {
        if (title.isBlank()) {
            _feedbackMessage.value = "Title cannot be empty."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.updateNotebookMetadata(notebookId, title, subject, colorToken)
                    .onFailure { err ->
                        _feedbackMessage.value = err.localizedMessage ?: "Failed to update notebook."
                    }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteNotebook(notebookId: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.deleteNotebook(notebookId)
                    .onSuccess {
                        _feedbackMessage.value = "Notebook deleted."
                    }
                    .onFailure { err ->
                        _feedbackMessage.value = err.localizedMessage ?: "Failed to delete notebook."
                    }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _busy.value = false
            }
        }
    }

    private companion object {
        const val TAG = "LibraryViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
