package net.osmand.test.junit

import android.os.Looper
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.keyevent.InputDevicesHelper
import net.osmand.plus.keyevent.assignment.KeyAssignment
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile
import net.osmand.plus.keyevent.listener.EventType
import net.osmand.plus.keyevent.listener.InputDevicesEventListener
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomInputDevicesMergeTest {

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: InputDevicesHelper

	private var savedGlobalDevices: String? = null
	private var savedMergedDevices: String? = null
	private val savedLegacyDevices = mutableMapOf<ApplicationMode, String>()
	private val savedSelectedDevices = mutableMapOf<ApplicationMode, String>()

	@Before
	fun setUp() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		settings = app.settings
		helper = app.inputDeviceHelper

		if (settings.CUSTOM_EXTERNAL_INPUT_DEVICES.isSet) {
			savedGlobalDevices = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.get()
		}
		for (appMode in ApplicationMode.allPossibleValues()) {
			if (settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.isSetForMode(appMode)) {
				savedLegacyDevices[appMode] = settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.getModeValue(appMode)
			}
			if (settings.EXTERNAL_INPUT_DEVICE.isSetForMode(appMode)) {
				savedSelectedDevices[appMode] = settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode)
			}
			settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.resetModeToDefault(appMode)
		}
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.resetToDefault()
		if (settings.MERGED_LEGACY_INPUT_DEVICES.isSet) {
			savedMergedDevices = settings.MERGED_LEGACY_INPUT_DEVICES.get()
		}
		settings.MERGED_LEGACY_INPUT_DEVICES.resetToDefault()
		InstrumentationRegistry.getInstrumentation().waitForIdleSync()
		helper.releaseCustomizationCollection()
	}

	@After
	fun tearDown() {
		val globalDevices = savedGlobalDevices
		if (globalDevices != null) {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalDevices)
		} else {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.resetToDefault()
		}
		val mergedDevices = savedMergedDevices
		if (mergedDevices != null) {
			settings.MERGED_LEGACY_INPUT_DEVICES.set(mergedDevices)
		} else {
			settings.MERGED_LEGACY_INPUT_DEVICES.resetToDefault()
		}
		for (appMode in ApplicationMode.allPossibleValues()) {
			val legacyDevices = savedLegacyDevices[appMode]
			if (legacyDevices != null) {
				settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(appMode, legacyDevices)
			} else {
				settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.resetModeToDefault(appMode)
			}
			val selectedDevice = savedSelectedDevices[appMode]
			if (selectedDevice != null) {
				settings.EXTERNAL_INPUT_DEVICE.setModeValue(appMode, selectedDevice)
			} else {
				settings.EXTERNAL_INPUT_DEVICE.resetModeToDefault(appMode)
			}
		}
		helper.releaseCustomizationCollection()
	}

	@Test
	fun duplicateIsKeptWhenAnotherProfileHasOneCopy() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		setLegacyDevices(ApplicationMode.BICYCLE,
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard X (1)", KeyEvent.KEYCODE_A))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_2")

		assertEquals(listOf("custom_1", "custom_2"), customDeviceIds(ApplicationMode.CAR))
		assertEquals("custom_2", settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.BICYCLE))
	}

	@Test
	fun identicalCopiesFromProfilesBecomeOneDeviceAndKeepSelection() {
		val devices = arrayOf(
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard X (1)", KeyEvent.KEYCODE_A))
		setLegacyDevices(ApplicationMode.CAR, *devices)
		setLegacyDevices(ApplicationMode.BICYCLE, *devices)
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_2")

		assertEquals(listOf("custom_1", "custom_2"), customDeviceIds(ApplicationMode.BICYCLE))
		assertEquals("custom_2", settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.BICYCLE))
		assertFalse(settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.isSetForMode(ApplicationMode.CAR))
		assertFalse(settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.isSetForMode(ApplicationMode.BICYCLE))
	}

	@Test
	fun mergeTriggeredByAnotherCollectionRefreshesCachedDevices() {
		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))

		setLegacyDevices(ApplicationMode.BICYCLE, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		helper.reloadFunctionalityCollection(ApplicationMode.CAR)

		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun repeatedImportOfSameListAddsNothing() {
		val device = device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A, null)
		setLegacyDevices(ApplicationMode.CAR, device)
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))

		setLegacyDevices(ApplicationMode.BICYCLE, device)
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun divergedCopyIsAddedUnderNewNameAndSelectionFollowsIt() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		setLegacyDevices(ApplicationMode.BICYCLE, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_1")

		val firstSelected = helper.getSelectedDevice(ApplicationMode.BICYCLE)
		assertEquals(listOf(KeyEvent.KEYCODE_B), firstSelected.assignmentsCopy.flatMap { it.keyCodes })
		val devices = customDevices(ApplicationMode.CAR)
		assertEquals(2, devices.size)
		assertEquals(2, devices.map { it.toHumanString(app) }.toSet().size)
		assertEquals(listOf(KeyEvent.KEYCODE_A), selectedDeviceKeyCodes(devices, ApplicationMode.CAR))
		assertEquals(listOf(KeyEvent.KEYCODE_B), selectedDeviceKeyCodes(devices, ApplicationMode.BICYCLE))
	}

	@Test
	fun emptyLegacyListsDoNotWriteGlobalDevices() {
		settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(ApplicationMode.CAR, "")
		settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(ApplicationMode.BICYCLE, "")

		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))
		assertFalse(settings.CUSTOM_EXTERNAL_INPUT_DEVICES.isSet)
		assertFalse(settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.isSetForMode(ApplicationMode.CAR))
	}

	@Test
	fun legacyDeviceWithIdOfGlobalListAndOtherKeysIsKeptAsCopy() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")

		val devices = customDevices(ApplicationMode.CAR)

		assertEquals(listOf("custom_1", "custom_1_" + ApplicationMode.CAR.stringKey), devices.map { it.id })
		assertEquals(listOf(KeyEvent.KEYCODE_B), selectedDeviceKeyCodes(devices, ApplicationMode.CAR))
	}

	@Test
	fun sameLegacyDeviceImportedAgainIsIgnored() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		val firstIds = customDeviceIds(ApplicationMode.CAR)

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		helper.releaseCustomizationCollection()
		val devices = customDevices(ApplicationMode.CAR)

		assertEquals(firstIds, devices.map { it.id })
		assertEquals(listOf(KeyEvent.KEYCODE_B), selectedDeviceKeyCodes(devices, ApplicationMode.CAR))
	}

	@Test
	fun divergedCopiesImportedOneAfterAnotherStaySeparate() {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_1")
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))

		setLegacyDevices(ApplicationMode.BICYCLE, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		helper.releaseCustomizationCollection()
		val devices = customDevices(ApplicationMode.CAR)

		assertEquals(2, devices.size)
		assertEquals(listOf(KeyEvent.KEYCODE_A), selectedDeviceKeyCodes(devices, ApplicationMode.CAR))
		assertEquals(listOf(KeyEvent.KEYCODE_B), selectedDeviceKeyCodes(devices, ApplicationMode.BICYCLE))
	}

	@Test
	fun legacyDeviceEditedOnOlderClientArrivesAsCopy() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		helper.releaseCustomizationCollection()

		assertEquals(2, customDeviceIds(ApplicationMode.CAR).size)
	}

	@Test
	fun divergedCopyGetsDeterministicId() {
		val first = mergeDivergedCopyAndReturnIds()
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.resetToDefault()
		helper.releaseCustomizationCollection()
		val second = mergeDivergedCopyAndReturnIds()

		assertEquals(listOf("custom_1", "custom_1_" + ApplicationMode.CAR.stringKey), first)
		assertEquals(first, second)
	}

	@Test
	fun selectedCustomDeviceJsonContainsOnlySelectedDevice() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard Y", KeyEvent.KEYCODE_B)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_2")

		val items = JSONObject(helper.getSelectedCustomDeviceJson(ApplicationMode.CAR)!!).getJSONArray("items")
		assertEquals(1, items.length())
		assertEquals("custom_2", items.getJSONObject(0).getString("id"))

		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, KeyboardDeviceProfile.ID)
		assertNull(helper.getSelectedCustomDeviceJson(ApplicationMode.CAR))
	}

	@Test
	fun removeCustomDeviceResetsSelectionInOtherProfiles() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_1")

		helper.removeCustomDevice(ApplicationMode.CAR, "custom_1")

		assertEquals(KeyboardDeviceProfile.ID, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
		assertEquals(KeyboardDeviceProfile.ID, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.BICYCLE))
		assertTrue(settings.EXTERNAL_INPUT_DEVICE.isSetForMode(ApplicationMode.CAR))
		assertTrue(settings.EXTERNAL_INPUT_DEVICE.isSetForMode(ApplicationMode.BICYCLE))
	}

	@Test
	fun writeToGlobalPreferenceOutsideHelperRefreshesDevices() {
		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))

		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.runOnMainSync {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		}
		instrumentation.waitForIdleSync()

		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")

		assertNotNull(helper.getDeviceById(ApplicationMode.CAR, "custom_1"))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		assertNotNull(helper.getFunctionalityDevice(ApplicationMode.CAR)!!.findAction(KeyEvent.KEYCODE_A))
	}

	@Test
	fun remergeAfterSplitRepointsSelectionToMatchingContent() {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.BICYCLE, "custom_1")
		mergeDivergedCopyAndReturnIds()
		val splitId = "custom_1_" + ApplicationMode.CAR.stringKey
		assertEquals(splitId, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		helper.releaseCustomizationCollection()

		customDevices(ApplicationMode.CAR)

		assertEquals(splitId, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
	}

	@Test
	fun remergeAfterSplitFollowsEditedSplitDevice() {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		mergeDivergedCopyAndReturnIds()
		val splitId = "custom_1_" + ApplicationMode.CAR.stringKey
		helper.addAssignment(ApplicationMode.CAR, splitId, KeyAssignment("zoom_in", KeyEvent.KEYCODE_Z))
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1", splitId), customDeviceIds(ApplicationMode.CAR))
		assertEquals(splitId, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
	}

	@Test
	fun globalListIsSavedBeforeLegacyListIsCleared() {
		val events = mutableListOf<String>()
		val globalListener = StateChangedListener<String> { events.add("global") }
		val legacyListener = StateChangedListener<String> { events.add("legacy") }
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.addListener(globalListener)
		settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.addListener(legacyListener)

		customDeviceIds(ApplicationMode.CAR)

		assertEquals(listOf("global", "legacy"), events)
	}

	@Test
	fun divergedCopyKeepsOwnIdWhenOnlyNameIsTaken() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		setLegacyDevices(ApplicationMode.CAR, device("custom_2", "Keyboard X", KeyEvent.KEYCODE_B))

		val devices = customDevices(ApplicationMode.CAR)

		assertEquals(listOf("custom_1", "custom_2"), devices.map { it.id })
		assertEquals(2, devices.map { it.toHumanString(app) }.toSet().size)
	}

	@Test
	fun editsKeepSeeingTheSameDeviceInstance() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.waitForIdleSync()
		val selected = helper.getSelectedDevice(ApplicationMode.CAR)
		val before = selected.assignmentsCopy.size

		helper.addAssignment(ApplicationMode.CAR, "custom_1", KeyAssignment("back_to_location", KeyEvent.KEYCODE_C))
		instrumentation.waitForIdleSync()
		helper.addAssignment(ApplicationMode.CAR, "custom_1", KeyAssignment("back_to_location", KeyEvent.KEYCODE_D))
		instrumentation.waitForIdleSync()

		assertEquals(before + 2, selected.assignmentsCopy.size)
	}

	@Test
	fun staleLegacyCopyDoesNotDuplicateEditedDevice() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.addAssignment(ApplicationMode.CAR, "custom_1", KeyAssignment("zoom_in", KeyEvent.KEYCODE_Z))

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		assertEquals("custom_1", settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
	}

	@Test
	fun removedDeviceDoesNotComeBackFromSameLegacyList() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.removeCustomDevice(ApplicationMode.CAR, "custom_1")

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		helper.releaseCustomizationCollection()

		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun removedDeviceStaysAwayWhenLegacyListGetsNewDevice() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.removeCustomDevice(ApplicationMode.CAR, "custom_1")

		setLegacyDevices(ApplicationMode.CAR,
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard Y", KeyEvent.KEYCODE_B))
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_2"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun fileImportRestoresRemovedDevice() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.removeCustomDevice(ApplicationMode.CAR, "custom_1")

		helper.forgetRemovedMergedDevices()
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun selectionOfRemovedDeviceFallsBackToKeyboardOnLegacyImport() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.removeCustomDevice(ApplicationMode.CAR, "custom_1")

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		helper.releaseCustomizationCollection()
		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))

		assertEquals(KeyboardDeviceProfile.ID, settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
	}

	@Test
	fun importOfLastOwnWrittenListIsAppliedAfterForeignList() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
		helper.addAssignment(ApplicationMode.CAR, "custom_1", KeyAssignment("zoom_in", KeyEvent.KEYCODE_Z))
		val ownJson = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.get()
		val instrumentation = InstrumentationRegistry.getInstrumentation()

		instrumentation.runOnMainSync {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(device("custom_2", "Keyboard Y", KeyEvent.KEYCODE_B)))
		}
		instrumentation.waitForIdleSync()
		assertEquals(listOf("custom_2"), customDeviceIds(ApplicationMode.CAR))

		instrumentation.runOnMainSync {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(ownJson)
		}
		instrumentation.waitForIdleSync()
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun legacyListWrittenOutsideHelperRefreshesDevices() {
		assertEquals(emptyList<String>(), customDeviceIds(ApplicationMode.CAR))

		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.runOnMainSync {
			setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		}
		instrumentation.waitForIdleSync()

		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun duplicateWithSameKeysDoesNotSwitchEditedDevice() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard X 1", KeyEvent.KEYCODE_A)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		helper.releaseCustomizationCollection()
		customDeviceIds(ApplicationMode.CAR)
		helper.addAssignment(ApplicationMode.CAR, "custom_1", KeyAssignment("zoom_in", KeyEvent.KEYCODE_Z))

		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1", "custom_2"), customDeviceIds(ApplicationMode.CAR))
		assertEquals("custom_1", settings.EXTERNAL_INPUT_DEVICE.getModeValue(ApplicationMode.CAR))
	}

	@Test
	fun externalChangeOfGlobalListNotifiesListeners() {
		val events = java.util.Collections.synchronizedList(mutableListOf<EventType>())
		val latch = CountDownLatch(1)
		val listener = InputDevicesEventListener { _, event ->
			events.add(event)
			latch.countDown()
		}
		helper.addListener(listener)
		try {
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(
				device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
			assertTrue(latch.await(5, TimeUnit.SECONDS))
		} finally {
			helper.removeListener(listener)
		}
		assertTrue(events.contains(EventType.CUSTOM_DEVICES_CHANGED))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))
	}

	@Test
	fun ownChangeOfGlobalListDoesNotRaiseExternalChangeEvent() {
		val events = java.util.Collections.synchronizedList(mutableListOf<EventType>())
		val listener = InputDevicesEventListener { _, event -> events.add(event) }
		helper.addListener(listener)
		try {
			helper.createAndSaveCustomDevice(ApplicationMode.CAR, "Keyboard Z")
			InstrumentationRegistry.getInstrumentation().waitForIdleSync()
		} finally {
			helper.removeListener(listener)
		}
		assertTrue(events.contains(EventType.ADD_NEW_DEVICE))
		assertFalse(events.contains(EventType.CUSTOM_DEVICES_CHANGED))
	}

	@Test
	fun onlyUserFacingExportCarriesSelectedDevice() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_1")
		val key = settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.id

		assertTrue(exportProfileFileText(true).contains(key))
		assertFalse(exportProfileFileText(false).contains(key))
	}

	@Test
	fun profileExportWritesSelectedDeviceOnlyWhenRequested() {
		settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(globalJson(
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard Y", KeyEvent.KEYCODE_B)))
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, "custom_2")
		val key = settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.id

		val cloud = exportProfileJson(false)
		assertFalse(cloud.has(key))

		val file = exportProfileJson(true)
		val items = JSONObject(file.getString(key)).getJSONArray("items")
		assertEquals(1, items.length())
		assertEquals("custom_2", items.getJSONObject(0).getString("id"))

		settings.EXTERNAL_INPUT_DEVICE.setModeValue(ApplicationMode.CAR, KeyboardDeviceProfile.ID)
		assertFalse(exportProfileJson(true).has(key))
	}

	@Test
	fun knownMergedDeviceIsNotReusedForItsDuplicateInSameList() {
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		assertEquals(listOf("custom_1"), customDeviceIds(ApplicationMode.CAR))

		setLegacyDevices(ApplicationMode.CAR,
			device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A),
			device("custom_2", "Keyboard Y", KeyEvent.KEYCODE_A))
		helper.releaseCustomizationCollection()

		assertEquals(listOf("custom_1", "custom_2"), customDeviceIds(ApplicationMode.CAR))
	}

	private fun exportProfileFileText(shared: Boolean): String {
		val dir = app.cacheDir
		val name = "input_devices_export_" + shared
		val file = File(dir, name + ".osf")
		file.delete()
		val latch = CountDownLatch(1)
		var succeed = false
		val listener = SettingsExportListener { _, result ->
			succeed = result
			latch.countDown()
		}
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val items = mutableListOf<SettingsItem>(ProfileSettingsItem(app, ApplicationMode.CAR))
			if (shared) {
				app.fileSettingsHelper.exportSharedSettings(dir, name, listener, items)
			} else {
				app.fileSettingsHelper.exportSettings(dir, name, listener, items, true)
			}
		}
		assertTrue(latch.await(30, TimeUnit.SECONDS))
		assertTrue(succeed)
		try {
			ZipFile(file).use { zip ->
				return zip.entries().toList().joinToString("\n") { entry ->
					zip.getInputStream(entry).bufferedReader().readText()
				}
			}
		} finally {
			file.delete()
		}
	}

	private fun exportProfileJson(writeSelectedDevice: Boolean): JSONObject {
		val item = ProfileSettingsItem(app, ApplicationMode.CAR)
		if (writeSelectedDevice) {
			item.enableSelectedInputDeviceExport()
		}
		val out = ByteArrayOutputStream()
		item.writer!!.writeToStream(out, null)
		return JSONObject(out.toString("UTF-8"))
	}

	private fun mergeDivergedCopyAndReturnIds(): List<String> {
		setLegacyDevices(ApplicationMode.BICYCLE, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_A))
		setLegacyDevices(ApplicationMode.CAR, device("custom_1", "Keyboard X", KeyEvent.KEYCODE_B))
		return customDeviceIds(ApplicationMode.CAR)
	}

	private fun globalJson(vararg devices: JSONObject): String {
		return JSONObject().put("items", JSONArray(devices.toList())).toString()
	}

	private fun setLegacyDevices(appMode: ApplicationMode, vararg devices: JSONObject) {
		val json = JSONObject().put("items", JSONArray(devices.toList()))
		settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(appMode, json.toString())
		if (Looper.myLooper() != Looper.getMainLooper()) {
			InstrumentationRegistry.getInstrumentation().waitForIdleSync()
		}
	}

	private fun device(id: String, name: String, keyCode: Int,
	                   assignmentId: String? = "key_assignment_1"): JSONObject {
		val assignment = JSONObject()
			.put("commandId", "back_to_location")
			.put("keycodes", JSONArray().put(JSONObject().put("keycode", keyCode)))
		if (assignmentId != null) {
			assignment.put("id", assignmentId)
		}
		return JSONObject()
			.put("id", id)
			.put("name", name)
			.put("assignments", JSONArray().put(assignment))
	}

	private fun selectedDeviceKeyCodes(devices: List<CustomInputDeviceProfile>,
	                                   appMode: ApplicationMode): List<Int> {
		val selectedId = settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode)
		return devices.first { it.id == selectedId }.assignmentsCopy.flatMap { it.keyCodes }
	}

	private fun customDevices(appMode: ApplicationMode): List<CustomInputDeviceProfile> {
		return helper.getAllDevices(appMode).filterIsInstance<CustomInputDeviceProfile>()
	}

	private fun customDeviceIds(appMode: ApplicationMode): List<String> {
		return customDevices(appMode).map { it.id }
	}
}
