package net.osmand.shared.util

import kotlin.math.pow
import kotlin.math.round

class StringBundle {

	private val map: MutableMap<String, Item<*>> = LinkedHashMap()

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
			return value?.toBooleanStrictOrNull()
		}

		fun asIntArray(defaultValue: IntArray?): IntArray? {
			return try {
				value?.split(",")!!.map { it.toInt() }.toIntArray()
			} catch (e: NumberFormatException) {
				defaultValue
			}
		}

		fun asIntIntArray(defaultValue: Array<IntArray>?): Array<IntArray>? {
			return try {
				value?.split(";")!!.map {
					it.split(",").map { num -> num.toInt() }.toIntArray()
				}.toTypedArray()
			} catch (e: NumberFormatException) {
				defaultValue
			}
		}

		companion object {
			private fun formatValue(value: Float, maxDigits: Int): String {
				val factor = 10.0.pow(maxDigits).toFloat()
				return (round(value * factor) / factor).toString()
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

	fun putArray(key: String, array: Array<IntArray>?) {
		array?.let {
			map[key] = StringItem(key, it.joinToString(";") { it.joinToString(",") })
		}
	}

	fun getIntIntArray(key: String, defaultValue: Array<IntArray>?): Array<IntArray>? {
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

//	fun <T> putMap(key: String, map: Map<Int, T>) {
//		val bundle = StringBundle()
//		map.forEach { (k, v) -> bundle.putString(k.toString(), v.toString()) }
//		this.map[key] = StringBundleItem(key, bundle)
//	}

	fun <K, V> putMap(key: String, map: Map<K, V>) {
		val bundle = StringBundle()
		map.forEach { (k, v) -> bundle.putString(k.toString(), v.toString()) }
		this.map[key] = StringBundleItem(key, bundle)
	}
}
