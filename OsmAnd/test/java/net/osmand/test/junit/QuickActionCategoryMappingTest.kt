package net.osmand.test.junit

import net.osmand.plus.quickaction.MapButtonsHelper
import net.osmand.plus.quickaction.QuickActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class QuickActionCategoryMappingTest {

	@Test
	fun categoryIdsResolveToExistingHeaderInstances() {
		val categories = listOf(
			QuickActionType.CREATE_CATEGORY to MapButtonsHelper.TYPE_ADD_ITEMS,
			QuickActionType.CONFIGURE_MAP to MapButtonsHelper.TYPE_CONFIGURE_MAP,
			QuickActionType.NAVIGATION to MapButtonsHelper.TYPE_NAVIGATION,
			QuickActionType.CONFIGURE_SCREEN to MapButtonsHelper.TYPE_CONFIGURE_SCREEN,
			QuickActionType.SETTINGS to MapButtonsHelper.TYPE_SETTINGS,
			QuickActionType.MAP_INTERACTIONS to MapButtonsHelper.TYPE_MAP_INTERACTIONS,
			QuickActionType.MY_PLACES to MapButtonsHelper.TYPE_MY_PLACES,
			QuickActionType.INTERFACE to MapButtonsHelper.TYPE_INTERFACE
		)

		for ((categoryId, header) in categories) {
			assertSame("Category $categoryId", header,
				MapButtonsHelper.getCategoryActionTypeFromId(categoryId))
			assertEquals(categoryId, header.category)
			assertEquals(0, header.id)
		}
	}

	@Test
	fun invalidCategoryIdsReturnNull() {
		for (categoryId in listOf(Int.MIN_VALUE, -1, 8, Int.MAX_VALUE)) {
			assertNull("Category $categoryId",
				MapButtonsHelper.getCategoryActionTypeFromId(categoryId))
		}
	}
}
