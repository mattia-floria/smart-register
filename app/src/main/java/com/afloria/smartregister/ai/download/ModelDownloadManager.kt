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
        val modelFile = File(model.getPath(context))
        
        return workManager.getWorkInfosByTagFlow(model.name).map { workInfos ->
            val workInfo = workInfos.firstOrNull()
            
            // Se il file esiste ed è di dimensioni ragionevoli, lo consideriamo scaricato
            // (a meno che non ci sia un lavoro in corso che lo sta sovrascrivendo)
            if (modelFile.exists() && modelFile.length() > 0 && (workInfo == null || workInfo.state != WorkInfo.State.RUNNING)) {
                if (model.sizeInBytes <= 0 || modelFile.length() >= model.sizeInBytes) {
                    return@map ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
                }
            }
            
            if (workInfo == null) return@map ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val receivedBytes = workInfo.progress.getLong("receivedBytes", 0L)
                    val totalBytes = workInfo.progress.getLong("totalBytes", model.sizeInBytes)
                    ModelDownloadStatus(
                        ModelDownloadStatusType.IN_PROGRESS,
                        totalBytes = if (totalBytes > 0) totalBytes else model.sizeInBytes,
                        receivedBytes = receivedBytes
                    )
                }
                WorkInfo.State.SUCCEEDED -> {
                    if (modelFile.exists() && modelFile.length() > 0) {
                        ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
                    } else {
                        ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
                    }
                }
                WorkInfo.State.FAILED -> ModelDownloadStatus(
                    ModelDownloadStatusType.FAILED,
                    errorMessage = workInfo.outputData.getString("error") ?: "Unknown error"
                )
                WorkInfo.State.CANCELLED -> ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
                else -> ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            }
        }
    }
}
