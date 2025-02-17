package net.osmand.shared.gpx

expect object GpxFormatter {

    // 0.00#####
    fun formatLatLon(value: Double): String

    // #.#
    fun formatDecimal(value: Double): String
}