package net.osmand.plus.helpers

import net.osmand.plus.OsmandApplication
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.router.GeneralRouter

/**
 * Reads navigation/routing profile parameters and custom routing properties
 */
interface RoutingSettingsProvider {
    fun getParametersForDerivedProfile(): Map<String, GeneralRouter.RoutingParameter>
    fun isFastMode(): Boolean
    fun getCustomRoutingBooleanPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter): Boolean
    fun getCustomRoutingPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter): String
}

/**
 * Sets navigation/routing properties
 */
interface RoutingSettingsApplier {
    fun setCustomRoutingBooleanPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter, value: Boolean)
    fun setCustomRoutingPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter, value: String)
    fun setFastMode(value: Boolean)
}

/**
 * Contains methods for composing, parsing and applying "routing/navigation params" query param value
 */
object RoutingUriQueryHandler {

    /**
     * Utility class for holding key-value pairs parsed from query params
     */
    data class KeyValue(val key: String, val value: String)

    /**
     * Parses value of "routing/navigation params" query parameter into list of key/value
     *
     * @param queryParamValue
     * @return
     */
    @JvmStatic
    fun parseRoutingParamsQueryValue(queryParamValue: String): List<KeyValue> {
        val parsed = mutableListOf<KeyValue>()

        val rawParts = queryParamValue.split(",")
        for (raw in rawParts) {
            val splits = raw.split(":", "=", limit = 2)
            if (splits.isEmpty()) continue

            val p = if(splits.size == 2) {
                // "key:value"
                KeyValue(splits[0], splits[1])
            } else {
                // for "key" without ":value", consider value to be "true"
                KeyValue(splits[0], "true")
            }
            parsed.add(p)
        }
        return parsed
    }

    /**
     * Creates query param value for routing/navigation parameters for supplied navigation profile
     *
     * @param appMode navigation profile (car, bicycle, etc.)
     * @param provider reads navigation/routing profile parameters and custom routing properties
     * @return query value string, or null if there's nothing to add
     */
    @JvmStatic
    fun getRoutingParamsQueryValueForAppMode(
        appMode: ApplicationMode,
        provider: RoutingSettingsProvider
    ): String? {
        var hasChanges = false
        val parts = mutableListOf(appMode.stringKey)
        val params = provider.getParametersForDerivedProfile()
        for ((key, pr) in params) {
            if (key == GeneralRouter.USE_SHORTEST_WAY) {
                if (!provider.isFastMode()) {
                    hasChanges = true
                    parts.add(GeneralRouter.USE_SHORTEST_WAY)
                    continue
                }
            }
            if (pr.type == GeneralRouter.RoutingParameterType.BOOLEAN) {
                val settingsValue = provider.getCustomRoutingBooleanPropertyValue(key, pr)
                if (settingsValue == pr.defaultBoolean) continue
                hasChanges = true
                val value = if (settingsValue) key else "$key:false"
                parts.add(value)
            } else {
                val settingsValue = provider.getCustomRoutingPropertyValue(key, pr)
                if (settingsValue == pr.defaultString) continue
                hasChanges = true
                val value = "$key:$settingsValue"
                parts.add(value)
            }
        }
        val result = if (hasChanges) parts.joinToString(",") else null
        return result
    }

    /**
     * Loads supplied routing/navigation params into OsmandSettings
     *
     * @param appMode
     * @param appMode navigation profile (car, bicycle, etc.)
     * @param routingSettingsProvider reads navigation/routing profile parameters and custom routing properties
     * @param routingSettingsApplier sets navigation/routing properties
     */
    @JvmStatic
    fun loadParamsIntoApp(
        appMode: ApplicationMode,
        params: List<KeyValue>,
        routingSettingsProvider: RoutingSettingsProvider,
        routingSettingsApplier: RoutingSettingsApplier
    ) {
        val appParams = routingSettingsProvider.getParametersForDerivedProfile()
        loadDerivedDefaultsIntoApp(appParams, routingSettingsApplier)
        for (p in params) {
            if (p.key.isEmpty())
                continue

            if (p.key == appMode.stringKey || p.key == appMode.routingProfile)
                continue

            if (p.key == GeneralRouter.USE_SHORTEST_WAY) {
                val isOn = (p.value != "false")
                routingSettingsApplier.setFastMode(!isOn)
                continue
            }
            val ap = appParams[p.key] ?: continue
            if (ap.type == GeneralRouter.RoutingParameterType.BOOLEAN) {
                val value = (p.value != "false")
                routingSettingsApplier.setCustomRoutingBooleanPropertyValue(p.key, ap, value)
            } else {
                routingSettingsApplier.setCustomRoutingPropertyValue(p.key, ap, p.value)
            }
        }
    }

    private fun loadDerivedDefaultsIntoApp(
        appParams: Map<String, GeneralRouter.RoutingParameter>,
        routingSettingsApplier: RoutingSettingsApplier
        ) {
        for ((key, pr) in appParams) {
            if (key == GeneralRouter.USE_SHORTEST_WAY) {
                val isFastMode = !(pr.defaultBoolean)
                routingSettingsApplier.setFastMode(isFastMode)
            }
            if (pr.type == GeneralRouter.RoutingParameterType.BOOLEAN) {
                routingSettingsApplier.setCustomRoutingBooleanPropertyValue(
                    key,
                    pr,
                    pr.defaultBoolean
                )
            } else {
                routingSettingsApplier.setCustomRoutingPropertyValue(key, pr, pr.defaultString)
            }
        }
    }
}


/**
 * Constructs an implementation of RoutingSettingsApplier for use in e.g. IntentHelper
 */
fun commonRoutingSettingsApplierImpl(app: OsmandApplication, appMode: ApplicationMode): RoutingSettingsApplier {
    val applier = object : RoutingSettingsApplier {
        private val settings = app.settings
        private val mode = appMode
        override fun setCustomRoutingBooleanPropertyValue(
            key: String,
            routingParam: GeneralRouter.RoutingParameter,
            value: Boolean
        ) {
            settings
                .getCustomRoutingBooleanProperty(key, routingParam.defaultBoolean)
                .setModeValue(mode, value)
        }

        override fun setCustomRoutingPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter, value: String) {
            settings
                .getCustomRoutingProperty(key, routingParam.defaultString)
                .setModeValue(mode, value)
        }

        override fun setFastMode(value: Boolean) {
            settings.FAST_ROUTE_MODE.setModeValue(mode, value)
        }
    }
    return applier
}

/**
 * Constructs an implementation of RoutingSettingsProvider for use in e.g. IntentHelper
 */
fun commonRoutingSettingsProviderImpl(
    app: OsmandApplication,
    appMode: ApplicationMode
):  RoutingSettingsProvider? {
    val appRouter = app.getRouter(appMode) ?: return null
    val provider = object : RoutingSettingsProvider {
        private val settings = app.settings
        private val router = appRouter
        private val mode = appMode
        override fun getCustomRoutingBooleanPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter) =
            settings.getCustomRoutingBooleanProperty(key, routingParam.defaultBoolean)
                .getModeValue(mode)

        override fun getParametersForDerivedProfile() =
            RoutingHelperUtils.getParametersForDerivedProfile(mode, router)

        override fun isFastMode() = settings.FAST_ROUTE_MODE.getModeValue(mode)

        override fun getCustomRoutingPropertyValue(key: String, routingParam: GeneralRouter.RoutingParameter) =
            settings.getCustomRoutingProperty(key, routingParam.defaultString)
                .getModeValue(mode)
    }
    return provider
}