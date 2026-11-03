package net.osmand.shared.util

import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.math.abs
import kotlin.math.floor

open class StringBundle {

	private val map: MutableMap<String, Item<*>>

	constructor() {
		map = LinkedHashMap()
	}

	/** Shares the contents of [bundle], so a subclass can be built around an existing bundle. */
	protected constructor(bundle: StringBundle) {
		this.map = bundle.map
	}

	/** An empty bundle of the same kind, so a reader can build nested bundles of a subclass. */
	open fun newInstance(): StringBundle = StringBundle()

	enum class ItemType {
		STRING,
		LIST,
		MAP
	}

	open class Item<T>(val name: String, val type: ItemType, val value: T?)

	class StringItem(name: String, value: String?) : Item<String>(name, ItemType.STRING, value) {

		constructor(name: String, value: Int) : this(name, value.toString())
		constructor(name: String, value: Long) : this(name, value.toString())
		constructor(name: String, value: Float) : this(name, value.toString())
		constructor(name: String, value: Float, maxDigits: Int) : this(name, formatValue(value, maxDigits))
		constructor(name: String, value: Boolean) : this(name, value.toString())

		fun asInt(defaultValue: Int?): Int? {
			return value?.toIntOrNull() ?: defaultValue
		}

		fun asLong(defaultValue: Long?): Long? {
			return value?.toLongOrNull() ?: defaultValue
		}

		fun asFloat(defaultValue: Float?): Float? {
			return value?.toFloatOrNull() ?: defaultValue
		}

		fun asBoolean(defaultValue: Boolean?): Boolean? {
			// java read this with Boolean.parseBoolean, which answers false for anything but "true"
			return if (value == null) defaultValue else value.equals("true", ignoreCase = true)
		}

		fun asIntArray(defaultValue: IntArray?): IntArray? {
			return try {
				val text = value ?: return defaultValue
				splitDroppingTrailingEmpty(text, ",").map { it.toInt() }.toIntArray()
			} catch (e: NumberFormatException) {
				defaultValue
			}
		}

		fun asIntIntArray(defaultValue: Array<IntArray?>?): Array<IntArray?>? {
			return try {
				val text = value ?: return defaultValue
				val items = splitDroppingTrailingEmpty(text, ";")
				Array(items.size) { i ->
					val item = items[i]
					// an empty item stands for a row that had no values, and reads back as null
					if (item.isEmpty()) {
						null
					} else {
						item.split(",").map { num -> num.toInt() }.toIntArray()
					}
				}
			} catch (e: NumberFormatException) {
				defaultValue
			}
		}

		companion object {

			/**
			 * Splits the way `String.split(regex)` does on the jvm, which drops the empty parts at
			 * the end. The text these arrays are read from was written by that, so the two agree.
			 */
			private fun splitDroppingTrailingEmpty(text: String, separator: String): List<String> {
				if (!text.contains(separator)) {
					// java answers with the whole input when the separator does not occur in it
					return listOf(text)
				}
				val parts = text.split(separator)
				var end = parts.size
				while (end > 0 && parts[end - 1].isEmpty()) {
					end--
				}
				return parts.subList(0, end)
			}

			/**
			 * The same text `DecimalFormat("#.##")` and its wider siblings produced on the jvm:
			 * at most [maxDigits] fraction digits, half even rounding of the exact value, trailing
			 * zeros and a bare decimal point dropped.
			 *
			 * Java only had formatters for two to six digits and fell back to the plain text of the
			 * number otherwise; so does this, and so does a value too large to scale into a Long.
			 */
			private fun formatValue(value: Float, maxDigits: Int): String {
				if (maxDigits < 2 || maxDigits > 6) {
					return value.toString()
				}
				if (value.isNaN()) {
					return "NaN"
				}
				if (value.isInfinite()) {
					return if (value > 0) "\u221E" else "-\u221E"
				}
				val d = value.toDouble()
				if (abs(d) >= 1e12) {
					return value.toString()
				}
				var factor = 1L
				repeat(maxDigits) { factor *= 10L }
				val scaled = abs(d) * factor
				var rounded = floor(scaled)
				val rest = scaled - rounded
				if (rest > 0.5 || (rest == 0.5 && rounded.toLong() % 2L != 0L)) {
					rounded += 1.0
				}
				val units = rounded.toLong()
				val builder = StringBuilder()
				// the sign survives a value that rounds to zero, and so does the sign of -0.0
				if (value.toRawBits() < 0) {
					builder.append('-')
				}
				builder.append(units / factor)
				var fraction = units % factor
				if (fraction != 0L) {
					var digits = maxDigits
					while (fraction % 10L == 0L) {
						fraction /= 10L
						digits--
					}
					val text = fraction.toString()
					builder.append('.')
					repeat(digits - text.length) { builder.append('0') }
					builder.append(text)
				}
				return builder.toString()
			}
		}
	}

