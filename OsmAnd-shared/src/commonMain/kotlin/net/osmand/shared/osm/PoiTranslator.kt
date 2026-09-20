package net.osmand.shared.osm

/**
 * Where [MapPoiTypes] gets the human readable name of a type from. The names are not in
 * poi_types.xml: they live in the platform's string resources, keyed by the type's key name.
 *
 * A copy of `MapPoiTypes.PoiTranslator` in OsmAnd-java. Left unimplemented here - the platform
 * supplies one, and without it a type falls back to its key name spelled out.
 */
interface PoiTranslator {

	fun getTranslation(type: AbstractPoiType): String?

	fun getTranslation(keyName: String): String?

	fun getEnTranslation(type: AbstractPoiType): String?

	fun getEnTranslation(keyName: String): String?

	fun getSynonyms(type: AbstractPoiType): String?

	fun getSynonyms(keyName: String): String?

	fun getAllLanguagesTranslationSuffix(): String?
}
