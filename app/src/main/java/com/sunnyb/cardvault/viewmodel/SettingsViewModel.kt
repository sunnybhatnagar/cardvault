package com.sunnyb.cardvault.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.ui.theme.ThemeMode
import com.sunnyb.cardvault.util.DriveBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TimeoutOption(val label: String, val ms: Long)

class SettingsViewModel : ViewModel() {

    private val repository = CardVaultApp.instance.cardRepository
    private val sessionManager = CardVaultApp.instance.sessionManager
    private val encryptionManager = CardVaultApp.instance.encryptionManager

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    private val _currentTimeoutMs = MutableStateFlow(sessionManager.timeoutMs)
    val currentTimeoutMs: StateFlow<Long> = _currentTimeoutMs.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var pendingCards: List<Card> = emptyList()

    private val _driveState = MutableStateFlow<DriveState>(DriveState.Idle)
    val driveState: StateFlow<DriveState> = _driveState.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<com.google.api.services.drive.model.File>>(emptyList())
    val driveBackups: StateFlow<List<com.google.api.services.drive.model.File>> = _driveBackups.asStateFlow()

    sealed class ExportState {
        data object Idle : ExportState()
        data object Exporting : ExportState()
        data class Success(val filePath: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    sealed class RestoreState {
        data object Idle : RestoreState()
        data class PendingConfirmation(val cardCount: Int) : RestoreState()
        data object Restoring : RestoreState()
        data object Success : RestoreState()
        data class Error(val message: String) : RestoreState()
    }

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("cardvault_theme", Context.MODE_PRIVATE)
        val mode = ThemeMode.valueOf(prefs.getString("theme_mode", "DARK") ?: "DARK")
        _themeMode.value = mode
        CardVaultApp.instance.themeMode = mode
    }

    fun toggleTheme(context: Context) {
        val newMode = if (_themeMode.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        _themeMode.value = newMode
        CardVaultApp.instance.themeMode = newMode
        context.getSharedPreferences("cardvault_theme", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", newMode.name).apply()
    }

    fun setLockTimeout(ms: Long) {
        sessionManager.setTimeoutMs(ms)
        _currentTimeoutMs.value = ms
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun parseBackupFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val encrypted = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.readBytes()
                } ?: throw Exception("Could not read file")

                val decrypted = encryptionManager.decryptData(encrypted)
                val jsonString = String(decrypted)
                val jsonArr = JSONArray(jsonString)

                val cards = mutableListOf<Card>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    cards.add(Card(
                        nickname = obj.optString("nickname", ""),
                        issuer = obj.optString("issuer", "").ifEmpty { null },
                        cardNumber = obj.optString("cardNumber", ""),
                        expiry = obj.optString("expiry", ""),
                        cvv = obj.optString("cvv", ""),
                        categoryId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId", -1).let { if (it == -1L) null else it },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
                pendingCards = cards
                _restoreState.value = RestoreState.PendingConfirmation(cards.size)
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error("Invalid or corrupted backup file")
            }
        }
    }

    fun confirmRestore() {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Restoring
            try {
                repository.deleteAllCards()
                for (card in pendingCards) {
                    repository.insertCard(card)
                }
                pendingCards = emptyList()
                _restoreState.value = RestoreState.Success
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun cancelRestore() {
        pendingCards = emptyList()
        _restoreState.value = RestoreState.Idle
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val cards = repository.allCards.first()
                val jsonBytes = buildBackupJson(cards).toString(2).toByteArray()
                val encrypted = encryptionManager.encryptData(jsonBytes)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(encrypted)
                    }
                }

                _exportState.value = ExportState.Success(uri.toString())
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    sealed class DriveState {
        data object Idle : DriveState()
        data object SigningIn : DriveState()
        data object BackingUp : DriveState()
        data class ListingBackups(val backups: List<String>) : DriveState()
        data object Restoring : DriveState()
        data object Success : DriveState()
        data class Error(val message: String) : DriveState()
    }

    fun onDriveSignedIn() {
        _driveState.value = DriveState.Idle
    }

    fun backupToDrive(context: Context) {
        viewModelScope.launch {
            _driveState.value = DriveState.BackingUp
            try {
                val cards = repository.allCards.first()
                val jsonBytes = buildBackupJson(cards).toString(2).toByteArray()
                val encrypted = encryptionManager.encryptData(jsonBytes)
                val b64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
                val ok = DriveBackupService.uploadBackup(context, b64)
                _driveState.value = if (ok) DriveState.Success else DriveState.Error("Upload failed")
            } catch (e: Exception) {
                _driveState.value = DriveState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun listDriveBackups(context: Context) {
        viewModelScope.launch {
            _driveState.value = DriveState.ListingBackups(emptyList())
            val files = DriveBackupService.listBackups(context)
            _driveBackups.value = files
        }
    }

    fun restoreFromDrive(context: Context, fileId: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.Restoring
            try {
                val b64 = DriveBackupService.downloadBackupJson(context, fileId)
                    ?: throw Exception("Could not download backup")
                val encrypted = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
                val decrypted = encryptionManager.decryptData(encrypted)
                val jsonArr = JSONArray(String(decrypted))

                val cards = mutableListOf<Card>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    cards.add(Card(
                        nickname = obj.optString("nickname", ""),
                        issuer = obj.optString("issuer", "").ifEmpty { null },
                        cardNumber = obj.optString("cardNumber", ""),
                        expiry = obj.optString("expiry", ""),
                        cvv = obj.optString("cvv", ""),
                        categoryId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId", -1).let { if (it == -1L) null else it },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
                repository.deleteAllCards()
                for (card in cards) {
                    repository.insertCard(card)
                }
                _driveState.value = DriveState.Success
            } catch (e: Exception) {
                _driveState.value = DriveState.Error(e.message ?: "Restore failed")
            }
        }
    }

    fun resetDriveState() {
        _driveState.value = DriveState.Idle
    }

    private fun buildBackupJson(cards: List<Card>): JSONArray {
        val arr = JSONArray()
        cards.forEach { card ->
            arr.put(JSONObject().apply {
                put("nickname", card.nickname)
                put("issuer", card.issuer ?: "")
                put("cardNumber", card.cardNumber)
                put("expiry", card.expiry)
                put("cvv", card.cvv)
                put("categoryId", card.categoryId ?: JSONObject.NULL)
                put("createdAt", card.createdAt)
            })
        }
        return arr
    }

    companion object {
        val TIMEOUT_OPTIONS = listOf(
            TimeoutOption("Immediately", 0L),
            TimeoutOption("15 seconds", 15_000L),
            TimeoutOption("30 seconds", 30_000L),
            TimeoutOption("1 minute", 60_000L),
            TimeoutOption("5 minutes", 300_000L),
            TimeoutOption("Never", -1L)
        )
    }
}