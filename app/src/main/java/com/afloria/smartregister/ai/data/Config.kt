package com.afloria.smartregister.ai.data

import androidx.annotation.StringRes
import kotlin.math.abs

enum class ConfigEditorType {
    LABEL,
    NUMBER_SLIDER,
    BOOLEAN_SWITCH,
    SEGMENTED_BUTTON,
    BOTTOMSHEET_SELECTOR,
}

enum class ValueType {
    INT,
    FLOAT,
    DOUBLE,
    STRING,
    BOOLEAN,
}

data class ConfigKey(val id: String, val label: String)

object ConfigKeys {
    val MAX_TOKENS = ConfigKey("max_tokens", "Max tokens")
    val TOPK = ConfigKey("topk", "TopK")
    val TOPP = ConfigKey("topp", "TopP")
    val TEMPERATURE = ConfigKey("temperature", "Temperature")
    val ACCELERATOR = ConfigKey("accelerator", "Accelerator")
    val ENABLE_THINKING = ConfigKey("enable_thinking", "Enable thinking")
}

open class Config(
    val type: ConfigEditorType,
    open val key: ConfigKey,
    open val defaultValue: Any,
    open val valueType: ValueType,
    open val needReinitialization: Boolean = true,
)

class LabelConfig(override val key: ConfigKey, override val defaultValue: String = "") :
    Config(
        type = ConfigEditorType.LABEL,
        key = key,
        defaultValue = defaultValue,
        valueType = ValueType.STRING,
    )

class NumberSliderConfig(
    override val key: ConfigKey,
    val sliderMin: Float,
    val sliderMax: Float,
    override val defaultValue: Float,
    override val valueType: ValueType,
    override val needReinitialization: Boolean = true,
) :
    Config(
        type = ConfigEditorType.NUMBER_SLIDER,
        key = key,
        defaultValue = defaultValue,
        valueType = valueType,
    )

class BooleanSwitchConfig(
    override val key: ConfigKey,
    override val defaultValue: Boolean,
    override val needReinitialization: Boolean = true,
) :
    Config(
        type = ConfigEditorType.BOOLEAN_SWITCH,
        key = key,
        defaultValue = defaultValue,
        valueType = ValueType.BOOLEAN,
    )

class SegmentedButtonConfig(
    override val key: ConfigKey,
    override val defaultValue: String,
    val options: List<String>,
    val allowMultiple: Boolean = false,
) :
    Config(
        type = ConfigEditorType.SEGMENTED_BUTTON,
        key = key,
        defaultValue = defaultValue,
        valueType = ValueType.STRING,
    )

fun convertValueToTargetType(value: Any, valueType: ValueType): Any {
    return when (valueType) {
        ValueType.INT ->
            when (value) {
                is Int -> value
                is Float -> value.toInt()
                is Double -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                is Boolean -> if (value) 1 else 0
                else -> 0
            }

        ValueType.FLOAT ->
            when (value) {
                is Int -> value.toFloat()
                is Float -> value
                is Double -> value.toFloat()
                is String -> value.toFloatOrNull() ?: 0f
                is Boolean -> if (value) 1f else 0f
                else -> 0f
            }

        ValueType.DOUBLE ->
            when (value) {
                is Int -> value.toDouble()
                is Float -> value.toDouble()
                is Double -> value
                is String -> value.toDoubleOrNull() ?: 0.0
                is Boolean -> if (value) 1.0 else 0.0
                else -> 0.0
            }

        ValueType.BOOLEAN ->
            when (value) {
                is Int -> value != 0
                is Boolean -> value
                is Float -> abs(value) > 1e-6
                is Double -> abs(value) > 1e-6
                is String -> value.isNotEmpty()
                else -> false
            }

        ValueType.STRING -> value.toString()
    }
}
