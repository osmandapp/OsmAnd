package net.osmand.shared.grid

import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_BOTTOM
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_FULL_WIDTH
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_LEFT
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_RIGHT
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_TOP
import kotlin.test.Test
import kotlin.test.assertTrue

class ButtonPositionSizeTest {

	private fun check(buttons: List<ButtonPositionSize>, id: String, x: Double, y: Double): Boolean {
		return buttons.any { it.id == id && it.bounds.left == x && it.bounds.top == y }
	}

	@Test
	fun testLayout0() {
		ButtonPositionSize.DEBUG_PRINT = true;
		val buttons = listOf(
			ButtonPositionSize("map_right_widgets_panel", 15, false, true).
			setSize(15, 21).setMoveDescendantsHorizontal(),
			ButtonPositionSize("map_top_widgets_panel", 114, true, true).
			setSize(114, 12).setMargin(0, 0).setMoveDescendantsVertical(),
			ButtonPositionSize("map_bottom_widgets_panel", 68, true, false).
			setSize(68, 9).setMargin(22, 0).setMoveDescendantsVertical(),

			ButtonPositionSize("map.view.layers", 6, true, true),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveHorizontal(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveVertical(),

			ButtonPositionSize("map.view.zoom_out", 7, false, false),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveVertical(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveHorizontal(),

			ButtonPositionSize("map.view.menu", 7, true, false),
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveHorizontal(),

			ButtonPositionSize("map.quick_actions1", 7, false, false).setMargin(8, 7),
			ButtonPositionSize("map.quick_actions2", 7, false, true).setMargin(0, 15),
		)
		val compute = ButtonPositionSize.computeNonOverlap(1, buttons, 114, 45);
		assertTrue(compute);
//		assertTrue { check(buttons, "map.view.zoom_out", 107.0, 38.0) }

	}

	@Test
	fun testLayout1() {
		ButtonPositionSize.DEBUG_PRINT = true;
//		ButtonPositionSize.ALTERNATIVE_ALGORITHM = true;
		val buttons = listOf(
			ButtonPositionSize("map.view.zoom_out", 7, false, false),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveVertical(),
			ButtonPositionSize("map.view.zoom_id2", 7, false, false).setMoveVertical(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveHorizontal(),
			ButtonPositionSize("map.view.zoom_id3", 7, false, false).setMoveVertical(),
		)
		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 40, 40)
		assertTrue(computed)

		assertTrue { check(buttons, "map.view.zoom_out", 33.0, 33.0) }
		assertTrue { check(buttons, "map.view.zoom_id", 33.0, 25.0) }
		assertTrue { check(buttons, "map.view.zoom_id2", 33.0, 17.0) }
		assertTrue { check(buttons, "map.view.zoom_id3", 33.0, 9.0) }
		assertTrue { check(buttons, "map.view.back_to_loc", 25.0, 33.0) }

	}

	@Test
	fun testLayout2() {
		ButtonPositionSize.DEBUG_PRINT = true;
//		ButtonPositionSize.ALTERNATIVE_ALGORITHM = true;
		val buttons = listOf(
			ButtonPositionSize("map_right_widgets_panel", 12, false, true).
			setSize(12, 4),
			ButtonPositionSize("top_widgets_panel", 68, true, true).
			setSize(68, 7).setMargin(22, 0),
			ButtonPositionSize("map_bottom_widgets_panel", 68, true, false).
			setSize(68, 9).setMargin(22, 0),
			ButtonPositionSize("recording_note_layout", 40, true, false).
			setSize(40, 45),
			ButtonPositionSize("map.view.zoom_out", 7, false, false),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveVertical(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveHorizontal()
		)
		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 114, 45)
		assertTrue(computed)

		assertTrue { check(buttons, "map.view.zoom_out", 107.0, 38.0) }
		assertTrue { check(buttons, "map.view.zoom_id", 107.0, 30.0) }
		assertTrue { check(buttons, "map.view.back_to_loc", 99.0, 38.0) }
	}

	@Test
	fun testLayout3() {
		ButtonPositionSize.DEBUG_PRINT = true
		// ButtonPositionSize.ALTERNATIVE_ALGORITHM = true

		val buttons = listOf(
			ButtonPositionSize("top_widgets_panel").setSize(51, 8).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},

			ButtonPositionSize("map_right_widgets_panel").setSize(22, 91).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_RIGHT
					posV = POS_TOP
					marginY = 9
				},

			ButtonPositionSize("map.view.layers", 6, true, true).apply {
				yMove = true
			}, ButtonPositionSize("map.view.quick_search", 6, true, true).apply {
				xMove = true
			}, ButtonPositionSize("map.view.compass", 6, true, true).apply {
				yMove = true
			}, ButtonPositionSize("map.view.zoom_out", 7, false, false).apply {
				yMove = true
			}, ButtonPositionSize("map.view.zoom_id", 7, false, false).apply {
				yMove = true
			}, ButtonPositionSize("map.view.back_to_loc", 7, false, false).apply {
				xMove = true
			}, ButtonPositionSize("map.view.menu", 7, true, false).apply {
				xMove = true
			}, ButtonPositionSize("map.view.route_planning", 7, true, false).apply {
				xMove = true
			}, ButtonPositionSize("map.view.map_3d", 7, true, false).apply {
				yMove = true
				xMove = true
				marginX = 9
				marginY = 35
			}, ButtonPositionSize("map_ruler_layout").setSize(8, 3).apply {
				xMove = true
				posH = POS_LEFT
				posV = POS_BOTTOM
			})

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 100)
		assertTrue(computed)

