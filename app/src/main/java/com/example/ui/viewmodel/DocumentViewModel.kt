package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ApryseDocumentModel
import com.example.data.model.DocumentFormat
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State representation for Apryse Document Viewer and Editor.
 */
sealed interface DocumentUiState {
    data object Idle : DocumentUiState
    data class Loading(val message: String = "Preparing document...") : DocumentUiState
    data class Success(val document: ApryseDocumentModel) : DocumentUiState
    data class Error(val message: String, val throwable: Throwable? = null) : DocumentUiState
}

/**
 * ViewModel managing client-side document processing, format selection,
 * Apryse viewer state, and export operations.
 */
class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Idle)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _selectedFormatFilter = MutableStateFlow<DocumentFormat?>(null)
    val selectedFormatFilter: StateFlow<DocumentFormat?> = _selectedFormatFilter.asStateFlow()

    private val _recentDocuments = MutableStateFlow<List<ApryseDocumentModel>>(emptyList())
    val recentDocuments: StateFlow<List<ApryseDocumentModel>> = _recentDocuments.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun setFormatFilter(format: DocumentFormat?) {
        _selectedFormatFilter.value = format
    }

    fun loadDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = DocumentUiState.Loading("Loading and preparing document for Apryse...")
            val result = repository.prepareDocumentFromUri(uri)
            result.onSuccess { doc ->
                _uiState.value = DocumentUiState.Success(doc)
                addToRecent(doc)
            }.onFailure { err ->
                _uiState.value = DocumentUiState.Error(
                    message = err.message ?: "Failed to open document",
                    throwable = err
                )
            }
        }
    }

    fun loadSampleDocument(format: DocumentFormat) {
        viewModelScope.launch {
            _uiState.value = DocumentUiState.Loading("Generating sample ${format.displayName}...")
            val result = repository.createSampleDocument(format)
            result.onSuccess { doc ->
                _uiState.value = DocumentUiState.Success(doc)
                addToRecent(doc)
            }.onFailure { err ->
                _uiState.value = DocumentUiState.Error(
                    message = err.message ?: "Failed to create sample document",
                    throwable = err
                )
            }
        }
    }

    fun saveDocument(destinationUri: Uri) {
        val currentState = _uiState.value
        if (currentState !is DocumentUiState.Success) return

        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveDocumentToUri(currentState.document, destinationUri)
            _isSaving.value = false
            result.onSuccess {
                _userMessage.value = "Document saved successfully"
            }.onFailure { err ->
                _userMessage.value = "Failed to save: ${err.message}"
            }
        }
    }

    fun exportAsPdf(destinationUri: Uri) {
        val currentState = _uiState.value
        if (currentState !is DocumentUiState.Success) return

        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.exportDocumentAsPdf(currentState.document, destinationUri)
            _isSaving.value = false
            result.onSuccess {
                _userMessage.value = "Exported as PDF successfully"
            }.onFailure { err ->
                _userMessage.value = "Export failed: ${err.message}"
            }
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun clearError() {
        _uiState.value = DocumentUiState.Idle
    }

    fun closeDocument() {
        _uiState.value = DocumentUiState.Idle
    }

    private fun addToRecent(doc: ApryseDocumentModel) {
        _recentDocuments.update { current ->
            val filtered = current.filterNot { it.localFilePath == doc.localFilePath }
            listOf(doc) + filtered.take(19)
        }
    }
}
