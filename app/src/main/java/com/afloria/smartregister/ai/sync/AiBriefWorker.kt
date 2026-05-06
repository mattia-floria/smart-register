package com.afloria.smartregister.ai.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.afloria.smartregister.ai.models.AiModels
import com.afloria.smartregister.ai.runtime.LlmChatModelHelper
import com.afloria.smartregister.ai.utils.DataPreprocessor
import com.afloria.smartregister.data.local.AuthStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AiBriefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val authStorage = AuthStorage(applicationContext)
        
        if (!authStorage.isAiBriefEnabled()) {
            return@withContext Result.success()
        }

        val agenda = authStorage.getAgenda()
        val loginResponse = authStorage.getLoginResponse()
        val studentName = loginResponse?.firstName ?: "Studente"
        
        if (agenda.isEmpty()) {
            return@withContext Result.success()
        }

        val selectedModelName = authStorage.getAiModel()
        val model = AiModels.ALL_MODELS.find { it.name == selectedModelName } ?: AiModels.ALL_MODELS.first()
        
        if (!model.isDownloaded(applicationContext)) {
            return@withContext Result.retry()
        }

        val deferred = CompletableDeferred<Result>()
        
        LlmChatModelHelper.initialize(
            context = applicationContext,
            model = model,
            supportImage = model.llmSupportImage,
            supportAudio = model.llmSupportAudio,
            onDone = { error ->
                if (error.isNotEmpty()) {
                    deferred.complete(Result.failure())
                } else {
                    val prompt = DataPreprocessor.buildAiBriefPrompt(agenda.take(10), studentName)
                    var finalBrief = ""
                    
                    LlmChatModelHelper.runInference(
                        model = model,
                        input = prompt,
                        resultListener = { part, done, _ ->
                            if (done) {
                                if (finalBrief.isNotEmpty()) {
                                    authStorage.saveAiBriefSummary(finalBrief)
                                    Log.d("AiBriefWorker", "Brief updated successfully")
                                }
                                LlmChatModelHelper.cleanUp(model) {}
                                deferred.complete(Result.success())
                            } else {
                                if (part.startsWith(finalBrief) && finalBrief.isNotEmpty()) {
                                    finalBrief = part
                                } else {
                                    finalBrief += part
                                }
                            }
                        },
                        cleanUpListener = {},
                        onError = {
                            LlmChatModelHelper.cleanUp(model) {}
                            deferred.complete(Result.failure())
                        }
                    )
                }
            }
        )

        deferred.await()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AiBriefWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag("ai_brief_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ai_brief_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
