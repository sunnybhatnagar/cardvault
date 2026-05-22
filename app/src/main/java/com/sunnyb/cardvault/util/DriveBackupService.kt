package com.sunnyb.cardvault.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object DriveBackupService {

    private const val BACKUP_FOLDER = "CardVaultBackups"
    private const val APP_NAME = "Card Vault"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getSignInIntent(context: Context): Intent {
        return getSignInClient(context).signInIntent
    }

    private suspend fun getDriveService(context: Context): Drive? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    private suspend fun getOrCreateBackupFolder(drive: Drive): String? = withContext(Dispatchers.IO) {
        try {
            val query = drive.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER' and trashed=false")
                .setFields("files(id, name)")
                .execute()
            val existing = query.files?.firstOrNull()
            if (existing != null) return@withContext existing.id

            val folder = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FOLDER
                mimeType = "application/vnd.google-apps.folder"
            }
            drive.files().create(folder).execute().id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadBackup(context: Context, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context) ?: return@withContext false
            val folderId = getOrCreateBackupFolder(drive) ?: return@withContext false

            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())

            val file = com.google.api.services.drive.model.File().apply {
                name = "cardvault_backup_$dateStr.json"
                parents = listOf(folderId)
            }

            val contentStream = ByteArrayContent("application/json", jsonContent.toByteArray())
            drive.files().create(file, contentStream).execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listBackups(context: Context): List<com.google.api.services.drive.model.File> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context) ?: return@withContext emptyList()
            val folderId = getOrCreateBackupFolder(drive) ?: return@withContext emptyList()

            val result = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, createdTime)")
                .execute()
            result.files ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadBackupJson(context: Context, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context) ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            null
        }
    }

    fun isSignedIn(context: Context): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun signOut(context: Context) {
        getSignInClient(context).signOut()
    }
}