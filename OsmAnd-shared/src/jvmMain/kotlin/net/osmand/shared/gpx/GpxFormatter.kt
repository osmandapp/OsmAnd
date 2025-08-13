package net.osmand.shared.gpx

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

actual object GpxFormatter {

    private val LAT_LON_FORMAT = DecimalFormat("0.00#####", DecimalFormatSymbols(Locale.US))
    private val DECIMAL_FORMAT = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))

    actual fun formatLatLon(value: Double): String = LAT_LON_FORMAT.format(value)

    actual fun formatDecimal(value: Double): String = DECIMAL_FORMAT.format(value)
}