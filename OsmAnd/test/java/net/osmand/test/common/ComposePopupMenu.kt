package net.osmand.test.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * The popup menus of the app (`PopUpMenu`, the card selectors) are the Compose
 * `OsmAndDropdownMenu`: their entries are semantics nodes, not views, so Espresso cannot see them.
 * A test that opens one keeps a [ComposeTestRule] from [rule] and drives the entries through here.
 */
object ComposePopupMenu {

	@JvmStatic
	fun rule(): ComposeTestRule = createEmptyComposeRule()

	@JvmStatic
	fun clickItem(rule: ComposeTestRule, title: String) {
		rule.onNodeWithText(title).performClick()
		rule.waitForIdle()
	}

	@JvmStatic
	fun assertItemDisplayed(rule: ComposeTestRule, title: String) {
		rule.onNodeWithText(title).assertIsDisplayed()
	}

	@JvmStatic
	fun assertNoItem(rule: ComposeTestRule, title: String) {
		rule.onNodeWithText(title).assertDoesNotExist()
	}
}
