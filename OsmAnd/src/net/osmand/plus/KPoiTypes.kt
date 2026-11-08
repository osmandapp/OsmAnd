package net.osmand.plus

import net.osmand.IndexConstants.SETTINGS_DIR
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiTranslator
import okio.source
import java.util.Locale

// app.poiTypes in OsmAnd-shared, until android moves from the java types to the shared ones
object KPoiTypes {

	private const val POI_TYPES_RESOURCE = "net/osmand/osm/poi_types.xml"

	@JvmStatic
	fun createEmpty(): MapPoiTypes = MapPoiTypes(null)

	@JvmStatic
	fun init(app: OsmandApplication, poiTypes: MapPoiTypes, translator: MapPoiTypesTranslator) {
		poiTypes.setForbiddenTypes(app.settings.forbiddenTypes)
		val customPoiTypes = app.getAppPath(SETTINGS_DIR + "poi_types.xml")
		if (customPoiTypes.exists()) {
			poiTypes.init(customPoiTypes.absolutePath)
		} else {
			val stream = KPoiTypes::class.java.classLoader?.getResourceAsStream(POI_TYPES_RESOURCE)
				?: throw IllegalStateException("$POI_TYPES_RESOURCE is not in the app")
			stream.use { poiTypes.initFromSource(it.source()) }
		}
		poiTypes.setPoiTranslator(Translator(app, poiTypes, translator))
		MapPoiTypes.setDefault(poiTypes)
	}

	private class Translator(
		private val app: OsmandApplication,
		private val poiTypes: MapPoiTypes,
		private val translator: MapPoiTypesTranslator
	) : PoiTranslator {

		override fun getTranslation(type: AbstractPoiType): String? {
			val baseLangType = type.getBaseLangType() ?: return getTranslation(type.getFormattedKeyName())
			val translation = getTranslation(baseLangType) ?: poiTypes.getBasePoiName(baseLangType)
			return translation + getLangSuffix(type)
		}

		override fun getTranslation(keyName: String): String? = translator.getTranslation(keyName)

		override fun getEnTranslation(type: AbstractPoiType): String? {
			val baseLangType = type.getBaseLangType() ?: return getEnTranslation(type.getFormattedKeyName())
			return getEnTranslation(baseLangType) + getLangSuffix(type)
		}

		override fun getEnTranslation(keyName: String): String? = translator.getEnTranslation(keyName)

		override fun getSynonyms(type: AbstractPoiType): String? {
			val baseLangType = type.getBaseLangType() ?: return getSynonyms(type.getFormattedKeyName())
			return getSynonyms(baseLangType)
		}

		override fun getSynonyms(keyName: String): String? = translator.getSynonyms(keyName)

		override fun getAllLanguagesTranslationSuffix(): String? = translator.allLanguagesTranslationSuffix

		private fun getLangSuffix(type: AbstractPoiType): String =
			" (" + AndroidUtils.getLangTranslation(app, type.getLang().orEmpty()).lowercase(Locale.getDefault()) + ")"
	}
}
