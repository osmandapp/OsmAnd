package net.osmand.test.junit

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.keyevent.InputDevicesHelper
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomInputDevicesMergeTest {

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: InputDevicesHelper

	private var savedGlobalDevices: String? = null
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

	private fun setLegacyDevices(appMode: ApplicationMode, vararg devices: JSONObject) {
		val json = JSONObject().put("items", JSONArray(devices.toList()))
		settings.LEGACY_CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(appMode, json.toString())
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
