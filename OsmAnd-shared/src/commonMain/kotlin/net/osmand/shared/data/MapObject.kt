package net.osmand.shared.data

import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KTransliterationHelper
import net.osmand.shared.util.primaryCollator
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import okio.Buffer
import okio.GzipSource
import okio.buffer

/**
 * Anything an obf file names and puts on the map: its id, where it is, and what it is called in
 * every language the file carries.
 *
 * A copy of `MapObject` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS. `toJSON` and `parseJSON` are left out: the only things that use them are the address
 * classes, which are not copied yet, and the search history, which stays on the platform. They
 * would come back with kotlinx.serialization if the search port needs them.
 */
abstract class MapObject : Comparable<MapObject> {

	@JvmField
	protected var name: String? = null
	@JvmField
	protected var enName: String? = null

	/**
	 * Looks like: {dz=མོསི་ཀོ...} and does not contain values of OSM tags "name" and "name:en",
	 * see [name] and [enName] respectively.
	 */
	@JvmField
	protected var names: MutableMap<String, String>? = null
	@JvmField
	protected var location: KLatLon? = null
	@JvmField
	protected var fileOffset: Long = 0
	@JvmField
	protected var id: Long? = null
	private var referenceFile: Any? = null

	fun setId(id: Long?) {
		this.id = id
	}

	open fun getId(): Long? = id

	open fun getName(): String {
		val name = this.name
		if (name != null) {
			return unzipContent(name)!!
		}
		return ""
	}

	fun setName(name: String?) {
		this.name = name
	}

	fun setName(lang: String?, name: String?) {
		if (KAlgorithms.isEmpty(lang)) {
			setName(name)
		} else if (lang == "en") {
			setEnName(name)
		} else {
			var names = this.names
			if (names == null) {
				names = LinkedHashMap()
				this.names = names
			}
			if (name != null && lang != null) {
				names[lang] = unzipContent(name)!!
			}
		}
	}

	fun setNames(name: Map<String, String>?) {
		if (name != null) {
			var names = this.names
			if (names == null) {
				names = LinkedHashMap()
				this.names = names
			}
			names.putAll(name)
		}
	}

	fun getNamesMap(includeEn: Boolean): Map<String, String> {
		val names = this.names
		if ((!includeEn || KAlgorithms.isEmpty(enName)) && names == null) {
			return emptyMap()
		}
		val mp = LinkedHashMap<String, String>()
		if (names != null) {
			for (e in names.entries) {
				mp[e.key] = unzipContent(e.value)!!
			}
		}
		if (includeEn && !KAlgorithms.isEmpty(enName)) {
			mp["en"] = unzipContent(enName)!!
		}
		return mp
	}

	fun getOtherNames(): List<String> = getOtherNames(false)

	fun getOtherNames(transliterate: Boolean): List<String> = getOtherNames(transliterate, null)

	fun getOtherNames(transliterate: Boolean, localeName: String?): List<String> {
		val l = ArrayList<String>()
		val enName = getEnName(transliterate)
		if (!KAlgorithms.isEmpty(enName)) {
			if (localeName == null || localeName != enName) {
				l.add(enName)
			}
		}
		val names = this.names
		if (names != null) {
			for (key in names.keys) {
				// skip name:place, name:admin_level... (for search and indexing!)
				if (key == NAME_ADMIN_LEVEL_ATTR || key == NAME_PLACE_ATTR ||
					key.contains(NAME_ETYMOLOGY_ATTR) || key == NAME_WIKIDATA_ATTR
				) {
					continue
				}
				val name = names[key] ?: continue
				if (localeName != null && localeName == name) {
					continue
				}
				l.add(name)
			}
		}
		val name = this.name
		if (!KAlgorithms.isEmpty(name) && localeName != null && localeName != name) {
			l.add(name!!)
		}
		return l
	}

	fun copyNames(
		otherName: String?, otherEnName: String?, otherNames: Map<String, String>?, overwrite: Boolean
	) {
		if (!KAlgorithms.isEmpty(otherName) && (overwrite || KAlgorithms.isEmpty(name))) {
			name = otherName
		}
		if (!KAlgorithms.isEmpty(otherEnName) && (overwrite || KAlgorithms.isEmpty(enName))) {
			enName = otherEnName
		}
		if (!KAlgorithms.isEmpty(otherNames)) {
			if (otherNames!!.containsKey("name:en")) {
				enName = otherNames["name:en"]
			} else if (otherNames.containsKey("en")) {
				enName = otherNames["en"]
			}

			for (e in otherNames.entries) {
				var key = e.key
				if (key.startsWith("name:")) {
					key = key.substring("name:".length)
				}
				var names = this.names
				if (names == null) {
					names = LinkedHashMap()
					this.names = names
				}
				if (overwrite || KAlgorithms.isEmpty(names[key])) {
					names[key] = e.value
				}
			}
		}
	}

	fun copyNames(otherName: String?, otherEnName: String?, otherNames: Map<String, String>?) {
		copyNames(otherName, otherEnName, otherNames, false)
	}

	fun copyNames(s: MapObject, copyName: Boolean, copyEnName: Boolean, overwrite: Boolean) {
		copyNames(if (copyName) s.name else null, if (copyEnName) s.enName else null, s.names, overwrite)
	}

	fun copyNames(s: MapObject) {
		copyNames(s, true, true, false)
	}

	fun getName(lang: String?): String = getName(lang, false)

	fun getName(lang: String?, transliterate: Boolean): String {
		if (lang != null && lang.isNotEmpty()) {
			if (lang == "en") {
				// for some objects like wikipedia, english name is stored 'name' tag
				val enName = getEnName(transliterate)
				return if (!KAlgorithms.isEmpty(enName)) enName else getName()
			} else {
				// get name
				val names = this.names
				if (names != null) {
					val nm = names[lang]
					if (!KAlgorithms.isEmpty(nm)) {
						return unzipContent(nm)!!
					}
					if (transliterate) {
						return KTransliterationHelper.transliterate(getName())
					}
				}
			}
		}
		return getName()
	}

