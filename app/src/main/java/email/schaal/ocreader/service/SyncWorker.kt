package email.schaal.ocreader.service

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.api.API
import email.schaal.ocreader.util.LoginError

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val KEY_SYNC_TYPE = "KEY_SYNC_TYPE"
        const val KEY_EXCEPTION = "KEY_EXCEPTION"
    }

    override suspend fun doWork(): Result {
        val syncType: SyncType = SyncType[inputData.getString(KEY_SYNC_TYPE)] ?: SyncType.FULL_SYNC
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        if(syncType != SyncType.SYNC_CHANGES_ONLY)
            preferences.edit().putBoolean(Preferences.SYS_SYNC_RUNNING.key, true).apply()

        return try {
            API(applicationContext).sync(syncType)
            Result.success()
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(
                workDataOf(
                    KEY_EXCEPTION to LoginError.getError(
                        applicationContext, e).message
                )
            )
        } finally {
            preferences.edit().putBoolean(Preferences.SYS_SYNC_RUNNING.key, false).apply()
        }
    }
}