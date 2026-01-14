package net.osmand.test.ui

import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import org.junit.Assert
import org.junit.Test

class OrganizeByTypeTest {
	private val context = InstrumentationRegistry.getInstrumentation().targetContext

	@Test
	fun testOrganizeByGroupTypeResources() {
		OrganizeByCategory.entries.forEach { group ->
			val nameResId = getStringIdByName(group.nameResId)
			val name = context.getString(nameResId)
			Assert.assertNotNull(name)
			Assert.assertTrue(name.isNotEmpty())
		}
	}

	@Test
	fun testOrganizeByTypeResources() {
		OrganizeByType.entries.forEach { type ->
			// Test Name
			val nameResId = getStringIdByName(type.nameResId)
			val name = context.getString(nameResId)
			Assert.assertNotNull(name)
			Assert.assertTrue(name.isNotEmpty())

			// Test Icon
			val iconName = type.getIconName()
			val iconRes = getDrawableIdByName(iconName)
			val icon = context.resources.getDrawable(iconRes, context.theme)
			Assert.assertNotNull(icon)

		}
	}

	private fun getDrawableIdByName(resName: String): Int {
		return context.resources.getIdentifier(resName, "drawable", context.packageName)
	}

	private fun getStringIdByName(resName: String): Int {
		return context.resources.getIdentifier(resName, "string", context.packageName)
	}
}