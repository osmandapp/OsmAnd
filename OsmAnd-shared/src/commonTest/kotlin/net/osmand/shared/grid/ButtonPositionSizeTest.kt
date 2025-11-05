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
		ButtonPositionSize.SIMPLIFIED_ALGORITHM = true;
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
}