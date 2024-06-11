package net.osmand.shared

import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory
import okio.IOException
import okio.buffer

class ColorPalette {

	companion object {
		private val LOG = LoggerFactory.getLogger("ColorPalette")

		val DARK_GREY = rgbaToDecimal(92, 92, 92, 255)
		val LIGHT_GREY = rgbaToDecimal(200, 200, 200, 255)
		val GREEN = rgbaToDecimal(90, 220, 95, 255)
		val YELLOW = rgbaToDecimal(212, 239, 50, 255)
		val RED = rgbaToDecimal(243, 55, 77, 255)
		val BLUE_SLOPE = rgbaToDecimal(0, 0, 255, 255)
		val CYAN_SLOPE = rgbaToDecimal(0, 255, 255, 255)
		val GREEN_SLOPE = rgbaToDecimal(46, 185, 0, 255)
		val WHITE = rgbaToDecimal(255, 255, 255, 255)
		val YELLOW_SLOPE = rgbaToDecimal(255, 222, 2, 255)
		val RED_SLOPE = rgbaToDecimal(255, 1, 1, 255)
		val PURPLE_SLOPE = rgbaToDecimal(130, 1, 255, 255)

		val COLORS = intArrayOf(GREEN, YELLOW, RED)
		val SLOPE_COLORS = intArrayOf(CYAN_SLOPE, GREEN_SLOPE, LIGHT_GREY, YELLOW_SLOPE, RED_SLOPE)

		const val SLOPE_MIN_VALUE = -1.0
		const val SLOPE_MAX_VALUE = 1.0

		val SLOPE_PALETTE = parsePalette(
			arrayOf(
				doubleArrayOf(SLOPE_MIN_VALUE, BLUE_SLOPE.toDouble()),
				doubleArrayOf(-0.15, CYAN_SLOPE.toDouble()),
				doubleArrayOf(-0.05, GREEN_SLOPE.toDouble()),
				doubleArrayOf(0.0, LIGHT_GREY.toDouble()),
				doubleArrayOf(0.05, YELLOW_SLOPE.toDouble()),
				doubleArrayOf(0.15, RED_SLOPE.toDouble()),
				doubleArrayOf(SLOPE_MAX_VALUE, PURPLE_SLOPE.toDouble())
			)
		)

		val MIN_MAX_PALETTE = parsePalette(
			arrayOf(
				doubleArrayOf(0.0, GREEN.toDouble()),
				doubleArrayOf(0.5, YELLOW.toDouble()),
				doubleArrayOf(1.0, RED.toDouble())
			)
		)

		fun rgbaToDecimal(r: Int, g: Int, b: Int, a: Int): Int {
			return (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF shl 0)
		}

		private fun parsePalette(vl: Array<DoubleArray>): ColorPalette {
			val palette = ColorPalette()
			for (v in vl) {
				val c = when {
					v.size == 2 -> ColorValue(v[0], v[1].toInt())
					v.size == 4 -> ColorValue(v[0], v[1].toInt(), v[2].toInt(), v[3].toInt(), 255)
					v.size >= 5 -> ColorValue(
						v[0],
						v[1].toInt(),
						v[2].toInt(),
						v[3].toInt(),
						v[4].toInt()
					)

					else -> null
				}
				c?.let { palette.colors.add(it) }
			}
			palette.sortPalette()
			return palette
		}

		fun getTransparentColor(): Int {
			return rgbaToDecimal(0, 0, 0, 0)
		}

		private fun red(value: Int): Int {
			return (value shr 16) and 0xFF
		}

		private fun green(value: Int): Int {
			return (value shr 8) and 0xFF
		}

		private fun blue(value: Int): Int {
			return value and 0xFF
		}

		private fun alpha(value: Int): Int {
			return value shr 24 and 0xFF
		}

		fun getIntermediateColor(min: Int, max: Int, percent: Double): Int {
			val r: Double = red(min) + percent * (red(max) - red(min))
			val g: Double = green(min) + percent * (green(max) - green(min))
			val b: Double = blue(min) + percent * (blue(max) - blue(min))
			val a: Double = alpha(min) + percent * (alpha(max) - alpha(min))
			return rgbaToDecimal(r.toInt(), g.toInt(), b.toInt(), a.toInt())
		}
		
		@Throws(IOException::class)
		fun parseColorPalette(file: KFile): ColorPalette {
			val palette = ColorPalette()
			val buf = file.source().buffer()
			var line: String?
			while (true) {
				line = buf.readUtf8Line()
				if (line == null) break
				val t = line.trim { it <= ' ' }
				if (t.startsWith("#")) {
					continue
				}
				val values = t.split(",".toRegex()).dropLastWhile { it.isEmpty() }
					.toTypedArray()
				if (values.size >= 4) {
					try {
						val rgba: ColorValue =
							ColorValue.rgba(
								values[0].toDouble(),
								values[1].toInt(),
								values[2].toInt(),
								values[3].toInt(),
								if (values.size >= 4) values[4].toInt() else 255
							)
						palette.colors.add(rgba)
					} catch (e: NumberFormatException) {
						LOG.error(e.message, e)
					}
				}
			}
			palette.sortPalette()
			return palette
		}
	}

	val colors = mutableListOf<ColorValue>()

	constructor()

	constructor(c: ColorPalette, minVal: Double, maxVal: Double) {
		for (cv in c.colors) {
			val value = cv.value * (maxVal - minVal) + minVal
			colors.add(ColorValue(value, cv.clr))
		}
	}

	fun getColorByValue(value: Double): Int {
		if (value.isNaN()) {
			return LIGHT_GREY
		}
		for (i in 0 until colors.size - 1) {
			val min = colors[i]
			val max = colors[i + 1]
			if (value == min.value) return min.clr
			if (value >= min.value && value <= max.value) {
				val percent = (value - min.value) / (max.value - min.value)
				return getIntermediateColor(min, max, percent)
			}
		}
		return when {
			value <= colors[0].value -> colors[0].clr
			value >= colors[colors.size - 1].value -> colors[colors.size - 1].clr
			else -> getTransparentColor()
		}
	}

	private fun getIntermediateColor(min: ColorValue, max: ColorValue, percent: Double): Int {
		val r = min.r + percent * (max.r - min.r)
		val g = min.g + percent * (max.g - min.g)
		val b = min.b + percent * (max.b - min.b)
		val a = min.a + percent * (max.a - min.a)
		return rgbaToDecimal(r.toInt(), g.toInt(), b.toInt(), a.toInt())
	}

	override fun toString(): String {
		return writeColorPalette()
	}

	fun writeColorPalette(): String {
		val bld = StringBuilder()
		for (v in colors) {
			bld.append(v.value).append(",")
			bld.append(v.r).append(",").append(v.g).append(",").append(v.b).append(",").append(v.a)
				.append("\n")
		}
		return bld.toString().trim()
	}

	private fun sortPalette() {
		colors.sortWith(compareBy { it.value })
	}

	class ColorValue(
		val value: Double,
		val r: Int,
		val g: Int,
		val b: Int,
		val a: Int,
		val clr: Int = rgbaToDecimal(r, g, b, a)
	) {
		constructor(value: Double, clr: Int) : this(
			value,
			red(clr),
			green(clr),
			blue(clr),
			alpha(clr),
			clr
		)

		override fun toString(): String {
			return "ColorValue [r=$r, g=$g, b=$b, a=$a, val=$value]"
		}

		companion object {
			fun rgba(value: Double, r: Int, g: Int, b: Int, a: Int): ColorValue {
				return ColorValue(value, r, g, b, a)
			}
		}
	}
}
