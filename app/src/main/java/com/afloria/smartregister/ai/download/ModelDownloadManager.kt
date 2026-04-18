package com.afloria.smartregister.ai.download

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afloria.smartregister.ai.data.Model
import com.afloria.smartregister.ai.data.ModelDownloadStatus
import com.afloria.smartregister.ai.data.ModelDownloadStatusType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ModelDownloadManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun downloadModel(model: Model) {
        val data = Data.Builder()
            .putString("url", model.url)
            .putString("name", model.displayName)
            .putString("fileName", model.downloadFileName)
            .putString("modelDir", model.normalizedName)
            .putString("version", model.version)
            .putLong("totalBytes", model.sizeInBytes)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag(model.name)
            .build()

        workManager.enqueueUniqueWork(
            model.name,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun deleteModel(model: Model) {
        workManager.cancelUniqueWork(model.name)
        val modelDir = File(context.getExternalFilesDir("llm_models"), "${model.normalizedName}/${model.version}")
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
    }

    fun getDownloadStatus(model: Model): Flow<ModelDownloadStatus> {
        return workManager.getWorkInfosByTagFlow(model.name).map { workInfos ->
            val workInfo = workInfos.firstOrNull() ?: return@map ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val receivedBytes = workInfo.progress.getLong("receivedBytes", 0L)
                    ModelDownloadStatus(
                        ModelDownloadStatusType.IN_PROGRESS,
                        totalBytes = model.sizeInBytes,
                        receivedBytes = receivedBytes
                    )
                }
                WorkInfo.State.SUCCEEDED -> ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
                WorkInfo.State.FAILED -> ModelDownloadStatus(
                    ModelDownloadStatusType.FAILED,
                    errorMessage = workInfo.outputData.getString("error") ?: "Unknown error"
                )
                else -> ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            }
        }
    }
}
