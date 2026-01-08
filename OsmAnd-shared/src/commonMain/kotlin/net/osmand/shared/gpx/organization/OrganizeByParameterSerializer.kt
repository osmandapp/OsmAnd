package net.osmand.shared.gpx.organization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.shared.gpx.organization.strategy.OrganizeByRangeStrategy

object OrganizeByParameterSerializer : KSerializer<OrganizeByParameter> {
	override val descriptor: SerialDescriptor =
		buildClassSerialDescriptor("OrganizeByParameter") {
			element<String>("type")
			element<Double>("stepSize", isOptional = true)
		}

	override fun serialize(encoder: Encoder, value: OrganizeByParameter) {
		val composite = encoder.beginStructure(descriptor)
		composite.encodeStringElement(descriptor, 0, value.type.name)
		if (value is OrganizeByRangeParameter) {
			composite.encodeDoubleElement(descriptor, 1, value.stepSize)
		}
		composite.endStructure(descriptor)
	}

	override fun deserialize(decoder: Decoder): OrganizeByParameter {
		val input = decoder as? JsonDecoder ?: throw SerializationException("Only JSON supported")
		val jsonObject = input.decodeJsonElement().jsonObject

		val typeStr = jsonObject["type"]?.jsonPrimitive?.content
			?: throw SerializationException("Missing type")
		val type = OrganizeByType.valueOf(typeStr)
		
		return if (type.strategy is OrganizeByRangeStrategy) {
			val step = jsonObject["stepSize"]?.jsonPrimitive?.double ?: 0.0
			OrganizeByRangeParameter(type, step)
		} else {
			OrganizeByParameter(type)
		}
	}
}