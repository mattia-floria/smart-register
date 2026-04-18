package com.afloria.smartregister.ai.data

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.io.File

data class ModelDataFile(
    val name: String,
    val url: String,
    val downloadFileName: String,
    val sizeInBytes: Long,
)

private val NORMALIZE_NAME_REGEX = Regex("[^a-zA-Z0-9]")

enum class RuntimeType {
    @SerializedName("unknown") UNKNOWN,
    @SerializedName("litert_lm") LITERT_LM,
}

data class Model(
    val name: String,
    val displayName: String = "",
    val info: String = "",
    var configs: List<Config> = listOf(),
    val url: String = "",
    val sizeInBytes: Long = 0L,
    val downloadFileName: String = "_",
    val version: String = "_",
    val isLlm: Boolean = true,
    val runtimeType: RuntimeType = RuntimeType.LITERT_LM,
    val llmSupportImage: Boolean = false,
    val llmSupportAudio: Boolean = false,
    val accelerators: List<Accelerator> = listOf(Accelerator.GPU),
) {
    var normalizedName: String = NORMALIZE_NAME_REGEX.replace(name, "_")
    var instance: Any? = null
    var initializing: Boolean = false
    var configValues: Map<String, Any> = mapOf()
    var totalBytes: Long = sizeInBytes

    init {
        val initialConfigs: MutableMap<String, Any> = mutableMapOf()
        for (config in this.configs) {
            initialConfigs[config.key.label] = config.defaultValue
        }
        this.configValues = initialConfigs
    }

    fun getPath(context: Context, fileName: String = downloadFileName): String {
        val baseDir = File(context.getExternalFilesDir("llm_models"), "$normalizedName/$version")
        if (!baseDir.exists()) baseDir.mkdirs()
        return File(baseDir, fileName).absolutePath
    }

    fun getIntConfigValue(key: ConfigKey, defaultValue: Int = 0): Int {
        return convertValueToTargetType(
            configValues.getOrDefault(key.label, defaultValue),
            ValueType.INT
        ) as Int
    }

    fun getFloatConfigValue(key: ConfigKey, defaultValue: Float = 0.0f): Float {
        return convertValueToTargetType(
            configValues.getOrDefault(key.label, defaultValue),
            ValueType.FLOAT
        ) as Float
    }
}

enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
}

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0,
    val receivedBytes: Long = 0,
    val errorMessage: String = "",
)
