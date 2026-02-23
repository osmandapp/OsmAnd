package net.osmand.shared.palette.domain

import kotlin.jvm.JvmStatic

object DefaultPaletteColors {

	private const val SUNNY_YELLOW = 0xFFEECC22.toInt()
	private const val BRIGHT_RED = 0xFFD00D0D.toInt()
	private const val ORANGE_RED = 0xFFFF5020.toInt()
	private const val LEMON_YELLOW = 0xFFEEEE10.toInt()
	private const val SPRING_GREEN = 0xFF88E030.toInt()
	private const val FOREST_GREEN = 0xFF00842B.toInt()
	private const val SKY_BLUE = 0xFF10C0F0.toInt()
	private const val ROYAL_BLUE = 0xFF1010A0.toInt()
	private const val PURPLE = 0xFFA71DE1.toInt()
	private const val PINK = 0xFFE044BB.toInt()
	private const val BRICK_RED = 0xFF8E2512.toInt()
	private const val ALMOST_BLACK = 0xFF000001.toInt()
	private const val LAVENDER = 0xFFDC5FFF.toInt()
	private const val PURE_BLUE = 0xFF0000FF.toInt()
	private const val CYAN = 0xFF00FFFF.toInt()

	@JvmStatic
	fun values(): List<Int> = listOf(
		SUNNY_YELLOW, BRIGHT_RED, ORANGE_RED, LEMON_YELLOW,
		SPRING_GREEN, FOREST_GREEN, SKY_BLUE, ROYAL_BLUE,
		PURPLE, PINK, BRICK_RED, ALMOST_BLACK, LAVENDER,
		PURE_BLUE, CYAN
	)
}