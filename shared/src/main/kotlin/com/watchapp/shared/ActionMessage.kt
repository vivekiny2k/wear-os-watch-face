package com.watchapp.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ActionMessage(
    val type: String,
    val action: String,
    val extra: Map<String, String> = emptyMap(),
)

@Serializable
data class ActionResult(
    val ok: Boolean,
    val action: String,
    val message: String = "",
)

object ActionJson {
    val json = Json { ignoreUnknownKeys = true }

    fun encode(message: ActionMessage): ByteArray =
        json.encodeToString(ActionMessage.serializer(), message).toByteArray()

    fun decode(bytes: ByteArray): ActionMessage =
        json.decodeFromString(ActionMessage.serializer(), bytes.decodeToString())

    fun encodeResult(result: ActionResult): ByteArray =
        json.encodeToString(ActionResult.serializer(), result).toByteArray()
}
