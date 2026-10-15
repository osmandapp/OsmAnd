package net.osmand.test.junit

import android.annotation.SuppressLint
import androidx.car.app.HandshakeInfo
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.suggestion.model.Suggestion
import androidx.car.app.testing.TestCarContext
import androidx.car.app.versioning.CarAppApiLevels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.PointDescription
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarSuggestionsHelper
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class CarSuggestionsHelperPublishTest {

	companion object {
		private const val MAX_SUGGESTIONS = 5

		private const val TEST_ADDRESS = "Unter den Linden 1, Berlin"

		private const val BERLIN_LAT_A = 52.5200

		private const val BERLIN_LON_A = 13.4050

		private const val BERLIN_LAT_B = 52.5186

		private const val BERLIN_LON_B = 13.4083
	}

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: CarSuggestionsHelper

	private var savedNavigationHistory = true
	private var savedLastModifiedTime: Long = 0L
	private val testPrefix = "25977-test-" + UUID.randomUUID().toString() + "-"
	private val createdDescriptions = mutableListOf<PointDescription>()

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		settings = app.settings
		helper = CarSuggestionsHelper(app)

		savedNavigationHistory = settings.NAVIGATION_HISTORY.get()
		savedLastModifiedTime = app.searchHistoryHelper.lastModifiedTime
		settings.NAVIGATION_HISTORY.set(true)

		CarTestUtils.waitForAppInitialization(app)
		CarTestUtils.assumeHistoryHasRoom(app)
	}

	@After
	fun tearDown() {
		try {
			CarTestUtils.cleanupHistoryEntries(
				app = app,
				testPrefix = testPrefix,
				createdDescriptions = createdDescriptions
			)
		} finally {
			app.searchHistoryHelper.lastModifiedTime = savedLastModifiedTime
			settings.NAVIGATION_HISTORY.set(savedNavigationHistory)
		}
	}

	private fun assumePublishingPrerequisites() {
		assumeTrue("Android Auto feature must be available", InAppPurchaseUtils.isAndroidAutoAvailable(app))
		assumeTrue("Location permission must be granted", OsmAndLocationProvider.isLocationPermissionAvailable(app))
		assumeFalse(
			"Real navigation must not be active or paused",
			app.routingHelper.isFollowingMode || app.routingHelper.isPauseNavigation
		)
	}

	private fun createTestCarContext(apiLevel: Int): Pair<TestCarContext, CarTestUtils.RecordingSuggestionManager> {
		lateinit var carContext: TestCarContext
		lateinit var recordingManager: CarTestUtils.RecordingSuggestionManager
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, apiLevel))
			recordingManager = CarTestUtils.RecordingSuggestionManager(carContext)
			carContext.overrideCarService(SuggestionManager::class.java, recordingManager)
		}
		return Pair(carContext, recordingManager)
	}

	private fun updateOnMainSync(carContext: TestCarContext) {
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			helper.update(carContext)
		}
	}

	@Test
	fun updateDoesNotCallSuggestionManagerWhenApiLevelIsBelow5() {
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_4)

		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}ApiLevel4")
		CarTestUtils.addNewestEntry(app, pd, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)

		updateOnMainSync(carContext)

		assertTrue(
			"updateSuggestions must not be called when host API level is below LEVEL_5",
			recordingManager.publications.isEmpty()
		)
	}

	@Test
	fun updatePublishesNewestNavigationHistoryEntriesUpToLimit() {
		assumePublishingPrerequisites()
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_5)

		val pdA = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}A")
		val pdB = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}B")

		CarTestUtils.addNewestEntry(app, pdA, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)
		Thread.sleep(CarTestUtils.ENTRY_TIMESTAMP_INTERVAL_MS)

		CarTestUtils.addNewestEntry(app, pdB, BERLIN_LAT_B, BERLIN_LON_B, createdDescriptions)

		updateOnMainSync(carContext)

		assertEquals(1, recordingManager.publications.size)
		val suggestions = recordingManager.publications[0]

		assertTrue("Suggestions count must be between 1 and $MAX_SUGGESTIONS", suggestions.size in 1..MAX_SUGGESTIONS)
		assertTrue("Suggestions must contain at least the 2 test entries", suggestions.size >= 2)

		assertTrue("First suggestion card must be entry B", suggestions[0].identifier.contains(pdB.name))
		assertEquals(pdB.name, suggestions[0].title.toString())

		assertTrue("Second suggestion card must be entry A", suggestions[1].identifier.contains(pdA.name))
		assertEquals(pdA.name, suggestions[1].title.toString())

		for (suggestion in suggestions) {
			assertNotNull("Suggestion action must not be null", suggestion.action)
			assertTrue("Suggestion title must not be empty", suggestion.title.toString().isNotEmpty())
		}
	}

	@Test
	fun updatePublishesAddressAsSubtitleOrEmptyWhenMissing() {
		assumePublishingPrerequisites()
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_5)

		val pdNoAddress = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}NoAddress")
		CarTestUtils.addNewestEntry(app, pdNoAddress, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)
		Thread.sleep(CarTestUtils.ENTRY_TIMESTAMP_INTERVAL_MS)

		val pdWithAddress = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}WithAddress")
		createdDescriptions.add(pdWithAddress)
		val entryWithAddress = HistoryEntry(BERLIN_LAT_B, BERLIN_LON_B, pdWithAddress, HistorySource.NAVIGATION)
		entryWithAddress.setAddress(TEST_ADDRESS)
		entryWithAddress.markAsAccessed(CarTestUtils.nextNewestTime(app))
		app.searchHistoryHelper.addItemsToHistory(listOf(entryWithAddress))

		updateOnMainSync(carContext)

		assertEquals(1, recordingManager.publications.size)
		val suggestions = recordingManager.publications[0]

		val cardWithAddress = suggestions.firstOrNull { it.identifier.contains(pdWithAddress.name) }
		val cardNoAddress = suggestions.firstOrNull { it.identifier.contains(pdNoAddress.name) }

		assertNotNull("Suggestion card with address must be present", cardWithAddress)
		assertNotNull("Suggestion card without address must be present", cardNoAddress)

		assertEquals(TEST_ADDRESS, cardWithAddress!!.subtitle?.toString())

		assertNotNull("Subtitle of card without address must not be null", cardNoAddress!!.subtitle)
		assertEquals("", cardNoAddress.subtitle?.toString())
		assertNotEquals("null", cardNoAddress.subtitle?.toString())
	}

	@Test
	fun updateDeduplicatesConsecutiveCallsWithoutChanges() {
		assumePublishingPrerequisites()
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_5)

		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}Deduplicate")
		CarTestUtils.addNewestEntry(app, pd, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)

		updateOnMainSync(carContext)
		assertEquals(1, recordingManager.publications.size)

		updateOnMainSync(carContext)
		assertEquals(1, recordingManager.publications.size)
	}

	@Test
	fun updateRepublishesWhenNewHistoryEntryIsAdded() {
		assumePublishingPrerequisites()
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_5)

		val pdA = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}First")
		CarTestUtils.addNewestEntry(app, pdA, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)

		updateOnMainSync(carContext)
		assertEquals(1, recordingManager.publications.size)

		Thread.sleep(CarTestUtils.ENTRY_TIMESTAMP_INTERVAL_MS)

		val pdB = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}Newest")
		CarTestUtils.addNewestEntry(app, pdB, BERLIN_LAT_B, BERLIN_LON_B, createdDescriptions)

		updateOnMainSync(carContext)
		assertEquals(2, recordingManager.publications.size)

		val secondPublication = recordingManager.publications[1]
		assertTrue("Second publication must not be empty", secondPublication.isNotEmpty())
		assertTrue("Newest entry must occupy the first card slot", secondPublication[0].identifier.contains(pdB.name))
		assertEquals(pdB.name, secondPublication[0].title.toString())
		assertEquals(recordingManager.publications[0][0].action, secondPublication[1].action)
	}

	@Test
	fun updateExcludesTestEntriesWhenNavigationHistoryIsDisabled() {
		assumePublishingPrerequisites()
		val (carContext, recordingManager) = createTestCarContext(CarAppApiLevels.LEVEL_5)

		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}NavHistory")
		CarTestUtils.addNewestEntry(app, pd, BERLIN_LAT_A, BERLIN_LON_A, createdDescriptions)

		updateOnMainSync(carContext)
		assertEquals(1, recordingManager.publications.size)
		assertTrue(
			"First publication must contain the test entry",
			recordingManager.publications[0].any { it.identifier.contains(pd.name) }
		)

		settings.NAVIGATION_HISTORY.set(false)

		updateOnMainSync(carContext)
		assertEquals(2, recordingManager.publications.size)

		val secondPublication = recordingManager.publications[1]
		assertTrue(
			"Second publication must not contain any test entries when navigation history is disabled",
			secondPublication.none { it.identifier.contains(testPrefix) }
		)
	}
}
