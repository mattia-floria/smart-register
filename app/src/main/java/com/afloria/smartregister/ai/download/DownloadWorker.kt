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

    // Token rimosso per sicurezza
    private val encodedToken = ""
    private val hfToken: String
        get() = ""

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
        // Permetti di passare un token personalizzato tramite inputData
        val customToken = inputData.getString("hf_token")
        val activeToken = if (!customToken.isNullOrBlank()) customToken else hfToken

        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo(0, modelName))

                val url = URL(fileUrl)
                var currentUrl = fileUrl
                var currentConnection = url.openConnection() as HttpURLConnection
                currentConnection.instanceFollowRedirects = false 
                currentConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                currentConnection.setRequestProperty("Accept", "*/*")
                
                if (currentUrl.contains("huggingface.co") && activeToken.isNotBlank()) {
                    currentConnection.setRequestProperty("Authorization", "Bearer $activeToken")
                }
                
                currentConnection.connect()
                var responseCode = currentConnection.responseCode
                Log.d(TAG, "Initial request to $currentUrl returned $responseCode")

                // Gestione manuale dei redirect
                var redirectCount = 0
                while ((responseCode in 300..399) && redirectCount < 10) {
                    val location = currentConnection.getHeaderField("Location") ?: break
                    currentConnection.disconnect()
                    
                    val nextUrl = URL(URL(currentUrl), location).toString()
                    currentUrl = nextUrl
                    redirectCount++
                    
                    currentConnection = URL(nextUrl).openConnection() as HttpURLConnection
                    currentConnection.instanceFollowRedirects = false
                    currentConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    
                    // Invia il token SOLO se siamo su huggingface.co e NON siamo già stati reindirizzati a storage esterni (S3/CloudFront)
                    // Nota: cdn-lfs.huggingface.co fa parte di HF, ma spesso i problemi nascono qui
                    if (nextUrl.contains("huggingface.co") && !nextUrl.contains("amazonaws.com") && !nextUrl.contains("cloudfront.net") && activeToken.isNotBlank()) {
                        currentConnection.setRequestProperty("Authorization", "Bearer $activeToken")
                    }
                    
                    currentConnection.connect()
                    responseCode = currentConnection.responseCode
                    Log.d(TAG, "Redirect #$redirectCount to $currentUrl returned $responseCode")
                }

                if (responseCode !in 200..299) {
                    Log.e(TAG, "Server returned error code: $responseCode for URL: $currentUrl")
                    val errorMsg = when(responseCode) {
                        401 -> "Non autorizzato (401). Il token Hugging Face non è valido o non ha accesso a questo modello."
                        403 -> "Accesso negato (403). Assicurati di aver accettato i termini della licenza su Hugging Face per questo modello gated."
                        404 -> "File non trovato (404) sul server."
                        else -> "Errore del server: $responseCode"
                    }
                    return@withContext Result.failure(Data.Builder().putString("error", errorMsg).build())
                }

                val contentLength = currentConnection.contentLengthLong
                val expectedBytes = if (contentLength > 0) contentLength else totalBytes

                val outputDir = File(applicationContext.getExternalFilesDir("llm_models"), "$modelDir/$version")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val outputFile = File(outputDir, fileName)
                // Usiamo un file temporaneo per evitare di considerare il download completato se interrotto
                val tempFile = File(outputDir, "$fileName.tmp")
                
                val inputStream = currentConnection.inputStream
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastUpdate = 0L

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 500) {
                            val progress = if (expectedBytes > 0) (downloadedBytes * 100 / expectedBytes).toInt() else 0
                            setProgress(Data.Builder()
                                .putLong("receivedBytes", downloadedBytes)
                                .putLong("totalBytes", expectedBytes)
                                .build())
                            setForeground(createForegroundInfo(progress, modelName))
                            lastUpdate = now
                        }
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                }

                // Verifica che il download sia completo
                if (expectedBytes > 0 && downloadedBytes < expectedBytes) {
                    tempFile.delete()
                    Log.e(TAG, "Download incomplete: expected $expectedBytes, got $downloadedBytes")
                    return@withContext Result.failure(Data.Builder().putString("error", "Download incompleto").build())
                }

                // Rinomina il file temporaneo al nome finale
                if (outputFile.exists()) outputFile.delete()
                if (tempFile.renameTo(outputFile)) {
                    Log.d(TAG, "Download completed successfully: ${outputFile.absolutePath}")
                    Result.success()
                } else {
                    Log.e(TAG, "Failed to rename temp file to final destination")
                    Result.failure(Data.Builder().putString("error", "Errore nel salvataggio del file").build())
                }
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
