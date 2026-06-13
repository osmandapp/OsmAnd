package net.sf.marineapi.ais.message

/**
 * Implementation of https://www.navcen.uscg.gov/?pageName=AISMessage27
 *
 * @author Krzysztof Borowski
 */
interface AISMessage27 : AISPositionReport {
    /**
     * Returns the RAIM flag.
     *
     * @return `true` if RAIM in use, otherwise `false`.
     */
    val rAIMFlag: Boolean

    /**
     * Returns Position Latency.
     *
     * @return 0 = Reported position latency is less than 5 seconds; 1 = Reported position latency is greater than 5 seconds = default
     */
    val positionLatency: Int
}