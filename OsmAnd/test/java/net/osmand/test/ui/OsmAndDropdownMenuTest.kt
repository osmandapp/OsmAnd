package net.osmand.test.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.R
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuColors
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuDefaults
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuOption
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.toDropdownOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmAndDropdownMenuTest {

	@Test
	fun testDropdownMenuOptionDefaults() {
		val option = OsmAndDropdownMenuOption(
			value = "item_1",
			title = "Item Title"
		)

		assertEquals("item_1", option.value)
		assertEquals("Item Title", option.title)
		assertNull(option.iconId)
		assertNull(option.iconDrawable)
		assertNull(option.description)
		assertFalse(option.selected)
		assertNull(option.selectedColor)
		assertFalse(option.isCheckbox)
		assertTrue(option.enabled)
		assertFalse(option.showDividerAfter)
		assertFalse(option.titleBold)
		assertNull(option.titleColor)
		assertNull(option.trailingBadgeTitle)
	}

	@Test
	fun testDropdownMenuOptionCustomValues() {
		val option = OsmAndDropdownMenuOption(
			value = 42,
			title = "Custom Title",
			iconId = R.drawable.ic_action_settings,
			description = "Custom Description",
			selected = true,
			selectedColor = Color.Blue,
			isCheckbox = true,
			enabled = false,
			showDividerAfter = true,
			titleBold = true,
			titleColor = Color.Magenta,
			trailingBadgeTitle = "Get",
			trailingBadgeColor = Color.Cyan
		)

		assertEquals(42, option.value)
		assertEquals("Custom Title", option.title)
		assertEquals(R.drawable.ic_action_settings, option.iconId)
		assertEquals("Custom Description", option.description)
		assertTrue(option.selected)
		assertEquals(Color.Blue, option.selectedColor)
		assertTrue(option.isCheckbox)
		assertFalse(option.enabled)
		assertTrue(option.showDividerAfter)
		assertTrue(option.titleBold)
		assertEquals(Color.Magenta, option.titleColor)
		assertEquals("Get", option.trailingBadgeTitle)
		assertEquals(Color.Cyan, option.trailingBadgeColor)
	}

	@Test
	fun testDropdownMenuDefaultsOffset() {
		assertEquals(DpOffset(0.dp, 4.dp), OsmAndDropdownMenuDefaults.Offset)
	}

	@Test
	fun testDropdownMenuColors() {
		val colors = OsmAndDropdownMenuColors(
			background = Color.White,
			divider = Color.Gray,
			text = Color.Black,
			secondaryText = Color.DarkGray,
			icon = Color.Blue,
			selected = Color.Green,
			control = Color.Red
		)

		assertEquals(Color.White, colors.background)
		assertEquals(Color.Gray, colors.divider)
		assertEquals(Color.Black, colors.text)
		assertEquals(Color.DarkGray, colors.secondaryText)
		assertEquals(Color.Blue, colors.icon)
		assertEquals(Color.Green, colors.selected)
		assertEquals(Color.Red, colors.control)
	}


	@Test
	fun testPopUpMenuItemMapping() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item1 = PopUpMenuItem.Builder(context)
			.setTitle("Edit")
			.setSelected(true)
			.showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO)
			.create()

		val item2 = PopUpMenuItem.Builder(context)
			.setTitle("Delete")
			.showTopDivider(true)
			.setTitleBold(true)
			.create()

		val options = listOf(item1, item2).toDropdownOptions()
		assertEquals(2, options.size)

		val option1 = options[0]
		assertEquals("Edit", option1.title)
		assertTrue(option1.selected)
		assertTrue(option1.enabled)
		assertTrue(option1.showDividerAfter)

		val option2 = options[1]
		assertEquals("Delete", option2.title)
		assertTrue(option2.titleBold)
		assertFalse(option2.enabled)
		assertFalse(option2.showDividerAfter)

		val clickableItem = PopUpMenuItem.Builder(context)
			.setTitle("Clickable")
			.setOnClickListener { }
			.create()
		val clickableOptions = listOf(clickableItem).toDropdownOptions()
		assertTrue(clickableOptions[0].enabled)

		val checkboxItem = PopUpMenuItem.Builder(context)
			.setTitle("Weather Layer")
			.showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX)
			.create()
		val checkboxOptions = listOf(checkboxItem).toDropdownOptions()
		assertTrue(checkboxOptions[0].isCheckbox)
		assertFalse(checkboxOptions[0].selected)
		assertNull(checkboxOptions[0].selectedColor)

		val coloredItem = PopUpMenuItem.Builder(context)
			.setTitle("Car Profile")
			.setSelected(true)
			.showCompoundBtn(android.graphics.Color.BLUE, PopUpMenuItem.CompoundButtonType.RADIO)
			.create()
		val coloredOptions = listOf(coloredItem).toDropdownOptions()
		assertEquals(Color(android.graphics.Color.BLUE), coloredOptions[0].selectedColor)
	}

	@Test
	fun testPopUpMenuItemWithDisplayDataListener() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val displayData = PopUpMenuDisplayData()
		displayData.onItemClickListener = net.osmand.plus.widgets.popup.OnPopUpMenuItemClickListener { }

		val item = PopUpMenuItem.Builder(context)
			.setTitle("Category")
			.create()

		val options = listOf(item).toDropdownOptions(displayData)
		assertEquals(1, options.size)
		assertTrue(options[0].enabled)
	}
}
