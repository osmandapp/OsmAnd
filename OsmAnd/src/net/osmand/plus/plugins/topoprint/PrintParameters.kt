package net.osmand.plus.plugins.topoprint

/**
 * Physical paper sizes. `shortSideMm`/`longSideMm` are the sheet dimensions
 * with the shorter side first; [PrintOrientation] decides which edge is the
 * sheet width. [tag] is the token used in export file names.
 */
enum class PaperSize(val shortSideMm: Double, val longSideMm: Double, val tag: String) {
	A3(297.0, 420.0, "A3"),
	A4(210.0, 297.0, "A4"),
	A5(148.0, 210.0, "A5"),
	A6(105.0, 148.0, "A6"),
	B3(353.0, 500.0, "B3"),
	B4(250.0, 353.0, "B4"),
	B5(176.0, 250.0, "B5"),
	B6(125.0, 176.0, "B6"),
	LETTER(215.9, 279.4, "LETTER"),
	INCH_3_5X5_25(88.9, 133.35, "3.5x5.25"),
	INCH_4X6(101.6, 152.4, "4x6"),
	INCH_5X7(127.0, 177.8, "5x7"),
	INCH_6X8(152.4, 203.2, "6x8"),
	CUSTOM(0.0, 0.0, "CUSTOM");

	companion object {
		@JvmStatic
		fun parse(name: String?, default: PaperSize): PaperSize =
			entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
	}
}

enum class PrintOrientation {
	PORTRAIT, LANDSCAPE;

	companion object {
		@JvmStatic
		fun parse(name: String?, default: PrintOrientation): PrintOrientation =
			entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
	}
}

/**
 * Map projection of the printed basemap. Spherical (curved-earth) rendering is
 * only supported by the OpenGL engine; the legacy software rasterizer always
 * falls back to Mercator.
 */
enum class MapProjection {
	SPHERICAL, MERCATOR;

	companion object {
		@JvmStatic
		fun parse(name: String?, default: MapProjection): MapProjection =
			entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
	}
}

/**
 * Physical layout of the printable map: sheet size, margins and the map
 * area. Sheets at least A4 large use the absolute A4 margins (portrait:
 * 17.5/29/58 mm, landscape: 22.5/29/58 mm), so the chrome — title, notes,
 * compass, scale bar, all sized from the margins — keeps the A4 size and the
 * map area simply grows with the sheet. Smaller sheets scale the A4 factors
 * with their short side so the chrome still fits.
 */
data class PrintLayout(
	val paper: PaperSize,
	val orientation: PrintOrientation,
	val customWidthMm: Double = 0.0,
	val customHeightMm: Double = 0.0
) {
	val mapWidthMm: Double
	val mapHeightMm: Double
	val paperWidthMm: Double
	val paperHeightMm: Double
	val leftMarginMm: Double
	val topMarginMm: Double
	val rightMarginMm: Double
	val bottomMarginMm: Double

	init {
		val custom = paper == PaperSize.CUSTOM &&
				customWidthMm in 50.0..3000.0 && customHeightMm in 50.0..3000.0
		val shortSide = if (custom) minOf(customWidthMm, customHeightMm) else paper.shortSideMm
		val longSide = if (custom) maxOf(customWidthMm, customHeightMm) else paper.longSideMm
		val absolute = shortSide >= 210.0
		if (orientation == PrintOrientation.PORTRAIT) {
			paperWidthMm = shortSide
			paperHeightMm = longSide
			leftMarginMm = if (absolute) 17.5 else shortSide / 12.0
			topMarginMm = if (absolute) 29.0 else shortSide * 29.0 / 210.0
			bottomMarginMm = if (absolute) 58.0 else shortSide * 58.0 / 210.0
		} else {
			paperWidthMm = longSide
			paperHeightMm = shortSide
			leftMarginMm = if (absolute) 22.5 else shortSide * 22.5 / 210.0
			topMarginMm = if (absolute) 29.0 else shortSide * 29.0 / 210.0
			bottomMarginMm = if (absolute) 58.0 else shortSide * 58.0 / 210.0
		}
		rightMarginMm = leftMarginMm
		mapWidthMm = paperWidthMm - leftMarginMm - rightMarginMm
		mapHeightMm = paperHeightMm - topMarginMm - bottomMarginMm
	}

	companion object {
		fun of(paper: PaperSize, orientation: PrintOrientation,
		       customWidthMm: Double = 0.0, customHeightMm: Double = 0.0): PrintLayout =
			PrintLayout(paper, orientation, customWidthMm, customHeightMm)
	}
}

/**
 * User-selected print job parameters: paper, page orientation, output DPI and
 * map scale denominator (1:N). `customPaperWidthMm/HeightMm` are the user's
 * custom sheet dimensions in millimetres; they are only used when [paper] is
 * [PaperSize.CUSTOM].
 */
data class PrintParameters(
	val paper: PaperSize,
	val orientation: PrintOrientation,
	val dpi: Int,
	val scaleDenominator: Int,
	val projection: MapProjection = MapProjection.SPHERICAL,
	val customPaperWidthMm: Double = 0.0,
	val customPaperHeightMm: Double = 0.0
) {
	val layout: PrintLayout = PrintLayout.of(paper, orientation, customPaperWidthMm, customPaperHeightMm)

	/** Size token used in export file names. */
	val sizeTag: String
		get() = if (paper == PaperSize.CUSTOM)
			"${Math.round(customPaperWidthMm)}x${Math.round(customPaperHeightMm)}"
		else paper.tag

	/** Scale token used in export file names, e.g. 25000 → "25K", 100000 → "100K". */
	val scaleTag: String
		get() = "${scaleDenominator / 1000}K"

	/** Size of the geographic area covered by the map frame, in metres. */
	val mapWidthMeters: Double
		get() = layout.mapWidthMm * scaleDenominator / 1000.0

	val mapHeightMeters: Double
		get() = layout.mapHeightMm * scaleDenominator / 1000.0

	/** Raster pixel dimensions of the map frame at the selected DPI. */
	val rasterWidthPx: Int
		get() = Math.round(layout.mapWidthMm * dpi / MM_PER_INCH).toInt()

	/** Full map pixel dimensions including margins. */
	val paperWidthPx: Int
		get() = Math.round(layout.paperWidthMm * dpi / MM_PER_INCH).toInt()

	val paperHeightPx: Int
		get() = Math.round(layout.paperHeightMm * dpi / MM_PER_INCH).toInt()

	companion object {
		const val MM_PER_INCH = 25.4

		@JvmField
		val SUPPORTED_DPI = intArrayOf(150, 300, 600)

		@JvmField
		val SUPPORTED_SCALES = intArrayOf(5000, 10000, 24000, 25000, 50000, 63360, 100000)
	}
}
