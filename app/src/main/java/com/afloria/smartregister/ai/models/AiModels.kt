package com.afloria.smartregister.ai.models

import com.afloria.smartregister.ai.data.Config
import com.afloria.smartregister.ai.data.ConfigKeys
import com.afloria.smartregister.ai.data.Model
import com.afloria.smartregister.ai.data.NumberSliderConfig
import com.afloria.smartregister.ai.data.ValueType

object AiModels {
    private fun createDefaultConfigs(
        temp: Float = 0.3f,
        topK: Int = 40,
        topP: Float = 0.9f,
        maxTokens: Int = 1024
    ): List<Config> = listOf(
        NumberSliderConfig(ConfigKeys.TEMPERATURE, 0.0f, 2.0f, temp, ValueType.FLOAT),
        NumberSliderConfig(ConfigKeys.TOPK, 1.0f, 100.0f, topK.toFloat(), ValueType.INT),
        NumberSliderConfig(ConfigKeys.TOPP, 0.0f, 1.0f, topP, ValueType.FLOAT),
        NumberSliderConfig(ConfigKeys.MAX_TOKENS, 256.0f, 4096.0f, maxTokens.toFloat(), ValueType.INT)
    )

    val GEMMA_3_1B = Model(
        name = "Gemma3-1B-IT q4",
        displayName = "Gemma 3 1B IT",
        info = "Modello compatto ottimizzato per inferenza veloce su dispositivi mobili.",
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        downloadFileName = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        version = "20250514",
        sizeInBytes = 554_661_246L,
        configs = createDefaultConfigs(temp = 1.0f, topK = 64, topP = 0.95f, maxTokens = 1024)
    )

    val GEMMA_3N_2B = Model(
        name = "Gemma-3n-E2B-it-int4",
        displayName = "Gemma 3n E2B IT (int4)",
        info = "Modello preview ad alte prestazioni con supporto per la visione.",
        url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
        downloadFileName = "gemma-3n-E2B-it-int4.task",
        version = "20250520",
        sizeInBytes = 3_136_226_711L,
        llmSupportImage = true,
        configs = createDefaultConfigs(temp = 1.0f, topK = 64, topP = 0.95f, maxTokens = 4096)
    )

    val GEMMA_3N_4B = Model(
        name = "Gemma-3n-E4B-it-int4",
        displayName = "Gemma 3n E4B IT (int4)",
        info = "Modello ad alta fedeltà con visione per ragionamenti complessi.",
        url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
        downloadFileName = "gemma-3n-E4B-it-int4.task",
        version = "20250520",
        sizeInBytes = 4_405_655_031L,
        llmSupportImage = true,
        configs = createDefaultConfigs(temp = 1.0f, topK = 64, topP = 0.95f, maxTokens = 4096)
    )

    val QWEN_1_5B = Model(
        name = "Qwen2.5-1.5B-Instruct q8",
        displayName = "Qwen 2.5 1.5B",
        info = "Modello bilanciato con ottima capacità di seguire le istruzioni.",
        url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        downloadFileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        version = "20250514",
        sizeInBytes = 1_625_493_432L,
        configs = createDefaultConfigs(temp = 1.0f, topK = 40, topP = 0.95f, maxTokens = 1024)
    )

    val ALL_MODELS = listOf(GEMMA_3N_4B, GEMMA_3_1B, GEMMA_3N_2B, QWEN_1_5B)
}
