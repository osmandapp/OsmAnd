package net.osmand.shared.gpx.filters

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

object RangeTrackFilterSerializer : KSerializer<RangeTrackFilter<*>> {

	@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
	override val descriptor: SerialDescriptor =
		buildClassSerialDescriptor(RangeTrackFilter::class.simpleName ?: "RangeTrackFilter") {
			element("filterType", TrackFilterType.serializer().descriptor)
			element("minValue", buildSerialDescriptor("minValue", SerialKind.CONTEXTUAL))
			element("maxValue", buildSerialDescriptor("maxValue", SerialKind.CONTEXTUAL))
			element("valueFrom", buildSerialDescriptor("minValue", SerialKind.CONTEXTUAL))
			element("valueTo", buildSerialDescriptor("maxValue", SerialKind.CONTEXTUAL))
		}

	override fun serialize(encoder: Encoder, value: RangeTrackFilter<*>) {
		val compositeOutput = encoder.beginStructure(descriptor)
		val serializer = serializerForClass(value.minValue::class)
		compositeOutput.encodeSerializableElement(
			descriptor,
			0,
			TrackFilterType.serializer(),
			value.trackFilterType)
		compositeOutput.encodeSerializableElement(descriptor, 1, serializer, value.minValue)
		compositeOutput.encodeSerializableElement(descriptor, 2, serializer, value.maxValue)
		compositeOutput.encodeSerializableElement(descriptor, 3, serializer, value.valueFrom)
		compositeOutput.encodeSerializableElement(descriptor, 4, serializer, value.valueTo)
		compositeOutput.endStructure(descriptor)
	}

	override fun deserialize(decoder: Decoder): RangeTrackFilter<*> {
		val compositeInput = decoder.beginStructure(descriptor)
		var typeName: String? = null
		var minValue: Comparable<Any>? = null
		var maxValue: Comparable<Any>? = null
		var valueFrom: Comparable<Any>? = null
		var valueTo: Comparable<Any>? = null
		var trackFilterType = compositeInput.decodeSerializableElement(
			descriptor,
			0,
			TrackFilterType.serializer())

		val parameterSerializer = when (trackFilterType.property?.typeClass) {
			Int::class -> Int.serializer()
			Long::class -> Long.serializer()
			Double::class -> Double.serializer()
			Float::class -> Float.serializer()
			else -> throw IllegalArgumentException("Unsupported data type")
		}

		minValue = compositeInput.decodeSerializableElement(
			descriptor,
			1,
			parameterSerializer as KSerializer<Comparable<Any>>)
		maxValue = compositeInput.decodeSerializableElement(
			descriptor,
			2,
			parameterSerializer as KSerializer<Comparable<Any>>)
		valueFrom = compositeInput.decodeSerializableElement(
			descriptor,
			3,
			parameterSerializer as KSerializer<Comparable<Any>>)
		valueTo = compositeInput.decodeSerializableElement(
			descriptor,
			4,
			parameterSerializer as KSerializer<Comparable<Any>>)

		compositeInput.endStructure(descriptor)
		TrackFiltersHelper.createFilter(trackFilterType, null)
		val filter = RangeTrackFilter(
			minValue = minValue,
			maxValue = maxValue,
			trackFilterType = trackFilterType,
			null
		)
		filter.valueTo = valueTo
		filter.valueFrom = valueFrom
		return filter
	}

	private fun serializerForClass(kClass: KClass<*>): KSerializer<Any> {
		return when (kClass) {
			Int::class -> Int.serializer()
			Double::class -> Double.serializer()
			String::class -> String.serializer()
			Long::class -> Long.serializer()
			Float::class -> Float.serializer()
			else -> throw SerializationException("No serializer for class $kClass")
		} as KSerializer<Any>
	}
}