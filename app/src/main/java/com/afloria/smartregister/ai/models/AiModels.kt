package com.afloria.smartregister.ai.models

import com.afloria.smartregister.ai.data.Accelerator
import com.afloria.smartregister.ai.data.Config
import com.afloria.smartregister.ai.data.ConfigKeys
import com.afloria.smartregister.ai.data.Model
import com.afloria.smartregister.ai.data.NumberSliderConfig
import com.afloria.smartregister.ai.data.SegmentedButtonConfig
import com.afloria.smartregister.ai.data.ValueType

object AiModels {
    private fun createDefaultConfigs(
        temp: Float = 0.1f,
        topK: Int = 1,
        topP: Float = 0.1f,
        maxTokens: Int = 2048,
        accelerator: Accelerator = Accelerator.GPU
    ): List<Config> = listOf(
        NumberSliderConfig(ConfigKeys.TEMPERATURE, 0.0f, 2.0f, temp, ValueType.FLOAT),
        NumberSliderConfig(ConfigKeys.TOPK, 1.0f, 100.0f, topK.toFloat(), ValueType.INT),
        NumberSliderConfig(ConfigKeys.TOPP, 0.0f, 1.0f, topP, ValueType.FLOAT),
        NumberSliderConfig(ConfigKeys.MAX_TOKENS, 256.0f, 4096.0f, maxTokens.toFloat(), ValueType.INT),
        SegmentedButtonConfig(ConfigKeys.ACCELERATOR, accelerator.name, Accelerator.values().map { it.name })
    )

    val GEMMA_3_1B_IT = Model(
        name = "gemma-3-1b-it",
        displayName = "Gemma 3 1B IT",
        info = "Modello avanzato di ultima generazione (Gemma 3), bilanciato tra prestazioni e velocità.",
        // Using the litert-community repository which is compatible with the .task format
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        downloadFileName = "gemma-3-1b-it.task",
        version = "20250601",
        sizeInBytes = 554661246L,
        llmSupportImage = false,
        configs = createDefaultConfigs(temp = 0.45f, topK = 40, topP = 0.9f, maxTokens = 2048, accelerator = Accelerator.GPU)
    )

    val ALL_MODELS = listOf(GEMMA_3_1B_IT)
}
