package net.osmand.test.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.plus.helpers.RoutingSettingsApplier
import net.osmand.plus.helpers.RoutingSettingsProvider
import net.osmand.plus.helpers.RoutingUriQueryHandler
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.router.GeneralRouter
import net.osmand.router.RoutingConfiguration
import net.osmand.router.TestRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutingDeeplinkTest {

    @Test
    fun testRouteQueryParam() {
        val testCases = testData
        for (i in testCases.indices) {
            testRoutingQueryParam(i, testCases[i])
        }
    }


    private fun testRoutingQueryParam(caseIndex: Int, data: TestData) {
        // obtain prerequisites
        val router = getRouter(data.appMode)
        checkNotNull(router)
        val routingParams = RoutingHelperUtils.getParametersForDerivedProfile(data.appMode, router)
        val sourceSettings = mockSettings(routingParams, data.srcSettings.paramValues, data.srcSettings.isFastMode)

        // check query param creation and parsing
        val queryParam =
            RoutingUriQueryHandler.getRoutingParamsQueryValueForAppMode(data.appMode, sourceSettings)
        checkNotNull(queryParam)
        val parsed = RoutingUriQueryHandler.parseRoutingParamsQueryValue(queryParam)
        assertTrue(
            "case $caseIndex: parsed query param doesn't match expected",
            haveSameElements(data.expectedQueryParams, parsed)
        )

        // check query param applying
        val destSettings = mockSettings(routingParams, emptyMap(), true)
        RoutingUriQueryHandler.loadParamsIntoApp(data.appMode, parsed, destSettings, destSettings)
        assertEquals("case $caseIndex: fastMode doesn't match", data.expectedDestSettings.isFastMode, destSettings.isFastMode())
        for (q in data.expectedQueryParams) {
            if (q.key == "short_way") continue // special treatment: sets fast mode instead
            val rp = routingParams[q.key] ?: continue
            val expected = q.value
            val actual = destSettings.getCustomRoutingPropertyValue(q.key, rp)
            assertEquals("case $caseIndex: ${q.key} doesn't match", expected, actual)
        }
    }

    private fun <T> haveSameElements(a: List<T>, b: List<T>): Boolean {
        if (a.size != b.size) return false
        val countsA = a.groupingBy { it }.eachCount()
        val countsB = b.groupingBy { it }.eachCount()
        return countsA == countsB
    }

    private interface MockSettings : RoutingSettingsProvider, RoutingSettingsApplier

    private fun mockSettings(
        profileParams: Map<String, GeneralRouter.RoutingParameter>,
        props: Map<String, String>,
        isFastMode: Boolean
    ): MockSettings {
        return object : MockSettings {
            private val profileParameters = profileParams
            private var fastMode = isFastMode
            private val properties = props.toMutableMap()

            override fun getParametersForDerivedProfile() = profileParameters
            override fun isFastMode() = fastMode
            override fun getCustomRoutingBooleanPropertyValue(
                key: String,
                routingParam: GeneralRouter.RoutingParameter
            ): Boolean {
                if (properties.containsKey(key)) {
                    return properties[key].toBoolean()
                }
                return routingParam.defaultBoolean
            }

            override fun getCustomRoutingPropertyValue(
                key: String,
                routingParam: GeneralRouter.RoutingParameter
            ): String {
                if (properties.containsKey(key)) {
                    return properties[key]!!
                }
                return routingParam.defaultString
            }

            override fun setCustomRoutingBooleanPropertyValue(
                key: String,
                routingParam: GeneralRouter.RoutingParameter,
                value: Boolean
            ) {
                properties[key] = value.toString()
            }

            override fun setCustomRoutingPropertyValue(
                key: String,
                routingParam: GeneralRouter.RoutingParameter,
                value: String
            ) {
                properties[key] = value
            }

            override fun setFastMode(value: Boolean) {
                fastMode = value
            }
        }
    }


    private data class TestData(
        val appMode: ApplicationMode,
        val srcSettings: SettingsValues,
        val expectedQueryParams: List<RoutingUriQueryHandler.KeyValue>,
        val expectedDestSettings: SettingsValues
    ) {
        companion object {
            fun make(
                appMode: ApplicationMode,
                initialSettings: SettingsValues,
                expectedQueryParams: List<Pair<String, String>>,
                expectedSettings: SettingsValues
            ): TestData {
                return TestData(
                    appMode,
                    initialSettings,
                    expectedQueryParams.map { (k, v) -> RoutingUriQueryHandler.KeyValue(k, v) },
                    expectedSettings
                )
            }
        }
    }

    private data class SettingsValues(
        val isFastMode: Boolean,
        val paramValues: Map<String, String>,
    )

    private fun getRouter(mode: ApplicationMode): GeneralRouter? {
        val builder = RoutingConfiguration.getDefault()
        val memoryLimits = RoutingConfiguration.RoutingMemoryLimits(
            TestRouting.MEMORY_TEST_LIMIT,
            TestRouting.NATIVE_MEMORY_TEST_LIMIT
        )
        val config = builder.build(mode.routingProfile, memoryLimits)
        val router = config.router
        return router
    }


    /**
     *
     * defaults:
     *     car:
     *         avoid_ferries: false
     *         avoid_stairs: -
     *         avoid_unpaved: false
     *         height_obstacles: -
     *         short_way: false
     *     pedestrian:
     *         avoid_ferries: false
     *         avoid_stairs: false
     *         avoid_unpaved: false
     *         height_obstacles: true
     *         short_way: -
     *
     *  "fast mode" is true by default
     */
    /**
     *
     * defaults:
     *     car:
     *         avoid_ferries: false
     *         avoid_stairs: -
     *         avoid_unpaved: false
     *         height_obstacles: -
     *         short_way: false
     *     pedestrian:
     *         avoid_ferries: false
     *         avoid_stairs: false
     *         avoid_unpaved: false
     *         height_obstacles: true
     *         short_way: -
     *
     *  "fast mode" is true by default
     */
    private val testData = listOf(
        TestData.make(
            appMode = ApplicationMode.CAR,
            initialSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_ferries" to "true",
                    "avoid_stairs" to "true",
                    "avoid_unpaved" to "true",
                    "height_obstacles" to "true"
                )
            ),
            expectedQueryParams = listOf(
                "car" to "true",
                "avoid_ferries" to "true",
                "avoid_unpaved" to "true",
            ),
            expectedSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_ferries" to "true",
                    "avoid_stairs" to "false",
                    "avoid_unpaved" to "true",
                    "height_obstacles" to "false"
                )
            )
        ),
        TestData.make(
            appMode = ApplicationMode.CAR,
            initialSettings = SettingsValues(
                isFastMode = false,
                paramValues = mapOf(
                    "avoid_ferries" to "false",
                    "avoid_stairs" to "false",
                    "avoid_unpaved" to "false",
                    "height_obstacles" to "false"
                )
            ),
            expectedQueryParams = listOf(
                "car" to "true",
                "short_way" to "true",
            ),
            expectedSettings = SettingsValues(
                isFastMode = false,
                paramValues = mapOf(
                    "avoid_ferries" to "false",
                    "avoid_stairs" to "false",
                    "avoid_unpaved" to "false",
                    "height_obstacles" to "false"
                )
            )
        ),
        TestData.make(
            appMode = ApplicationMode.PEDESTRIAN,
            initialSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_ferries" to "true",
                    "avoid_stairs" to "true",
                    "avoid_unpaved" to "true",
                    "height_obstacles" to "true"
                )
            ),
            expectedQueryParams = listOf(
                "pedestrian" to "true",
                "avoid_ferries" to "true",
                "avoid_stairs" to "true",
                "avoid_unpaved" to "true",
            ),
            expectedSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_ferries" to "true",
                    "avoid_stairs" to "true",
                    "avoid_unpaved" to "true",
                    "height_obstacles" to "true"
                )
            )
        ),
        TestData.make(
            appMode = ApplicationMode.PEDESTRIAN,
            initialSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_unpaved" to "false",
                    "avoid_stairs" to "false",
                    "avoid_ferries" to "false",
                    "height_obstacles" to "false"
                )
            ),
            expectedQueryParams = listOf(
                "pedestrian" to "true",
                "height_obstacles" to "false",
            ),
            expectedSettings = SettingsValues(
                isFastMode = true,
                paramValues = mapOf(
                    "avoid_ferries" to "false",
                    "avoid_stairs" to "false",
                    "avoid_unpaved" to "false",
                    "height_obstacles" to "false"
                )
            )
        )
    )
}