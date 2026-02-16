package net.osmand.plus.plugins.astro

import net.osmand.plus.settings.backend.preferences.CommonPreference
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

class StarWatcherSettings(private val settingsPref: CommonPreference<String>) {

	companion object {
		private const val KEY_COMMON = "common"
		private const val KEY_SHOW_REGULAR_MAP = "showRegularMap"
		private const val KEY_SHOW_STAR_CHART = "showStarChart"
		private const val KEY_IS_2D_MODE = "is2DMode"

		private const val KEY_STAR_MAP = "star_map"

		private const val KEY_SHOW_AZIMUTHAL = "showAzimuthalGrid"
		private const val KEY_SHOW_EQUATORIAL = "showEquatorialGrid"
		private const val KEY_SHOW_ECLIPTIC = "showEclipticLine"
		private const val KEY_SHOW_MERIDIAN = "showMeridianLine"
		private const val KEY_SHOW_EQUATOR = "showEquatorLine"
		private const val KEY_SHOW_GALACTIC = "showGalacticLine"
		private const val KEY_SHOW_SUN = "showSun"
		private const val KEY_SHOW_MOON = "showMoon"
		private const val KEY_SHOW_PLANETS = "showPlanets"
		private const val KEY_SHOW_FAVORITES = "showFavorites"
		private const val KEY_SHOW_RED_FILTER = "showRedFilter"

		private const val KEY_SHOW_CONSTELLATIONS = "showConstellations"

		private const val KEY_SHOW_STARS = "showStars"
		private const val KEY_SHOW_GALAXIES = "showGalaxies"
		private const val KEY_SHOW_NEBULAE = "showNebulae"
		private const val KEY_SHOW_OPEN_CLUSTERS = "showOpenClusters"
		private const val KEY_SHOW_GLOBULAR_CLUSTERS = "showGlobularClusters"
		private const val KEY_SHOW_GALAXY_CLUSTERS = "showGalaxyClusters"
		private const val KEY_SHOW_BLACK_HOLES = "showBlackHoles"
		private const val KEY_SHOW_MAGNITUDE_FILTER = "showMagnitudeFilter"
		private const val KEY_MAGNITUDE_FILTER = "magnitudeFilter"

		private const val KEY_FAVORITES = "favorites"
		private const val KEY_DIRECTIONS = "directions"
		private const val KEY_CELESTIAL_PATHS = "celestialPaths"
		private const val KEY_ID = "id"
	}

	abstract class Config(
		open val id: String,
	)

	data class FavoriteConfig(
		override val id: String,
	) : Config(id)

	data class DirectionConfig(
		override val id: String,
	) : Config(id)

	data class CelestialPathConfig(
		override val id: String,
	) : Config(id)

	data class CommonConfig(
		val showRegularMap: Boolean,
		val showStarChart: Boolean
	)

	data class StarMapConfig(
		val showAzimuthalGrid: Boolean,
		val showEquatorialGrid: Boolean,
		val showEclipticLine: Boolean,
		val showMeridianLine: Boolean,
		val showEquatorLine: Boolean,
		val showGalacticLine: Boolean,
		val showFavorites: Boolean,
		val showRedFilter: Boolean,

		val showSun: Boolean,
		val showMoon: Boolean,
		val showPlanets: Boolean,
		val showConstellations: Boolean,
		val showStars: Boolean,
		val showGalaxies: Boolean,
		val showNebulae: Boolean,
		val showOpenClusters: Boolean,
		val showGlobularClusters: Boolean,
		val showGalaxyClusters: Boolean,
		val showBlackHoles: Boolean,
		val is2DMode: Boolean,
		val showMagnitudeFilter: Boolean,
		val magnitudeFilter: Double?,
		val favorites: List<FavoriteConfig>,
		val directions: List<DirectionConfig>,
		val celestialPaths: List<CelestialPathConfig>
	)

	private fun getSettingsJson(): JSONObject {
		val str = settingsPref.get()
		if (str.isNullOrEmpty()) {
			return JSONObject()
		}
		return try {
			JSONObject(str)
		} catch (_: Exception) {
			JSONObject()
		}
	}

	private fun setSettingsJson(json: JSONObject) {
		settingsPref.set(json.toString())
	}

	private fun <T : Config> parseItems(json: JSONObject?, key: String, factory: (String) -> T): List<T> {
		val itemsList = mutableListOf<T>()
		val itemsJson = json?.optJSONArray(key)

		if (itemsJson != null) {
			for (i in 0 until itemsJson.length()) {
				val itemObj = itemsJson.optJSONObject(i)
				val id = itemObj?.optString(KEY_ID)
				if (!id.isNullOrEmpty()) {
					itemsList.add(factory(id))
				}
			}
		}

		return itemsList
	}

	private fun serializeItems(items: List<Config>): JSONArray {
		val array = JSONArray()
		items.forEach { item ->
			val obj = JSONObject()
			obj.put(KEY_ID, item.id)
			array.put(obj)
		}
		return array
	}