		assertTrue { !check(buttons, "map.view.zoom_id", 44.0, 1.0) } // buttons should not be moved above top_widgets_panel
		assertTrue { !check(buttons, "map.view.zoom_out", 44.0, 1.0) }
		assertTrue { !check(buttons, "map.view.back_to_loc", 36.0, 1.0) }
	}

	@Test
	fun testLandscapeRotation_searchShouldStayNearConfigureMap() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("map_left_widgets_panel", 17, true, true)
				.setSize(17, 12).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveHorizontal(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveHorizontal(),
			ButtonPositionSize("map.view.menu", 7, true, false).setMoveHorizontal(),
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveHorizontal(),
			ButtonPositionSize("quick_actions", 7, false, true).setMargin(0, 18).setMoveVertical(),
			ButtonPositionSize("map_ruler_layout", 7, true, false).setSize(7, 3).setMoveHorizontal(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		val layers = buttons.first { it.id == "map.view.layers" }
		val search = buttons.first { it.id == "map.view.quick_search" }
		val leftPanel = buttons.first { it.id == "map_left_widgets_panel" }

		// Current layout keeps search on the top row, but it must stay to the right of the left panel.
		assertTrue(search.bounds.left >= leftPanel.bounds.right + 1.0, "search=${search.bounds}, leftPanel=${leftPanel.bounds}")
		assertTrue(!search.overlap(layers), "search=${search.bounds}, layers=${layers.bounds}")
	}

	@Test
	fun testLandscapeRotation_threeDigitSpeedWidget_searchNearConfigureAndCompassBelow() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("map_left_widgets_panel", 17, true, true)
				.setSize(17, 12).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveHorizontal(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveHorizontal(),
			ButtonPositionSize("map.view.menu", 7, true, false).setMoveHorizontal(),
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveHorizontal(),
			ButtonPositionSize("quick_actions", 7, false, true).setMargin(0, 18).setMoveVertical(),
			ButtonPositionSize("map_ruler_layout", 12, true, false).setSize(12, 3).setMoveHorizontal(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		assertTrue { check(buttons, "map_left_widgets_panel", 0.0, 0.0) }
		val layers = buttons.first { it.id == "map.view.layers" }
		val search = buttons.first { it.id == "map.view.quick_search" }
		val compass = buttons.first { it.id == "map.view.compass" }
		val leftPanel = buttons.first { it.id == "map_left_widgets_panel" }

		assertTrue(search.bounds.left >= leftPanel.bounds.right + 1.0, "search=${search.bounds}, leftPanel=${leftPanel.bounds}")
		assertTrue(!search.overlap(layers), "search=${search.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(layers), "compass=${compass.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(search), "compass=${compass.bounds}, search=${search.bounds}")
	}

	@Test
	fun testLandscapeRotation_alarmsContainerShouldBeAboveBottomMenu() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("alarms_container", 14, true, false).setSize(14, 7).setMoveVertical().apply {
				marginX = 0
				marginY = 0
			},
			ButtonPositionSize("map.view.menu", 7, true, false).setMoveHorizontal().apply {
				marginX = 0
				marginY = 0
			},
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveHorizontal().apply {
				marginX = 0
				marginY = 0
			}
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		val alarms = buttons.first { it.id == "alarms_container" }
		val menu = buttons.first { it.id == "map.view.menu" }
		val routePlanning = buttons.first { it.id == "map.view.route_planning" }

		assertTrue(!alarms.overlap(menu), "alarms=${alarms.bounds}, menu=${menu.bounds}")
		assertTrue(!alarms.overlap(routePlanning), "alarms=${alarms.bounds}, route=${routePlanning.bounds}")
	}

	@Test
	fun testLandscapeRotation_leftPanel4h_searchRightOfConfigureAndCompassBelow() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("top_widgets_panel", 65, true, true)
				.setSize(65, 9).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_bottom_widgets_panel", 65, true, false)
				.setSize(65, 14).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_left_widgets_panel", 14, true, true)
				.setSize(14, 4).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("alarms_container", 19, true, false).setSize(19, 12).setMoveAny(),
			ButtonPositionSize("map.view.menu", 7, true, false).setMoveAny(),
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 12, true, false).setSize(12, 3).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		val layers = buttons.first { it.id == "map.view.layers" }
		val search = buttons.first { it.id == "map.view.quick_search" }
		val compass = buttons.first { it.id == "map.view.compass" }

		assertTrue(search.bounds.top > 0.0, "search=${search.bounds}")
		assertTrue(!search.overlap(layers), "search=${search.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(layers), "compass=${compass.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(search), "compass=${compass.bounds}, search=${search.bounds}")
		assertTrue { !check(buttons, "map.view.quick_search", 14.0, 0.0) }

		val alarms = buttons.first { it.id == "alarms_container" }
		val menu = buttons.first { it.id == "map.view.menu" }
		val routePlanning = buttons.first { it.id == "map.view.route_planning" }
		val ruler = buttons.first { it.id == "map_ruler_layout" }
		assertTrue(!alarms.overlap(menu), "alarms=${alarms.bounds}, menu=${menu.bounds}")
		assertTrue(!alarms.overlap(routePlanning), "alarms=${alarms.bounds}, route=${routePlanning.bounds}")
		assertTrue(!alarms.overlap(ruler), "alarms=${alarms.bounds}, ruler=${ruler.bounds}")
	}

	@Test
	fun testLandscapeRotation_leftPanel8h_searchRightOfConfigureAndCompassBelow() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("top_widgets_panel", 65, true, true)
				.setSize(65, 9).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_bottom_widgets_panel", 65, true, false)
				.setSize(65, 14).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_left_widgets_panel", 13, true, true)
				.setSize(13, 8).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("alarms_container", 10, true, false).setSize(10, 12).setMoveAny(),
			ButtonPositionSize("map.view.menu", 7, true, false).setMoveAny(),
			ButtonPositionSize("map.view.route_planning", 7, true, false).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 12, true, false).setSize(12, 3).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		val layers = buttons.first { it.id == "map.view.layers" }
		val search = buttons.first { it.id == "map.view.quick_search" }
		val compass = buttons.first { it.id == "map.view.compass" }

		assertTrue(search.bounds.top > 0.0, "search=${search.bounds}")
		assertTrue(!search.overlap(layers), "search=${search.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(layers), "compass=${compass.bounds}, layers=${layers.bounds}")
		assertTrue(!compass.overlap(search), "compass=${compass.bounds}, search=${search.bounds}")
		assertTrue { !check(buttons, "map.view.quick_search", 13.0, 0.0) }

		val alarms = buttons.first { it.id == "alarms_container" }
		val menu = buttons.first { it.id == "map.view.menu" }
		val routePlanning = buttons.first { it.id == "map.view.route_planning" }
		val ruler = buttons.first { it.id == "map_ruler_layout" }
		assertTrue(!alarms.overlap(menu), "alarms=${alarms.bounds}, menu=${menu.bounds}")
		assertTrue(!alarms.overlap(routePlanning), "alarms=${alarms.bounds}, route=${routePlanning.bounds}")
		assertTrue(!alarms.overlap(ruler), "alarms=${alarms.bounds}, ruler=${ruler.bounds}")
	}

	@Test
	fun testLandscapeRotation_leftPanel8hWideAlerts_withoutMenu_shouldStayLowAndNotJumpTop() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("top_widgets_panel", 65, true, true)
				.setSize(65, 9).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_bottom_widgets_panel", 65, true, false)
				.setSize(65, 14).setMargin(21, 0).setMoveVertical(),
			ButtonPositionSize("map_left_widgets_panel", 13, true, true)
				.setSize(13, 8).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("alarms_container", 31, true, false).setSize(31, 12).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 9, true, false).setSize(9, 3).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 109, 42)
		assertTrue(computed)

		val alarms = buttons.first { it.id == "alarms_container" }
		val bottomPanel = buttons.first { it.id == "map_bottom_widgets_panel" }

		// Alerts should not jump to the very top.
		assertTrue(alarms.bounds.top > 0.0, "alarms.marginY=${alarms.marginY}, bounds=${alarms.bounds}")
		// Alerts should stay above bottom widgets stack.
		assertTrue { alarms.bounds.bottom <= bottomPanel.bounds.top - 1.0 }
	}

	@Test
	fun testPlanRoute_rulerShouldBeRightOfMeasurementButtons() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("widget_top_bar").setSize(51, 7).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},
			ButtonPositionSize("map_left_widgets_panel", 17, true, true)
				.setSize(17, 12).setMoveVertical(),
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveAny(),
			ButtonPositionSize("measurement_buttons", 8, true, false).setSize(8, 7).setNonMoveable(),
			ButtonPositionSize("map_ruler_layout", 7, true, false).setSize(7, 3).setMoveHorizontal(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 82)
		assertTrue(computed)

		val measurement = buttons.first { it.id == "measurement_buttons" }
		val ruler = buttons.first { it.id == "map_ruler_layout" }

		// Fixed behavior: ruler should be to the right of measurement_buttons (not above it).
		assertTrue(
			ruler.bounds.left >= measurement.bounds.right + 1.0,
			"ruler=${ruler.bounds}, measurement=${measurement.bounds}"
		)
		assertTrue(
			ruler.bounds.bottom == measurement.bounds.bottom,
			"ruler=${ruler.bounds}, measurement=${measurement.bounds}"
		)
		assertTrue(
			!(ruler.bounds.left == measurement.bounds.left && ruler.bounds.top < measurement.bounds.top),
			"ruler=${ruler.bounds}, measurement=${measurement.bounds}"
		)
	}

	@Test
	fun testMapMarkers_rulerShouldStayBottomLeftWhenAlone() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("widget_top_bar").setSize(51, 7).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},
			ButtonPositionSize("map_ruler_layout", 7, true, false).setSize(7, 3).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 27)
		assertTrue(computed)

		// Map Markers: ruler remains in bottom-left corner.
		assertTrue { check(buttons, "map_ruler_layout", 0.0, 24.0) }
	}

	@Test
	fun testPlanRoute_transparencySliderShouldStayAtBottomExpectedPosition() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("widget_top_bar").setSize(51, 7).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveAny(),
			ButtonPositionSize("measurement_buttons", 8, true, false).setSize(8, 7).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 9, true, false).setSize(9, 3).setMoveAny(),
			ButtonPositionSize("map_transparency_layout", 11, true, false)
				.setSize(11, 6).setMargin(19, 6).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 82)
		assertTrue(computed)

		// From log: x=(left ->19), y=(bott->6) => top = 82 - 6 - 6 = 70
		assertTrue { check(buttons, "map_transparency_layout", 19.0, 70.0) }
	}

	@Test
	fun testPlanRoute_transparencySliderShouldMoveUpWhenBottomWidgetAdded() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("widget_top_bar").setSize(51, 7).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveAny(),
			ButtonPositionSize("measurement_buttons", 8, true, false).setSize(8, 7).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 9, true, false).setSize(9, 3).setMoveAny(),
			// Simulates newly added bottom widget in slider area.
			ButtonPositionSize("added_bottom_widget", 11, true, false)
				.setSize(11, 10).setMargin(19, 0).setNonMoveable(),
			ButtonPositionSize("map_transparency_layout", 11, true, false)
				.setSize(11, 6).setMargin(19, 6).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 82)
		assertTrue(computed)

		val slider = buttons.first { it.id == "map_transparency_layout" }
		val added = buttons.first { it.id == "added_bottom_widget" }

		// Regression: slider must move up to avoid overlap with added bottom widget.
		assertTrue(
			slider.marginY > 6,
			"slider.marginY=${slider.marginY}, slider=${slider.bounds}, added=${added.bounds}"
		)
		assertTrue(!slider.overlap(added), "slider=${slider.bounds}, added=${added.bounds}")
	}

	@Test
	fun testPlanRoute_transparencySliderShouldNotJumpTooHigh() {
		ButtonPositionSize.DEBUG_PRINT = false
		val buttons = listOf(
			ButtonPositionSize("widget_top_bar").setSize(51, 7).setNonMoveable()
				.setMoveDescendantsVertical().apply {
					posH = POS_FULL_WIDTH
					posV = POS_TOP
				},
			ButtonPositionSize("map.view.layers", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.quick_search", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.compass", 6, true, true).setMoveAny(),
			ButtonPositionSize("map.view.zoom_out", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.zoom_id", 7, false, false).setMoveAny(),
			ButtonPositionSize("map.view.back_to_loc", 7, false, false).setMoveAny(),
			ButtonPositionSize("measurement_buttons", 8, true, false).setSize(8, 7).setMoveAny(),
			ButtonPositionSize("map_ruler_layout", 9, true, false).setSize(9, 3).setMoveAny(),
			ButtonPositionSize("map_transparency_layout", 11, true, false)
				.setSize(11, 6).setMargin(19, 6).setMoveAny(),
		)

		val computed = ButtonPositionSize.computeNonOverlap(1, buttons, 51, 82)
		assertTrue(computed)

		val slider = buttons.first { it.id == "map_transparency_layout" }
		val measurement = buttons.first { it.id == "measurement_buttons" }
		val ruler = buttons.first { it.id == "map_ruler_layout" }

		// Regression guard: without bottom conflicts the transparency slider should stay near bottom.
		assertTrue(
			slider.marginY <= 12,
			"slider.marginY=${slider.marginY}, slider=${slider.bounds}"
		)
		assertTrue(
			slider.bounds.top >= 64.0,
			"slider unexpectedly too high: slider=${slider.bounds}"
		)
		assertTrue(!slider.overlap(measurement), "slider=${slider.bounds}, measurement=${measurement.bounds}")
		assertTrue(!slider.overlap(ruler), "slider=${slider.bounds}, ruler=${ruler.bounds}")
	}



}