	fun getEnName(transliterate: Boolean): String {
		if (!KAlgorithms.isEmpty(enName)) {
			return unzipContent(this.enName)!!
		} else if (!KAlgorithms.isEmpty(getName()) && transliterate) {
			return KTransliterationHelper.transliterate(getName())
		}
		return ""
	}

	fun setEnName(enName: String?) {
		this.enName = enName
	}

	fun getLocation(): KLatLon? = location

	fun setLocation(latitude: Double, longitude: Double) {
		location = KLatLon(latitude, longitude)
	}

	fun setLocation(loc: KLatLon?) {
		location = loc
	}

	override fun compareTo(other: MapObject): Int =
		primaryCollator().compare(getName(), other.getName())

	fun getFileOffset(): Long = fileOffset

	fun setFileOffset(fileOffset: Long) {
		this.fileOffset = fileOffset
	}

	open fun toStringEn(): String = simpleName() + ":" + getEnName(true)

	private fun simpleName(): String = this::class.simpleName ?: ""

	override fun toString(): String {
		// no ternary here: mixing Long and long operands unboxes a null id and throws NPE
		var osmId = id
		val id = this.id
		if (id != null && id >= 0) {
			osmId = ObfConstants.getOsmIdFromMapObjectId(id)
		}
		return simpleName() + " " + name + "(" + osmId + ")"
	}

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		result = prime * result + (id?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null) {
			return false
		}
		if (other !is MapObject || this::class != other::class) {
			return false
		}
		if (id == null) {
			if (other.id != null) {
				return false
			}
		} else if (id != other.id) {
			return false
		}
		return true
	}

	open fun compareObject(thatObj: MapObject?): Boolean {
		if (this === thatObj) {
			return true
		}
		if (thatObj == null || this.id == null || thatObj.id == null) {
			return false
		}
		return this.id == thatObj.id &&
				getLocation() == thatObj.getLocation() &&
				getName() == thatObj.getName() &&
				getNamesMap(true) == thatObj.getNamesMap(true)
	}

	/** Orders objects by name, in one language and with a collator that ignores case and accents. */
	class MapObjectComparator(private val l: String? = null, private val transliterate: Boolean = false) :
		Comparator<MapObject?> {

		private val collator: KCollator = primaryCollator()

		override fun compare(a: MapObject?, b: MapObject?): Int {
			if ((a == null) != (b == null)) {
				return if (a == null) -1 else 1
			} else if (a === b) {
				return 0
			} else {
				return collator.compare(a!!.getName(l, transliterate), b!!.getName(l, transliterate))
			}
		}

		fun areEqual(a: MapObject?, b: MapObject?): Boolean {
			if ((a == null) != (b == null)) {
				return false
			} else if (a === b) {
				return true
			} else {
				return collator.equals(a!!.getName(l, transliterate), b!!.getName(l, transliterate))
			}
		}
	}

	fun setReferenceFile(referenceFile: Any?) {
		this.referenceFile = referenceFile
	}

	fun getReferenceFile(): Any? = referenceFile

	protected fun getMinBbox(ll: KLatLon): KQuadRect {
		val d = 0.002
		return KQuadRect(
			ll.longitude - d, ll.latitude + d, ll.longitude + d, ll.latitude - d
		)
	}

	open fun getBbox31(): IntArray? = null

	open fun getWikidata(): String? {
		val wikidata = names?.get(NAME_WIKIDATA_ATTR)
		return if (KAlgorithms.isEmpty(wikidata)) null else unzipContent(wikidata)
	}

	companion object {
		@JvmField
		val BY_NAME_COMPARATOR = MapObjectComparator()

		const val AMENITY_ID_RIGHT_SHIFT: Int = 1
		const val WAY_MODULO_REMAINDER: Int = 1

		const val NAME_REF_ATTR: String = "ref"
		const val NAME_PLACE_ATTR: String = "place"
		const val NAME_ADMIN_LEVEL_ATTR: String = "admin_level"
		const val NAME_WIKIDATA_ATTR: String = "wikidata"
		const val NAME_ETYMOLOGY_ATTR: String = "etymology"

		/**
		 * A long value the indexer gzipped and re-encoded one byte per char, marked by a " gz "
		 * prefix. Anything else comes back untouched.
		 */
		@JvmStatic
		fun unzipContent(str: String?): String? {
			var result = str
			if (isContentZipped(str)) {
				try {
					val ind = 4
					val bytes = ByteArray(str!!.length - ind)
					for (i in ind until str.length) {
						val ch = str[i]
						bytes[i - ind] = (ch.code - 128 - 32).toByte()
					}
					val source = GzipSource(Buffer().write(bytes)).buffer()
					val bld = StringBuilder()
					while (true) {
						val line = source.readUtf8Line() ?: break
						bld.append(line)
						bld.append("\n") // could be space for name
					}
					source.close()
					result = bld.toString().trim()
				} catch (e: Exception) {
					// a value that says it is zipped but is not comes back as it was
				}
			}
			return result
		}

		@JvmStatic
		fun isContentZipped(str: String?): Boolean = str != null && str.startsWith(" gz ")

		@JvmStatic
		fun isNameLangTag(tag: String): Boolean {
			if (tag.startsWith("name:")) {
				// languages code <= 3
				if (tag.length <= "name:".length + 3) {
					return true
				}
				val l = tag.indexOf("-")
				if (l <= "name:".length + 3) {
					return true
				}
			}
			return false
		}
	}
}