	fun getCommonConfig(): CommonConfig {
		val root = getSettingsJson()
		val settings = root.optJSONObject(KEY_COMMON)

		val showStarMap = settings?.optBoolean(KEY_SHOW_REGULAR_MAP, true) ?: true
		val showStarChart = settings?.optBoolean(KEY_SHOW_STAR_CHART, false) ?: false

		return CommonConfig(showStarMap, showStarChart)
	}

	fun setCommonConfig(config: CommonConfig) {
		val root = getSettingsJson()
		val settings = root.optJSONObject(KEY_COMMON) ?: JSONObject()

		settings.put(KEY_SHOW_REGULAR_MAP, config.showRegularMap)
		settings.put(KEY_SHOW_STAR_CHART, config.showStarChart)

		root.put(KEY_COMMON, settings)
		setSettingsJson(root)
	}

	fun getStarMapConfig(): StarMapConfig {
		val root = getSettingsJson()
		val mapSettings = root.optJSONObject(KEY_STAR_MAP)

		val showAzimuthal = mapSettings?.optBoolean(KEY_SHOW_AZIMUTHAL, true) ?: true
		val showEquatorial = mapSettings?.optBoolean(KEY_SHOW_EQUATORIAL, false) ?: false
		val showEcliptic = mapSettings?.optBoolean(KEY_SHOW_ECLIPTIC, false) ?: false
		val showMeridian = mapSettings?.optBoolean(KEY_SHOW_MERIDIAN, false) ?: false
		val showEquator = mapSettings?.optBoolean(KEY_SHOW_EQUATOR, false) ?: false
		val showGalactic = mapSettings?.optBoolean(KEY_SHOW_GALACTIC, false) ?: false
		val showFavorites = mapSettings?.optBoolean(KEY_SHOW_FAVORITES, true) ?: true
		val showRedFilter = mapSettings?.optBoolean(KEY_SHOW_RED_FILTER, false) ?: false

		val showSun = mapSettings?.optBoolean(KEY_SHOW_SUN, true) ?: true
		val showMoon = mapSettings?.optBoolean(KEY_SHOW_MOON, true) ?: true
		val showPlanets = mapSettings?.optBoolean(KEY_SHOW_PLANETS, true) ?: true

		val showConstellations = mapSettings?.optBoolean(KEY_SHOW_CONSTELLATIONS, false) ?: false

		val showStars = mapSettings?.optBoolean(KEY_SHOW_STARS, false) ?: false
		val showGalaxies = mapSettings?.optBoolean(KEY_SHOW_GALAXIES, false) ?: false
		val showNebulae = mapSettings?.optBoolean(KEY_SHOW_NEBULAE, false) ?: false
		val showOpenClusters = mapSettings?.optBoolean(KEY_SHOW_OPEN_CLUSTERS, false) ?: false
		val showGlobularClusters = mapSettings?.optBoolean(KEY_SHOW_GLOBULAR_CLUSTERS, false) ?: false
		val showGalaxyClusters = mapSettings?.optBoolean(KEY_SHOW_GALAXY_CLUSTERS, false) ?: false
		val showBlackHoles = mapSettings?.optBoolean(KEY_SHOW_BLACK_HOLES, false) ?: false

		val is2DMode = mapSettings?.optBoolean(KEY_IS_2D_MODE, false) ?: false

		val showMagnitudeFilter = mapSettings?.optBoolean(KEY_SHOW_MAGNITUDE_FILTER, false) ?: false
		val magnitudeFilter = mapSettings?.optDouble(KEY_MAGNITUDE_FILTER)?.takeIf { !it.isNaN() }

		val favorites = parseItems(mapSettings, KEY_FAVORITES) { FavoriteConfig(it) }
		val directions = parseItems(mapSettings, KEY_DIRECTIONS) { DirectionConfig(it) }
		val celestialPaths = parseItems(mapSettings, KEY_CELESTIAL_PATHS) { CelestialPathConfig(it) }

		return StarMapConfig(
			showAzimuthalGrid = showAzimuthal,
			showEquatorialGrid = showEquatorial,
			showEclipticLine = showEcliptic,
			showMeridianLine = showMeridian,
			showEquatorLine = showEquator,
			showGalacticLine = showGalactic,
			showFavorites = showFavorites,
			showRedFilter = showRedFilter,
			showSun = showSun,
			showMoon = showMoon,
			showPlanets = showPlanets,
			showConstellations = showConstellations,
			showStars = showStars,
			showGalaxies = showGalaxies,
			showNebulae = showNebulae,
			showOpenClusters = showOpenClusters,
			showGlobularClusters = showGlobularClusters,
			showGalaxyClusters = showGalaxyClusters,
			showBlackHoles = showBlackHoles,
			is2DMode = is2DMode,
			showMagnitudeFilter = showMagnitudeFilter,
			magnitudeFilter = magnitudeFilter,
			favorites = favorites,
			directions = directions,
			celestialPaths = celestialPaths
		)
	}

