package net.osmand.test.ui

import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.R
import net.osmand.plus.widgets.popup.AndroidDrawableIcon
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuColors
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuContent
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuDefaults
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuOption
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuTheme
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.colorAttr
import net.osmand.plus.widgets.popup.showComposeDropdownMenu
import net.osmand.plus.widgets.popup.toDropdownOption
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

	@Test
	fun testShowComposeDropdownMenuNullAnchorView() {
		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = null
		assertNull(showComposeDropdownMenu(displayData))
	}

	@Test
	fun testShowComposeDropdownMenuUnattachedAnchorView() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val anchorView = View(context)
		val displayData = PopUpMenuDisplayData().apply {
			this.anchorView = anchorView
			menuItems = emptyList()
		}
		assertNull(showComposeDropdownMenu(displayData))
	}

	@Test
	fun testShowComposeDropdownMenuWithDisplayDataSettings() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val anchorView = View(context)
		val displayData = PopUpMenuDisplayData().apply {
			this.anchorView = anchorView
			dropDownGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
			horizontalOffset = 12
			verticalOffset = 24
			bgColor = Color.DarkGray.toArgb()
			customDropDown = PopUpMenuDisplayData.CustomDropDown.TOP_DROPDOWN
		}
		assertNull(showComposeDropdownMenu(displayData))
	}

	@Test
	fun testToDropdownOptionsEmptyList() {
		val options = emptyList<PopUpMenuItem>().toDropdownOptions()
		assertTrue(options.isEmpty())
	}

	@Test
	fun testToDropdownOptionsNullMenuItems() {
		val displayData = PopUpMenuDisplayData()
		displayData.menuItems = null
		assertNull(displayData.menuItems?.toDropdownOptions(displayData))
	}

	@Test
	fun testPopUpMenuItemWithNullTitle() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item = PopUpMenuItem.Builder(context).create()
		val option = item.toDropdownOption()
		assertEquals("", option.title)
	}

	@Test
	fun testPopUpMenuItemDividersChain() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item1 = PopUpMenuItem.Builder(context).setTitle("First").create()
		val item2 = PopUpMenuItem.Builder(context).setTitle("Second").showTopDivider(true).create()
		val item3 = PopUpMenuItem.Builder(context).setTitle("Third").showTopDivider(false).create()
		val item4 = PopUpMenuItem.Builder(context).setTitle("Fourth").showTopDivider(true).create()

		val options = listOf(item1, item2, item3, item4).toDropdownOptions()
		assertEquals(4, options.size)
		assertTrue(options[0].showDividerAfter)
		assertFalse(options[1].showDividerAfter)
		assertTrue(options[2].showDividerAfter)
		assertFalse(options[3].showDividerAfter)
	}

	@Test
	fun testPopUpMenuItemSingleItemDivider() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item = PopUpMenuItem.Builder(context).setTitle("Solo").showTopDivider(true).create()
		val options = listOf(item).toDropdownOptions()
		assertEquals(1, options.size)
		assertFalse(options[0].showDividerAfter)
	}

	@Test
	fun testPopUpMenuItemTrailingBadge() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item = PopUpMenuItem.Builder(context)
			.setTitle("Subscription")
			.setTrailingBadge(null, "PRO", Color.Red.toArgb())
			.create()

		val option = item.toDropdownOption()
		assertEquals("PRO", option.trailingBadgeTitle)
		assertEquals(Color.Red, option.trailingBadgeColor)
		assertNull(option.trailingBadgeIcon)
	}

	@Test
	fun testPopUpMenuItemCheckboxLayoutVariants() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item = PopUpMenuItem.Builder(context).setTitle("Layer").create()

		val dataCheckbox = PopUpMenuDisplayData().apply {
			layoutId = R.layout.popup_menu_item_checkbox
		}
		assertTrue(item.toDropdownOption(dataCheckbox).isCheckbox)

		val dataFullDivider = PopUpMenuDisplayData().apply {
			layoutId = R.layout.popup_menu_item_full_divider_check_box
		}
		assertTrue(item.toDropdownOption(dataFullDivider).isCheckbox)
	}

	@Test
	fun testPopUpMenuItemDisabledWhenNoListeners() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val item = PopUpMenuItem.Builder(context).setTitle("Plain Item").create()
		val option = item.toDropdownOption()
		assertFalse(option.enabled)
	}

	@Test
	fun testDropdownMenuPositioningGravityRules() {
		val screenWidth = 1080
		val anchorWidth = 100

		val leftAnchorLocationX = 100
		val leftAnchorCenterX = leftAnchorLocationX + anchorWidth / 2
		val isLeftOnRight = leftAnchorCenterX > screenWidth / 2
		assertFalse(isLeftOnRight)

		val leftGravityLtr = if (isLeftOnRight) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
		val leftGravityRtl = if (isLeftOnRight) Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP
		assertEquals(Gravity.START or Gravity.TOP, leftGravityLtr)
		assertEquals(Gravity.END or Gravity.TOP, leftGravityRtl)

		val rightAnchorLocationX = 900
		val rightAnchorCenterX = rightAnchorLocationX + anchorWidth / 2
		val isRightOnRight = rightAnchorCenterX > screenWidth / 2
		assertTrue(isRightOnRight)

		val rightGravityLtr = if (isRightOnRight) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
		val rightGravityRtl = if (isRightOnRight) Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP
		assertEquals(Gravity.END or Gravity.TOP, rightGravityLtr)
		assertEquals(Gravity.START or Gravity.TOP, rightGravityRtl)
	}

	@Test
	fun testDropdownMenuPositioningMarginAndOffsets() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val metrics = context.resources.displayMetrics
		val screenMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, metrics).toInt()
		val screenWidth = metrics.widthPixels

		val leftAnchorX = 10
		val minHOffset = screenMarginPx - leftAnchorX
		val defaultLeftHOffset = screenMarginPx
		val clampedLeftHOffset = maxOf(defaultLeftHOffset + (-500), minHOffset)
		assertEquals(minHOffset, clampedLeftHOffset)

		val anchorWidth = 100
		val rightAnchorX = screenWidth - 120
		val maxHOffset = screenWidth - screenMarginPx - (rightAnchorX + anchorWidth)
		val defaultRightHOffset = -screenMarginPx
		val clampedRightHOffset = minOf(defaultRightHOffset + 500, maxHOffset)
		assertEquals(maxHOffset, clampedRightHOffset)
	}

	@Test
	fun testDropdownMenuPositioningHeightEstimationWithDividers() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val metrics = context.resources.displayMetrics

		var baseHeightDp = 16f + 16f
		val items = listOf(
			PopUpMenuItem.Builder(context).setTitle("1").create(),
			PopUpMenuItem.Builder(context).setTitle("2").showTopDivider(true).create()
		)
		for (item in items) {
			baseHeightDp += 48f
			if (item.shouldShowTopDivider()) {
				baseHeightDp += 8f
			}
		}
		assertEquals(32f + 48f * 2 + 8f, baseHeightDp, 0.01f)

		val expectedPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, baseHeightDp, metrics).toInt()
		assertTrue(expectedPx > 0)
	}

	@Test
	fun testDropdownMenuPositioningVerticalFlippingLogic() {
		val screenHeight = 1000
		val anchorY = 800
		val anchorHeight = 50
		val approxMenuHeightPx = 300

		val spaceBelow = screenHeight - (anchorY + anchorHeight)
		val spaceAbove = anchorY

		val shouldFlipAbove = spaceBelow < approxMenuHeightPx && spaceAbove > spaceBelow
		assertTrue(shouldFlipAbove)

		val forcedTop = PopUpMenuDisplayData.CustomDropDown.TOP_DROPDOWN
		assertTrue(forcedTop == PopUpMenuDisplayData.CustomDropDown.TOP_DROPDOWN)

		val forcedBottom = PopUpMenuDisplayData.CustomDropDown.BOTTOM_DROPDOWN
		assertFalse(forcedBottom == PopUpMenuDisplayData.CustomDropDown.TOP_DROPDOWN)
	}

	@Test
	fun testComposeDropdownMenuContentRendering() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val options = listOf(
			OsmAndDropdownMenuOption(value = "opt1", title = "First Option", description = "First Description"),
			OsmAndDropdownMenuOption(value = "opt2", title = "Second Option", trailingBadgeTitle = "PRO", enabled = false)
		)

		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val composeView = createTestComposeView(context)
			composeView.setContent {
				OsmAndDropdownMenuTheme {
					OsmAndDropdownMenuContent(
						options = options,
						onOptionSelected = {}
					)
				}
			}
		}
	}

	@Test
	fun testComposeDropdownMenuContentWithTitleHeader() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val options = listOf(
			OsmAndDropdownMenuOption(value = "item", title = "Choice")
		)

		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val composeView = createTestComposeView(context)
			composeView.setContent {
				OsmAndDropdownMenuTheme {
					OsmAndDropdownMenuContent(
						title = "Header Title",
						options = options,
						onOptionSelected = {}
					)
				}
			}
		}
	}

	@Test
	fun testComposeDropdownMenuContentEmptyList() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val composeView = createTestComposeView(context)
			composeView.setContent {
				OsmAndDropdownMenuTheme {
					OsmAndDropdownMenuContent(
						options = emptyList<OsmAndDropdownMenuOption<String>>(),
						onOptionSelected = {}
					)
				}
			}
		}
	}

	@Test
	fun testComposeAndroidDrawableIcon() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val drawable = ColorDrawable(android.graphics.Color.BLUE)

		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val composeView = createTestComposeView(context)
			composeView.setContent {
				AndroidDrawableIcon(
					drawable = drawable,
					modifier = Modifier.size(24.dp),
					tint = Color.Red
				)
			}
		}
	}

	@Test
	fun testComposeColorAttr() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			val composeView = createTestComposeView(context)
			composeView.setContent {
				colorAttr(android.R.attr.textColorPrimary)
			}
		}
	}

	private fun createTestComposeView(context: android.content.Context): ComposeView {
		val composeView = ComposeView(context)
		val lifecycleOwner = object : LifecycleOwner {
			private val registry = LifecycleRegistry(this).apply {
				currentState = Lifecycle.State.RESUMED
			}
			override val lifecycle: Lifecycle = registry
		}
		composeView.setViewTreeLifecycleOwner(lifecycleOwner)
		return composeView
	}
}
