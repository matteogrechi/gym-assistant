package it.matteogrechi.gymassystant.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.work.WorkManager
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymApplication
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.data.BackupWorker
import it.matteogrechi.gymassystant.data.LocalDocuments
import it.matteogrechi.gymassystant.data.PortableFiles
import it.matteogrechi.gymassystant.domain.Dataset
import it.matteogrechi.gymassystant.domain.Language
import it.matteogrechi.gymassystant.domain.Theme
import it.matteogrechi.gymassystant.domain.Training
import it.matteogrechi.gymassystant.ui.ButtonEmphasis
import it.matteogrechi.gymassystant.ui.Disclosure
import it.matteogrechi.gymassystant.ui.DropdownField
import it.matteogrechi.gymassystant.ui.GymButton
import it.matteogrechi.gymassystant.ui.GymColors
import it.matteogrechi.gymassystant.ui.Heading
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsExport(val mime: String, val stem: String, val extension: String) {
    Json("application/json", "status", "json"),
    Csv("text/csv", "statistics", "csv"),
    Guide("text/plain", "recovery-guide", "txt"),
}

// GYM-022/023/024/027: settings change private state; only verified local writes report success.
@Composable
fun SettingsFeature(dataset: Dataset, vm: GymViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    val scope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf<Int?>(null) }
    var messageError by rememberSaveable { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var folderBusy by rememberSaveable { mutableStateOf(false) }
    // Only a cache filename crosses activity recreation; never put a dataset in saved-instance state.
    var exportCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var writing by remember { mutableStateOf(false) }
    var verifiedBackup by remember(dataset.revision, dataset.settings.backupTreeUri) { mutableStateOf(false) }
    var retryRequested by remember { mutableStateOf(false) }
    val workManager = remember(app) { WorkManager.getInstance(app) }
    val work by remember(workManager) {
        workManager.getWorkInfosForUniqueWorkFlow(BackupWorker.UNIQUE_WORK)
    }.collectAsState(initial = emptyList())
    val backupRunning = work.any { !it.state.isFinished }
    val localFolderLabel = stringResource(R.string.settings_local_folder)

    LaunchedEffect(dataset.revision, dataset.settings.backupTreeUri, work) {
        verifiedBackup = withContext(Dispatchers.IO) {
            try {
                val stored = app.repository.stored()
                val uri = dataset.settings.backupTreeUri ?: return@withContext false
                LocalDocuments.tree(app, Uri.parse(uri))
                stored.backupRevision >= stored.revision && stored.revision == dataset.revision
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
        if (work.isNotEmpty()) retryRequested = false
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            folderBusy = false
        } else {
            val grant = result.data!!.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            scope.launch {
                try {
                    val label = withContext(Dispatchers.IO) {
                        LocalDocuments.requireLocal(uri, tree = true)
                        require(grant == (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                        app.contentResolver.takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        LocalDocuments.tree(app, uri).name?.takeIf(String::isNotBlank) ?: localFolderLabel
                    }
                    vm.change({ current, _ ->
                        Training.updateSettings(current, current.settings.copy(
                            backupTreeUri = uri.toString(), backupLabel = label,
                        ))
                    })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = R.string.settings_folder_error
                    messageError = true
                } finally {
                    folderBusy = false
                }
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        val captureName = exportCapture
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            exportCapture = null
            if (captureName != null) scope.launch(Dispatchers.IO) { File(app.cacheDir, captureName).delete() }
        } else {
            writing = true
            message = R.string.settings_saving_file
            messageError = false
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val capture = File(app.cacheDir, requireNotNull(captureName))
                        writeSettingsExport(app, uri, capture)
                    }
                    message = R.string.export_saved
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = R.string.settings_export_error
                    messageError = true
                } finally {
                    withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                        if (captureName != null) File(app.cacheDir, captureName).delete()
                    }
                    exportCapture = null
                    writing = false
                }
            }
        }
    }

    fun export(kind: SettingsExport) {
        if (preparing || writing || exportCapture != null) return
        val captured = vm.dataset.value ?: dataset
        val clock = vm.now()
        preparing = true
        message = R.string.settings_preparing_file
        messageError = false
        scope.launch {
            var capture: File? = null
            try {
                capture = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "unknown"
                    val text = when (kind) {
                        SettingsExport.Json -> PortableFiles.json(captured, clock, version)
                        SettingsExport.Csv -> PortableFiles.csv(PortableFiles.snapshot(captured, clock), clock.observation.localDate)
                        SettingsExport.Guide -> PortableFiles.guide()
                    }
                    File.createTempFile("gym-export-", ".${kind.extension}", app.cacheDir).also { file ->
                        try { file.writeText(text, Charsets.UTF_8) }
                        catch (failure: Exception) { file.delete(); throw failure }
                    }
                }
                exportCapture = capture.name
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(kind.mime)
                    .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    .putExtra(Intent.EXTRA_TITLE, "gym-assystant-${kind.stem}-${clock.observation.instant.replace(':', '-')}.${kind.extension}")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                documentPicker.launch(intent)
                message = null
            } catch (cancelled: CancellationException) {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { capture?.delete() }
                exportCapture = null
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.IO) { capture?.delete() }
                exportCapture = null
                message = R.string.settings_export_error
                messageError = true
            } finally {
                preparing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Heading(stringResource(R.string.settings), back = onBack)
        Spacer(Modifier.height(12.dp))
        val languages = listOf(Language.SYSTEM, Language.EN, Language.IT, Language.ES)
        DropdownField(stringResource(R.string.language),
            options = listOf(stringResource(R.string.system_default), stringResource(R.string.settings_english),
                stringResource(R.string.settings_italian), stringResource(R.string.settings_spanish)),
            selected = languages.indexOf(dataset.settings.language), onSelect = { index ->
                val language = languages[index]
                vm.change({ current, _ -> Training.updateSettings(current, current.settings.copy(language = language)) },
                    onSuccess = {
                        val tag = when (language) {
                            Language.SYSTEM -> ""
                            Language.EN -> "en"
                            Language.IT -> "it"
                            Language.ES -> "es"
                        }
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    })
            })
        Spacer(Modifier.height(18.dp))
        val themes = listOf(Theme.SYSTEM, Theme.LIGHT, Theme.DARK)
        DropdownField(stringResource(R.string.theme),
            options = listOf(stringResource(R.string.system_default), stringResource(R.string.light), stringResource(R.string.dark)),
            selected = themes.indexOf(dataset.settings.theme), onSelect = { index ->
                vm.change({ current, _ -> Training.updateSettings(current, current.settings.copy(theme = themes[index])) })
            })
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.your_data), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Surface(color = GymColors.surfaceContainer, contentColor = GymColors.onSurface,
            shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.save_location), style = MaterialTheme.typography.labelSmall)
                Text(dataset.settings.backupLabel ?: stringResource(R.string.no_folder),
                    modifier = Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodyLarge)
                GymButton(stringResource(R.string.change_folder), onClick = {
                    message = null
                    folderBusy = true
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                        dataset.settings.backupTreeUri?.let { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(it)) }
                        folderPicker.launch(intent)
                    } catch (_: Exception) {
                        folderBusy = false
                        message = R.string.settings_folder_error
                        messageError = true
                    }
                }, enabled = !folderBusy, emphasis = ButtonEmphasis.Outlined)
            }
        }
        Text(stringResource(R.string.folder_note), modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            style = MaterialTheme.typography.bodySmall, color = GymColors.onSurfaceVariant)
        if (dataset.settings.backupTreeUri != null) {
            Text(stringResource(if (verifiedBackup) R.string.backup_saved else R.string.backup_pending),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall, color = GymColors.onSurfaceVariant)
            if (!verifiedBackup) {
                GymButton(stringResource(R.string.retry_backup), onClick = {
                    retryRequested = true
                    try { BackupWorker.enqueue(app) }
                    catch (_: Exception) { retryRequested = false }
                }, enabled = !backupRunning && !retryRequested, emphasis = ButtonEmphasis.Text)
            }
            Spacer(Modifier.height(10.dp))
        }
        val canExport = !preparing && !writing && exportCapture == null
        GymButton(stringResource(R.string.status_json), { export(SettingsExport.Json) },
            modifier = Modifier.fillMaxWidth(), enabled = canExport)
        Spacer(Modifier.height(10.dp))
        GymButton(stringResource(R.string.statistics_csv), { export(SettingsExport.Csv) },
            modifier = Modifier.fillMaxWidth(), enabled = canExport, emphasis = ButtonEmphasis.Outlined)
        if (message != null) {
            Text(stringResource(message!!), modifier = Modifier.padding(top = 8.dp).semantics {
                liveRegion = if (messageError) LiveRegionMode.Assertive else LiveRegionMode.Polite
            }, style = MaterialTheme.typography.bodySmall,
                color = if (messageError) MaterialTheme.colorScheme.error else GymColors.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Disclosure(stringResource(R.string.about_files)) {
            Text(stringResource(R.string.files_note), style = MaterialTheme.typography.bodySmall,
                color = GymColors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            GymButton(stringResource(R.string.recovery_guide), { export(SettingsExport.Guide) },
                enabled = canExport, emphasis = ButtonEmphasis.Text)
        }
    }
}

private suspend fun writeSettingsExport(context: Context, uri: Uri, capture: File) {
    LocalDocuments.requireLocal(uri)
    require(capture.isFile)
    val resolver = context.contentResolver
    requireNotNull(resolver.openOutputStream(uri, "wt")).use { output ->
        capture.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
        }
    }
    requireNotNull(resolver.openInputStream(uri)).use { actual ->
        capture.inputStream().use { expected -> verifySettingsExport(expected, actual) }
    }
}

// Separate stream verification allows bounded checks without a device/document provider.
internal suspend fun verifySettingsExport(expected: InputStream, actual: InputStream) {
    val verification = DataInputStream(actual)
    val expectedBytes = ByteArray(64 * 1024)
    val actualBytes = ByteArray(64 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = expected.read(expectedBytes)
        if (count < 0) break
        verification.readFully(actualBytes, 0, count)
        for (index in 0 until count) check(expectedBytes[index] == actualBytes[index]) { "Export verification failed" }
    }
    check(verification.read() == -1) { "Export verification failed" }
}
