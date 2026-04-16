package com.example.aibookreader.data.local.profile

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profilePrefs by preferencesDataStore(name = "profile_prefs")

@Singleton
class ProfileAvatarStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun keyAvatar(userId: String) = stringPreferencesKey("avatar_path_$userId")

    fun localAvatarPathFlow(userId: String): Flow<String?> =
        context.profilePrefs.data.map { prefs -> prefs[keyAvatar(userId)] }

    suspend fun saveAvatarFromUri(userId: String, sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "profile_avatars").apply { mkdirs() }
            val dest = File(dir, "$userId.jpg")
            context.contentResolver.openInputStream(sourceUri).use { input ->
                requireNotNull(input) { "Не удалось открыть файл" }
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            context.profilePrefs.edit { prefs ->
                prefs[keyAvatar(userId)] = dest.absolutePath
            }
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
