package it.matteogrechi.gymassystant.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import it.matteogrechi.gymassystant.data.BackupWorker
import it.matteogrechi.gymassystant.data.LocalDocuments
import it.matteogrechi.gymassystant.domain.ClockReading
import it.matteogrechi.gymassystant.domain.Dataset
import it.matteogrechi.gymassystant.domain.Plan
import it.matteogrechi.gymassystant.domain.Training
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// GYM-001/018/023/027: screens issue domain events; only committed Room state reaches UI.
class GymViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val container = application as GymApplication
    val dataset = container.repository.state
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val editingPlans = mutableStateListOf<String>().apply {
        addAll(savedState.get<List<String>>("editingPlans") ?: emptyList())
    }

    fun isEditing(id: String): Boolean = id in editingPlans
    fun setEditing(id: String, enabled: Boolean) {
        if (enabled && id !in editingPlans) editingPlans.add(id)
        if (!enabled) editingPlans.remove(id)
        savedState["editingPlans"] = editingPlans.toList()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                container.repository.load()
                container.repository.update { Training.checkpoint(it, now()) }
                BackupWorker.enqueue(application)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableError.value = true
            }
        }
    }

    fun now(): ClockReading = container.clock.read()
    fun clearError() { mutableError.value = false }

    fun change(action: (Dataset, ClockReading) -> Dataset, onSuccess: (Dataset) -> Unit = {}) {
        val reading = now()
        viewModelScope.launch {
            try {
                val committed = withContext(Dispatchers.IO) { container.repository.update { action(it, reading) } }
                onSuccess(committed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableError.value = true
            }
        }
    }

    fun editDraft(planId: String, transform: (Plan) -> Plan) = change({ dataset, _ ->
        val plan = dataset.drafts.single { it.plan.id == planId }.plan
        Training.retainDraft(dataset, transform(plan))
    })

    fun checkpoint() = change({ dataset, clock -> Training.checkpoint(dataset, clock) })

    fun importVideo(uri: Uri, exerciseId: String) {
        viewModelScope.launch {
            mutableBusy.value = true
            try {
                val media = withContext(Dispatchers.IO) { LocalDocuments.importVideo(container, uri, exerciseId) }
                withContext(Dispatchers.IO) { container.repository.update { Training.attachMedia(it, media) } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableError.value = true
            } finally {
                mutableBusy.value = false
            }
        }
    }
}