	class StringListItem(name: String, list: List<Item<*>>) : Item<List<Item<*>>>(name,
		ItemType.LIST, list)

	open class StringMapItem(name: String, map: Map<String, Item<*>>) : Item<Map<String, Item<*>>>(name,
		ItemType.MAP, map)

	class StringBundleItem(name: String, bundle: StringBundle) : StringMapItem(name, bundle.map)

	fun getMap(): Map<String, Item<*>> {
		return map.toMap()
	}

	fun isEmpty(): Boolean {
		return map.isEmpty()
	}

	fun getItem(key: String): Item<*>? {
		return map[key]
	}

	fun putInt(key: String, value: Int) {
		map[key] = StringItem(key, value)
	}

	fun getInt(key: String, defaultValue: Int): Int {
		return (map[key] as? StringItem)?.asInt(defaultValue) ?: defaultValue
	}

	fun putLong(key: String, value: Long) {
		map[key] = StringItem(key, value)
	}

	fun getLong(key: String, defaultValue: Long?): Long? {
		return (map[key] as? StringItem)?.asLong(defaultValue) ?: defaultValue
	}

	fun putFloat(key: String, value: Float) {
		map[key] = StringItem(key, value)
	}

	fun putFloat(key: String, value: Float, maxDigits: Int) {
		map[key] = StringItem(key, value, maxDigits)
	}

	fun getFloat(key: String, defaultValue: Float?): Float? {
		return (map[key] as? StringItem)?.asFloat(defaultValue) ?: defaultValue
	}

	fun putBoolean(key: String, value: Boolean) {
		map[key] = StringItem(key, value)
	}

	fun getBoolean(key: String, defaultValue: Boolean?): Boolean? {
		return (map[key] as? StringItem)?.asBoolean(defaultValue) ?: defaultValue
	}

	fun putString(key: String, value: String?) {
		map[key] = StringItem(key, value)
	}

	fun getString(key: String, defaultValue: String?): String? {
		return (map[key] as? StringItem)?.value ?: defaultValue
	}

	fun putBundleList(key: String, itemName: String, list: List<StringBundle>?) {
		list?.let {
			val itemList = it.map { bundle -> StringBundleItem(itemName, bundle) }
			map[key] = StringListItem(key, itemList)
		}
	}

	fun putBundle(key: String, bundle: StringBundle) {
		map[key] = StringBundleItem(key, bundle)
	}

	fun putArray(key: String, array: IntArray?) {
		array?.let {
			map[key] = StringItem(key, it.joinToString(","))
		}
	}

	fun getIntArray(key: String, defaultValue: IntArray?): IntArray? {
		return (map[key] as? StringItem)?.asIntArray(defaultValue) ?: defaultValue
	}

	/** Rows may be null or empty, and are written as an empty item, the way the java bundle did. */
	fun putArray(key: String, array: Array<IntArray?>?) {
		array?.let { rows ->
			map[key] = StringItem(key, rows.joinToString(";") { row ->
				if (row == null) "" else row.joinToString(",")
			})
		}
	}

	fun getIntIntArray(key: String, defaultValue: Array<IntArray?>?): Array<IntArray?>? {
		return (map[key] as? StringItem)?.asIntIntArray(defaultValue) ?: defaultValue
	}

	fun putArray(key: String, array: LongArray?) {
		array?.let {
			map[key] = StringItem(key, it.joinToString(","))
		}
	}

	fun putArray(key: String, array: FloatArray?) {
		array?.let {
			map[key] = StringItem(key, it.joinToString(","))
		}
	}

	fun <T : Any> putMap(key: String, map: KTIntObjectMap<T>) {
		val bundle = StringBundle()
		map.forEach { k, v -> bundle.putString(k.toString(), v.toString()) }
		this.map[key] = StringBundleItem(key, bundle)
	}

	fun <K, V> putMap(key: String, map: Map<K, V>) {
		val bundle = StringBundle()
		map.forEach { (k, v) -> bundle.putString(k.toString(), v.toString()) }
		this.map[key] = StringBundleItem(key, bundle)
	}
}
