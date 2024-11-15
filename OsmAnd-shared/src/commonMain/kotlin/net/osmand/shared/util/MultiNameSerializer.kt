package net.osmand.shared.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SerialNames(vararg val names: String)

object MultiNameSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MultiNameSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): String {
        require(decoder is JsonDecoder) { "This class can be loaded only by JSON" }
        val jsonElement = decoder.decodeJsonElement()

        val jsonObject = jsonElement as? JsonObject
            ?: throw SerializationException("Expected JsonObject")

        val annotations = decoder.serializersModule.getContextualDescriptor(descriptor)?.getElementAnnotations(0)
            ?: throw SerializationException("Descriptor or annotations not found")

        val possibleNames = annotations
            .filterIsInstance<SerialNames>()
            .flatMap { it.names.toList() }

        for (name in possibleNames) {
            jsonObject[name]?.let {
                return it.toString().trim('"')
            }
        }

        throw SerializationException("None of the expected names were found in the JSON")
    }
}