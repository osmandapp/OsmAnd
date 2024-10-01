package net.osmand.shared.settings.backend

import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.gpx.ColoringPurpose
import net.osmand.shared.routing.ColoringType
import net.osmand.shared.routing.ColoringType.Companion.valuesOf
import net.osmand.shared.settings.backend.preferences.CommonPreference
import net.osmand.shared.settings.backend.preferences.OsmandPreference
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants

class OsmandSettings {
	/// Settings variables
	val context: OsmandApplication
	private var settingsAPI: SettingsAPI
	var globalPreferences: Any? = null
		private set
	private var profilePreferences: Any? = null
	private var currentMode: ApplicationMode? = null
	private val registeredPreferences: MutableMap<String, OsmandPreference<*>> = LinkedHashMap()

	// cache variables
	private val lastTimeInternetConnectionChecked: Long = 0
	private var internetConnectionAvailable = true

	// TODO variable
	private val customRoutingProps: MutableMap<String, CommonPreference<String>> = LinkedHashMap()
	private val customRendersProps: MutableMap<String, CommonPreference<String>> = LinkedHashMap()
	private val customBooleanRoutingProps: MutableMap<String, CommonPreference<Boolean>> =
		LinkedHashMap()
	private val customBooleanRendersProps: MutableMap<String, CommonPreference<Boolean>> =
		LinkedHashMap()

	private val impassableRoadsStorage = ImpassableRoadsStorage(this)
	private val intermediatePointsStorage = IntermediatePointsStorage(this)

	private var appModeListener: StateChangedListener<ApplicationMode>? = null

	private var objectToShow: Any? = null
	private var editObjectToShow = false
	private var searchRequestToShow: String? = null

	constructor(clientContext: OsmandApplication, settinsAPI: SettingsAPI) {
		context = clientContext
		this.settingsAPI = settinsAPI
		initPrefs()
	}

	constructor(
		clientContext: OsmandApplication,
		settinsAPI: SettingsAPI,
		sharedPreferencesName: String
	) {
		context = clientContext
		this.settingsAPI = settinsAPI
		CUSTOM_SHARED_PREFERENCES_NAME = CUSTOM_SHARED_PREFERENCES_PREFIX + sharedPreferencesName
		initPrefs()
		setCustomized()
	}

	private fun initPrefs() {
		globalPreferences = settingsAPI.getPreferenceObject(getSharedPreferencesName(null))
		currentMode = readApplicationMode()
		profilePreferences = getProfilePreferences(currentMode)
		registeredPreferences[APPLICATION_MODE.id] = APPLICATION_MODE
		initBaseAppMode()
	}

	private fun initBaseAppMode() {
		setAppModeCustomProperties()
		appModeListener =
			StateChangedListener { applicationMode: ApplicationMode? -> setAppModeCustomProperties() }
		APPLICATION_MODE.addListener(appModeListener)
	}

	fun setAppModeCustomProperties() {
		val appMode = APPLICATION_MODE.get()
		val parentAppMode = APPLICATION_MODE.get().parent

		getCustomRenderProperty(RenderingRuleStorageProperties.A_APP_MODE).setModeValue(
			appMode,
			appMode.stringKey
		)
		getCustomRenderProperty(RenderingRuleStorageProperties.A_BASE_APP_MODE).setModeValue(
			appMode,
			parentAppMode?.stringKey ?: appMode.stringKey
		)
	}

	fun getRegisteredPreferences(): Map<String, OsmandPreference<*>> {
		return Collections.unmodifiableMap(registeredPreferences)
	}

	private fun setCustomized() {
		settingsAPI.edit(globalPreferences).putBoolean(SETTING_CUSTOMIZED_ID, true).commit()
	}

	fun setSettingsAPI(settingsAPI: SettingsAPI) {
		this.settingsAPI = settingsAPI
		initPrefs()
	}

	fun getSettingsAPI(): SettingsAPI {
		return settingsAPI
	}

	fun getDataStore(appMode: ApplicationMode?): OsmAndPreferencesDataStore {
		return OsmAndPreferencesDataStore(this, appMode ?: APPLICATION_MODE.get())
	}

	// TODO doesn't look correct package visibility
	fun getProfilePreferences(mode: ApplicationMode?): Any {
		return settingsAPI.getPreferenceObject(getSharedPreferencesName(mode))
	}

	// TODO doesn't look correct package visibility
	fun getProfilePreferences(modeKey: String?): Any {
		return settingsAPI.getPreferenceObject(getSharedPreferencesNameForKey(modeKey))
	}

	fun getPreference(key: String): OsmandPreference<*> {
		return registeredPreferences[key]!!
	}

	// TODO doesn't look correct
	fun setPreferenceForAllModes(key: String, value: Any?) {
		for (mode in ApplicationMode.allPossibleValues()) {
			setPreference(key, value, mode)
		}
	}

	// TODO doesn't look correct
	fun setPreference(key: String, value: Any?): Boolean {
		return setPreference(key, value, APPLICATION_MODE.get())
	}

	// TODO doesn't look correct
	fun setPreference(key: String, value: Any?, mode: ApplicationMode?): Boolean {
		var value = value
		val preference = registeredPreferences[key] ?: return false
		if (preference === APPLICATION_MODE) {
			if (value is String) {
				val appMode = ApplicationMode.valueOfStringKey(value, null)
				if (appMode != null) {
					return setApplicationMode(appMode)
				}
			}
		} else if (preference === DEFAULT_APPLICATION_MODE) {
			if (value is String) {
				val appMode = ApplicationMode.valueOfStringKey(value, null)
				if (appMode != null) {
					return DEFAULT_APPLICATION_MODE.set(appMode)
				}
			}
		} else if (preference is EnumStringPreference<*>) {
			val enumPref = preference
			if (value is String) {
				val enumValue = enumPref.parseString(value as String?)
				if (enumValue != null) {
					return enumPref.setModeValue(mode, enumValue)
				}
				return false
			} else if (value is Enum<*>) {
				return enumPref.setModeValue(mode, value)
			} else if (value is Int) {
				val newVal = value
				if (newVal >= 0 && newVal < enumPref.values.size) {
					val enumValue = enumPref.values[newVal]
					return enumPref.setModeValue(mode, enumValue)
				}
				return false
			}
		} else if (preference is StringPreference) {
			if (value is String) {
				return preference.setModeValue(mode, value)
			}
		} else {
			if (value is String) {
				value = preference.parseString(value as String?)
			}
			if (preference is BooleanPreference) {
				if (value is Boolean) {
					return preference.setModeValue(mode, value)
				}
			} else if (preference is FloatPreference) {
				if (value is Float) {
					return preference.setModeValue(mode, value)
				}
			} else if (preference is IntPreference) {
				if (value is Int) {
					return preference.setModeValue(mode, value)
				}
			} else if (preference is LongPreference) {
				if (value is Long) {
					return preference.setModeValue(mode, value)
				}
			} else if (preference is ContextMenuItemsPreference) {
				if (value is ContextMenuItemsSettings) {
					return preference.setModeValue(mode, value)
				}
			}
		}
		return false
	}

	fun resetPreferenceForAllModes(prefId: String) {
		val preference = getPreference(prefId)
		if (preference != null) {
			for (mode in ApplicationMode.allPossibleValues()) {
				preference.resetModeToDefault(mode)
			}
		}
	}

	fun resetPreference(prefId: String, mode: ApplicationMode) {
		val preference = getPreference(prefId)
		if (preference != null) {
			preference.resetModeToDefault(mode)
		}
	}

	fun copyPreferencesFromProfile(modeFrom: ApplicationMode, modeTo: ApplicationMode) {
		copyProfilePreferences(modeFrom, modeTo, ArrayList(registeredPreferences.values))
	}

	fun copyProfilePreferences(
		modeFrom: ApplicationMode,
		modeTo: ApplicationMode,
		profilePreferences: List<OsmandPreference<*>>
	) {
		for (pref in profilePreferences) {
			if (prefCanBeCopiedOrReset(pref) && USER_PROFILE_NAME.id != pref.id) {
				val profilePref = pref as CommonPreference<*>
				if (PARENT_APP_MODE.id == pref.getId()) {
					if (modeTo.isCustomProfile) {
						modeTo.setParentAppMode((if (modeFrom.isCustomProfile) modeFrom.parent else modeFrom)!!)
					}
				} else {
					val copiedValue = profilePref.getModeValue(modeFrom)
					profilePref.setModeValue(modeTo, copiedValue)
				}
			}
		}
	}

	fun resetGlobalPreferences(preferences: List<OsmandPreference<*>>) {
		for (preference in preferences) {
			if (preference is CommonPreference<*>) {
				preference.resetToDefault()
			}
		}
	}

	fun resetPreferencesForProfile(mode: ApplicationMode?) {
		resetProfilePreferences(mode, ArrayList(registeredPreferences.values))
		setAppModeCustomProperties()
	}

	fun resetProfilePreferences(
		mode: ApplicationMode?,
		profilePreferences: List<OsmandPreference<*>>
	) {
		for (pref in profilePreferences) {
			if (prefCanBeCopiedOrReset(pref)) {
				pref.resetModeToDefault(mode)
			}
		}
	}

	private fun prefCanBeCopiedOrReset(pref: OsmandPreference<*>): Boolean {
		return (pref is CommonPreference<*> && !pref.isGlobal
				&& APP_MODE_ORDER.id != pref.getId()
				&& APP_MODE_VERSION.id != pref.getId())
	}

	fun isExportAvailableForPref(preference: OsmandPreference<*>): Boolean {
		if (APPLICATION_MODE.id == preference.id) {
			return true
		} else if (preference is CommonPreference<*>) {
			val commonPreference = preference
			return !commonPreference.isGlobal || commonPreference.isShared
		}
		return false
	}

	@JvmField
	var LAST_ROUTING_APPLICATION_MODE: ApplicationMode? = null

	fun switchAppModeToNext(): Boolean {
		return switchAppMode(true)
	}

	fun switchAppModeToPrevious(): Boolean {
		return switchAppMode(false)
	}

	fun switchAppMode(next: Boolean): Boolean {
		val appMode = applicationMode
		val enabledModes = ApplicationMode.values(context)
		val indexOfCurrent = enabledModes.indexOf(appMode)
		val indexOfNext = if (next) {
			if (indexOfCurrent < enabledModes.size - 1) indexOfCurrent + 1 else 0
		} else {
			if (indexOfCurrent > 0) indexOfCurrent - 1 else enabledModes.size - 1
		}
		val nextAppMode = enabledModes[indexOfNext]
		if (appMode !== nextAppMode && setApplicationMode(nextAppMode)) {
			val pattern = context.getString(R.string.application_profile_changed)
			val message = String.format(pattern, nextAppMode.toHumanString())
			context.showShortToastMessage(message)
			return true
		}
		return false
	}

	fun setApplicationMode(appMode: ApplicationMode): Boolean {
		return setApplicationMode(appMode, true)
	}

	fun setApplicationMode(appMode: ApplicationMode, markAsLastUsed: Boolean): Boolean {
		val valueSaved = APPLICATION_MODE.set(appMode)
		if (markAsLastUsed && valueSaved) {
			LAST_USED_APPLICATION_MODE.set(appMode.stringKey)
		}
		return valueSaved
	}

	@JvmField
	val APPLICATION_MODE: OsmandPreference<ApplicationMode> =
		object : PreferenceWithListener<ApplicationMode?>() {
			override fun getId(): String {
				return "application_mode"
			}

			override fun get(): ApplicationMode {
				return currentMode!!
			}

			override fun overrideDefaultValue(newDefaultValue: ApplicationMode) {
				throw UnsupportedOperationException()
			}

			override fun resetToDefault() {
				set(ApplicationMode.DEFAULT)
			}

			override fun resetModeToDefault(m: ApplicationMode) {
				throw UnsupportedOperationException()
			}

			override fun isSet(): Boolean {
				return true
			}

			override fun isSetForMode(m: ApplicationMode): Boolean {
				return true
			}

			override fun set(`val`: ApplicationMode): Boolean {
				val oldMode = currentMode
				val valueSaved =
					settingsAPI.edit(globalPreferences).putString(id, `val`.stringKey).commit()
				if (valueSaved) {
					currentMode = `val`
					profilePreferences = getProfilePreferences(currentMode)

					fireEvent(oldMode)
				}
				return valueSaved
			}

			override fun getModeValue(m: ApplicationMode): ApplicationMode {
				return m
			}

			override fun setModeValue(m: ApplicationMode, obj: ApplicationMode): Boolean {
				throw UnsupportedOperationException()
			}

			@Throws(JSONException::class)
			override fun writeToJson(json: JSONObject, appMode: ApplicationMode): Boolean {
				return if (appMode == null) {
					writeAppModeToJson(json, this)
				} else {
					true
				}
			}

			@Throws(JSONException::class)
			override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
				if (appMode == null) {
					readAppModeFromJson(json, this)
				}
			}

			override fun asString(): String {
				return appModeToString(get())
			}

			override fun asStringModeValue(m: ApplicationMode): String {
				return appModeToString(m)
			}

