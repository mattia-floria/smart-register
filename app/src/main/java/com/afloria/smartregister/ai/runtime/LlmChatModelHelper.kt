package com.afloria.smartregister.ai.runtime

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.afloria.smartregister.ai.data.Accelerator
import com.afloria.smartregister.ai.data.ConfigKeys
import com.afloria.smartregister.ai.data.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.ToolSet
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope

private const val TAG = "LlmChatModelHelper"

data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

object LlmChatModelHelper : LlmModelHelper {
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    @OptIn(ExperimentalApi::class)
    override fun initialize(
        context: Context,
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        onDone: (String) -> Unit,
        systemInstruction: Contents?,
        tools: List<ToolProvider>,
        enableConversationConstrainedDecoding: Boolean,
        coroutineScope: CoroutineScope?,
    ) {
        val maxTokens = model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = 2048)
        val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = 1)
        val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = 0.1f)
        val temperature = model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = 0.1f)
        
        val acceleratorName = model.configValues[ConfigKeys.ACCELERATOR.label] as? String
        val preferredBackend = when (acceleratorName) {
            Accelerator.CPU.name -> Backend.CPU()
            Accelerator.GPU.name -> Backend.GPU()
            Accelerator.NPU.name -> Backend.NPU()
            else -> Backend.CPU()
        }

        val modelPath = model.getPath(context = context)
        Log.d(TAG, "Model path: $modelPath")
        val cacheDir = context.getExternalFilesDir("llm_cache")?.absolutePath
        Log.d(TAG, "Cache dir: $cacheDir")

        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = preferredBackend,
            visionBackend = if (supportImage) Backend.GPU() else null,
            audioBackend = if (supportAudio) Backend.CPU() else null,
            maxNumTokens = maxTokens,
            cacheDir = cacheDir,
        )

        try {
            Log.d(TAG, "Initializing engine with backend: ${preferredBackend::class.java.simpleName}")
            val engine = Engine(engineConfig)
            engine.initialize()

            ExperimentalFlags.enableConversationConstrainedDecoding = enableConversationConstrainedDecoding
            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble(),
                    ),
                    systemInstruction = systemInstruction,
                    tools = tools,
                )
            )
            ExperimentalFlags.enableConversationConstrainedDecoding = false
            model.instance = LlmModelInstance(engine = engine, conversation = conversation)
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error during engine initialization", t)
            onDone(t.message ?: "Unknown error")
            return
        }
        onDone("")
    }

    @OptIn(ExperimentalApi::class)
    override fun resetConversation(
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        systemInstruction: Contents?,
        tools: List<ToolProvider>,
        enableConversationConstrainedDecoding: Boolean,
    ) {
        try {
            val instance = model.instance as LlmModelInstance? ?: return
            instance.conversation.close()

            val engine = instance.engine
            val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = 40)
            val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = 0.9f)
            val temperature = model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = 0.2f)

            ExperimentalFlags.enableConversationConstrainedDecoding = enableConversationConstrainedDecoding
            val newConversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble(),
                    ),
                    systemInstruction = systemInstruction,
                    tools = tools,
                )
            )
            ExperimentalFlags.enableConversationConstrainedDecoding = false
            instance.conversation = newConversation
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset conversation", e)
        }
    }

    override fun cleanUp(model: Model, onDone: () -> Unit) {
        val instance = model.instance as? LlmModelInstance
        if (instance != null) {
            try {
                instance.conversation.close()
                instance.engine.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean up", e)
            }
        }
        cleanUpListeners.remove(model.name)?.invoke()
        model.instance = null
        onDone()
    }

    override fun stopResponse(model: Model) {
        (model.instance as? LlmModelInstance)?.conversation?.cancelProcess()
    }

    override fun runInference(
        model: Model,
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        onError: (message: String) -> Unit,
        images: List<Bitmap>,
        audioClips: List<ByteArray>,
        coroutineScope: CoroutineScope?,
        extraContext: Map<String, String>?,
    ) {
        val instance = model.instance as? LlmModelInstance ?: run {
            onError("LlmModelInstance is not initialized.")
            return
        }

        if (!cleanUpListeners.containsKey(model.name)) {
            cleanUpListeners[model.name] = cleanUpListener
        }

        val contents = mutableListOf<Content>()
        images.forEach { contents.add(Content.ImageBytes(it.toPngByteArray())) }
        audioClips.forEach { contents.add(Content.AudioBytes(it)) }
        if (input.trim().isNotEmpty()) {
            contents.add(Content.Text(input))
        }

        instance.conversation.sendMessageAsync(
            Contents.of(contents),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    resultListener(message.toString(), false, message.channels["thought"])
                }

                override fun onDone() {
                    resultListener("", true, null)
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        resultListener("", true, null)
                    } else {
                        onError("Error: ${throwable.message}")
                    }
                }
            },
            extraContext ?: emptyMap(),
        )
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
