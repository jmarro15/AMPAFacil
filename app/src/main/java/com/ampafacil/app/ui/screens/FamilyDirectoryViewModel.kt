package com.ampafacil.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ampafacil.app.data.FamilyDirectoryItem
import com.ampafacil.app.data.FamilyDirectoryRepository

data class FamilyDirectoryUiState(
    val isLoading: Boolean = false,
    val families: List<FamilyDirectoryItem> = emptyList(),
    val errorMessage: String? = null
)

class FamilyDirectoryViewModel(
    private val repository: FamilyDirectoryRepository = FamilyDirectoryRepository()
) : ViewModel() {

    var uiState by mutableStateOf(FamilyDirectoryUiState(isLoading = true))
        private set

    init {
        loadFamilies()
    }

    fun loadFamilies() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        repository.loadFamilies { result ->
            uiState = when (result) {
                is FamilyDirectoryRepository.LoadResult.Success -> {
                    FamilyDirectoryUiState(
                        isLoading = false,
                        families = result.families,
                        errorMessage = null
                    )
                }

                is FamilyDirectoryRepository.LoadResult.Error -> {
                    FamilyDirectoryUiState(
                        isLoading = false,
                        families = emptyList(),
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}