			override fun parseString(s: String): ApplicationMode {
				return appModeFromString(s)
			}
		}

	private fun appModeToString(appMode: ApplicationMode): String {
		return appMode.stringKey
	}

	private fun appModeFromString(s: String): ApplicationMode {
		return ApplicationMode.valueOfStringKey(s, ApplicationMode.DEFAULT)
	}

	@Throws(JSONException::class)
	private fun writeAppModeToJson(
		json: JSONObject,
		appModePref: OsmandPreference<ApplicationMode>
	): Boolean {
		json.put(appModePref.id, appModePref.asString())
		return true
	}

	@Throws(JSONException::class)
	private fun readAppModeFromJson(
		json: JSONObject,
		appModePref: OsmandPreference<ApplicationMode>
	) {
		val s = json.getString(appModePref.id)
		appModePref.set(appModePref.parseString(s))
	}

	val applicationMode: ApplicationMode
		get() = APPLICATION_MODE.get()

	fun hasAvailableApplicationMode(): Boolean {
		val currentModeCount = ApplicationMode.values(context).size
		return currentModeCount != 0 && (currentModeCount != 1 || applicationMode !== ApplicationMode.DEFAULT)
	}

	fun readApplicationMode(): ApplicationMode {
		val s = settingsAPI.getString(
			globalPreferences,
			APPLICATION_MODE.id,
			ApplicationMode.DEFAULT.stringKey
		)
		return ApplicationMode.valueOfStringKey(s, ApplicationMode.DEFAULT)
	}


	// Check internet connection available every 15 seconds
	fun isInternetConnectionAvailable(): Boolean {
		return isInternetConnectionAvailable(false)
	}

	fun isInternetConnectionAvailable(update: Boolean): Boolean {
		val delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked
		if (delta < 0 || delta > 15000 || update) {
			internetConnectionAvailable = isInternetConnected
		}
		return internetConnectionAvailable
	}

	val isWifiConnected: Boolean
		get() {
			try {
				val mgr =
					context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
				val ni = mgr.activeNetworkInfo
				return ni != null && ni.type == ConnectivityManager.TYPE_WIFI
			} catch (e: Exception) {
				return false
			}
		}

	private val isInternetConnected: Boolean
		get() {
			try {
				val mgr =
					context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
				val active = mgr.activeNetworkInfo
				if (active == null) {
					return false
				} else {
					val state = active.state
					return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING
				}
			} catch (e: Exception) {
				return false
			}
		}

	fun <T> registerInternalPreference(id: String, tCommonPreference: CommonPreference<T>) {
		registeredPreferences[id] = tCommonPreference
	}

	fun isSet(global: Boolean, id: String?): Boolean {
		return settingsAPI.contains(getPreferences(global), id)
	}

	fun isSet(m: ApplicationMode?, id: String?): Boolean {
		return settingsAPI.contains(getProfilePreferences(m), id)
	}

	fun getPreferences(global: Boolean): Any? {
		return if (global) globalPreferences else profilePreferences
	}

	fun getLastModePreferencesEditTime(mode: ApplicationMode?): Long {
		val preferences = getProfilePreferences(mode)
		return getLastPreferencesEditTime(preferences)
	}

	fun setLastModePreferencesEditTime(mode: ApplicationMode?, lastModifiedTime: Long) {
		val preferences = getProfilePreferences(mode)
		updateLastPreferencesEditTime(preferences, lastModifiedTime)
	}

	var lastGlobalPreferencesEditTime: Long
		get() = getLastPreferencesEditTime(globalPreferences)
		set(lastModifiedTime) {
			updateLastPreferencesEditTime(globalPreferences, lastModifiedTime)
		}

	private fun getLastPreferencesEditTime(preferences: Any?): Long {
		return settingsAPI.getLong(preferences, LAST_PREFERENCES_EDIT_TIME, 0)
	}

	fun updateLastPreferencesEditTime(preferences: Any?) {
		val time = System.currentTimeMillis()
		updateLastPreferencesEditTime(preferences, time)
	}

	protected fun updateLastPreferencesEditTime(preferences: Any?, time: Long) {
		settingsAPI.edit(preferences).putLong(LAST_PREFERENCES_EDIT_TIME, time).commit()
	}

	fun resetLastGlobalPreferencesEditTime() {
		settingsAPI.edit(globalPreferences).remove(LAST_PREFERENCES_EDIT_TIME).commit()
	}

	fun resetLastPreferencesEditTime(mode: ApplicationMode) {
		val profilePrefs = getProfilePreferences(mode)
		settingsAPI.edit(profilePrefs).remove(LAST_PREFERENCES_EDIT_TIME).commit()
	}

	fun removeFromGlobalPreferences(vararg prefIds: String) {
		val editor = settingsAPI.edit(globalPreferences)
		for (prefId in prefIds) {
			editor.remove(prefId)
		}
		editor.commit()
	}

	val savedGlobalPrefsCount: Int
		get() = (globalPreferences as SharedPreferences?)!!.all.size

	fun getSavedModePrefsCount(mode: ApplicationMode): Int {
		return (getProfilePreferences(mode) as SharedPreferences).all.size
	}

	fun registerBooleanPreference(id: String, defValue: Boolean): CommonPreference<Boolean> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<Boolean>
		}
		val p = BooleanPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun registerBooleanAccessibilityPreference(
		id: String,
		defValue: Boolean
	): CommonPreference<Boolean> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<Boolean>
		}
		val p: BooleanPreference = BooleanAccessibilityPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun registerStringPreference(id: String, defValue: String?): CommonPreference<String> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<String>
		}
		val p = StringPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun registerStringListPreference(
		id: String,
		defValue: String?,
		delimiter: String
	): ListStringPreference {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as ListStringPreference
		}
		val preference = ListStringPreference(this, id, defValue, delimiter)
		registeredPreferences[id] = preference
		return preference
	}

	fun registerIntPreference(id: String, defValue: Int): CommonPreference<Int> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<Int>
		}
		val p = IntPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun registerLongPreference(id: String, defValue: Long): CommonPreference<Long> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<Long>
		}
		val p = LongPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun registerFloatPreference(id: String, defValue: Float): CommonPreference<Float> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<Float>
		}
		val p = FloatPreference(this, id, defValue)
		registeredPreferences[id] = p
		return p
	}

	fun <T : Enum<*>?> registerEnumStringPreference(
		id: String,
		defaultValue: Enum<*>?,
		values: Array<Enum<*>?>?,
		clz: Class<T>?
	): CommonPreference<T> {
		if (registeredPreferences.containsKey(id)) {
			return registeredPreferences[id] as CommonPreference<T>
		}
		val preference: EnumStringPreference<*> =
			EnumStringPreference<Any?>(this, id, defaultValue, values)
		registeredPreferences[id] = preference
		return preference
	}

	private val PLUGINS: OsmandPreference<String> =
		StringPreference(this, "enabled_plugins", "").makeGlobal().makeShared()

	val enabledPlugins: Set<String>
		get() {
			val plugs = PLUGINS.get()
			val toks = StringTokenizer(plugs, ",")
			val res: MutableSet<String> = LinkedHashSet()
			while (toks.hasMoreTokens()) {
				val tok = toks.nextToken()
				if (!tok.startsWith("-")) {
					res.add(tok)
				}
			}
			return res
		}

	val plugins: MutableSet<String>
		get() {
			val plugs = PLUGINS.get()
			val toks = StringTokenizer(plugs, ",")
			val res: MutableSet<String> = LinkedHashSet()
			while (toks.hasMoreTokens()) {
				res.add(toks.nextToken())
			}
			return res
		}

	fun enablePlugin(pluginId: String, enable: Boolean): Boolean {
		val set = plugins
		if (enable) {
			set.remove("-$pluginId")
			set.add(pluginId)
		} else {
			set.remove(pluginId)
			set.add("-$pluginId")
		}
		val serialization = StringBuilder()
		val it: Iterator<String> = set.iterator()
		while (it.hasNext()) {
			serialization.append(it.next())
			if (it.hasNext()) {
				serialization.append(",")
			}
		}
		if (serialization.toString() != PLUGINS.get()) {
			return PLUGINS.set(serialization.toString())
		}
		return false
	}

	@JvmField
	val ENABLE_3D_MAPS: CommonPreference<Boolean> =
		registerBooleanPreference("enable_3d_maps", true).makeProfile().makeShared().cache()
	@JvmField
	val VERTICAL_EXAGGERATION_SCALE: CommonPreference<Float> =
		registerFloatPreference("vertical_exaggeration_scale", 1f).makeProfile()

	@JvmField
	val SIMULATE_POSITION_SPEED: CommonPreference<Int> =
		IntPreference(this, "simulate_position_movement_speed", 1).makeGlobal().makeShared()

	@JvmField
	val DISTANCE_BY_TAP_TEXT_SIZE: CommonPreference<DistanceByTapTextSize> = EnumStringPreference(
		this,
		"distance_by_tap_text_size",
		DistanceByTapTextSize.NORMAL,
		DistanceByTapTextSize.entries.toTypedArray()
	).makeProfile()

	@JvmField
	val RADIUS_RULER_MODE: OsmandPreference<RadiusRulerMode> = EnumStringPreference(
		this,
		"ruler_mode",
		RadiusRulerMode.FIRST,
		RadiusRulerMode.entries.toTypedArray()
	).makeProfile()
	@JvmField
	val SHOW_COMPASS_ON_RADIUS_RULER: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_compass_ruler", true).makeProfile()

	@JvmField
	val SHOW_DISTANCE_RULER: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_distance_ruler", false).makeProfile()

	@JvmField
	val SHOW_SPEED_LIMIT_WARNING: CommonPreference<SpeedLimitWarningState> = EnumStringPreference(
		this,
		"show_speed_limit_warning",
		SpeedLimitWarningState.WHEN_EXCEEDED,
		SpeedLimitWarningState.entries.toTypedArray()
	).makeProfile()

	@JvmField
	val SHOW_LINES_TO_FIRST_MARKERS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_lines_to_first_markers", false).makeProfile()
	@JvmField
	val SHOW_ARROWS_TO_FIRST_MARKERS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_arrows_to_first_markers", false).makeProfile()

	@JvmField
	val WIKI_ARTICLE_SHOW_IMAGES_ASKED: CommonPreference<Boolean> =
		BooleanPreference(this, "wikivoyage_show_images_asked", false).makeGlobal()
	@JvmField
	val WIKI_ARTICLE_SHOW_IMAGES: CommonPreference<WikiArticleShowImages> = EnumStringPreference(
		this,
		"wikivoyage_show_imgs",
		WikiArticleShowImages.OFF,
		WikiArticleShowImages.entries.toTypedArray()
	).makeGlobal().makeShared()

	@JvmField
	val SELECT_MARKER_ON_SINGLE_TAP: CommonPreference<Boolean> =
		BooleanPreference(this, "select_marker_on_single_tap", false).makeProfile()
	@JvmField
	val KEEP_PASSED_MARKERS_ON_MAP: CommonPreference<Boolean> =
		BooleanPreference(this, "keep_passed_markers_on_map", true).makeProfile()

	@JvmField
	val COORDS_INPUT_USE_RIGHT_SIDE: CommonPreference<Boolean> =
		BooleanPreference(this, "coords_input_use_right_side", true).makeGlobal().makeShared()
	@JvmField
	val COORDS_INPUT_FORMAT: OsmandPreference<CoordinateInputFormats.Format> = EnumStringPreference(
		this,
		"coords_input_format",
		CoordinateInputFormats.Format.DD_MM_MMM,
		CoordinateInputFormats.Format.entries.toTypedArray()
	).makeGlobal().makeShared()
	@JvmField
	val COORDS_INPUT_USE_OSMAND_KEYBOARD: CommonPreference<Boolean> = BooleanPreference(
		this,
		"coords_input_use_osmand_keyboard",
		Build.VERSION.SDK_INT >= 16
	).makeGlobal().makeShared()
	@JvmField
	val COORDS_INPUT_TWO_DIGITS_LONGTITUDE: CommonPreference<Boolean> = BooleanPreference(
		this, "coords_input_two_digits_longitude", false
	).makeGlobal().makeShared()

	@JvmField
	val FORCE_PRIVATE_ACCESS_ROUTING_ASKED: CommonPreference<Boolean> = BooleanPreference(
		this, "force_private_access_routing", false
	).makeProfile().cache()

	@JvmField
	val SHOW_CARD_TO_CHOOSE_DRAWER: CommonPreference<Boolean> =
		BooleanPreference(this, "show_card_to_choose_drawer", false).makeGlobal().makeShared()
	@JvmField
	val SHOW_DASHBOARD_ON_START: CommonPreference<Boolean> =
		BooleanPreference(this, "should_show_dashboard_on_start", false).makeGlobal().makeShared()
	@JvmField
	val SHOW_DASHBOARD_ON_MAP_SCREEN: CommonPreference<Boolean> =
		BooleanPreference(this, "show_dashboard_on_map_screen", false).makeGlobal().makeShared()
	@JvmField
	val SHOW_OSMAND_WELCOME_SCREEN: CommonPreference<Boolean> =
		BooleanPreference(this, "show_osmand_welcome_screen", true).makeGlobal()

	@JvmField
	val API_NAV_DRAWER_ITEMS_JSON: CommonPreference<String> =
		StringPreference(this, "api_nav_drawer_items_json", "{}").makeGlobal()
	@JvmField
	val API_CONNECTED_APPS_JSON: CommonPreference<String> =
		StringPreference(this, "api_connected_apps_json", "[]").makeGlobal()
	@JvmField
	val NAV_DRAWER_LOGO: CommonPreference<String> =
		StringPreference(this, "drawer_logo", "").makeProfile()
	@JvmField
	val NAV_DRAWER_URL: CommonPreference<String> =
		StringPreference(this, "drawer_url", "").makeProfile()

	@JvmField
	val NUMBER_OF_STARTS_FIRST_XMAS_SHOWN: CommonPreference<Int> =
		IntPreference(this, "number_of_starts_first_xmas_shown", 0).makeGlobal()

	@JvmField
	val AVAILABLE_APP_MODES: OsmandPreference<String> = object : StringPreference(
		this,
		"available_application_modes",
		"car,bicycle,pedestrian,public_transport,"
	) {
		@Throws(JSONException::class)
		override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
			val appModesKeys = Algorithms.decodeStringSet(json.getString(id), ",")
			val nonexistentAppModesKeys: MutableSet<String> = HashSet()
			for (appModeKey in appModesKeys) {
				if (ApplicationMode.valueOfStringKey(appModeKey, null) == null) {
					nonexistentAppModesKeys.add(appModeKey)
				}
			}
			if (!nonexistentAppModesKeys.isEmpty()) {
				appModesKeys.removeAll(nonexistentAppModesKeys)
			}
			set(parseString(Algorithms.encodeCollection(appModesKeys, ",")))
		}
	}.makeGlobal().makeShared().cache()

	@JvmField
	val LAST_FAV_CATEGORY_ENTERED: OsmandPreference<String> =
		StringPreference(this, "last_fav_category", "").makeGlobal()

	@JvmField
	val USE_LAST_APPLICATION_MODE_BY_DEFAULT: OsmandPreference<Boolean> = BooleanPreference(
		this, "use_last_application_mode_by_default", false
	).makeGlobal().makeShared()

	@JvmField
	val LAST_USED_APPLICATION_MODE: OsmandPreference<String> = StringPreference(
		this,
		"last_used_application_mode",
		ApplicationMode.DEFAULT.stringKey
	).makeGlobal().makeShared()

	@JvmField
	val DEFAULT_APPLICATION_MODE: OsmandPreference<ApplicationMode> =
		object : CommonPreference<ApplicationMode?>(
			this, "default_application_mode_string", ApplicationMode.DEFAULT
		) {
			override fun getValue(prefs: Any, defaultValue: ApplicationMode): ApplicationMode {
				val key = if (USE_LAST_APPLICATION_MODE_BY_DEFAULT.get()) {
					LAST_USED_APPLICATION_MODE.get()
				} else {
					settingsAPI.getString(prefs, id, defaultValue.stringKey)
				}
				return ApplicationMode.valueOfStringKey(key, defaultValue)
			}

			protected override fun setValue(prefs: Any, `val`: ApplicationMode): Boolean {
				val valueSaved = (super.setValue(prefs, `val`)
						&& settingsAPI.edit(prefs).putString(id, `val`.stringKey).commit())
				if (valueSaved) {
					setApplicationMode(`val`)
				}

				return valueSaved
			}

			@Throws(JSONException::class)
			override fun writeToJson(json: JSONObject, appMode: ApplicationMode): Boolean {
				return if (appMode == null) {
					writeAppModeToJson(json, this)
				} else {
					true
				}
			}

			@Throws(JSONException::class)
			override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
				if (appMode == null) {
					readAppModeFromJson(json, this)
				}
			}

			protected override fun toString(o: ApplicationMode): String {
				return appModeToString(o)
			}


			override fun parseString(s: String): ApplicationMode {
				return appModeFromString(s)
			}
		}.makeGlobal().makeShared()

	@JvmField
	val LAST_ROUTE_APPLICATION_MODE: OsmandPreference<ApplicationMode?> =
		object : CommonPreference<ApplicationMode?>(
			this, "last_route_application_mode_backup_string", ApplicationMode.DEFAULT
		) {
			override fun getValue(prefs: Any, defaultValue: ApplicationMode): ApplicationMode {
				val key = settingsAPI.getString(prefs, id, defaultValue.stringKey)
				return ApplicationMode.valueOfStringKey(key, defaultValue)
			}

			protected override fun setValue(prefs: Any, `val`: ApplicationMode): Boolean {
				return (super.setValue(prefs, `val`)
						&& settingsAPI.edit(prefs).putString(id, `val`.stringKey).commit())
			}

			@Throws(JSONException::class)
			override fun writeToJson(json: JSONObject, appMode: ApplicationMode): Boolean {
				return if (appMode == null) {
					writeAppModeToJson(json, this)
				} else {
					true
				}
			}

			@Throws(JSONException::class)
			override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
				if (appMode == null) {
					readAppModeFromJson(json, this)
				}
			}

			protected override fun toString(o: ApplicationMode): String {
				return appModeToString(o)
			}


			override fun parseString(s: String): ApplicationMode {
				return appModeFromString(s)
			}
		}.makeGlobal()

	@JvmField
	val FIRST_MAP_IS_DOWNLOADED: OsmandPreference<Boolean> =
		BooleanPreference(this, "first_map_is_downloaded", false)

	@JvmField
	val DRIVING_REGION_AUTOMATIC: CommonPreference<Boolean> =
		BooleanPreference(this, "driving_region_automatic", true).makeProfile().cache()
	@JvmField
	val DRIVING_REGION: OsmandPreference<DrivingRegion> =
		object : EnumStringPreference<DrivingRegion?>(
			this,
			"default_driving_region",
			DrivingRegion.EUROPE_ASIA,
			DrivingRegion.entries.toTypedArray()
		) {
			override fun setValue(prefs: Any, `val`: DrivingRegion): Boolean {
				val overrideMetricSystem =
					!DRIVING_REGION_AUTOMATIC.getValue(prefs, DRIVING_REGION_AUTOMATIC.defaultValue)
				if (overrideMetricSystem && `val` != null) {
					METRIC_SYSTEM.setValue(prefs, `val`.defMetrics)
				}
				return super.setValue(prefs, `val`)
			}

			override fun getDefaultValue(): DrivingRegion {
				return DrivingRegion.getDrivingRegionByLocale()
			}

			override fun getProfileDefaultValue(mode: ApplicationMode): DrivingRegion {
				return DrivingRegion.getDrivingRegionByLocale()
			}
		}.makeProfile().cache()

	// cache of metrics constants as they are used very often
	@JvmField
	val METRIC_SYSTEM: EnumStringPreference<MetricsConstants?> =
		object : EnumStringPreference<MetricsConstants?>(
			this,
			"default_metric_system",
			MetricsConstants.KILOMETERS_AND_METERS,
			MetricsConstants.entries.toTypedArray()
		) {
			override fun getDefaultValue(): MetricsConstants {
				return DRIVING_REGION.get().defMetrics
			}

			override fun getProfileDefaultValue(mode: ApplicationMode): MetricsConstants {
				return DRIVING_REGION.getModeValue(mode).defMetrics
			}
		}.makeProfile() as EnumStringPreference<MetricsConstants?>

	//public final OsmandPreference<Integer> COORDINATES_FORMAT = new IntPreference("coordinates_format", PointDescription.FORMAT_DEGREES).makeGlobal();
	@JvmField
	val ANGULAR_UNITS: OsmandPreference<AngularConstants> = EnumStringPreference(
		this,
		"angular_measurement", AngularConstants.DEGREES, AngularConstants.entries.toTypedArray()
	).makeProfile()

	val lastStartPoint: LatLon?
		get() {
			if (settingsAPI.contains(globalPreferences, LAST_START_LAT) && settingsAPI.contains(
					globalPreferences,
					LAST_START_LON
				)
			) {
				return LatLon(
					settingsAPI.getFloat(globalPreferences, LAST_START_LAT, 0f).toDouble(),
					settingsAPI.getFloat(globalPreferences, LAST_START_LON, 0f).toDouble()
				)
			}
			return null
		}

	fun setLastStartPoint(l: LatLon?): Boolean {
		return if (l == null) {
			settingsAPI.edit(globalPreferences).remove(LAST_START_LAT)
				.remove(LAST_START_LON).commit()
		} else {
			setLastStartPoint(l.latitude, l.longitude)
		}
	}

	fun setLastStartPoint(lat: Double, lon: Double): Boolean {
		return settingsAPI.edit(globalPreferences).putFloat(LAST_START_LAT, lat.toFloat()).putFloat
		LAST_START_LON, lon.toFloat()).commit()
	}

	@JvmField
	val SPEED_SYSTEM: OsmandPreference<SpeedConstants?> =
		object : EnumStringPreference<SpeedConstants?>(
			this,
			"default_speed_system",
			SpeedConstants.KILOMETERS_PER_HOUR,
			SpeedConstants.entries.toTypedArray()
		) {
			override fun getProfileDefaultValue(mode: ApplicationMode): SpeedConstants {
				val mc = METRIC_SYSTEM.getModeValue(mode)
				if (mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
					return if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
						SpeedConstants.MINUTES_PER_KILOMETER
					} else {
						SpeedConstants.MILES_PER_HOUR
					}
				}
				if (mode.isDerivedRoutingFrom(ApplicationMode.BOAT)) {
					return SpeedConstants.NAUTICALMILES_PER_HOUR
				}
				return if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
					SpeedConstants.NAUTICALMILES_PER_HOUR
				} else if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
					SpeedConstants.KILOMETERS_PER_HOUR
				} else {
					SpeedConstants.MILES_PER_HOUR
				}
			}
		}.makeProfile()


	// cache of metrics constants as they are used very often
	@JvmField
	val DIRECTION_STYLE: OsmandPreference<RelativeDirectionStyle> = EnumStringPreference(
		this,
		"direction_style",
		RelativeDirectionStyle.SIDEWISE,
		RelativeDirectionStyle.entries.toTypedArray()
	).makeProfile().cache()

	// cache of metrics constants as they are used very often
	@JvmField
	val ACCESSIBILITY_MODE: OsmandPreference<AccessibilityMode> = EnumStringPreference(
		this,
		"accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.entries.toTypedArray()
	).makeProfile().cache()

	@JvmField
	val SPEECH_RATE: OsmandPreference<Float> =
		FloatPreference(this, "speech_rate", 1f).makeProfile()

	@JvmField
	val ARRIVAL_DISTANCE_FACTOR: OsmandPreference<Float> =
		FloatPreference(this, "arrival_distance_factor", 1f).makeProfile()

	@JvmField
	val SPEED_LIMIT_EXCEED_KMH: OsmandPreference<Float> =
		FloatPreference(this, "speed_limit_exceed", 5f).makeProfile()

	@JvmField
	val DEFAULT_SPEED: CommonPreference<Float> =
		FloatPreference(this, "default_speed", 10f).makeProfile().cache()

	init {
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.DEFAULT, 1.5f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.CAR, 12.5f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.BICYCLE, 2.77f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 1.11f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.BOAT, 1.38f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.AIRCRAFT, 200f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.SKI, 1.38f)
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.HORSE, 1.66f)
	}

	@JvmField
	val MIN_SPEED: OsmandPreference<Float> = FloatPreference(
		this,
		"min_speed", 0f
	).makeProfile().cache()

	@JvmField
	val MAX_SPEED: OsmandPreference<Float> = FloatPreference(
		this,
		"max_speed", 0f
	).makeProfile().cache()

	@JvmField
	val ICON_RES_NAME: CommonPreference<String> =
		object : StringPreference(this, "app_mode_icon_res_name", "ic_world_globe_dark") {
			override fun getModeValue(mode: ApplicationMode): String {
				val iconResName = super.getModeValue(mode)
				if (AndroidUtils.getDrawableId(context, iconResName) != 0) {
					return iconResName
				}
				return getProfileDefaultValue(mode)
			}
		}.makeProfile().cache()

	init {
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.DEFAULT, "ic_world_globe_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.CAR, "ic_action_car_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, "ic_action_bicycle_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "ic_action_pedestrian_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.PUBLIC_TRANSPORT, "ic_action_bus_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.MOPED, "ic_action_motor_scooter")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.BOAT, "ic_action_sail_boat_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.AIRCRAFT, "ic_action_aircraft")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.SKI, "ic_action_skiing")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.TRUCK, "ic_action_truck_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.MOTORCYCLE, "ic_action_motorcycle_dark")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.HORSE, "ic_action_horse")
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.TRAIN, "ic_action_train")
	}

	@JvmField
	val ICON_COLOR: CommonPreference<ProfileIconColors> = EnumStringPreference(
		this,
		"app_mode_icon_color", ProfileIconColors.DEFAULT, ProfileIconColors.entries.toTypedArray()
	).makeProfile().cache()

	@JvmField
	val CUSTOM_ICON_COLOR: CommonPreference<String> =
		StringPreference(this, "custom_icon_color", null).makeProfile().cache()

	@JvmField
	val USER_PROFILE_NAME: CommonPreference<String> =
		StringPreference(this, "user_profile_name", "").makeProfile().cache()

	@JvmField
	val PARENT_APP_MODE: CommonPreference<String> =
		StringPreference(this, "parent_app_mode", null).makeProfile().cache()

	@JvmField
	val DERIVED_PROFILE: CommonPreference<String> =
		StringPreference(this, "derived_profile", "default").makeProfile().cache()

	init {
		DERIVED_PROFILE.setModeDefaultValue(ApplicationMode.MOTORCYCLE, "motorcycle")
		DERIVED_PROFILE.setModeDefaultValue(ApplicationMode.TRUCK, "truck")
	}

	@JvmField
	val ROUTING_PROFILE: CommonPreference<String> =
		StringPreference(this, "routing_profile", "").makeProfile().cache()

	init {
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.CAR, "car")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.BICYCLE, "bicycle")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "pedestrian")
		ROUTING_PROFILE.setModeDefaultValue(
			ApplicationMode.PUBLIC_TRANSPORT,
			TransportRoutingHelper.PUBLIC_TRANSPORT_KEY
		)
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.BOAT, "boat")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.AIRCRAFT, "STRAIGHT_LINE_MODE")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.SKI, "ski")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.HORSE, "horsebackriding")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.MOPED, "moped")
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.TRAIN, "train")
	}

	@JvmField
	val ROUTE_SERVICE: CommonPreference<RouteService> =
		object : EnumStringPreference<RouteService?>(
			this, "route_service", RouteService.OSMAND, RouteService.entries.toTypedArray()
		) {
			override fun getModeValue(mode: ApplicationMode): RouteService {
				return if (mode === ApplicationMode.DEFAULT) {
					RouteService.STRAIGHT
				} else {
					super.getModeValue(mode)!!
				}
			}
		}.makeProfile().cache()

	init {
		ROUTE_SERVICE.setModeDefaultValue(ApplicationMode.DEFAULT, RouteService.STRAIGHT)
		ROUTE_SERVICE.setModeDefaultValue(ApplicationMode.AIRCRAFT, RouteService.STRAIGHT)
	}

	@JvmField
	val ONLINE_ROUTING_ENGINES: CommonPreference<String> =
		StringPreference(this, "online_routing_engines", null).makeGlobal().makeShared()
			.storeLastModifiedTime()

	@JvmField
	val NAVIGATION_ICON: CommonPreference<String> =
		StringPreference(this, "navigation_icon", LocationIcon.MOVEMENT_DEFAULT.name).makeProfile()
			.cache()

	init {
		NAVIGATION_ICON.setModeDefaultValue(
			ApplicationMode.DEFAULT,
			LocationIcon.MOVEMENT_DEFAULT.name
		)
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.CAR, LocationIcon.MOVEMENT_DEFAULT.name)
		NAVIGATION_ICON.setModeDefaultValue(
			ApplicationMode.BICYCLE,
			LocationIcon.MOVEMENT_DEFAULT.name
		)
		NAVIGATION_ICON.setModeDefaultValue(
			ApplicationMode.BOAT,
			LocationIcon.MOVEMENT_NAUTICAL.name
		)
		NAVIGATION_ICON.setModeDefaultValue(
			ApplicationMode.AIRCRAFT,
			LocationIcon.MOVEMENT_DEFAULT.name
		)
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.SKI, LocationIcon.MOVEMENT_DEFAULT.name)
		NAVIGATION_ICON.setModeDefaultValue(
			ApplicationMode.HORSE,
			LocationIcon.MOVEMENT_DEFAULT.name
		)
	}

	@JvmField
	val LOCATION_ICON: CommonPreference<String> =
		StringPreference(this, "location_icon", LocationIcon.STATIC_DEFAULT.name).makeProfile()
			.cache()

	init {
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.DEFAULT, LocationIcon.STATIC_DEFAULT.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.CAR, LocationIcon.STATIC_CAR.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.BICYCLE, LocationIcon.STATIC_BICYCLE.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.BOAT, LocationIcon.STATIC_DEFAULT.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.AIRCRAFT, LocationIcon.STATIC_CAR.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.SKI, LocationIcon.STATIC_BICYCLE.name)
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.HORSE, LocationIcon.STATIC_BICYCLE.name)
	}

	@JvmField
	val APP_MODE_ORDER: CommonPreference<Int> =
		IntPreference(this, "app_mode_order", 0).makeProfile().cache()

	init {
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.DEFAULT, 0)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.CAR, 1)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.BICYCLE, 2)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 3)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.TRUCK, 4)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.MOTORCYCLE, 5)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.MOPED, 6)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.PUBLIC_TRANSPORT, 7)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.TRAIN, 8)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.BOAT, 9)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.AIRCRAFT, 10)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.SKI, 11)
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.HORSE, 12)
	}

	@JvmField
	val APP_MODE_VERSION: CommonPreference<Int> =
		IntPreference(this, "app_mode_version", 0).makeProfile().cache()

	@JvmField
	val USE_TRACKBALL_FOR_MOVEMENTS: OsmandPreference<Boolean> =
		BooleanPreference(this, "use_trackball_for_movements", true).makeProfile()

	@JvmField
	val ACCESSIBILITY_SMART_AUTOANNOUNCE: OsmandPreference<Boolean> =
		BooleanAccessibilityPreference(
			this, "accessibility_smart_autoannounce", true
		).makeProfile()

	// cache of metrics constants as they are used very often
	@JvmField
	val ACCESSIBILITY_AUTOANNOUNCE_PERIOD: OsmandPreference<Int> =
		IntPreference(this, "accessibility_autoannounce_period", 10000).makeProfile().cache()

	@JvmField
	val DISABLE_OFFROUTE_RECALC: OsmandPreference<Boolean> =
		BooleanPreference(this, "disable_offroute_recalc", false).makeProfile()

	@JvmField
	val DISABLE_WRONG_DIRECTION_RECALC: OsmandPreference<Boolean> =
		BooleanPreference(this, "disable_wrong_direction_recalc", false).makeProfile()

	@JvmField
	val HAZMAT_TRANSPORTING_ENABLED: OsmandPreference<Boolean> =
		BooleanPreference(this, "hazmat_transporting_enabled", false).makeProfile()

	@JvmField
	val DIRECTION_AUDIO_FEEDBACK: OsmandPreference<Boolean> = BooleanAccessibilityPreference(
		this, "direction_audio_feedback", false
	).makeProfile()

	@JvmField
	val DIRECTION_HAPTIC_FEEDBACK: OsmandPreference<Boolean> = BooleanAccessibilityPreference(
		this, "direction_haptic_feedback", false
	).makeProfile()

	// magnetic field doesn'torkmost of the time on some phones
	@JvmField
	val USE_MAGNETIC_FIELD_SENSOR_COMPASS: OsmandPreference<Boolean> =
		BooleanPreference(this, "use_magnetic_field_sensor_compass", false).makeProfile().cache()
	@JvmField
	val USE_KALMAN_FILTER_FOR_COMPASS: OsmandPreference<Boolean> =
		BooleanPreference(this, "use_kalman_filter_compass", true).makeProfile().cache()
	@JvmField
	val USE_VOLUME_BUTTONS_AS_ZOOM: OsmandPreference<Boolean> =
		BooleanPreference(this, "use_volume_buttons_as_zoom", false).makeProfile().cache()

	@JvmField
	val PRECISE_DISTANCE_NUMBERS: CommonPreference<Boolean> =
		BooleanPreference(this, "precise_distance_numbers", true).makeProfile().cache()

	init {
		PRECISE_DISTANCE_NUMBERS.setModeDefaultValue(ApplicationMode.CAR, false)
	}

	@JvmField
	val DO_NOT_SHOW_STARTUP_MESSAGES: OsmandPreference<Boolean> =
		BooleanPreference(this, "do_not_show_startup_messages", false).makeGlobal().makeShared()
			.cache()
	@JvmField
	val SHOW_DOWNLOAD_MAP_DIALOG: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_download_map_dialog", true).makeGlobal().makeShared().cache()
	@JvmField
	val DO_NOT_USE_ANIMATIONS: OsmandPreference<Boolean> =
		BooleanPreference(this, "do_not_use_animations", false).makeProfile().cache()

	@JvmField
	val SEND_ANONYMOUS_MAP_DOWNLOADS_DATA: OsmandPreference<Boolean> =
		BooleanPreference(this, "send_anonymous_map_downloads_data", false).makeGlobal()
			.makeShared().cache()
	@JvmField
	val SEND_ANONYMOUS_APP_USAGE_DATA: OsmandPreference<Boolean> =
		BooleanPreference(this, "send_anonymous_app_usage_data", false).makeGlobal().makeShared()
			.cache()
	@JvmField
	val SEND_ANONYMOUS_DATA_REQUEST_PROCESSED: OsmandPreference<Boolean> = BooleanPreference(
		this, "send_anonymous_data_request_processed", false
	).makeGlobal().makeShared().cache()
	@JvmField
	val SEND_ANONYMOUS_DATA_REQUESTS_COUNT: OsmandPreference<Int> =
		IntPreference(this, "send_anonymous_data_requests_count", 0).makeGlobal().cache()
	@JvmField
	val SEND_ANONYMOUS_DATA_LAST_REQUEST_NS: OsmandPreference<Int> =
		IntPreference(this, "send_anonymous_data_last_request_ns", -1).makeGlobal().cache()

	@JvmField
	val SEND_UNIQUE_USER_IDENTIFIER: OsmandPreference<Boolean> =
		BooleanPreference(this, "send_unique_user_identifier", true).makeGlobal().cache()

	@JvmField
	val LOCATION_SOURCE: CommonPreference<LocationSource> = EnumStringPreference(
		this,
		"location_source",
		if (Version.isGooglePlayEnabled()) LocationSource.GOOGLE_PLAY_SERVICES else LocationSource.ANDROID_API,
		LocationSource.entries.toTypedArray()
	).makeGlobal().makeShared()

	@JvmField
	val MAP_EMPTY_STATE_ALLOWED: OsmandPreference<Boolean> =
		BooleanPreference(this, "map_empty_state_allowed", false).makeProfile().cache()

	val FIXED_NORTH_MAP: OsmandPreference<Boolean> =
		BooleanPreference(this, "fix_north_map", false).makeProfile().cache()


	@JvmField
	val TEXT_SCALE: CommonPreference<Float> =
		FloatPreference(this, "text_scale", 1f).makeProfile().cache()

	init {
		TEXT_SCALE.setModeDefaultValue(ApplicationMode.CAR, 1.25f)
	}

	@JvmField
	val MAP_DENSITY: CommonPreference<Float> =
		FloatPreference(this, "map_density_n", 1f).makeProfile().cache()

	@JvmField
	val SHOW_POI_LABEL: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_poi_label", false).makeProfile()

	@JvmField
	val ONLINE_PHOTOS_ROW_COLLAPSED: OsmandPreference<Boolean> =
		BooleanPreference(this, "online_photos_menu_collapsed", true).makeGlobal().makeShared()
	@JvmField
	val WEBGL_SUPPORTED: OsmandPreference<Boolean> =
		BooleanPreference(this, "webgl_supported", true).makeGlobal()

	@JvmField
	val PREFERRED_LOCALE: OsmandPreference<String> =
		StringPreference(this, "preferred_locale", "").makeGlobal().makeShared()

	@JvmField
	val MAP_PREFERRED_LOCALE: OsmandPreference<String> =
		StringPreference(this, "map_preferred_locale", "").makeGlobal().makeShared().cache()
	@JvmField
	val MAP_TRANSLITERATE_NAMES: OsmandPreference<Boolean> =
		object : BooleanPreference(this, "map_transliterate_names", false) {
			override fun getDefaultValue(): Boolean {
				return usingEnglishNames()
			}
		}.makeGlobal().makeShared().cache()

	fun usingEnglishNames(): Boolean {
		return MAP_PREFERRED_LOCALE.get() == "en"
	}

	@JvmField
	val INAPPS_READ: OsmandPreference<Boolean> =
		BooleanPreference(this, "inapps_read", false).makeGlobal()

	@JvmField
	val BILLING_USER_ID: OsmandPreference<String> =
		StringPreference(this, "billing_user_id", "").makeGlobal()
	@JvmField
	val BILLING_USER_TOKEN: OsmandPreference<String> =
		StringPreference(this, "billing_user_token", "").makeGlobal()
	val BILLING_USER_NAME: OsmandPreference<String> =
		StringPreference(this, "billing_user_name", "").makeGlobal()
	@JvmField
	val BILLING_USER_EMAIL: OsmandPreference<String> =
		StringPreference(this, "billing_user_email", "").makeGlobal()
	@JvmField
	val BILLING_USER_COUNTRY: OsmandPreference<String> =
		StringPreference(this, "billing_user_country", "").makeGlobal()
	@JvmField
	val BILLING_USER_COUNTRY_DOWNLOAD_NAME: OsmandPreference<String> = StringPreference(
		this,
		"billing_user_country_download_name",
		BILLING_USER_DONATION_NONE_PARAMETER
	).makeGlobal()
	val BILLING_HIDE_USER_NAME: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_hide_user_name", false).makeGlobal()
	val BILLING_PURCHASE_TOKEN_SENT: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_purchase_token_sent", false).makeGlobal()
	@JvmField
	val BILLING_PURCHASE_TOKENS_SENT: OsmandPreference<String> =
		StringPreference(this, "billing_purchase_tokens_sent", "").makeGlobal()
	@JvmField
	val LIVE_UPDATES_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_live_updates_purchased", false).makeGlobal()
	@JvmField
	val LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME: OsmandPreference<Long> = LongPreference(
		this, "live_updates_expired_first_dlg_shown_time", 0
	).makeGlobal()
	@JvmField
	val LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME: OsmandPreference<Long> = LongPreference(
		this, "live_updates_expired_second_dlg_shown_time", 0
	).makeGlobal()
	@JvmField
	val FULL_VERSION_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_full_version_purchased", false).makeGlobal()
	@JvmField
	val DEPTH_CONTOURS_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_sea_depth_purchased", false).makeGlobal()
	@JvmField
	val CONTOUR_LINES_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_srtm_purchased", false).makeGlobal()
	@JvmField
	val EMAIL_SUBSCRIBED: OsmandPreference<Boolean> =
		BooleanPreference(this, "email_subscribed", false).makeGlobal()
	@JvmField
	val OSMAND_PRO_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_osmand_pro_purchased", false).makeGlobal()
	@JvmField
	val OSMAND_MAPS_PURCHASED: OsmandPreference<Boolean> =
		BooleanPreference(this, "billing_osmand_maps_purchased", false).makeGlobal()
	@JvmField
	val MAPPER_LIVE_UPDATES_EXPIRE_TIME: OsmandPreference<Long> =
		LongPreference(this, "mapper_live_updates_expire_time", 0L).makeGlobal()

	@JvmField
	val DISCOUNT_ID: OsmandPreference<Int> = IntPreference(this, "discount_id", 0).makeGlobal()
	@JvmField
	val DISCOUNT_SHOW_NUMBER_OF_STARTS: OsmandPreference<Int> =
		IntPreference(this, "number_of_starts_on_discount_show", 0).makeGlobal()
	@JvmField
	val DISCOUNT_TOTAL_SHOW: OsmandPreference<Int> =
		IntPreference(this, "discount_total_show", 0).makeGlobal()
	@JvmField
	val DISCOUNT_SHOW_DATETIME_MS: OsmandPreference<Long> =
		LongPreference(this, "show_discount_datetime_ms", 0).makeGlobal()

	@JvmField
	val BACKUP_USER_EMAIL: OsmandPreference<String> =
		StringPreference(this, "backup_user_email", "").makeGlobal()
	@JvmField
	val BACKUP_USER_ID: OsmandPreference<String> =
		StringPreference(this, "backup_user_id", "").makeGlobal()
	@JvmField
	val BACKUP_DEVICE_ID: OsmandPreference<String> =
		StringPreference(this, "backup_device_id", "").makeGlobal()
	@JvmField
	val BACKUP_NATIVE_DEVICE_ID: OsmandPreference<String> =
		StringPreference(this, "backup_native_device_id", "").makeGlobal()
	@JvmField
	val BACKUP_ACCESS_TOKEN: OsmandPreference<String> =
		StringPreference(this, "backup_access_token", "").makeGlobal()
	@JvmField
	val BACKUP_ACCESS_TOKEN_UPDATE_TIME: OsmandPreference<String> =
		StringPreference(this, "backup_access_token_update_time", "").makeGlobal()

	@JvmField
	val BACKUP_PROMOCODE: OsmandPreference<String> =
		StringPreference(this, "backup_promocode", "").makeGlobal()
	@JvmField
	val BACKUP_PURCHASE_ACTIVE: OsmandPreference<Boolean> =
		BooleanPreference(this, "backup_promocode_active", false).makeGlobal()
	@JvmField
	val BACKUP_PURCHASE_START_TIME: OsmandPreference<Long> =
		LongPreference(this, "promo_website_start_time", 0L).makeGlobal()
	@JvmField
	val BACKUP_PURCHASE_EXPIRE_TIME: OsmandPreference<Long> =
		LongPreference(this, "promo_website_expire_time", 0L).makeGlobal()
	@JvmField
	val BACKUP_PURCHASE_STATE: CommonPreference<SubscriptionState> = EnumStringPreference(
		this,
		"promo_website_state",
		SubscriptionState.UNDEFINED,
		SubscriptionState.entries.toTypedArray()
	).makeGlobal()
	@JvmField
	val BACKUP_SUBSCRIPTION_ORIGIN: CommonPreference<PurchaseOrigin> = EnumStringPreference(
		this,
		"backup_subscription_origin",
		PurchaseOrigin.UNDEFINED,
		PurchaseOrigin.entries.toTypedArray()
	).makeGlobal()
	@JvmField
	val BACKUP_PURCHASE_PERIOD: OsmandPreference<Period.PeriodUnit?> = EnumStringPreference(
		this, "backup_purchase_period", null, Period.PeriodUnit.entries.toTypedArray()
	).makeGlobal()


	@JvmField
	val FAVORITES_LAST_UPLOADED_TIME: OsmandPreference<Long> =
		LongPreference(this, "favorites_last_uploaded_time", 0L).makeGlobal()
	@JvmField
	val BACKUP_LAST_UPLOADED_TIME: OsmandPreference<Long> =
		LongPreference(this, "backup_last_uploaded_time", 0L).makeGlobal()
	@JvmField
	val BACKUP_LAST_DOWNLOADED_TIME: OsmandPreference<Long> =
		LongPreference(this, "backup_last_downloaded_time", 0L).makeGlobal()
	@JvmField
	val ITINERARY_LAST_CALCULATED_MD5: OsmandPreference<String> =
		StringPreference(this, "itinerary_last_calculated_md5", "").makeGlobal()

	@JvmField
	val AUTO_BACKUP_ENABLED: OsmandPreference<Boolean> =
		BooleanPreference(this, OsmandBackupAgent.AUTO_BACKUP_ENABLED, true).makeGlobal()
			.makeShared()

	@JvmField
	val DAYNIGHT_MODE: CommonPreference<DayNightMode> = EnumStringPreference(
		this,
		"daynight_mode",
		DayNightMode.DAY,
		DayNightMode.entries.toTypedArray()
	)

	init {
		DAYNIGHT_MODE.makeProfile().cache()
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.CAR, DayNightMode.AUTO)
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, DayNightMode.AUTO)
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, DayNightMode.DAY)
	}

	@JvmField
	val AUTO_ZOOM_MAP: CommonPreference<Boolean> =
		BooleanPreference(this, "auto_zoom_map_on_off", false).makeProfile().cache()

	init {
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.CAR, true)
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, false)
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false)
	}

	@JvmField
	val AUTO_ZOOM_MAP_SCALE: CommonPreference<AutoZoomMap> = EnumStringPreference(
		this, "auto_zoom_map_scale", AutoZoomMap.FAR,
		AutoZoomMap.entries.toTypedArray()
	).makeProfile().cache()

	init {
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.CAR, AutoZoomMap.FAR)
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.BICYCLE, AutoZoomMap.CLOSE)
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, AutoZoomMap.CLOSE)
	}

	val DELAY_TO_START_NAVIGATION: CommonPreference<Int> =
		object : IntPreference(this, "delay_to_start_navigation", -1) {
			override fun getDefaultValue(): Int {
				if (DEFAULT_APPLICATION_MODE.get().isDerivedRoutingFrom(ApplicationMode.CAR)) {
					return 10
				}
				return -1
			}
		}.makeGlobal().makeShared().cache()

	@JvmField
	val SNAP_TO_ROAD: CommonPreference<Boolean> =
		BooleanPreference(this, "snap_to_road", false).makeProfile().cache()

	init {
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.CAR, true)
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.BICYCLE, true)
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true)
	}

	@JvmField
	val VIEW_ANGLE_VISIBILITY: CommonPreference<MarkerDisplayOption> = EnumStringPreference(
		this,
		"view_angle_visibility",
		MarkerDisplayOption.RESTING,
		MarkerDisplayOption.entries.toTypedArray()
	).makeProfile().makeShared()
	@JvmField
	val LOCATION_RADIUS_VISIBILITY: CommonPreference<MarkerDisplayOption> = EnumStringPreference(
		this,
		"location_radius_visibility",
		MarkerDisplayOption.RESTING_NAVIGATION,
		MarkerDisplayOption.entries.toTypedArray()
	).makeProfile().makeShared()

	@JvmField
	val INTERRUPT_MUSIC: CommonPreference<Boolean> =
		BooleanPreference(this, "interrupt_music", false).makeProfile()

	@JvmField
	val ENABLE_PROXY: CommonPreference<Boolean> =
		object : BooleanPreference(this, "enable_proxy", false) {
			override fun setValue(prefs: Any, `val`: Boolean): Boolean {
				val valueSaved = super.setValue(prefs, `val`)
				if (valueSaved) {
					NetworkUtils.setProxy(
						if (`val`) PROXY_HOST.get() else null,
						if (`val`) PROXY_PORT.get() else 0
					)
				}
				return valueSaved
			}
		}.makeGlobal().makeShared()

	@JvmField
	val PROXY_HOST: CommonPreference<String?> =
		StringPreference(this, "proxy_host", null).makeGlobal().makeShared()
	@JvmField
	val PROXY_PORT: CommonPreference<Int> =
		IntPreference(this, "proxy_port", 0).makeGlobal().makeShared()

	val isProxyEnabled: Boolean
		get() = PROXY_HOST.get() != null && PROXY_PORT.get() > 0 && ENABLE_PROXY.get()

	@JvmField
	val USER_ANDROID_ID: CommonPreference<String> =
		StringPreference(this, "user_android_id", "").makeGlobal()
	@JvmField
	val USER_ANDROID_ID_EXPIRED_TIME: CommonPreference<Long> =
		LongPreference(this, "user_android_id_expired_time", 0).makeGlobal()


	@JvmField
	val SAVE_GLOBAL_TRACK_TO_GPX: CommonPreference<Boolean> =
		BooleanPreference(this, "save_global_track_to_gpx", false).makeGlobal().cache()
	@JvmField
	val SAVE_GLOBAL_TRACK_INTERVAL: CommonPreference<Int> =
		IntPreference(this, "save_global_track_interval", 5000).makeProfile().cache()
	@JvmField
	val SAVE_GLOBAL_TRACK_REMEMBER: CommonPreference<Boolean> =
		BooleanPreference(this, "save_global_track_remember", false).makeProfile().cache()
	@JvmField
	val SHOW_TRIP_REC_START_DIALOG: CommonPreference<Boolean> =
		BooleanPreference(this, "show_trip_recording_start_dialog", true).makeGlobal().makeShared()
	@JvmField
	val SHOW_BATTERY_OPTIMIZATION_DIALOG: CommonPreference<Boolean> =
		BooleanPreference(this, "show_battery_optimization_dialog", true).makeGlobal().makeShared()
	@JvmField
	val SAVE_TRACK_TO_GPX: CommonPreference<Boolean> =
		BooleanPreference(this, "save_track_to_gpx", false).makeProfile().cache()

	init {
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, false)
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, false)
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false)
	}

	//	public static final Integer DAILY_DIRECTORY = 2;
	@JvmField
	val DISABLE_RECORDING_ONCE_APP_KILLED: CommonPreference<Boolean> =
		BooleanPreference(this, "disable_recording_once_app_killed", false).makeProfile()

	@JvmField
	val TRACK_STORAGE_DIRECTORY: CommonPreference<Int> =
		IntPreference(this, "track_storage_directory", 0).makeProfile()

	@JvmField
	val FAST_ROUTE_MODE: OsmandPreference<Boolean> =
		BooleanPreference(this, "fast_route_mode", true).makeProfile()

	@JvmField
	val ROUTING_TYPE: CommonPreference<RoutingType> = EnumStringPreference(
		this,
		"routing_method",
		RoutingType.HH_CPP,
		RoutingType.entries.toTypedArray()
	).makeProfile().cache()
	@JvmField
	val APPROXIMATION_TYPE: CommonPreference<ApproximationType> = EnumStringPreference(
		this,
		"approximation_method_r49_default",
		ApproximationType.APPROX_GEO_CPP,
		ApproximationType.entries.toTypedArray()
	).makeProfile().cache()

	@JvmField
	val ENABLE_TIME_CONDITIONAL_ROUTING: CommonPreference<Boolean> =
		BooleanPreference(this, "enable_time_conditional_routing", true).makeProfile()

	@JvmField
	var simulateNavigation: Boolean = false
	@JvmField
	var simulateNavigationStartedFromAdb: Boolean = false
	@JvmField
	var simulateNavigationMode: String = SimulationMode.PREVIEW.key
	@JvmField
	var simulateNavigationSpeed: Float = SIM_MIN_SPEED

	@JvmField
	val SHOW_ROUTING_ALARMS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_routing_alarms", true).makeProfile().cache()

	@JvmField
	val SHOW_TRAFFIC_WARNINGS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_traffic_warnings", false).makeProfile().cache()

	init {
		SHOW_TRAFFIC_WARNINGS.setModeDefaultValue(ApplicationMode.CAR, true)
	}

	@JvmField
	val SHOW_SPEEDOMETER: CommonPreference<Boolean> =
		BooleanPreference(this, "show_speedometer", false).makeProfile().cache()

	init {
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.CAR, true)
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.TRUCK, true)
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.MOTORCYCLE, true)
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.MOPED, true)
	}

	@JvmField
	val SPEEDOMETER_SIZE: CommonPreference<WidgetSize> = EnumStringPreference(
		this,
		"speedometer_size",
		WidgetSize.MEDIUM,
		WidgetSize.entries.toTypedArray()
	).makeProfile()

	init {
		SPEEDOMETER_SIZE.setModeDefaultValue(ApplicationMode.CAR, WidgetSize.SMALL)
	}

	@JvmField
	val SHOW_SPEED_LIMIT_WARNINGS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_speed_limit_warnings", false).makeProfile().cache()

	@JvmField
	val SHOW_PEDESTRIAN: CommonPreference<Boolean> =
		BooleanPreference(this, "show_pedestrian", false).makeProfile().cache()

	init {
		SHOW_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true)
	}

	@JvmField
	val SHOW_TUNNELS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_tunnels", false).makeProfile().cache()

	init {
		SHOW_TUNNELS.setModeDefaultValue(ApplicationMode.CAR, true)
	}

	@JvmField
	val SHOW_CAMERAS: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_cameras", false).makeProfile().cache()

	@JvmField
	val SHOW_WPT: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_gpx_wpt", true).makeGlobal().makeShared().cache()
	@JvmField
	val SHOW_NEARBY_FAVORITES: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_nearby_favorites", false).makeProfile().cache()
	@JvmField
	val SHOW_NEARBY_POI: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_nearby_poi", false).makeProfile().cache()

	@JvmField
	val SPEAK_STREET_NAMES: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_street_names", true).makeProfile().cache()
	@JvmField
	val SPEAK_TRAFFIC_WARNINGS: CommonPreference<Boolean> =
		BooleanPreference(this, "speak_traffic_warnings", true).makeProfile().cache()
	@JvmField
	val SPEAK_PEDESTRIAN: CommonPreference<Boolean> =
		BooleanPreference(this, "speak_pedestrian", false).makeProfile().cache()

	init {
		SPEAK_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true)
	}

	@JvmField
	val SPEAK_SPEED_LIMIT: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_speed_limit", false).makeProfile().cache()
	@JvmField
	val SPEAK_SPEED_CAMERA: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_cameras", false).makeProfile().cache()
	@JvmField
	val SPEAK_TUNNELS: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_tunnels", false).makeProfile().cache()
	@JvmField
	val SPEAK_EXIT_NUMBER_NAMES: OsmandPreference<Boolean> =
		BooleanPreference(this, "exit_number_names", true).makeProfile().cache()
	@JvmField
	val SPEAK_ROUTE_RECALCULATION: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_route_recalculation", true).makeProfile().cache()
	@JvmField
	val SPEAK_GPS_SIGNAL_STATUS: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_gps_signal_status", true).makeProfile().cache()
	@JvmField
	val SPEAK_ROUTE_DEVIATION: OsmandPreference<Boolean> =
		BooleanPreference(this, "speak_route_deviation", true).makeProfile().cache()

	@JvmField
	val SPEED_CAMERAS_UNINSTALLED: OsmandPreference<Boolean> =
		BooleanPreference(this, "speed_cameras_uninstalled", false).makeGlobal().makeShared()
	@JvmField
	val SPEED_CAMERAS_ALERT_SHOWED: OsmandPreference<Boolean> =
		BooleanPreference(this, "speed_cameras_alert_showed", false).makeGlobal().makeShared()

	val forbiddenTypes: Set<String>
		get() {
			val typeNames: MutableSet<String> = HashSet()
			if (SPEED_CAMERAS_UNINSTALLED.get()) {
				typeNames.add(MapPoiTypes.SPEED_CAMERA)
			}
			return typeNames
		}

	@JvmField
	val ANNOUNCE_WPT: OsmandPreference<Boolean> =
		object : BooleanPreference(this, "announce_wpt", true) {
			override fun setValue(prefs: Any, `val`: Boolean): Boolean {
				val valueSaved = super.setValue(prefs, `val`)
				if (valueSaved) {
					SHOW_WPT.set(`val`)
				}

				return valueSaved
			}
		}.makeProfile().cache()

	@JvmField
	val ANNOUNCE_NEARBY_FAVORITES: OsmandPreference<Boolean> = object : BooleanPreference(
		this, "announce_nearby_favorites", false
	) {
		override fun setValue(prefs: Any, `val`: Boolean): Boolean {
			val valueSaved = super.setValue(prefs, `val`)
			if (valueSaved) {
				SHOW_NEARBY_FAVORITES.set(`val`)
			}

			return valueSaved
		}
	}.makeProfile().cache()

	@JvmField
	val ANNOUNCE_NEARBY_POI: OsmandPreference<Boolean> =
		object : BooleanPreference(this, "announce_nearby_poi", false) {
			override fun setValue(prefs: Any, `val`: Boolean): Boolean {
				val valueSaved = super.setValue(prefs, `val`)
				if (valueSaved) {
					SHOW_NEARBY_POI.set(`val`)
				}

				return valueSaved
			}
		}.makeProfile().cache()

	@JvmField
	val GPX_ROUTE_CALC_OSMAND_PARTS: OsmandPreference<Boolean> =
		BooleanPreference(this, "gpx_routing_calculate_osmand_route", true).makeGlobal()
			.makeShared().cache()
	val GPX_CALCULATE_RTEPT: OsmandPreference<Boolean> =
		BooleanPreference(this, "gpx_routing_calculate_rtept", true).makeGlobal().makeShared()
			.cache()
	@JvmField
	val GPX_ROUTE_CALC: OsmandPreference<Boolean> =
		BooleanPreference(this, "calc_gpx_route", false).makeGlobal().makeShared().cache()
	@JvmField
	val GPX_SEGMENT_INDEX: OsmandPreference<Int> =
		IntPreference(this, "gpx_route_segment", -1).makeGlobal().makeShared().cache()
	@JvmField
	val GPX_ROUTE_INDEX: OsmandPreference<Int> =
		IntPreference(this, "gpx_route_index", -1).makeGlobal().makeShared().cache()

	val AVOID_TOLL_ROADS: OsmandPreference<Boolean> =
		BooleanPreference(this, "avoid_toll_roads", false).makeProfile().cache()
	val AVOID_MOTORWAY: OsmandPreference<Boolean> =
		BooleanPreference(this, "avoid_motorway", false).makeProfile().cache()
	val AVOID_UNPAVED_ROADS: OsmandPreference<Boolean> =
		BooleanPreference(this, "avoid_unpaved_roads", false).makeProfile().cache()
	val AVOID_FERRIES: OsmandPreference<Boolean> =
		BooleanPreference(this, "avoid_ferries", false).makeProfile().cache()

	val PREFER_MOTORWAYS: OsmandPreference<Boolean> =
		BooleanPreference(this, "prefer_motorways", false).makeProfile().cache()

	val LAST_UPDATES_CARD_REFRESH: OsmandPreference<Long> =
		LongPreference(this, "last_updates_card_refresh", 0).makeGlobal()
	@JvmField
	val CURRENT_TRACK_COLOR: CommonPreference<Int> =
		IntPreference(this, "current_track_color", 0).makeGlobal().makeShared().cache()
	@JvmField
	val CURRENT_TRACK_COLORING_TYPE: CommonPreference<ColoringType> = EnumStringPreference(
		this,
		"current_track_coloring_type", ColoringType.TRACK_SOLID, valuesOf(ColoringPurpose.TRACK)
	).makeGlobal().makeShared().cache()
	@JvmField
	val CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE: CommonPreference<String> = StringPreference(
		this,
		"current_track_route_info_attribute", null
	)
	@JvmField
	val CURRENT_TRACK_WIDTH: CommonPreference<String> =
		StringPreference(this, "current_track_width", "").makeGlobal().makeShared().cache()
	@JvmField
	val CURRENT_TRACK_SHOW_ARROWS: CommonPreference<Boolean> =
		BooleanPreference(this, "current_track_show_arrows", false).makeGlobal().makeShared()
			.cache()
	@JvmField
	val CURRENT_TRACK_SHOW_START_FINISH: CommonPreference<Boolean> =
		BooleanPreference(this, "current_track_show_start_finish", true).makeGlobal().makeShared()
			.cache()
	@JvmField
	val CURRENT_TRACK_3D_VISUALIZATION_TYPE: CommonPreference<String> =
		StringPreference(this, "currentTrackVisualization3dByType", "none").makeGlobal()
			.makeShared().cache()
	@JvmField
	val CURRENT_TRACK_3D_WALL_COLORING_TYPE: CommonPreference<String> =
		StringPreference(this, "currentTrackVisualization3dWallColorType", "none").makeGlobal()
			.makeShared().cache()
	@JvmField
	val CURRENT_TRACK_3D_LINE_POSITION_TYPE: CommonPreference<String> =
		StringPreference(this, "currentTrackVisualization3dPositionType", "none").makeGlobal()
			.makeShared().cache()
	@JvmField
	val CURRENT_TRACK_ADDITIONAL_EXAGGERATION: CommonPreference<Float> =
		FloatPreference(this, "currentTrackVerticalExaggerationScale", 1f).makeGlobal().makeShared()
			.cache()
	@JvmField
	val CURRENT_TRACK_ELEVATION_METERS: CommonPreference<Float> =
		FloatPreference(this, "current_track_elevation_meters", 1000f).makeGlobal().makeShared()
			.cache()
	@JvmField
	val CURRENT_GRADIENT_PALETTE: CommonPreference<String> = StringPreference(
		this,
		"current_track_gradient_palette",
		PaletteGradientColor.DEFAULT_NAME
	).makeGlobal().makeShared().cache()
	@JvmField
	val CURRENT_TRACK_ROUTE_ACTIVITY: CommonPreference<String> =
		StringPreference(this, "current_track_route_activity", "").makeProfile().cache()

	@JvmField
	val GRADIENT_PALETTES: CommonPreference<String> =
		StringPreference(this, "gradient_color_palettes", null).makeGlobal().makeShared()
	@JvmField
	val LAST_USED_FAV_ICONS: ListStringPreference =
		ListStringPreference(this, "last_used_favorite_icons", null, ",").makeShared()
			.makeGlobal() as ListStringPreference

	@JvmField
	val SAVE_TRACK_INTERVAL: CommonPreference<Int> =
		IntPreference(this, "save_track_interval", 5000).makeProfile()

	init {
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.CAR, 3000)
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.BICYCLE, 5000)
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 10000)
	}

	// Please note that SAVE_TRACK_MIN_DISTANCE, SAVE_TRACK_PRECISION, SAVE_TRACK_MIN_SPEED should all be "0" for the default profile, as we have no interface to change them
	@JvmField
	val SAVE_TRACK_MIN_DISTANCE: CommonPreference<Float> =
		FloatPreference(this, "save_track_min_distance", 0f).makeProfile()

	//{
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.CAR, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.BICYCLE, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 5.f);
	//}
	@JvmField
	val SAVE_TRACK_PRECISION: CommonPreference<Float> =
		FloatPreference(this, "save_track_precision", 50.0f).makeProfile()

	//{
	//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.CAR, 50.f);
	//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.BICYCLE, 50.f);
	//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 50.f);
	//}
	@JvmField
	val SAVE_TRACK_MIN_SPEED: CommonPreference<Float> =
		FloatPreference(this, "save_track_min_speed", 0f).makeProfile()

	//{
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.CAR, 2.f);
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.BICYCLE, 1.f);
	//		SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0.f);
	//}
	@JvmField
	val AUTO_SPLIT_RECORDING: CommonPreference<Boolean> =
		BooleanPreference(this, "auto_split_recording", true).makeProfile()

	@JvmField
	val TRIP_RECORDING_X_AXIS: ListStringPreference = ListStringPreference(
		this,
		"trip_recording_x_axis",
		GPXDataSetType.ALTITUDE.name,
		";"
	).makeShared().makeGlobal() as ListStringPreference

	@JvmField
	val TRIP_RECORDING_Y_AXIS: CommonPreference<GPXDataSetAxisType> = EnumStringPreference(
		this,
		"trip_recording_Y_axis",
		GPXDataSetAxisType.DISTANCE,
		GPXDataSetAxisType.entries.toTypedArray()
	)

	@JvmField
	val SHOW_TRIP_REC_NOTIFICATION: CommonPreference<Boolean> =
		BooleanPreference(this, "show_trip_recording_notification", true).makeProfile()

	@JvmField
	val LIVE_MONITORING: CommonPreference<Boolean> =
		BooleanPreference(this, "live_monitoring", false).makeProfile()

	@JvmField
	val LIVE_MONITORING_INTERVAL: CommonPreference<Int> =
		IntPreference(this, "live_monitoring_interval", 5000).makeProfile()

	@JvmField
	val LIVE_MONITORING_MAX_INTERVAL_TO_SEND: CommonPreference<Int> =
		IntPreference(this, "live_monitoring_maximum_interval_to_send", 900000).makeProfile()

	@JvmField
	val LIVE_MONITORING_URL: CommonPreference<String> = StringPreference(
		this, "live_monitoring_url",
		"https://example.com?lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}"
	).makeProfile()

	@JvmField
	val GPS_STATUS_APP: CommonPreference<String> =
		StringPreference(this, "gps_status_app", "").makeGlobal().makeShared()

	@JvmField
	val MAP_INFO_CONTROLS: CommonPreference<String> =
		StringPreference(this, "map_info_controls", "").makeProfile()

	init {
		for (mode in ApplicationMode.allPossibleValues()) {
			MAP_INFO_CONTROLS.setModeDefaultValue(mode, "")
		}
	}

	@JvmField
	val BATTERY_SAVING_MODE: OsmandPreference<Boolean> =
		BooleanPreference(this, "battery_saving", false).makeGlobal().makeShared()

	@JvmField
	val DEBUG_RENDERING_INFO: OsmandPreference<Boolean> =
		BooleanPreference(this, "debug_rendering", false).makeGlobal().makeShared()

	@JvmField
	val DISABLE_MAP_LAYERS: OsmandPreference<Boolean> =
		BooleanPreference(this, "disable_map_layers", false).makeGlobal().makeShared()

	@JvmField
	val SHOW_FAVORITES: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_favorites", true).makeProfile().cache()

	val SHOW_ZOOM_BUTTONS_NAVIGATION: CommonPreference<Boolean> =
		BooleanPreference(this, "show_zoom_buttons_navigation", false).makeProfile().cache()

	init {
		SHOW_ZOOM_BUTTONS_NAVIGATION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true)
	}

	// Json
	@JvmField
	val SELECTED_GPX: OsmandPreference<String> =
		StringPreference(this, "selected_gpx", "").makeGlobal().makeShared()

	@JvmField
	val MAP_SCREEN_ORIENTATION: OsmandPreference<Int> = IntPreference(
		this,
		"map_screen_orientation",
		-1 /*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/
	).makeProfile()

	//	public final CommonPreference<Boolean> SHOW_VIEW_ANGLE = new BooleanPreference("show_view_angle", false).makeProfile().cache();
	//	{
	//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.CAR, false);
	//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.BICYCLE, true);
	//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	//	}
	// seconds to auto_follow
	@JvmField
	val AUTO_FOLLOW_ROUTE: CommonPreference<Int> =
		IntPreference(this, "auto_follow_route", 0).makeProfile()

	init {
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.CAR, 15)
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.BICYCLE, 15)
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0)
	}

	// seconds to auto_follow
	@JvmField
	val KEEP_INFORMING: CommonPreference<Int> =
		IntPreference(this, "keep_informing", 0).makeProfile()

	init {
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.CAR, 0)
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.BICYCLE, 0)
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0)
	}

	@JvmField
	val USE_SYSTEM_SCREEN_TIMEOUT: CommonPreference<Boolean> =
		BooleanPreference(this, "use_system_screen_timeout", false).makeProfile()

	@JvmField
	val TURN_SCREEN_ON_TIME_INT: CommonPreference<Int> =
		IntPreference(this, "turn_screen_on_time_int", 0).makeProfile()

	init {
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.CAR, 0)
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.BICYCLE, 0)
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0)
	}

	@JvmField
	val TURN_SCREEN_ON_SENSOR: CommonPreference<Boolean> =
		BooleanPreference(this, "turn_screen_on_sensor", false).makeProfile()

	init {
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.CAR, false)
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.BICYCLE, false)
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false)
	}

	@JvmField
	val TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS: CommonPreference<Boolean> = BooleanPreference(
		this, "turn_screen_on_navigation_instructions", false
	).makeProfile()

	@JvmField
	val TURN_SCREEN_ON_POWER_BUTTON: CommonPreference<Boolean> =
		BooleanPreference(this, "turn_screen_on_power_button", false).makeProfile()

	@JvmField
	val ROTATE_MAP: CommonPreference<Int> =
		IntPreference(this, "rotate_map", ROTATE_MAP_MANUAL).makeProfile().cache()

	init {
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.CAR, ROTATE_MAP_BEARING)
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, ROTATE_MAP_BEARING)
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, ROTATE_MAP_BEARING)
	}

	fun isCompassMode(compassMode: CompassMode): Boolean {
		return ROTATE_MAP.get() == compassMode.value
	}

	var compassMode: CompassMode
		get() = getCompassMode(applicationMode)
		set(compassMode) {
			ROTATE_MAP.set(compassMode.value)
		}

	fun getCompassMode(appMode: ApplicationMode): CompassMode {
		return CompassMode.getByValue(ROTATE_MAP.getModeValue(appMode))
	}

	fun setCompassMode(compassMode: CompassMode, appMode: ApplicationMode) {
		ROTATE_MAP.setModeValue(appMode, compassMode.value)
	}

	@JvmField
	val POSITION_PLACEMENT_ON_MAP: CommonPreference<Int> =
		object : IntPreference(this, "position_placement_on_map", 0) {
			override fun getProfileDefaultValue(mode: ApplicationMode): Int {
				// By default display position shifts to the bottom part of the screen
				// only if the "Map orientation" was set to "Movement direction".
				return 0
			}
		}.makeProfile()

	@JvmField
	val LAST_MAP_ACTIVITY_PAUSED_TIME: CommonPreference<Long> =
		LongPreference(this, "last_map_activity_paused_time", 0).makeGlobal().cache()
	@JvmField
	val MAP_LINKED_TO_LOCATION: CommonPreference<Boolean> =
		BooleanPreference(this, "map_linked_to_location", true).makeGlobal().cache()

	val MAX_LEVEL_TO_DOWNLOAD_TILE: OsmandPreference<Int> =
		IntPreference(this, "max_level_download_tile", 20).makeProfile().cache()

	@JvmField
	val LEVEL_TO_SWITCH_VECTOR_RASTER: OsmandPreference<Int> =
		IntPreference(this, "level_to_switch_vector_raster", 1).makeGlobal().cache()

	@JvmField
	val AUDIO_MANAGER_STREAM: OsmandPreference<Int> =
		IntPreference(this, "audio_stream", 3 /*AudioManager.STREAM_MUSIC*/).makeProfile()

	// Corresponding USAGE value for AudioAttributes
	@JvmField
	val AUDIO_USAGE: Array<OsmandPreference<Int>?> = arrayOfNulls<IntPreference>(10)

	init {
		AUDIO_USAGE[0] = IntPreference(this, "audio_usage_0", 2).makeGlobal().makeShared()
			.cache() /*AudioManager.STREAM_VOICE_CALL -> AudioAttributes.USAGE_VOICE_COMMUNICATION*/
		AUDIO_USAGE[3] = IntPreference(this, "audio_usage_3", 12).makeGlobal().makeShared()
			.cache() /*AudioManager.STREAM_MUSIC -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE*/
		AUDIO_USAGE[5] = IntPreference(this, "audio_usage_5", 5).makeGlobal().makeShared()
			.cache() /*AudioManager.STREAM_NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION*/
	}

	// For now this can be changed only in TestVoiceActivity
	@JvmField
	val VOICE_PROMPT_DELAY: Array<OsmandPreference<Int>?> = arrayOfNulls<IntPreference>(10)

	init {
		// 1500 ms delay works for most configurations to establish a BT SCO link
		VOICE_PROMPT_DELAY[0] =
			IntPreference(this, "voice_prompt_delay_0", 1500).makeGlobal().makeShared()
				.cache() /*AudioManager.STREAM_VOICE_CALL*/
		// On most devices sound output works pomptly so usually no voice prompt delay needed
		VOICE_PROMPT_DELAY[3] =
			IntPreference(this, "voice_prompt_delay_3", 0).makeGlobal().makeShared()
				.cache() /*AudioManager.STREAM_MUSIC*/
		VOICE_PROMPT_DELAY[5] =
			IntPreference(this, "voice_prompt_delay_5", 0).makeGlobal().makeShared()
				.cache() /*AudioManager.STREAM_NOTIFICATION*/
	}

	@JvmField
	val DISPLAY_TTS_UTTERANCE: OsmandPreference<Boolean> =
		BooleanPreference(this, "display_tts_utterance", false).makeGlobal().makeShared()

	@JvmField
	val MAP_ONLINE_DATA: CommonPreference<Boolean> =
		BooleanPreference(this, "map_online_data", false).makeProfile()

	@JvmField
	val MAP_OVERLAY: CommonPreference<String> =
		StringPreference(this, "map_overlay", null).makeProfile().cache()

	@JvmField
	val MAP_UNDERLAY: CommonPreference<String?> =
		StringPreference(this, "map_underlay", null).makeProfile().cache()

	@JvmField
	val MAP_OVERLAY_TRANSPARENCY: CommonPreference<Int> =
		IntPreference(this, "overlay_transparency", 100).makeProfile().cache()

	@JvmField
	val MAP_TRANSPARENCY: CommonPreference<Int> =
		IntPreference(this, "map_transparency", 255).makeProfile().cache()

	@JvmField
	val SHOW_MAP_LAYER_PARAMETER: CommonPreference<Boolean> =
		BooleanPreference(this, "show_map_layer_parameter", false).makeProfile().cache()

	@JvmField
	val KEEP_MAP_LABELS_VISIBLE: CommonPreference<Boolean> =
		BooleanPreference(this, "keep_map_labels_visible", false).makeProfile().cache()

	@JvmField
	val SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON: CommonPreference<Boolean> =
		BooleanPreference(this, "show_polygons_when_underlay_is_on", false).makeProfile().cache()

	fun shouldHidePolygons(groundPolygons: Boolean): Boolean {
		val attrName =
			if (groundPolygons) OsmandRasterMapsPlugin.NO_POLYGONS_ATTR else OsmandRasterMapsPlugin.HIDE_WATER_POLYGONS_ATTR
		val hidePreference = getCustomRenderBooleanProperty(attrName)
		return hidePreference.get() || (MAP_UNDERLAY.get() != null && !SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON.get())
	}

	@JvmField
	val MAP_TILE_SOURCES: CommonPreference<String> = StringPreference(
		this, "map_tile_sources",
		TileSourceManager.getMapnikSource().name
	).makeProfile()

	@JvmField
	val LAYER_TRANSPARENCY_SEEKBAR_MODE: CommonPreference<LayerTransparencySeekbarMode> =
		EnumStringPreference(
			this,
			"layer_transparency_seekbar_mode",
			LayerTransparencySeekbarMode.UNDEFINED,
			LayerTransparencySeekbarMode.entries.toTypedArray()
		)

	@JvmField
	val MAP_OVERLAY_PREVIOUS: CommonPreference<String> =
		StringPreference(this, "map_overlay_previous", null).makeGlobal().cache()

	@JvmField
	val MAP_UNDERLAY_PREVIOUS: CommonPreference<String> =
		StringPreference(this, "map_underlay_previous", null).makeGlobal().cache()

	@JvmField
	var PREVIOUS_INSTALLED_VERSION: CommonPreference<String> =
		StringPreference(this, "previous_installed_version", "").makeGlobal()

	@JvmField
	val SHOULD_SHOW_FREE_VERSION_BANNER: OsmandPreference<Boolean> =
		BooleanPreference(this, "should_show_free_version_banner", false).makeGlobal().makeShared()
			.cache()

	@JvmField
	val USE_DISCRETE_AUTO_ZOOM: OsmandPreference<Boolean> =
		BooleanPreference(this, "use_v1_auto_zoom", false).makeGlobal().makeShared().cache()
	@JvmField
	val TRANSPARENT_STATUS_BAR: OsmandPreference<Boolean> =
		BooleanPreference(this, "transparent_status_bar", true).makeGlobal().makeShared()

	@JvmField
	val SHOW_INFO_ABOUT_PRESSED_KEY: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_info_about_pressed_key", false).makeGlobal().makeShared()

	@JvmField
	val TOP_WIDGET_PANEL_ORDER: ListStringPreference = object : ListStringPreference(
		this,
		"top_widget_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.TOP.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	) {
		override fun getModeValue(mode: ApplicationMode): String {
			val value = super.getModeValue(mode)
			if (!Algorithms.isEmpty(value)) {
				return getPagedWidgetIds(
					Arrays.asList(*value.split(
						delimiter.toRegex()
					).dropLastWhile { it.isEmpty() }.toTypedArray()
					)
				)
			}
			return value
		}
	}.makeProfile() as ListStringPreference

	@JvmField
	val BOTTOM_WIDGET_PANEL_ORDER: ListStringPreference = object : ListStringPreference(
		this,
		"bottom_widget_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.BOTTOM.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	) {
		override fun getModeValue(mode: ApplicationMode): String {
			val value = super.getModeValue(mode)
			if (!Algorithms.isEmpty(value)) {
				return getPagedWidgetIds(
					Arrays.asList(*value.split(
						delimiter.toRegex()
					).dropLastWhile { it.isEmpty() }.toTypedArray()
					)
				)
			}
			return value
		}
	}.makeProfile() as ListStringPreference

	@JvmField
	val LEFT_WIDGET_PANEL_ORDER: ListStringPreference = ListStringPreference(
		this,
		"left_widget_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.LEFT.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	).makeProfile() as ListStringPreference

	@JvmField
	@Deprecated("")
	val WIDGET_TOP_PANEL_ORDER: ListStringPreference = object : ListStringPreference(
		this,
		"widget_top_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.TOP.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	) {
		@Throws(JSONException::class)
		override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
			if (appMode != null) {
				val value = json.getString(id)
				TOP_WIDGET_PANEL_ORDER.setModeValue(appMode, parseString(value))
				AppVersionUpgradeOnInit.updateExistingWidgetIds(
					this@OsmandSettings,
					appMode,
					TOP_WIDGET_PANEL_ORDER,
					LEFT_WIDGET_PANEL_ORDER
				)
				AppVersionUpgradeOnInit.updateExistingWidgetIds(
					this@OsmandSettings,
					appMode,
					TOP_WIDGET_PANEL_ORDER,
					RIGHT_WIDGET_PANEL_ORDER
				)
			}
		}

		override fun writeToJson(json: JSONObject, appMode: ApplicationMode): Boolean {
			return false
		}
	}.makeProfile() as ListStringPreference

	@JvmField
	@Deprecated("")
	val WIDGET_BOTTOM_PANEL_ORDER: ListStringPreference = object : ListStringPreference(
		this,
		"widget_bottom_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.BOTTOM.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	) {
		@Throws(JSONException::class)
		override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
			if (appMode != null) {
				val value = json.getString(id)
				BOTTOM_WIDGET_PANEL_ORDER.setModeValue(appMode, parseString(value))
				AppVersionUpgradeOnInit.updateExistingWidgetIds(
					this@OsmandSettings,
					appMode,
					BOTTOM_WIDGET_PANEL_ORDER,
					LEFT_WIDGET_PANEL_ORDER
				)
				AppVersionUpgradeOnInit.updateExistingWidgetIds(
					this@OsmandSettings,
					appMode,
					BOTTOM_WIDGET_PANEL_ORDER,
					RIGHT_WIDGET_PANEL_ORDER
				)
			}
		}

		override fun writeToJson(json: JSONObject, appMode: ApplicationMode): Boolean {
			return false
		}
	}.makeProfile() as ListStringPreference

	@JvmField
	val RIGHT_WIDGET_PANEL_ORDER: ListStringPreference = ListStringPreference(
		this,
		"right_widget_panel_order",
		TextUtils.join(WidgetsPanel.WIDGET_SEPARATOR, WidgetsPanel.RIGHT.originalOrder),
		WidgetsPanel.PAGE_SEPARATOR
	).makeProfile() as ListStringPreference

	private fun getPagedWidgetIds(pages: List<String>): String {
		val builder = StringBuilder()

		val iterator = pages.iterator()
		while (iterator.hasNext()) {
			var pageSeparatorAdded = false
			val page = iterator.next()
			for (id in page.split(WidgetsPanel.WIDGET_SEPARATOR.toRegex())
				.dropLastWhile { it.isEmpty() }
				.toTypedArray()) {
				if (WidgetType.isComplexWidget(id)) {
					pageSeparatorAdded = true
					builder.append(id).append(WidgetsPanel.PAGE_SEPARATOR)
				} else {
					pageSeparatorAdded = false
					builder.append(id).append(WidgetsPanel.WIDGET_SEPARATOR)
				}
			}
			if (iterator.hasNext() && !pageSeparatorAdded) {
				builder.append(WidgetsPanel.PAGE_SEPARATOR)
			}
		}
		return builder.toString()
	}

	@JvmField
	val CUSTOM_WIDGETS_KEYS: ListStringPreference = ListStringPreference(
		this,
		"custom_widgets_keys",
		null,
		WidgetsPanel.WIDGET_SEPARATOR
	).makeProfile() as ListStringPreference

	@JvmField
	val DISPLAYED_MARKERS_WIDGETS_COUNT: OsmandPreference<Int> =
		IntPreference(this, "displayed_markers_widgets_count", 1).makeProfile()

	@JvmField
	val SHOW_MAP_MARKERS: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_map_markers", true).makeProfile()

	@JvmField
	val SEARCH_TRACKS_SORT_MODE: CommonPreference<TracksSortMode> = EnumStringPreference(
		this,
		"search_tracks_sort_mode",
		TracksSortMode.getDefaultSortMode(),
		TracksSortMode.entries.toTypedArray()
	)
	val TRACKS_TABS_SORT_MODES: ListStringPreference =
		ListStringPreference(this, "tracks_tabs_sort_modes", null, ";;").makeGlobal().makeShared()
			.cache() as ListStringPreference

	val trackSortModes: Map<String, String>
		get() = getTrackSortModes(TRACKS_TABS_SORT_MODES.stringsList)

	fun saveTabsSortModes(tabsSortModes: Map<String, String>) {
		val sortModes = getPlainSortModes(tabsSortModes)
		TRACKS_TABS_SORT_MODES.stringsList = sortModes
	}

	private fun getTrackSortModes(modes: List<String?>?): Map<String, String> {
		val sortModes: MutableMap<String, String> = HashMap()
		if (!Algorithms.isEmpty(modes)) {
			for (sortMode in modes!!) {
				val tabSortMode =
					sortMode!!.split(",,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				if (tabSortMode.size == 2) {
					sortModes[tabSortMode[0]] = tabSortMode[1]
				}
			}
		}
		return sortModes
	}

	private fun getPlainSortModes(tabsSortModes: Map<String, String>): List<String> {
		val sortTypes: MutableList<String> = ArrayList()
		for ((key, value) in tabsSortModes) {
			sortTypes.add("$key,,$value")
		}
		return sortTypes
	}

	@JvmField
	val ANIMATE_MY_LOCATION: OsmandPreference<Boolean> =
		BooleanPreference(this, "animate_my_location", true).makeProfile().cache()

	@JvmField
	val EXTERNAL_INPUT_DEVICE: OsmandPreference<String> = StringPreference(
		this,
		"selected_external_input_device",
		KeyboardDeviceProfile.ID
	).makeProfile()
	@JvmField
	val CUSTOM_EXTERNAL_INPUT_DEVICES: CommonPreference<String> =
		StringPreference(this, "custom_external_input_devices", "").makeProfile()
	@JvmField
	val EXTERNAL_INPUT_DEVICE_ENABLED: OsmandPreference<Boolean> =
		BooleanPreference(this, "external_input_device_enabled", true).makeProfile()

	@JvmField
	val ROUTE_MAP_MARKERS_START_MY_LOC: OsmandPreference<Boolean> =
		BooleanPreference(this, "route_map_markers_start_my_loc", false).makeGlobal().makeShared()
			.cache()
	@JvmField
	val ROUTE_MAP_MARKERS_ROUND_TRIP: OsmandPreference<Boolean> =
		BooleanPreference(this, "route_map_markers_round_trip", false).makeGlobal().makeShared()
			.cache()

	@JvmField
	val SEARCH_HISTORY: OsmandPreference<Boolean> =
		BooleanPreference(this, "search_history", true).makeGlobal().makeShared()
	@JvmField
	val NAVIGATION_HISTORY: OsmandPreference<Boolean> =
		BooleanPreference(this, "navigation_history", true).makeGlobal().makeShared()
	@JvmField
	val MAP_MARKERS_HISTORY: OsmandPreference<Boolean> =
		BooleanPreference(this, "map_markers_history", true).makeGlobal().makeShared()

	fun getMapTileSource(warnWhenSelected: Boolean): ITileSource {
		val tileSource = getLayerTileSource(MAP_TILE_SOURCES, warnWhenSelected)
		return tileSource ?: TileSourceManager.getMapnikSource()
	}

	fun getLayerTileSource(
		layerSetting: CommonPreference<String>,
		warnWhenSelected: Boolean
	): ITileSource? {
		val tileName = layerSetting.get()
		if (tileName != null) {
			val tileSource = getTileSourceByName(tileName, warnWhenSelected)
			if (tileSource != null) {
				return tileSource
			}
		}
		return null
	}

	private fun checkAmongAvailableTileSources(
		dir: File,
		list: List<TileSourceManager.TileSourceTemplate>?
	): TileSourceManager.TileSourceTemplate? {
		if (list != null) {
			for (l in list) {
				if (dir.name == l.name) {
					try {
						dir.mkdirs()
						TileSourceManager.createMetaInfoFile(dir, l, true)
					} catch (e: IOException) {
					}
					return l
				}
			}
		}
		return null
	}

	val selectedMapSourceTitle: String
		get() = if (MAP_ONLINE_DATA.get()) getTileSourceTitle(MAP_TILE_SOURCES.get()) else context.getString(
			R.string.vector_data
		)

	fun getTileSourceTitle(fileName: String): String {
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			val tileSource = getTileSourceByName(fileName, false)
			return getTileSourceTitle(tileSource, fileName)
		}
		return fileName
	}

	fun getTileSourceTitle(tileSource: ITileSource?, fileName: String): String {
		if (tileSource is SQLiteTileSource) {
			return tileSource.title!!
		}
		return fileName.replace(IndexConstants.SQLITE_EXT, "")
	}

	fun getTileSourceByName(tileName: String?, warnWhenSelected: Boolean): ITileSource? {
		if (tileName == null || tileName.length == 0) {
			return null
		}
		val knownTemplates = TileSourceManager.getKnownSourceTemplates()
		val tPath = context.getAppPath(IndexConstants.TILES_INDEX_DIR)
		val dir = File(tPath, tileName)
		if (!dir.exists()) {
			return checkAmongAvailableTileSources(dir, knownTemplates)
		} else if (tileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return SQLiteTileSource(context, dir, knownTemplates)
		} else if (dir.isDirectory && !dir.name.startsWith(".")) {
			var t = TileSourceManager.createTileSourceTemplate(dir)
			if (warnWhenSelected && !t.isRuleAcceptable) {
				context.showToastMessage(R.string.warning_tile_layer_not_downloadable, dir.name)
			}
			if (!TileSourceManager.isTileSourceMetaInfoExist(dir)) {
				val ret = checkAmongAvailableTileSources(dir, knownTemplates)
				if (ret != null) {
					t = ret
				}
			}
			return t
		}
		return null
	}

	fun installTileSource(toInstall: TileSourceManager.TileSourceTemplate): Boolean {
		val tPath = context.getAppPath(IndexConstants.TILES_INDEX_DIR)
		val dir = File(tPath, toInstall.name)
		dir.mkdirs()
		if (dir.exists() && dir.isDirectory) {
			try {
				TileSourceManager.createMetaInfoFile(dir, toInstall, true)
			} catch (e: IOException) {
				return false
			}
		}
		return true
	}

	val tileSourceEntries: Map<String, String>
		get() = getTileSourceEntries(true)

	fun getTileSourceEntries(sqlite: Boolean): Map<String, String> {
		val map: MutableMap<String, String> = LinkedHashMap()
		val dir = context.getAppPath(IndexConstants.TILES_INDEX_DIR)
		if (dir != null && dir.canRead()) {
			val files = dir.listFiles()
			if (files != null) {
				Arrays.sort(files) { f1: File, f2: File ->
					if (f1.lastModified() > f2.lastModified()) {
						return@sort -1
					} else if (f1.lastModified() == f2.lastModified()) {
						return@sort 0
					}
					1
				}
				for (f in files) {
					val fileName = f.name
					if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
						if (sqlite) {
							map[fileName] = getTileSourceTitle(fileName)
						}
					} else if (f.isDirectory && fileName != IndexConstants.TEMP_SOURCE_TO_LOAD
						&& !fileName.startsWith(".")
					) {
						map[fileName] = fileName
					}
				}
			}
		}
		for (l in TileSourceManager.getKnownSourceTemplates()) {
			if (!l.isHidden) {
				map[l.name] = l.name
			} else {
				map.remove(l.name)
			}
		}
		return map
	}

	@JvmField
	val SHARED_STORAGE_MIGRATION_FINISHED: OsmandPreference<Boolean> = BooleanPreference(
		this,
		"shared_storage_migration_finished", false
	).makeGlobal()

	@JvmField
	val OSMAND_USAGE_SPACE: OsmandPreference<Long> =
		LongPreference(this, "osmand_usage_space", 0).makeGlobal()

	fun freezeExternalStorageDirectory() {
		val type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1)
		if (type == -1) {
			val vh = ValueHolder<Int>()
			val f = getExternalStorageDirectoryV19(vh)
			setExternalStorageDirectoryV19(vh.value, f!!.absolutePath)
		}
	}

	fun initExternalStorageDirectory() {
		val externalStorage = external1AppPath
		if (externalStorage != null && FileUtils.isWritable(externalStorage)) {
			setExternalStorageDirectoryV19(
				EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE,
				external1AppPath!!.absolutePath
			)
		} else {
			setExternalStorageDirectoryV19(
				EXTERNAL_STORAGE_TYPE_INTERNAL_FILE,
				internalAppPath.absolutePath
			)
		}
	}

	val externalStorageDirectory: File?
		get() = getExternalStorageDirectory(null)

	fun getExternalStorageDirectory(type: ValueHolder<Int>?): File? {
		return getExternalStorageDirectoryV19(type)
	}

	val internalAppPath: File
		get() {
			val fl = noBackupPath
			if (fl != null) {
				return fl
			}
			return context.filesDir
		}

	val external1AppPath: File?
		get() {
			val externals = context.getExternalFilesDirs(null)
			return if (externals != null && externals.size > 0) {
				externals[0]
			} else {
				null
			}
		}

	val noBackupPath: File
		get() = context.noBackupFilesDir

	fun getExternalStorageDirectoryV19(tp: ValueHolder<Int>?): File? {
		val type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1)
		var location = defaultLocationV19
		if (type == -1) {
			if (FileUtils.isWritable(location)) {
				if (tp != null) {
					tp.value = if (settingsAPI.contains(
							globalPreferences,
							EXTERNAL_STORAGE_DIR_V19
						)
					) EXTERNAL_STORAGE_TYPE_SPECIFIED else EXTERNAL_STORAGE_TYPE_DEFAULT
				}
				return location
			}
			val external = context.getExternalFilesDirs(null)
			if (external != null && external.size > 0 && external[0] != null) {
				location = external[0]
				if (tp != null) {
					tp.value = EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE
				}
			} else {
				val obbDirs = context.obbDirs
				if (obbDirs != null && obbDirs.size > 0 && obbDirs[0] != null) {
					location = obbDirs[0]
					if (tp != null) {
						tp.value = EXTERNAL_STORAGE_TYPE_OBB
					}
				} else {
					location = internalAppPath
					if (tp != null) {
						tp.value = EXTERNAL_STORAGE_TYPE_INTERNAL_FILE
					}
				}
			}
		}
		return location
	}

	val defaultLocationV19: File
		get() {
			val location = settingsAPI.getString(
				globalPreferences, EXTERNAL_STORAGE_DIR_V19,
				externalStorageDirectoryPre19.absolutePath
			)
			return File(location)
		}

	val isExternalStorageDirectoryTypeSpecifiedV19: Boolean
		get() = settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19)

	val externalStorageDirectoryTypeV19: Int
		get() = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1)

	val isExternalStorageDirectorySpecifiedV19: Boolean
		get() = settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_V19)

	val externalStorageDirectoryV19: String
		get() = settingsAPI.getString(globalPreferences, EXTERNAL_STORAGE_DIR_V19, null)

	val externalStorageDirectoryPre19: File
		get() {
			val defaultLocation = Environment.getExternalStorageDirectory().absolutePath
			val rootFolder = File(
				settingsAPI.getString(
					globalPreferences, EXTERNAL_STORAGE_DIR,
					defaultLocation
				)
			)
			return File(rootFolder, IndexConstants.APP_DIR)
		}

	val defaultInternalStorage: File
		get() = File(Environment.getExternalStorageDirectory(), IndexConstants.APP_DIR)

	fun setExternalStorageDirectoryV19(type: Int, externalStorageDir: String?): Boolean {
		return settingsAPI.edit(globalPreferences).putInt
		EXTERNAL_STORAGE_DIR_TYPE_V19, type).putString
		EXTERNAL_STORAGE_DIR_V19, externalStorageDir).commit()
	}

	@get:SuppressLint("NewApi")
	val secondaryStorage: File?
		get() {
			val externals = context.getExternalFilesDirs(null)
			for (file in externals) {
				if (file != null && !file.absolutePath.contains("emulated")) {
					return file
				}
			}
			return null
		}

	fun setExternalStorageDirectory(type: Int, directory: String?) {
		setExternalStorageDirectoryV19(type, directory)
	}

	val isExternalStorageDirectorySpecifiedPre19: Boolean
		get() = settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR)

	fun setExternalStorageDirectoryPre19(externalStorageDir: String?): Boolean {
		return settingsAPI.edit(globalPreferences)
			.putString(EXTERNAL_STORAGE_DIR, externalStorageDir).commit()
	}


	var lastKnownMapLocation: LatLon
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LAT, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LON, 0f)
			return LatLon(lat.toDouble(), lon.toDouble())
		}
		// Do not use that method if you want to show point on map. Use setMapLocationToShow
		set(mapLocation) {
			val edit = settingsAPI.edit(globalPreferences)
			edit.putFloat(LAST_KNOWN_MAP_LAT, mapLocation.latitude.toFloat())
			edit.putFloat(LAST_KNOWN_MAP_LON, mapLocation.longitude.toFloat())
			edit.commit()
		}

	fun isLastKnownMapLocation(): Boolean {
		return settingsAPI.contains(globalPreferences, LAST_KNOWN_MAP_LAT)
	}

	val andClearMapLocationToShow: LatLon?
		get() {
			if (!settingsAPI.contains(globalPreferences, MAP_LAT_TO_SHOW)) {
				return null
			}
			val lat = settingsAPI.getFloat(globalPreferences, MAP_LAT_TO_SHOW, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, MAP_LON_TO_SHOW, 0f)
			settingsAPI.edit(globalPreferences).remove(MAP_LAT_TO_SHOW).commit()
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	fun getAndClearMapLabelToShow(l: LatLon?): PointDescription? {
		val label = settingsAPI.getString(globalPreferences, MAP_LABEL_TO_SHOW, null)
		settingsAPI.edit(globalPreferences).remove(MAP_LABEL_TO_SHOW).commit()
		return if (label != null) {
			PointDescription.deserializeFromString(label, l)
		} else {
			null
		}
	}

	fun setSearchRequestToShow(request: String?) {
		this.searchRequestToShow = request
	}

	val andClearSearchRequestToShow: String?
		get() {
			val searchRequestToShow = this.searchRequestToShow
			this.searchRequestToShow = null
			return searchRequestToShow
		}

	val andClearObjectToShow: Any?
		get() {
			val objectToShow = this.objectToShow
			this.objectToShow = null
			return objectToShow
		}

	val andClearEditObjectToShow: Boolean
		get() {
			val res = this.editObjectToShow
			this.editObjectToShow = false
			return res
		}

	fun hasMapZoomToShow(): Boolean {
		return settingsAPI.contains(globalPreferences, MAP_ZOOM_TO_SHOW)
	}

	val mapZoomToShow: Int
		get() = settingsAPI.getInt(globalPreferences, MAP_ZOOM_TO_SHOW, 5)

	fun setMapLocationToShow(
		latitude: Double, longitude: Double, zoom: Int, pointDescription: PointDescription?,
		addToHistory: Boolean, toShow: Any?
	) {
		val edit = settingsAPI.edit(globalPreferences)
		edit.putFloat(MAP_LAT_TO_SHOW, latitude.toFloat())
		edit.putFloat(MAP_LON_TO_SHOW, longitude.toFloat())
		if (pointDescription != null) {
			edit.putString(MAP_LABEL_TO_SHOW, PointDescription.serializeToString(pointDescription))
		} else {
			edit.remove(MAP_LABEL_TO_SHOW)
		}
		edit.putInt(MAP_ZOOM_TO_SHOW, zoom)
		edit.commit()
		objectToShow = toShow
		if (addToHistory && pointDescription != null) {
			SearchHistoryHelper.getInstance(context)
				.addNewItemToHistory(latitude, longitude, pointDescription, HistorySource.SEARCH)
		}
	}

	fun setEditObjectToShow() {
		this.editObjectToShow = true
	}

	fun setMapLocationToShow(latitude: Double, longitude: Double, zoom: Int) {
		setMapLocationToShow(latitude, longitude, zoom, null, false, null)
	}

	fun setMapLocationToShow(
		latitude: Double,
		longitude: Double,
		zoom: Int,
		historyDescription: PointDescription?
	) {
		setMapLocationToShow(latitude, longitude, zoom, historyDescription, true, null)
	}

	var lastKnownMapZoom: Int
		get() = settingsAPI.getInt(globalPreferences, LAST_KNOWN_MAP_ZOOM, 5)
		set(zoom) {
			settingsAPI.edit(globalPreferences).putInt(LAST_KNOWN_MAP_ZOOM, zoom).commit()
		}

	var lastKnownMapZoomFloatPart: Float
		get() = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_ZOOM_FLOAT_PART, 0.0f)
		set(zoomFloatPart) {
			settingsAPI.edit(globalPreferences)
				.putFloat(LAST_KNOWN_MAP_ZOOM_FLOAT_PART, zoomFloatPart).commit()
		}

	var lastKnownMapHeight: Float
		get() = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_HEIGHT, 0.0f)
		set(height) {
			settingsAPI.edit(globalPreferences).putFloat(LAST_KNOWN_MAP_HEIGHT, height).commit()
		}

	private val LAST_KNOWN_MAP_ROTATION: CommonPreference<Float> =
		FloatPreference(this, "last_known_map_rotation", 0f).makeProfile()
	private val LAST_KNOWN_MANUALLY_MAP_ROTATION: CommonPreference<Float> = FloatPreference(
		this, "last_known_manually_map_rotation", 0f
	).makeProfile()
	private val LAST_KNOWN_MAP_ELEVATION: CommonPreference<Float> =
		FloatPreference(this, "last_known_map_elevation", 90f).makeProfile()

	var lastKnownMapRotation: Float
		get() = getLastKnownMapRotation(applicationMode)
		set(rotation) {
			setLastKnownMapRotation(applicationMode, rotation)
		}

	fun getLastKnownMapRotation(appMode: ApplicationMode): Float {
		return LAST_KNOWN_MAP_ROTATION.getModeValue(appMode)
	}

	fun setLastKnownMapRotation(appMode: ApplicationMode, rotation: Float) {
		LAST_KNOWN_MAP_ROTATION.setModeValue(appMode, rotation)
	}

	var manuallyMapRotation: Float
		get() = getManuallyMapRotation(applicationMode)
		set(rotation) {
			setManuallyMapRotation(applicationMode, rotation)
		}

	fun getManuallyMapRotation(appMode: ApplicationMode): Float {
		return LAST_KNOWN_MANUALLY_MAP_ROTATION.getModeValue(appMode)
	}

	fun setManuallyMapRotation(appMode: ApplicationMode, rotation: Float) {
		LAST_KNOWN_MANUALLY_MAP_ROTATION.setModeValue(appMode, rotation)
	}

	var lastKnownMapElevation: Float
		get() = getLastKnownMapElevation(applicationMode)
		set(elevation) {
			setLastKnownMapElevation(applicationMode, elevation)
		}

	fun getLastKnownMapElevation(appMode: ApplicationMode): Float {
		return LAST_KNOWN_MAP_ELEVATION.getModeValue(appMode)
	}

	fun setLastKnownMapElevation(appMode: ApplicationMode, elevation: Float) {
		LAST_KNOWN_MAP_ELEVATION.setModeValue(appMode, elevation)
	}

	fun backupPointToStart() {
		settingsAPI.edit(globalPreferences)
			.putFloat(
				START_POINT_LAT_BACKUP,
				settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0f)
			)
			.putFloat(
				START_POINT_LON_BACKUP,
				settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0f)
			)
			.putString(
				START_POINT_DESCRIPTION_BACKUP,
				settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, "")
			)
			.commit()
	}

	private fun backupPointToNavigate() {
		settingsAPI.edit(globalPreferences)
			.putFloat(
				POINT_NAVIGATE_LAT_BACKUP,
				settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0f)
			)
			.putFloat(
				POINT_NAVIGATE_LON_BACKUP,
				settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0f)
			)
			.putString(
				POINT_NAVIGATE_DESCRIPTION_BACKUP,
				settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, "")
			)
			.commit()
	}

	private fun backupIntermediatePoints() {
		settingsAPI.edit(globalPreferences)
			.putString(
				INTERMEDIATE_POINTS_BACKUP,
				settingsAPI.getString(
					globalPreferences,
					IntermediatePointsStorage.INTERMEDIATE_POINTS,
					""
				)
			)
			.putString(
				INTERMEDIATE_POINTS_DESCRIPTION_BACKUP,
				settingsAPI.getString(
					globalPreferences,
					IntermediatePointsStorage.INTERMEDIATE_POINTS_DESCRIPTION,
					""
				)
			)
			.commit()
	}

	fun backupTargetPoints() {
		if (NAVIGATION_HISTORY.get()) {
			backupPointToStart()
			backupPointToNavigate()
			backupIntermediatePoints()
		}
	}

	fun restoreTargetPoints() {
		settingsAPI.edit(globalPreferences)
			.putFloat(
				START_POINT_LAT,
				settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0f)
			)
			.putFloat(
				START_POINT_LON,
				settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0f)
			)
			.putString(
				START_POINT_DESCRIPTION,
				settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION_BACKUP, "")
			)
			.putFloat(
				POINT_NAVIGATE_LAT,
				settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0f)
			)
			.putFloat(
				POINT_NAVIGATE_LON,
				settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON_BACKUP, 0f)
			)
			.putString(
				POINT_NAVIGATE_DESCRIPTION,
				settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION_BACKUP, "")
			)
			.putString(
				IntermediatePointsStorage.INTERMEDIATE_POINTS,
				settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_BACKUP, "")
			)
			.putString(
				IntermediatePointsStorage.INTERMEDIATE_POINTS_DESCRIPTION,
				settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_DESCRIPTION_BACKUP, "")
			)
			.commit()
	}

	fun restorePointToStart(): Boolean {
		if (settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0f) == 0f) {
			settingsAPI.edit(globalPreferences)
				.putFloat(
					START_POINT_LAT,
					settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0f)
				)
				.putFloat(
					START_POINT_LON,
					settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0f)
				)
				.commit()
			return true
		} else {
			return false
		}
	}

	val pointToNavigate: LatLon?
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0f)
			if (lat == 0f && lon == 0f) {
				return null
			}
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	val pointToStart: LatLon?
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0f)
			if (lat == 0f && lon == 0f) {
				return null
			}
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	val startPointDescription: PointDescription
		get() = PointDescription.deserializeFromString(
			settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, ""), pointToStart
		)

	val pointNavigateDescription: PointDescription
		get() = PointDescription.deserializeFromString(
			settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, ""),
			pointToNavigate
		)

	val pointToNavigateBackup: LatLon?
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON_BACKUP, 0f)
			if (lat == 0f && lon == 0f) {
				return null
			}
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	val pointToStartBackup: LatLon?
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0f)
			if (lat == 0f && lon == 0f) {
				return null
			}
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	val startPointDescriptionBackup: PointDescription
		get() = PointDescription.deserializeFromString(
			settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION_BACKUP, ""),
			pointToStart
		)

	val pointNavigateDescriptionBackup: PointDescription
		get() = PointDescription.deserializeFromString(
			settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION_BACKUP, ""),
			pointToNavigate
		)

	val myLocationToStart: LatLon?
		get() {
			val lat = settingsAPI.getFloat(globalPreferences, MY_LOC_POINT_LAT, 0f)
			val lon = settingsAPI.getFloat(globalPreferences, MY_LOC_POINT_LON, 0f)
			if (lat == 0f && lon == 0f) {
				return null
			}
			return LatLon(lat.toDouble(), lon.toDouble())
		}

	val myLocationToStartDescription: PointDescription
		get() = PointDescription.deserializeFromString(
			settingsAPI.getString(globalPreferences, MY_LOC_POINT_DESCRIPTION, ""),
			myLocationToStart
		)

	fun setMyLocationToStart(latitude: Double, longitude: Double, p: PointDescription?) {
		settingsAPI.edit(globalPreferences).putFloat(MY_LOC_POINT_LAT, latitude.toFloat()).putFloat(
			MY_LOC_POINT_LON, longitude.toFloat()
		).commit()
		settingsAPI.edit(globalPreferences)
			.putString(MY_LOC_POINT_DESCRIPTION, PointDescription.serializeToString(p)).commit()
	}

	fun clearMyLocationToStart() {
		settingsAPI.edit(globalPreferences).remove(MY_LOC_POINT_LAT).remove(MY_LOC_POINT_LON).remove
		MY_LOC_POINT_DESCRIPTION.commit()
	}

	val isRouteToPointNavigateAndClear: Int
		get() {
			val vl = settingsAPI.getInt(globalPreferences, POINT_NAVIGATE_ROUTE, 0)
			if (vl != 0) {
				settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_ROUTE).commit()
			}
			return vl
		}

	fun clearIntermediatePoints(): Boolean {
		return settingsAPI.edit(globalPreferences)
			.remove(IntermediatePointsStorage.INTERMEDIATE_POINTS)
			.remove(IntermediatePointsStorage.INTERMEDIATE_POINTS_DESCRIPTION).commit()
	}

	@JvmField
	val USE_INTERMEDIATE_POINTS_NAVIGATION: CommonPreference<Boolean> = BooleanPreference(
		this, "use_intermediate_points_navigation", false
	).makeGlobal().cache()


	fun getIntermediatePointDescriptions(sz: Int): List<String> {
		return intermediatePointsStorage.getPointDescriptions(sz)
	}

	val intermediatePoints: List<LatLon>
		get() = intermediatePointsStorage.points

	fun insertIntermediatePoint(
		latitude: Double,
		longitude: Double,
		historyDescription: PointDescription?,
		index: Int
	): Boolean {
		return intermediatePointsStorage.insertPoint(latitude, longitude, historyDescription, index)
	}

	fun updateIntermediatePoint(
		latitude: Double,
		longitude: Double,
		historyDescription: PointDescription?
	): Boolean {
		return intermediatePointsStorage.updatePoint(latitude, longitude, historyDescription)
	}

	fun deleteIntermediatePoint(index: Int): Boolean {
		return intermediatePointsStorage.deletePoint(index)
	}

	fun saveIntermediatePoints(ps: List<LatLon?>?, ds: List<String?>?): Boolean {
		return intermediatePointsStorage.savePoints(ps!!, ds!!)
	}

	fun clearPointToNavigate(): Boolean {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT).remove(
			POINT_NAVIGATE_LON
		).remove
		POINT_NAVIGATE_DESCRIPTION.commit()
	}

	fun clearPointToStart(): Boolean {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT)
			.remove(START_POINT_LON).remove
		START_POINT_DESCRIPTION.commit()
	}

	fun clearPointToNavigateBackup(): Boolean {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT_BACKUP).remove(
			POINT_NAVIGATE_LON_BACKUP
		).remove
		POINT_NAVIGATE_DESCRIPTION_BACKUP.commit()
	}

	fun clearPointToStartBackup(): Boolean {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT_BACKUP).remove(
			START_POINT_LON_BACKUP
		).remove
		START_POINT_DESCRIPTION_BACKUP.commit()
	}

	fun clearIntermediatePointsBackup(): Boolean {
		return settingsAPI.edit(globalPreferences).remove(INTERMEDIATE_POINTS_BACKUP).remove(
			INTERMEDIATE_POINTS_DESCRIPTION_BACKUP
		).commit()
	}

	fun setPointToNavigate(latitude: Double, longitude: Double, p: PointDescription?): Boolean {
		val add =
			settingsAPI.edit(globalPreferences).putFloat(POINT_NAVIGATE_LAT, latitude.toFloat())
				.putFloat(
					POINT_NAVIGATE_LON, longitude.toFloat()
				).commit()
		settingsAPI.edit(globalPreferences)
			.putString(POINT_NAVIGATE_DESCRIPTION, PointDescription.serializeToString(p)).commit()
		if (add && NAVIGATION_HISTORY.get()) {
			if (p != null && !p.isSearchingAddress(context)) {
				SearchHistoryHelper.getInstance(context)
					.addNewItemToHistory(latitude, longitude, p, HistorySource.NAVIGATION)
			}
		}
		backupTargetPoints()
		return add
	}

	fun setPointToStart(latitude: Double, longitude: Double, p: PointDescription?): Boolean {
		val add = settingsAPI.edit(globalPreferences).putFloat(START_POINT_LAT, latitude.toFloat())
			.putFloat(
				START_POINT_LON, longitude.toFloat()
			).commit()
		settingsAPI.edit(globalPreferences)
			.putString(START_POINT_DESCRIPTION, PointDescription.serializeToString(p)).commit()
		backupTargetPoints()
		return add
	}

	fun navigateDialog(): Boolean {
		return settingsAPI.edit(globalPreferences).putInt(POINT_NAVIGATE_ROUTE, NAVIGATE).commit()
	}

	var impassableRoadsLastModifiedTime: Long
		get() = impassableRoadsStorage.lastModifiedTime
		set(lastModifiedTime) {
			impassableRoadsStorage.lastModifiedTime = lastModifiedTime
		}

	val impassableRoadPoints: List<AvoidRoadInfo>
		get() = impassableRoadsStorage.impassableRoadsInfo

	fun addImpassableRoad(avoidRoadInfo: AvoidRoadInfo?): Boolean {
		return impassableRoadsStorage.addImpassableRoadInfo(avoidRoadInfo!!)
	}

	fun updateImpassableRoadInfo(avoidRoadInfo: AvoidRoadInfo?): Boolean {
		return impassableRoadsStorage.updateImpassableRoadInfo(avoidRoadInfo!!)
	}

	fun removeImpassableRoad(index: Int): Boolean {
		return impassableRoadsStorage.deletePoint(index)
	}

	fun removeImpassableRoad(latLon: LatLon): Boolean {
		return impassableRoadsStorage.deletePoint(latLon)
	}

	fun moveImpassableRoad(latLonEx: LatLon, latLonNew: LatLon): Boolean {
		return impassableRoadsStorage.movePoint(latLonEx, latLonNew)
	}

	@JvmField
	val IS_QUICK_ACTION_TUTORIAL_SHOWN: CommonPreference<Boolean> =
		BooleanPreference(this, "quick_action_tutorial", false).makeGlobal().makeShared()
	@JvmField
	val QUICK_ACTION_BUTTONS: ListStringPreference = ListStringPreference(
		this,
		"quick_action_buttons",
		QuickActionButtonState.DEFAULT_BUTTON_ID + ";",
		";"
	).makeGlobal().makeShared().storeLastModifiedTime() as ListStringPreference


	val lastSearchedPoint: LatLon?
		get() {
			if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_LAT) && settingsAPI.contains(
					globalPreferences,
					LAST_SEARCHED_LON
				)
			) {
				return LatLon(
					settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LAT, 0f).toDouble(),
					settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LON, 0f).toDouble()
				)
			}
			return null
		}

	fun setLastSearchedPoint(l: LatLon?): Boolean {
		return if (l == null) {
			settingsAPI.edit(globalPreferences).remove(LAST_SEARCHED_LAT)
				.remove(LAST_SEARCHED_LON).commit()
		} else {
			setLastSearchedPoint(l.latitude, l.longitude)
		}
	}

	fun setLastSearchedPoint(lat: Double, lon: Double): Boolean {
		return settingsAPI.edit(globalPreferences)
			.putFloat(LAST_SEARCHED_LAT, lat.toFloat()).putFloat
		LAST_SEARCHED_LON, lon.toFloat()).commit()
	}

	val lastSearchedRegion: String
		get() = settingsAPI.getString(globalPreferences, LAST_SEARCHED_REGION, "")

	fun setLastSearchedRegion(region: String?, l: LatLon?): Boolean {
		val edit =
			settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_REGION, region).putLong(
				LAST_SEARCHED_CITY, -1
			).putString
		LAST_SEARCHED_CITY_NAME, "").putString(OsmandSettings.Companion.LAST_SEARCHED_POSTCODE, "").putString
		LAST_SEARCHED_STREET, "").putString(OsmandSettings.Companion.LAST_SEARCHED_BUILDING, "") //$NON-NLS-2$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "")
		}
		val res = edit.commit()
		setLastSearchedPoint(l)
		return res
	}

	val lastSearchedPostcode: String
		get() = settingsAPI.getString(globalPreferences, LAST_SEARCHED_POSTCODE, null)

	fun setLastSearchedPostcode(postcode: String?, point: LatLon?): Boolean {
		val edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, -1).putString(
			LAST_SEARCHED_STREET, ""
		)
			.putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, postcode)
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "")
		}
		val res = edit.commit()
		setLastSearchedPoint(point)
		return res
	}

	val lastSearchedCity: Long
		get() = settingsAPI.getLong(globalPreferences, LAST_SEARCHED_CITY, -1)

	val lastSearchedCityName: String
		get() = settingsAPI.getString(globalPreferences, LAST_SEARCHED_CITY_NAME, "")

	fun setLastSearchedCity(cityId: Long?, name: String?, point: LatLon?): Boolean {
		val edit =
			settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, cityId!!).putString(
				LAST_SEARCHED_CITY_NAME, name
			).putString
		LAST_SEARCHED_STREET, "").putString(OsmandSettings.Companion.LAST_SEARCHED_BUILDING, "").putString(OsmandSettings.Companion.LAST_SEARCHED_POSTCODE, "")
		//edit.remove(LAST_SEARCHED_POSTCODE);
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "")
		}
		val res = edit.commit()
		setLastSearchedPoint(point)
		return res
	}

	val lastSearchedStreet: String
		get() = settingsAPI.getString(globalPreferences, LAST_SEARCHED_STREET, "")

	fun setLastSearchedStreet(street: String?, point: LatLon?): Boolean {
		val edit =
			settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_STREET, street).putString(
				LAST_SEARCHED_BUILDING, ""
			)
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "")
		}
		val res = edit.commit()
		setLastSearchedPoint(point)
		return res
	}

	val lastSearchedBuilding: String
		get() = settingsAPI.getString(globalPreferences, LAST_SEARCHED_BUILDING, "")

	fun setLastSearchedBuilding(building: String?, point: LatLon?): Boolean {
		val res =
			settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_BUILDING, building).remove(
				LAST_SEARCHED_INTERSECTED_STREET
			).commit()
		setLastSearchedPoint(point)
		return res
	}

	val lastSearchedIntersectedStreet: String?
		get() {
			if (!settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
				return null
			}
			return settingsAPI.getString(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET, "")
		}

	fun setLastSearchedIntersectedStreet(street: String?, l: LatLon?): Boolean {
		setLastSearchedPoint(l)
		return settingsAPI.edit(globalPreferences)
			.putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit()
	}

	@JvmField
	val LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT: OsmandPreference<String> = StringPreference(
		this, "last_selected_gpx_track_for_new_point", null
	).makeGlobal().cache()

	// Avoid using this property, probably you need to use PoiFiltersHelper.getSelectedPoiFilters()
	private val SELECTED_POI_FILTER_FOR_MAP =
		ListStringPreference(this, "selected_poi_filters_for_map", null, ",,").makeProfile()
			.cache() as ListStringPreference

	var selectedPoiFilters: Set<String?>?
		get() {
			val result = SELECTED_POI_FILTER_FOR_MAP.stringsList
			return if (result != null) LinkedHashSet(result) else emptySet<String>()
		}
		set(poiFilters) {
			setSelectedPoiFilters(APPLICATION_MODE.get(), poiFilters)
		}

	fun setSelectedPoiFilters(appMode: ApplicationMode, poiFilters: Set<String?>?) {
		val filters: List<String>? = if (poiFilters != null) ArrayList(poiFilters) else null
		SELECTED_POI_FILTER_FOR_MAP.setStringsListForProfile(appMode, filters)
	}

	@JvmField
	val POI_FILTERS_ORDER: ListStringPreference =
		ListStringPreference(this, "poi_filters_order", null, ",,").makeProfile()
			.cache() as ListStringPreference

	@JvmField
	val INACTIVE_POI_FILTERS: ListStringPreference =
		ListStringPreference(this, "inactive_poi_filters", null, ",,").makeProfile()
			.cache() as ListStringPreference

	@JvmField
	val DRAWER_ITEMS: ContextMenuItemsPreference = ContextMenuItemsPreference(
		this,
		"drawer_items",
		OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME,
		DrawerMenuItemsSettings.getDrawerDefaultInstance()
	)
		.makeProfile().cache() as ContextMenuItemsPreference

	@JvmField
	val COLLAPSED_CONFIGURE_MAP_CATEGORIES: ListStringPreference =
		ListStringPreference(this, "collapsed_configure_map_categories", "", ",,").makeProfile()
			.cache() as ListStringPreference

	@JvmField
	val CONFIGURE_MAP_ITEMS: ContextMenuItemsPreference = ContextMenuItemsPreference(
		this,
		"configure_map_items",
		OsmAndCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME,
		ContextMenuItemsSettings()
	)
		.makeProfile().cache() as ContextMenuItemsPreference

	@JvmField
	val CONTEXT_MENU_ACTIONS_ITEMS: ContextMenuItemsPreference = ContextMenuItemsPreference(
		this,
		"context_menu_items",
		OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ACTIONS,
		MainContextMenuItemsSettings()
	)
		.makeProfile().cache() as ContextMenuItemsPreference

	val CONTEXT_MENU_ITEMS_PREFERENCES: List<ContextMenuItemsPreference> =
		Arrays.asList(DRAWER_ITEMS, CONFIGURE_MAP_ITEMS, CONTEXT_MENU_ACTIONS_ITEMS)

	fun getContextMenuItemsPreference(id: String): ContextMenuItemsPreference? {
		for (preference in CONTEXT_MENU_ITEMS_PREFERENCES) {
			if (id.startsWith(preference.idScheme)) {
				return preference
			}
		}
		return null
	}

	// this value could localized
	@JvmField
	val VOICE_PROVIDER: OsmandPreference<String> =
		object : StringPreference(this, "voice_provider", null) {
			override fun getProfileDefaultValue(mode: ApplicationMode): String {
				val language = context.resources.configuration.locale.language
				val supportedTTS = DownloadOsmandIndexesHelper.getSupportedTtsByLanguages(
					context
				)
				val index = supportedTTS[language]
				if (index != null) {
					if (!index.isDownloaded && (context.isApplicationInitializing || !index.isDownloading(
							context
						))
					) {
						DownloadOsmandIndexesHelper.downloadTtsWithoutInternet(context, index)
					}
					return language + IndexConstants.VOICE_PROVIDER_SUFFIX
				}
				return VOICE_PROVIDER_NOT_USE
			}
		}.makeProfile()

	fun isVoiceProviderNotSelected(appMode: ApplicationMode?): Boolean {
		val voiceProvider = VOICE_PROVIDER.getModeValue(appMode)
		return Algorithms.isEmpty(voiceProvider) || VOICE_PROVIDER_NOT_USE == voiceProvider
	}

	@JvmField
	val RENDERER: CommonPreference<String> =
		object : StringPreference(this, "renderer", RendererRegistry.DEFAULT_RENDER) {
			override fun setValue(prefs: Any, `val`: String): Boolean {
				var `val`: String? = `val`
				if (`val` == null) {
					`val` = RendererRegistry.DEFAULT_RENDER
				}
				val loaded = context.rendererRegistry.getRenderer(`val`)
				if (loaded != null) {
					return super.setValue(prefs, `val`)
				}
				return false
			}
		}.makeProfile()

	init {
		RENDERER.setModeDefaultValue(ApplicationMode.BOAT, RendererRegistry.NAUTICAL_RENDER)
		RENDERER.setModeDefaultValue(ApplicationMode.SKI, RendererRegistry.WINTER_SKI_RENDER)
	}

	fun getRenderBooleanPropertyValue(attrName: String): Boolean {
		return if (attrName == OsmandRasterMapsPlugin.NO_POLYGONS_ATTR) {
			shouldHidePolygons(true)
		} else if (attrName == OsmandRasterMapsPlugin.HIDE_WATER_POLYGONS_ATTR) {
			shouldHidePolygons(false)
		} else {
			getCustomRenderBooleanProperty(attrName).get()
		}
	}

	fun getRenderPropertyValue(attrName: String): String? {
		return getCustomRenderProperty(attrName).get()
	}

	fun getCustomRenderProperty(attrName: String): CommonPreference<String> {
		if (!customRendersProps.containsKey(attrName)) {
			registerCustomRenderProperty(attrName, "")
		}
		return customRendersProps[attrName]!!
	}

	fun registerCustomRenderProperty(
		attrName: String,
		defaultValue: String?
	): CommonPreference<String> {
		val preference = StringPreference(
			this,
			RENDERER_PREFERENCE_PREFIX + attrName,
			defaultValue
		).makeProfile()
		customRendersProps[attrName] = preference
		return preference
	}

	init {
		getCustomRenderProperty("appMode")
		getCustomRenderProperty("defAppMode")
	}

	fun isRenderProperty(prefId: String): Boolean {
		return prefId.startsWith(RENDERER_PREFERENCE_PREFIX)
	}

	fun isRoutingProperty(prefId: String): Boolean {
		return prefId.startsWith(ROUTING_PREFERENCE_PREFIX)
	}

	fun getCustomRenderBooleanProperty(attrName: String): CommonPreference<Boolean> {
		if (!customBooleanRendersProps.containsKey(attrName)) {
			registerCustomRenderBooleanProperty(attrName, false)
		}
		return customBooleanRendersProps[attrName]!!
	}

	fun registerCustomRenderBooleanProperty(
		attrName: String,
		defaultValue: Boolean
	): CommonPreference<Boolean> {
		val preference = BooleanPreference(
			this,
			RENDERER_PREFERENCE_PREFIX + attrName,
			defaultValue
		).makeProfile()
		customBooleanRendersProps[attrName] = preference
		return preference
	}

	fun getCustomRoutingProperty(attrName: String, defValue: String?): CommonPreference<String> {
		if (!customRoutingProps.containsKey(attrName)) {
			customRoutingProps[attrName] = StringPreference(
				this,
				ROUTING_PREFERENCE_PREFIX + attrName,
				defValue
			).makeProfile()
		}
		return customRoutingProps[attrName]!!
	}

	fun getCustomRoutingBooleanProperty(
		attrName: String,
		defaulfValue: Boolean
	): CommonPreference<Boolean> {
		if (!customBooleanRoutingProps.containsKey(attrName)) {
			customBooleanRoutingProps[attrName] = BooleanStringPreference(
				this,
				ROUTING_PREFERENCE_PREFIX + attrName,
				defaulfValue
			).makeProfile()
		}
		return customBooleanRoutingProps[attrName]!!
	}

	@JvmField
	val SHOW_TRAVEL: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_travel_routes", false).makeProfile().cache()

	@JvmField
	val ROUTE_RECALCULATION_DISTANCE: CommonPreference<Float> =
		FloatPreference(this, "routing_recalc_distance", 0f).makeProfile()
	@JvmField
	val ROUTE_STRAIGHT_ANGLE: CommonPreference<Float> =
		FloatPreference(this, "routing_straight_angle", 30f).makeProfile()

	@JvmField
	val CUSTOM_ROUTE_COLOR_DAY: CommonPreference<Int> = IntPreference(
		this,
		"route_line_color", DefaultColors.values()[0].color
	).cache().makeProfile()
	@JvmField
	val CUSTOM_ROUTE_COLOR_NIGHT: CommonPreference<Int> = IntPreference(
		this,
		"route_line_color_night", DefaultColors.values()[0].color
	).cache().makeProfile()
	@JvmField
	val ROUTE_COLORING_TYPE: CommonPreference<ColoringType> = EnumStringPreference(
		this,
		"route_line_coloring_type", ColoringType.DEFAULT, valuesOf(ColoringPurpose.ROUTE_LINE)
	).cache().makeProfile()

	@JvmField
	val ROUTE_GRADIENT_PALETTE: CommonPreference<String> = StringPreference(
		this,
		"route_gradient_palette",
		PaletteGradientColor.DEFAULT_NAME
	).makeProfile().cache()
	@JvmField
	val ROUTE_INFO_ATTRIBUTE: CommonPreference<String> =
		StringPreference(this, "route_info_attribute", null)
			.cache().makeProfile()
	@JvmField
	val ROUTE_LINE_WIDTH: CommonPreference<String> =
		StringPreference(this, "route_line_width", null).makeProfile()
	@JvmField
	val ROUTE_SHOW_TURN_ARROWS: CommonPreference<Boolean> =
		BooleanPreference(this, "route_show_turn_arrows", true).makeProfile()

	@JvmField
	val USE_OSM_LIVE_FOR_ROUTING: OsmandPreference<Boolean> =
		BooleanPreference(this, "enable_osmc_routing", true).makeProfile()

	@JvmField
	val USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT: OsmandPreference<Boolean> =
		BooleanPreference(this, "enable_osmc_public_transport", false).makeProfile()

	@JvmField
	val VOICE_MUTE: OsmandPreference<Boolean> =
		BooleanPreference(this, "voice_mute", false).makeProfile().cache()
	@JvmField
	val DETAILED_TRACK_GUIDANCE: CommonPreference<TrackApproximationType> = EnumStringPreference(
		this, "detailed_track_guidance",
		TrackApproximationType.MANUAL, TrackApproximationType.entries.toTypedArray()
	).makeProfile().makeShared()
	@JvmField
	val GPX_APPROXIMATION_DISTANCE: OsmandPreference<Int> = IntPreference(
		this,
		"gpx_approximation_distance",
		GpxApproximator.DEFAULT_POINT_APPROXIMATION
	).makeProfile().makeShared()

	// for background service
	@JvmField
	var MAP_ACTIVITY_ENABLED: Boolean = false

	@JvmField
	val SAFE_MODE: OsmandPreference<Boolean> =
		BooleanPreference(this, "safe_mode", false).makeGlobal().makeShared()
	@JvmField
	val PT_SAFE_MODE: OsmandPreference<Boolean> =
		BooleanPreference(this, "pt_safe_mode", false).makeProfile()
	@JvmField
	val NATIVE_RENDERING_FAILED: OsmandPreference<Boolean> =
		BooleanPreference(this, "native_rendering_failed_init", false).makeGlobal()

	@JvmField
	val USE_OPENGL_RENDER: OsmandPreference<Boolean> = BooleanPreference(
		this,
		"use_opengl_render",
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
	).makeGlobal().makeShared().cache()
	@JvmField
	val OPENGL_RENDER_FAILED: OsmandPreference<Int> =
		IntPreference(this, "opengl_render_failed_count", 0).makeGlobal().cache()

	val CONTRIBUTION_INSTALL_APP_DATE: OsmandPreference<String> =
		StringPreference(this, "CONTRIBUTION_INSTALL_APP_DATE", null).makeGlobal()

	@JvmField
	val COORDINATES_FORMAT: OsmandPreference<Int> =
		IntPreference(this, "coordinates_format", PointDescription.FORMAT_DEGREES).makeProfile()

	@JvmField
	val FOLLOW_THE_ROUTE: OsmandPreference<Boolean> =
		BooleanPreference(this, "follow_to_route", false).makeGlobal()
	@JvmField
	val FOLLOW_THE_GPX_ROUTE: OsmandPreference<String> =
		StringPreference(this, "follow_gpx", null).makeGlobal()
	val SHOW_RESTART_NAVIGATION_DIALOG: OsmandPreference<Boolean> =
		BooleanPreference(this, "show_restart_navigation_dialog", true).makeGlobal().makeShared()

	@JvmField
	val SELECTED_TRAVEL_BOOK: OsmandPreference<String> =
		StringPreference(this, "selected_travel_book", "").makeGlobal().makeShared()

	@JvmField
	val DISPLAYED_TRANSPORT_SETTINGS: ListStringPreference = ListStringPreference(
		this,
		"displayed_transport_settings",
		null,
		","
	).makeProfile() as ListStringPreference

	@JvmField
	val SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING: OsmandPreference<Boolean> =
		BooleanPreference(
			this, "show_relative_bearing", true
		).makeProfile()

	@JvmField
	val AGPS_DATA_LAST_TIME_DOWNLOADED: OsmandPreference<Long> =
		LongPreference(this, "agps_data_downloaded", 0).makeGlobal()

	@JvmField
	val MEMORY_ALLOCATED_FOR_ROUTING: CommonPreference<Int> =
		IntPreference(this, "memory_allocated_for_routing", 256).makeGlobal()

	// Live Updates
	@JvmField
	val IS_LIVE_UPDATES_ON: OsmandPreference<Boolean> =
		BooleanPreference(this, "is_live_updates_on", false).makeGlobal().makeShared()
	@JvmField
	val LIVE_UPDATES_RETRIES: OsmandPreference<Int> =
		IntPreference(this, "live_updates_retryes", 2).makeGlobal()

	// UI boxes
	@JvmField
	val TRANSPARENT_MAP_THEME: CommonPreference<Boolean> =
		BooleanPreference(this, "transparent_map_theme", false).makeProfile()

	val SHOW_STREET_NAME: CommonPreference<Boolean> =
		BooleanPreference(this, "show_street_name", false).makeProfile()

	init {
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.DEFAULT, false)
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.CAR, true)
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, false)
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false)
	}

	val SEARCH_TAB: CommonPreference<Int> =
		IntPreference(this, "SEARCH_TAB", 0).makeGlobal().cache()

	@JvmField
	val FAVORITES_TAB: CommonPreference<Int> =
		IntPreference(this, "FAVORITES_TAB", 0).makeGlobal().cache()

	@JvmField
	val OSMAND_THEME: CommonPreference<Int> = object : IntPreference(
		this,
		"osmand_theme",
		if (isSupportSystemTheme) SYSTEM_DEFAULT_THEME else OSMAND_LIGHT_THEME
	) {
		@Throws(JSONException::class)
		override fun readFromJson(json: JSONObject, appMode: ApplicationMode) {
			var theme = parseString(json.getString(id))
			if (theme == SYSTEM_DEFAULT_THEME && !this.isSupportSystemTheme) {
				theme = OSMAND_LIGHT_THEME
			}
			setModeValue(appMode, theme)
		}
	}.makeProfile().cache()

	@JvmField
	val OPEN_ONLY_HEADER_STATE_ROUTE_CALCULATED: OsmandPreference<Boolean> = BooleanPreference(
		this, "open_only_header_route_calculated", false
	).makeProfile()

	val isLightContent: Boolean
		get() = isLightContentForMode(APPLICATION_MODE.get())

	fun isLightContentForMode(appMode: ApplicationMode): Boolean {
		if (isSystemThemeUsed(appMode)) {
			return isLightSystemTheme
		}
		return OSMAND_THEME.getModeValue(appMode) != OSMAND_DARK_THEME
	}

	val isLightSystemTheme: Boolean
		get() = !getNightMode(context.resources.configuration)

	private fun getNightMode(config: Configuration): Boolean {
		val currentNightMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
		when (currentNightMode) {
			Configuration.UI_MODE_NIGHT_NO -> return false
			Configuration.UI_MODE_NIGHT_YES -> return true
		}
		LOG.info("Undefined night mode$config")
		return false
	}

	fun isSystemThemeUsed(appMode: ApplicationMode): Boolean {
		return isSupportSystemTheme && OSMAND_THEME.getModeValue(appMode) == SYSTEM_DEFAULT_THEME
	}

	val isSupportSystemTheme: Boolean
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

	val FLUORESCENT_OVERLAYS: CommonPreference<Boolean> =
		BooleanPreference(this, "fluorescent_overlays", false).makeGlobal().makeShared().cache()


	//	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS_V2 = new IntPreference("free_downloads_v2", 0).makeGlobal();
	@JvmField
	val NUMBER_OF_FREE_DOWNLOADS: OsmandPreference<Int> =
		IntPreference(this, NUMBER_OF_FREE_DOWNLOADS_ID, 0).makeGlobal()

	// For RateUsDialog
	@JvmField
	val LAST_DISPLAY_TIME: OsmandPreference<Long> =
		LongPreference(this, "last_display_time", 0).makeGlobal().cache()

	@JvmField
	val LAST_CHECKED_UPDATES: OsmandPreference<Long> =
		LongPreference(this, "last_checked_updates", 0).makeGlobal()

	@JvmField
	val NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT: OsmandPreference<Int> =
		IntPreference(this, "number_of_app_starts_on_dislike_moment", 0).makeGlobal().cache()

	@JvmField
	val RATE_US_STATE: OsmandPreference<RateUsState> = EnumStringPreference(
		this,
		"rate_us_state",
		RateUsState.INITIAL_STATE,
		RateUsState.entries.toTypedArray()
	).makeGlobal()

	@JvmField
	val CUSTOM_APP_MODES_KEYS: CommonPreference<String> =
		StringPreference(this, "custom_app_modes_keys", "").makeGlobal().cache()

	@JvmField
	val SHOW_BORDERS_OF_DOWNLOADED_MAPS: CommonPreference<Boolean> =
		BooleanPreference(this, "show_borders_of_downloaded_maps", true).makeProfile()

	val customAppModesKeys: Set<String>
		get() {
			val appModesKeys = CUSTOM_APP_MODES_KEYS.get()
			val toks = StringTokenizer(appModesKeys, ",")
			val res: MutableSet<String> = LinkedHashSet()
			while (toks.hasMoreTokens()) {
				res.add(toks.nextToken())
			}
			return res
		}

	@JvmField
	val LAST_CYCLE_ROUTES_NODE_NETWORK_STATE: CommonPreference<Boolean> = BooleanPreference(
		this, "cycle_routes_last_node_network_state", false
	).makeProfile()

	@JvmField
	val LAST_MTB_ROUTES_CLASSIFICATION: CommonPreference<String> = StringPreference(
		this,
		"mtb_routes_last_classification",
		MtbClassification.SCALE.attrName
	).makeProfile()

	@JvmField
	val LAST_HIKING_ROUTES_VALUE: CommonPreference<String> =
		StringPreference(this, "hiking_routes_last_selected_value", "").makeProfile()

	@JvmField
	val FAVORITES_FREE_ACCOUNT_CARD_DISMISSED: OsmandPreference<Boolean> = BooleanPreference(
		this, "favorites_free_account_card_dismissed", false
	).makeGlobal()

	@JvmField
	val CONFIGURE_PROFILE_FREE_ACCOUNT_CARD_DISMISSED: OsmandPreference<Boolean> =
		BooleanPreference(
			this, "configure_profile_free_account_card_dismissed", false
		).makeGlobal()

	@JvmField
	val TRIPLTEK_PROMO_SHOWED: OsmandPreference<Boolean> =
		BooleanPreference(this, "tripltek_promo_showed", false).makeGlobal().makeShared()
	@JvmField
	val HUGEROCK_PROMO_SHOWED: OsmandPreference<Boolean> =
		BooleanPreference(this, "hugerock_promo_showed", false).makeGlobal().makeShared()
	@JvmField
	val CONTEXT_GALLERY_SPAN_GRID_COUNT: CommonPreference<Int> =
		IntPreference(this, "context_gallery_span_grid_count", 3).makeProfile()
	@JvmField
	val CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE: CommonPreference<Int> =
		IntPreference(this, "context_gallery_span_grid_count_landscape", 7).makeProfile()

	companion object {
		private val LOG: Log = PlatformUtil.getLog(
			OsmandSettings::class.java.name
		)

		const val VERSION: Int = 1

		// These settings are stored in SharedPreferences
		const val CUSTOM_SHARED_PREFERENCES_PREFIX: String = "net.osmand.customsettings."
		const val SHARED_PREFERENCES_NAME: String = "net.osmand.settings"
		private var CUSTOM_SHARED_PREFERENCES_NAME: String? = null

		const val RENDERER_PREFERENCE_PREFIX: String = "nrenderer_"
		const val ROUTING_PREFERENCE_PREFIX: String = "prouting_"

		const val SIM_MIN_SPEED: Float = 5 / 3.6f
		fun isRendererPreference(key: String): Boolean {
			return key.startsWith(RENDERER_PREFERENCE_PREFIX)
		}

		@JvmStatic
		fun isRoutingPreference(key: String): Boolean {
			return key.startsWith(ROUTING_PREFERENCE_PREFIX)
		}

		private const val SETTING_CUSTOMIZED_ID = "settings_customized"

		@JvmStatic
		fun getSharedPreferencesName(mode: ApplicationMode?): String? {
			val modeKey = mode?.stringKey
			return getSharedPreferencesNameForKey(modeKey)
		}

		fun getSharedPreferencesNameForKey(modeKey: String?): String? {
			val sharedPreferencesName =
				if (!Algorithms.isEmpty(CUSTOM_SHARED_PREFERENCES_NAME)) CUSTOM_SHARED_PREFERENCES_NAME else SHARED_PREFERENCES_NAME
			return if (modeKey == null) {
				sharedPreferencesName
			} else {
				sharedPreferencesName + "." + modeKey.lowercase(Locale.getDefault())
			}
		}

		@JvmStatic
		fun areSettingsCustomizedForPreference(
			sharedPreferencesName: String,
			app: OsmandApplication?
		): Boolean {
			val customPrefName = CUSTOM_SHARED_PREFERENCES_PREFIX + sharedPreferencesName
			val settingsAPI = SettingsAPIImpl(app)
			val globalPreferences =
				settingsAPI.getPreferenceObject(customPrefName) as SharedPreferences

			return globalPreferences != null && globalPreferences.getBoolean(
				SETTING_CUSTOMIZED_ID,
				false
			)
		}

		private const val LAST_PREFERENCES_EDIT_TIME = "last_preferences_edit_time"

		///////////////////// PREFERENCES ////////////////
		const val NUMBER_OF_FREE_DOWNLOADS_ID: String = "free_downloads_v3"

		const val LAST_START_LAT: String = "last_searched_lat"
		const val LAST_START_LON: String = "last_searched_lon"

		const val BILLING_USER_DONATION_WORLD_PARAMETER: String = ""
		const val BILLING_USER_DONATION_NONE_PARAMETER: String = "none"

		const val REC_DIRECTORY: Int = 0
		const val MONTHLY_DIRECTORY: Int = 1

		@JvmField
		var IGNORE_MISSING_MAPS: Boolean = false

		// try without AUTO_FOLLOW_ROUTE_NAV (see forum discussion 'Simplify our navigation preference menu')
		//public final CommonPreference<Boolean> AUTO_FOLLOW_ROUTE_NAV = new BooleanPreference("auto_follow_route_navigation", true, false);
		const val ROTATE_MAP_NONE: Int = 0
		const val ROTATE_MAP_BEARING: Int = 1
		const val ROTATE_MAP_COMPASS: Int = 2
		const val ROTATE_MAP_MANUAL: Int = 3

		const val POSITION_PLACEMENT_AUTOMATIC: Int = 0
		const val POSITION_PLACEMENT_CENTER: Int = 1
		const val POSITION_PLACEMENT_BOTTOM: Int = 2
		const val EXTERNAL_STORAGE_DIR: String = "external_storage_dir"

		const val EXTERNAL_STORAGE_DIR_V19: String = "external_storage_dir_V19"
		const val EXTERNAL_STORAGE_DIR_TYPE_V19: String = "external_storage_dir_type_V19"
		const val EXTERNAL_STORAGE_TYPE_DEFAULT: Int =
			0 // Environment.getExternalStorageDirectory()
		const val EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE: Int = 1 // ctx.getExternalFilesDirs(null)
		const val EXTERNAL_STORAGE_TYPE_INTERNAL_FILE: Int = 2 // ctx.getFilesDir()
		const val EXTERNAL_STORAGE_TYPE_OBB: Int = 3 // ctx.getObbDirs
		const val EXTERNAL_STORAGE_TYPE_SPECIFIED: Int = 4

		// This value is a key for saving last known location shown on the map
		const val LAST_KNOWN_MAP_LAT: String = "last_known_map_lat"
		const val LAST_KNOWN_MAP_LON: String = "last_known_map_lon"
		const val LAST_KNOWN_MAP_ZOOM: String = "last_known_map_zoom"
		const val LAST_KNOWN_MAP_ZOOM_FLOAT_PART: String = "last_known_map_zoom_float_part"
		const val LAST_KNOWN_MAP_HEIGHT: String = "last_known_map_height"
		const val MAP_LABEL_TO_SHOW: String = "map_label_to_show"
		const val MAP_LAT_TO_SHOW: String = "map_lat_to_show"
		const val MAP_LON_TO_SHOW: String = "map_lon_to_show"
		const val MAP_ZOOM_TO_SHOW: String = "map_zoom_to_show"

		const val POINT_NAVIGATE_LAT: String = "point_navigate_lat"
		const val POINT_NAVIGATE_LON: String = "point_navigate_lon"
		const val POINT_NAVIGATE_ROUTE: String = "point_navigate_route_integer"
		const val NAVIGATE: Int = 1
		const val POINT_NAVIGATE_DESCRIPTION: String = "point_navigate_description"
		const val START_POINT_LAT: String = "start_point_lat"
		const val START_POINT_LON: String = "start_point_lon"
		const val START_POINT_DESCRIPTION: String = "start_point_description"


		const val POINT_NAVIGATE_LAT_BACKUP: String = "point_navigate_lat_backup"
		const val POINT_NAVIGATE_LON_BACKUP: String = "point_navigate_lon_backup"
		const val POINT_NAVIGATE_DESCRIPTION_BACKUP: String = "point_navigate_description_backup"
		const val START_POINT_LAT_BACKUP: String = "start_point_lat_backup"
		const val START_POINT_LON_BACKUP: String = "start_point_lon_backup"
		const val START_POINT_DESCRIPTION_BACKUP: String = "start_point_description_backup"
		const val INTERMEDIATE_POINTS_BACKUP: String = "intermediate_points_backup"
		const val INTERMEDIATE_POINTS_DESCRIPTION_BACKUP: String =
			"intermediate_points_description_backup"
		const val MY_LOC_POINT_LAT: String = "my_loc_point_lat"
		const val MY_LOC_POINT_LON: String = "my_loc_point_lon"
		const val MY_LOC_POINT_DESCRIPTION: String = "my_loc_point_description"


		/**
		 * the location of a parked car
		 */
		const val LAST_SEARCHED_REGION: String = "last_searched_region"
		const val LAST_SEARCHED_CITY: String = "last_searched_city"
		const val LAST_SEARCHED_CITY_NAME: String = "last_searched_city_name"
		const val LAST_SEARCHED_POSTCODE: String = "last_searched_postcode"
		const val LAST_SEARCHED_STREET: String = "last_searched_street"
		const val LAST_SEARCHED_BUILDING: String = "last_searched_building"
		const val LAST_SEARCHED_INTERSECTED_STREET: String = "last_searched_intersected_street"
		const val LAST_SEARCHED_LAT: String = "last_searched_lat"
		const val LAST_SEARCHED_LON: String = "last_searched_lon"

		const val VOICE_PROVIDER_NOT_USE: String = "VOICE_PROVIDER_NOT_USE"

		const val OSMAND_DARK_THEME: Int = 0
		const val OSMAND_LIGHT_THEME: Int = 1
		const val SYSTEM_DEFAULT_THEME: Int = 2
	}
}