	fun setStarMapConfig(config: StarMapConfig) {
		val root = getSettingsJson()
		val mapSettings = root.optJSONObject(KEY_STAR_MAP) ?: JSONObject()

		mapSettings.put(KEY_SHOW_AZIMUTHAL, config.showAzimuthalGrid)
		mapSettings.put(KEY_SHOW_EQUATORIAL, config.showEquatorialGrid)
		mapSettings.put(KEY_SHOW_ECLIPTIC, config.showEclipticLine)
		mapSettings.put(KEY_SHOW_MERIDIAN, config.showMeridianLine)
		mapSettings.put(KEY_SHOW_EQUATOR, config.showEquatorLine)
		mapSettings.put(KEY_SHOW_GALACTIC, config.showGalacticLine)
		mapSettings.put(KEY_SHOW_FAVORITES, config.showFavorites)
		mapSettings.put(KEY_SHOW_RED_FILTER, config.showRedFilter)

		mapSettings.put(KEY_SHOW_SUN, config.showSun)
		mapSettings.put(KEY_SHOW_MOON, config.showMoon)
		mapSettings.put(KEY_SHOW_PLANETS, config.showPlanets)

		mapSettings.put(KEY_SHOW_CONSTELLATIONS, config.showConstellations)

		mapSettings.put(KEY_SHOW_STARS, config.showStars)
		mapSettings.put(KEY_SHOW_GALAXIES, config.showGalaxies)
		mapSettings.put(KEY_SHOW_NEBULAE, config.showNebulae)
		mapSettings.put(KEY_SHOW_OPEN_CLUSTERS, config.showOpenClusters)
		mapSettings.put(KEY_SHOW_GLOBULAR_CLUSTERS, config.showGlobularClusters)
		mapSettings.put(KEY_SHOW_GALAXY_CLUSTERS, config.showGalaxyClusters)
		mapSettings.put(KEY_SHOW_BLACK_HOLES, config.showBlackHoles)

		mapSettings.put(KEY_IS_2D_MODE, config.is2DMode)

		mapSettings.put(KEY_SHOW_MAGNITUDE_FILTER, config.showMagnitudeFilter)
		if (config.magnitudeFilter == null) {
			mapSettings.remove(KEY_MAGNITUDE_FILTER)
		} else {
			mapSettings.put(KEY_MAGNITUDE_FILTER, config.magnitudeFilter)
		}

		if (config.favorites.isEmpty()) {
			mapSettings.remove(KEY_FAVORITES)
		} else {
			mapSettings.put(KEY_FAVORITES, serializeItems(config.favorites))
		}
		if (config.directions.isEmpty()) {
			mapSettings.remove(KEY_DIRECTIONS)
		} else {
			mapSettings.put(KEY_DIRECTIONS, serializeItems(config.directions))
		}
		if (config.celestialPaths.isEmpty()) {
			mapSettings.remove(KEY_CELESTIAL_PATHS)
		} else {
			mapSettings.put(KEY_CELESTIAL_PATHS, serializeItems(config.celestialPaths))
		}

		root.put(KEY_STAR_MAP, mapSettings)
		setSettingsJson(root)
	}

	fun addFavorite(id: String) {
		val config = getStarMapConfig()
		if (config.favorites.none { it.id == id }) {
			val favorites = config.favorites.toMutableList()
			favorites.add(FavoriteConfig(id))
			setStarMapConfig(config.copy(favorites = favorites))
		}
	}

	fun removeFavorite(id: String) {
		val config = getStarMapConfig()
		val favorites = config.favorites.toMutableList()
		if (favorites.removeAll { it.id == id }) {
			setStarMapConfig(config.copy(favorites = favorites))
		}
	}

	fun addDirection(id: String) {
		val config = getStarMapConfig()
		if (config.directions.none { it.id == id }) {
			val directions = config.directions.toMutableList()
			directions.add(DirectionConfig(id))
			setStarMapConfig(config.copy(directions = directions))
		}
	}

	fun removeDirection(id: String) {
		val config = getStarMapConfig()
		val directions = config.directions.toMutableList()
		if (directions.removeAll { it.id == id }) {
			setStarMapConfig(config.copy(directions = directions))
		}
	}

	fun addCelestialPath(id: String) {
		val config = getStarMapConfig()
		if (config.celestialPaths.none { it.id == id }) {
			val paths = config.celestialPaths.toMutableList()
			paths.add(CelestialPathConfig(id))
			setStarMapConfig(config.copy(celestialPaths = paths))
		}
	}

	fun removeCelestialPath(id: String) {
		val config = getStarMapConfig()
		val paths = config.celestialPaths.toMutableList()
		if (paths.removeAll { it.id == id }) {
			setStarMapConfig(config.copy(celestialPaths = paths))
		}
	}
}
