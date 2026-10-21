package net.osmand.shared.osm

import net.osmand.shared.map.WorldRegion
import net.osmand.shared.util.KAlgorithms
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Labels of additional property values written as "Name: value" in phrases.xml ("Fridge: yes",
 * "Smoking: not allowed", "Kayaks: yes"). A translation without the separator is shown as it is.
 *
 * A copy of `PoiAdditionalLabels` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS.
 */
object PoiAdditionalLabels {

	@OptIn(ExperimentalObjCName::class)
	@ObjCName("yes")
	const val YES = "yes"

	// OSM values of a property state ("smoking=no"); a value outside it is an option ("diesel", "gynaecology")
	private val STATE_VALUES = setOf(
		YES, "no", "only", "designated", "limited", "permissive",
		"private", "customers", "destination", "unknown", "outside", "separated", "isolated", "dedicated"
	)

	// states that mean the property is there
	private val ACCEPTED_VALUES = setOf(YES, "only", "designated")

	/**
	 * @return {name, value} or null if the translation is not "Name: value"
	 */
	fun splitNameValue(translation: String?): Array<String>? {
		if (translation == null || translation.isEmpty()) {
			return null
		}
		val index = translation.indexOf(':')
		if (index < 0) {
			return null
		}
		val name = translation.substring(0, index).trim { it <= ' ' }
		val value = translation.substring(index + 1).trim { it <= ' ' }
		// a name has letters: "1:25 fuel" is a value, not "1" + "25 fuel"
		return if (!hasLetter(name) || value.isEmpty()) null else arrayOf(name, value)
	}

	private fun hasLetter(s: String): Boolean {
		for (c in s) {
			if (c.isLetter()) {
				return true
			}
		}
		return false
	}

	/**
	 * Search shows and indexes an additional value unless it is a state that does not mean the property is there:
	 * yes, only, designated and every option ("diesel", "gynaecology", station=subway) are taken; no, limited,
	 * outside... are not.
	 */
	fun isAcceptedValue(osmValue: String?): Boolean =
		osmValue == null || !STATE_VALUES.contains(osmValue) || ACCEPTED_VALUES.contains(osmValue)

	fun isAcceptedValue(poiType: PoiType): Boolean = isAcceptedValue(poiType.getOsmValue())

	/**
	 * Name of an additional value in search, the part before ":" ("Subway station: yes" -> "Subway station").
	 * The older "Yes;Subway station" gives the first synonym, a translation without a name stays as it is.
	 */
	fun getSearchName(translation: String, enTranslation: String?, synonyms: String?, groupName: String?): String {
		val nameValue = splitNameValue(translation)
		if (nameValue != null) {
			return nameValue[0]
		}
		if (YES.equals(enTranslation, ignoreCase = true)) {
			if (synonyms != null && synonyms.isNotEmpty()) {
				val first = WorldRegion.splitLikeJava(synonyms, ";")[0].trim { it <= ' ' }
				if (first.isNotEmpty()) {
					return first
				}
			}
			if (groupName != null && groupName.isNotEmpty()) {
				return groupName
			}
		}
		return translation
	}

	fun getSearchName(poiType: AbstractPoiType): String {
		if (!poiType.isAdditional()) {
			return poiType.getTranslation()
		}
		return getSearchName(
			poiType.getTranslation(), poiType.getEnTranslation(), poiType.getSynonyms(),
			poiType.getPoiAdditionalCategoryTranslation()
		)
	}

	fun getEnSearchName(poiType: AbstractPoiType): String? {
		val enTranslation = poiType.getEnTranslation()
		if (!poiType.isAdditional()) {
			return enTranslation
		}
		val nameValue = splitNameValue(enTranslation)
		if (nameValue != null) {
			return nameValue[0]
		}
		// the older "Yes;Name" has no English name, only the value
		return if (YES.equals(enTranslation, ignoreCase = true)) null else enTranslation
	}

	/**
	 * A group of one "yes" value is a single switch, not a list.
	 */
	fun isSingleYes(osmValues: List<String>): Boolean = osmValues.size == 1 && YES == osmValues[0]

	/**
	 * Label of the switch for a group of one "yes" value: the name, else the group name.
	 */
	fun getSwitchLabel(translation: String, groupName: String?): String {
		val nameValue = splitNameValue(translation)
		if (nameValue != null) {
			return nameValue[0]
		}
		return if (groupName == null || groupName.isEmpty()) translation else groupName
	}

	/**
	 * Labels of the values of one group, in the same order:
	 * - all values have the same name ("Smoking: allowed", "Smoking: not allowed"): the value, "Allowed";
	 * - otherwise ("Kayaks: yes", "Canoe: yes"): the name for a "yes" value whose name is not repeated in the group,
	 * "Kayaks", and the whole translation for the others, "Adoption: no".
	 */
	fun getGroupLabels(translations: List<String>, osmValues: List<String>): List<String> {
		val size = translations.size
		val nameValues = ArrayList<Array<String>?>(size)
		val nameCounts = HashMap<String, Int>()
		var sameName = size > 0
		var firstName: String? = null
		for (translation in translations) {
			val nameValue = splitNameValue(translation)
			nameValues.add(nameValue)
			if (nameValue == null) {
				sameName = false
				continue
			}
			val name = nameValue[0].lowercase()
			nameCounts[name] = (nameCounts[name] ?: 0) + 1
			if (firstName == null) {
				firstName = name
			} else if (firstName != name) {
				sameName = false
			}
		}
		val labels = ArrayList<String>(size)
		for (i in 0 until size) {
			val translation = translations[i]
			val nameValue = nameValues[i]
			if (nameValue == null) {
				labels.add(translation)
			} else if (sameName) {
				labels.add(KAlgorithms.capitalizeFirstLetter(nameValue[1])!!)
			} else if (YES == osmValues[i] && nameCounts[nameValue[0].lowercase()] == 1) {
				labels.add(nameValue[0])
			} else {
				labels.add(translation)
			}
		}
		return labels
	}
}
