package email.schaal.ocreader.service

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import androidx.work.*
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.api.API
import email.schaal.ocreader.util.LoginError

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val KEY_SYNC_TYPE = "KEY_SYNC_TYPE"
        const val KEY_EXCEPTION = "KEY_EXCEPTION"
        const val WORK_ID = "WORK_ID_SYNC"

        fun sync(context: Context, syncType: SyncType): LiveData<WorkInfo> {
            val workManager = WorkManager.getInstance(context)
            val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(KEY_SYNC_TYPE to syncType.action))
                .build()
            workManager.enqueueUniqueWork(WORK_ID, ExistingWorkPolicy.KEEP, syncWork)
            return workManager.getWorkInfoByIdLiveData(syncWork.id)
        }

        fun getLiveData(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_ID)
        }
    }

    override suspend fun doWork(): Result {
        val syncType: SyncType = SyncType[inputData.getString(KEY_SYNC_TYPE)] ?: SyncType.FULL_SYNC

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
        }
    }
}