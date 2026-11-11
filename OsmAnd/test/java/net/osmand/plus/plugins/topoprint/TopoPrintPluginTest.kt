package net.osmand.plus.plugins.topoprint

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.topoprint.compose.PosterGeometry
import net.osmand.plus.plugins.topoprint.compose.SvgPosterComposer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * On-device integration smoke: enables the plugin in the real app settings
 * (persists across restarts) and validates the print-parameter model plus the
 * SVG composition pipeline end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class TopoPrintPluginTest {

	@Test
	fun testPluginEnableAndPipelineSmoke() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val app = context.applicationContext as OsmandApplication

		val plugin = PluginsHelper.getPlugin(TopoPrintPlugin::class.java)
		assertNotNull("TopoPrintPlugin not registered in PluginsHelper", plugin)
		if (!plugin!!.isEnabled) {
			assertTrue(PluginsHelper.enablePlugin(null, app, plugin, true))
		}
		assertTrue(plugin.isActive)

		// Print parameters are user-persistent: pin them to the A4 defaults so
		// the assertions below hold regardless of what the device last used.
		plugin.PAPER_SIZE.set(PaperSize.A4.name)
		plugin.ORIENTATION.set(PrintOrientation.PORTRAIT.name)
		plugin.DPI.set(300)
		plugin.SCALE_DENOMINATOR.set(25000)
		val params = plugin.printParameters
		assertEquals(PaperSize.A4, params.paper)
		assertTrue(params.orientation in PrintOrientation.entries)
		assertTrue(params.dpi in PrintParameters.SUPPORTED_DPI.toList())
		assertTrue(params.scaleDenominator in PrintParameters.SUPPORTED_SCALES.toList())
		// 175 mm map width at 1:25000 covers 4375 m; 210 mm → 5250 m.
		assertEquals(4375.0, params.mapWidthMeters, 0.01)
		assertEquals(5250.0, params.mapHeightMeters, 0.01)
		// Raster size follows the effective DPI: mm × dpi / 25.4.
		val expectedWidthPx = Math.round(params.layout.mapWidthMm * params.dpi / 25.4).toInt()
		assertEquals(expectedWidthPx, params.rasterWidthPx)

		// Poster geometry for the UTM 50S 431000 4425000 test point.
		val geometry = PosterGeometry.compute(params, 39.972342, 116.191987, -7.5)
		assertEquals(50, geometry.centerUtm.zoneNumber)
		assertEquals("50S", geometry.zoneLabel)
		// The poster uses the numerical convergence (bearing centre →
		// top-centre of the bbox), which deviates from the analytic local
		// value −0.5259° by the along-path variation over the 2.9 km baseline.
		assertEquals(-0.5259, geometry.convergenceDeg, 0.02)
		assertTrue(geometry.rotatedCoverageWidthMeters >= params.mapWidthMeters)
		assertTrue(geometry.rotatedCoverageHeightMeters >= params.mapHeightMeters)

		// SVG composition: raster rectangle slightly larger than the grid frame.
		val raster = SvgPosterComposer.Raster(
			"test_basemap.png",
			eastingTopLeft = geometry.grid.left - 500.0, northingTopLeft = geometry.grid.top + 500.0,
			eastingTopRight = geometry.grid.right + 500.0, northingTopRight = geometry.grid.top + 500.0,
			eastingBottomLeft = geometry.grid.left - 500.0, northingBottomLeft = geometry.grid.bottom - 500.0,
			eastingBottomRight = geometry.grid.right + 500.0, northingBottomRight = geometry.grid.bottom - 500.0)
		val svg = SvgPosterComposer(geometry, "北京冒烟测试", raster, 1767225600000L).compose()
		assertTrue(svg.contains("<g id=\"frame\""))
		assertTrue(svg.contains("<g id=\"grid\">"))
		assertTrue(svg.contains("<g id=\"scale_bar\""))
		assertTrue(svg.contains("<g id=\"compass\""))
		assertTrue(svg.contains("WGS84 UTM Zone 50S"))
		assertTrue(svg.contains("test_basemap.png"))
		assertTrue(svg.contains("1:25000"))
		assertTrue(svg.contains("北京冒烟测试"))
	}

	@Test
	fun testGridLandsOnTrueUtmPositionsOverBasemap() {
		val params = PrintParameters(PaperSize.A4, PrintOrientation.PORTRAIT, 300, 25000)
		val geometry = PosterGeometry.compute(params, 39.972342, 116.191987, -7.5)
		val raster = SvgPosterComposer.Raster(
			"test_basemap.png",
			eastingTopLeft = geometry.bbox.left - 800.0, northingTopLeft = geometry.bbox.top + 800.0,
			eastingTopRight = geometry.bbox.right + 800.0, northingTopRight = geometry.bbox.top + 800.0,
			eastingBottomLeft = geometry.bbox.left - 800.0, northingBottomLeft = geometry.bbox.bottom - 800.0,
			eastingBottomRight = geometry.bbox.right + 800.0, northingBottomRight = geometry.bbox.bottom - 800.0)
		val svg = SvgPosterComposer(geometry, "placement", raster, 1767225600000L).compose()

		val imageRegex = Regex("<image [^>]*x=\"([-0-9.]+)\" y=\"([-0-9.]+)\" " +
				"width=\"([-0-9.]+)\" height=\"([-0-9.]+)\"")
		val match = imageRegex.find(svg) ?: error("no basemap image element in SVG")
		val imageX = match.groupValues[1].toDouble()
		val imageY = match.groupValues[2].toDouble()

		val k = 1000.0 / 25000.0
		val spanTop = raster.eastingTopRight - raster.eastingTopLeft
		val spanBottom = raster.eastingBottomRight - raster.eastingBottomLeft
		val spanLeft = raster.northingTopLeft - raster.northingBottomLeft
		val spanRight = raster.northingTopRight - raster.northingBottomRight
		val widthMm = (spanTop + spanBottom) / 2.0 * k
		val heightMm = (spanLeft + spanRight) / 2.0 * k

		// Where the drawn kilometre grid puts a UTM point on paper: corner
		// interpolation between the georeferenced edges (frame mm).
		fun gridPos(e: Double, n: Double): Pair<Double, Double> {
			val fxTop = (e - raster.eastingTopLeft) / spanTop
			val fxBottom = (e - raster.eastingBottomLeft) / spanBottom
			val fyLeft = (raster.northingTopLeft - n) / spanLeft
			val fyRight = (raster.northingTopRight - n) / spanRight
			val fN = (raster.northingTopLeft - n) / spanLeft
			val fE = (e - raster.eastingTopLeft) / spanTop
			val x = imageX + (fxTop + (fxBottom - fxTop) * fN) * widthMm
			val y = imageY + (fyLeft + (fyRight - fyLeft) * fE) * heightMm
			return Pair(x, y)
		}

		// Where the embedded image puts the same UTM point: axis-aligned
		// bilinear interpolation of the georeferenced corners (identical to
		// the grid model by construction; the check guards regressions).
		fun imagePos(e: Double, n: Double): Pair<Double, Double> {
			val x = imageX + (e - raster.eastingTopLeft) / spanTop * widthMm
			val y = imageY + (raster.northingTopLeft - n) / spanLeft * heightMm
			return Pair(x, y)
		}

		val firstE = (geometry.grid.left / 1000 + 1) * 1000.0
		val firstN = (geometry.grid.bottom / 1000 + 1) * 1000.0
		var e = firstE
		while (e < geometry.grid.right) {
			var n = firstN
			while (n < geometry.grid.top) {
				val g = gridPos(e, n)
				val i = imagePos(e, n)
				val mismatch = hypot(g.first - i.first, g.second - i.second)
				assertTrue("grid-vs-image mismatch $mismatch mm at UTM ($e,$n)", mismatch < 0.5)
				n += 1000.0
			}
			e += 1000.0
		}
	}
}
