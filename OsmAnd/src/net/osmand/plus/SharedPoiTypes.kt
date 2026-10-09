package net.osmand.plus

import net.osmand.IndexConstants.SETTINGS_DIR
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiTranslator
import okio.source

// the POI types of OsmAnd-shared for the point card rows (AdditionalInfoBundle), read on the first card, not at startup
object SharedPoiTypes {

	private const val POI_TYPES_RESOURCE = "net/osmand/osm/poi_types.xml"

	@Volatile
	private var poiTypes: MapPoiTypes? = null

	@JvmStatic
	fun get(app: OsmandApplication): MapPoiTypes {
		poiTypes?.let { return it }
		synchronized(this) {
			poiTypes?.let { return it }
			val types = MapPoiTypes(null)
			val customPoiTypes = app.getAppPath(SETTINGS_DIR + "poi_types.xml")
			if (customPoiTypes.exists()) {
				types.init(customPoiTypes.absolutePath)
			} else {
				SharedPoiTypes::class.java.classLoader?.getResourceAsStream(POI_TYPES_RESOURCE)?.use {
					types.initFromSource(it.source())
				}
			}
			types.setPoiTranslator(Translator(app, types))
			MapPoiTypes.setDefault(types)
			poiTypes = types
			return types
		}
	}

	@JvmStatic
	fun reset() {
		poiTypes = null
	}

	// the translations of MapPoiTypesTranslator for the shared types
	private class Translator(
		private val app: OsmandApplication,
		private val poiTypes: MapPoiTypes
	) : PoiTranslator {

		private val translator = MapPoiTypesTranslator(app)

		override fun getTranslation(type: AbstractPoiType): String? {
			val baseLangType = type.getBaseLangType() ?: return translator.getTranslation(type.getFormattedKeyName())
			val translation = getTranslation(baseLangType) ?: poiTypes.getBasePoiName(baseLangType)
			return translation + langSuffix(type)
		}

		override fun getTranslation(keyName: String): String? = translator.getTranslation(keyName)

		override fun getEnTranslation(type: AbstractPoiType): String? {
			val baseLangType = type.getBaseLangType() ?: return translator.getEnTranslation(type.getFormattedKeyName())
			return getEnTranslation(baseLangType) + langSuffix(type)
		}

		override fun getEnTranslation(keyName: String): String? = translator.getEnTranslation(keyName)

		override fun getSynonyms(type: AbstractPoiType): String? =
			translator.getSynonyms((type.getBaseLangType() ?: type).getFormattedKeyName())

		override fun getSynonyms(keyName: String): String? = translator.getSynonyms(keyName)

		override fun getAllLanguagesTranslationSuffix(): String? = translator.allLanguagesTranslationSuffix

		private fun langSuffix(type: AbstractPoiType): String =
			" (" + AndroidUtils.getLangTranslation(app, type.getLang() ?: "").lowercase() + ")"
	}
}
