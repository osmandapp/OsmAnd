package net.osmand.test.ui.navigation

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.data.LatLon
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.IntentHelper
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.router.GeneralRouter
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RouteLinkGenerationTest : AndroidTest() {

    companion object {
        private val START: LatLon = LatLon(45.92051, 35.20653)
        private val END: LatLon = LatLon(45.91741, 35.21372)
        private const val ROUTING_PARAMS_KEY = "params"
    }

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

    override fun cleanUp() {
        super.cleanUp()
        listOf(ApplicationMode.CAR, ApplicationMode.PEDESTRIAN).forEach { mode ->
            app.settings.resetPreferencesForProfile(mode)
        }
    }

    @Test
    fun testRoutingParamsGeneration() {
        ActivityScenario.launch(MapActivity::class.java)
        skipAppStartDialogs(app)

        app.getTargetPointsHelper()
            .setStartPoint(START, true, null)
        app.getTargetPointsHelper().navigateToPoint(END, true, -1)

        var routeUrl = IntentHelper.generateRouteUrl(app)
        assertNoRoutingParams(routeUrl)

        var (appMode, modeParams) = setApplicationMode(ApplicationMode.CAR)
        routeUrl = IntentHelper.generateRouteUrl(app)
        assertNoRoutingParams(routeUrl)

        setCustomRoutingBooleanPref(modeParams, appMode, true, GeneralRouter.AVOID_FERRIES)
        routeUrl = IntentHelper.generateRouteUrl(app)
        assertRoutingParamsMatch(routeUrl, "car", "avoid_ferries")

        settings.FAST_ROUTE_MODE.setModeValue(appMode, false)
        routeUrl = IntentHelper.generateRouteUrl(app)
        assertRoutingParamsMatch(routeUrl, "car", "avoid_ferries", "short_way")

        setApplicationMode(ApplicationMode.PEDESTRIAN).let { (m, p) ->
            appMode = m
            modeParams = p
        }
        routeUrl = IntentHelper.generateRouteUrl(app)
        assertNoRoutingParams(routeUrl)

        setCustomRoutingBooleanPref(modeParams, appMode, true, GeneralRouter.AVOID_FERRIES)
        setCustomRoutingBooleanPref(modeParams, appMode, false, GeneralRouter.USE_HEIGHT_OBSTACLES)
        routeUrl = IntentHelper.generateRouteUrl(app)
        assertRoutingParamsMatch(
            routeUrl,
            "pedestrian",
            "avoid_ferries",
            "height_obstacles:false"
        )
    }

    private fun assertNoRoutingParams(routeUrl: String) {
        val uri = Uri.parse(routeUrl)
        assertTrue(
            "settings not changed but routing params added",
            !uri.queryParameterNames.contains(ROUTING_PARAMS_KEY)
        )
    }

    private fun assertRoutingParamsMatch(routeUrl: String, vararg expectedParts: String) {
        val uri = Uri.parse(routeUrl)
        val actualParts = uri.getQueryParameter(ROUTING_PARAMS_KEY)?.split(",")?.toSet()
        assertEquals("routing params don't match", expectedParts.toSet(), actualParts)
    }

    private fun setApplicationMode(appMode: ApplicationMode): Pair<ApplicationMode, Map<String, GeneralRouter.RoutingParameter>> {
        app.settings.resetPreferencesForProfile(appMode)
        app.routingHelper.appMode = appMode
        val router = app.getRouter(appMode)
        assertNotNull("failed to obtain router for ${appMode.stringKey} ApplicationMode", router)
        val modeParams = RoutingHelperUtils.getParametersForDerivedProfile(appMode, router!!)
        return appMode to modeParams
    }

    private fun setCustomRoutingBooleanPref(
        modeParams: Map<String, GeneralRouter.RoutingParameter>,
        appMode: ApplicationMode,
        value: Boolean,
        key: String
    ) {
        modeParams[key]?.let { p ->
            if (p.type == GeneralRouter.RoutingParameterType.BOOLEAN) {
                settings
                    .getCustomRoutingBooleanProperty(p.id, p.defaultBoolean)
                    .setModeValue(appMode, value)
            }
        }
    }
}