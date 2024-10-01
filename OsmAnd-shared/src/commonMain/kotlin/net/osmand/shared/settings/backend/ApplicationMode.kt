package net.osmand.shared.settings.backend

import net.osmand.shared.api.KStateChangedListener

class ApplicationMode private constructor(val keyName: String, val stringKey: String) {

	companion object {
		const val CUSTOM_MODE_KEY_SEPARATOR = "_"
		const val FAST_SPEED_THRESHOLD = 10f
		private const val MIN_VALUE_KM_H = -10f
		private const val MAX_VALUE_KM_H = 20f

		private val defaultValues = mutableListOf<ApplicationMode>()
		private val values = mutableListOf<ApplicationMode>()
		private var cachedFilteredValues = mutableListOf<ApplicationMode>()

		var listener: KStateChangedListener<String>? = null
		var iconNameListener: KStateChangedListener<String>? = null
		var customizationListener: OsmAndAppCustomizationListener? = null

		val DEFAULT = createBase("app_mode_default", "default")
			.icon("ic_world_globe_dark").reg()

		val CAR = createBase("app_mode_car", "car")
			.icon("ic_action_car_dark")
			.description("base_profile_descr_car").reg()

		val BICYCLE = createBase("app_mode_bicycle", "bicycle")
			.icon("ic_action_bicycle_dark")
			.description("base_profile_descr_bicycle").reg()

		val PEDESTRIAN = createBase("app_mode_pedestrian", "pedestrian")
			.icon("ic_action_pedestrian_dark")
			.description("base_profile_descr_pedestrian").reg()

		val TRUCK = create(CAR, "app_mode_truck", "truck")
			.icon("ic_action_truck_dark")
			.description("app_mode_truck").reg()

		val MOTORCYCLE = create(CAR, "app_mode_motorcycle", "motorcycle")
			.icon("ic_action_motorcycle_dark")
			.description("app_mode_motorcycle").reg()

		val MOPED = create(BICYCLE, "app_mode_moped", "moped")
			.icon("ic_action_motor_scooter")
			.description("app_mode_moped").reg()

		val PUBLIC_TRANSPORT = createBase("app_mode_public_transport", "public_transport")
			.icon("ic_action_bus_dark")
			.description("base_profile_descr_public_transport").reg()

		val TRAIN = createBase("app_mode_train", "train")
			.icon("ic_action_train")
			.description("app_mode_train").reg()

		val BOAT = createBase("app_mode_boat", "boat")
			.icon("ic_action_sail_boat_dark")
			.description("base_profile_descr_boat").reg()

		val AIRCRAFT = createBase("app_mode_aircraft", "aircraft")
			.icon("ic_action_aircraft")
			.description("base_profile_descr_aircraft").reg()

		val SKI = createBase("app_mode_skiing", "ski")
			.icon("ic_action_skiing")
			.description("base_profile_descr_ski").reg()

		val HORSE = createBase("horseback_riding", "horse")
			.icon("ic_action_horse")
			.description("horseback_riding").reg()

		fun values(app: OsmandApplication): List<ApplicationMode> {
			if (customizationListener == null) {
				customizationListener = {
					cachedFilteredValues = mutableListOf()
				}
				app.getAppCustomization().addListener(customizationListener!!)
			}
			if (cachedFilteredValues.isEmpty()) {
				val settings = app.settings
				if (listener == null) {
					listener = {
						cachedFilteredValues = mutableListOf()
					}
					settings.availableAppModes.addListener(listener!!)
				}
				val available = settings.availableAppModes.get()
				cachedFilteredValues = mutableListOf()
				for (v in values) {
					if (available.contains("${v.stringKey},") || v == DEFAULT) {
						cachedFilteredValues.add(v)
					}
				}
			}
			return cachedFilteredValues
		}

		fun allPossibleValues(): List<ApplicationMode> = values

		fun getDefaultValues(): List<ApplicationMode> = defaultValues

		fun getCustomValues(): List<ApplicationMode> {
			return values.filter { it.isCustomProfile() }
		}

		fun valueOfStringKey(key: String, def: ApplicationMode): ApplicationMode {
			return values.find { it.stringKey == key } ?: def
		}

		fun getModesDerivedFrom(am: ApplicationMode): List<ApplicationMode> {
			return values.filter { it == am || it.parent == am }
		}

		fun getModesForRouting(app: OsmandApplication): List<ApplicationMode> {
			return values(app).filter { it != DEFAULT }
		}

		fun create(parent: ApplicationMode, key: String, stringKey: String): ApplicationModeBuilder {
			return ApplicationModeBuilder(ApplicationMode(key, stringKey)).parent(parent)
		}

		fun createBase(key: String, stringKey: String): ApplicationModeBuilder {
			return ApplicationModeBuilder(ApplicationMode(key, stringKey))
		}
	}

	var descriptionId: String = ""
	var parentAppMode: ApplicationMode? = null
	var iconRes: String = "ic_world_globe_dark"
	lateinit var app: OsmandApplication

	val parent: ApplicationMode?
		get() = parentAppMode

	fun isCustomProfile(): Boolean {
		return isCustomProfile(stringKey)
	}

	fun isCustomProfile(key: String): Boolean {
		return defaultValues.none { it.stringKey == key }
	}

	fun reg(): ApplicationMode {
		Companion.values.add(this)
		Companion.defaultValues.add(this)
		return this
	}

	fun icon(iconRes: String): ApplicationMode {
		this.iconRes = iconRes
		return this
	}

	fun description(strId: String): ApplicationMode {
		this.descriptionId = strId
		return this
	}

	class ApplicationModeBuilder(private val applicationMode: ApplicationMode) {

		private var userProfileName: String? = null
		private var routeService: RouteService? = null
		private var routingProfile: String? = null
		private var iconResName: String? = null
		private var iconColor: ProfileIconColors? = null
		private var customIconColor: Int? = null
		private var locationIcon: String? = null
		private var navigationIcon: String? = null
		private var viewAngle: MarkerDisplayOption? = null
		private var locationRadius: MarkerDisplayOption? = null
		private var order: Int = -1
		private var version: Int = -1

		fun reg(): ApplicationMode {
			Companion.values.add(applicationMode)
			return applicationMode
		}

		fun parent(parent: ApplicationMode): ApplicationModeBuilder {
			applicationMode.parentAppMode = parent
			return this
		}

		fun icon(iconRes: String): ApplicationModeBuilder {
			applicationMode.iconRes = iconRes
			return this
		}

		fun description(strId: String): ApplicationModeBuilder {
			applicationMode.descriptionId = strId
			return this
		}
	}

	override fun toString(): String {
		return stringKey
	}
}