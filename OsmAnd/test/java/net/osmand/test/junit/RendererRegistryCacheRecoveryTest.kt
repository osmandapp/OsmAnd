package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.render.RendererRegistry
import net.osmand.plus.render.RendererRegistry.DEFAULT_RENDER
import net.osmand.plus.render.RendererRegistry.ROUTES_RENDER
import net.osmand.plus.render.RendererRegistry.TOURING_VIEW
import net.osmand.render.RenderingRulesStorage
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Built-in rendering styles are read from cached copies that are rewritten only on app update, so
 * a damaged copy used to break the style and every style depending on it until then (#26306).
 * Each case damages one cached copy and expects the style to load completely from a restored copy.
 */
@RunWith(AndroidJUnit4::class)
class RendererRegistryCacheRecoveryTest {

	private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
	private val registry = RendererRegistry(app)
	private val damagedStyles = mutableSetOf<String>()

	@Before
	fun waitForAppInit() {
		// The app's own registry reads the cached styles while the app initializes
		val deadline = System.currentTimeMillis() + 60_000
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(100)
		}
		assertFalse("app is still initializing", app.isApplicationInitializing)
	}

	@After
	fun restoreCachedStyles() = damagedStyles.forEach { cachedFile(it).writeBytes(bundled(it)) }

	@Test
	fun styleCutInsideTagIsRestored() {
		assertRestored(DEFAULT_RENDER, bundled(DEFAULT_RENDER).copyOf(4096), DEFAULT_RENDER)
	}

	/** An empty file parses without an error. */
	@Test
	fun emptyDependencyIsRestored() {
		assertRestored(DEFAULT_RENDER, ByteArray(0), TOURING_VIEW)
	}

	/** A file cut off right after a complete tag parses without an error too. */
	@Test
	fun styleCutBetweenTagsIsRestored() {
		val bundled = bundled(DEFAULT_RENDER)
		val cut = Regex(">\r?\n").findAll(String(bundled, 0, 8192, Charsets.ISO_8859_1)).last().range.last + 1
		assertRestored(DEFAULT_RENDER, bundled.copyOf(cut), DEFAULT_RENDER)
	}

	@Test
	fun damagedAddonIsRestored() {
		val storage = assertRestored(ROUTES_RENDER, bundled(ROUTES_RENDER).copyOf(4096), DEFAULT_RENDER)
		// Defined only in routes.addon.render.xml, far past the cut
		assertNotNull("routes addon rules are missing", storage.getRenderingAttributeRule("climbingGradeAliensColor"))
	}

	/** A damaged copy may differ from the bundled style, so none of its values may outlive the restore. */
	@Test
	fun valuesOfDamagedCopyAreDiscarded() {
		val constant = "name=\"roadStylePale_streetColor_day\" value=\"#fefefe\""
		val text = String(bundled(DEFAULT_RENDER), Charsets.ISO_8859_1)
		val end = text.indexOf(constant) + constant.length
		assertTrue("constant not found in default.render.xml", end >= constant.length)
		val damaged = text.replace(constant, constant.replace("#fefefe", "#010203")).substring(0, end + 100)
		val storage = assertRestored(DEFAULT_RENDER, damaged.toByteArray(Charsets.ISO_8859_1), DEFAULT_RENDER)
		val constants = RenderingRulesStorage::class.java.getDeclaredField("renderingConstants")
				.apply { isAccessible = true }.get(storage) as Map<*, *>
		assertEquals("#fefefe", constants["roadStylePale_streetColor_day"])
	}

	@Test
	fun intactStyleIsNotRewritten() {
		val content = bundled(DEFAULT_RENDER) + "\n<!-- not rewritten -->\n".toByteArray()
		damage(DEFAULT_RENDER, content)
		assertDefaultRules(registry.getRenderer(TOURING_VIEW))
		assertArrayEquals("intact cached style was rewritten", content, cachedFile(DEFAULT_RENDER).readBytes())
	}

	/** User files are loaded as before: an addon without its closing tag must not break the built-in styles. */
	@Test
	fun userAddonWithoutClosingTagDoesNotBreakStyles() {
		val addon = File(cachedFile(DEFAULT_RENDER).parentFile, "renderer-recovery-test.addon.render.xml")
		try {
			addon.writeText("<renderingStyle name=\"recovery-test\" addon=\"yes\" depends=\"\" version=\"1\">\n\t<order>\n\t</order>\n")
			assertDefaultRules(registry.getRenderer(DEFAULT_RENDER))
		} finally {
			addon.delete()
		}
	}

	private fun assertRestored(style: String, damaged: ByteArray, renderer: String): RenderingRulesStorage {
		damage(style, damaged)
		val storage = assertDefaultRules(registry.getRenderer(renderer))
		assertArrayEquals("cached $style was not restored", bundled(style), cachedFile(style).readBytes())
		return storage
	}

	/** The attribute is defined far past the cuts used here, so a cut-off copy lacks it. */
	private fun assertDefaultRules(storage: RenderingRulesStorage?): RenderingRulesStorage {
		assertNotNull("style was not loaded", storage)
		assertNotNull("default style rules are missing", storage!!.getRenderingAttributeRule("pisteDefaultColor"))
		return storage
	}

	private fun damage(style: String, content: ByteArray) {
		damagedStyles += style
		cachedFile(style).writeBytes(content)
	}

	private fun cachedFile(style: String) = registry.getFileForInternalStyle(style)

	private fun bundled(style: String): ByteArray {
		val resource = registry.internalRenderers.getValue(style)
		return RenderingRulesStorage::class.java.getResourceAsStream(resource)!!.use { it.readBytes() }
	}
}
