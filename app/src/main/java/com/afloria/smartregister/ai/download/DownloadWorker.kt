package com.afloria.smartregister.ai.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.afloria.smartregister.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DownloadWorker"
private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "model_download_channel"

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationId: Int = params.id.hashCode()

    init {
        val channel = NotificationChannel(
            FOREGROUND_NOTIFICATION_CHANNEL_ID,
            "Model Downloading",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString("url") ?: return Result.failure()
        val modelName = inputData.getString("name") ?: "Model"
        val fileName = inputData.getString("fileName") ?: "model.bin"
        val modelDir = inputData.getString("modelDir") ?: "default"
        val version = inputData.getString("version") ?: "1"
        val totalBytes = inputData.getLong("totalBytes", 0L)

        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo(0, modelName))

                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                if (fileUrl.contains("huggingface.co")) {
                    connection.setRequestProperty("Authorization", "Bearer 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")
                }
                
                val outputDir = File(applicationContext.getExternalFilesDir("llm_models"), "$modelDir/$version")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val outputFile = File(outputDir, fileName)
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 500) {
                        val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                        setProgress(Data.Builder()
                            .putLong("receivedBytes", downloadedBytes)
                            .build())
                        setForeground(createForegroundInfo(progress, modelName))
                        lastUpdate = now
                    }
                }

                outputStream.close()
                inputStream.close()
                Result.success()
            } catch (e: IOException) {
                Log.e(TAG, "Download failed", e)
                Result.failure(Data.Builder().putString("error", e.message).build())
            }
        }
    }

    private fun createForegroundInfo(progress: Int, modelName: String): ForegroundInfo {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, FOREGROUND_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $modelName")
            .setContentText("Progress: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()

        return ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
