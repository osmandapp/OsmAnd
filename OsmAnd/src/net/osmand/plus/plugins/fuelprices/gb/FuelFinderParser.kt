package net.osmand.plus.plugins.fuelprices.gb

import net.osmand.plus.plugins.fuelprices.FuelType
import net.osmand.plus.plugins.fuelprices.ProviderPrices
import net.osmand.util.Algorithms
import java.io.Reader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

object FuelFinderParser {

	private val FUEL_TYPES = mapOf(
		"E10" to FuelType.E10,
		"E5" to FuelType.E5,
		"B7S" to FuelType.DIESEL,
		"B7P" to FuelType.PREMIUM_DIESEL,
		"B10" to FuelType.B10,
		"HVO" to FuelType.HVO,
	)

	fun parse(reader: Reader): List<ProviderPrices> {
		val timestampFormat = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US)
		val lines = reader.buffered().lineSequence().iterator()
		if (!lines.hasNext()) {
			return emptyList()
		}
		val columns = split(lines.next()).withIndex().associate { (index, name) -> name to index }
		val stations = mutableListOf<ProviderPrices>()
		for (line in lines) {
			val row = split(line)
			fun get(column: String) = columns[column]?.let { row.getOrNull(it) }

			val prices = mutableMapOf<FuelType, Double>()
			var updatedAt = 0L
			for ((code, type) in FUEL_TYPES) {
				val pence = Algorithms.parseDoubleSilently(get("forecourts.fuel_price.$code"), 0.0)
				if (pence > 0) {
					prices[type] = pence / 100
					updatedAt = maxOf(updatedAt,
						parseTimestamp(timestampFormat,
						get("forecourts.price_submission_timestamp.$code")))
				}
			}
			if (prices.isNotEmpty()) {
				stations += ProviderPrices(
					get("forecourts.node_id").orEmpty(),
					Algorithms.parseDoubleSilently(get("forecourts.location.latitude"), 0.0),
					Algorithms.parseDoubleSilently(get("forecourts.location.longitude"), 0.0),
					prices,
					updatedAt,
				)
			}
		}
		return stations
	}

	private fun parseTimestamp(format: SimpleDateFormat, value: String?): Long =
		try {
			value?.let { format.parse(it.substringBefore(" ("))?.time } ?: 0
		} catch (e: ParseException) {
			0
		}

	private fun split(line: String): List<String> {
		val fields = mutableListOf<String>()
		val field = StringBuilder()
		var quoted = false
		for (c in line) {
			when {
				c == '"' -> quoted = !quoted
				c == ',' && !quoted -> {
					fields += field.toString()
					field.clear()
				}
				else -> field.append(c)
			}
		}
		fields += field.toString()
		return fields
	}
}
