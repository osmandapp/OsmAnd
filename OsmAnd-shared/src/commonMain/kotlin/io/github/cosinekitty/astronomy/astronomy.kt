/*
    Astronomy Engine for Kotlin / JVM.
    https://github.com/cosinekitty/astronomy

    MIT License

    Copyright (c) 2019-2025 Don Cross <cosinekitty@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/
@file:JvmName("Astronomy")

package io.github.cosinekitty.astronomy

import kotlin.math.absoluteValue
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import net.osmand.shared.extensions.format
import net.osmand.shared.util.KLock
import net.osmand.shared.util.synchronized
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * An invalid body was specified for the given function.
 */
class InvalidBodyException(body: Body) : Exception("Invalid body: $body")

/**
 * The Earth is not allowed as the body parameter.
 */
class EarthNotAllowedException : Exception("The Earth is not allowed as the body parameter.")

/**
 * An unexpected internal error occurred in Astronomy Engine
 */
class InternalError(message: String) : Exception(message)

/**
 * The factor to convert degrees to radians = pi/180.
 */
const val DEG2RAD = 0.017453292519943296

/**
 * Convert an angle expressed in degrees to an angle expressed in radians.
 */
fun Double.degreesToRadians() = this * DEG2RAD

internal fun dsin(degrees: Double) = sin(degrees.degreesToRadians())
internal fun dcos(degrees: Double) = cos(degrees.degreesToRadians())
internal fun dtan(degrees: Double) = tan(degrees.degreesToRadians())
internal fun datan(slope: Double) = atan(slope).radiansToDegrees()
internal fun datan2(y: Double, x: Double) = atan2(y, x).radiansToDegrees()

/**
 * The factor to convert radians to degrees = 180/pi.
 */
const val RAD2DEG = 57.295779513082321

/**
 * The factor to convert radians to sidereal hours = 12/pi.
 */
const val RAD2HOUR = 3.819718634205488

/**
 * The factor to convert sidereal hours to radians = pi/12.
 */
const val HOUR2RAD = 0.2617993877991494365


/**
 * The equatorial radius of Jupiter, expressed in kilometers.
 */
const val JUPITER_EQUATORIAL_RADIUS_KM = 71492.0

/**
 * The polar radius of Jupiter, expressed in kilometers.
 */
const val JUPITER_POLAR_RADIUS_KM = 66854.0

/**
 * The volumetric mean radius of Jupiter, expressed in kilometers.
 */
const val JUPITER_MEAN_RADIUS_KM = 69911.0

/**
 * The mean radius of Jupiter's moon Io, expressed in kilometers.
 */
const val IO_RADIUS_KM = 1821.6

/**
 * The mean radius of Jupiter's moon Europa, expressed in kilometers.
 */
const val EUROPA_RADIUS_KM = 1560.8

/**
 * The mean radius of Jupiter's moon Ganymede, expressed in kilometers.
 */
const val GANYMEDE_RADIUS_KM = 2631.2

/**
 * The mean radius of Jupiter's moon Callisto, expressed in kilometers.
 */
const val CALLISTO_RADIUS_KM = 2410.3

/**
 * The speed of light in AU/day.
 */
const val C_AUDAY = 173.1446326846693

/**
 * The number of astronomical units per light-year.
 */
const val AU_PER_LY = 63241.07708807546

/**
 * Convert an angle expressed in radians to an angle expressed in degrees.
 */
fun Double.radiansToDegrees() = this * RAD2DEG

/**
 * The number of kilometers in one astronomical unit (AU).
 */
const val KM_PER_AU = 1.4959787069098932e+8

/**
 * The number of minutes in a day.
 */
const val MINUTES_PER_DAY = 60.0 * 24.0

/**
 * The number of seconds in a day.
 */
const val SECONDS_PER_DAY = 60.0 * MINUTES_PER_DAY

/**
 * The number of milliseconds in a day.
 */
const val MILLISECONDS_PER_DAY = 1000.0 * SECONDS_PER_DAY


private const val DAYS_PER_TROPICAL_YEAR = 365.24217
private const val DAYS_PER_MILLENNIUM = 365250.0

private const val ASEC360 = 1296000.0
private const val ASEC2RAD = 4.848136811095359935899141e-6
private const val PI2 = 2.0 * PI
private const val SUN_RADIUS_KM  = 695700.0
private const val SUN_RADIUS_AU  = SUN_RADIUS_KM / KM_PER_AU
private const val EARTH_FLATTENING = 0.996647180302104
private const val EARTH_FLATTENING_SQUARED = EARTH_FLATTENING * EARTH_FLATTENING
private const val EARTH_EQUATORIAL_RADIUS_KM = 6378.1366
private const val EARTH_EQUATORIAL_RADIUS_AU = EARTH_EQUATORIAL_RADIUS_KM / KM_PER_AU
private const val EARTH_POLAR_RADIUS_KM = EARTH_EQUATORIAL_RADIUS_KM * EARTH_FLATTENING
private const val EARTH_MEAN_RADIUS_KM = 6371.0    // mean radius of the Earth's geoid, without atmosphere
private const val EARTH_ATMOSPHERE_KM = 88.0       // effective atmosphere thickness for lunar eclipses
private const val EARTH_ECLIPSE_RADIUS_KM = EARTH_MEAN_RADIUS_KM + EARTH_ATMOSPHERE_KM
private const val MOON_EQUATORIAL_RADIUS_KM = 1738.1
private const val MOON_EQUATORIAL_RADIUS_AU = (MOON_EQUATORIAL_RADIUS_KM / KM_PER_AU)
private const val MOON_MEAN_RADIUS_KM       = 1737.4
private const val MOON_POLAR_RADIUS_KM      = 1736.0
private const val MOON_POLAR_RADIUS_AU      = (MOON_POLAR_RADIUS_KM / KM_PER_AU)
private const val ANGVEL = 7.2921150e-5
private const val SOLAR_DAYS_PER_SIDEREAL_DAY = 0.9972695717592592
private const val MEAN_SYNODIC_MONTH = 29.530588     // average number of days for Moon to return to the same phase
private const val EARTH_ORBITAL_PERIOD = 365.256
private const val NEPTUNE_ORBITAL_PERIOD = 60189.0
private const val REFRACTION_NEAR_HORIZON = 34.0 / 60.0  // degrees of refractive "lift" seen for objects near horizon
private const val ASEC180 = 180.0 * 60.0 * 60.0          // arcseconds per 180 degrees (or pi radians)
private const val AU_PER_PARSEC = (ASEC180 / PI)         // exact definition of how many AU = one parsec
private const val EARTH_MOON_MASS_RATIO = 81.30056

/*
    Masses of the Sun and outer planets, used for:
    (1) Calculating the Solar System Barycenter
    (2) Integrating the movement of Pluto

    https://web.archive.org/web/20120220062549/http://iau-comm4.jpl.nasa.gov/de405iom/de405iom.pdf

    Page 10 in the above document describes the constants used in the DE405 ephemeris.
    The following are G*M values (gravity constant * mass) in [au^3 / day^2].
    This side-steps issues of not knowing the exact values of G and masses M[i];
    the products GM[i] are known extremely accurately.
*/
private const val SUN_GM     = 0.2959122082855911e-03
private const val MERCURY_GM = 0.4912547451450812e-10
private const val VENUS_GM   = 0.7243452486162703e-09
private const val EARTH_GM   = 0.8887692390113509e-09
private const val MARS_GM    = 0.9549535105779258e-10
private const val JUPITER_GM = 0.2825345909524226e-06
private const val SATURN_GM  = 0.8459715185680659e-07
private const val URANUS_GM  = 0.1292024916781969e-07
private const val NEPTUNE_GM = 0.1524358900784276e-07
private const val PLUTO_GM   = 0.2188699765425970e-11
private const val MOON_GM = EARTH_GM / EARTH_MOON_MASS_RATIO

private fun cbrt(x: Double): Double =       // cube root of any real number
    if (x < 0.0)
        -cbrt(-x)
    else
        x.pow(1.0 / 3.0)

private fun Double.withMinDegreeValue(min: Double): Double {
    var deg = this
    while (deg < min)
        deg += 360.0
    while (deg >= min + 360.0)
        deg -= 360.0
    return deg
}

private fun Double.withMaxDegreeValue(max: Double): Double {
    var deg = this
    while (deg <= max - 360.0)
        deg += 360.0
    while (deg > max)
        deg -= 360.0
    return deg
}


private fun toggleAzimuthDirection(az: Double) = (360.0 - az).withMinDegreeValue(0.0)
private fun longitudeOffset(diff: Double) = diff.withMaxDegreeValue(+180.0)
private fun normalizeLongitude(lon: Double) = lon.withMinDegreeValue(0.0)

/**
 * The enumeration of celestial bodies supported by Astronomy Engine.
 */
enum class Body(
    internal val massProduct: Double?,
    internal val orbitalPeriod: Double?
) {
    /**
     * The planet Mercury.
     */
    Mercury(MERCURY_GM, 87.969),

    /**
     * The planet Venus.
     */
    Venus(VENUS_GM, 224.701),

    /**
     * The planet Earth.
     *
     * Some functions that accept a `Body` parameter will fail if passed this value
     * because they assume that an observation is being made from the Earth,
     * and therefore the Earth is not a target of observation.
     */
    Earth(EARTH_GM, EARTH_ORBITAL_PERIOD),

    /**
     * The planet Mars.
     */
    Mars(MARS_GM, 686.980),

    /**
     * The planet Jupiter.
     */
    Jupiter(JUPITER_GM, 4332.589),

    /**
     * The planet Saturn.
     */
    Saturn(SATURN_GM, 10759.22),

    /**
     * The planet Uranus.
     */
    Uranus(URANUS_GM, 30685.4),

    /**
     * The planet Neptune.
     */
    Neptune(NEPTUNE_GM, NEPTUNE_ORBITAL_PERIOD),

    /**
     * The planet Pluto.
     */
    Pluto(PLUTO_GM, 90560.0),

    /**
     * The Sun.
     */
    Sun(SUN_GM, null),

    /**
     * The Earth's natural satellite, the Moon.
     */
    Moon(MOON_GM, MEAN_SYNODIC_MONTH),

    /**
     * The Earth/Moon Barycenter.
     *
     * This is the common center of gravity between the Earth and Moon,
     * and is the point that both bodies co-orbit. The EMB is located about 4670 km
     * away from the center of the Earth in the direction toward the Moon's center.
     * Thus the EMB is always inside the Earth.
     */
    EMB(EARTH_GM + MOON_GM, MEAN_SYNODIC_MONTH),

    /**
     * The Solar System Barycenter.
     *
     * This is the Solar System's center of gravity, about which the Sun
     * and all the planets revolve. The SSB is sometimes inside the Sun,
     * sometimes outside the Sun, depending mainly on the relative positions
     * of the four most massive planets: Jupiter, Saturn, Uranus, and Neptune.
     * The SSB serves as an inertial reference point that is ideal for
     * simulating the movement of objects in the Solar System.
     */
    SSB(null, null),

    /**
     * User-defined star 1.
     */
    Star1(null, null),

    /**
     * User-defined star 2.
     */
    Star2(null, null),

    /**
     * User-defined star 3.
     */
    Star3(null, null),

    /**
     * User-defined star 4.
     */
    Star4(null, null),

    /**
     * User-defined star 5.
     */
    Star5(null, null),

    /**
     * User-defined star 6.
     */
    Star6(null, null),

    /**
     * User-defined star 7.
     */
    Star7(null, null),

    /**
     * User-defined star 8.
     */
    Star8(null, null),
}


private class StarDef {
    public var ra: Double = 0.0       // heliocentric right ascension in EQJ
    public var dec: Double = 0.0      // heliocentric declination in EQJ
    public var dist: Double = 0.0     // heliocentric distance in AU
}

private val starTable = arrayOf(
    StarDef(),      // Star1
    StarDef(),      // Star2
    StarDef(),      // Star3
    StarDef(),      // Star4
    StarDef(),      // Star5
    StarDef(),      // Star6
    StarDef(),      // Star7
    StarDef()       // Star8
)

private fun getStar(body: Body): StarDef? =
    when (body) {
        Body.Star1 -> starTable[0]
        Body.Star2 -> starTable[1]
        Body.Star3 -> starTable[2]
        Body.Star4 -> starTable[3]
        Body.Star5 -> starTable[4]
        Body.Star6 -> starTable[5]
        Body.Star7 -> starTable[6]
        Body.Star8 -> starTable[7]
        else -> null
    }

private fun userDefinedStar(body: Body): StarDef? {
    val star = getStar(body)
    return if (star != null && star.dist > 0.0)     // has the star been defined?
        star
    else
        null
}


/**
 * Assign equatorial coordinates to a user-defined star.
 *
 * Some Astronomy Engine functions allow their `body` parameter to
 * be a user-defined fixed point in the sky, loosely called a "star".
 * This function assigns a right ascension, declination, and distance
 * to one of the eight user-defined stars `Body.Star1`..`Body.Star8`.
 *
 * Stars are not valid until defined. Once defined, they retain their
 * definition until re-defined by another call to `defineStar`.
 *
 * @param body
 * One of the eight user-defined star identifiers: `Body.Star1`, `Body.Star2`, ..., `Body.Star8`.
 *
 * @param ra
 * The right ascension to be assigned to the star, expressed in J2000 equatorial coordinates (EQJ).
 * The value is in units of sidereal hours, and must be within the half-open range [0, 24).
 *
 * @param dec
 * The declination to be assigned to the star, expressed in J2000 equatorial coordinates (EQJ).
 * The value is in units of degrees north (positive) or south (negative) of the J2000 equator,
 * and must be within the closed range [-90, +90].
 *
 * @param distanceLightYears
 * The distance between the star and the Sun, expressed in light-years.
 * This value is used to calculate the tiny parallax shift as seen by an observer on Earth.
 * If you don't know the distance to the star, using a large value like 1000 will generally work well.
 * The minimum allowed distance is 1 light-year, which is required to provide certain internal optimizations.
 */
public fun defineStar(body: Body, ra: Double, dec: Double, distanceLightYears: Double) {
    val star = getStar(body) ?: throw InvalidBodyException(body)
    if (!ra.isFinite() || ra < 0.0 || ra >= 24.0) throw IllegalArgumentException("Invalid right ascension: $ra")
    if (!dec.isFinite() || dec < -90.0 || dec > +90.0) throw IllegalArgumentException("Invalid declination: $dec")
    if (!distanceLightYears.isFinite() || distanceLightYears < 1.0) throw IllegalArgumentException("Invalid distance: $distanceLightYears")
    star.ra = ra
    star.dec = dec
    star.dist = distanceLightYears * AU_PER_LY;
}


private fun universalTimeDays(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Double): Double {
    // This formula is adapted from NOVAS C 3.1 function julian_date(),
    // which in turn comes from Henry F. Fliegel & Thomas C. Van Flendern:
    // Communications of the ACM, Vol 11, No 10, October 1968, p. 657.
    // See: https://dl.acm.org/doi/pdf/10.1145/364096.364097
    //
    // [Don Cross - 2023-02-25] I modified the formula so that it will
    // work correctly with years as far back as -999999.

    val y = year.toLong()
    val m = month.toLong()
    val d = day.toLong()
    val f = (14 - m) / 12

    val y2000: Long = (
        (d - 365972956)
        + (1461*(y + 1000000 - f))/4
        + (367*(m - 2 + 12*f))/12
        - (3*((y + 1000100 - f) / 100))/4
    )

    return y2000 - 0.5 + (hour / 24.0) + (minute / (24.0 * 60.0)) + (second / (24.0 * 3600.0))
}


/**
 * A universal time resolved into UTC calendar date and time fields.
 */
class DateTime(
    /**
     * The integer year value.
     */
    val year: Int,

    /**
     * The month value 1=January, ..., 12=December.
     */
    val month: Int,

    /**
     * The day of the month, 1..31.
     */
    val day: Int,

    /**
     * The hour of the day, 0..23.
     */
    val hour: Int,

    /**
     * The minute value 0..59.
     */
    val minute: Int,

    /**
     * The floating point second value in the half-open range [0, 60).
     */
    val second: Double
) {
    /**
     * Convert this date and time to the floating point number of days since the J2000 epoch.
     */
    fun toDays() = universalTimeDays(year, month, day, hour, minute, second)

    /**
     * Convert this date and time to a [Time] value that can be used for astronomy calculations.
     */
    fun toTime() = Time(year, month, day, hour, minute, second)

    /**
     * Converts this `DateTime` to ISO 8601 format, expressed in UTC with millisecond resolution.
     *
     * @return Example: "2019-08-30T17:45:22.763Z".
     */
    override fun toString(): String {
        // We want to round milliseconds down, not round to the nearest millisecond.
        // Otherwise, we have to handle very complicated logic of rounding up
        // seconds, minutes, hours, ... years, handling leap years, etc.
        // Nothing in Astronomy Engine requires sub-millisecond precision in string formats.

        val wholeSeconds: Int = second.toInt()
        val millis: Double = 1000.0 * (second - wholeSeconds)
        val wholeMillis: Int = millis.toInt()

        val ytext = when {
            year < 0 -> "-%06d".format(-year)
            year <= 9999 -> "%04d".format(year)
            else -> "+%06d".format(year)
        }

        return "%s-%02d-%02dT%02d:%02d:%02d.%03dZ"
            .format(ytext, month, day, hour, minute, wholeSeconds, wholeMillis)
    }
}

internal fun dayValueToDateTime(ut: Double): DateTime {
    // Adapted from the NOVAS C 3.1 function cal_date()
    val djd = ut + 2451545.5
    val jd = floor(djd).toLong()

    var x = 24.0 * (djd % 1.0)
    if (x < 0.0)
        x += 24.0
    val hour = x.toInt()
    x = 60.0 * (x % 1.0)
    val minute = x.toInt()
    val second = 60.0 * (x % 1.0)

    // This is my own adjustment to the NOVAS cal_date logic
    // so that it can handle dates much farther back in the past.
    // I add c*400 years worth of days at the front,
    // then subtract c*400 years at the back,
    // which avoids negative values in the formulas that mess up
    // the calendar date calculations.
    // Any multiple of 400 years has the same number of days,
    // because it eliminates all the special cases for leap years.
    val c = 2500L

    var k = jd + (68569 + c*146097)
    val n = (4 * k) / 146097
    k -= (146097*n + 3) / 4
    val m = (4000*(k+1)) / 1461001
    k = k - (1461*m)/4 + 31

    var month = ((80 * k) / 2447).toInt()
    val day = (k - (2447 * month.toLong())/80).toInt()
    k = month.toLong() / 11

    month = (month.toLong() + 2 - 12*k).toInt()
    val year = (100*(n - 49) + m + k - 400*c).toInt()

    return DateTime(year, month, day, hour, minute, second)
}


/**
 * A date and time used for astronomical calculations.
 */
class Time private constructor(
    /**
     * UT1/UTC number of days since noon on January 1, 2000.
     *
     * The floating point number of days of Universal Time since noon UTC January 1, 2000.
     * Astronomy Engine approximates UTC and UT1 as being the same thing, although they are
     * not exactly equivalent; UTC and UT1 can disagree by up to plus or minus 0.9 seconds.
     * This approximation is sufficient for the accuracy requirements of Astronomy Engine.
     *
     * Universal Time Coordinate (UTC) is the international standard for legal and civil
     * timekeeping and replaces the older Greenwich Mean Time (GMT) standard.
     * UTC is kept in sync with unpredictable observed changes in the Earth's rotation
     * by occasionally adding leap seconds as needed.
     *
     * UT1 is an idealized time scale based on observed rotation of the Earth, which
     * gradually slows down in an unpredictable way over time, due to tidal drag by the Moon and Sun,
     * large scale weather events like hurricanes, and internal seismic and convection effects.
     * Conceptually, UT1 drifts from atomic time continuously and erratically, whereas UTC
     * is adjusted by a scheduled whole number of leap seconds as needed.
     *
     * The value in `ut` is appropriate for any calculation involving the Earth's rotation,
     * such as calculating rise/set times, culumination, and anything involving apparent
     * sidereal time.
     *
     * Before the era of atomic timekeeping, days based on the Earth's rotation
     * were often known as *mean solar days*.
     */
    val ut: Double,

    /**
     * Terrestrial Time days since noon on January 1, 2000.
     *
     * Terrestrial Time is an atomic time scale defined as a number of days since noon on January 1, 2000.
     * In this system, days are not based on Earth rotations, but instead by
     * the number of elapsed [SI seconds](https://physics.nist.gov/cuu/Units/second.html)
     * divided by 86400. Unlike `ut`, `tt` increases uniformly without adjustments
     * for changes in the Earth's rotation.
     *
     * The value in `tt` is used for calculations of movements not involving the Earth's rotation,
     * such as the orbits of planets around the Sun, or the Moon around the Earth.
     *
     * Historically, Terrestrial Time has also been known by the term *Ephemeris Time* (ET).
     */
    val tt: Double
): Comparable<Time> {
    /*
     * For internal use only. Used to optimize Earth tilt calculations.
     */
    internal var psi = Double.NaN

    /*
     * For internal use only. Used to optimize Earth tilt calculations.
     */
    internal var eps = Double.NaN

    /*
     * For internal use only. Lazy-caches sidereal time (Earth rotation).
     */
    internal var st = Double.NaN

    constructor(ut: Double) : this(ut, terrestrialTime(ut))

    /**
     * Creates a `Time` object from a UTC year, month, day, hour, minute and second.
     *
     * @param year The UTC year value.
     * @param month The UTC month value 1..12.
     * @param day The UTC day of the month 1..31.
     * @param hour The UTC hour value 0..23.
     * @param minute The UTC minute value 0..59.
     * @param second The UTC second in the half-open range [0, 60).
     */
    constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Double):
        this(universalTimeDays(year, month, day, hour, minute, second))

    /**
     * Resolves this `Time` into year, month, day, hour, minute, second.
     */
    fun toDateTime(): DateTime = dayValueToDateTime(ut)

    /**
     * Converts this `Time` to the integer number of millseconds since 1970.
     */
    fun toMillisecondsSince1970() = round((ut + 10957.5) * MILLISECONDS_PER_DAY).toLong()

    /**
     * Converts this `Time` to ISO 8601 format, expressed in UTC with millisecond resolution.
     *
     * @return Example: "2019-08-30T17:45:22.763Z".
     */
    override fun toString() = toDateTime().toString()

    /**
     * Calculates the sum or difference of an [Time] with a specified floating point number of days.
     *
     * Sometimes we need to adjust a given [Time] value by a certain amount of time.
     * This function adds the given real number of days in `days` to the date and time in this object.
     *
     * More precisely, the result's Universal Time field `ut` is exactly adjusted by `days` and
     * the Terrestrial Time field `tt` is adjusted for the resulting UTC date and time,
     * using a best-fit piecewise polynomial model devised by
     * [Espenak and Meeus](https://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html).
     *
     * @param days A floating point number of days by which to adjust `time`. May be negative, 0, or positive.
     * @return A date and time that is conceptually equal to `time + days`.
     */
    fun addDays(days: Double) = Time(ut + days)

    internal fun julianCenturies() = tt / 36525.0
    internal fun julianMillennia() = tt / DAYS_PER_MILLENNIUM

    /**
     * Compares the chronological order of two `Time` values.
     *
     * Two instances of `Time` can be compared for chronological order
     * using the usual operators like `t1 < t2` or `t1 == t2`.
     */
    override operator fun compareTo(other: Time): Int = this.tt.compareTo(other.tt)

    fun nutationPsi() = psi
    fun nutationEps() = eps

    companion object {
        /**
         * Creates a `Time` object from a Terrestrial Time day value.
         *
         * This function can be used in rare cases where a time must be based
         * on Terrestrial Time (TT) rather than Universal Time (UT).
         * Most developers will want to use `Time(ut)` with a universal time
         * instead of this function, because usually time is based on civil time adjusted
         * by leap seconds to match the Earth's rotation, rather than the uniformly
         * flowing TT used to calculate solar system dynamics. In rare cases
         * where the caller already knows TT, this function is provided to create
         * a `Time` value that can be passed to Astronomy Engine functions.
         *
         * @param tt The number of days after the J2000 epoch.
         */
        @JvmStatic
        fun fromTerrestrialTime(tt: Double) = Time(universalTime(tt), tt)

        /**
         * Creates a `Time` object from the number of milliseconds since the 1970 epoch.
         *
         * Operating systems and runtime libraries commonly measure civil time
         * in integer milliseconds since January 1, 1970 at 00:00 UTC (midnight).
         * To facilitate using such values for astronomy calculations, this
         * function converts a millsecond count into a `Time` object.
         */
        @JvmStatic
        fun fromMillisecondsSince1970(millis: Long) = Time((millis - 946728000000L) / MILLISECONDS_PER_DAY)
    }
}


internal data class TerseVector(var x: Double, var y: Double, var z: Double) {
    fun toAstroVector(time: Time) =
        Vector(x, y, z, time)

    operator fun plus(other: TerseVector) =
        TerseVector(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: TerseVector) =
        TerseVector(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus() =
        TerseVector(-x, -y, -z)

    operator fun times(other: Double) =
        TerseVector(x * other, y * other, z * other)

    operator fun div(denom: Double) =
        TerseVector(x / denom, y / denom, z / denom)

    fun mean(other: TerseVector) =
        TerseVector((x + other.x) / 2.0, (y + other.y) / 2.0, (z + other.z) / 2.0)

    fun quadrature() = (x * x) + (y * y) + (z * z)
    fun magnitude() = sqrt(quadrature())

    fun decrement(other: TerseVector) {
        x -= other.x
        y -= other.y
        z -= other.z
    }

    fun increment(other: TerseVector) {
        x += other.x
        y += other.y
        z += other.z
    }

    fun mix(ramp: Double, other: TerseVector) {
        x = (1.0 - ramp)*x + ramp*other.x
        y = (1.0 - ramp)*y + ramp*other.y
        z = (1.0 - ramp)*z + ramp*other.z
    }

    fun setToZero() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    fun negate() {
        x = -x
        y = -y
        z = -z
    }

    fun copyFrom(other: TerseVector) {
        x = other.x
        y = other.y
        z = other.z
    }

    companion object {
        @JvmStatic
        fun zero() = TerseVector(0.0, 0.0, 0.0)
    }
}

internal operator fun Double.times(vec: TerseVector) =
    TerseVector(this * vec.x, this * vec.y, this * vec.z)


internal fun verifyIdenticalTimes(t1: Time, t2: Time): Time {
    if (t1.tt != t2.tt)
        throw IllegalArgumentException("Attempt to operate on two vectors from different times.")
    return t1
}


/**
 * A 3D Cartesian vector whose components are expressed in Astronomical Units (AU).
 */
data class Vector(
    /**
     * A Cartesian x-coordinate expressed in AU.
     */
    val x: Double,

    /**
     * A Cartesian y-coordinate expressed in AU.
     */
    val y: Double,

    /**
     * A Cartesian z-coordinate expressed in AU.
     */
    val z: Double,

    /**
     * The date and time at which this vector is valid.
     */
    val t: Time
) {
    /**
     * The total distance in AU represented by this vector.
     */
    fun length() = sqrt((x * x) + (y * y) + (z * z))

    /**
     * Adds two vectors. Both operands must have identical times.
     */
    operator fun plus(other: Vector) =
        Vector(x + other.x, y + other.y, z + other.z, verifyIdenticalTimes(t, other.t))

    /**
     * Subtracts one vector from another. Both operands must have identical times.
     */
    operator fun minus(other: Vector) =
        Vector(x - other.x, y - other.y, z - other.z, verifyIdenticalTimes(t, other.t))

    /**
     * Negates a vector; the same as multiplying the vector by the scalar -1.
     */
    operator fun unaryMinus() =
        Vector(-x, -y, -z, t)

    /**
     * Takes the dot product of two vectors.
     */
    infix fun dot(other: Vector): Double {
        verifyIdenticalTimes(t, other.t)
        return x*other.x + y*other.y + z*other.z
    }

    /**
     * Calculates the angle in degrees (0..180) between two vectors.
     */
    fun angleWith(other: Vector): Double {
        val d = (this dot other) / (length() * other.length())
        return when {
            d <= -1.0 -> 180.0
            d >= +1.0 -> 0.0
            else -> acos(d).radiansToDegrees()
        }
    }

    /**
     * Divides a vector by a scalar.
     */
    operator fun div(denom: Double) =
        Vector(x/denom, y/denom, z/denom, t)

    /**
     * Converts Cartesian coordinates to spherical coordinates.
     *
     * Given a Cartesian vector, returns latitude, longitude, and distance.
     */
    fun toSpherical(): Spherical {
        val xyproj = x*x + y*y
        val dist = sqrt(xyproj + z*z)
        val lat: Double
        val lon: Double
        if (xyproj == 0.0) {
            if (z == 0.0) {
                // Indeterminate coordinates; pos vector has zero length.
                throw IllegalArgumentException("Cannot convert zero-length vector to spherical coordinates.")
            }
            lon = 0.0
            lat = if (z < 0.0) -90.0 else +90.0
        } else {
            lon = datan2(y, x).withMinDegreeValue(0.0)
            lat = datan2(z, sqrt(xyproj))
        }
        return Spherical(lat, lon, dist)
    }

    /**
     * Given an equatorial vector, calculates equatorial angular coordinates.
     */
    fun toEquatorial(): Equatorial {
        val sphere = toSpherical()
        return Equatorial(sphere.lon / 15.0, sphere.lat, sphere.dist, this)
    }

    /**
     * Converts Cartesian coordinates to horizontal coordinates.
     *
     * Given a horizontal Cartesian vector, returns horizontal azimuth and altitude.
     * *IMPORTANT:* This function differs from [Vector.toSpherical] in two ways:
     * - `toSpherical` returns a `lon` value that represents azimuth defined counterclockwise
     *   from north (e.g., west = +90), but this function represents a clockwise rotation
     *   (e.g., east = +90). The difference is because `toSpherical` is intended
     *   to preserve the vector "right-hand rule", while this function defines azimuth in a more
     *   traditional way as used in navigation and cartography.
     * - This function optionally corrects for atmospheric refraction, while `toSpherical`
     *   does not.
     *
     * The returned object contains the azimuth in `lon`.
     * It is measured in degrees clockwise from north: east = +90 degrees, west = +270 degrees.
     *
     * The altitude is stored in `lat`.
     *
     * The distance to the observed object is stored in `dist`,
     * and is expressed in astronomical units (AU).
     *
     * @param refraction
     * [Refraction.None]: no atmospheric refraction correction is performed.
     * [Refraction.Normal]: correct altitude for atmospheric refraction.
     * [Refraction.JplHor]: for JPL Horizons compatibility testing only; not recommended for normal use.
     */
    fun toHorizontal(refraction: Refraction): Spherical {
        val sphere = toSpherical()
        return Spherical(
            sphere.lat + refractionAngle(refraction, sphere.lat),
            toggleAzimuthDirection(sphere.lon),
            sphere.dist
        )
    }

    /**
     * Calculates the geographic location corresponding to a geocentric equatorial vector.
     *
     * This is the inverse function of [Observer.toVector].
     * Given an equatorial vector from the center of the Earth to
     * an observer on or near the Earth's surface, this function returns
     * the geographic latitude, longitude, and elevation for that observer.
     *
     * @param equator
     * Selects the date of the Earth's equator in which this vector is expressed.
     * The caller may select [EquatorEpoch.J2000] to use the orientation of the Earth's equator
     * at noon UTC on January 1, 2000, in which case this function corrects for precession
     * and nutation of the Earth as it was at the moment specified by the time `this.t`.
     * Or the caller may select [EquatorEpoch.OfDate] to use the Earth's equator at `this.t`
     * as the orientation.
     *
     * @return The geographic coordinates corresponding to the vector.
     */
    fun toObserver(equator: EquatorEpoch): Observer {
        val vector = when (equator) {
            EquatorEpoch.J2000  -> gyration(this, PrecessDirection.From2000)
            EquatorEpoch.OfDate -> this
        }
        return inverseTerra(vector)
    }

    /**
     * Creates a new vector with the same coordinates but a different time.
     *
     * Usually it is a mistake to add or subtract vectors corresponding
     * to different times. The overloaded operators for adding and subtracting
     * vectors will throw an exception if the times do not match.
     * However, occasionally it is helpful to adjust the time associated
     * with a vector to get around this safety check. For example,
     * the time an event occurs may be different from the time it is observed.
     * The `geoVector` function stores the observation time in the returned vector,
     * while `helioVector` stores the event time.
     *
     * @param time
     * A time to include in a new vector with the same coordinates as this one.
     *
     * @return
     * A cloned vector that has the same coordinates as this one, but at a different time.
     */
    fun withTime(time: Time) = Vector(x, y, z, time)
}

/**
 * Multiply a scalar by a vector, yielding another vector.
 */
operator fun Double.times(vec: Vector) =
    Vector(this*vec.x, this*vec.y, this*vec.z, vec.t)


/**
 * Represents a combined position vector and velocity vector at a given moment in time.
 */
data class StateVector(
    /**
     * A Cartesian position x-coordinate expressed in AU.
     */
    val x: Double,

    /**
     * A Cartesian position y-coordinate expressed in AU.
     */
    val y: Double,

    /**
     * A Cartesian position z-coordinate expressed in AU.
     */
    val z: Double,

    /**
     * A Cartesian velocity x-component expressed in AU/day.
     */
    val vx: Double,

    /**
     * A Cartesian velocity y-component expressed in AU/day.
     */
    val vy: Double,

    /**
     * A Cartesian velocity z-component expressed in AU/day.
     */
    val vz: Double,

    /**
     * The date and time at which this vector is valid.
     */
    val t: Time
) {

    /**
     * Combines a position vector and a velocity vector into a single state vector.
     *
     * @param pos   A position vector.
     * @param vel   A velocity vector.
     * @param time  The common time that represents the given position and velocity.
     */
    constructor(pos: Vector, vel: Vector, time: Time)
        : this(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, time)

    internal constructor(state: BodyState, time: Time)
        : this(state.r.x, state.r.y, state.r.z, state.v.x, state.v.y, state.v.z, time)

    /**
     * Returns the position vector associated with this state vector.
     */
    fun position() = Vector(x, y, z, t)

    /**
     * Returns the velocity vector associated with this state vector.
     */
    fun velocity() = Vector(vx, vy, vz, t)

    /**
     * Adds two state vetors, yielding the state vector sum.
     */
    operator fun plus(other: StateVector) =
        StateVector(
            x + other.x,
            y + other.y,
            z + other.z,
            vx + other.vx,
            vy + other.vy,
            vz + other.vz,
            verifyIdenticalTimes(t, other.t)
        )

    /**
     * Subtracts two state vetors, yielding the state vector difference.
     */
    operator fun minus(other: StateVector) =
        StateVector(
            x - other.x,
            y - other.y,
            z - other.z,
            vx - other.vx,
            vy - other.vy,
            vz - other.vz,
            verifyIdenticalTimes(t, other.t)
        )

    /**
     * Divides a state vector by a scalar.
     */
    operator fun div(denom: Double) =
        StateVector(
            x / denom,
            y / denom,
            z / denom,
            vx / denom,
            vy / denom,
            vz / denom,
            t
        )

    /**
     * Negates a state vector; the same as multiplying the state vector by the scalar -1.
     */
    operator fun unaryMinus() =
        StateVector(-x, -y, -z, -vx, -vy, -vz, t)
}


/**
 * Holds the positions and velocities of Jupiter's major 4 moons.
 *
 * The [jupiterMoons] function returns an object of this type
 * to report position and velocity vectors for Jupiter's largest 4 moons
 * Io, Europa, Ganymede, and Callisto. Each position vector is relative
 * to the center of Jupiter. Both position and velocity are oriented in
 * the EQJ system (that is, using Earth's equator at the J2000 epoch).
 * The positions are expressed in astronomical units (AU),
 * and the velocities in AU/day.
 */
class JupiterMoonsInfo(
    /**
     * The position and velocity of Jupiter's moon Io.
     */
    val io: StateVector,

    /**
     * The position and velocity of Jupiter's moon Europa.
     */
    val europa: StateVector,

    /**
     * The position and velocity of Jupiter's moon Ganymede.
     */
    val ganymede: StateVector,

    /**
     * The position and velocity of Jupiter's moon Callisto.
     */
    val callisto: StateVector
)

/**
 * A rotation matrix that can be used to transform one coordinate system to another.
 */
class RotationMatrix(
    /**
     * A 3x3 array of numbers to initialize the rotation matrix.
     */
    val rot: Array<DoubleArray>
) {
    init {
        if (rot.size != 3 || rot.any { it.size != 3 })
            throw IllegalArgumentException("Rotation matrix must be a 3x3 array.")
    }

    constructor(
        a00: Double, a01: Double, a02: Double,
        a10: Double, a11: Double, a12: Double,
        a20: Double, a21: Double, a22: Double
    ) : this(
        arrayOf(
            doubleArrayOf(a00, a01, a02),
            doubleArrayOf(a10, a11, a12),
            doubleArrayOf(a20, a21, a22)
        )
    )

    /**
     * Calculates the inverse of a rotation matrix.
     *
     * Returns a rotation matrix that performs the reverse transformation
     * that this rotation matrix performs.
     */
    fun inverse() = RotationMatrix(
        rot[0][0], rot[1][0], rot[2][0],
        rot[0][1], rot[1][1], rot[2][1],
        rot[0][2], rot[1][2], rot[2][2]
    )

    /**
     * Applies a rotation to a vector, yielding a rotated vector.
     *
     * This function transforms a vector in one orientation to a vector
     * in another orientation.
     *
     * @param vec
     * The vector whose orientation is to be changed.
     */
    fun rotate(vec: Vector) = Vector(
        rot[0][0]*vec.x + rot[1][0]*vec.y + rot[2][0]*vec.z,
        rot[0][1]*vec.x + rot[1][1]*vec.y + rot[2][1]*vec.z,
        rot[0][2]*vec.x + rot[1][2]*vec.y + rot[2][2]*vec.z,
        vec.t
    )

    /**
     * Applies a rotation to a state vector, yielding a rotated state vector.
     *
     * This function transforms a state vector in one orientation to a state vector in another orientation.
     * The resulting state vector has both position and velocity reoriented.
     *
     * @param state
     * The state vector whose orientation is to be changed.
     * The value of `state` is not changed; the return value is a new state vector object.
     */
    fun rotate(state: StateVector) = StateVector(
        rotate(state.position()),
        rotate(state.velocity()),
        state.t
    )

    /**
     * Creates a rotation based on applying one rotation followed by another.
     *
     * Given two rotation matrices, returns a combined rotation matrix that is
     * equivalent to rotating based on this matrix, followed by the matrix `other`.
     */
    infix fun combine(other: RotationMatrix) = RotationMatrix (
        other.rot[0][0]*rot[0][0] + other.rot[1][0]*rot[0][1] + other.rot[2][0]*rot[0][2],
        other.rot[0][1]*rot[0][0] + other.rot[1][1]*rot[0][1] + other.rot[2][1]*rot[0][2],
        other.rot[0][2]*rot[0][0] + other.rot[1][2]*rot[0][1] + other.rot[2][2]*rot[0][2],
        other.rot[0][0]*rot[1][0] + other.rot[1][0]*rot[1][1] + other.rot[2][0]*rot[1][2],
        other.rot[0][1]*rot[1][0] + other.rot[1][1]*rot[1][1] + other.rot[2][1]*rot[1][2],
        other.rot[0][2]*rot[1][0] + other.rot[1][2]*rot[1][1] + other.rot[2][2]*rot[1][2],
        other.rot[0][0]*rot[2][0] + other.rot[1][0]*rot[2][1] + other.rot[2][0]*rot[2][2],
        other.rot[0][1]*rot[2][0] + other.rot[1][1]*rot[2][1] + other.rot[2][1]*rot[2][2],
        other.rot[0][2]*rot[2][0] + other.rot[1][2]*rot[2][1] + other.rot[2][2]*rot[2][2]
    )

    /**
     * Re-orients the rotation matrix by pivoting it by an angle around one of its axes.
     *
     * Given this rotation matrix, a selected coordinate axis, and an angle in degrees,
     * this function pivots the rotation matrix by that angle around that coordinate axis.
     * The function returns a new rotation matrix; it does not mutate this matrix.
     *
     * For example, if you have rotation matrix that converts ecliptic coordinates (ECL)
     * to horizontal coordinates (HOR), but you really want to convert ECL to the orientation
     * of a telescope camera pointed at a given body, you can use `RotationMatrix.pivot` twice:
     * (1) pivot around the zenith axis by the body's azimuth, then (2) pivot around the
     * western axis by the body's altitude angle. The resulting rotation matrix will then
     * reorient ECL coordinates to the orientation of your telescope camera.
     *
     * @param axis
     * An integer that selects which coordinate axis to rotate around:
     * 0 = x, 1 = y, 2 = z. Any other value will cause an exception.
     *
     * @param angle
     * An angle in degrees indicating the amount of rotation around the specified axis.
     * Positive angles indicate rotation counterclockwise as seen from the positive
     * direction along that axis, looking towards the origin point of the orientation system.
     * Any finite number of degrees is allowed, but best precision will result from keeping
     * `angle` in the range [-360, +360].
     */
    fun pivot(axis: Int, angle: Double): RotationMatrix {
        if (axis < 0 || axis > 2)
            throw IllegalArgumentException("Invalid coordinate axis $axis. Must be 0..2.")

        if (!angle.isFinite())
            throw IllegalArgumentException("Angle must be a finite number.")

        val radians = angle.degreesToRadians()
        val c = cos(radians)
        val s = sin(radians)

        // We need to maintain the "right-hand" rule, no matter which
        // axis was selected. That means we pick (i, j, k) axis order
        // such that the following vector cross product is satisfied:
        // i x j = k
        val i = (axis + 1) % 3
        val j = (axis + 2) % 3
        val k = axis

        val piv = arrayOf(DoubleArray(3), DoubleArray(3), DoubleArray(3))

        piv[i][i] = c*rot[i][i] - s*rot[i][j]
        piv[i][j] = s*rot[i][i] + c*rot[i][j]
        piv[i][k] = rot[i][k]
        piv[j][i] = c*rot[j][i] - s*rot[j][j]
        piv[j][j] = s*rot[j][i] + c*rot[j][j]
        piv[j][k] = rot[j][k]
        piv[k][i] = c*rot[k][i] - s*rot[k][j]
        piv[k][j] = s*rot[k][i] + c*rot[k][j]
        piv[k][k] = rot[k][k]

        return RotationMatrix(piv)
    }

    companion object {
        /**
         * Creates an identity rotation matrix.
         *
         * Creates a matrix that has no effect on orientation.
         * This matrix can be the starting point for other operations,
         * such as calling a series of [RotationMatrix.combine] or [RotationMatrix.pivot].
         */
        @JvmStatic
        fun identity() = RotationMatrix(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }
}


/**
 * Spherical coordinates: latitude, longitude, distance.
 */
data class Spherical(
    /**
     * The latitude angle: -90..+90 degrees.
     */
    val lat: Double,

    /**
     * The longitude angle: 0..360 degrees.
     */
    val lon: Double,

    /**
     * Distance in AU.
     */
    val dist: Double
) {
    /**
     * Converts spherical coordinates to Cartesian coordinates.
     *
     * Given spherical coordinates and a time at which they are valid,
     * returns a vector of Cartesian coordinates. The returned value
     * includes the time, as required by the type [Vector].
     *
     * @param time
     * The time that should be included in the return value.
     */
    fun toVector(time: Time): Vector {
        val radlat = lat.degreesToRadians()
        val radlon = lon.degreesToRadians()
        val rcoslat = dist * cos(radlat)
        return Vector(
            rcoslat * cos(radlon),
            rcoslat * sin(radlon),
            dist * sin(radlat),
            time
        )
    }

    /**
     * Given apparent angular horizontal coordinates, calculate the unrefracted horizontal vector.
     *
     * Assumes `this` contains apparent horizontal coordinates:
     * `lat` holds the refracted altitude angle,
     * `lon` holds the azimuth in degrees clockwise from north,
     * and `dist` holds the distance from the observer to the object in AU.
     *
     * @param time
     * The date and time of the observation. This is needed because the returned
     * [Vector] requires a valid time value when passed to certain other functions.
     *
     * @param refraction
     * The refraction option used to model atmospheric lensing. See [refractionAngle].
     * This specifies how refraction is to be removed from the altitude stored in `this.lat`.
     *
     * @return A vector in the horizontal system: `x` = north, `y` = west, and `z` = zenith (up).
     */
    fun toVectorFromHorizon(time: Time, refraction: Refraction): Vector =
        Spherical(
            lat + inverseRefractionAngle(refraction, lat),
            toggleAzimuthDirection(lon),
            dist
        )
        .toVector(time)
}

/**
 * The location of an observer on (or near) the surface of the Earth.
 *
 * This object is passed to functions that calculate phenomena as observed
 * from a particular place on the Earth.
 */
data class Observer(
    /**
     * Geographic latitude in degrees north (positive) or south (negative) of the equator.
     */
    val latitude: Double,

    /**
     * Geographic longitude in degrees east (positive) or west (negative) of the prime meridian at Greenwich, England.
     */
    val longitude: Double,

    /**
     * The height above (positive) or below (negative) sea level, expressed in meters.
     */
    val height: Double
) {
    /**
     * Calculates geocentric equatorial coordinates of an observer on the surface of the Earth.
     *
     * This function calculates a vector from the center of the Earth to
     * a point on or near the surface of the Earth, expressed in equatorial
     * coordinates. It takes into account the rotation of the Earth at the given
     * time, along with the given latitude, longitude, and elevation of the observer.
     *
     * The caller may pass a value in `equator` to select either [EquatorEpoch.J2000]
     * for using J2000 coordinates, or [EquatorEpoch.OfDate] for using coordinates relative
     * to the Earth's equator at the specified time.
     *
     * The returned vector has components expressed in astronomical units (AU).
     * To convert to kilometers, multiply the vector values by the scalar value [KM_PER_AU].
     *
     * The inverse of this function is also available: [Vector.toObserver].
     *
     * @param time
     * The date and time for which to calculate the observer's position vector.
     *
     * @param equator
     * Selects the date of the Earth's equator in which to express the equatorial coordinates.
     * The caller may select [EquatorEpoch.J2000] to use the orientation of the Earth's equator
     * at noon UTC on January 1, 2000, in which case this function corrects for precession
     * and nutation of the Earth as it was at the moment specified by the `time` parameter.
     * Or the caller may select [EquatorEpoch.OfDate] to use the Earth's equator at `time`
     * as the orientation.
     *
     * @return A vector from the center of the Earth to this geographic location.
     */
    fun toVector(time: Time, equator: EquatorEpoch): Vector =
        toStateVector(time, equator).position()

    /**
     * Calculates geocentric equatorial position and velocity of an observer on the surface of the Earth.
     *
     * This function calculates position and velocity vectors of an observer
     * on or near the surface of the Earth, expressed in equatorial
     * coordinates. It takes into account the rotation of the Earth at the given
     * time, along with the given latitude, longitude, and elevation of the observer.
     *
     * The caller may pass a value in `equator` to select either [EquatorEpoch.J2000]
     * for using J2000 coordinates, or [EquatorEpoch.OfDate] for using coordinates relative
     * to the Earth's equator at the specified time.
     *
     * The returned position vector has components expressed in astronomical units (AU).
     * To convert to kilometers, multiply the vector values by the scalar value [KM_PER_AU].
     *
     * The returned velocity vector is measured in AU/day.
     *
     * @param time
     * The date and time for which to calculate the observer's position vector.
     *
     * @param equator
     * Selects the date of the Earth's equator in which to express the equatorial coordinates.
     * The caller may select [EquatorEpoch.J2000] to use the orientation of the Earth's equator
     * at noon UTC on January 1, 2000, in which case this function corrects for precession
     * and nutation of the Earth as it was at the moment specified by the `time` parameter.
     * Or the caller may select [EquatorEpoch.OfDate] to use the Earth's equator at `time`
     * as the orientation.
     *
     * @return The position and velocity of this observer with respect to the Earth's center.
     */
    fun toStateVector(time: Time, equator: EquatorEpoch): StateVector {
        val state = terra(this, time)
        return when (equator) {
            EquatorEpoch.OfDate -> state
            EquatorEpoch.J2000  -> gyrationPosVel(state, PrecessDirection.Into2000)
        }
    }
}


/**
 * Selects the date for which the Earth's equator is to be used for representing equatorial coordinates.
 *
 * The Earth's equator is not always in the same plane due to precession and nutation.
 *
 * Sometimes it is useful to have a fixed plane of reference for equatorial coordinates
 * across different calendar dates.  In these cases, a fixed *epoch*, or reference time,
 * is helpful. Astronomy Engine provides the J2000 epoch for such cases.  This refers
 * to the plane of the Earth's orbit as it was on noon UTC on 1 January 2000.
 *
 * For some other purposes, it is more helpful to represent coordinates using the Earth's
 * equator exactly as it is on that date. For example, when calculating rise/set times
 * or horizontal coordinates, it is most accurate to use the orientation of the Earth's
 * equator at that same date and time. For these uses, Astronomy Engine allows *of-date*
 * calculations.
 */
enum class EquatorEpoch {
    /**
     * Represent equatorial coordinates in the J2000 epoch.
     */
    J2000,

    /**
     * Represent equatorial coordinates using the Earth's equator at the given date and time.
     */
    OfDate,
}


/**
 * Aberration calculation options.
 *
 * [Aberration](https://en.wikipedia.org/wiki/Aberration_of_light) is an effect
 * causing the apparent direction of an observed body to be shifted due to transverse
 * movement of the Earth with respect to the rays of light coming from that body.
 * This angular correction can be anywhere from 0 to about 20 arcseconds,
 * depending on the position of the observed body relative to the instantaneous
 * velocity vector of the Earth.
 *
 * Some Astronomy Engine functions allow optional correction for aberration by
 * passing in a value of this enumerated type.
 *
 * Aberration correction is useful to improve accuracy of coordinates of
 * apparent locations of bodies seen from the Earth.
 * However, because aberration affects not only the observed body (such as a planet)
 * but the surrounding stars, aberration may be unhelpful (for example)
 * for determining exactly when a planet crosses from one constellation to another.
 */
enum class Aberration {
    /**
     * Request correction for aberration.
     */
    Corrected,

    /**
     * Do not correct for aberration.
     */
    None,
}


/**
 * Selects whether to correct for atmospheric refraction, and if so, how.
 */
enum class Refraction {
    /**
     * No atmospheric refraction correction (airless).
     */
    None,

    /**
     * Recommended correction for standard atmospheric refraction.
     */
    Normal,

    /**
     * Used only for compatibility testing with JPL Horizons online tool.
     */
    JplHor,
}


/**
 * Selects whether to search for a rising event or a setting event for a celestial body.
 */
enum class Direction(
    /**
     * A numeric value that is helpful in formulas involving rise/set.
     * The sign is +1 for a rising event, or -1 for a setting event.
     */
    val sign: Int
) {
    /**
     * Indicates a rising event: a celestial body is observed to rise above the horizon by an observer on the Earth.
     */
    Rise(+1),

    /**
     * Indicates a setting event: a celestial body is observed to sink below the horizon by an observer on the Earth.
     */
    Set(-1),
}


/**
 * Indicates whether a body (especially Mercury or Venus) is best seen in the morning or evening.
 */
enum class Visibility {
    /**
     * The body is best visible in the morning, before sunrise.
     */
    Morning,

    /**
     * The body is best visible in the evening, after sunset.
     */
    Evening,
}


/**
 * Equatorial angular and cartesian coordinates.
 *
 * Coordinates of a celestial body as seen from the Earth
 * (geocentric or topocentric, depending on context),
 * oriented with respect to the projection of the Earth's equator onto the sky.
 */
class Equatorial(
    /**
     * Right ascension in sidereal hours.
     */
    val ra: Double,

    /**
     * Declination in degrees.
     */
    val dec: Double,

    /**
     * Distance to the celestial body in AU.
     */
    val dist: Double,

    /**
     * Equatorial coordinates in cartesian vector form: x = March equinox, y = June solstice, z = north.
     */
    val vec: Vector
)


/**
 * Ecliptic angular and Cartesian coordinates.
 *
 * Coordinates of a celestial body as seen from the center of the Sun (heliocentric),
 * oriented with respect to the plane of the Earth's orbit around the Sun (the ecliptic).
 */
data class Ecliptic(
    /**
     * Cartesian ecliptic vector, with components as follows:
     * x: the direction of the equinox along the ecliptic plane.
     * y: in the ecliptic plane 90 degrees prograde from the equinox.
     * z: perpendicular to the ecliptic plane. Positive is north.
     */
    val vec: Vector,

    /**
     * Latitude in degrees north (positive) or south (negative) of the ecliptic plane.
     */
    val elat: Double,

    /**
     * Longitude in degrees around the ecliptic plane prograde from the equinox.
     */
    val elon: Double
)


/**
 * Coordinates of a celestial body as seen by a topocentric observer.
 *
 * Horizontal and equatorial coordinates seen by an observer on or near
 * the surface of the Earth (a topocentric observer).
 * Optionally corrected for atmospheric refraction.
 */
data class Topocentric(
    /**
     * Compass direction around the horizon in degrees. 0=North, 90=East, 180=South, 270=West.
     */
    val azimuth: Double,

    /**
     * Angle in degrees above (positive) or below (negative) the observer's horizon.
     */
    val altitude: Double,

    /**
     * Right ascension in sidereal hours.
     */
    val ra: Double,

    /**
     * Declination in degrees.
     */
    val dec: Double
)


/**
 * The dates and times of changes of season for a given calendar year.
 *
 * Call [seasons] to calculate this data structure for a given year.
 */
class SeasonsInfo(
    /**
     * The date and time of the March equinox for the specified year.
     */
    val marchEquinox: Time,

    /**
     * The date and time of the June soltice for the specified year.
     */
    val juneSolstice: Time,

    /**
     * The date and time of the September equinox for the specified year.
     */
    val septemberEquinox: Time,

    /**
     * The date and time of the December solstice for the specified year.
     */
    val decemberSolstice: Time
)


/**
 * Information about idealized atmospheric variables at a given elevation.
 */
class AtmosphereInfo(
    /**
     * Atmospheric pressure in pascals.
     */
    val pressure: Double,

    /**
     * Atmospheric temperature in kelvins.
     */
    val temperature: Double,

    /**
     * Atmospheric density relative to sea level.
     */
    val density: Double
)



/**
 * A lunar quarter event (new moon, first quarter, full moon, or third quarter) along with its date and time.
 */
class MoonQuarterInfo(
    /**
    * 0=new moon, 1=first quarter, 2=full moon, 3=third quarter.
    */
    val quarter: Int,

    /**
    * The date and time of the lunar quarter.
    */
    val time: Time
)


/**
 * Lunar libration angles, returned by [libration].
 */
data class LibrationInfo(
    /**
     * Sub-Earth libration ecliptic latitude angle, in degrees.
     */
    val elat: Double,

    /**
     * Sub-Earth libration ecliptic longitude angle, in degrees.
     */
    val elon: Double,

    /**
     * Moon's geocentric ecliptic latitude.
     */
    val mlat: Double,

    /**
     * Moon's geocentric ecliptic longitude.
     */
    val mlon: Double,

    /**
     * Distance between the centers of the Earth and Moon in kilometers.
     */
    val distanceKm: Double,

    /**
     * The apparent angular diameter of the Moon, in degrees, as seen from the center of the Earth.
     */
    val diamDeg: Double
)


/**
 * Information about a celestial body crossing a specific hour angle.
 *
 * Returned by the function [searchHourAngle] to report information about
 * a celestial body crossing a certain hour angle as seen by a specified topocentric observer.
 */
class HourAngleInfo(
    /**
     * The date and time when the body crosses the specified hour angle.
     */
    val time: Time,

    /**
     * Apparent coordinates of the body at the time it crosses the specified hour angle.
     */
    val hor: Topocentric
)


/**
 * Contains information about the visibility of a celestial body at a given date and time.
 *
 * See [elongation] for more detailed information about the members of this class.
 * See also [searchMaxElongation] for how to search for maximum elongation events.
 */
class ElongationInfo(
    /**
     * The date and time of the observation.
     */
    val time: Time,

    /**
     * Whether the body is best seen in the morning or the evening.
     */
    val visibility: Visibility,

    /**
     * The angle in degrees between the body and the Sun, as seen from the Earth.
     */
    val elongation: Double,

    /**
     * The difference between the ecliptic longitudes of the body and the Sun, as seen from the Earth.
     */
    val eclipticSeparation: Double
) {
    private fun validateAngle(angle: Double, name: String) {
        if (angle < 0.0 || angle > 180.0) {
            throw InternalError("$name angle is not in the required range [0, 180].")
        }
    }

    init {
        validateAngle(elongation, "Elongation")
        validateAngle(eclipticSeparation, "Ecliptic separation")
    }
}


/**
 * The type of apsis: pericenter (closest approach) or apocenter (farthest distance).
 */
enum class ApsisKind {
    /**
     * The body is at its closest approach to the object it orbits.
     */
    Pericenter,

    /**
     * The body is at its farthest distance from the object it orbits.
     */
    Apocenter,
}


/**
 * An apsis event: pericenter (closest approach) or apocenter (farthest distance).
 *
 * For the Moon orbiting the Earth, or a planet orbiting the Sun, an *apsis* is an
 * event where the orbiting body reaches its closest or farthest point from the primary body.
 * The closest approach is called *pericenter* and the farthest point is *apocenter*.
 *
 * More specific terminology is common for particular orbiting bodies.
 * The Moon's closest approach to the Earth is called *perigee* and its farthest
 * point is called *apogee*. The closest approach of a planet to the Sun is called
 * *perihelion* and the furthest point is called *aphelion*.
 *
 * This data structure is returned by [searchLunarApsis] and [nextLunarApsis]
 * to iterate through consecutive alternating perigees and apogees.
 */
class ApsisInfo(
    /**
     * The date and time of the apsis.
     */
    val time: Time,

    /**
     * Whether this is a pericenter or apocenter event.
     */
    val kind: ApsisKind,

    /**
     * The distance between the centers of the bodies in astronomical units.
     */
    val distanceAu: Double,
) {
    /**
     * The distance between the centers of the bodies in kilometers.
     */
    val distanceKm = distanceAu * KM_PER_AU
}


/**
 * The different kinds of lunar/solar eclipses.
 */
enum class EclipseKind {
    /**
     * A penumbral lunar eclipse. (Never used for a solar eclipse.)
     */
    Penumbral,

    /**
     * A partial lunar/solar eclipse.
     */
    Partial,

    /**
     * An annular solar eclipse. (Never used for a lunar eclipse.)
     */
    Annular,

    /**
     * A total lunar/solar eclipse.
     */
    Total,
}


/**
 * Information about a lunar eclipse.
 *
 * Returned by [searchLunarEclipse] or [nextLunarEclipse]
 * to report information about a lunar eclipse event.
 * When a lunar eclipse is found, it is classified as penumbral, partial, or total.
 * Penumbral eclipses are difficult to observe, because the Moon is only slightly dimmed
 * by the Earth's penumbra; no part of the Moon touches the Earth's umbra.
 * Partial eclipses occur when part, but not all, of the Moon touches the Earth's umbra.
 * Total eclipses occur when the entire Moon passes into the Earth's umbra.
 *
 * The `kind` field thus holds `EclipseKind.Penumbral`, `EclipseKind.Partial`,
 * or `EclipseKind.Total`, depending on the kind of lunar eclipse found.
 *
 * The `obscuration` field holds a value in the range [0, 1] that indicates what fraction
 * of the Moon's apparent disc area is covered by the Earth's umbra at the eclipse's peak.
 * This indicates how dark the peak eclipse appears. For penumbral eclipses, the obscuration
 * is 0, because the Moon does not pass through the Earth's umbra. For partial eclipses,
 * the obscuration is somewhere between 0 and 1. For total lunar eclipses, the obscuration is 1.
 *
 * Field `peak` holds the date and time of the center of the eclipse, when it is at its peak.
 *
 * Fields `sdPenum`, `sdPartial`, and `sdTotal` hold the semi-duration of each phase
 * of the eclipse, which is half of the amount of time the eclipse spends in each
 * phase (expressed in minutes), or 0.0 if the eclipse never reaches that phase.
 * By converting from minutes to days, and subtracting/adding with `peak`, the caller
 * may determine the date and time of the beginning/end of each eclipse phase.
 */
class LunarEclipseInfo(
    /**
     * The type of lunar eclipse found.
     */
    val kind: EclipseKind,

    /**
     * The peak fraction of the Moon's apparent disc that is covered by the Earth's umbra.
     */
    val obscuration: Double,

    /**
     * The time of the eclipse at its peak.
     */
    val peak: Time,

    /**
     * The semi-duration of the penumbral phase in minutes.
     */
    val sdPenum: Double,

    /**
     * The semi-duration of the partial phase in minutes, or 0.0 if none.
     */
    val sdPartial: Double,

    /**
     * The semi-duration of the total phase in minutes, or 0.0 if none.
     */
    val sdTotal: Double
)


/**
 * Reports the time and geographic location of the peak of a solar eclipse.
 *
 * Returned by [searchGlobalSolarEclipse] or [nextGlobalSolarEclipse]
 * to report information about a solar eclipse event.
 *
 * The eclipse is classified as partial, annular, or total, depending on the
 * maximum amount of the Sun's disc obscured, as seen at the peak location
 * on the surface of the Earth.
 *
 * The `kind` field thus holds `EclipseKind.Partial`, `EclipseKind.Annular`, or `EclipseKind.Total`.
 * A total eclipse is when the peak observer sees the Sun completely blocked by the Moon.
 * An annular eclipse is like a total eclipse, but the Moon is too far from the Earth's surface
 * to completely block the Sun; instead, the Sun takes on a ring-shaped appearance.
 * A partial eclipse is when the Moon blocks part of the Sun's disc, but nobody on the Earth
 * observes either a total or annular eclipse.
 *
 * If `kind` is `EclipseKind.Total` or `EclipseKind.Annular`, the `latitude` and `longitude`
 * fields give the geographic coordinates of the center of the Moon's shadow projected
 * onto the daytime side of the Earth at the instant of the eclipse's peak.
 * If `kind` has any other value, `latitude` and `longitude` are undefined and should
 * not be used.
 *
 * For total or annular eclipses, the `obscuration` field holds the fraction (0, 1]
 * of the Sun's apparent disc area that is blocked from view by the Moon's silhouette,
 * as seen by an observer located at the geographic coordinates `latitude`, `longitude`
 * at the darkest time `peak`. The value will always be 1 for total eclipses, and less than
 * 1 for annular eclipses.
 * For partial eclipses, `obscuration` is undefined and should not be used.
 * This is because there is little practical use for an obscuration value of
 * a partial eclipse without supplying a particular observation location.
 * Developers who wish to find an obscuration value for partial solar eclipses should therefore use
 * [searchLocalSolarEclipse] and provide the geographic coordinates of an observer.
 */
class GlobalSolarEclipseInfo(
    /**
     * The type of solar eclipse: `EclipseKind.Partial`, `EclipseKind.Annular`, or `EclipseKind.Total`.
     */
    val kind: EclipseKind,

    /**
     * The peak fraction of the Sun's apparent disc area obscured by the Moon (total and annular eclipses only).
     */
    val obscuration: Double,

    /**
     * The date and time when the solar eclipse is darkest.
     * This is the instant when the axis of the Moon's shadow cone passes closest to the Earth's center.
     */
    val peak: Time,

    /**
     * The distance between the Sun/Moon shadow axis and the center of the Earth, in kilometers.
     */
    val distance: Double,

    /**
     * The geographic latitude at the center of the peak eclipse shadow.
     */
    val latitude: Double,

    /**
     * The geographic longitude at the center of the peak eclipse shadow.
     */
    val longitude: Double
)


/**
 * Holds a time and the observed altitude of the Sun at that time.
 *
 * When reporting a solar eclipse observed at a specific location on the Earth
 * (a "local" solar eclipse), a series of events occur. In addition
 * to the time of each event, it is important to know the altitude of the Sun,
 * because each event may be invisible to the observer if the Sun is below
 * the horizon (i.e. it at night).
 *
 * If `altitude` is negative, the event is theoretical only; it would be
 * visible if the Earth were transparent, but the observer cannot actually see it.
 * If `altitude` is positive but less than a few degrees, visibility will be impaired by
 * atmospheric interference (sunrise or sunset conditions).
 */
class EclipseEvent (
    /**
     * The date and time of the event.
     */
    val time: Time,

    /**
     * The angular altitude of the center of the Sun above/below the horizon, at `time`,
     * corrected for atmospheric refraction and expressed in degrees.
     */
    val altitude: Double
)


/**
 * Information about a solar eclipse as seen by an observer at a given time and geographic location.
 *
 * Returned by [searchLocalSolarEclipse] or [nextLocalSolarEclipse]
 * to report information about a solar eclipse as seen at a given geographic location.
 *
 * When a solar eclipse is found, it is classified as partial, annular, or total.
 * The `kind` field thus holds `EclipseKind.Partial`, `EclipseKind.Annular`, or `EclipseKind.Total`.
 * A partial solar eclipse is when the Moon does not line up directly enough with the Sun
 * to completely block the Sun's light from reaching the observer.
 * An annular eclipse occurs when the Moon's disc is completely visible against the Sun
 * but the Moon is too far away to completely block the Sun's light; this leaves the
 * Sun with a ring-like appearance.
 * A total eclipse occurs when the Moon is close enough to the Earth and aligned with the
 * Sun just right to completely block all sunlight from reaching the observer.
 *
 * The `obscuration` field reports what fraction of the Sun's disc appears blocked
 * by the Moon when viewed by the observer at the peak eclipse time.
 * This is a value that ranges from 0 (no blockage) to 1 (total eclipse).
 * The obscuration value will be between 0 and 1 for partial eclipses and annular eclipses.
 * The value will be exactly 1 for total eclipses. Obscuration gives an indication
 * of how dark the eclipse appears.
 *
 * There are 5 "event" fields, each of which contains a time and a solar altitude.
 * Field `peak` holds the date and time of the center of the eclipse, when it is at its peak.
 * The fields `partialBegin` and `partialEnd` are always set, and indicate when
 * the eclipse begins/ends. If the eclipse reaches totality or becomes annular,
 * `totalBegin` and `totalEnd` indicate when the total/annular phase begins/ends.
 * When an event field is valid, the caller must also check its `altitude` field to
 * see whether the Sun is above the horizon at the time indicated by the `time` field.
 * </remarks>
 */
class LocalSolarEclipseInfo (
    /**
     * The type of solar eclipse: `EclipseKind.Partial`, `EclipseKind.Annular`, or `EclipseKind.Total`.
     */
    val kind: EclipseKind,

    /**
     * The fraction of the Sun's apparent disc area obscured by the Moon at the eclipse peak.
     */
    val obscuration: Double,

    /**
     * The time and Sun altitude at the beginning of the eclipse.
     */
    val partialBegin: EclipseEvent,

    /**
     * If this is an annular or a total eclipse, the time and Sun altitude when annular/total phase begins; otherwise `null`.
     */
    val totalBegin: EclipseEvent?,

    /**
     * The time and Sun altitude when the eclipse reaches its peak.
     */
    val peak: EclipseEvent,

    /**
     * If this is an annular or a total eclipse, the time and Sun altitude when annular/total phase ends; otherwise `null`.
     */
    val totalEnd: EclipseEvent?,

    /**
     * The time and Sun altitude at the end of the eclipse.
     */
    val partialEnd: EclipseEvent
)


/**
 * Information about a transit of Mercury or Venus, as seen from the Earth.
 *
 * Returned by [searchTransit] or [nextTransit] to report
 * information about a transit of Mercury or Venus.
 * A transit is when Mercury or Venus passes between the Sun and Earth so that
 * the other planet is seen in silhouette against the Sun.
 *
 * The `start` field reports the moment in time when the planet first becomes
 * visible against the Sun in its background.
 * The `peak` field reports when the planet is most aligned with the Sun,
 * as seen from the Earth.
 * The `finish` field reports the last moment when the planet is visible
 * against the Sun in its background.
 *
 * The calculations are performed from the point of view of a geocentric observer.
 */
class TransitInfo(
    /**
     * Date and time at the beginning of the transit.
     */
    val start: Time,

    /**
     * Date and time of the peak of the transit.
     */
    val peak: Time,

    /**
     * Date and time at the end of the transit.
     */
    val finish: Time,

    /**
     * Angular separation in arcminutes between the centers of the Sun and the planet at time `peak`.
     */
    val separation: Double
)


internal class ShadowInfo(
    /**
     * The time of the shadow state.
     */
    val time: Time,

    /**
     * km distance between center of Moon and the line passing through the centers of the Sun and Earth.
     */
    val r: Double,

    /**
     * Umbra radius in km, at the shadow plane.
     */
    val k: Double,

    /**
     * Penumbra radius in km, at the shadow plane.
     */
    val p: Double,

    /**
     * Coordinates of target body relative to shadow-casting body at `time`.
     */
    val target: Vector,

    /**
     * Heliocentric coordinates of shadow-casting body at `time`.
     */
    val dir: Vector
)

internal fun calcShadow(
    bodyRadiusKm: Double,
    time: Time,
    target: Vector,
    dir: Vector
): ShadowInfo {
    val u = (dir dot target) / (dir dot dir)
    val dx = (u * dir.x) - target.x
    val dy = (u * dir.y) - target.y
    val dz = (u * dir.z) - target.z
    val r = KM_PER_AU * sqrt(dx*dx + dy*dy + dz*dz)
    val k = +SUN_RADIUS_KM - (1.0 + u)*(SUN_RADIUS_KM - bodyRadiusKm)
    val p = -SUN_RADIUS_KM + (1.0 + u)*(SUN_RADIUS_KM + bodyRadiusKm)
    return ShadowInfo(time, r, k, p, target, dir)
}

internal fun earthShadow(time: Time): ShadowInfo {
    // This function helps find when the Earth's shadow falls upon the Moon.
    val s = geoVector(Body.Sun, time, Aberration.Corrected)
    val m = geoMoon(time)
    return calcShadow(EARTH_ECLIPSE_RADIUS_KM, time, m, -s)
}

internal fun moonShadow(time: Time): ShadowInfo {
    // This function helps find when the Moon's shadow falls upon the Earth.
    val s = geoVector(Body.Sun, time, Aberration.Corrected)
    val m = geoMoon(time)

    // -m  = lunacentric Earth
    // m-s = heliocentric Moon
    return calcShadow(MOON_MEAN_RADIUS_KM, time, -m, m-s)
}

internal fun localMoonShadow(time: Time, observer: Observer): ShadowInfo {
    // Calculate observer's geocentric position.
    val o = geoPos(time, observer)

    // Calculate light-travel and aberration corrected Sun.
    val s = geoVector(Body.Sun, time, Aberration.Corrected)

    // Calculate geocentric Moon.
    val m = geoMoon(time)

    // o-m = lunacentric observer
    // m-s = heliocentric Moon
    return calcShadow(MOON_MEAN_RADIUS_KM, time, o-m, m-s)
}

internal fun planetShadow(body: Body, planetRadiusKm: Double, time: Time): ShadowInfo {
    // Calculate light-travel-corrected vector from Earth to planet.
    val g = geoVector(body, time, Aberration.Corrected)

    // Calculate light-travel-corrected vector from Earth to Sun.
    val e = geoVector(Body.Sun, time, Aberration.Corrected)

    // -g  = planetocentric Earth
    // g-e = heliocentric planet
    return calcShadow(planetRadiusKm, time, -g, g-e)
}

internal fun shadowSemiDurationMinutes(centerTime: Time, radiusLimit: Double, windowMinutes: Double): Double {
    // Search backwards and forwards from the center time until
    // the shadow axis distance crosses the specified radius limit.
    val windowDays = windowMinutes / MINUTES_PER_DAY
    val before = centerTime.addDays(-windowDays)
    val after  = centerTime.addDays(+windowDays)
    val t1 = searchEarthShadow(radiusLimit, -1.0, before, centerTime)
    val t2 = searchEarthShadow(radiusLimit, +1.0, centerTime, after)
    // Convert days to minutes and average the semi-durations.
    return (t2.ut - t1.ut) * (MINUTES_PER_DAY / 2.0)
}

internal fun searchEarthShadow(radiusLimit: Double, direction: Double, t1: Time, t2: Time): Time {
    return search(t1, t2, 1.0) { time ->
        direction * (earthShadow(time).r - radiusLimit)
    } ?: throw InternalError("Failed to find Earth shadow transition.")
}

// We can get away with creating a single Earth shadow slope context
// because it contains no state and it has no side-effects.
// This reduces memory allocation overhead and is thread-safe.
internal val earthShadowSlopeContext = SearchContext { time ->
    val dt = 1.0 / SECONDS_PER_DAY
    val t1 = time.addDays(-dt)
    val t2 = time.addDays(+dt)
    val shadow1 = earthShadow(t1)
    val shadow2 = earthShadow(t2)
    (shadow2.r - shadow1.r) / dt
}

internal fun peakEarthShadow(searchCenterTime: Time): ShadowInfo {
    val window = 0.03       // initial search window, in days, before/after searchCenterTime
    val t1 = searchCenterTime.addDays(-window)
    val t2 = searchCenterTime.addDays(+window)
    val tx = search(t1, t2, 1.0, earthShadowSlopeContext) ?:
        throw InternalError("Failed to find Earth peak shadow event.")
    return earthShadow(tx)
}

internal val moonShadowSlopeContext = SearchContext { time ->
    val dt = 1.0 / SECONDS_PER_DAY
    val t1 = time.addDays(-dt)
    val t2 = time.addDays(+dt)
    val shadow1 = moonShadow(t1)
    val shadow2 = moonShadow(t2)
    (shadow2.r - shadow1.r) / dt
}

internal fun peakMoonShadow(searchCenterTime: Time): ShadowInfo {
    // Search for when the Moon's shadow axis is closest to the center of the Earth.
    val window = 0.03       // days before/after new moon to search for minimum shadow distance
    val t1 = searchCenterTime.addDays(-window)
    val t2 = searchCenterTime.addDays(+window)
    val tx = search(t1, t2, 1.0, moonShadowSlopeContext) ?:
        throw InternalError("Failed to find Moon peak shadow event.")
    return moonShadow(tx)
}

internal fun peakLocalMoonShadow(searchCenterTime: Time, observer: Observer): ShadowInfo {
    // Search for the time near searchCenterTime that the Moon's shadow axis
    // comes closest to the given observer.
    val window = 0.2
    val time1 = searchCenterTime.addDays(-window)
    val time2 = searchCenterTime.addDays(+window)
    val time = search(time1, time2, 1.0) { time ->
        val dt = 1.0 / SECONDS_PER_DAY
        val t1 = time.addDays(-dt)
        val t2 = time.addDays(+dt)
        val shadow1 = localMoonShadow(t1, observer)
        val shadow2 = localMoonShadow(t2, observer)
        (shadow2.r - shadow1.r) / dt
    } ?: throw InternalError("Failed to find local Moon peak shadow event.")
    return localMoonShadow(time, observer)
}

internal fun peakPlanetShadow(body: Body, planetRadiusKm: Double, searchCenterTime: Time): ShadowInfo {
    // Search for when the body's shadow axis is closest to the center of the Earth.
    val window = 1.0        // days before/after conjunction to search for minimum shadow distance
    val t1 = searchCenterTime.addDays(-window)
    val t2 = searchCenterTime.addDays(+window)
    val time = search(t1, t2, 1.0) { time ->
        val dt = 1.0 / SECONDS_PER_DAY
        val shadow1 = planetShadow(body, planetRadiusKm, time.addDays(-dt))
        val shadow2 = planetShadow(body, planetRadiusKm, time.addDays(+dt))
        (shadow2.r - shadow1.r) / dt
    } ?: throw InternalError("Failed to find peak planet shadow event.")
    return planetShadow(body, planetRadiusKm, time)
}

internal fun planetTransitBoundary(body: Body, planetRadiusKm: Double, t1: Time, t2: Time, direction: Double): Time {
    // Search for the time when the planet's penumbra begins/ends making contact with the center of the Earth.
    return search(t1, t2, 1.0) { time ->
        val shadow = planetShadow(body, planetRadiusKm, time)
        direction * (shadow.r - shadow.p)
    } ?: throw InternalError("Planet transit boundary search failed.")
}

internal fun discObscuration(a: Double, b: Double, c: Double): Double {
    // a = radius of first disc
    // b = radius of second disc
    // c = distance between centers of discs
    if (a <= 0.0) throw InternalError("Radius of first disc must be positive.")
    if (b <= 0.0) throw InternalError("Radius of second disc must be positive.")
    if (c < 0.0) throw InternalError("Distance between discs is not allowed to be negative.")

    if (c >= a + b) {
        // The discs are too far apart to have any overlapping area.
        return 0.0
    }

    if (c == 0.0) {
        // The discs have a common center. Therefore, one disc is inside the other.
        return if (a <= b) 1.0 else (b*b)/(a*a)
    }

    val x = (a*a - b*b + c*c) / (2*c)
    val radicand = a*a - x*x
    if (radicand <= 0.0) {
        // The circumferences do not intersect, or are tangent.
        // We already ruled out the case of non-overlapping discs.
        // Therefore, one disc is inside the other.
        return if (a <= b) 1.0 else (b*b)/(a*a)
    }

    // The discs overlap fractionally in a pair of lens-shaped areas.

    val y = sqrt(radicand)

    // Return the overlapping fractional area.
    // There are two lens-shaped areas, one to the left of x, the other to the right of x.
    // Each part is calculated by subtracting a triangular area from a sector's area.
    val lens1 = a*a*acos(x/a) - x*y
    val lens2 = b*b*acos((c-x)/b) - (c-x)*y

    // Find the fractional area with respect to the first disc.
    return (lens1 + lens2) / (PI*a*a)
}


internal fun solarEclipseObscuration(hm: Vector, lo: Vector): Double {
    // Find heliocentric observer.
    val ho = hm + lo

    // Calculate the apparent angular radius of the Sun for the observer.
    val sunRadius = asin(SUN_RADIUS_AU / ho.length())

    // Calculate the apparent angular radius of the Moon for the observer.
    val moonRadius = asin(MOON_POLAR_RADIUS_AU / lo.length())

    // Calculate the apparent angular separation between the Sun's center and the Moon's center.
    val sunMoonSeparation = lo.angleWith(ho).degreesToRadians()

    // Find the fraction of the Sun's apparent disc area that is covered by the Moon.
    val obscuration = discObscuration(sunRadius, moonRadius, sunMoonSeparation)

    // HACK: In marginal cases, we need to clamp obscuration to less than 1.0.
    // This function is never called for total eclipses, so it should never return 1.0.
    return min(0.9999, obscuration)
}


/**
 * Searches for a lunar eclipse.
 *
 * This function finds the first lunar eclipse that occurs after `startTime`.
 * A lunar eclipse may be penumbral, partial, or total.
 * See [LunarEclipseInfo] for more information.
 * To find a series of lunar eclipses, call this function once,
 * then keep calling [nextLunarEclipse] as many times as desired,
 * passing in the `center` value returned from the previous call.
 *
 * See [lunarEclipsesAfter] for convenient iteration of consecutive lunar eclipses.
 *
 * @param startTime
 * The date and time for starting the search for a lunar eclipse.
 *
 * @return Information about the first lunar eclipse that occurs after `startTime`.
 */
fun searchLunarEclipse(startTime: Time): LunarEclipseInfo {
    val pruneLatitude = 1.8   // full Moon's ecliptic latitude above which eclipse is impossible

    // Iterate through consecutive full moons until we find any kind of lunar eclipse.
    var fmtime = startTime
    for (fmcount in 0..11) {
        val fullmoon = searchMoonPhase(180.0, fmtime, 40.0) ?:
            throw InternalError("Failed to find the next full moon.")

        // Pruning: if the full Moon's ecliptic latitude is too large,
        // a lunar eclipse is not possible. Avoid needless work searching for
        // the minimum moon distance.
        val moon = MoonContext(fullmoon).calcMoon()
        if (moon.lat < pruneLatitude) {
            // Search near the full moon for the time when the center of the Moon
            // is closest to the line passing through the centers of the Sun and Earth.
            val shadow = peakEarthShadow(fullmoon)
            if (shadow.r < shadow.p + MOON_MEAN_RADIUS_KM) {
                // This is at least a penumbral eclipse. We will return a result.
                var kind = EclipseKind.Penumbral
                var obscuration = 0.0
                val sdPenum = shadowSemiDurationMinutes(shadow.time, shadow.p + MOON_MEAN_RADIUS_KM, 200.0)
                var sdPartial = 0.0
                var sdTotal = 0.0

                if (shadow.r < shadow.k + MOON_MEAN_RADIUS_KM) {
                    // This is at least a partial eclipse.
                    kind = EclipseKind.Partial
                    sdPartial = shadowSemiDurationMinutes(shadow.time, shadow.k + MOON_MEAN_RADIUS_KM, sdPenum)

                    if (shadow.r + MOON_MEAN_RADIUS_KM < shadow.k) {
                        // This is a total eclipse.
                        kind = EclipseKind.Total
                        obscuration = 1.0
                        sdTotal = shadowSemiDurationMinutes(shadow.time, shadow.k - MOON_MEAN_RADIUS_KM, sdPartial)
                    } else {
                        obscuration = discObscuration(MOON_MEAN_RADIUS_KM, shadow.k, shadow.r)
                    }
                }

                return LunarEclipseInfo(kind, obscuration, shadow.time, sdPenum, sdPartial, sdTotal)
            }
        }

        fmtime = fullmoon.addDays(10.0)
    }

    // This should never happen, because there should be at least 2 lunar eclipses per year.
    throw InternalError("Failed to find a lunar eclipse within 12 full moons.")
}


/**
 * Searches for the next lunar eclipse in a series.
 *
 * After using [searchLunarEclipse] to find the first lunar eclipse
 * in a series, you can call this function to find the next consecutive lunar eclipse.
 * Pass in the `center` value from the [LunarEclipseInfo] returned by the
 * previous call to `searchLunarEclipse` or `nextLunarEclipse`
 * to find the next lunar eclipse.
 *
 * See [lunarEclipsesAfter] for convenient iteration of consecutive lunar eclipses.
 *
 * @param prevEclipseTime
 * A time near a full moon. Lunar eclipse search will start at the next full moon.
 *
 * @return
 * Information about the next lunar eclipse in a series.
 */
fun nextLunarEclipse(prevEclipseTime: Time) =
    searchLunarEclipse(prevEclipseTime.addDays(10.0))


/**
 * Enumerates a series of consecutive lunar eclipses that occur after a given time.
 *
 * This function enables iteration through an unlimited number
 * of consecutive lunar eclipses starting at a given time.
 *
 * This is a convenience wrapper around [searchLunarEclipse] and [nextLunarEclipse].
 *
 * @param startTime
 * The date and time for starting the search for a series of lunar eclipses.
 */
fun lunarEclipsesAfter(startTime: Time): Sequence<LunarEclipseInfo> =
    generateSequence(searchLunarEclipse(startTime)) { nextLunarEclipse(it.peak) }


internal fun moonEclipticLatitudeDegrees(time: Time) = MoonContext(time).calcMoon().lat

// Convert all distances from AU to km.
// But dilate the z-coordinates so that the Earth becomes a perfect sphere.
// Then find the intersection of the vector with the sphere.
// See p 184 in Montenbruck & Pfleger's "Astronomy on the Personal Computer", second edition.
internal fun kmSpherical(v: Vector) =
    Vector(
        v.x * KM_PER_AU,
        v.y * KM_PER_AU,
        v.z * (KM_PER_AU / EARTH_FLATTENING),
        v.t
    )

internal fun eclipseKindFromUmbra(k: Double) = (
    // The umbra radius tells us what kind of eclipse the observer sees.
    // If the umbra radius is positive, this is a total eclipse. Otherwise, it's annular.
    // HACK: I added a tiny bias (14 meters) to match Espenak test data.
    if (k > 0.014)
        EclipseKind.Total
    else
        EclipseKind.Annular
)

internal fun geoidIntersect(shadow: ShadowInfo): GlobalSolarEclipseInfo {
    var kind = EclipseKind.Partial
    var obscuration = Double.NaN
    var latitude = Double.NaN
    var longitude = Double.NaN

    // We want to calculate the intersection of the shadow axis with the Earth's geoid.
    // First we must convert EQJ (equator of J2000) coordinates to EQD (equator of date)
    // coordinates that are perfectly aligned with the Earth's equator at this
    // moment in time.
    val rot = rotationEqjEqd(shadow.time)
    val v = kmSpherical(rot.rotate(shadow.dir))      // shadow axis vector passing through Sun and Moon
    val e = kmSpherical(rot.rotate(shadow.target))   // lunacentric Earth

    // Solve the quadratic equation that finds whether and where
    // the shadow axis intersects with the Earth in the dilated coordinate system.
    val R = EARTH_EQUATORIAL_RADIUS_KM
    val A = v dot v
    val B = -2.0 * (v dot e)
    val C = (e dot e) - R*R
    val radic = B*B - 4.0*A*C
    if (radic > 0.0) {
        // Calculate the closer of the two intersection points.
        // This will be on the sunlit side of the Earth.
        val u = (-B - sqrt(radic)) / (2.0 * A)

        // Convert lunacentric dilated coordinates to geocentric coordinates.
        val px = u*v.x - e.x
        val py = u*v.y - e.y
        val pz = (u*v.z - e.z) * EARTH_FLATTENING

        // Convert cartesian coordinates into geodetic latitude/longitude.
        val proj = hypot(px, py) * EARTH_FLATTENING_SQUARED
        latitude = if (proj == 0.0) (
            if (pz > 0.0) +90.0 else -90.0
        ) else (
            datan(pz / proj)
        )

        // Adjust longitude for Earth's rotation at the given time.
        val gast = siderealTime(shadow.time)
        longitude = ((datan2(py,px) - (15.0 * gast)) % 360.0).withMaxDegreeValue(180.0)

        // We want to determine whether the observer sees a total eclipse or an annular eclipse.
        // We need to perform a series of vector calculations.
        // Calculate the inverse rotation matrix, so we can convert EQD to EQJ.
        val inv = rot.inverse()

        // Get the EQD geocentric coordinates of the observer.
        val obs = Vector(px / KM_PER_AU, py / KM_PER_AU, pz / KM_PER_AU, shadow.time)

        // Rotate the observer's geocentric EQD back to the EQJ system,
        // and convert the geocentric vector to a lunacentric vector.
        val luna = inv.rotate(obs) + shadow.target

        // Calculate the shadow using a vector from the Moon's center toward the observer.
        val surface = calcShadow(MOON_POLAR_RADIUS_KM, shadow.time, luna, shadow.dir)

        // If we did everything right, the shadow distance should be very close to zero.
        // That's because we already determined the observer is on the shadow axis!
        if (surface.r > 1.0e-9 || surface.r < 0.0)
            throw InternalError("Invalid surface distance from intersection.")

        kind = eclipseKindFromUmbra(surface.k)
        obscuration = if (kind == EclipseKind.Total) 1.0 else solarEclipseObscuration(shadow.dir, luna)
    }

    return GlobalSolarEclipseInfo(kind, obscuration, shadow.time, shadow.r, latitude, longitude)
}


/**
 * Searches for a solar eclipse visible anywhere on the Earth's surface.
 *
 * This function finds the first solar eclipse that occurs after `startTime`.
 * A solar eclipse may be partial, annular, or total.
 * See [GlobalSolarEclipseInfo] for more information.
 * To find a series of solar eclipses, call this function once,
 * then keep calling [nextGlobalSolarEclipse] as many times as desired,
 * passing in the `peak` value returned from the previous call.
 *
 * See [globalSolarEclipsesAfter] for convenient iteration of consecutive eclipses.
 *
 * @param startTime
 * The date and time for starting the search for a solar eclipse.
 *
 * @return Information about the first solar eclipse after `startTime`.
 */
fun searchGlobalSolarEclipse(startTime: Time): GlobalSolarEclipseInfo {
    val pruneLatitude = 1.8     // Moon's ecliptic latitude beyond which eclipse is impossible
    var nmtime = startTime
    for (nmcount in 0..11) {
        // Search for the next new moon. Any solar eclipse will be near it.
        val newmoon = searchMoonPhase(0.0, nmtime, 40.0) ?:
            throw InternalError("Failed to find next new moon.")

        // Pruning: if the new moon's ecliptic latitude is too large, a solar eclipse is not possible.
        val eclipLat = moonEclipticLatitudeDegrees(newmoon)
        if (abs(eclipLat) < pruneLatitude) {
            // Search near the new moon for the time when the center of the Earth
            // is closest to the line passing through the centers of the Sun and Moon.
            val shadow = peakMoonShadow(newmoon)
            if (shadow.r < shadow.p + EARTH_MEAN_RADIUS_KM) {
                // This is at least a partial solar eclipse visible somewhere on Earth.
                // Try to find an intersection between the shadow axis and the Earth's oblate geoid.
                return geoidIntersect(shadow)
            }
        }

        nmtime = newmoon.addDays(10.0)
    }
    // This should never happen, because at least 2 solar eclipses happen every year.
    throw InternalError("Failure to find global solar eclipse.")
}


/**
 * Searches for the next global solar eclipse in a series.
 *
 * After using [searchGlobalSolarEclipse] to find the first solar eclipse
 * in a series, you can call this function to find the next consecutive solar eclipse.
 * Pass in the `peak` value from the [GlobalSolarEclipseInfo] returned by the
 * previous call to `searchGlobalSolarEclipse` or `nextGlobalSolarEclipse`
 * to find the next solar eclipse.
 *
 * See [globalSolarEclipsesAfter] for convenient iteration of consecutive eclipses.
 *
 * @param prevEclipseTime
 * A date and time near a new moon. Solar eclipse search will start at the next new moon.
 *
 * @return Information about the next consecutive solar eclipse.
 */
fun nextGlobalSolarEclipse(prevEclipseTime: Time) =
    searchGlobalSolarEclipse(prevEclipseTime.addDays(10.0))


/**
 * Enumerates a series of consecutive global solar eclipses that occur after a given time.
 *
 * This function enables iteration through an unlimited number
 * of consecutive global solar eclipses starting at a given time.
 * This is a convenience wrapper around [searchGlobalSolarEclipse] and [nextGlobalSolarEclipse].
 *
 * @param startTime
 * The date and time for starting the search for a series of global solar eclipses.
 */
fun globalSolarEclipsesAfter(startTime: Time): Sequence<GlobalSolarEclipseInfo> =
    generateSequence(searchGlobalSolarEclipse(startTime)) { nextGlobalSolarEclipse(it.peak) }


/**
 * Searches for a solar eclipse visible at a specific location on the Earth's surface.
 *
 * This function finds the first solar eclipse that occurs after `startTime`.
 * A solar eclipse may be partial, annular, or total.
 * See [LocalSolarEclipseInfo] for more information.
 *
 * To find a series of solar eclipses, call this function once,
 * then keep calling [nextLocalSolarEclipse] as many times as desired,
 * passing in the `peak` value returned from the previous call.
 *
 * IMPORTANT: An eclipse reported by this function might be partly or
 * completely invisible to the observer due to the time of day.
 * See [LocalSolarEclipseInfo] for more information about this topic.
 *
 * See [localSolarEclipsesAfter] for convenient iteration of consecutive eclipses.
 *
 * @param startTime
 * The date and time for starting the search for a solar eclipse.
 *
 * @param observer
 * The geographic location of the observer.
 *
 * @return Information about the first solar eclipse visible at the specified observer location.
 */
fun searchLocalSolarEclipse(startTime: Time, observer: Observer): LocalSolarEclipseInfo {
    val pruneLatitude = 1.8     // Moon's ecliptic latitude beyond which eclipise is impossible.

    // Iterate through consecutive new moons until we find a solar eclipse visible somewhere on Earth.
    var nmtime = startTime
    while (true) {
        // Search for the next new moon. Any eclipse will be near it.
        val newmoon = searchMoonPhase(0.0, nmtime, 40.0) ?:
            throw InternalError("Failed to find next new moon")

        // Pruning: if the new moon's ecliptic latitude is too large, a solar eclipse is not possible.
        val eclipLat = moonEclipticLatitudeDegrees(newmoon)
        if (abs(eclipLat) < pruneLatitude) {
            // Search near the new moon for the time when the observer
            // is closest to the line passing through the centers of the Sun and Moon.
            val shadow = peakLocalMoonShadow(newmoon, observer)
            if (shadow.r < shadow.p) {
                // This is at least a partial solar eclipse for the observer.
                val eclipse = localEclipse(shadow, observer)

                // Ignore any eclipse that happens completely at night.
                // More precisely, the center of the the Sun must be above the horizon
                // at the beginning or the end of the eclipse, or we skip the event.
                if (eclipse.partialBegin.altitude > 0.0 || eclipse.partialEnd.altitude > 0.0)
                    return eclipse
            }
        }

        // We didn't find an eclipse on this new moon, so search for the next one.
        nmtime = newmoon.addDays(10.0)
    }
}


internal fun localEclipse(shadow: ShadowInfo, observer: Observer): LocalSolarEclipseInfo {
    val PARTIAL_WINDOW = 0.2
    val TOTAL_WINDOW = 0.01
    val peak = calcEvent(observer, shadow.time)
    val t1p = shadow.time.addDays(-PARTIAL_WINDOW)
    val t2p = shadow.time.addDays(+PARTIAL_WINDOW)
    val partialBegin = localEclipseTransition(observer, +1.0, t1p, shadow.time) { it.p - it.r }
    val partialEnd   = localEclipseTransition(observer, -1.0, shadow.time, t2p) { it.p - it.r }
    var totalBegin: EclipseEvent? = null
    var totalEnd: EclipseEvent? = null
    val kind: EclipseKind
    if (shadow.r < abs(shadow.k)) {     // take absolute value of `k` to handle annular eclipses too.
        val t1t = shadow.time.addDays(-TOTAL_WINDOW)
        val t2t = shadow.time.addDays(+TOTAL_WINDOW)
        totalBegin = localEclipseTransition(observer, +1.0, t1t, shadow.time) { abs(it.k) - it.r }
        totalEnd   = localEclipseTransition(observer, -1.0, shadow.time, t2t) { abs(it.k) - it.r }
        kind = eclipseKindFromUmbra(shadow.k)
    } else {
        kind = EclipseKind.Partial
    }
    val obscuration = if (kind == EclipseKind.Total) 1.0 else solarEclipseObscuration(shadow.dir, shadow.target)
    return LocalSolarEclipseInfo(kind, obscuration, partialBegin, totalBegin, peak, totalEnd, partialEnd)
}


internal fun localEclipseTransition(
    observer: Observer,
    direction: Double,
    t1: Time,
    t2: Time,
    func: (ShadowInfo) -> Double
): EclipseEvent {
    val time = search(t1, t2, 1.0) { time ->
        direction * func(localMoonShadow(time, observer))
    } ?: throw InternalError("Local eclipse transition search failed in range [$t1, $t2].")
    return calcEvent(observer, time)
}


internal fun calcEvent(observer: Observer, time: Time): EclipseEvent {
    val sunEqu = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
    val sunHor = horizon(time, observer, sunEqu.ra, sunEqu.dec, Refraction.Normal)
    return EclipseEvent(time, sunHor.altitude)
}


/**
 * Searches for the next local solar eclipse in a series.
 *
 * After using [searchLocalSolarEclipse] to find the first solar eclipse
 * in a series, you can call this function to find the next consecutive solar eclipse.
 * Pass in the `peak` value from the [LocalSolarEclipseInfo] returned by the
 * previous call to `searchLocalSolarEclipse` or `nextLocalSolarEclipse`
 * to find the next solar eclipse.
 *
 * See [localSolarEclipsesAfter] for convenient iteration of consecutive eclipses.
 *
 * @param prevEclipseTime
 * A date and time near a new moon. Solar eclipse search will start at the next new moon.
 *
 * @param observer
 * The geographic location of the observer.
 *
 * @return Information about the next solar eclipse visible at the specified observer location.
 */
fun nextLocalSolarEclipse(prevEclipseTime: Time, observer: Observer) =
    searchLocalSolarEclipse(prevEclipseTime.addDays(10.0), observer)


/**
 * Enumerates a series of consecutive local solar eclipses that occur after a given time.
 *
 * This function enables iteration through an unlimited number
 * of consecutive local solar eclipses starting at a given time.
 * This is a convenience wrapper around [searchLocalSolarEclipse] and [nextLocalSolarEclipse].
 *
 * @param startTime
 * The date and time for starting the search for a series of local solar eclipses.
 *
 * @param observer
 * The geographic location of the observer.
 */
fun localSolarEclipsesAfter(startTime: Time, observer: Observer): Sequence<LocalSolarEclipseInfo> =
    generateSequence(searchLocalSolarEclipse(startTime, observer)) { nextLocalSolarEclipse(it.peak.time, observer) }


/**
 * Information about the brightness and illuminated shape of a celestial body.
 *
 * Returned by the functions [illumination] and [searchPeakMagnitude]
 * to report the visual magnitude and illuminated fraction of a celestial body at a given date and time.
 */
class IlluminationInfo(
    /**
     * The date and time of the observation.
     */
    val time: Time,

    /**
     * The visual magnitude of the body. Smaller values are brighter.
     */
    val mag: Double,

    /**
     * The angle in degrees between the Sun and the Earth, as seen from the body.
     * Indicates the body's phase as seen from the Earth.
     */
    val phaseAngle: Double,

    /**
     * A value in the range [0.0, 1.0] indicating what fraction of the body's
     * apparent disc is illuminated, as seen from the Earth.
     */
    val phaseFraction: Double,

    /**
     * The distance between the Sun and the body at the observation time.
     */
    val helioDist: Double,

    /**
     * For Saturn, the tilt angle in degrees of its rings as seen from Earth.
     * For all other bodies, 0.0.
     */
    val ringTilt: Double
)


/**
 * Information about a body's rotation axis at a given time.
 *
 * This structure is returned by [rotationAxis] to report
 * the orientation of a body's rotation axis at a given moment in time.
 * The axis is specified by the direction in space that the body's north pole
 * points, using angular equatorial coordinates in the J2000 system (EQJ).
 *
 * Thus `ra` is the right ascension, and `dec` is the declination, of the
 * body's north pole vector at the given moment in time. The north pole
 * of a body is defined as the pole that lies on the north side of the
 * [Solar System's invariable plane](https://en.wikipedia.org/wiki/Invariable_plane),
 * regardless of the body's direction of rotation.
 *
 * The `spin` field indicates the angular position of a prime meridian
 * arbitrarily recommended for the body by the International Astronomical
 * Union (IAU).
 *
 * The fields `ra`, `dec`, and `spin` correspond to the variables
 * α0, δ0, and W, respectively, from
 * [Report of the IAU Working Group on Cartographic Coordinates and Rotational Elements: 2015](https://astropedia.astrogeology.usgs.gov/download/Docs/WGCCRE/WGCCRE2015reprint.pdf).
 *
 * The field `north` is a unit vector pointing in the direction of the body's north pole.
 * It is expressed in the J2000 mean equator system (EQJ).
 */
class AxisInfo(
    /**
     * The J2000 right ascension of the body's north pole direction, in sidereal hours.
     */
    val ra: Double,

    /**
     * The J2000 declination of the body's north pole direction, in degrees.
     */
    val dec: Double,

    /**
     * Rotation angle of the body's prime meridian, in degrees.
     */
    val spin: Double,

    /**
     * A J2000 dimensionless unit vector pointing in the direction of the body's north pole.
     */
    val north: Vector
)


/**
 * Indicates whether a crossing through the ecliptic plane is ascending or descending.
 */
enum class NodeEventKind {
    /**
     * The body passes through the ecliptic plane from south to north.
     */
    Ascending,

    /**
     * The body passes through the ecliptic plane from north to south.
     */
    Descending,
}


/**
 * Information about an ascending or descending node of a body.
 *
 * This object is returned by [searchMoonNode] and [nextMoonNode]
 * to report information about the center of the Moon passing through the ecliptic plane.
 */
class NodeEventInfo(
    /**
     * The time of the body's node.
     */
    val time: Time,

    /**
     * Whether the node is ascending or descending.
     */
    val kind: NodeEventKind
)


/**
 * Represents a function whose ascending root is to be found.
 *
 * This interface must be implemented for callers of [search]
 * in order to find the ascending root of a smooth function.
 * A class that implements `SearchContext` can hold state information
 * needed to evaluate the scalar function `eval`.
 */
fun interface SearchContext {
    /**
     * Evaluates a scalar function at a given time.
     *
     * @param time
     * The time at which to evaluate the function.
     *
     * @return The floating point value of the scalar function at the given time.
     */
    fun eval(time: Time): Double
}

//---------------------------------------------------------------------------------------

/**
 * Reports the constellation that a given celestial point lies within.
 *
 * The [constellation] function returns this object
 * to report which constellation corresponds with a given point in the sky.
 * Constellations are defined with respect to the B1875 equatorial system
 * per IAU standard. Although `constellation` requires J2000 equatorial
 * coordinates, `ConstellationInfo` contains converted B1875 coordinates for reference.
 */
class ConstellationInfo(
    /**
     * 3-character mnemonic symbol for the constellation, e.g. "Ori".
     */
    val symbol: String,

    /**
     * Full name of constellation, e.g. "Orion".
     */
    val name: String,

    /**
     * Right ascension expressed in B1875 coordinates.
     */
    val ra1875: Double,

    /**
     * Declination expressed in B1875 coordinates.
     */
    val dec1875: Double
)

internal class ConstellationText(
    val symbol: String,
    val name: String
)

internal class ConstellationBoundary(
    val index: Int,
    val raLo: Double,
    val raHi: Double,
    val decLo: Double
)

//---------------------------------------------------------------------------------------
// VSOP87: semi-analytic model of major planet positions

internal class VsopTerm(
    val amplitude: Double,
    val phase: Double,
    val frequency: Double
)

internal class VsopSeries(
    val term: Array<VsopTerm>
)

internal class VsopFormula(
    val series: Array<VsopSeries>
)

internal class VsopModel(
    val lon: VsopFormula,
    val lat: VsopFormula,
    val rad: VsopFormula
)

private fun vsopFormulaCalc(formula: VsopFormula, t: Double, clampAngle: Boolean): Double {
    var coord = 0.0
    var tpower = 1.0
    for (series in formula.series) {
        var sum = 0.0
        for (term in series.term)
            sum += term.amplitude * cos(term.phase + (t * term.frequency))
        coord +=
            if (clampAngle)
                (tpower * sum) % PI2    // improve precision: longitude angles can be hundreds of radians
            else
                tpower * sum
        tpower *= t
    }
    return coord
}

private fun vsopDistance(model: VsopModel, time: Time) =
    vsopFormulaCalc(model.rad, time.julianMillennia(), false)

private fun vsopRotate(eclip: TerseVector) =
    TerseVector(
        eclip.x + 0.000000440360*eclip.y - 0.000000190919*eclip.z,
        -0.000000479966*eclip.x + 0.917482137087*eclip.y - 0.397776982902*eclip.z,
        0.397776982902*eclip.y + 0.917482137087*eclip.z
    )

private fun vsopSphereToRect(lon: Double, lat: Double, radius: Double): TerseVector {
    val rCosLat = radius * cos(lat)
    return TerseVector (
        rCosLat * cos(lon),
        rCosLat * sin(lon),
        radius * sin(lat)
    )
}

private fun calcVsop(model: VsopModel, time: Time): Vector {
    val t = time.julianMillennia()

    // Calculate the VSOP "B" trigonometric series to obtain ecliptic spherical coordinates.
    val lon = vsopFormulaCalc(model.lon, t, true)
    val lat = vsopFormulaCalc(model.lat, t, false)
    val rad = vsopFormulaCalc(model.rad, t, false)

    // Convert ecliptic spherical coordinates to ecliptic Cartesian coordinates.
    val eclip = vsopSphereToRect(lon, lat, rad)

    // Convert ecliptic Cartesian coordinates to equatorial Cartesian coordinates.
    // Also convert from TerseVector (coordinates only) to Vector (coordinates + time).
    return vsopRotate(eclip).toAstroVector(time)
}

private fun vsopDerivCalc(formula: VsopFormula, t: Double): Double {
    var tpower = 1.0        // t^s
    var dpower = 0.0        // t^(s-1)
    var deriv = 0.0
    formula.series.forEachIndexed { s, series ->
        var sinSum = 0.0
        var cosSum = 0.0
        for (term in series.term) {
            val angle = term.phase + (term.frequency * t)
            sinSum += term.amplitude * (term.frequency * sin(angle))
            if (s > 0)
                cosSum += term.amplitude * cos(angle)
        }
        deriv += (s * dpower * cosSum) - (tpower * sinSum)
        dpower = tpower
        tpower *= t
    }
    return deriv
}

internal class BodyState(
    var tt: Double,
    val r: TerseVector,
    val v: TerseVector
) {
    fun increment(other: BodyState) {
        r.increment(other.r)
        v.increment(other.v)
    }

    fun decrement(other: BodyState) {
        r.decrement(other.r)
        v.decrement(other.v)
    }

    fun copyFrom(other: BodyState) {
        tt = other.tt
        r.copyFrom(other.r)
        v.copyFrom(other.v)
    }

    operator fun minus(other: BodyState) =
        BodyState(tt, r - other.r, v - other.v)
}


private fun exportState(bodyState: BodyState, time: Time) =
    StateVector(
        bodyState.r.x,  bodyState.r.y,  bodyState.r.z,
        bodyState.v.x,  bodyState.v.y,  bodyState.v.z,
        time
    )


private fun exportGravCalc(calc: BodyGravCalc, time: Time) =
    StateVector(
        calc.r.x, calc.r.y, calc.r.z,
        calc.v.x, calc.v.y, calc.v.z,
        time
    )


private fun calcVsopPosVel(model: VsopModel, tt: Double): BodyState {
    val t = tt / DAYS_PER_MILLENNIUM

    // Calculate the VSOP "B" trigonometric series to obtain ecliptic spherical coordinates.
    val lon = vsopFormulaCalc(model.lon, t, true)
    val lat = vsopFormulaCalc(model.lat, t, false)
    val rad = vsopFormulaCalc(model.rad, t, false)

    val eclipPos = vsopSphereToRect(lon, lat, rad)

    // Calculate derivatives of spherical coordinates with respect to time.
    val dlon = vsopDerivCalc(model.lon, t)      // dlon = d(lon) / dt
    val dlat = vsopDerivCalc(model.lat, t)      // dlat = d(lat) / dt
    val drad = vsopDerivCalc(model.rad, t)      // drad = d(rad) / dt

    // Use spherical coords and spherical derivatives to calculate
    // the velocity vector in rectangular coordinates.

    val coslon = cos(lon)
    val sinlon = sin(lon)
    val coslat = cos(lat)
    val sinlat = sin(lat)

    val vx = (
        + (drad * coslat * coslon)
        - (rad * sinlat * coslon * dlat)
        - (rad * coslat * sinlon * dlon)
    )

    val vy = (
        + (drad * coslat * sinlon)
        - (rad * sinlat * sinlon * dlat)
        + (rad * coslat * coslon * dlon)
    )

    val vz = (
        + (drad * sinlat)
        + (rad * coslat * dlat)
    )

    // Convert speed units from [AU/millennium] to [AU/day].
    val eclipVel = TerseVector(
        vx / DAYS_PER_MILLENNIUM,
        vy / DAYS_PER_MILLENNIUM,
        vz / DAYS_PER_MILLENNIUM
    )

    // Rotate the vectors from ecliptic to equatorial coordinates.
    val equPos = vsopRotate(eclipPos)
    val equVel = vsopRotate(eclipVel)
    return BodyState(tt, equPos, equVel)
}

private fun optionalVsopModel(body: Body): VsopModel? = when (body) {
    Body.Mercury -> vsopModelMercury
    Body.Venus   -> vsopModelVenus
    Body.Earth   -> vsopModelEarth
    Body.Mars    -> vsopModelMars
    Body.Jupiter -> vsopModelJupiter
    Body.Saturn  -> vsopModelSaturn
    Body.Uranus  -> vsopModelUranus
    Body.Neptune -> vsopModelNeptune
    else         -> null
}

private fun vsopModel(body: Body): VsopModel =
    optionalVsopModel(body) ?: throw InvalidBodyException(body)

private fun vsopHelioVector(body: Body, time: Time) =
    calcVsop(vsopModel(body), time)

//---------------------------------------------------------------------------------------
// Geocentric Moon

/**
 * Emulate two-dimensional arrays from the Pascal programming language.
 *
 * The original Pascal source code used 2D arrays
 * with negative lower bounds on the indices. This is trivial
 * in Pascal, but most other languages use 0-based arrays.
 * This wrapper class adapts the original algorithm to Kotlin's
 * zero-based arrays.
 */
private class PascalArray2(
    val xmin: Int,
    xmax: Int,
    val ymin: Int,
    ymax: Int
) {
    private val array = Array<DoubleArray>((xmax- xmin) + 1) { DoubleArray((ymax - ymin) + 1) }
    operator fun get(x: Int, y: Int) = array[x - xmin][y - ymin]
    operator fun set(x: Int, y: Int, v: Double) { array[x - xmin][y - ymin] = v }
}

private class MoonContext(time: Time) {
    // Variable names inside this class do not follow coding style on purpose.
    // They reflect the exact names from the original Pascal source code,
    // for ease of porting and consistent code generation.
    private var T: Double
    private var DGAM = 0.0
    private var DLAM: Double
    private var N = 0.0
    private var GAM1C: Double
    private var SINPI: Double
    private var L0: Double
    private var L: Double
    private var LS: Double
    private var F: Double
    private var D: Double
    private var DL0 = 0.0
    private var DL = 0.0
    private var DLS = 0.0
    private var DF = 0.0
    private var DD = 0.0
    private var DS = 0.0
    private val CO = PascalArray2(-6, 6, 1, 4)
    private val SI = PascalArray2(-6, 6, 1, 4)
    private val ARC = 3600.0 * RAD2DEG      // arcseconds per radian

    init {
        T = time.julianCenturies()
        val T2 = T*T
        DLAM = 0.0
        DS = 0.0
        GAM1C = 0.0
        SINPI = 3422.7
        longPeriodic()
        L0 = PI2 * frac(0.60643382 + (1336.85522467 * T) - (0.00000313 * T2)) + (DL0 / ARC)
        L  = PI2 * frac(0.37489701 + (1325.55240982 * T) + (0.00002565 * T2)) + (DL  / ARC)
        LS = PI2 * frac(0.99312619 + (  99.99735956 * T) - (0.00000044 * T2)) + (DLS / ARC)
        F  = PI2 * frac(0.25909118 + (1342.22782980 * T) - (0.00000892 * T2)) + (DF  / ARC)
        D  = PI2 * frac(0.82736186 + (1236.85308708 * T) - (0.00000397 * T2)) + (DD  / ARC)
        for (I in 1..4) {
            var ARG: Double
            var MAX: Int
            var FAC: Double
            when (I) {
                1    -> { ARG=L;  MAX=4; FAC=1.000002208               }
                2    -> { ARG=LS; MAX=3; FAC=0.997504612-0.002495388*T }
                3    -> { ARG=F;  MAX=4; FAC=1.000002708+139.978*DGAM  }
                else -> { ARG=D;  MAX=6; FAC=1.0;                      }
            }
            CO[0,I] = 1.0
            CO[1,I] = cos(ARG) * FAC
            SI[0,I] = 0.0
            SI[1,I] = sin(ARG) * FAC

            for (J in 2..MAX) {
                val c1 = CO[J-1,I]
                val s1 = SI[J-1,I]
                val c2 = CO[1,I]
                val s2 = SI[1,I]
                CO[J,I] = c1*c2 - s1*s2
                SI[J,I] = s1*c2 + c1*s2
            }

            for (J in 1..MAX) {
                CO[-J,I] =  CO[J,I]
                SI[-J,I] = -SI[J,I]
            }
        }
    }

    /**
     * Sine of an angle `phi` expressed in revolutions, not radians or degrees.
     */
    private fun sine(phi: Double) = sin(PI2 * phi)

    private fun frac(x: Double) = x - floor(x)

    private fun longPeriodic() {
        val S1 = sine(0.19833 + (0.05611 * T))
        val S2 = sine(0.27869 + (0.04508 * T))
        val S3 = sine(0.16827 - (0.36903 * T))
        val S4 = sine(0.34734 - (5.37261 * T))
        val S5 = sine(0.10498 - (5.37899 * T))
        val S6 = sine(0.42681 - (0.41855 * T))
        val S7 = sine(0.14943 - (5.37511 * T))

        DL0 = ( 0.84 * S1) + (0.31 * S2) + (14.27 * S3) + (7.26 * S4) + (0.28 * S5) + (0.24 * S6)
        DL  = ( 2.94 * S1) + (0.31 * S2) + (14.27 * S3) + (9.34 * S4) + (1.12 * S5) + (0.83 * S6)
        DLS = (-6.40 * S1) - (1.89 * S6)
        DF  = ( 0.21 * S1) + (0.31 * S2) + (14.27 * S3) - (88.70*S4) - (15.30 * S5) + (0.24 * S6) - (1.86 * S7)
        DD  = DL0 - DLS
        DGAM = (
            -3332.0e-9 * sine(0.59734 - (5.37261 * T))
             -539.0e-9 * sine(0.35498 - (5.37899 * T))
              -64.0e-9 * sine(0.39943 - (5.37511 * T))
        )
    }

    // This is an ugly hack for efficiency.
    // Kotlin does not allow passing variables by reference.
    // Because this function is called *millions* of times
    // during the unit test, we can't afford to be allocating
    // tiny arrays or classes to pass a tuple (xTerm, yTerm) by reference.
    // It is much more efficient to keep (xTerm, yTerm) at object scope,
    // and allow term() to overwrite their values.
    // Callers know that each time they call term(), they must
    // access the output through (xTerm, yTerm) before calling term() again.

    var xTerm = Double.NaN
    var yTerm = Double.NaN
    private fun term(p: Int, q: Int, r: Int, s: Int) {
        xTerm = 1.0
        yTerm = 0.0
        if (p != 0) addTheta(CO[p, 1], SI[p, 1])
        if (q != 0) addTheta(CO[q, 2], SI[q, 2])
        if (r != 0) addTheta(CO[r, 3], SI[r, 3])
        if (s != 0) addTheta(CO[s, 4], SI[s, 4])
    }

    private fun addTheta(c2: Double, s2: Double) {
        val c1 = xTerm
        val s1 = yTerm
        xTerm = c1*c2 - s1*s2
        yTerm = s1*c2 + c1*s2
    }

    fun addSol(
        coeffl: Double,
        coeffs: Double,
        coeffg: Double,
        coeffp: Double,
        p: Int,
        q: Int,
        r: Int,
        s: Int
    ) {
        term(p, q, r, s)
        DLAM  += coeffl * yTerm
        DS    += coeffs * yTerm
        GAM1C += coeffg * xTerm
        SINPI += coeffp * xTerm
    }

    fun addn(coeffn: Double, p: Int, q: Int, r: Int, s: Int) {
        term(p, q, r, s)
        N += (coeffn * yTerm)
    }

    private fun solarN() {
        N = 0.0
        addn(-526.069,  0, 0, 1, -2)
        addn(  -3.352,  0, 0, 1, -4)
        addn( +44.297, +1, 0, 1, -2)
        addn(  -6.000, +1, 0, 1, -4)
        addn( +20.599, -1, 0, 1,  0)
        addn( -30.598, -1, 0, 1, -2)
        addn( -24.649, -2, 0, 1,  0)
        addn(  -2.000, -2, 0, 1, -2)
        addn( -22.571,  0,+1, 1, -2)
        addn( +10.985,  0,-1, 1, -2)
    }

    private fun planetary() {
        DLAM += (
            +0.82*sine(0.7736   -62.5512*T) + 0.31*sine(0.0466  -125.1025*T)
            +0.35*sine(0.5785   -25.1042*T) + 0.66*sine(0.4591 +1335.8075*T)
            +0.64*sine(0.3130   -91.5680*T) + 1.14*sine(0.1480 +1331.2898*T)
            +0.21*sine(0.5918 +1056.5859*T) + 0.44*sine(0.5784 +1322.8595*T)
            +0.24*sine(0.2275    -5.7374*T) + 0.28*sine(0.2965    +2.6929*T)
            +0.33*sine(0.3132    +6.3368*T)
        )
    }

    fun calcMoon(): Spherical {
        addSolarTerms(this)
        solarN()
        planetary()
        val S = F + DS/ARC
        val latSeconds = (1.000002708 + 139.978*DGAM)*(18518.511+1.189+GAM1C)*sin(S)-6.24*sin(3*S) + N
        return Spherical(
            latSeconds / 3600.0,
            360.0 * frac((L0+DLAM/ARC) / PI2),
            (ARC * EARTH_EQUATORIAL_RADIUS_AU) / (0.999953253 * SINPI)
        )
    }
}

//---------------------------------------------------------------------------------------
// Pluto/SSB gravitation simulator

private class BodyGravCalc(
    var tt: Double,         // J2000 terrestrial time [days]
    var r: TerseVector,     // position [au]
    var v: TerseVector,     // velocity [au/day]
    var a: TerseVector      // acceleration [au/day]
) {
    fun copyFrom(other: BodyGravCalc) {
        tt = other.tt
        r.copyFrom(other.r)
        v.copyFrom(other.v)
        a.copyFrom(other.a)
    }
}

private class MajorBodies(
    val sun:        BodyState,
    val jupiter:    BodyState,
    val saturn:     BodyState,
    val uranus:     BodyState,
    val neptune:    BodyState
) {
    fun acceleration(smallPos: TerseVector): TerseVector = (
        accelerationIncrement(smallPos, SUN_GM,     sun.r    ) +
        accelerationIncrement(smallPos, JUPITER_GM, jupiter.r) +
        accelerationIncrement(smallPos, SATURN_GM,  saturn.r ) +
        accelerationIncrement(smallPos, URANUS_GM,  uranus.r ) +
        accelerationIncrement(smallPos, NEPTUNE_GM, neptune.r)
    )

    private fun accelerationIncrement(smallPos: TerseVector, gm: Double, majorPos: TerseVector): TerseVector {
        val delta = majorPos - smallPos
        val r2 = delta.quadrature()
        return (gm / (r2 * sqrt(r2))) * delta
    }
}

private fun updatePosition(dt: Double, r: TerseVector, v: TerseVector, a: TerseVector) =
    TerseVector(
        r.x + dt*(v.x + dt*a.x/2.0),
        r.y + dt*(v.y + dt*a.y/2.0),
        r.z + dt*(v.z + dt*a.z/2.0),
    )

private fun updateVelocity(dt: Double, v: TerseVector, a: TerseVector) =
    TerseVector(
        v.x + dt*a.x,
        v.y + dt*a.y,
        v.z + dt*a.z
    )

private fun adjustBarycenterPosVel(ssb: BodyState, tt: Double, body: Body, planetGm: Double): BodyState {
    val shift = planetGm / (planetGm + SUN_GM)
    val planet = calcVsopPosVel(vsopModel(body), tt)
    ssb.r.increment(shift * planet.r)
    ssb.v.increment(shift * planet.v)
    return planet
}

private fun majorBodyBary(tt: Double): MajorBodies {
    // Calculate the state vector of the Solar System Barycenter (SSB).
    val ssb = BodyState(tt, TerseVector.zero(), TerseVector.zero())
    val jupiter = adjustBarycenterPosVel(ssb, tt, Body.Jupiter, JUPITER_GM)
    val saturn  = adjustBarycenterPosVel(ssb, tt, Body.Saturn,  SATURN_GM )
    val uranus  = adjustBarycenterPosVel(ssb, tt, Body.Uranus,  URANUS_GM )
    val neptune = adjustBarycenterPosVel(ssb, tt, Body.Neptune, NEPTUNE_GM)

    // Convert planet state vectors from heliocentric to barycentric.
    jupiter.decrement(ssb)
    saturn.decrement(ssb)
    uranus.decrement(ssb)
    neptune.decrement(ssb)

    // Convert the heliocentric SSB to a barycentric Sun state.
    val sun = BodyState(tt, -ssb.r, -ssb.v)

    return MajorBodies(sun, jupiter, saturn, uranus, neptune)
}

private class GravSim(
    val bary: MajorBodies,
    val grav: BodyGravCalc
)

private fun simulateGravity(
    tt2: Double,            // a target time to be calculated (before or after current time tt1)
    calc1: BodyGravCalc     // [pos, vel, acc] of the simulated body at tt1
): GravSim {
    val dt = tt2 - calc1.tt

    // Calculate where the major bodies (Sun, Jupiter...Neptune) will be at tt2.
    val bary = majorBodyBary(tt2)

    // Estimate the position of the small body as if the current
    // acceleration applies across the whole time interval.
    val approxPos = updatePosition(dt, calc1.r, calc1.v, calc1.a)

    // Calculate the average acceleration of the endpoints.
    // This becomes our estimate of the mean effective acceleration
    // over the whole time interval.
    val meanAcc = bary.acceleration(approxPos).mean(calc1.a)

    // Refine the estimates of [pos, vel, acc] at tt2 using the mean acceleration.
    val pos = updatePosition(dt, calc1.r, calc1.v, meanAcc)
    val vel = updateVelocity(dt, calc1.v, meanAcc)
    val acc = bary.acceleration(pos)

    val grav = BodyGravCalc(tt2, pos, vel, acc)
    return GravSim(bary, grav)
}


private fun clampIndex(frac: Double, nsteps: Int): Int {
    val index = frac.toInt()
    return when {
        index < 0 -> 0
        index >= nsteps -> nsteps - 1
        else -> index
    }
}

private fun gravFromState(state: BodyState): GravSim {
    val bary = majorBodyBary(state.tt)
    val r = state.r + bary.sun.r
    val v = state.v + bary.sun.v
    val a = bary.acceleration(r)
    val grav = BodyGravCalc(state.tt, r, v, a)
    return GravSim(bary, grav)
}

private fun getPlutoSegment(tt: Double): List<BodyGravCalc>? {
    if (tt < plutoStateTable[0].tt || tt > plutoStateTable[PLUTO_NUM_STATES-1].tt)
        return null     // Don't bother calculating a segment. Let the caller crawl backward/forward to this time

    val segIndex = clampIndex((tt - plutoStateTable[0].tt) / PLUTO_TIME_STEP, PLUTO_NUM_STATES-1)
    return synchronized(plutoCacheLock) {
        plutoCache.getOrPut(segIndex) {
            val seg = mutableListOf<BodyGravCalc>()

            // The first endpoint is exact.
            var sim = gravFromState(plutoStateTable[segIndex])
            seg.add(sim.grav)

            // Simulate forwards from the lower time bound.
            var steptt = sim.grav.tt
            for (i in 1 until PLUTO_NSTEPS-1) {
                steptt += PLUTO_DT
                sim = simulateGravity(steptt, sim.grav)
                seg.add(sim.grav)
            }

            // The last endpoint is exact.
            sim = gravFromState(plutoStateTable[segIndex + 1])
            seg.add(sim.grav)

            // Simulate backwards from the upper time bound.
            val reverse = buildList {
                add(sim.grav)

                steptt = sim.grav.tt
                for (i in (PLUTO_NSTEPS-2) downTo 1) {
                    steptt -= PLUTO_DT
                    sim = simulateGravity(steptt, sim.grav)
                    add(sim.grav)
                }

                add(seg[0])
            }.reversed()

            // Fade-mix the two series so that there are no discontinuities.
            for (i in (PLUTO_NSTEPS-2) downTo 1) {
                val ramp = i.toDouble() / (PLUTO_NSTEPS - 1)
                seg[i].r.mix(ramp, reverse[i].r)
                seg[i].v.mix(ramp, reverse[i].v)
                seg[i].a.mix(ramp, reverse[i].a)
            }

            seg
        }
    }
}

private fun calcPlutoOneWay(
    initState: BodyState,
    targetTt: Double,
    dt: Double
) : GravSim {
    var sim = gravFromState(initState)
    val n: Int = ceil((targetTt - sim.grav.tt) / dt).toInt()
    for (i in 0 until n) {
        val tt = if (i+1 == n) targetTt else (sim.grav.tt + dt)
        sim = simulateGravity(tt, sim.grav)
    }
    return sim
}

private fun calcPluto(time: Time, helio: Boolean): StateVector {
    val seg = getPlutoSegment(time.tt)
    val calc: BodyGravCalc
    var bary: MajorBodies? = null
    if (seg == null) {
        // The target time is outside the year range 0000..4000.
        // Calculate it by crawling backward from 0000 or forward from 4000.
        // FIXFIXFIX - This is super slow. Could optimize this with extra caching if needed.
        val sim =
            if (time.tt < plutoStateTable[0].tt)
                calcPlutoOneWay(plutoStateTable[0], time.tt, (-PLUTO_DT).toDouble())
            else
                calcPlutoOneWay(plutoStateTable[PLUTO_NUM_STATES-1], time.tt, (+PLUTO_DT).toDouble())

        calc = sim.grav
        bary = sim.bary
    } else {
        val left = clampIndex((time.tt - seg[0].tt) / PLUTO_DT, PLUTO_NSTEPS-1)
        val s1 = seg[left]
        val s2 = seg[left+1]

        // Find mean acceleration vector over the interval.
        val acc: TerseVector = s1.a.mean(s2.a)

        // Use Newtonian mechanics to extrapolate away from t1 in the positive time direction.
        val ra: TerseVector = updatePosition(time.tt - s1.tt, s1.r, s1.v, acc)
        val va: TerseVector = updateVelocity(time.tt - s1.tt, s1.v, acc)

        // Use Newtonian mechanics to extrapolate away from t2 in the negative time direction.
        val rb: TerseVector = updatePosition(time.tt - s2.tt, s2.r, s2.v, acc)
        val vb: TerseVector = updateVelocity(time.tt - s2.tt, s2.v, acc)

        // Use fade in/out idea to blend the two position estimates.
        val ramp = (time.tt - s1.tt)/PLUTO_DT
        calc = BodyGravCalc(
            time.tt,
            (1.0 - ramp)*ra + ramp*rb,      // ramp/mix the position vector
            (1.0 - ramp)*va + ramp*vb,      // ramp/mix the velocity vector
            TerseVector.zero()              // the acceleration isn't used
        )
    }

    if (helio) {
        // Lazy evaluate the Solar System Barycenter state if needed.
        if (bary == null)
            bary = majorBodyBary(time.tt)

        // Convert barycentric vectors to heliocentric vectors.
        calc.r.decrement(bary.sun.r)
        calc.v.decrement(bary.sun.v)
    }

    return StateVector(
        calc.r.x, calc.r.y, calc.r.z,
        calc.v.x, calc.v.y, calc.v.z,
        time
    )
}

//---------------------------------------------------------------------------------------

internal class JupiterMoon (
    val mu: Double,
    val al0: Double,
    val al1: Double,
    val a: Array<VsopTerm>,
    val l: Array<VsopTerm>,
    val z: Array<VsopTerm>,
    val zeta: Array<VsopTerm>
)


private fun jupiterMoonElemToPv(
    time: Time,
    mu: Double,
    A: Double,
    AL: Double,
    K: Double,
    H: Double,
    Q: Double,
    P: Double
): StateVector {
    // Translation of FORTRAN subroutine ELEM2PV from:
    // https://ftp.imcce.fr/pub/ephem/satel/galilean/L1/L1.2/

    val AN = sqrt(mu / (A*A*A))

    var CE: Double
    var SE: Double
    var DE: Double
    var EE = AL + K*sin(AL) - H*cos(AL)
    do {
        CE = cos(EE)
        SE = sin(EE)
        DE = (AL - EE + K*SE - H*CE) / (1.0 - K*CE - H*SE)
        EE += DE
    } while (DE.absoluteValue >= 1.0e-12)

    CE = cos(EE)
    SE = sin(EE)
    val DLE = H*CE - K*SE
    val RSAM1 = -K*CE - H*SE
    val ASR = 1.0/(1.0 + RSAM1)
    val PHI = sqrt(1.0 - K*K - H*H)
    val PSI = 1.0/(1.0 + PHI)
    val X1 = A*(CE - K - PSI*H*DLE)
    val Y1 = A*(SE - H + PSI*K*DLE)
    val VX1 = AN*ASR*A*(-SE - PSI*H*RSAM1)
    val VY1 = AN*ASR*A*(+CE + PSI*K*RSAM1)
    val F2 = 2.0*sqrt(1.0 - Q*Q - P*P)
    val P2 = 1.0 - 2.0*P*P
    val Q2 = 1.0 - 2.0*Q*Q
    val PQ = 2.0*P*Q

    return StateVector(
        X1*P2 + Y1*PQ,
        X1*PQ + Y1*Q2,
        (Q*Y1 - X1*P)*F2,
        VX1*P2 + VY1*PQ,
        VX1*PQ + VY1*Q2,
        (Q*VY1 - VX1*P)*F2,
        time
    )
}


private fun calcJupiterMoon(time: Time, m: JupiterMoon): StateVector {
    // This is a translation of FORTRAN code by Duriez, Lainey, and Vienne:
    // https://ftp.imcce.fr/pub/ephem/satel/galilean/L1/L1.2/

    val t = time.tt + 18262.5     // number of days since 1950-01-01T00:00:00Z

    // Calculate 6 orbital elements at the given time t.
    var elem0 = 0.0
    for (term in m.a)
        elem0 += term.amplitude * cos(term.phase + (t * term.frequency))

    var elem1 = m.al0 + (t * m.al1)
    for (term in m.l)
        elem1 += term.amplitude * sin(term.phase + (t * term.frequency))

    elem1 %= PI2
    if (elem1 < 0)
        elem1 += PI2

    var elem2 = 0.0
    var elem3 = 0.0
    for (term in m.z) {
        val arg = term.phase + (t * term.frequency)
        elem2 += term.amplitude * cos(arg)
        elem3 += term.amplitude * sin(arg)
    }

    var elem4 = 0.0
    var elem5 = 0.0
    for (term in m.zeta) {
        val arg = term.phase + (t * term.frequency)
        elem4 += term.amplitude * cos(arg)
        elem5 += term.amplitude * sin(arg)
    }

    // Convert the oribital elements into position vectors in the Jupiter equatorial system (JUP).
    val state = jupiterMoonElemToPv(time, m.mu, elem0, elem1, elem2, elem3, elem4, elem5)

    // Re-orient position and velocity vectors from Jupiter-equatorial (JUP) to Earth-equatorial in J2000 (EQJ).
    return rotationJupEqj.rotate(state)
}


//---------------------------------------------------------------------------------------


internal fun terrestrialTime(ut: Double): Double = ut + deltaT(ut) / SECONDS_PER_DAY

private val epoch2000 = Time(0.0)

internal fun deltaT(ut: Double): Double {
    /*
        Fred Espenak writes about Delta-T generically here:
        https://eclipse.gsfc.nasa.gov/SEhelp/deltaT.html
        https://eclipse.gsfc.nasa.gov/SEhelp/deltat2004.html

        He provides polynomial approximations for distant years here:
        https://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html

        They start with a year value 'y' such that y=2000 corresponds
        to the UTC Date 15-January-2000. Convert difference in days
        to mean tropical years.
    */
    val u: Double
    val u2: Double
    val u3: Double
    val u4: Double
    val u5: Double
    val u6: Double
    val u7: Double
    val y = 2000 + (ut - 14) / DAYS_PER_TROPICAL_YEAR
    return when {
        y < -500 -> {
            u = (y - 1820) / 100
            -20.0 + (32.0 * u * u)
        }
        y < 500.0 -> {
            u = y / 100
            u2 = u * u; u3 = u * u2; u4 = u2 * u2; u5 = u2 * u3; u6 = u3 * u3
            10583.6 - (1014.41 * u) + (33.78311 * u2) - (5.952053 * u3) - (0.1798452 * u4) + (0.022174192 * u5) + (0.0090316521 * u6)
        }
        y < 1600.0 -> {
            u = (y - 1000) / 100
            u2 = u * u; u3 = u * u2; u4 = u2 * u2; u5 = u2 * u3; u6 = u3 * u3
            1574.2 - (556.01 * u) + (71.23472 * u2) + (0.319781 * u3) - (0.8503463 * u4) - (0.005050998 * u5) + (0.0083572073 * u6)
        }
        y < 1700.0 -> {
            u = y - 1600
            u2 = u * u; u3 = u * u2
            120.0 - (0.9808 * u) - (0.01532 * u2) + (u3 / 7129)
        }
        y < 1800.0 -> {
            u = y - 1700
            u2 = u * u; u3 = u * u2; u4 = u2 * u2
            8.83 + (0.1603 * u) - (0.0059285 * u2) + (0.00013336 * u3) - (u4 / 1174000)
        }
        y < 1860.0 -> {
            u = y - 1800
            u2 = u * u; u3 = u * u2; u4 = u2 * u2; u5 = u2 * u3; u6 = u3 * u3; u7 = u3 * u4
            13.72 - (0.332447 * u) + (0.0068612 * u2) + (0.0041116 * u3) - (0.00037436 * u4) + (0.0000121272 * u5) - (0.0000001699 * u6) + (0.000000000875 * u7)
        }
        y < 1900.0 -> {
            u = y - 1860
            u2 = u * u; u3 = u * u2; u4 = u2 * u2; u5 = u2 * u3
            7.62 + (0.5737 * u) - (0.251754 * u2) + (0.01680668 * u3) - (0.0004473624 * u4) + (u5 / 233174)
        }
        y < 1920.0 -> {
            u = y - 1900
            u2 = u * u; u3 = u * u2; u4 = u2 * u2
            -2.79 + (1.494119 * u) - (0.0598939 * u2) + (0.0061966 * u3) - (0.000197 * u4)
        }
        y < 1941.0 -> {
            u = y - 1920
            u2 = u * u; u3 = u * u2
            21.20 + (0.84493 * u) - (0.076100 * u2) + (0.0020936 * u3)
        }
        y < 1961 -> {
            u = y - 1950
            u2 = u * u; u3 = u * u2
            29.07 + (0.407 * u) - (u2 / 233) + (u3 / 2547)
        }
        y < 1986.0 -> {
            u = y - 1975
            u2 = u * u; u3 = u * u2
            45.45 + (1.067 * u) - (u2 / 260) - (u3 / 718)
        }
        y < 2005 -> {
            u = y - 2000
            u2 = u * u; u3 = u * u2; u4 = u2 * u2; u5 = u2 * u3
            63.86 + (0.3345 * u) - (0.060374 * u2) + (0.0017275 * u3) + (0.000651814 * u4) + (0.00002373599 * u5)
        }
        y < 2050 -> {
            u = y - 2000
            62.92 + (0.32217 * u) + (0.005589 * u * u)
        }
        y < 2150 -> {
            u = (y - 1820) / 100
            -20 + (32 * u * u) - (0.5628 * (2150 - y))
        }
        else -> {   // all years after 2150
            u = (y - 1820) / 100
            -20 + (32 * u * u)
        }
    }
}

internal fun universalTime(tt: Double): Double {
    // This is the inverse function of terrestrialTime.
    // This is an iterative numerical solver, but because
    // the relationship between UT and TT is almost perfectly linear,
    // it converges extremely fast (never more than 3 iterations).

    // dt = tt - ut
    var dt = terrestrialTime(tt) - tt
    while (true) {
        val ut = tt - dt
        val ttCheck = terrestrialTime(ut)
        val err = ttCheck - tt
        if (err.absoluteValue < 1.0e-12) return ut
        dt += err
    }
}

/**
 * Calculates the amount of "lift" to an altitude angle caused by atmospheric refraction.
 *
 * Given an altitude angle and a refraction option, calculates
 * the amount of "lift" caused by atmospheric refraction.
 * This is the number of degrees higher in the sky an object appears
 * due to the lensing of the Earth's atmosphere.
 * This function works best near sea level.
 * To correct for higher elevations, call [atmosphere] for that
 * elevation and multiply the refraction angle by the resulting relative density.
 *
 * @param refraction
 * The option selecting which refraction correction to use.
 * If `Refraction.Normal`, uses a well-behaved refraction model that works well for
 * all valid values (-90 to +90) of `altitude`.
 * If `Refraction.JplHor`, this function returns a compatible value with the JPL Horizons tool.
 * If any other value, including `Refraction.None`, this function returns 0.
 *
 * @param altitude
 * An altitude angle in a horizontal coordinate system. Must be a value between -90 and +90.
 */
fun refractionAngle(refraction: Refraction, altitude: Double): Double {
    if (altitude < -90.0 || altitude > +90.0)
        return 0.0     // no attempt to correct an invalid altitude

    var angle: Double
    if (refraction == Refraction.Normal || refraction == Refraction.JplHor) {
        // http://extras.springer.com/1999/978-1-4471-0555-8/chap4/horizons/horizons.pdf
        // JPL Horizons says it uses refraction algorithm from
        // Meeus "Astronomical Algorithms", 1991, p. 101-102.
        // I found the following Go implementation:
        // https://github.com/soniakeys/meeus/blob/master/v3/refraction/refract.go
        // This is a translation from the function "Saemundsson" there.
        // I found experimentally that JPL Horizons clamps the angle to 1 degree below the horizon.
        // This is important because the tangent formula below goes crazy near hd = -5.11.
        val hd = altitude.coerceAtLeast(-1.0)
        angle = (1.02 / dtan(hd + 10.3/(hd + 5.11))) / 60.0

        if (refraction == Refraction.Normal && altitude < -1.0) {
            // In "normal" mode we gradually reduce refraction toward the nadir
            // so that we never get an altitude angle less than -90 degrees.
            // When horizon angle is -1 degrees, the factor is exactly 1.
            // As altitude approaches -90 (the nadir), the fraction approaches 0 linearly.
            angle *= (altitude + 90.0) / 89.0
        }
    } else {
        // No refraction, or the refraction option is invalid.
        angle = 0.0
    }
    return angle
}

/**
 * Calculates the inverse of an atmospheric refraction angle.
 *
 * Given an observed altitude angle that includes atmospheric refraction,
 * calculates the negative angular correction to obtain the unrefracted
 * altitude. This is useful for cases where observed horizontal
 * coordinates are to be converted to another orientation system,
 * but refraction first must be removed from the observed position.
 *
 * @param refraction
 * The option selecting which refraction correction to use.
 *
 * @param bentAltitude
 * The apparent altitude that includes atmospheric refraction.
 *
 * @return
 * The angular adjustment in degrees to be added to the
 * altitude angle to remove atmospheric lensing.
 * This will be less than or equal to zero.
 */
fun inverseRefractionAngle(refraction: Refraction, bentAltitude: Double): Double {
    if (bentAltitude < -90.0 || bentAltitude > +90.0)
        return 0.0     // no attempt to correct an invalid altitude

    // Find the pre-adjusted altitude whose refraction correction leads to 'altitude'.
    var altitude = bentAltitude - refractionAngle(refraction, bentAltitude)
    while (true) {
        // See how close we got.
        val diff = (altitude + refractionAngle(refraction, altitude)) - bentAltitude
        if (diff.absoluteValue < 1.0e-14)
            return altitude - bentAltitude
        altitude -= diff
    }
}

/**
 * Returns the product of mass and universal gravitational constant of a Solar System body.
 *
 * For problems involving the gravitational interactions of Solar System bodies,
 * it is helpful to know the product GM, where G = the universal gravitational constant
 * and M = the mass of the body. In practice, GM is known to a higher precision than
 * either G or M alone, and thus using the product results in the most accurate results.
 * This function returns the product GM in the units au^3/day^2.
 * The values come from page 10 of a
 * [JPL memorandum regarding the DE405/LE405 ephemeris](https://web.archive.org/web/20120220062549/http://iau-comm4.jpl.nasa.gov/de405iom/de405iom.pdf).
 *
 * @param body
 * The body for which to find the GM product.
 * Allowed to be the Sun, Moon, EMB (Earth/Moon Barycenter), or any planet.
 * Any other value will cause an exception to be thrown.
 *
 * @return The mass product of the given body in au^3/day^2.
 */
fun massProduct(body: Body): Double =
    body.massProduct ?: throw InvalidBodyException(body)

private enum class PrecessDirection {
    From2000,
    Into2000,
}

private fun precessionRot(time: Time, dir: PrecessDirection): RotationMatrix {
    val t = time.julianCenturies()
    val eps0 = 84381.406

    val psia   = (((((-    0.0000000951  * t
                      +    0.000132851 ) * t
                      -    0.00114045  ) * t
                      -    1.0790069   ) * t
                      + 5038.481507    ) * t) * ASEC2RAD

    val omegaa = (((((+    0.0000003337  * t
                      -    0.000000467 ) * t
                      -    0.00772503  ) * t
                      +    0.0512623   ) * t
                      -    0.025754    ) * t + eps0) * ASEC2RAD

    val chia   = (((((-    0.0000000560  * t
                      +    0.000170663 ) * t
                      -    0.00121197  ) * t
                      -    2.3814292   ) * t
                      +   10.556403    ) * t) * ASEC2RAD

    val sa = sin(eps0 * ASEC2RAD)
    val ca = cos(eps0 * ASEC2RAD)
    val sb = sin(-psia)
    val cb = cos(-psia)
    val sc = sin(-omegaa)
    val cc = cos(-omegaa)
    val sd = sin(chia)
    val cd = cos(chia)

    val xx =  cd*cb - sb*sd*cc
    val yx =  cd*sb*ca + sd*cc*cb*ca - sa*sd*sc
    val zx =  cd*sb*sa + sd*cc*cb*sa + ca*sd*sc
    val xy = -sd*cb - sb*cd*cc
    val yy = -sd*sb * ca + cd*cc*cb*ca - sa*cd*sc
    val zy = -sd*sb * sa + cd*cc*cb*sa + ca*cd*sc
    val xz =  sb*sc
    val yz = -sc*cb*ca - sa*cc
    val zz = -sc*cb*sa + cc*ca

    return when (dir) {
        // Perform rotation from other epoch to J2000.0.
        PrecessDirection.Into2000 ->
            RotationMatrix(
                xx, yx, zx,
                xy, yy, zy,
                xz, yz, zz
            )

        // Perform rotation from J2000.0 to other epoch.
        PrecessDirection.From2000 ->
            RotationMatrix(
                xx, xy, xz,
                yx, yy, yz,
                zx, zy, zz
            )
    }
}

private fun precession(pos: Vector, dir: PrecessDirection) =
    precessionRot(pos.t, dir).rotate(pos)

private fun precessionPosVel(state: StateVector, dir: PrecessDirection) =
    precessionRot(state.t, dir).rotate(state)

private class EarthTilt(
    val dpsi: Double,
    val ee: Double,
    val mobl: Double,
    val tobl: Double
)

private fun iau2000b(time: Time) {
    // Truncated version of the NOVAS C 3.1 function of the same name.
    // We cache Earth nutation angles `psi` and `eps` inside Time for efficiency.
    // If nutation has not already been calculated, these values will be NaN.
    // Lazy-evaluate both angles.

    if (time.psi.isNaN()) {
        val t = time.julianCenturies()
        val elp = ((1287104.79305 + t * 129596581.0481)  % ASEC360) * ASEC2RAD
        val f   = ((335779.526232 + t * 1739527262.8478) % ASEC360) * ASEC2RAD
        val d   = ((1072260.70369 + t * 1602961601.2090) % ASEC360) * ASEC2RAD
        val om  = ((450160.398036 - t * 6962890.5431)    % ASEC360) * ASEC2RAD

        var sarg = sin(om)
        var carg = cos(om)
        var dp = (-172064161.0 - 174666.0*t)*sarg + 33386.0*carg
        var de = (92052331.0 + 9086.0*t)*carg + 15377.0*sarg

        var arg = 2.0*(f - d + om)
        sarg = sin(arg)
        carg = cos(arg)
        dp += (-13170906.0 - 1675.0*t)*sarg - 13696.0*carg
        de += (5730336.0 - 3015.0*t)*carg - 4587.0*sarg

        arg = 2.0*(f + om)
        sarg = sin(arg)
        carg = cos(arg)
        dp += (-2276413.0 - 234.0*t)*sarg + 2796.0*carg
        de += (978459.0 - 485.0*t)*carg + 1374.0*sarg

        arg = 2.0*om
        sarg = sin(arg)
        carg = cos(arg)
        dp += (2074554.0 + 207.0*t)*sarg - 698.0*carg
        de += (-897492.0 + 470.0*t)*carg - 291.0*sarg

        sarg = sin(elp)
        carg = cos(elp)
        dp += (1475877.0 - 3633.0*t)*sarg + 11817.0*carg
        de += (73871.0 - 184.0*t)*carg - 1924.0*sarg

        time.psi = -0.000135 + (dp * 1.0e-7)
        time.eps = +0.000388 + (de * 1.0e-7)
    }
}

private fun meanObliquity(time: Time): Double {
    val t = time.julianCenturies()
    val asec =
        ((((  -0.0000000434   * t
            -  0.000000576  ) * t
            +  0.00200340   ) * t
            -  0.0001831    ) * t
            - 46.836769     ) * t + 84381.406
    return asec / 3600
}

private fun earthTilt(time: Time): EarthTilt {
    iau2000b(time)  // lazy-evaluate time.psi and time.eps
    val mobl = meanObliquity(time)
    val tobl = mobl + (time.eps / 3600)
    val ee = time.psi * dcos(mobl) / 15.0
    return EarthTilt(time.psi, ee, mobl, tobl)
}

private fun earthRotationAngle(time: Time): Double {
    val thet1 = 0.7790572732640 + (0.00273781191135448 * time.ut)
    val thet3 = time.ut % 1.0
    val theta = 360.0 *((thet1 + thet3) % 1.0)
    return if (theta < 0.0) theta + 360.0 else theta
}

/**
 * Calculates Greenwich Apparent Sidereal Time (GAST).
 *
 * Given a date and time, this function calculates the rotation of the
 * Earth, represented by the equatorial angle of the Greenwich prime meridian
 * with respect to distant stars (not the Sun, which moves relative to background
 * stars by almost one degree per day).
 * This angle is called Greenwich Apparent Sidereal Time (GAST).
 * GAST is measured in sidereal hours in the half-open range [0, 24).
 * When GAST = 0, it means the prime meridian is aligned with the of-date equinox,
 * corrected at that time for precession and nutation of the Earth's axis.
 * In this context, the *equinox* is the direction in space where the Earth's
 * orbital plane (the ecliptic) intersects with the plane of the Earth's equator,
 * at the location on the Earth's orbit of the (seasonal) March equinox.
 * As the Earth rotates, GAST increases from 0 up to 24 sidereal hours,
 * then starts over at 0.
 * To convert to degrees, multiply the return value by 15.
 *
 * @param time
 * The date and time for which to find GAST.
 * As an optimization, this function caches the sidereal time value in `time`,
 * unless it has already been cached, in which case the cached value is reused.
 */
fun siderealTime(time: Time): Double {
    if (time.st.isNaN()) {
        val t = time.julianCenturies()
        val eqeq = 15.0 * earthTilt(time).ee
        val theta = earthRotationAngle(time)
        val st = (eqeq + 0.014506 +
               (((( -    0.0000000368  * t
                   -    0.000029956  ) * t
                   -    0.00000044   ) * t
                   +    1.3915817    ) * t
                   + 4612.156534     ) * t)
        val gst = ((st/3600.0 + theta) % 360.0) / 15.0
        time.st = if (gst < 0.0) gst + 24.0 else gst
    }
    return time.st
}

/**
 * Calcluate the geocentric position and velocity of a topocentric observer.
 *
 * Given the geographic location of an observer and a time, calculate the position
 * and velocity of the observer, relative to the center of the Earth.
 * The state vector is expressed in the equator-of-date system (EQD).
 *
 * @param observer
 * The latitude, longitude, and elevation of the observer.
 *
 * @param time
 * The time of the observation.
 *
 * @return
 * An EQD state vector that holds the geocentric position and velocity
 * of the observer at the given time.
 */
private fun terra(observer: Observer, time: Time): StateVector {
    val st = siderealTime(time)
    val phi = observer.latitude.degreesToRadians()
    val sinphi = sin(phi)
    val cosphi = cos(phi)
    val c = 1.0 / hypot(cosphi, EARTH_FLATTENING * sinphi)
    val s = c * EARTH_FLATTENING_SQUARED
    val heightKm = observer.height / 1000.0
    val ach = (EARTH_EQUATORIAL_RADIUS_KM * c) + heightKm
    val ash = (EARTH_EQUATORIAL_RADIUS_KM * s) + heightKm
    val stlocl = (15.0*st + observer.longitude).degreesToRadians()
    val sinst = sin(stlocl)
    val cosst = cos(stlocl)

    return StateVector(
        ach * cosphi * cosst / KM_PER_AU,
        ach * cosphi * sinst / KM_PER_AU,
        ash * sinphi / KM_PER_AU,
        -(ANGVEL * 86400.0 / KM_PER_AU) * ach * cosphi * sinst,
        +(ANGVEL * 86400.0 / KM_PER_AU) * ach * cosphi * cosst,
        0.0,
        time
    )
}

/**
 * Calculate the geographic coordinates corresponding to a position vector.
 *
 * Given a geocentric position vector expressed in equator-of-date (EQD) coordinates,
 * this function calculates the latitude, longitude, and elevation of that location.
 * Note that the time `ovec.t` must be set correctly in order to determine the
 * Earth's rotation angle.
 *
 * This function is intended for positions known to be on or near the Earth's surface.
 *
 * @param ovec
 * A geocentric position on or near the Earth's surface, in EQD coordinates.
 *
 * @return
 * The location on or near the Earth's surface corresponding to
 * the given position vector and time.
 */
private fun inverseTerra(ovec: Vector): Observer {
    val lonDeg: Double
    val latDeg: Double
    val heightKm: Double

    // Convert from AU to kilometers.
    val x = ovec.x * KM_PER_AU
    val y = ovec.y * KM_PER_AU
    val z = ovec.z * KM_PER_AU
    val p = hypot(x, y)
    if (p < 1.0e-6) {
        // Special case: within 1 millimeter of a pole!
        // Use arbitrary longitude, and latitude determined by polarity of z.
        lonDeg = 0.0
        latDeg = if (z > 0.0) +90.0 else -90.0
        // Elevation is calculated directly from z.
        heightKm = z.absoluteValue - EARTH_POLAR_RADIUS_KM
    } else {
        // Calculate exact longitude in the half-open range (-180, +180].
        val stlocl = atan2(y, x)
        lonDeg = longitudeOffset(stlocl.radiansToDegrees() - (15 * siderealTime(ovec.t)))
        // Numerically solve for exact latitude, using Newton's Method.
        val F = EARTH_FLATTENING_SQUARED
        // Start with initial latitude estimate, based on a spherical Earth.
        var lat = atan2(z, p)
        var c: Double
        var s: Double
        var denom: Double
        var count = 0
        val distanceAu = max(1.0, ovec.length())
        while (true) {
            ++count
            if (count > 10)
                throw InternalError("inverseTerra solver failed to converge.")
            // Calculate the error function W(lat).
            // We try to find the root of W, meaning where the error is 0.
            c = cos(lat)
            s = sin(lat)
            val factor = (F-1)*EARTH_EQUATORIAL_RADIUS_KM
            val c2 = c*c
            val s2 = s*s
            val radicand = c2 + F*s2
            denom = sqrt(radicand)
            val W = ((factor * s * c) / denom) - (z * c) + (p * s)
            if (W.absoluteValue < distanceAu * 2.0e-8)
                break  // The error is now negligible.
            // Error is still too large. Find the next estimate.
            // Calculate D = the derivative of W with respect to lat.
            val D = (factor * ((c2 - s2) / denom) - (s2 * c2 * (F - 1)/(factor * radicand))) + (z * s) + (p * c)
            lat -= (W / D)
        }
        // We now have a solution for the latitude in radians.
        latDeg = lat.radiansToDegrees()
        // Solve for exact height in kilometers.
        // There are two formulas I can use. Use whichever has the less risky denominator.
        val adjust = EARTH_EQUATORIAL_RADIUS_KM / denom
        heightKm =
            if (s.absoluteValue > c.absoluteValue)
                z/s - F*adjust
            else
                p/c - adjust
    }

    return Observer(latDeg, lonDeg, 1000.0 * heightKm)
}

private fun gyration(pos: Vector, dir: PrecessDirection) =
    when (dir) {
        PrecessDirection.Into2000 -> precession(nutation(pos, dir), dir)
        PrecessDirection.From2000 -> nutation(precession(pos, dir), dir)
    }

private fun gyrationPosVel(state: StateVector, dir: PrecessDirection) =
    when (dir) {
        PrecessDirection.Into2000 -> precessionPosVel(nutationPosVel(state, dir), dir)
        PrecessDirection.From2000 -> nutationPosVel(precessionPosVel(state, dir), dir)
    }

private fun geoPos(time: Time, observer: Observer): Vector =
    gyration(
        terra(observer, time).position(),
        PrecessDirection.Into2000
    )

private fun spin(angle: Double, pos: Vector): Vector {
    val cosang = dcos(angle)
    val sinang = dsin(angle)
    return Vector(
        +cosang*pos.x + sinang*pos.y,
        -sinang*pos.x + cosang*pos.y,
        pos.z,
        pos.t
    )
}

private fun nutationRot(time: Time, dir: PrecessDirection): RotationMatrix {
    val tilt = earthTilt(time)
    val oblm = tilt.mobl.degreesToRadians()
    val oblt = tilt.tobl.degreesToRadians()
    val psi = tilt.dpsi * ASEC2RAD
    val cobm = cos(oblm)
    val sobm = sin(oblm)
    val cobt = cos(oblt)
    val sobt = sin(oblt)
    val cpsi = cos(psi)
    val spsi = sin(psi)

    val xx = cpsi
    val yx = -spsi * cobm
    val zx = -spsi * sobm
    val xy = spsi * cobt
    val yy = cpsi * cobm * cobt + sobm * sobt
    val zy = cpsi * sobm * cobt - cobm * sobt
    val xz = spsi * sobt
    val yz = cpsi * cobm * sobt - sobm * cobt
    val zz = cpsi * sobm * sobt + cobm * cobt

    return when (dir) {
        // Perform rotation from other epoch to J2000.0.
        PrecessDirection.Into2000 ->
            RotationMatrix(
                xx, yx, zx,
                xy, yy, zy,
                xz, yz, zz
            )

        // Perform rotation from J2000.0 to other epoch.
        PrecessDirection.From2000 ->
            RotationMatrix(
                xx, xy, xz,
                yx, yy, yz,
                zx, zy, zz
            )
    }
}

private fun nutation(pos: Vector, dir: PrecessDirection) =
    nutationRot(pos.t, dir).rotate(pos)

private fun nutationPosVel(state: StateVector, dir: PrecessDirection) =
    nutationRot(state.t, dir).rotate(state)

private fun eclipticToEquatorial(oblRadians: Double, ecl: Vector): Vector {
    val cosObl = cos(oblRadians)
    val sinObl = sin(oblRadians)
    return Vector(
        ecl.x,
        (ecl.y * cosObl) - (ecl.z * sinObl),
        (ecl.y * sinObl) + (ecl.z * cosObl),
        ecl.t
    )
}

private fun eclipticToEquatorial(ecl: Vector) =
    eclipticToEquatorial(meanObliquity(ecl.t).degreesToRadians(), ecl)

private fun earthRotationAxis(time: Time): AxisInfo {
    // Unlike the other planets, we have a model of precession and nutation
    // for the Earth's axis that provides a north pole vector.
    // So calculate the vector first, then derive the (RA,DEC) angles from the vector.

    // Start with a north pole vector in equator-of-date coordinates: (0,0,1).
    val pos1 = Vector(0.0, 0.0, 1.0, time)

    // Convert the vector into J2000 coordinates to find the north pole direction.
    val pos2 = nutation(pos1, PrecessDirection.Into2000)
    val north = precession(pos2, PrecessDirection.Into2000)

    // Derive angular values: right ascension and declination.
    val equ = north.toEquatorial()

    // Use a modified version of the era() function that does not trim to 0..360 degrees.
    // This expression is also corrected to give the correct angle at the J2000 epoch.
    val spin = 190.41375788700253 + (360.9856122880876 * time.ut)

    return AxisInfo(equ.ra, equ.dec, spin, north)
}


/**
 * Calculates information about a body's rotation axis at a given time.
 *
 * Calculates the orientation of a body's rotation axis, along with
 * the rotation angle of its prime meridian, at a given moment in time.
 *
 * This function uses formulas standardized by the IAU Working Group
 * on Cartographics and Rotational Elements 2015 report, as described
 * in the following document:
 *
 * https://astropedia.astrogeology.usgs.gov/download/Docs/WGCCRE/WGCCRE2015reprint.pdf
 *
 * See [AxisInfo] for more detailed information.
 *
 * @param body
 * One of the following values:
 * [Body.Sun], [Body.Moon], [Body.Mercury], [Body.Venus], [Body.Earth], [Body.Mars],
 * [Body.Jupiter], [Body.Saturn], [Body.Uranus], [Body.Neptune], [Body.Pluto].
 *
 * @param time
 * The time at which to calculate the body's rotation axis.
 *
 * @return North pole orientation and body spin angle.
 */
fun rotationAxis(body: Body, time: Time): AxisInfo {
    if (body == Body.Earth)
        return earthRotationAxis(time)

    val d = time.tt
    val T = time.julianCenturies()
    val ra: Double
    val dec: Double
    val w: Double
    when (body) {
        Body.Sun -> {
            ra = 286.13
            dec = 63.87
            w = 84.176 + (14.1844 * d)
        }

        Body.Mercury -> {
            ra = 281.0103 - (0.0328 * T)
            dec = 61.4155 - (0.0049 * T)
            w = (
                329.5988
                + (6.1385108 * d)
                + (0.01067257 * dsin((174.7910857 + 4.092335*d)))
                - (0.00112309 * dsin((349.5821714 + 8.184670*d)))
                - (0.00011040 * dsin((164.3732571 + 12.277005*d)))
                - (0.00002539 * dsin((339.1643429 + 16.369340*d)))
                - (0.00000571 * dsin((153.9554286 + 20.461675*d)))
            )
        }

        Body.Venus -> {
            ra = 272.76
            dec = 67.16
            w = 160.20 - (1.4813688 * d)
        }

        Body.Moon -> {
            // See page 8, Table 2 in:
            // https://astropedia.astrogeology.usgs.gov/alfresco/d/d/workspace/SpacesStore/28fd9e81-1964-44d6-a58b-fbbf61e64e15/WGCCRE2009reprint.pdf
            val E1  = 125.045 -  0.0529921*d
            val E2  = 250.089 -  0.1059842*d
            val E3  = 260.008 + 13.0120009*d
            val E4  = 176.625 + 13.3407154*d
            val E5  = 357.529 +  0.9856003*d
            val E6  = 311.589 + 26.4057084*d
            val E7  = 134.963 + 13.0649930*d
            val E8  = 276.617 +  0.3287146*d
            val E9  = 34.226  +  1.7484877*d
            val E10 = 15.134  -  0.1589763*d
            val E11 = 119.743 +  0.0036096*d
            val E12 = 239.961 +  0.1643573*d
            val E13 = 25.053  + 12.9590088*d

            ra = (
                269.9949 + 0.0031*T
                - 3.8787*dsin(E1)
                - 0.1204*dsin(E2)
                + 0.0700*dsin(E3)
                - 0.0172*dsin(E4)
                + 0.0072*dsin(E6)
                - 0.0052*dsin(E10)
                + 0.0043*dsin(E13)
            )

            dec = (
                66.5392 + 0.0130*T
                + 1.5419*dcos(E1)
                + 0.0239*dcos(E2)
                - 0.0278*dcos(E3)
                + 0.0068*dcos(E4)
                - 0.0029*dcos(E6)
                + 0.0009*dcos(E7)
                + 0.0008*dcos(E10)
                - 0.0009*dcos(E13)
            )

            w = (
                38.3213 + (13.17635815 - 1.4e-12*d)*d
                + 3.5610*dsin(E1)
                + 0.1208*dsin(E2)
                - 0.0642*dsin(E3)
                + 0.0158*dsin(E4)
                + 0.0252*dsin(E5)
                - 0.0066*dsin(E6)
                - 0.0047*dsin(E7)
                - 0.0046*dsin(E8)
                + 0.0028*dsin(E9)
                + 0.0052*dsin(E10)
                + 0.0040*dsin(E11)
                + 0.0019*dsin(E12)
                - 0.0044*dsin(E13)
            )
        }

        Body.Mars -> {
            ra = (
                317.269202 - 0.10927547*T
                + 0.000068 * dsin(198.991226 + 19139.4819985*T)
                + 0.000238 * dsin(226.292679 + 38280.8511281*T)
                + 0.000052 * dsin(249.663391 + 57420.7251593*T)
                + 0.000009 * dsin(266.183510 + 76560.6367950*T)
                + 0.419057 * dsin(79.398797 + 0.5042615*T)
            )

            dec = (
                54.432516 - 0.05827105*T
                + 0.000051 * dcos(122.433576 + 19139.9407476*T)
                + 0.000141 * dcos(43.058401 + 38280.8753272*T)
                + 0.000031 * dcos(57.663379 + 57420.7517205*T)
                + 0.000005 * dcos(79.476401 + 76560.6495004*T)
                + 1.591274 * dcos(166.325722 + 0.5042615*T)
            )

            w = (
                176.049863 + 350.891982443297*d
                + 0.000145 * dsin(129.071773 + 19140.0328244*T)
                + 0.000157 * dsin(36.352167 + 38281.0473591*T)
                + 0.000040 * dsin(56.668646 + 57420.9295360*T)
                + 0.000001 * dsin(67.364003 + 76560.2552215*T)
                + 0.000001 * dsin(104.792680 + 95700.4387578*T)
                + 0.584542 * dsin(95.391654 + 0.5042615*T)
            )
        }

        Body.Jupiter -> {
            val Ja = 99.360714  + 4850.4046*T
            val Jb = 175.895369 + 1191.9605*T
            val Jc = 300.323162 + 262.5475*T
            val Jd = 114.012305 + 6070.2476*T
            val Je = 49.511251  + 64.3000*T

            ra = (
                268.056595 - 0.006499*T
                + 0.000117 * dsin(Ja)
                + 0.000938 * dsin(Jb)
                + 0.001432 * dsin(Jc)
                + 0.000030 * dsin(Jd)
                + 0.002150 * dsin(Je)
            )

            dec = (
                64.495303 + 0.002413*T
                + 0.000050 * dcos(Ja)
                + 0.000404 * dcos(Jb)
                + 0.000617 * dcos(Jc)
                - 0.000013 * dcos(Jd)
                + 0.000926 * dcos(Je)
            )

            w = 284.95 + 870.536*d
        }

        Body.Saturn -> {
            ra = 40.589 - 0.036*T
            dec = 83.537 - 0.004*T
            w = 38.90 + 810.7939024*d
        }

        Body.Uranus -> {
            ra = 257.311
            dec = -15.175
            w = 203.81 - 501.1600928*d
        }

        Body.Neptune -> {
            val N = 357.85 + 52.316*T
            val sinN = dsin(N)
            ra = 299.36 + 0.70*sinN
            dec = 43.46 - 0.51*dcos(N)
            w = 249.978 + 541.1397757*d - 0.48*sinN
        }

        Body.Pluto -> {
            ra = 132.993
            dec = -6.163
            w = 302.695 + 56.3625225*d
        }

        else -> throw InvalidBodyException(body)
    }

    // Calculate the north pole vector using the given angles.
    val rcoslat = dcos(dec)
    val north = Vector(
        rcoslat * dcos(ra),
        rcoslat * dsin(ra),
        dsin(dec),
        time
    )

    return AxisInfo(ra / 15.0, dec, w, north)
}

/**
* Calculates spherical ecliptic geocentric position of the Moon.
*
* Given a time of observation, calculates the Moon's geocentric position
* in ecliptic spherical coordinates. Provides the ecliptic latitude and
* longitude in degrees, and the geocentric distance in astronomical units (AU).
*
* The ecliptic angles are measured in "ECT": relative to the true ecliptic plane and
* equatorial plane at the specified time. This means the Earth's equator
* is corrected for precession and nutation, and the plane of the Earth's
* orbit is corrected for gradual obliquity drift.
*
* This algorithm is based on the Nautical Almanac Office's *Improved Lunar Ephemeris* of 1954,
* which in turn derives from E. W. Brown's lunar theories from the early twentieth century.
* It is adapted from Turbo Pascal code from the book
* [Astronomy on the Personal Computer](https://www.springer.com/us/book/9783540672210)
* by Montenbruck and Pfleger.
*
* To calculate a J2000 mean equator vector instead, use [geoMoon].
*
* @param time
* The date and time for which to calculate the Moon's position.
*
* @return The Moon's distance, ecliptic latitude, and ecliptic longitude, expressed in true equinox of date.
*/
fun eclipticGeoMoon(time: Time): Spherical {
    // Find ecliptic coordinates of the Moon in mean equinox of date (ECM).
    val moon = MoonContext(time).calcMoon()

    // Convert spherical coordinates to a vector.
    val latRad = moon.lat.degreesToRadians()
    val lonRad = moon.lon.degreesToRadians()
    val distCosLat = moon.dist * cos(latRad)
    val ecm = Vector(
        distCosLat * cos(lonRad),
        distCosLat * sin(lonRad),
        moon.dist * sin(latRad),
        time
    )

    // Obtain true and mean obliquity angles for the given time.
    // This serves to pre-calculate the nutation also, and cache it in `time`.
    val et = earthTilt(time)

    // Convert ecliptic coordinates to equatorial coordinates, both in mean equinox of date.
    val eqm = eclipticToEquatorial(et.mobl.degreesToRadians(), ecm)

    // Add nutation to convert ECM to true equatorial coordinates of date (EQD).
    val eqd = nutation(eqm, PrecessDirection.From2000)

    // Convert back to ecliptic, this time in true equinox of date (ECT).
    val eclip = rotateEquatorialToEcliptic(eqd, et.tobl.degreesToRadians())

    return Spherical(eclip.elat, eclip.elon, moon.dist)
}

/**
 * Calculates equatorial geocentric position of the Moon at a given time.
 *
 * Given a time of observation, calculates the Moon's position vector.
 * The vector indicates the Moon's center relative to the Earth's center.
 * The vector components are expressed in AU (astronomical units).
 * The coordinates are oriented with respect to the Earth's equator at the J2000 epoch.
 * In Astronomy Engine, this orientation is called EQJ.
 *
 * @param time
 * The date and time for which to calculate the Moon's position.
 *
 * @return The Moon's position vector in J2000 equatorial coordinates (EQJ).
 */
fun geoMoon(time: Time): Vector {
    val eclSphere = MoonContext(time).calcMoon()
    val eclVec = eclSphere.toVector(time)
    val equVec = eclipticToEquatorial(eclVec)
    return precession(equVec, PrecessDirection.Into2000)
}

/**
 * Calculates equatorial geocentric position and velocity of the Moon at a given time.
 *
 * Given a time of observation, calculates the Moon's position and velocity vectors.
 * The position and velocity are of the Moon's center relative to the Earth's center.
 * The position (x, y, z) components are expressed in AU (astronomical units).
 * The velocity (vx, vy, vz) components are expressed in AU/day.
 * The coordinates are oriented with respect to the Earth's equator at the J2000 epoch.
 * In Astronomy Engine, this orientation is called EQJ.
 * If you need the Moon's position only, and not its velocity,
 * it is much more efficient to use [geoMoon] instead.
 *
 * @param time
 * The date and time for which to calculate the Moon's position and velocity.
 *
 * @return The Moon's position and velocity vectors in J2000 equatorial coordinates (EQJ).
 */
fun geoMoonState(time: Time): StateVector {
    // This is a hack, because trying to figure out how to derive
    // a time derivative for MoonContext.calcMoon() would be painful!
    // Calculate just before and just after the given time.
    // Average to find position, subtract to find velocity.
    val dt = 1.0e-5        // 0.864 seconds
    val t1 = time.addDays(-dt)
    val t2 = time.addDays(+dt)
    val r1 = geoMoon(t1)
    val r2 = geoMoon(t2)

    // The desired position is the average of the two calculated positions.
    // The difference of position vectors divided by the time span gives the velocity vector.
    return StateVector(
        (r1.x + r2.x) / 2.0,
        (r1.y + r2.y) / 2.0,
        (r1.z + r2.z) / 2.0,
        (r2.x - r1.x) / (2.0 * dt),
        (r2.y - r1.y) / (2.0 * dt),
        (r2.z - r1.z) / (2.0 * dt),
        time
    )
}

/**
 * Calculates the geocentric position and velocity of the Earth/Moon barycenter.
 *
 * Given a time of observation, calculates the geocentric position and velocity vectors
 * of the Earth/Moon barycenter (EMB).
 * The position (x, y, z) components are expressed in AU (astronomical units).
 * The velocity (vx, vy, vz) components are expressed in AU/day.
 *
 * @param time
 * The date and time for which to calculate the EMB vectors.
 *
 * @return The EMB's position and velocity vectors in geocentric J2000 equatorial coordinates.
 */
fun geoEmbState(time: Time): StateVector =
    geoMoonState(time) / (1.0 + EARTH_MOON_MASS_RATIO)

private fun helioEarthPos(time: Time) =
    calcVsop(vsopModel(Body.Earth), time)

private fun helioEarthState(time: Time) =
    StateVector(calcVsopPosVel(vsopModel(Body.Earth), time.tt), time)

private fun barycenterPosContrib(time: Time, body: Body, planetGm: Double) =
    (planetGm / (planetGm + SUN_GM)) * vsopHelioVector(body, time)

private fun solarSystemBarycenterPos(time: Time): Vector {
    val j = barycenterPosContrib(time, Body.Jupiter, JUPITER_GM)
    val s = barycenterPosContrib(time, Body.Saturn,  SATURN_GM)
    val u = barycenterPosContrib(time, Body.Uranus,  URANUS_GM)
    val n = barycenterPosContrib(time, Body.Neptune, NEPTUNE_GM)
    return Vector(
        j.x + s.x + u.x + n.x,
        j.y + s.y + u.y + n.y,
        j.z + s.z + u.z + n.z,
        time
    )
}

private fun barycenterStateContrib(time: Time, body: Body, planetGm: Double): StateVector {
    val helioPlanet = calcVsopPosVel(vsopModel(body), time.tt)
    val factor = planetGm / (planetGm + SUN_GM)
    return StateVector(
        factor * helioPlanet.r.x,
        factor * helioPlanet.r.y,
        factor * helioPlanet.r.z,
        factor * helioPlanet.v.x,
        factor * helioPlanet.v.y,
        factor * helioPlanet.v.z,
        time
    )
}

private fun solarSystemBarycenterState(time: Time): StateVector {
    val j = barycenterStateContrib(time, Body.Jupiter, JUPITER_GM)
    val s = barycenterStateContrib(time, Body.Saturn,  SATURN_GM)
    val u = barycenterStateContrib(time, Body.Uranus,  URANUS_GM)
    val n = barycenterStateContrib(time, Body.Neptune, NEPTUNE_GM)
    return StateVector(
        j.x + s.x + u.x + n.x,
        j.y + s.y + u.y + n.y,
        j.z + s.z + u.z + n.z,
        j.vx + s.vx + u.vx + n.vx,
        j.vy + s.vy + u.vy + n.vy,
        j.vz + s.vz + u.vz + n.vz,
        time
    )
}

/**
 * Calculates heliocentric Cartesian coordinates of a body in the J2000 equatorial system.
 *
 * This function calculates the position of the given celestial body as a vector,
 * using the center of the Sun as the origin.  The result is expressed as a Cartesian
 * vector in the J2000 equatorial system: the coordinates are based on the mean equator
 * of the Earth at noon UTC on 1 January 2000.
 *
 * The position is not corrected for light travel time or aberration.
 * This is different from the behavior of [geoVector].
 *
 * If given an invalid value for `body`, this function will throw an [InvalidBodyException].
 *
 * @param body
 * A body for which to calculate a heliocentric position:
 * the Sun, Moon, EMB, SSB, or any of the planets.
 * Also allowed to be a user-defined star created by [defineStar].
 *
 * @param time
 * The date and time for which to calculate the position.
 *
 * @return The heliocentric position vector of the center of the given body.
 */
fun helioVector(body: Body, time: Time): Vector {
    val star = userDefinedStar(body)
    if (star != null)
        return Spherical(star.dec, 15.0*star.ra, star.dist).toVector(time)

    return when (body) {
        Body.Sun   -> Vector(0.0, 0.0, 0.0, time)
        Body.Pluto -> calcPluto(time, true).position()
        Body.Moon  -> helioEarthPos(time) + geoMoon(time)
        Body.EMB   -> helioEarthPos(time) + (geoMoon(time) / (1.0 + EARTH_MOON_MASS_RATIO))
        Body.SSB   -> solarSystemBarycenterPos(time)
        else       -> calcVsop(vsopModel(body), time)
    }
}

/**
 * Calculates the distance between a body and the Sun at a given time.
 *
 * Given a date and time, this function calculates the distance between
 * the center of `body` and the center of the Sun, expressed in AU.
 * For the planets Mercury through Neptune, this function is significantly
 * more efficient than calling [helioVector] followed by taking the length
 * of the resulting vector.
 *
 * @param body
 * A body for which to calculate a heliocentric distance:
 * the Sun, Moon, EMB, SSB, any of the planets, or a user-defined star.
 *
 * @param time
 * The date and time for which to calculate the distance.
 *
 * @return The heliocentric distance in AU.
 */
fun helioDistance(body: Body, time: Time): Double {
    if (body == Body.Sun)
        return 0.0
    val star = userDefinedStar(body)
    if (star != null)
        return star.dist
    val vm = optionalVsopModel(body)
    return if (vm != null)
        vsopDistance(vm, time)
    else
        helioVector(body, time).length()
}

/**
 * Calculates heliocentric position and velocity vectors for the given body.
 *
 * Given a body and a time, calculates the position and velocity
 * vectors for the center of that body at that time, relative to the center of the Sun.
 * The vectors are expressed in J2000 mean equator coordinates (EQJ).
 * If you need the position vector only, it is more efficient to call [helioVector].
 * The Sun's center is a non-inertial frame of reference. In other words, the Sun
 * experiences acceleration due to gravitational forces, mostly from the larger
 * planets (Jupiter, Saturn, Uranus, and Neptune). If you want to calculate momentum,
 * kinetic energy, or other quantities that require a non-accelerating frame
 * of reference, consider using [baryState] instead.
 *
 * @param body
 * The celestial body whose heliocentric state vector is to be calculated.
 * Supported values are [Body.Sun], [Body.Moon], [Body.EMB], [Body.SSB], and all planets:
 * [Body.Mercury], [Body.Venus], [Body.Earth], [Body.Mars], [Body.Jupiter],
 * [Body.Saturn], [Body.Uranus], [Body.Neptune], [Body.Pluto].
 * Also allowed to be a user-defined star created by [defineStar].
 *
 * @param time
 * The date and time for which to calculate position and velocity.
 *
 * @return
 * A state vector that contains heliocentric position and velocity vectors.
 * The positions are expressed in AU.
 * The velocities are expressed in AU/day.
 */
fun helioState(body: Body, time: Time): StateVector {
    if (null != userDefinedStar(body)) {
        val vec = helioVector(body, time)
        return StateVector(vec.x, vec.y, vec.z, 0.0, 0.0, 0.0, time)
    }

    return when (body) {
        Body.Sun   -> StateVector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, time)
        Body.Pluto -> calcPluto(time, true)
        Body.Moon  -> helioEarthState(time) + geoMoonState(time)
        Body.EMB   -> helioEarthState(time) + (geoMoonState(time) / (1.0 + EARTH_MOON_MASS_RATIO))
        Body.SSB   -> solarSystemBarycenterState(time)
        else       -> StateVector(calcVsopPosVel(vsopModel(body), time.tt), time)
    }
}


/**
 * Calculates barycentric position and velocity vectors for the given body.
 *
 * Given a body and a time, calculates the barycentric position and velocity
 * vectors for the center of that body at that time.
 * The vectors are expressed in J2000 mean equator coordinates (EQJ).
 *
 * @param body
 * The celestial body whose barycentric state vector is to be calculated.
 * Supported values are [Body.Sun], [Body.Moon], [Body.EMB], [Body.SSB], and all planets:
 * [Body.Mercury], [Body.Venus], [Body.Earth], [Body.Mars], [Body.Jupiter],
 * [Body.Saturn], [Body.Uranus], [Body.Neptune], [Body.Pluto].
 *
 * @param time
 * The date and time for which to calculate position and velocity.
 *
 * @return The barycentric position and velocity vectors of the body.
 */
fun baryState(body: Body, time: Time): StateVector {
    // Trival case: the solar system barycenter itself:
    if (body == Body.SSB)
        return StateVector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, time)

    if (body == Body.Pluto)
        return calcPluto(time, false)

    // Find the barycentric positions and velocities for the 5 major bodies.
    val bary = majorBodyBary(time.tt)

    return when (body) {
        // If the caller is asking for one of the major bodies, we already have the answer.
        Body.Sun     -> exportState(bary.sun,     time)
        Body.Jupiter -> exportState(bary.jupiter, time)
        Body.Saturn  -> exportState(bary.saturn,  time)
        Body.Uranus  -> exportState(bary.uranus,  time)
        Body.Neptune -> exportState(bary.neptune, time)

        // The Moon and EMB require calculating both the heliocentric Earth and geocentric Moon.
        Body.Moon -> exportState(bary.sun, time) + helioEarthState(time) + geoMoonState(time)
        Body.EMB  -> exportState(bary.sun, time) + helioEarthState(time) + geoEmbState(time)

        else -> {
            val planet: BodyState = calcVsopPosVel(vsopModel(body), time.tt)
            StateVector(
                bary.sun.r.x + planet.r.x,
                bary.sun.r.y + planet.r.y,
                bary.sun.r.z + planet.r.z,
                bary.sun.v.x + planet.v.x,
                bary.sun.v.y + planet.v.y,
                bary.sun.v.z + planet.v.z,
                time
            )
        }
    }
}

/**
 * A function for which to solve a light-travel time problem.
 *
 * The function [correctLightTravel] solves a generalized
 * problem of deducing how far in the past light must have left
 * a target object to be seen by an observer at a specified time.
 * This interface expresses an arbitrary position vector as
 * function of time that is passed to [correctLightTravel].
 */
fun interface PositionFunction {
    /**
     * Returns a relative position vector for a given time.
     *
     * @param time
     * The time at which to evaluate a relative position vector.
     *
     * @return The relative position vector at the specified time.
     */
    fun position(time: Time): Vector
}


/**
 * Solve for light travel time of a vector function.
 *
 * When observing a distant object, for example Jupiter as seen from Earth,
 * the amount of time it takes for light to travel from the object to the
 * observer can significantly affect the object's apparent position.
 * This function is a generic solver that figures out how long in the
 * past light must have left the observed object to reach the observer
 * at the specified observation time. It uses [PositionFunction]
 * to express an arbitrary position vector as a function of time.
 *
 * This function repeatedly calls `func.Position`, passing a series of time
 * estimates in the past. Then `func.Position` must return a relative state vector between
 * the observer and the target. `correctLightTravel` keeps calling
 * `func.Position` with more and more refined estimates of the time light must have
 * left the target to arrive at the observer.
 *
 * For common use cases, it is simpler to use [backdatePosition]
 * for calculating the light travel time correction of one body observing another body.
 *
 * For geocentric calculations, #geoVector also backdates the returned
 * position vector for light travel time, only it returns the observation time in
 * the returned vector's `t` field rather than the backdated time.
 *
 * @param func
 * An arbitrary position vector as a function of time.
 *
 * @param time
 * The observation time for which to solve for light travel delay.
 *
 * @return
 * The position vector at the solved backdated time.
 * The `t` field holds the time that light left the observed
 * body to arrive at the observer at the observation time.
 */
fun correctLightTravel(func: PositionFunction, time: Time): Vector {
    var ltime = time
    for (iter in 0..10) {
        val pos = func.position(ltime)
        val ltime2 = time.addDays(-pos.length() / C_AUDAY)
        val dt = abs(ltime2.tt - ltime.tt)
        if (dt < 1.0e-9)        // 86.4 microseconds
            return pos
        ltime = ltime2
    }
    throw InternalError("Light travel time correction did not converge.")
}


internal class BodyPosition(
    val observerBody: Body,
    val targetBody: Body,
    val aberration: Aberration,
    val observerPos: Vector
) : PositionFunction {
    override fun position(time: Time): Vector {
        val opos = when (aberration) {
            Aberration.None ->
                // No aberration, so use the pre-calculated initial position of
                // the observer body that is already stored in `observerPos`.
                // To avoid an exception in the subtraction below, patch the time.
                observerPos.withTime(time)

            Aberration.Corrected ->
                // The following discussion is worded with the observer body being the Earth,
                // which is often the case. However, the same reasoning applies to any observer body
                // without loss of generality.
                //
                // To include aberration, make a good first-order approximation
                // by backdating the Earth's position also.
                // This is confusing, but it works for objects within the Solar System
                // because the distance the Earth moves in that small amount of light
                // travel time (a few minutes to a few hours) is well approximated
                // by a line segment that substends the angle seen from the remote
                // body viewing Earth. That angle is pretty close to the aberration
                // angle of the moving Earth viewing the remote body.
                // In other words, both of the following approximate the aberration angle:
                //     (transverse distance Earth moves) / (distance to body)
                //     (transverse speed of Earth) / (speed of light).
                helioVector(observerBody, time)
        }

        // Subtract the bodies' heliocentric positions to obtain a relative position vector.
        return helioVector(targetBody, time) - opos
    }
}


/**
 * Solve for light travel time correction of apparent position.
 *
 * When observing a distant object, for example Jupiter as seen from Earth,
 * the amount of time it takes for light to travel from the object to the
 * observer can significantly affect the object's apparent position.
 *
 * This function solves the light travel time correction for the apparent
 * relative position vector of a target body as seen by an observer body
 * at a given observation time.
 *
 * For geocentric calculations, #geoVector also includes light
 * travel time correction, but the time `t` embedded in its returned vector
 * refers to the observation time, not the backdated time that light left
 * the observed body. Thus `backdatePosition` provides direct
 * access to the light departure time for callers that need it.
 *
 * For a more generalized light travel correction solver, see [correctLightTravel].
 *
 * @param time
 * The time of observation.
 *
 * @param observerBody
 * The body to be used as the observation location.
 *
 * @param targetBody
 * The body to be observed.
 *
 * @param aberration
 * `Aberration.Corrected` to correct for aberration, or `Aberration.None` to leave uncorrected.
 *
 * @return
 * The position vector at the solved backdated time.
 * Its `t` field holds the time that light left the observed
 * body to arrive at the observer at the observation time.
 */
fun backdatePosition(
    time: Time,
    observerBody: Body,
    targetBody: Body,
    aberration: Aberration
): Vector {
    val observerPos = when (aberration) {
        Aberration.None ->
            // Without aberration, we need the observer body position at the observation time only.
            // For efficiency, calculate it once and hold onto it, so `BodyPosition` can keep using it.
            helioVector(observerBody, time)

        Aberration.Corrected ->
            // With aberration, `backdatePosition` will calculate `observerPos` at different times.
            // Therefore, do not waste time calculating it now.
            // Provide a placeholder value, even though it will be ignored.
            Vector(0.0, 0.0, 0.0, time)
    }
    val func = BodyPosition(observerBody, targetBody, aberration, observerPos)
    val vec = correctLightTravel(func, time)
    return vec.withTime(time)       // tricky: return the observation time, not the backdated time
}


/**
 * Calculates geocentric Cartesian coordinates of a body in the J2000 equatorial system.
 *
 * This function calculates the position of the given celestial body as a vector,
 * using the center of the Earth as the origin.  The result is expressed as a Cartesian
 * vector in the J2000 equatorial system: the coordinates are based on the mean equator
 * of the Earth at noon UTC on 1 January 2000.
 *
 * If given an invalid value for `body`, this function will throw an exception.
 *
 * Unlike [helioVector], this function always corrects for light travel time.
 * This means the position of the body is "back-dated" by the amount of time it takes
 * light to travel from that body to an observer on the Earth.
 *
 * Also, the position can optionally be corrected for
 * [aberration](https://en.wikipedia.org/wiki/Aberration_of_light), an effect
 * causing the apparent direction of the body to be shifted due to transverse
 * movement of the Earth with respect to the rays of light coming from that body.
 *
 * @param body
 * A body for which to calculate a heliocentric position: the Sun, Moon, or any of the planets.
 *
 * @param time
 * The date and time for which to calculate the position.
 *
 * @param aberration
 * [Aberration.Corrected] to correct for aberration, or [Aberration.None] to leave uncorrected.
 *
 * @return A geocentric position vector of the center of the given body.
 */
fun geoVector(body: Body, time: Time, aberration: Aberration): Vector =
    when (body) {
        Body.Earth -> Vector(0.0, 0.0, 0.0, time)
        Body.Moon  -> geoMoon(time)
        else       -> backdatePosition(time, Body.Earth, body, aberration).withTime(time)
    }


/**
 * Calculates equatorial coordinates of a celestial body as seen by an observer on the Earth's surface.
 *
 * Calculates topocentric equatorial coordinates in one of two different systems:
 * J2000 or true-equator-of-date, depending on the value of the `equdate` parameter.
 * Equatorial coordinates include right ascension, declination, and distance in astronomical units.
 *
 * This function corrects for light travel time: it adjusts the apparent location
 * of the observed body based on how long it takes for light to travel from the body to the Earth.
 *
 * This function corrects for *topocentric parallax*, meaning that it adjusts for the
 * angular shift depending on where the observer is located on the Earth. This is most
 * significant for the Moon, because it is so close to the Earth. However, parallax corection
 * has a small effect on the apparent positions of other bodies.
 *
 * Correction for aberration is optional, using the `aberration` parameter.
 *
 * @param body
 * The celestial body to be observed. Not allowed to be [Body.Earth].
 *
 * @param time
 * The date and time at which the observation takes place.
 *
 * @param observer
 * A location on or near the surface of the Earth.
 *
 * @param equdate
 * Selects the date of the Earth's equator in which to express the equatorial coordinates.
 *
 * @param aberration
 * Selects whether or not to correct for aberration.
 *
 * @return Topocentric equatorial coordinates of the celestial body.
 */
fun equator(
    body: Body,
    time: Time,
    observer: Observer,
    equdate: EquatorEpoch,
    aberration: Aberration
): Equatorial {
    val gcObserver = geoPos(time, observer)
    val gc = geoVector(body, time, aberration)
    val j2000 = gc - gcObserver
    return when (equdate) {
        EquatorEpoch.OfDate -> gyration(j2000, PrecessDirection.From2000)
        EquatorEpoch.J2000  -> j2000
    }.toEquatorial()
}

/**
 * Calculates the apparent location of a body relative to the local horizon of an observer on the Earth.
 *
 * Given a date and time, the geographic location of an observer on the Earth, and
 * equatorial coordinates (right ascension and declination) of a celestial body,
 * this function returns horizontal coordinates (azimuth and altitude angles) for the body
 * relative to the horizon at the geographic location.
 *
 * The right ascension `ra` and declination `dec` passed in must be *equator of date*
 * coordinates, based on the Earth's true equator at the date and time of the observation.
 * Otherwise the resulting horizontal coordinates will be inaccurate.
 * Equator of date coordinates can be obtained by calling [equator], passing in
 * [EquatorEpoch.OfDate] as its `equdate` parameter. It is also recommended to enable
 * aberration correction by passing in [Aberration.Corrected] as the `aberration` parameter.
 *
 * This function optionally corrects for atmospheric refraction.
 * For most uses, it is recommended to pass [Refraction.Normal] in the `refraction` parameter to
 * correct for optical lensing of the Earth's atmosphere that causes objects
 * to appear somewhat higher above the horizon than they actually are.
 * However, callers may choose to avoid this correction by passing in [Refraction.None].
 * If refraction correction is enabled, the azimuth, altitude, right ascension, and declination
 * in the [Topocentric] object returned by this function will all be corrected for refraction.
 * If refraction is disabled, none of these four coordinates will be corrected; in that case,
 * the right ascension and declination in the returned structure will be numerically identical
 * to the respective `ra` and `dec` values passed in.
 *
 * @param time
 * The date and time of the observation.
 *
 * @param observer
 * The geographic location of the observer.
 *
 * @param ra
 * The right ascension of the body in sidereal hours. See remarks above for more details.
 *
 * @param dec
 * The declination of the body in degrees. See remarks above for more details.
 *
 * @param refraction
 * Selects whether to correct for atmospheric refraction, and if so, which model to use.
 * The recommended value for most uses is `Refraction.Normal`.
 * See remarks above for more details.
 *
 * @return
 * The body's apparent horizontal coordinates and equatorial coordinates, both optionally corrected for refraction.
 */
fun horizon(
    time: Time,
    observer: Observer,
    ra: Double,
    dec: Double,
    refraction: Refraction
): Topocentric {
    val sinlat = dsin(observer.latitude)
    val coslat = dcos(observer.latitude)
    val sinlon = dsin(observer.longitude)
    val coslon = dcos(observer.longitude)
    val sindc = dsin(dec)
    val cosdc = dcos(dec)
    val sinra = dsin(ra * 15.0)
    val cosra = dcos(ra * 15.0)

    // Calculate three mutually perpendicular unit vectors
    // in equatorial coordinates: uze, une, uwe.
    //
    // uze = The direction of the observer's local zenith (straight up).
    // une = The direction toward due north on the observer's horizon.
    // uwe = The direction toward due west on the observer's horizon.
    //
    // HOWEVER, these are uncorrected for the Earth's rotation due to the time of day.
    //
    // The components of these 3 vectors are as follows:
    // x = direction from center of Earth toward 0 degrees longitude (the prime meridian) on equator.
    // y = direction from center of Earth toward 90 degrees west longitude on equator.
    // z = direction from center of Earth toward the north pole.
    val uze = Vector(coslat * coslon, coslat * sinlon, sinlat, time)
    val une = Vector(-sinlat * coslon, -sinlat * sinlon, coslat, time)
    val uwe = Vector(sinlon, -coslon, 0.0, time)

    // Correct the vectors uze, une, uwe for the Earth's rotation by calculating
    // sidereal time. Call spin() for each uncorrected vector to rotate about
    // the Earth's axis to yield corrected unit vectors uz, un, uw.
    // Multiply sidereal hours by -15 to convert to degrees and flip eastward
    // rotation of the Earth to westward apparent movement of objects with time.
    val angle = -15.0 * siderealTime(time)
    val uz = spin(angle, uze)
    val un = spin(angle, une)
    val uw = spin(angle, uwe)

    // Convert angular equatorial coordinates (RA, DEC) to
    // cartesian equatorial coordinates in 'p', using the
    // same orientation system as uze, une, uwe.
    val p = Vector(cosdc * cosra, cosdc * sinra, sindc, time)

    // Use dot products of p with the zenith, north, and west
    // vectors to obtain the cartesian coordinates of the body in
    // the observer's horizontal orientation system.
    // pz = zenith component [-1, +1]
    // pn = north  component [-1, +1]
    // pw = west   component [-1, +1]
    val pz = p dot uz
    val pn = p dot un
    val pw = p dot uw

    // projHor is the "shadow" of the body vector along the observer's flat ground.
    val projHor = hypot(pn, pw)

    // Calculate az = azimuth (compass direction clockwise from East.)
    val az = (
        if (projHor > 0.0) (
            // If the body is not exactly straight up/down, it has an azimuth.
            // Invert the angle to produce degrees eastward from north.
            (-datan2(pw, pn)).withMinDegreeValue(0.0)
        ) else (
            // The body is straight up/down, so it does not have an azimuth.
            // Report an arbitrary but reasonable value.
            0.0
        )
    )

    // zd = the angle of the body away from the observer's zenith, in degrees.
    var zd = datan2(projHor, pz)
    var horRa = ra
    var horDec = dec

    if (refraction != Refraction.None) {
        val zd0 = zd
        val refr = refractionAngle(refraction, 90.0 - zd)
        zd -= refr

        if (refr > 0.0 && zd > 3.0e-4) {
            // Calculate refraction-corrected equatorial coordinates.
            val sinzd  = dsin(zd)
            val coszd  = dcos(zd)
            val sinzd0 = dsin(zd0)
            val coszd0 = dcos(zd0)

            val prx = ((p.x - coszd0 * uz.x) / sinzd0)*sinzd + uz.x*coszd
            val pry = ((p.y - coszd0 * uz.y) / sinzd0)*sinzd + uz.y*coszd
            val prz = ((p.z - coszd0 * uz.z) / sinzd0)*sinzd + uz.z*coszd

            val projEqu = hypot(prx, pry)

            horRa =
                if (projEqu > 0.0)
                    datan2(pry, prx).withMinDegreeValue(0.0) / 15.0
                else
                    0.0

            horDec = datan2(prz, projEqu)
        }
    }

    return Topocentric(az, 90.0 - zd, horRa, horDec)
}


/**
 * Calculates heliocentric ecliptic longitude of a body.
 *
 * This function calculates the angle around the plane of the Earth's orbit
 * of a celestial body, as seen from the center of the Sun.
 * The angle is measured prograde (in the direction of the Earth's orbit around the Sun)
 * in degrees from the true equinox of date. The ecliptic longitude is always in the range [0, 360).
 *
 * @param body
 * A body other than the Sun.
 *
 * @param time
 * The date and time at which the body's ecliptic longitude is to be calculated.
 *
 * @return
 * The ecliptic longitude in degrees of the given body at the given time.
 */
fun eclipticLongitude(body: Body, time: Time): Double {
    if (body == Body.Sun)
        throw InvalidBodyException(body)

    val hv = helioVector(body, time)
    val eclip = equatorialToEcliptic(hv)
    return eclip.elon
}


internal fun relativeLongitudeOffset(body: Body, time: Time, direction: Int, targetRelativeLongitude: Double): Double {
    val plon = eclipticLongitude(body, time)
    val elon = eclipticLongitude(Body.Earth, time)
    val diff = direction * (elon - plon)
    return longitudeOffset(diff - targetRelativeLongitude)
}

/**
 * Finds the mean number of days after which `body` returns to the same ecliptic longitude as seen from the Earth.
 */
internal fun synodicPeriod(body: Body): Double {
    val period: Double = body.orbitalPeriod ?: throw InvalidBodyException(body)
    return abs(EARTH_ORBITAL_PERIOD / (EARTH_ORBITAL_PERIOD/period - 1.0))
}


/**
 * Searches for the time when the Earth and another planet are separated by a specified angle in ecliptic longitude, as seen from the Sun.
 *
 * A relative longitude is the angle between two bodies measured in the plane of the Earth's orbit
 * (the ecliptic plane). The distance of the bodies above or below the ecliptic plane is ignored.
 * If you imagine the shadow of the body cast onto the ecliptic plane, and the angle measured around
 * that plane from one body to the other in the direction the planets orbit the Sun, you will get an
 * angle somewhere between 0 and 360 degrees. This is the relative longitude.
 *
 * Given a planet other than the Earth in `body` and a time to start the search in `startTime`,
 * this function searches for the next time that the relative longitude measured from the planet
 * to the Earth is `targetRelLon`.
 *
 * Certain astronomical events are defined in terms of relative longitude between the Earth and another planet:
 *
 * - When the relative longitude is 0 degrees, it means both planets are in the same direction from the Sun.
 *   For planets that orbit closer to the Sun (Mercury and Venus), this is known as *inferior conjunction*,
 *   a time when the other planet becomes very difficult to see because of being lost in the Sun's glare.
 *   (The only exception is in the rare event of a transit, when we see the silhouette of the planet passing
 *   between the Earth and the Sun.)
 *
 * - When the relative longitude is 0 degrees and the other planet orbits farther from the Sun,
 *   this is known as *opposition*.  Opposition is when the planet is closest to the Earth, and
 *   also when it is visible for most of the night, so it is considered the best time to observe the planet.
 *
 * - When the relative longitude is 180 degrees, it means the other planet is on the opposite side of the Sun
 *   from the Earth. This is called *superior conjunction*. Like inferior conjunction, the planet is
 *   very difficult to see from the Earth. Superior conjunction is possible for any planet other than the Earth.
 *
 * @param body
 * A planet other than the Earth. Any other body will cause an exception.
 *
 * @param targetRelativeLongitude
 * The desired relative longitude, expressed in degrees. Must be in the range [0, 360).
 *
 * @param startTime
 * The date and time at which to begin the search.
 *
 * @return
 * The time of the first relative longitude event that occurs after `startTime`.
 */
fun searchRelativeLongitude(body: Body, targetRelativeLongitude: Double, startTime: Time): Time {
    val direction: Int = when (body) {
        // Planets that orbit closer to the Sun than the Earth are called "inferior" planets.
        // Because they orbit faster than the Earth, their relative longitudes are always decreasing.
        Body.Mercury, Body.Venus -> -1

        // Planets that orbit farther from the Sun than the Earth are called "superior" planets.
        // Because they orbit slower than the Earth, their relative longitudes are always increasing.
        Body.Mars, Body.Jupiter, Body.Saturn, Body.Uranus, Body.Neptune, Body.Pluto -> +1

        // No other bodies are supported by this function.
        else -> throw InvalidBodyException(body)
    }

    var syn: Double = synodicPeriod(body)

    // Iterate until we converge on the desired event.
    // Calculate the error angle, which will be a negative number of degrees,
    // meaning we are "behind" the target relative longitude.
    var errorAngle = relativeLongitudeOffset(body, startTime, direction, targetRelativeLongitude)
    if (errorAngle > 0.0)
        errorAngle -= 360.0     // force searching forward in time, not backward

    var time = startTime
    for (iter in 0..99) {
        // Estimate how many days in the future (postive) or past (negative)
        // we have to go to get closer to the target relative longitude.
        val dayAdjust = (-errorAngle/360.0) * syn
        time = time.addDays(dayAdjust)
        if (abs(dayAdjust) * SECONDS_PER_DAY < 1.0)
            return time     // we found the solution within a 1-second tolerance

        val prevAngle = errorAngle
        errorAngle = relativeLongitudeOffset(body, time, direction, targetRelativeLongitude)
        if (abs(prevAngle) < 30.0 && (prevAngle != errorAngle)) {
            // Improve convergence for the Mercury and Mars, which have eccentric orbits.
            // Adjust the synodic period to more closely match the variable speed of both
            // planets in this part of their respective orbits.
            val ratio = prevAngle / (prevAngle - errorAngle)
            if (ratio > 0.5 && ratio < 2.0)
                syn *= ratio
        }
    }

    throw InternalError("Relative longitude search failed to converge.")
}


/**
 * Searches for the first transit of Mercury or Venus after a given date.
 *
 * Finds the first transit of Mercury or Venus after a specified date.
 * A transit is when an inferior planet passes between the Sun and the Earth
 * so that the silhouette of the planet is visible against the Sun in the background.
 * To continue the search, pass the `finish` time in the returned object to [nextTransit].
 *
 * See [transitsAfter] for convenient iteration of consecutive transits.
 *
 * @param body
 * The planet whose transit is to be found. Must be [Body.Mercury] or [Body.Venus].
 *
 * @param startTime
 * The date and time for starting the search for a transit.
 */
fun searchTransit(body: Body, startTime: Time): TransitInfo {
    val thresholdAngle = 0.4    // maximum angular separation to attempt transit calculations
    val dtDays = 1.0

    // Validate the planet and find its mean radius.
    val planetRadiusKm = when (body) {
        Body.Mercury -> 2439.7
        Body.Venus   -> 6051.8
        else -> throw InvalidBodyException(body)
    }

    var searchTime = startTime
    while (true) {
        // Search for the next inferior conjunction of the given planet.
        // This is the next time the Earth and the other planet have the same
        // ecliptic longitude as seen from the Sun.
        val conj = searchRelativeLongitude(body, 0.0, searchTime)

        // Calculate the angular separation between the body and Sun at this time.
        val separation = angleFromSun(body, conj)

        if (separation < thresholdAngle) {
            // The planet's angular separation from the Sun is small enough
            // to consider it a transit candidate.
            // Search for the moment when the line passing through the Sun
            // and planet are closest to the Earth's center.
            val shadow = peakPlanetShadow(body, planetRadiusKm, conj)
            if (shadow.r < shadow.p) {      // does the planet's penumbra touch the Earth's center?
                // Find the beginning and end of the penumbral contact.
                val t1 = shadow.time.addDays(-dtDays)
                val transitStart = planetTransitBoundary(body, planetRadiusKm, t1, shadow.time, -1.0)

                val t2 = shadow.time.addDays(+dtDays)
                val transitFinish = planetTransitBoundary(body, planetRadiusKm, shadow.time, t2, +1.0)

                val transitSeparation = 60.0 * angleFromSun(body, shadow.time)

                return TransitInfo(transitStart, shadow.time, transitFinish, transitSeparation)
            }
        }

        // This inferior conjunction was not a transit. Try the next inferior conjunction.
        searchTime = conj.addDays(10.0)
    }
}


/**
 * Searches for another transit of Mercury or Venus.
 *
 * After calling [searchTransit] to find a transit of Mercury or Venus,
 * this function finds the next transit after that.
 * Keep calling this function as many times as you want to keep finding more transits.
 *
 * See [transitsAfter] for convenient iteration of consecutive transits.
 *
 * @param body
 * The planet whose transit is to be found. Must be [Body.Mercury] or [Body.Venus].
 *
 * @param prevTransitTime
 * A date and time near the previous transit.
 */
fun nextTransit(body: Body, prevTransitTime: Time) =
    searchTransit(body, prevTransitTime.addDays(100.0))


/**
 * Enumerates a series of consecutive transits of Mercury or Venus.
 *
 * This function enables iteration through a series of consecutive
 * transits of Mercury or Venus that occur after a specified time.
 *
 * This is a convenience wrapper around [searchTransit] and [nextTransit].
 *
 * @param body
 * The planet for which to enumerate transits. Must be [Body.Mercury] or [Body.Venus].
 *
 * @param startTime
 * The date and time for starting the search for a series of transits.
 */
fun transitsAfter(body: Body, startTime: Time): Sequence<TransitInfo> =
    generateSequence(searchTransit(body, startTime)) { nextTransit(body, it.finish) }



/**
 * Calculates jovicentric positions and velocities of Jupiter's largest 4 moons.
 *
 * Calculates position and velocity vectors for Jupiter's moons
 * Io, Europa, Ganymede, and Callisto, at the given date and time.
 * The vectors are jovicentric (relative to the center of Jupiter).
 * Their orientation is the Earth's equatorial system at the J2000 epoch (EQJ).
 * The position components are expressed in astronomical units (AU), and the
 * velocity components are in AU/day.
 *
 * To convert to heliocentric position vectors, call [helioVector]
 * with [Body.Jupiter] to get Jupiter's heliocentric position, then
 * add the jovicentric positions. Likewise, you can call [geoVector]
 * to convert to geocentric positions; however, you will have to manually
 * correct for light travel time from the Jupiter system to Earth to
 * figure out what time to pass to `jupiterMoons` to get an accurate picture
 * of how Jupiter and its moons look from Earth.
 */
fun jupiterMoons(time: Time) =
    JupiterMoonsInfo(
        calcJupiterMoon(time, jupiterMoonModel[0]),
        calcJupiterMoon(time, jupiterMoonModel[1]),
        calcJupiterMoon(time, jupiterMoonModel[2]),
        calcJupiterMoon(time, jupiterMoonModel[3])
    )

/**
 * Searches for a time at which a function's value increases through zero.
 *
 * Certain astronomy calculations involve finding a time when an event occurs.
 * Often such events can be defined as the root of a function:
 * the time at which the function's value becomes zero.
 *
 * `search` finds the *ascending root* of a function: the time at which
 * the function's value becomes zero while having a positive slope. That is, as time increases,
 * the function transitions from a negative value, through zero at a specific moment,
 * to a positive value later. The goal of the search is to find that specific moment.
 *
 * The `func` parameter is an instance of the interface [SearchContext].
 * As an example, a caller may wish to find the moment a celestial body reaches a certain
 * ecliptic longitude. In that case, the caller might derive a class that contains
 * a [Body] member to specify the body and a `Double` to hold the target longitude.
 * It could subtract the target longitude from the actual longitude at a given time;
 * thus the difference would equal zero at the moment in time the planet reaches the
 * desired longitude.
 *
 * Every time it is called, `func.eval` returns a `Double` value or it throws an exception.
 * If `func.eval` throws an exception, the search immediately fails and the exception
 * is propagated to the caller. Otherwise, the search proceeds until it either finds
 * the ascending root or fails for some reason.
 *
 * The search calls `func.eval` repeatedly to rapidly narrow in on any ascending
 * root within the time window specified by `time1` and `time2`. The search never
 * reports a solution outside this time window.
 *
 * `search` uses a combination of bisection and quadratic interpolation
 * to minimize the number of function calls. However, it is critical that the
 * supplied time window be small enough that there cannot be more than one root
 * (ascedning or descending) within it; otherwise the search can fail.
 * Beyond that, it helps to make the time window as small as possible, ideally
 * such that the function itself resembles a smooth parabolic curve within that window.
 *
 * If an ascending root is not found, or more than one root
 * (ascending and/or descending) exists within the window `time1`..`time2`,
 * the search will return `null`.
 *
 * If the search does not converge within 20 iterations, it will throw an exception.
 *
 * @param func
 * The function for which to find the time of an ascending root.
 * See remarks above for more details.
 *
 * @param time1
 * The lower time bound of the search window.
 * See remarks above for more details.
 *
 * @param time2
 * The upper time bound of the search window.
 * See remarks above for more details.
 *
 * @param toleranceSeconds
 * Specifies an amount of time in seconds within which a bounded ascending root
 * is considered accurate enough to stop. A typical value is 1 second.
 *
 * @return
 * If successful, returns an [Time] value indicating a date and time
 * that is within `toleranceSeconds` of an ascending root.
 * If no ascending root is found, or more than one root exists in the time
 * window `time1`..`time2`, the function returns `null`.
 */
fun search(
    time1: Time,
    time2: Time,
    toleranceSeconds: Double,
    func: SearchContext,
): Time? {
    var t1 = time1
    var t2 = time2
    val iterLimit = 20
    val toleranceDays = abs(toleranceSeconds / SECONDS_PER_DAY)
    var f1 = func.eval(t1)
    var f2 = func.eval(t2)
    var calcFmid = true
    var fmid = 0.0

    for (iter in 1..iterLimit) {
        val dt = (t2.tt - t1.tt) / 2.0
        val tmid = t1.addDays(dt)
        if (dt.absoluteValue < toleranceDays) {
            // We are close enough to the event to stop the search.
            return tmid
        }

        if (calcFmid)
            fmid = func.eval(tmid)
        else
            calcFmid = true     // we already have the correct value of fmid from the previous loop

        // Quadratic interpolation:
        // Try to find a parabola that passes through the 3 points we have sampled:
        // (t1,f1), (tmid,fmid), (t2,f2).
        val tm = tmid.ut
        val tspan = t2.ut - tmid.ut
        val q = (f2 + f1)/2.0 - fmid
        val r = (f2 - f1)/2.0
        val s = fmid
        var foundInterpolation = false
        var x = Double.NaN
        if (q == 0.0) {
            // This is a line, not a parabola.
            if (r != 0.0) {     // skip horizontal lines: they don't have a root
                x = -s / r
                foundInterpolation = (-1.0 <= x && x <= +1.0)
            }
        } else {
            // This really is a parabola. Find its roots x1, x2.
            val u = r*r - 4.0*q*s
            if (u > 0.0) {      // skip imaginary or tangent roots
                // See if there is a unique solution for x in the range [-1, +1].
                val ru = sqrt(u)
                val x1 = (-r + ru) / (2.0 * q)
                val x2 = (-r - ru) / (2.0 * q)
                val x1Valid = (-1.0 <= x1 && x1 <= +1.0)
                val x2Valid = (-1.0 <= x2 && x2 <= +1.0)
                if (x1Valid && !x2Valid) {
                    x = x1
                    foundInterpolation = true
                } else if (x2Valid && !x1Valid) {
                    x = x2
                    foundInterpolation = true
                }
            }
        }
        if (foundInterpolation) {
            val qut = tm + x*tspan
            val qslope = (2*q*x + r) / tspan
            val tq = Time(qut)
            val fq = func.eval(tq)
            if (qslope != 0.0) {
                var dtGuess = abs(fq / qslope)
                if (dtGuess < toleranceDays) {
                    // The estimated time error is small enough that we can quit now.
                    return tq
                }

                // Try guessing a tighter boundary with the interpolated root at the center.
                dtGuess *= 1.2
                if (dtGuess < dt / 10.0) {
                    val tleft = tq.addDays(-dtGuess)
                    val tright = tq.addDays(+dtGuess)
                    if ((tleft.ut - t1.ut)*(tleft.ut - t2.ut) < 0.0) {
                        if ((tright.ut - t1.ut)*(tright.ut - t2.ut) < 0.0) {
                            val fleft = func.eval(tleft)
                            val fright = func.eval(tright)
                            if ((fleft < 0.0) && (fright >= 0.0)) {
                                f1 = fleft
                                f2 = fright
                                t1 = tleft
                                t2 = tright
                                fmid = fq
                                calcFmid = false    // save a little work; no need to recalculate fmid next time
                                continue
                            }
                        }
                    }
                }
            }
        }

        // The quadratic interpolation didn't work this time.
        // Use bisection: divide the region in two parts and pick
        // whichever one appears to contain a root.
        if (f1 < 0.0 && fmid >= 0.0) {
            t2 = tmid
            f2 = fmid
            continue
        }

        if (fmid < 0.0 && f2 >= 0.0) {
            t1 = tmid
            f1 = fmid
            continue
        }

        // Either there is no ascending zero-crossing in this range
        // or the search window is too wide (more than one zero-crossing).
        // Either way, the search has failed.
        return null
    }

    throw InternalError("Search did not converge within $iterLimit iterations.")
}

/**
 * Calculates geocentric ecliptic coordinates for the Sun.
 *
 * This function calculates the position of the Sun as seen from the Earth.
 * The returned value includes both Cartesian and spherical coordinates.
 * The x-coordinate and longitude values in the returned object are based
 * on the *true equinox of date*: one of two points in the sky where the instantaneous
 * plane of the Earth's equator at the given date and time (the *equatorial plane*)
 * intersects with the plane of the Earth's orbit around the Sun (the *ecliptic plane*).
 * By convention, the apparent location of the Sun at the March equinox is chosen
 * as the longitude origin and x-axis direction, instead of the one for September.
 *
 * `sunPosition` corrects for precession and nutation of the Earth's axis
 * in order to obtain the exact equatorial plane at the given time.
 *
 * This function can be used for calculating changes of seasons: equinoxes and solstices.
 * In fact, the function [seasons] does use this function for that purpose.
 *
 * @param time
 * The date and time for which to calculate the Sun's position.
 *
 * @return The ecliptic coordinates of the Sun using the Earth's true equator of date.
 */
fun sunPosition(time: Time): Ecliptic {
    // Correct for light travel time from the Sun.
    // Otherwise season calculations (equinox, solstice) will all be early by about 8 minutes!
    val adjustedTime = time.addDays(-1.0 / C_AUDAY)
    val earth2000 = helioEarthPos(adjustedTime)

    // Convert heliocentric location of Earth to geocentric location of Sun.
    val sun2000 = -earth2000

    // Convert to equatorial Cartesian coordinates of date.
    val sunOfDate = gyration(sun2000, PrecessDirection.From2000)

    // Convert equatorial coordinates to ecliptic coordinates.
    val trueObliq = earthTilt(adjustedTime).tobl.degreesToRadians()
    return rotateEquatorialToEcliptic(sunOfDate, trueObliq)
}

private fun rotateEquatorialToEcliptic(pos: Vector, obliqRadians: Double): Ecliptic {
    val cosOb = cos(obliqRadians)
    val sinOb = sin(obliqRadians)
    val ex = +pos.x
    val ey = +pos.y*cosOb + pos.z*sinOb
    val ez = -pos.y*sinOb + pos.z*cosOb
    val xyproj = hypot(ex, ey)
    val elon =
        if (xyproj > 0.0)
            datan2(ey, ex).withMinDegreeValue(0.0)
        else
            0.0
    val elat = datan2(ez, xyproj)
    val vec = Vector(ex, ey, ez, pos.t)
    return Ecliptic(vec, elat, elon)
}

/**
 * Converts a J2000 mean equator (EQJ) vector to a true ecliptic of date (ETC) vector and angles.
 *
 * Given coordinates relative to the Earth's equator at J2000 (the instant of noon UTC
 * on 1 January 2000), this function converts those coordinates to true ecliptic coordinates of date,
 * which are relative to the plane of the Earth's orbit around the Sun.
 *
 * @param eqj
 * Equatorial coordinates in the J2000 frame of reference.
 * You can call [geoVector] to obtain suitable equatorial coordinates.
 *
 * @return Spherical and vector coordinates expressed in true ecliptic coordinates of date (ECT)..
 */
fun equatorialToEcliptic(eqj: Vector): Ecliptic {
    // Calculate nutation and obliquity for this time.
    // As an optimization, the nutation angles are cached in `eqj.t`,
    // and reused below when the `nutation` function is called.
    val et = earthTilt(eqj.t)

    // Convert J2000 mean equator (EQJ) to true equator of date (EQD).
    val eqd = gyration(eqj, PrecessDirection.From2000);

    // Rotate from EQD to true ecliptic of date (ECT).
    return rotateEquatorialToEcliptic(eqd, et.tobl.degreesToRadians())
}

/**
 * Searches for the time when the Sun reaches an apparent ecliptic longitude as seen from the Earth.
 *
 * This function finds the moment in time, if any exists in the given time window,
 * that the center of the Sun reaches a specific ecliptic longitude as seen from the center of the Earth.
 *
 * This function can be used to determine equinoxes and solstices.
 * However, it is usually more convenient and efficient to call [seasons]
 * to calculate all equinoxes and solstices for a given calendar year.
 *
 * The function searches the window of time specified by `startTime` and `startTime+limitDays`.
 * The search will return `null` if the Sun never reaches the longitude `targetLon` or
 * if the window is so large that the longitude ranges more than 180 degrees within it.
 * It is recommended to keep the window smaller than 10 days when possible.
 */
fun searchSunLongitude(targetLon: Double, startTime: Time, limitDays: Double): Time? {
    val time2 = startTime.addDays(limitDays)
    return search(startTime, time2, 0.01) { time ->
        longitudeOffset(sunPosition(time).elon - targetLon)
    }
}

/**
 * Finds both equinoxes and both solstices for a given calendar year.
 *
 * The changes of seasons are defined by solstices and equinoxes.
 * Given a calendar year number, this function calculates the
 * March and September equinoxes and the June and December solstices.
 *
 * The equinoxes are the moments twice each year when the plane of the
 * Earth's equator passes through the center of the Sun. In other words,
 * the Sun's declination is zero at both equinoxes.
 * The March equinox defines the beginning of spring in the northern hemisphere
 * and the beginning of autumn in the southern hemisphere.
 * The September equinox defines the beginning of autumn in the northern hemisphere
 * and the beginning of spring in the southern hemisphere.
 *
 * The solstices are the moments twice each year when one of the Earth's poles
 * is most tilted toward the Sun. More precisely, the Sun's declination reaches
 * its minimum value at the December solstice, which defines the beginning of
 * winter in the northern hemisphere and the beginning of summer in the southern
 * hemisphere. The Sun's declination reaches its maximum value at the June solstice,
 * which defines the beginning of summer in the northern hemisphere and the beginning
 * of winter in the southern hemisphere.
 *
 * @param year
 * The calendar year number for which to calculate equinoxes and solstices.
 * The value may be any integer, but only the years 1800 through 2100 have been
 * validated for accuracy: unit testing against data from the
 * United States Naval Observatory confirms that all equinoxes and solstices
 * for that range of years are within 2 minutes of the correct time.
 *
 * @return
 * A [SeasonsInfo] object that contains four [Time] values:
 * the March and September equinoxes and the June and December solstices.
 */
fun seasons(year: Int) =
    SeasonsInfo(
        findSeasonChange(  0.0, year,  3, 10),
        findSeasonChange( 90.0, year,  6, 10),
        findSeasonChange(180.0, year,  9, 10),
        findSeasonChange(270.0, year, 12, 10)
    )

private fun findSeasonChange(targetLon: Double, year: Int, month: Int, day: Int): Time {
    val startTime = Time(year, month, day, 0, 0, 0.0)
    return searchSunLongitude(targetLon, startTime, 20.0) ?:
        throw InternalError("Cannot find solution for Sun longitude $targetLon for year $year")
}

/**
 * Returns one body's ecliptic longitude with respect to another, as seen from the Earth.
 *
 * This function determines where one body appears around the ecliptic plane
 * (the plane of the Earth's orbit around the Sun) as seen from the Earth,
 * relative to the another body's apparent position.
 * The function returns an angle in the half-open range [0, 360) degrees.
 * The value is the ecliptic longitude of `body1` relative to the ecliptic
 * longitude of `body2`.
 *
 * The angle is 0 when the two bodies are at the same ecliptic longitude
 * as seen from the Earth. The angle increases in the prograde direction
 * (the direction that the planets orbit the Sun and the Moon orbits the Earth).
 *
 * When the angle is 180 degrees, it means the two bodies appear on opposite sides
 * of the sky for an Earthly observer.
 *
 * Neither `body1` nor `body2` is allowed to be [Body.Earth].
 * If this happens, the function throws an exception.
 *
 * @param body1
 * The first body, whose longitude is to be found relative to the second body.
 *
 * @param body2
 * The second body, relative to which the longitude of the first body is to be found.
 *
 * @param time
 * The date and time of the observation.
 *
 * @return
 * An angle in the range [0, 360), expressed in degrees.
 */
fun pairLongitude(body1: Body, body2: Body, time: Time): Double {
    if (body1 == Body.Earth || body2 == Body.Earth)
        throw EarthNotAllowedException()

    val vector1 = geoVector(body1, time, Aberration.None)
    val eclip1 = equatorialToEcliptic(vector1)

    val vector2 = geoVector(body2, time, Aberration.None)
    val eclip2 = equatorialToEcliptic(vector2)

    return normalizeLongitude(eclip1.elon - eclip2.elon)
}

/**
 * Returns the Moon's phase as an angle from 0 to 360 degrees.
 *
 * This function determines the phase of the Moon using its apparent
 * ecliptic longitude relative to the Sun, as seen from the center of the Earth.
 * Certain values of the angle have conventional definitions:
 *
 * - 0 = new moon
 * - 90 = first quarter
 * - 180 = full moon
 * - 270 = third quarter
 *
 * @param time
 * The date and time of the observation.
 *
 * @return
 * The angle as described above, a value in the range 0..360 degrees.
 */
fun moonPhase(time: Time): Double =
    pairLongitude(Body.Moon, Body.Sun, time)

/**
 * Searches for the time that the Moon reaches a specified phase.
 *
 * Lunar phases are conventionally defined in terms of the Moon's geocentric ecliptic
 * longitude with respect to the Sun's geocentric ecliptic longitude.
 * When the Moon and the Sun have the same longitude, that is defined as a new moon.
 * When their longitudes are 180 degrees apart, that is defined as a full moon.
 *
 * This function searches for any value of the lunar phase expressed as an
 * angle in degrees in the range [0, 360).
 *
 * If you want to iterate through lunar quarters (new moon, first quarter, full moon, third quarter)
 * it is much easier to call the functions [searchMoonQuarter] and [nextMoonQuarter].
 * This function is useful for finding general phase angles outside those four quarters.
 *
 * @param targetLon
 * The difference in geocentric longitude between the Sun and Moon
 * that specifies the lunar phase being sought. This can be any value
 * in the range [0, 360).  Certain values have conventional names:
 * 0 = new moon, 90 = first quarter, 180 = full moon, 270 = third quarter.
 *
 * @param startTime
 * The beginning of the time window in which to search for the Moon reaching the specified phase.
 *
 * @param limitDays
 * The number of days away from `startTime` that limits the time window for the search.
 * If the value is negative, the search is performed into the past from `startTime`.
 * Otherwise, the search is performed into the future from `startTime`.
 *
 * @return
 * If successful, returns the date and time the moon reaches the phase specified by
 * `targetlon`. This function will return `null` if the phase does not
 * occur within `limitDays` of `startTime`; that is, if the search window is too small.
 */
fun searchMoonPhase(targetLon: Double, startTime: Time, limitDays: Double): Time? {
    // To avoid discontinuities in the moonOffset function causing problems,
    // we need to approximate when that function will next return 0.
    // We probe it with the start time and take advantage of the fact
    // that every lunar phase repeats roughly every 29.5 days.
    // There is a surprising uncertainty in the quarter timing,
    // due to the eccentricity of the moon's orbit.
    // I have seen more than 0.9 days away from the simple prediction.
    // To be safe, we take the predicted time of the event and search
    // +/-1.5 days around it (a 3-day wide window).
    val uncertainty = 1.5
    val moonOffset = SearchContext { time -> longitudeOffset(moonPhase(time) - targetLon) }
    var estDt: Double
    var dt1: Double
    var dt2: Double
    var ya = moonOffset.eval(startTime)
    if (limitDays < 0.0) {
        // Search backward in time.
        if (ya < 0.0) ya += 360.0
        estDt = -(MEAN_SYNODIC_MONTH * ya) / 360.0
        dt2 = estDt + uncertainty
        if (dt2 < limitDays)
            return null    // not possible for moon phase to occur within specified window (too short)
        dt1 = max(limitDays, estDt - uncertainty)
    } else {
        // Search forward in time
        if (ya > 0.0) ya -= 360.0
        estDt = -(MEAN_SYNODIC_MONTH * ya) / 360.0
        dt1 = estDt - uncertainty
        if (dt1 > limitDays)
            return null    // not possible for moon phase to occur within specified window (too short)
        dt2 = min(limitDays, estDt + uncertainty)
    }
    val t1 = startTime.addDays(dt1)
    val t2 = startTime.addDays(dt2)
    return search(t1, t2, 0.1, moonOffset)
}

/**
 * Finds the first lunar quarter after the specified date and time.
 * A lunar quarter is one of the following four lunar phase events:
 * new moon, first quarter, full moon, third quarter.
 * This function finds the lunar quarter that happens soonest
 * after the specified date and time.
 *
 * To continue iterating through consecutive lunar quarters, call this function once,
 * followed by calls to #NextMoonQuarter as many times as desired.
 *
 * See [moonQuartersAfter] for convenient iteration of consecutive quarter phases.
 *
 * @param startTime
 * The date and time at which to start the search.
 *
 * @return
 * A [MoonQuarterInfo] object reporting the next quarter phase and the time it will occur.
 */
fun searchMoonQuarter(startTime: Time): MoonQuarterInfo {
    val currentPhaseAngle = moonPhase(startTime)
    val quarter: Int = (1 + floor(currentPhaseAngle / 90.0).toInt()) % 4
    val quarterTime = searchMoonPhase(90.0 * quarter, startTime, 10.0) ?:
        throw InternalError("Unable to find moon quarter $quarter for startTime=$startTime")
    return MoonQuarterInfo(quarter, quarterTime)
}

/**
 * Continues searching for lunar quarters from a previous search.
 *
 * After calling [searchMoonQuarter], this function can be called
 * one or more times to continue finding consecutive lunar quarters.
 * This function finds the next consecutive moon quarter event after
 * the one passed in as the parameter `mq`.
 *
 * See [moonQuartersAfter] for convenient iteration of consecutive quarter phases.
 *
 * @param mq
 * The previous moon quarter found by a call to [searchMoonQuarter] or `nextMoonQuarter`.
 *
 * @return
 * The moon quarter that occurs next in time after the one passed in `mq`.
 */
fun nextMoonQuarter(mq: MoonQuarterInfo): MoonQuarterInfo {
    // Skip 6 days past the previous found moon quarter to find the next one.
    // This is less than the minimum possible increment.
    // So far I have seen the interval well contained by the range (6.5, 8.3) days.
    val time = mq.time.addDays(6.0)
    val nextMoonQuarter = searchMoonQuarter(time)
    // Verify that we found the expected moon quarter.
    val expected = (1 + mq.quarter) % 4
    if (nextMoonQuarter.quarter != expected)
        throw InternalError("Expected to find next quarter $expected, but found ${nextMoonQuarter.quarter}")
    return nextMoonQuarter
}

/**
 * Enumerates a series of consecutive moon quarter phase events.
 *
 * This function enables iteration through an unlimited number
 * of consecutive lunar quarter phases starting at a given time.
 * This is a convenience wrapper around [searchMoonQuarter] and [nextMoonQuarter].
 *
 * @param startTime
 * The date and time for starting the search for a series of quarter phases.
 */
fun moonQuartersAfter(startTime: Time): Sequence<MoonQuarterInfo> =
    generateSequence(searchMoonQuarter(startTime)) { nextMoonQuarter(it) }


/**
 * Searches for the time when the center of a body reaches a specified hour angle as seen by an observer on the Earth.
 *
 * The *hour angle* of a celestial body indicates its position in the sky with respect
 * to the Earth's rotation. The hour angle depends on the location of the observer on the Earth.
 * The hour angle is 0 when the body's center reaches its highest angle above the horizon in a given day.
 * The hour angle increases by 1 unit for every sidereal hour that passes after that point, up
 * to 24 sidereal hours when it reaches the highest point again. So the hour angle indicates
 * the number of hours that have passed since the most recent time that the body has culminated,
 * or reached its highest point.
 *
 * This function searches for the next time a celestial body reaches the given hour angle
 * after the date and time specified by `startTime`.
 * To find when a body culminates, pass 0 for `hourAngle`.
 * To find when a body reaches its lowest point in the sky, pass 12 for `hourAngle`.
 *
 * Note that, especially close to the Earth's poles, a body as seen on a given day
 * may always be above the horizon or always below the horizon, so the caller cannot
 * assume that a culminating object is visible nor that an object is below the horizon
 * at its minimum altitude.
 *
 * On success, the function reports the date and time, along with the horizontal coordinates
 * of the body at that time, as seen by the given observer.
 *
 * @param body
 * The Sun, Moon, any planet other than the Earth,
 * or a user-defined star that was created by a call to [defineStar].
 *
 * @param observer
 * A location on or near the surface of the Earth where the observer is located.
 *
 * @param hourAngle
 * An hour angle value in the range [0, 24) indicating the number of sidereal hours after the
 * body's most recent culmination.
 *
 * @param startTime
 * The date and time at which to start the search.
 *
 * @param direction
 * The direction in time to perform the search: a positive value
 * searches forward in time, a negative value searches backward in time.
 * The function throws an exception if `direction` is zero.
 *
 * @return
 * The time when the body reaches the hour angle, and the horizontal coordinates of the body at that time.
 */
fun searchHourAngle(
    body: Body,
    observer: Observer,
    hourAngle: Double,
    startTime: Time,
    direction: Int = +1
): HourAngleInfo {
    if (body == Body.Earth)
        throw EarthNotAllowedException()

    if (hourAngle < 0.0 || hourAngle >= 24.0)
        throw IllegalArgumentException("hourAngle=$hourAngle is out of the allowed range [0, 24).")

    if (direction == 0)
        throw IllegalArgumentException("direction must be a positive or negative integer, not zero.")

    var time = startTime
    var iter = 0
    while (true) {
        ++iter

        // Calculate Greenwich Apparent Sidereal Time (GAST) at the given time.
        val gast = siderealTime(time)

        // Obtain equatorial coordinates of date for the body.
        val ofdate = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)

        // Calculate the adjustment needed in sidereal time
        // to bring the hour angle to the desired value.
        var deltaSiderealHours = ((hourAngle + ofdate.ra - observer.longitude/15.0) - gast) % 24.0
        if (iter == 1) {
            // On the first iteration, always search in the requested time direction.
            if (direction > 0) {
                // Search forward in time.
                if (deltaSiderealHours < 0.0)
                    deltaSiderealHours += 24.0
            } else {
                // Search backward in time.
                if (deltaSiderealHours > 0.0)
                    deltaSiderealHours -= 24.0
            }
        } else {
            // On subsequent iterations, we make the smallest possible adjustment,
            // either forward or backward in time.
            if (deltaSiderealHours < -12.0)
                deltaSiderealHours += 24.0
            else if (deltaSiderealHours > +12.0)
                deltaSiderealHours -= 24.0
        }

        // If the error is tolerable (less than 0.1 seconds), the search has succeeded.
        if (deltaSiderealHours.absoluteValue * 3600.0 < 0.1) {
            val hor = horizon(time, observer, ofdate.ra, ofdate.dec, Refraction.Normal)
            return HourAngleInfo(time, hor)
        }

        // We need to loop another time to get more accuracy.
        // Update the terrestrial time (in solar days) by adjusting sidereal time.
        time = time.addDays((deltaSiderealHours / 24.0) * SOLAR_DAYS_PER_SIDEREAL_DAY)
    }
}


/**
 * Finds the hour angle of a body for a given observer and time.
 *
 * The *hour angle* of a celestial body indicates its position in the sky with respect
 * to the Earth's rotation. The hour angle depends on the location of the observer on the Earth.
 * The hour angle is 0 when the body's center reaches its highest angle above the horizon in a given day.
 * The hour angle increases by 1 unit for every sidereal hour that passes after that point, up
 * to 24 sidereal hours when it reaches the highest point again. So the hour angle indicates
 * the number of hours that have passed since the most recent time that the body has culminated,
 * or reached its highest point.
 *
 * @param body
 * The body whose observed hour angle is to be found.
 *
 * @param time
 * The time of the observation.
 *
 * @param observer
 * The geographic location where the observation takes place.
 *
 * @return
 * The real-valued hour angle of the body in the half-open range [0, 24).
 */
fun hourAngle(body: Body, time: Time, observer: Observer): Double {
    val gast = siderealTime(time)
    val ofdate = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
    var hourAngle = (observer.longitude/15 + gast - ofdate.ra) % 24.0
    return if (hourAngle < 0.0) hourAngle + 24.0 else hourAngle
}


/**
 * Calculates U.S. Standard Atmosphere (1976) variables as a function of elevation.
 *
 * This function calculates idealized values of pressure, temperature, and density
 * using the U.S. Standard Atmosphere (1976) model.
 * 1. COESA, U.S. Standard Atmosphere, 1976, U.S. Government Printing Office, Washington, DC, 1976.
 * 2. Jursa, A. S., Ed., Handbook of Geophysics and the Space Environment, Air Force Geophysics Laboratory, 1985.
 * See:
 * https://hbcp.chemnetbase.com/faces/documents/14_12/14_12_0001.xhtml
 * https://ntrs.nasa.gov/api/citations/19770009539/downloads/19770009539.pdf
 * https://www.ngdc.noaa.gov/stp/space-weather/online-publications/miscellaneous/us-standard-atmosphere-1976/us-standard-atmosphere_st76-1562_noaa.pdf
 *
 * @param elevationMeters
 * The elevation above sea level at which to calculate atmospheric variables.
 * Must be in the range -500 to +100000, or an exception will occur.
 */
fun atmosphere(elevationMeters: Double): AtmosphereInfo {
    val P0 = 101325.0      // pressure at sea level [pascals]
    val T0 = 288.15        // temperature at sea level [kelvins]
    val T1 = 216.65        // temperature between 20 km and 32 km [kelvins]

    if (!elevationMeters.isFinite() || elevationMeters < -500.0 || elevationMeters > 100000.0)
        throw IllegalArgumentException("elevationMeters value is not valid: $elevationMeters")

    var temperature: Double
    var pressure: Double
    if (elevationMeters <= 11000.0) {
        temperature = T0 - 0.0065*elevationMeters
        pressure = P0 * (T0 / temperature).pow(-5.25577)
    } else if (elevationMeters <= 20000.0) {
        temperature = T1
        pressure = 22632.0 * exp(-0.00015768832 * (elevationMeters - 11000.0))
    } else {
        temperature = T1 + 0.001*(elevationMeters - 20000.0)
        pressure = 5474.87 * (T1 / temperature).pow(34.16319)
    }
    // The density is calculated relative to the sea level value.
    // Using the ideal gas law PV=nRT, we deduce that density is proportional to P/T.
    val density = (pressure / temperature) / (P0 / T0)
    return AtmosphereInfo(pressure, temperature, density)
}


private fun horizonDipAngle(observer: Observer, metersAboveGround: Double): Double {
    // Calculate the effective radius of the Earth at ground level below the observer.
    // Correct for the Earth's oblateness.
    val phi = observer.latitude.degreesToRadians()
    val sinphi = sin(phi)
    val cosphi = cos(phi)
    val c = 1.0 / hypot(cosphi, sinphi*EARTH_FLATTENING)
    val s = c * (EARTH_FLATTENING * EARTH_FLATTENING)
    val ht_km = (observer.height - metersAboveGround) / 1000.0     // height of ground above sea level
    val ach = EARTH_EQUATORIAL_RADIUS_KM*c + ht_km
    val ash = EARTH_EQUATORIAL_RADIUS_KM*s + ht_km
    val radius_m = 1000.0 * hypot(ach*cosphi, ash*sinphi)

    // Correct refraction of a ray of light traveling tangent to the Earth's surface.
    // Based on: https://www.largeformatphotography.info/sunmooncalc/SMCalc.js
    // which in turn derives from:
    // Sweer, John. 1938.  The Path of a Ray of Light Tangent to the Surface of the Earth.
    // Journal of the Optical Society of America 28 (September):327-329.

    // k = refraction index
    val k = 0.175 * (1.0 - (6.5e-3/283.15)*(observer.height - (2.0/3.0)*metersAboveGround)).pow(3.256)

    // Calculate how far below the observer's horizontal plane the observed horizon dips.
    return -(sqrt(2*(1 - k)*metersAboveGround / radius_m) / (1 - k)).radiansToDegrees()
}


private class AscentInfo(
    public val tx: Time,
    public val ty: Time,
    public val ax: Double,
    public val ay: Double
)


private fun findAscent(
    depth: Int,
    context: SearchContext_Altitude,
    maxDerivAlt: Double,
    t1: Time,
    t2: Time,
    a1: Double,
    a2: Double
): AscentInfo? {
    // See if we can find any time interval where the altitude-diff function
    // rises from non-positive to positive.
    if (a1 < 0.0 && a2 >= 0.0) {
        // Trivial success case: the endpoints already rise through zero.
        return AscentInfo(t1, t2, a1, a2)
    }

    if (a1 >= 0.0 && a2 < 0.0) {
        // Trivial failure case: Assume Nyquist condition prevents an ascent.
        return null
    }

    if (depth > 17) {
        // Safety valve: do not allow unlimited recursion.
        // This should never happen if the rest of the logic is working correctly,
        // so fail the whole search if it does happen. It's a bug!
        throw InternalError("Excessive recursion in rise/set ascent search.")
    }

    // Both altitudes are on the same side of zero: both are negative, or both are non-negative.
    // There could be a convex "hill" or a concave "valley" that passes through zero.
    // In polar regions sometimes there is a rise/set or set/rise pair within minutes of each other.
    // For example, the Moon can be below the horizon, then the very top of it becomes
    // visible (moonrise) for a few minutes, then it moves sideways and down below
    // the horizon again (moonset). We want to catch these cases.
    // However, for efficiency and practicality concerns, because the rise/set search itself
    // has a 0.1 second threshold, we do not worry about rise/set pairs that are less than
    // one second apart. These are marginal cases that are rendered highly uncertain
    // anyway, due to unpredictable atmospheric refraction conditions (air temperature and pressure).

    val dt = t2.ut - t1.ut
    if (dt * SECONDS_PER_DAY < 1.0)
        return null

    // Is it possible to reach zero from the altitude that is closer to zero?
    val da = min(abs(a1), abs(a2))

    // Without loss of generality, assume |a1| <= |a2|.
    // (Reverse the argument in the case |a2| < |a1|.)
    // Imagine you have to "drive" from a1 to 0, then back to a2.
    // You can't go faster than max_deriv_alt. If you can't reach 0 in half the time,
    // you certainly don't have time to reach 0, turn around, and still make your way
    // back up to a2 (which is at least as far from 0 than a1 is) in the time interval dt.
    // Therefore, the time threshold is half the time interval, or dt/2.
    if (da > maxDerivAlt*(dt / 2)) {
        // Prune: the altitude cannot change fast enough to reach zero.
        return null
    }

    // Bisect the time interval and evaluate the altitude at the midpoint.
    val tmid = Time((t1.ut + t2.ut)/2)
    val amid = context.eval(tmid)

    // Use recursion to home in on the first ascending point we can find
    return (
        findAscent(1+depth, context, maxDerivAlt, t1, tmid, a1, amid) ?:
        findAscent(1+depth, context, maxDerivAlt, tmid, t2, amid, a2)
    )
}


private fun maxAltitudeSlope(body: Body, latitude: Double): Double {
    // Calculate the maximum possible rate that this body's altitude
    // could change [degrees/day] as seen by this observer.
    // First use experimentally determined extreme bounds for this body
    // of how much topocentric RA and DEC can ever change per rate of time.
    // We need minimum possible d(RA)/dt, and maximum possible magnitude of d(DEC)/dt.
    // Conservatively, we round d(RA)/dt down, d(DEC)/dt up.
    // Then calculate the resulting maximum possible altitude change rate.

    if (!latitude.isFinite() || latitude < -90.0 || latitude > +90.0)
        throw IllegalArgumentException("Invalid geographic latitude: $latitude")

    val derivRa: Double
    val derivDec: Double

    when (body) {
        Body.Moon -> {
            derivRa  = +4.5
            derivDec = +8.2
        }

        Body.Sun -> {
            derivRa  = +0.8
            derivDec = +0.5
        }

        Body.Mercury -> {
            derivRa  = -1.6
            derivDec = +1.0
        }

        Body.Venus -> {
            derivRa  = -0.8
            derivDec = +0.6
        }

        Body.Mars -> {
            derivRa  = -0.5
            derivDec = +0.4
        }

        Body.Jupiter, Body.Saturn, Body.Uranus, Body.Neptune, Body.Pluto -> {
            derivRa  = -0.2
            derivDec = +0.2
        }

        Body.Star1, Body.Star2, Body.Star3, Body.Star4, Body.Star5, Body.Star6, Body.Star7, Body.Star8 -> {
            // The minimum allowed heliocentric distance of a user-defined star
            // is one light-year. This can cause a tiny amount of parallax (about 0.001 degrees).
            // Also, including stellar aberration (22 arcsec = 0.006 degrees), we provide a
            // generous safety buffer of 0.008 degrees.
            derivRa  = -0.008
            derivDec = +0.008
        }

        Body.Earth -> throw EarthNotAllowedException()
        else -> throw InvalidBodyException(body)
    }

    val latrad = latitude.degreesToRadians()
    return abs((360.0 / SOLAR_DAYS_PER_SIDEREAL_DAY) - derivRa)*cos(latrad) + abs(derivDec*sin(latrad))
}


private val RISE_SET_DT = 0.42      // 10.08 hours: Nyquist-safe for 22-hour period.


private class SearchContext_Altitude(
    private val body: Body,
    private val direction: Direction,
    private val observer: Observer,
    private val bodyRadiusAu: Double,
    private val targetAltitude: Double
) : SearchContext {
    public override fun eval(time: Time): Double {
        val ofdate: Equatorial = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val hor: Topocentric = horizon(time, observer, ofdate.ra, ofdate.dec, Refraction.None)
        val altitude = hor.altitude + asin(bodyRadiusAu / ofdate.dist).radiansToDegrees()
        return direction.sign*(altitude - targetAltitude)
    }
}


private fun internalSearchAltitude(
    body: Body,
    observer: Observer,
    direction: Direction,
    startTime: Time,
    limitDays: Double,
    bodyRadiusAu: Double,
    targetAltitude: Double
): Time? {
    if (!targetAltitude.isFinite() || targetAltitude < -90.0 || targetAltitude > +90.0)
        throw IllegalArgumentException("Target altitude is not valid: $targetAltitude")

    val maxDerivAlt = maxAltitudeSlope(body, observer.latitude)
    val context = SearchContext_Altitude(body, direction, observer, bodyRadiusAu, targetAltitude)

    // We allow searching forward or backward in time.
    // But we want to keep t1 < t2, so we need a few if/else statements.
    var t1: Time = startTime
    var t2: Time = t1
    var a1: Double = context.eval(t1)
    var a2: Double = a1

    while (true) {
        if (limitDays < 0.0) {
            t1 = t2.addDays(-RISE_SET_DT)
            a1 = context.eval(t1)
        } else {
            t2 = t1.addDays(+RISE_SET_DT)
            a2 = context.eval(t2)
        }

        val ascent = findAscent(0, context, maxDerivAlt, t1, t2, a1, a2)
        if (ascent != null) {
            // We found a time interval [t1, t2] that contains an alt-diff
            // rising from negative a1 to non-negative a2.
            // Search for the time where the root occurs.
            val time = search(ascent.tx, ascent.ty, 0.1, context)
            if (time != null) {
                // Now that we have a solution, we have to check whether it goes outside the time bounds.
                if (limitDays < 0.0) {
                    if (time.ut < startTime.ut + limitDays)
                        return null
                } else {
                    if (time.ut > startTime.ut + limitDays)
                        return null
                }
                return time    // success!
            }

            // The search should have succeeded. Something is wrong with the ascent finder!
            throw InternalError("Rise/set search failed after finding ascent: t1=$t1, t2=$t2, a1=$a1, a2=$a2")
        }

        // There is no ascent in this interval, so keep searching.
        if (limitDays < 0.0) {
            if (t1.ut < startTime.ut + limitDays)
                return null
            t2 = t1
            a2 = a1
        } else {
            if (t2.ut > startTime.ut + limitDays)
                return null
            t1 = t2
            a1 = a2
        }
    }
}


/**
 * Searches for the next time a celestial body rises or sets as seen by an observer on the Earth.
 *
 * This function finds the next rise or set time of the Sun, Moon, or planet other than the Earth.
 * Rise time is when the body first starts to be visible above the horizon.
 * For example, sunrise is the moment that the top of the Sun first appears to peek above the horizon.
 * Set time is the moment when the body appears to vanish below the horizon.
 * Therefore, this function adjusts for the apparent angular radius of the observed body
 * (significant only for the Sun and Moon).
 *
 * This function corrects for a typical value of atmospheric refraction, which causes celestial
 * bodies to appear higher above the horizon than they would if the Earth had no atmosphere.
 * Astronomy Engine uses a correction of 34 arcminutes. Real-world refraction varies based
 * on air temperature, pressure, and humidity; such weather-based conditions are outside
 * the scope of Astronomy Engine.
 *
 * Note that rise or set may not occur in every 24 hour period.
 * For example, near the Earth's poles, there are long periods of time where
 * the Sun stays below the horizon, never rising.
 * Also, it is possible for the Moon to rise just before midnight but not set during the subsequent 24-hour day.
 * This is because the Moon sets nearly an hour later each day due to orbiting the Earth a
 * significant amount during each rotation of the Earth.
 * Therefore callers must not assume that the function will always succeed.
 *
 * @param body
 * The Sun, Moon, any planet other than the Earth,
 * or a user-defined star that was created by a call to [defineStar].
 *
 * @param observer
 * The location where observation takes place.
 *
 * @param direction
 * Either [Direction.Rise] to find a rise time or [Direction.Set] to find a set time.
 *
 * @param startTime
 * The date and time at which to start the search.
 *
 * @param limitDays
 * Limits how many days to search for a rise or set time, and defines
 * the direction in time to search. When `limitDays` is positive, the
 * search is performed into the future, after `startTime`.
 * When negative, the search is performed into the past, before `startTime`.
 * To limit a rise or set time to the same day, you can use a value of 1 day.
 * In cases where you want to find the next rise or set time no matter how far
 * in the future (for example, for an observer near the south pole), you can
 * pass in a larger value like 365.
 *
 * @param metersAboveGround
 * Usually the observer is located at ground level. Then this parameter
 * should be zero. But if the observer is significantly higher than ground
 * level, for example in an airplane, this parameter should be a positive
 * number indicating how far above the ground the observer is.
 * An exception occurs if `metersAboveGround` is negative.
 *
 * @return
 * On success, returns the date and time of the rise or set time as requested.
 * If the function returns `null`, it means the rise or set event does not occur
 * within `limitDays` days of `startTime`. This is a normal condition,
 * not an error.
 */
@JvmOverloads
fun searchRiseSet(
    body: Body,
    observer: Observer,
    direction: Direction,
    startTime: Time,
    limitDays: Double,
    metersAboveGround: Double = 0.0
): Time? {
    if (!metersAboveGround.isFinite() || metersAboveGround < 0.0)
        throw IllegalArgumentException("metersAboveGround = $metersAboveGround is not valid.")

    val bodyRadiusAu = when (body) {
        Body.Sun -> SUN_RADIUS_AU
        Body.Moon -> MOON_EQUATORIAL_RADIUS_AU
        else -> 0.0
    }

    // Calculate atmospheric density at ground level.
    val atmos = atmosphere(observer.height - metersAboveGround)

    // Calculate the apparent angular dip of the horizon.
    val dip = horizonDipAngle(observer, metersAboveGround)

    // Correct refraction for objects near the horizon, using atmospheric density at the ground.
    val altitude = dip - (REFRACTION_NEAR_HORIZON * atmos.density)

    return internalSearchAltitude(body, observer, direction, startTime, limitDays, bodyRadiusAu, altitude)
}


/**
 * Finds the next time the center of a body reaches a given altitude.
 *
 * Finds when the center of the given body ascends or descends through a given
 * altitude angle, as seen by an observer at the specified location on the Earth.
 * By using the appropriate combination of `direction` and `altitude` parameters,
 * this function can be used to find when civil, nautical, or astronomical twilight
 * begins (dawn) or ends (dusk).
 *
 * Civil dawn begins before sunrise when the Sun ascends through 6 degrees below
 * the horizon. To find civil dawn, pass `Direction.Rise` for `direction` and -6 for `altitude`.
 *
 * Civil dusk ends after sunset when the Sun descends through 6 degrees below the horizon.
 * To find civil dusk, pass `Direction.Set` for `direction` and -6 for `altitude`.
 *
 * Nautical twilight is similar to civil twilight, only the `altitude` value should be -12 degrees.
 *
 * Astronomical twilight uses -18 degrees as the `altitude` value.
 *
 * By convention for twilight time calculations, the altitude is not corrected for
 * atmospheric refraction. This is because the target altitudes are below the horizon,
 * and refraction is not directly observable.
 *
 * `searchAltitude` is not intended to find rise/set times of a body for two reasons:
 * (1) Rise/set times of the Sun or Moon are defined by their topmost visible portion, not their centers.
 * (2) Rise/set times are affected significantly by atmospheric refraction.
 * Therefore, it is better to use [searchRiseSet] to find rise/set times, which
 * corrects for both of these considerations.
 *
 * `searchAltitude` will not work reliably for altitudes at or near the body's
 * maximum or minimum altitudes. To find the time a body reaches minimum or maximum altitude
 * angles, use [searchHourAngle].
 *
 * @param body
 * The Sun, Moon, any planet other than the Earth,
 * or a user-defined star that was created by a call to [defineStar].
 *
 * @param observer
 * The location where observation takes place.
 *
 * @param direction
 * Either [Direction.Rise] to find an ascending altitude event or [Direction.Set] to find a descending altitude event.
 *
 * @param startTime
 * The date and time at which to start the search.
 *
 * @param limitDays
 * Limits how many days to search for the body reaching the altitude angle,
 * and defines the direction in time to search. When `limitDays` is positive, the
 * search is performed into the future, after `startTime`.
 * When negative, the search is performed into the past, before `startTime`.
 * To limit the search to the same day, you can use a value of 1 day.
 * In cases where you want to find the altitude event no matter how far
 * in the future (for example, for an observer near the south pole), you can
 * pass in a larger value like 365.
 *
 * @param altitude
 * The desired altitude angle of the body's center above (positive) or below (negative)
 * the observer's local horizon, expressed in degrees. Must be in the range [-90, +90].
 *
 * @return
 * The date and time of the altitude event, or `null` if no such event occurs within the specified time window.
 */
fun searchAltitude(
    body: Body,
    observer: Observer,
    direction: Direction,
    startTime: Time,
    limitDays: Double,
    altitude: Double): Time? =
        internalSearchAltitude(body, observer, direction, startTime, limitDays, 0.0, altitude)

/**
 * Returns the angle between the given body and the Sun, as seen from the Earth.
 *
 * This function calculates the angular separation between the given body and the Sun,
 * as seen from the center of the Earth. This angle is helpful for determining how
 * easy it is to see the body away from the glare of the Sun.
 *
 * @param body
 * The celestial body whose angle from the Sun is to be measured.
 * Not allowed to be [Body.Earth].
 *
 * @param time
 * The time at which the observation is made.
 *
 * @return
 * The angle in degrees between the Sun and the specified body as seen from the center of the Earth.
 */
fun angleFromSun(body: Body, time: Time): Double {
    if (body == Body.Earth)
        throw EarthNotAllowedException()
    val sv = geoVector(Body.Sun, time, Aberration.Corrected)
    val bv = geoVector(body, time, Aberration.Corrected)
    return sv.angleWith(bv)
}


internal fun moonDistance(time: Time): Double = MoonContext(time).calcMoon().dist

internal fun moonRadialSpeed(time: Time): Double {
    val dt = 0.001
    val t1 = time.addDays(-dt/2.0)
    val t2 = time.addDays(+dt/2.0)
    val dist1 = moonDistance(t1)
    val dist2 = moonDistance(t2)
    return (dist2 - dist1) / dt
}


/**
 * Finds the date and time of the Moon's perigee or apogee.
 *
 * Given a date and time to start the search in `startTime`, this function finds the
 * next date and time that the center of the Moon reaches the closest or farthest point
 * in its orbit with respect to the center of the Earth, whichever comes first
 * after `startTime`.
 *
 * The closest point is called *perigee* and the farthest point is called *apogee*.
 * The word *apsis* refers to either event.
 *
 * To iterate through consecutive alternating perigee and apogee events, call `searchLunarApsis`
 * once, then use the return value to call [nextLunarApsis]. After that,
 * keep feeding the previous return value from `Astronomy.NextLunarApsis` into another
 * call of `Astronomy.NextLunarApsis` as many times as desired.
 *
 * See [lunarApsidesAfter] for convenient iteration of consecutive lunar apsides.
 *
 * @param startTime
 * The date and time at which to start searching for the next perigee or apogee.
 */
fun searchLunarApsis(startTime: Time): ApsisInfo {
    val increment = 0.5     // number of days to skip in each iteration

    // Check the rate of change of the distance dr/dt at the start time.
    // If it is positive, the Moon is currently getting farther away,
    // so start looking for apogee.
    // Conversely, if dr/dt < 0, start looking for perigee.
    // Either way, the polarity of the slope will change, so the product will be negative.
    // Handle the crazy corner case of exactly touching zero by checking for m1*m2 <= 0.

    var t1 = startTime
    var m1 = moonRadialSpeed(t1)
    var iter = 0
    while (iter * increment < 2.0 * MEAN_SYNODIC_MONTH) {
        val t2 = t1.addDays(increment)
        val m2 = moonRadialSpeed(t2)
        if (m1 * m2 <= 0.0) {
            // There is a change of slope polarity within the time range [t1, t2].
            // Therefore this time range contains an apsis.
            // Figure out whether it is perigee or apogee.
            val apsisTime: Time
            val kind: ApsisKind
            val direction: Int
            if (m1 < 0.0 || m2 > 0.0) {
                // We found a minimum distance event: perigee.
                // Search the time range for the time when the slope goes from negative to positive.
                direction = +1
                kind = ApsisKind.Pericenter
            } else if (m1 > 0.0 || m2 < 0.0) {
                // We found a maximum distance event: apogee.
                // Search the time range for the time when the slope goes from positive to negative.
                direction = -1
                kind = ApsisKind.Apocenter
            } else {
                throw InternalError("Both slopes are zero in SearchLunarApsis.")
            }

            apsisTime = search(t1, t2, 1.0) { time -> direction * moonRadialSpeed(time) } ?:
                throw InternalError("Failed to find slope transition in lunar apsis search.")

            val distanceAu = moonDistance(apsisTime)
            return ApsisInfo(apsisTime, kind, distanceAu)
        }
        // We have not yet found a slope polarity change. Keep searching.
        t1 = t2
        m1 = m2
        ++iter
    }

    // It should not be possible to fail to find an apsis within 2 synodic months.
    throw InternalError("Should have found lunar apsis within 2 synodic months.")
}


/**
 * Finds the next lunar perigee or apogee event in a series.
 *
 * Finds the next consecutive time the Moon is closest or farthest from the Earth in its orbit.
 *
 * See [searchLunarApsis] for more details.
 * See [lunarApsidesAfter] for convenient iteration of consecutive lunar apsides.
 *
 * @param apsis
 * An [ApsisInfo] value obtained from a call to [searchLunarApsis] or [nextLunarApsis].
 */
fun nextLunarApsis(apsis: ApsisInfo): ApsisInfo {
    val time = apsis.time.addDays(11.0)
    val next = searchLunarApsis(time)
    if (next.kind == apsis.kind)
        throw InternalError("Found ${next.kind} for two consecutive apsis events: ${apsis.time} and ${next.time}.")
    return next
}


/**
 * Enumerates a series of consecutive lunar apsides that occur after a given time.
 *
 * This function enables iteration through an unlimited number
 * of consecutive lunar perigees/apogees starting at a given time.
 * This is a convenience wrapper around [searchLunarApsis] and [nextLunarApsis].
 *
 * @param startTime
 * The date and time for starting the search for a series of lunar apsides.
 */
fun lunarApsidesAfter(startTime: Time): Sequence<ApsisInfo> =
    generateSequence(searchLunarApsis(startTime)) { nextLunarApsis(it) }


/**
 * Determines visibility of a celestial body relative to the Sun, as seen from the Earth.
 *
 * This function returns an [ElongationInfo] object, which provides the following
 * information about the given celestial body at the given time:
 *
 * - `visibility` is an enumerated type that specifies whether the body is more easily seen
 *    in the morning before sunrise, or in the evening after sunset.
 *
 * - `elongation` is the angle in degrees between two vectors: one from the center of the Earth to the
 *    center of the Sun, the other from the center of the Earth to the center of the specified body.
 *    This angle indicates how far away the body is from the glare of the Sun.
 *    The elongation angle is always in the range [0, 180].
 *
 * - `eclipticSeparation` is the absolute value of the difference between the body's ecliptic longitude
 *   and the Sun's ecliptic longitude, both as seen from the center of the Earth. This angle measures
 *   around the plane of the Earth's orbit, and ignores how far above or below that plane the body is.
 *   The ecliptic separation is measured in degrees and is always in the range [0, 180].
 *
 * @param body
 * The celestial body whose visibility is to be calculated.
 *
 * @param time
 * The date and time of the observation.
 */
fun elongation(body: Body, time: Time): ElongationInfo {
    val relativeLongitude = pairLongitude(body, Body.Sun, time)
    val elongation = angleFromSun(body, time)
    return if (relativeLongitude > 180.0)
        ElongationInfo(time, Visibility.Morning, elongation, 360.0 - relativeLongitude)
    else
        ElongationInfo(time, Visibility.Evening, elongation, relativeLongitude)
}


private fun negativeElongationSlope(body: Body, time: Time): Double {
    val dt = 0.1
    val t1 = time.addDays(-dt / 2.0)
    val t2 = time.addDays(+dt / 2.0)
    val e1 = angleFromSun(body, t1)
    val e2 = angleFromSun(body, t2)
    return (e1 - e2) / dt
}


/**
 * Finds a date and time when Mercury or Venus reaches its maximum angle from the Sun as seen from the Earth.
 *
 * Mercury and Venus are are often difficult to observe because they are closer to the Sun than the Earth is.
 * Mercury especially is almost always impossible to see because it gets lost in the Sun's glare.
 * The best opportunities for spotting Mercury, and the best opportunities for viewing Venus through
 * a telescope without atmospheric interference, are when these planets reach maximum elongation.
 * These are events where the planets reach the maximum angle from the Sun as seen from the Earth.
 *
 * This function solves for those times, reporting the next maximum elongation event's date and time,
 * the elongation value itself, the relative longitude with the Sun, and whether the planet is best
 * observed in the morning or evening. See [elongation] for more details about the returned structure.
 *
 * @param body
 * Either [Body.Mercury] or [Body.Venus]. Any other value will result in an exception.
 * To find the best viewing opportunites for planets farther from the Sun than the Earth is (Mars through Pluto)
 * use [searchRelativeLongitude] to find the next opposition event.
 *
 * @param startTime
 * The date and time at which to begin the search. The maximum elongation event found will always
 * be the first one that occurs after this date and time.
 */
fun searchMaxElongation(body: Body, startTime: Time): ElongationInfo {
    val s1: Double
    val s2: Double
    when (body) {
        Body.Mercury -> {
            s1 = 50.0
            s2 = 85.0
        }
        Body.Venus -> {
            s1 = 40.0
            s2 = 50.0
        }
        else -> throw InvalidBodyException(body)
    }

    val syn = synodicPeriod(body)
    var searchTime = startTime
    var iter = 0
    while (++iter <= 2) {
        val plon = eclipticLongitude(body, searchTime)
        val elon = eclipticLongitude(Body.Earth, searchTime)
        val rlon = longitudeOffset(plon - elon)     // clamp to (-180, +180].

        // The slope function is not well-behaved when rlon is near 0 degrees or 180 degrees
        // because there is a cusp there that causes a discontinuity in the derivative.
        // So we need to guard against searching near such times.
        var adjustDays: Double
        var rlonLo: Double
        var rlonHi: Double
        if (rlon >= -s1 && rlon < +s1) {
            // Seek to the window [+s1, +s2].
            adjustDays = 0.0
            // Search forward for the time t1 when rel lon = +s1.
            rlonLo = +s1
            // Search forward for the time t2 when rel lon = +s2.
            rlonHi = +s2
        } else if (rlon > +s2 || rlon < -s2) {
            // Seek to the next search window at [-s2, -s1].
            adjustDays = 0.0
            // Search forward for the time t1 when rel lon = -s2.
            rlonLo = -s2
            // Search forward for the time t2 when rel lon = -s1.
            rlonHi = -s1
        } else if (rlon >= 0.0) {
            // rlon must be in the middle of the window [+s1, +s2].
            // Search BACKWARD for the time t1 when rel lon = +s1.
            adjustDays = -syn / 4.0
            rlonLo = +s1
            rlonHi = +s2
            // Search forward from t1 to find t2 such that rel lon = +s2.
        } else {
            // rlon must be in the middle of the window [-s2, -s1].
            // Search BACKWARD for the time t1 when rel lon = -s2.
            adjustDays = -syn / 4.0
            rlonLo = -s2
            // Search forward from t1 to find t2 such that rel lon = -s1.
            rlonHi = -s1
        }

        val tStart = startTime.addDays(adjustDays)

        val t1 = searchRelativeLongitude(body, rlonLo, tStart)
        val t2 = searchRelativeLongitude(body, rlonHi, t1)

        // Now we have a time range [t1,t2] that brackets a maximum elongation event.
        // Confirm the bracketing.
        val m1 = negativeElongationSlope(body, t1)
        if (m1 >= 0.0)
            throw InternalError("There is a bug in the bracketing algorithm! m1 = $m1")

        val m2 = negativeElongationSlope(body, t2)
        if (m2 <= 0.0)
            throw InternalError("There is a bug in the bracketing algorithm! m2 = $m2")

        // Use the generic search algorithm to home in on where the slope crosses from negative to positive.
        val searchx = search(t1, t2, 10.0) { time -> negativeElongationSlope(body, time) } ?:
            throw InternalError("Maximum elongation search failed.")

        if (searchx.tt >= startTime.tt)
            return elongation(body, searchx)

        // This event is in the past (earlier than startTime).
        // We need to search forward from t2 to find the next possible window.
        searchTime = t2.addDays(1.0)
    }

    throw InternalError("Maximum elongation search iterated too many times.")
}


/**
 * Finds visual magnitude, phase angle, and other illumination information about a celestial body.
 *
 * This function calculates information about how bright a celestial body appears from the Earth,
 * reported as visual magnitude, which is a smaller (or even negative) number for brighter objects
 * and a larger number for dimmer objects.
 *
 * For bodies other than the Sun, it reports a phase angle, which is the angle in degrees between
 * the Sun and the Earth, as seen from the center of the body. Phase angle indicates what fraction
 * of the body appears illuminated as seen from the Earth. For example, when the phase angle is
 * near zero, it means the body appears "full" as seen from the Earth.  A phase angle approaching
 * 180 degrees means the body appears as a thin crescent as seen from the Earth.  A phase angle
 * of 90 degrees means the body appears "half full".
 * For the Sun, the phase angle is always reported as 0; the Sun emits light rather than reflecting it,
 * so it doesn't have a phase angle.
 *
 * When the body is Saturn, the returned structure contains a field `ringTilt` that holds
 * the tilt angle in degrees of Saturn's rings as seen from the Earth. A value of 0 means
 * the rings appear edge-on, and are thus nearly invisible from the Earth. The `ringTilt` holds
 * 0 for all bodies other than Saturn.
 *
 * @param body
 * The Sun, Moon, or any planet other than the Earth.
 *
 * @param time
 * The date and time of the observation.
 */
fun illumination(body: Body, time: Time): IlluminationInfo {
    if (body == Body.Earth)
        throw EarthNotAllowedException()

    val earth = helioEarthPos(time)

    val gc: Vector
    val hc: Vector
    val phaseAngle: Double

    if (body == Body.Sun) {
        gc = -earth
        hc = Vector(0.0, 0.0, 0.0, time)
        // The Sun emits light instead of reflecting it,
        // so we report a placeholder phase angle of 0.
        phaseAngle = 0.0
    } else {
        if (body == Body.Moon) {
            // For extra numeric precision, use geocentric Moon formula directly.
            gc = geoMoon(time)
            hc = earth + gc
        } else {
            // For planets, the heliocentric vector is more direct to calculate.
            hc = helioVector(body, time)
            gc = hc - earth
        }
        phaseAngle = gc.angleWith(hc)
    }

    val geoDist = gc.length()
    val helioDist = hc.length()
    val mag: Double
    val ringTilt: Double

    if (body == Body.Saturn) {
        val saturn = saturnMagnitude(phaseAngle, helioDist, geoDist, gc, time)
        ringTilt = saturn.tilt
        mag = saturn.mag
    } else {
        ringTilt = 0.0
        mag = when (body) {
            Body.Sun  -> -0.17 + 5.0*log10(geoDist / AU_PER_PARSEC)
            Body.Moon -> moonMagnitude(phaseAngle, helioDist, geoDist)
            else      -> visualMagnitude(body, phaseAngle, helioDist, geoDist)
        }
    }

    val phaseFraction = (1.0 + dcos(phaseAngle)) / 2.0
    return IlluminationInfo(time, mag, phaseAngle, phaseFraction, helioDist, ringTilt)
}

internal class MagTiltResult(
    val mag: Double,
    val tilt: Double
)

internal fun saturnMagnitude(
    phase: Double,
    helioDist: Double,
    geoDist: Double,
    gc: Vector,
    time: Time
): MagTiltResult {
    // Based on formulas by Paul Schlyter found here:
    // http://www.stjarnhimlen.se/comp/ppcomp.html#15

    // We must handle Saturn's rings as a major component of its visual magnitude.
    // Find geocentric ecliptic coordinates of Saturn.
    val eclip = equatorialToEcliptic(gc)

    val ir = 28.06   // tilt of Saturn's rings to the ecliptic, in degrees
    val nr = (169.51 + (3.82e-5 * time.tt))    // ascending node of Saturn's rings, in degrees

    // Find tilt of Saturn's rings, as seen from Earth.
    val tilt = asin(dsin(eclip.elat)*dcos(ir) - dcos(eclip.elat)*dsin(ir)*dsin(eclip.elon - nr))
    val sinTilt = sin(abs(tilt))

    val mag = (
        -9.0
        + 0.044*phase
        + sinTilt*(-2.6 + 1.2*sinTilt)
        + 5.0*log10(helioDist * geoDist)
    )

    return MagTiltResult(mag, RAD2DEG * tilt)
}


internal fun moonMagnitude(phase: Double, helioDist: Double, geoDist: Double): Double {
    // https://astronomy.stackexchange.com/questions/10246/is-there-a-simple-analytical-formula-for-the-lunar-phase-brightness-curve
    val rad = phase.degreesToRadians()
    val rad2 = rad * rad
    val rad4 = rad2 * rad2
    val mag = -12.717 + 1.49*abs(rad) + 0.0431*rad4
    val moonMeanDistanceAu = 385000.6 / KM_PER_AU
    val geoAu = geoDist / moonMeanDistanceAu
    return mag + 5.0*log10(helioDist * geoAu)
}


internal fun visualMagnitude(
    body: Body,
    phase: Double,
    helioDist: Double,
    geoDist: Double
): Double {
    // For Mercury and Venus, see:  https://iopscience.iop.org/article/10.1086/430212
    val c0: Double
    var c1 = 0.0
    var c2 = 0.0
    var c3 = 0.0
    when (body) {
        Body.Mercury -> {
            c0 = -0.60
            c1 = +4.98
            c2 = -4.88
            c3 = +3.02
        }
        Body.Venus -> {
            if (phase < 163.6) {
                c0 = -4.47
                c1 = +1.03
                c2 = +0.57
                c3 = +0.13
            } else {
                c0 = 0.98
                c1 = -1.02
            }
        }
        Body.Mars -> {
            c0 = -1.52
            c1 = +1.60
        }
        Body.Jupiter -> {
            c0 = -9.40
            c1 = +0.50
        }
        Body.Uranus -> {
            c0 = -7.19
            c1 = +0.25
        }
        Body.Neptune -> {
            c0 = -6.87
        }
        Body.Pluto -> {
            c0 = -1.00
            c1 = +4.00
        }
        else -> throw InvalidBodyException(body)
    }
    val x = phase / 100.0
    return (c0 + x*(c1 + x*(c2 + x*c3))) + 5.0*log10(helioDist * geoDist)
}


internal fun magnitudeSlope(body: Body, time: Time): Double {
    // The Search() function finds a transition from negative to positive values.
    // The derivative of magnitude y with respect to time t (dy/dt)
    // is negative as an object gets brighter, because the magnitude numbers
    // get smaller. At peak magnitude dy/dt = 0, then as the object gets dimmer,
    // dy/dt > 0.
    val dt = 0.01
    val t1 = time.addDays(-dt/2)
    val t2 = time.addDays(+dt/2)
    val y1 = illumination(body, t1)
    val y2 = illumination(body, t2)
    return (y2.mag - y1.mag) / dt
}


/**
 * Searches for the date and time Venus will next appear brightest as seen from the Earth.
 *
 * This function searches for the date and time Venus appears brightest as seen from the Earth.
 * Currently only Venus is supported for the `body` parameter, though this could change in the future.
 * Mercury's peak magnitude occurs at superior conjunction, when it is virtually impossible to see from the Earth,
 * so peak magnitude events have little practical value for that planet.
 * Planets other than Venus and Mercury reach peak magnitude at opposition, which can
 * be found using [searchRelativeLongitude].
 * The Moon reaches peak magnitude at full moon, which can be found using
 * [searchMoonQuarter] or [searchMoonPhase].
 * The Sun reaches peak magnitude at perihelion, which occurs each year in January.
 * However, the difference is minor and has little practical value.
 *
 * @param body
 * Currently only [Body.Venus] is allowed. Any other value causes an exception.
 *
 * @param startTime
 * The date and time to start searching for the next peak magnitude event.
 */
fun searchPeakMagnitude(body: Body, startTime: Time): IlluminationInfo {
    if (body != Body.Venus)
        throw InvalidBodyException(body)

    // s1 and s2 are relative longitudes within which peak magnitude of Venus can occur.
    val s1 = 10.0
    val s2 = 30.0

    var iter = 0
    var searchTime = startTime
    while (++iter <= 2) {
        // Find current heliocentric relative longitude between the
        // inferior planet and the Earth.
        val plon = eclipticLongitude(body, searchTime)
        val elon = eclipticLongitude(Body.Earth, searchTime)
        val rlon = longitudeOffset(plon - elon)     // clamp to (-180, +180].

        // The slope function is not well-behaved when rlon is near 0 degrees or 180 degrees
        // because there is a cusp there that causes a discontinuity in the derivative.
        // So we need to guard against searching near such times.

        var rlonLo: Double
        var rlonHi: Double
        var adjustDays: Double
        var syn: Double
        if (rlon >= -s1 && rlon < +s1) {
            // Seek to the window [+s1, +s2].
            adjustDays = 0.0
            // Search forward for the time t1 when rel lon = +s1.
            rlonLo = +s1
            // Search forward for the time t2 when rel lon = +s2.
            rlonHi = +s2
        } else if (rlon >= +s2 || rlon < -s2) {
            // Seek to the next search window at [-s2, -s1].
            adjustDays = 0.0
            // Search forward for the time t1 when rel lon = -s2.
            rlonLo = -s2
            // Search forward for the time t2 when rel lon = -s1.
            rlonHi = -s1
        } else if (rlon >= 0.0) {
            // rlon must be in the middle of the window [+s1, +s2].
            // Search BACKWARD for the time t1 when rel lon = +s1.
            syn = synodicPeriod(body)
            adjustDays = -syn / 4.0
            rlonLo = +s1
            // Search forward from t1 to find t2 such that rel lon = +s2.
            rlonHi = +s2
        } else {
            // rlon must be in the middle of the window [-s2, -s1].
            // Search BACKWARD for the time t1 when rel lon = -s2.
            syn = synodicPeriod(body)
            adjustDays = -syn / 4.0
            rlonLo = -s2
            // Search forward from t1 to find t2 such that rel lon = -s1.
            rlonHi = -s1
        }

        val tStart = searchTime.addDays(adjustDays)
        val t1 = searchRelativeLongitude(body, rlonLo, tStart)
        val t2 = searchRelativeLongitude(body, rlonHi, t1)

        // Now we have a time range [t1,t2] that brackets a maximum magnitude event.
        // Confirm the bracketing.
        val m1 = magnitudeSlope(body, t1)
        if (m1 >= 0.0)
            throw InternalError("m1 = $m1; should have been negative.")

        val m2 = magnitudeSlope(body, t2)
        if (m2 <= 0.0)
            throw InternalError("m2 = $m2; should have been positive.")

        // Use the generic search algorithm to home in on where the slope crosses from negative to positive.
        val tx = search(t1, t2, 10.0) { time -> magnitudeSlope(body, time) } ?:
            throw InternalError("Failed to find magnitude slope transition.")

        if (tx.tt >= startTime.tt)
            return illumination(body, tx)

        // This event is in the past (earlier than startTime).
        // We need to search forward from t2 to find the next possible window.
        // We never need to search more than twice.
        searchTime = t2.addDays(1.0)
    }
    // This should never happen. If it does, please report as a bug in Astronomy Engine.
    throw InternalError("Peak magnitude search failed.")
}


/**
 * Calculates the gravitational acceleration experienced by an observer on the Earth.
 *
 * This function implements the WGS 84 Ellipsoidal Gravity Formula.
 * The result is a combination of inward gravitational acceleration
 * with outward centrifugal acceleration, as experienced by an observer
 * in the Earth's rotating frame of reference.
 * The resulting value increases toward the Earth's poles and decreases
 * toward the equator, consistent with changes of the weight measured
 * by a spring scale of a fixed mass moved to different latitudes and heights
 * on the Earth.
 *
 * @param latitude
 * The latitude of the observer in degrees north or south of the equator.
 * By formula symmetry, positive latitudes give the same answer as negative
 * latitudes, so the sign does not matter.
 *
 * @param height
 * The height above the sea level geoid in meters.
 * No range checking is done; however, accuracy is only valid in the
 * range 0 to 100000 meters.
 *
 * @return
 * The effective gravitational acceleration expressed in meters per second squared.
 */
fun observerGravity(latitude: Double, height: Double): Double {
    val s = dsin(latitude)
    val s2 = s*s
    val g0 = 9.7803253359 * (1.0 + 0.00193185265241*s2) / sqrt(1.0 - 0.00669437999013*s2)
    return g0 * (1.0 - (3.15704e-07 - 2.10269e-09*s2)*height + 7.37452e-14*height*height)
}


/**
 * Calculates one of the 5 Lagrange points for a pair of co-orbiting bodies.
 *
 * Given a more massive "major" body and a much less massive "minor" body,
 * calculates one of the five Lagrange points in relation to the minor body's
 * orbit around the major body. The parameter `point` is an integer that
 * selects the Lagrange point as follows:
 *
 * 1 = the Lagrange point between the major body and minor body.
 * 2 = the Lagrange point on the far side of the minor body.
 * 3 = the Lagrange point on the far side of the major body.
 * 4 = the Lagrange point 60 degrees ahead of the minor body's orbital position.
 * 5 = the Lagrange point 60 degrees behind the minor body's orbital position.
 *
 * The function returns the state vector for the selected Lagrange point
 * in J2000 mean equator coordinates (EQJ), with respect to the center of the
 * major body.
 *
 * To calculate Sun/Earth Lagrange points, pass in [Body.Sun] for `majorBody`
 * and [Body.EMB] (Earth/Moon barycenter) for `minorBody`.
 * For Lagrange points of the Sun and any other planet, pass in just that planet
 * (e.g. [Body.Jupiter]) for `minorBody`.
 * To calculate Earth/Moon Lagrange points, pass in [Body.Earth] and [Body.Moon]
 * for the major and minor bodies respectively.
 *
 * In some cases, it may be more efficient to call [lagrangePointFast],
 * especially when the state vectors have already been calculated, or are needed
 * for some other purpose.
 *
 * @param point
 * An integer 1..5 that selects which of the Lagrange points to calculate.
 *
 * @param time
 * The time for which the Lagrange point is to be calculated.
 *
 * @param majorBody
 * The more massive of the co-orbiting bodies: [Body.Sun] or [Body.Earth].
 *
 * @param minorBody
 * The less massive of the co-orbiting bodies. See main remarks.
 *
 * @return
 * The position and velocity of the selected Lagrange point with respect to the major body's center.
 */
fun lagrangePoint(point: Int, time: Time, majorBody: Body, minorBody: Body): StateVector {
    val majorMass = massProduct(majorBody)
    val minorMass = massProduct(minorBody)
    val majorState: StateVector
    val minorState: StateVector

    // Calculate the state vectors for the major and minor bodies.
    if (majorBody == Body.Earth && minorBody == Body.Moon) {
        // Use geocentric calculations for more precision.
        majorState = StateVector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, time)
        minorState = geoMoonState(time)
    } else {
        majorState = helioState(majorBody, time)
        minorState = helioState(minorBody, time)
    }

    return lagrangePointFast(
        point,
        majorState,
        majorMass,
        minorState,
        minorMass
    )
}


/**
 * Calculates one of the 5 Lagrange points from body masses and state vectors.
 *
 * Given a more massive "major" body and a much less massive "minor" body,
 * calculates one of the five Lagrange points in relation to the minor body's
 * orbit around the major body. The parameter `point` is an integer that
 * selects the Lagrange point as follows:
 *
 * 1 = the Lagrange point between the major body and minor body.
 * 2 = the Lagrange point on the far side of the minor body.
 * 3 = the Lagrange point on the far side of the major body.
 * 4 = the Lagrange point 60 degrees ahead of the minor body's orbital position.
 * 5 = the Lagrange point 60 degrees behind the minor body's orbital position.
 *
 * The caller passes in the state vector and mass for both bodies.
 * The state vectors can be in any orientation and frame of reference.
 * The body masses are expressed as GM products, where G = the universal
 * gravitation constant and M = the body's mass. Thus the units for
 * `major_mass` and `minor_mass` must be au^3/day^2.
 * Use [massProduct] to obtain GM values for various solar system bodies.
 *
 * The function returns the state vector for the selected Lagrange point
 * using the same orientation as the state vector parameters `majorState` and `minorState`,
 * and the position and velocity components are with respect to the major body's center.
 *
 * Consider calling [lagrangePoint], instead of this function, for simpler usage in most cases.
 *
 * @param point
 * An integer 1..5 that selects which of the Lagrange points to calculate.
 *
 * @param majorState
 * The state vector of the major (more massive) of the pair of bodies.
 *
 * @param majorMass
 * The mass product GM of the major body.
 *
 * @param minorState
 * The state vector of the minor (less massive) of the pair of bodies.
 *
 * @param minorMass
 * The mass product GM of the minor body.
 *
 * @return
 * The position and velocity of the selected Lagrange point with respect to the major body's center.
 */
fun lagrangePointFast(
    point: Int,
    majorState: StateVector,
    majorMass: Double,
    minorState: StateVector,
    minorMass: Double
): StateVector {
    val cos_60 = 0.5
    val sin_60 = 0.8660254037844386   // sqrt(3) / 2

    if (point < 1 || point > 5)
        throw IllegalArgumentException("Invalid lagrange point $point")

    if (!majorMass.isFinite() || majorMass <= 0.0)
        throw IllegalArgumentException("Major mass must be a positive number.")

    if (!minorMass.isFinite() || minorMass <= 0.0)
        throw IllegalArgumentException("Minor mass must be a positive number.")

    verifyIdenticalTimes(majorState.t, minorState.t)

    // Find the relative position vector <dx, dy, dz>.
    var dx = minorState.x - majorState.x
    var dy = minorState.y - majorState.y
    var dz = minorState.z - majorState.z
    val R2 = dx*dx + dy*dy + dz*dz

    // R = Total distance between the bodies.
    val R = sqrt(R2)

    // Find the velocity vector <vx, vy, vz>.
    val vx = minorState.vx - majorState.vx
    val vy = minorState.vy - majorState.vy
    val vz = minorState.vz - majorState.vz

    if (point == 4 || point == 5) {
        // For L4 and L5, we need to find points 60 degrees away from the
        // line connecting the two bodies and in the instantaneous orbital plane.
        // Define the instantaneous orbital plane as the unique plane that contains
        // both the relative position vector and the relative velocity vector.

        // Take the cross product of position and velocity to find a normal vector <nx, ny, nz>.
        val nx = dy*vz - dz*vy
        val ny = dz*vx - dx*vz
        val nz = dx*vy - dy*vx

        // Take the cross product normal*position to get a tangential vector <ux, uy, uz>.
        var ux = ny*dz - nz*dy
        var uy = nz*dx - nx*dz
        var uz = nx*dy - ny*dx

        // Convert the tangential direction vector to a unit vector.
        val U = sqrt(ux*ux + uy*uy + uz*uz)
        ux /= U
        uy /= U
        uz /= U

        // Convert the relative position vector into a unit vector.
        dx /= R
        dy /= R
        dz /= R

        // Now we have two perpendicular unit vectors in the orbital plane: 'd' and 'u'.

        // Create new unit vectors rotated (+/-)60 degrees from the radius/tangent directions.
        val vert = if (point == 4) +sin_60 else -sin_60

        // Rotated radial vector
        val Dx = cos_60*dx + vert*ux
        val Dy = cos_60*dy + vert*uy
        val Dz = cos_60*dz + vert*uz

        // Rotated tangent vector
        val Ux = cos_60*ux - vert*dx
        val Uy = cos_60*uy - vert*dy
        val Uz = cos_60*uz - vert*dz


        // Use dot products to find radial and tangential components of the relative velocity.
        val vrad = vx*dx + vy*dy + vz*dz
        val vtan = vx*ux + vy*uy + vz*uz

        return StateVector(
            R * Dx,
            R * Dy,
            R * Dz,
            vrad*Dx + vtan*Ux,
            vrad*Dy + vtan*Uy,
            vrad*Dz + vtan*Uz,
            majorState.t
        )
    }

    // point = 1..3; all these lie on a straight line between the centers of the two bodies.
    // Calculate the distances of each body from their mutual barycenter.
    // r1 = negative distance of major mass from barycenter (e.g. Sun to the left of barycenter)
    // r2 = positive distance of minor mass from barycenter (e.g. Earth to the right of barycenter)
    val r1 = -R * (minorMass / (majorMass + minorMass))
    val r2 = +R * (majorMass / (majorMass + minorMass))

    // Calculate the square of the angular orbital speed in [rad^2 / day^2].
    val omega2 = (majorMass + minorMass) / (R2*R)

    // Use Newton's Method to numerically solve for the location where
    // outward centrifugal acceleration in the rotating frame of reference
    // is equal to net inward gravitational acceleration.
    // First derive a good initial guess based on approximate analysis.
    var scale: Double
    val numer1: Double
    val numer2: Double

    if (point == 1 || point == 2) {
        scale = (majorMass / (majorMass + minorMass)) * cbrt(minorMass / (3.0 * majorMass))
        numer1 = -majorMass    // The major mass is to the left of L1 and L2
        if (point == 1) {
            scale = 1.0 - scale
            numer2 = +minorMass    // The minor mass is to the right of L1.
        } else {
            scale = 1.0 + scale
            numer2 = -minorMass    // The minor mass is to the left of L2.
        }
    } else { // point == 3
        scale = ((7.0/12.0)*minorMass - majorMass) / (minorMass + majorMass)
        numer1 = +majorMass    // major mass is to the right of L3.
        numer2 = +minorMass    // minor mass is to the right of L3.
    }

    // Iterate Newton's Method until it converges.
    var x = R*scale - r1
    var deltax: Double
    do {
        val dr1 = x - r1
        val dr2 = x - r2
        val accel = omega2*x + numer1/(dr1*dr1) + numer2/(dr2*dr2)
        val deriv = omega2 - 2*numer1/(dr1*dr1*dr1) - 2*numer2/(dr2*dr2*dr2)
        deltax = accel/deriv
        x -= deltax
    } while (abs(deltax/R) > 1.0e-14)
    scale = (x - r1) / R

    return StateVector(
        scale * dx,
        scale * dy,
        scale * dz,
        scale * vx,
        scale * vy,
        scale * vz,
        majorState.t
    )
}


/**
 * Calculates the Moon's libration angles at a given moment in time.
 *
 * Libration is an observed back-and-forth wobble of the portion of the
 * Moon visible from the Earth. It is caused by the imperfect tidal locking
 * of the Moon's fixed rotation rate, compared to its variable angular speed
 * of orbit around the Earth.
 *
 * This function calculates a pair of perpendicular libration angles,
 * one representing rotation of the Moon in ecliptic longitude `elon`, the other
 * in ecliptic latitude `elat`, both relative to the Moon's mean Earth-facing position.
 *
 * This function also returns the geocentric position of the Moon
 * expressed in ecliptic longitude `mlon`, ecliptic latitude `mlat`, the
 * distance `dist_km` between the centers of the Earth and Moon expressed in kilometers,
 * and the apparent angular diameter of the Moon `diam_deg`.
 *
 * @param time
 * The date and time for which to calculate lunar libration.
 *
 * @return
 * The Moon's ecliptic position and libration angles as seen from the Earth.
 */
fun libration(time: Time): LibrationInfo {
    val t = time.julianCenturies()
    val t2 = t * t
    val t3 = t2 * t
    val t4 = t2 * t2

    val moon = MoonContext(time).calcMoon()

    val mlon = moon.lon.degreesToRadians()
    val mlat = moon.lat.degreesToRadians()
    val distanceKm = moon.dist * KM_PER_AU
    val diamDeg = 2.0 * datan(MOON_MEAN_RADIUS_KM / sqrt(distanceKm*distanceKm - MOON_MEAN_RADIUS_KM*MOON_MEAN_RADIUS_KM))

    // Inclination angle in radians = 0.026930430358272504
    val cosIncl = 0.99963739787586170
    val sinIncl = 0.02692717526916351

    // Moon's argument of latitude in radians.
    val f = normalizeLongitude(93.2720950 + 483202.0175233*t - 0.0036539*t2 - t3/3526000 + t4/863310000).degreesToRadians()

    // Moon's ascending node's mean longitude in radians.
    val omega = normalizeLongitude(125.0445479 - 1934.1362891*t + 0.0020754*t2 + t3/467441 - t4/60616000).degreesToRadians()

    // Sun's mean anomaly in radians.
    val m = normalizeLongitude(357.5291092 + 35999.0502909*t - 0.0001536*t2 + t3/24490000).degreesToRadians()

    // Moon's mean anomaly in radians.
    val mdash = normalizeLongitude(134.9633964 + 477198.8675055*t + 0.0087414*t2 + t3/69699 - t4/14712000).degreesToRadians()

    // Moon's mean elongation in radians.
    val d = normalizeLongitude(297.8501921 + 445267.1114034*t - 0.0018819*t2 + t3/545868 - t4/113065000).degreesToRadians()

    // Eccentricity of the Earth's orbit.
    val e = 1.0 - 0.002516*t - 0.0000074*t2

    // Optical librations
    val w = mlon - omega
    val a = atan2(sin(w)*cos(mlat)*cosIncl - sin(mlat)*sinIncl, cos(w)*cos(mlat))
    val ldash = longitudeOffset((a - f).radiansToDegrees())
    val bdash = asin(-sin(w)*cos(mlat)*sinIncl - sin(mlat)*cosIncl)

    // Physical librations
    val k1 = (119.75 + 131.849*t).degreesToRadians()
    val k2 = (72.56 + 20.186*t).degreesToRadians()

    val rho = (
        -0.02752*cos(mdash) +
        -0.02245*sin(f) +
        +0.00684*cos(mdash - 2*f) +
        -0.00293*cos(2*f) +
        -0.00085*cos(2*f - 2*d) +
        -0.00054*cos(mdash - 2*d) +
        -0.00020*sin(mdash + f) +
        -0.00020*cos(mdash + 2*f) +
        -0.00020*cos(mdash - f) +
        +0.00014*cos(mdash + 2*f - 2*d)
    )

    val sigma = (
        -0.02816*sin(mdash) +
        +0.02244*cos(f) +
        -0.00682*sin(mdash - 2*f) +
        -0.00279*sin(2*f) +
        -0.00083*sin(2*f - 2*d) +
        +0.00069*sin(mdash - 2*d) +
        +0.00040*cos(mdash + f) +
        -0.00025*sin(2*mdash) +
        -0.00023*sin(mdash + 2*f) +
        +0.00020*cos(mdash - f) +
        +0.00019*sin(mdash - f) +
        +0.00013*sin(mdash + 2*f - 2*d) +
        -0.00010*cos(mdash - 3*f)
    )

    val tau = (
        +0.02520*e*sin(m) +
        +0.00473*sin(2*mdash - 2*f) +
        -0.00467*sin(mdash) +
        +0.00396*sin(k1) +
        +0.00276*sin(2*mdash - 2*d) +
        +0.00196*sin(omega) +
        -0.00183*cos(mdash - f) +
        +0.00115*sin(mdash - 2*d) +
        -0.00096*sin(mdash - d) +
        +0.00046*sin(2*f - 2*d) +
        -0.00039*sin(mdash - f) +
        -0.00032*sin(mdash - m - d) +
        +0.00027*sin(2*mdash - m - 2*d) +
        +0.00023*sin(k2) +
        -0.00014*sin(2*d) +
        +0.00014*cos(2*mdash - 2*f) +
        -0.00012*sin(mdash - 2*f) +
        -0.00012*sin(2*mdash) +
        +0.00011*sin(2*mdash - 2*m - 2*d)
    )

    val elon = ldash - tau + (rho*cos(a) + sigma*sin(a))*tan(bdash)
    val elat = bdash.radiansToDegrees() + sigma*cos(a) - rho*sin(a)
    return LibrationInfo(elat, elon, moon.lon, moon.lat, distanceKm, diamDeg)
}

private const val moonNodeStepDays = 10.0     // a safe number of days to step without missing a Moon node

/**
 * Searches for a time when the Moon's center crosses through the ecliptic plane.
 *
 * Searches for the first ascending or descending node of the Moon after `startTime`.
 * An ascending node is when the Moon's center passes through the ecliptic plane
 * (the plane of the Earth's orbit around the Sun) from south to north.
 * A descending node is when the Moon's center passes through the ecliptic plane
 * from north to south. Nodes indicate possible times of solar or lunar eclipses,
 * if the Moon also happens to be in the correct phase (new or full, respectively).
 * Call `searchMoonNode` to find the first of a series of nodes.
 * Then call [nextMoonNode] to find as many more consecutive nodes as desired.
 *
 * See [moonNodesAfter] for convenient iteration of consecutive nodes.
 *
 * @param startTime
 * The date and time for starting the search for an ascending or descending node of the Moon.
 */
fun searchMoonNode(startTime: Time): NodeEventInfo {
    // Start at the given moment in time and sample the Moon's ecliptic latitude.
    // Step 10 days at a time, searching for an interval where that latitude crosses zero.
    var time1 = startTime
    var eclip1 = MoonContext(time1).calcMoon()
    while (true) {
        val time2 = time1.addDays(moonNodeStepDays)
        val eclip2 = MoonContext(time2).calcMoon()
        if (eclip1.lat * eclip2.lat <= 0.0) {
            // There is a node somewhere in this closed time interval.
            // Figure out whether it is an ascending node or a descending node.
            val kind: NodeEventKind
            val direction: Double
            if (eclip2.lat > eclip1.lat) {
                direction = +1.0
                kind = NodeEventKind.Ascending
            } else {
                direction = -1.0
                kind = NodeEventKind.Descending
            }
            val nodeTime = search(time1, time2, 1.0) {
                time -> direction * moonEclipticLatitudeDegrees(time)
            } ?: throw InternalError("Could not find Moon node.")
            return NodeEventInfo(nodeTime, kind)
        }
        time1 = time2
        eclip1 = eclip2
    }
}

/**
 * Searches for the next time when the Moon's center crosses through the ecliptic plane.
 *
 * Call [searchMoonNode] to find the first of a series of nodes.
 * Then call `nextMoonNode` to find as many more consecutive nodes as desired.
 *
 * See [moonNodesAfter] for convenient iteration of consecutive nodes.
 *
 * @param prevNode
 * The previous node found from calling [searchMoonNode] or `nextMoonNode`.
 */
fun nextMoonNode(prevNode: NodeEventInfo): NodeEventInfo {
    val time = prevNode.time.addDays(moonNodeStepDays)
    val node = searchMoonNode(time)
    if (node.kind == prevNode.kind)
        throw InternalError("Invalid repeated moon node kind: ${node.kind}")
    return node
}


/**
 * Enumerates a series of consecutive ascending/descending nodes of the Moon.
 *
 * This function enables iteration through an unlimited number
 * of consecutive lunar nodes starting at a given time.
 * This is a convenience wrapper around [searchMoonNode] and [nextMoonNode].
 *
 * @param startTime
 * The date and time for starting the search for a series of ascending/descending nodes of the Moon.
 */
fun moonNodesAfter(startTime: Time): Sequence<NodeEventInfo> =
    generateSequence(searchMoonNode(startTime)) { nextMoonNode(it) }



/**
 * Finds the first aphelion or perihelion for a planet after a given time.
 *
 * Given a date and time to start the search in `startTime`, this function finds the
 * next date and time that the center of the specified planet reaches the closest or farthest point
 * in its orbit with respect to the center of the Sun, whichever comes first,
 * after `startTime`.
 *
 * The closest point is called *perihelion* and the farthest point is called *aphelion*.
 * The word *apsis* refers to either event.
 *
 * To iterate through consecutive alternating perihelion and aphelion events,
 * call `searchPlanetApsis` once, then use the return value to call
 * [nextPlanetApsis]. After that, keep feeding the previous return value
 * from `nextPlanetApsis` into another call of `nextPlanetApsis`
 * as many times as desired.
 *
 * See [planetApsidesAfter] for convenient iteration of consecutive apsides.
 *
 * @param body
 * The planet for which to find the next perihelion/aphelion event.
 * Not allowed to be [Body.Sun] or [Body.Moon].
 *
 * @param startTime
 * The date and time at which to start searching for the next perihelion or aphelion.
 */
fun searchPlanetApsis(body: Body, startTime: Time): ApsisInfo {
    // Neptune and Pluto have "wavy" orbits for which a brute force search is needed.
    // Their orbits have multiple heliocentric distance maxima/minima clustered near each other!
    if (body == Body.Neptune || body == Body.Pluto)
        return bruteSearchPlanetApsis(body, startTime)

    // All the other planets have singular local maxima/minima per orbit.
    // Therefore we can use a more efficient calculus-based search.
    val orbitPeriodDays = planetOrbitalPeriod(body)
    val increment = orbitPeriodDays / 6.0
    var t1 = startTime
    var iter = 0
    var m1 = helioDistanceSlope(body, t1)
    while (iter * increment < 2.0 * orbitPeriodDays) {
        val t2 = t1.addDays(increment)
        val m2 = helioDistanceSlope(body, t2)
        if (m1 * m2 <= 0.0) {
            // There is a change of slope polarity within the time range [t1, t2].
            // Therefore this time range contains an apsis.
            // Figure out whether it is perihelion or aphelion.
            val kind: ApsisKind
            val direction: Double
            if (m1 < 0.0 || m2 > 0.0) {
                // We found a minimum-distance event: perihelion.
                // Search the time range for the time when the slope goes from negative to positive.
                direction = +1.0
                kind = ApsisKind.Pericenter
            } else if (m1 > 0.0 || m2 < 0.0) {
                // We found a maximum-distance event: aphelion.
                // Search the time range for the time when the slope goes from positive to negative.
                direction = -1.0
                kind = ApsisKind.Apocenter
            } else {
                // This should never happen. It should not be possible for both slopes to be zero.
                throw InternalError("Both slopes were zero in searchPlanetApsis.")
            }

            val apsisTime = search(t1, t2, 1.0) { time ->
                direction * helioDistanceSlope(body, time)
            } ?: throw InternalError("Failed to find slope transition in planetary apsis search.")

            val distance = helioDistance(body, apsisTime)
            return ApsisInfo(apsisTime, kind, distance)
        }
        // We have not yet found a slope polarity change. Keep searching.
        t1 = t2
        m1 = m2
        ++iter
    }
    // It should not be possible to fail to find an apsis within 2 planetary orbits.
    throw InternalError("Should have found planetary apsis within 2 orbital periods.")
}


/**
 * Finds the next planetary perihelion or aphelion event in a series.
 *
 * This function requires an [ApsisInfo] value obtained from a call
 * to [searchPlanetApsis] or `nextPlanetApsis`.
 * Given an aphelion event, this function finds the next perihelion event, and vice versa.
 *
 * See [searchPlanetApsis] for more details.
 * See [planetApsidesAfter] for convenient iteration of consecutive apsides.
 *
 * @param body
 * The planet for which to find the next perihelion/aphelion event.
 * Not allowed to be [Body.Sun] or [Body.Moon].
 * Must match the body passed into the call that produced the `apsis` parameter.
 *
 * @param apsis
 * An apsis event obtained from a call to [searchPlanetApsis] or `nextPlanetApsis`.
 */
fun nextPlanetApsis(body: Body, apsis: ApsisInfo): ApsisInfo {
    // Skip 1/4 of an orbit before starting search again.
    val time = apsis.time.addDays(planetOrbitalPeriod(body) / 4.0)
    val next = searchPlanetApsis(body, time)
    // Verify that we found the opposite kind of apsis from the previous one.
    if (next.kind == apsis.kind)
        throw InternalError("Found ${next.kind} twice in a row.")
    return next
}


/**
 * Enumerates a series of consecutive planetary perihelia/aphelia events.
 *
 * This function enables iteration through an unlimited number
 * of consecutive planetary apsides.
 * This is a convenience wrapper around [searchPlanetApsis] and [nextPlanetApsis].
 *
 * @param body
 * The planet for which to find a series of perihelia/aphelia events.
 * Not allowed to be [Body.Sun] or [Body.Moon].
 *
 * @param startTime
 * The date and time for starting the search for a series of apsides.
 */
fun planetApsidesAfter(body: Body, startTime: Time): Sequence<ApsisInfo> =
    generateSequence(searchPlanetApsis(body, startTime)) { nextPlanetApsis(body, it) }


/**
 * Returns the average number of days it takes for a planet to orbit the Sun.
 *
 * @param body
 * One of the planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, or Pluto.
 */
fun planetOrbitalPeriod(body: Body): Double =
    body.orbitalPeriod ?: throw InvalidBodyException(body)


internal fun helioDistanceSlope(body: Body, time: Time): Double {
    val dt = 0.001
    val t1 = time.addDays(-dt / 2.0)
    val t2 = time.addDays(+dt / 2.0)
    val r1 = helioDistance(body, t1)
    val r2 = helioDistance(body, t2)
    return (r2 - r1) / dt
}


internal fun bruteSearchPlanetApsis(body: Body, startTime: Time): ApsisInfo {
    // Neptune is a special case for two reasons:
    // 1. Its orbit is nearly circular (low orbital eccentricity).
    // 2. It is so distant from the Sun that the orbital period is very long.
    // Put together, this causes wobbling of the Sun around the Solar System Barycenter (SSB)
    // to be so significant that there are 3 local minima in the distance-vs-time curve
    // near each apsis. Therefore, unlike for other planets, we can't use an optimized
    // algorithm for finding dr/dt = 0.
    // Instead, we use a dumb, brute-force algorithm of sampling and finding min/max
    // heliocentric distance.
    //
    // There is a similar problem in the TOP2013 model for Pluto:
    // Its position vector has high-frequency oscillations that confuse the
    // slope-based determination of apsides.
    //
    // Rewind approximately 30 degrees in the orbit,
    // then search forward for 270 degrees.
    // This is a very cautious way to prevent missing an apsis.
    // Typically we will find two apsides, and we pick whichever
    // apsis is ealier, but after startTime.
    // Sample points around this orbital arc and find when the distance
    // is greatest and smallest.

    val npoints = 100
    val period = planetOrbitalPeriod(body)
    val t1 = startTime.addDays(period * ( -30.0 / 360.0))
    val t2 = startTime.addDays(period * (+270.0 / 360.0))
    var tMin = t1
    var tMax = t1
    val interval = (t2.ut - t1.ut) / (npoints - 1.0)
    var maxDist = -1.0
    var minDist = -1.0
    for (i in 0 until npoints) {
        val time = t1.addDays(i * interval)
        val dist = helioDistance(body, time)
        if (i == 0) {
            maxDist = dist
            minDist = dist
        } else {
            if (dist > maxDist) {
                maxDist = dist
                tMax = time
            }
            if (dist < minDist) {
                minDist = dist
                tMin = time
            }
        }
    }

    val perihelion = planetExtreme(body, ApsisKind.Pericenter, tMin.addDays(-2.0 * interval), 4.0 * interval)
    val aphelion   = planetExtreme(body, ApsisKind.Apocenter,  tMax.addDays(-2.0 * interval), 4.0 * interval)

    if (perihelion.time.tt >= startTime.tt) {
        if (aphelion.time.tt >= startTime.tt) {
            // Perihelion and aphelion are both valid. Pick the one that comes first.
            if (aphelion.time.tt < perihelion.time.tt)
                return aphelion
        }
        return perihelion
    }

    if (aphelion.time.tt >= startTime.tt)
        return aphelion

    throw InternalError("Failed to find apsis for $body.")
}


internal fun planetExtreme(body: Body, kind: ApsisKind, startTime: Time, initDaySpan: Double): ApsisInfo {
    val direction = when (kind) {
        ApsisKind.Apocenter  -> +1.0
        ApsisKind.Pericenter -> -1.0
    }
    val npoints = 10
    var searchTime = startTime
    var daySpan = initDaySpan
    while (true) {
        val interval = daySpan / (npoints - 1)
        if (interval < 1.0 / 1440.0) {      // iterate until uncertainty is less than one minute
            val apsisTime = searchTime.addDays(interval / 2.0)
            val distance = helioDistance(body, apsisTime)
            return ApsisInfo(apsisTime, kind, distance)
        }
        var bestI = -1
        var bestDistance = 0.0
        for (i in 0 until npoints) {
            val time = searchTime.addDays(i * interval)
            val distance = direction * helioDistance(body, time)
            if (i == 0 || distance > bestDistance) {
                bestI = i
                bestDistance = distance
            }
        }

        // Narrow in on the extreme point.
        searchTime = searchTime.addDays((bestI - 1) * interval)
        daySpan = 2.0 * interval
    }
}


/**
 * Calculates a rotation matrix from J2000 mean equator (EQJ) to J2000 mean ecliptic (ECL).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQJ = equatorial system, using equator at J2000 epoch.
 * Target: ECL = ecliptic system, using equator at J2000 epoch.
 */
fun rotationEqjEcl(): RotationMatrix {
    // ob = mean obliquity of the J2000 ecliptic = 0.40909260059599012 radians.
    val c = 0.9174821430670688    // cos(ob)
    val s = 0.3977769691083922    // sin(ob)
    return RotationMatrix(
        1.0, 0.0, 0.0,
        0.0,  +c,  -s,
        0.0,  +s,  +c
    )
}

/**
 * Calculates a rotation matrix from J2000 mean ecliptic (ECL) to J2000 mean equator (EQJ).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: ECL = ecliptic system, using equator at J2000 epoch.
 * Target: EQJ = equatorial system, using equator at J2000 epoch.
 */
fun rotationEclEqj(): RotationMatrix {
    // ob = mean obliquity of the J2000 ecliptic = 0.40909260059599012 radians.
    val c = 0.9174821430670688    // cos(ob)
    val s = 0.3977769691083922    // sin(ob)
    return RotationMatrix(
        1.0, 0.0, 0.0,
        0.0,  +c,  +s,
        0.0,  -s,  +c
    )
}

/**
 * Calculates a rotation matrix from J2000 mean equator (EQJ) to equatorial of-date (EQD).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQJ = equatorial system, using equator at J2000 epoch.
 * Target: EQD = equatorial system, using equator of the specified date/time.
 *
 * @param time
 * The date and time at which the Earth's equator defines the target orientation.
 */
fun rotationEqjEqd(time: Time): RotationMatrix =
    precessionRot(time, PrecessDirection.From2000) combine
    nutationRot(time, PrecessDirection.From2000)

/**
 * Calculates a rotation matrix from J2000 mean equator (EQJ) to true ecliptic of date (ECT).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQJ = equatorial system, using equator at J2000 epoch.
 * Target: ECT = ecliptic system, using true equinox of the specified date/time.
 *
 * @param time
 * The date and time at which the Earth's equator defines the target orientation.
 */
fun rotationEqjEct(time: Time): RotationMatrix =
    rotationEqjEqd(time) combine
    rotationEqdEct(time)

/**
 * Calculates a rotation matrix from true ecliptic of date (ECT) to J2000 mean equator (EQJ).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: ECT = ecliptic system, using true equinox of the specified date/time.
 * Target: EQJ = equatorial system, using equator at J2000 epoch.
 *
 * @param time
 * The date and time at which the Earth's equator defines the target orientation.
 */
fun rotationEctEqj(time: Time): RotationMatrix =
    rotationEctEqd(time) combine
    rotationEqdEqj(time)

/**
 * Calculates a rotation matrix from equatorial of-date (EQD) to J2000 mean equator (EQJ).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQD = equatorial system, using equator of the specified date/time.
 * Target: EQJ = equatorial system, using equator at J2000 epoch.
 *
 * @param time
 * The date and time at which the Earth's equator defines the source orientation.
 */
fun rotationEqdEqj(time: Time): RotationMatrix =
    nutationRot(time, PrecessDirection.Into2000) combine
    precessionRot(time, PrecessDirection.Into2000)

/**
 * Calculates a rotation matrix from equatorial of-date (EQD) to horizontal (HOR).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQD = equatorial system, using equator of the specified date/time.
 * Target: HOR = horizontal system.
 *
 * @param time
 * The date and time at which the Earth's equator applies.
 *
 * @param observer
 * A location near the Earth's mean sea level that defines the observer's horizon.
 *
 * @return
 * A rotation matrix that converts EQD to HOR at `time` and for `observer`.
 * The components of the horizontal vector are:
 * x = north, y = west, z = zenith (straight up from the observer).
 * These components are chosen so that the "right-hand rule" works for the vector
 * and so that north represents the direction where azimuth = 0.
 */
fun rotationEqdHor(time: Time, observer: Observer): RotationMatrix {
    // See the `horizon` function for more explanation of how this works.

    val sinlat = dsin(observer.latitude)
    val coslat = dcos(observer.latitude)
    val sinlon = dsin(observer.longitude)
    val coslon = dcos(observer.longitude)

    val uze = Vector(coslat * coslon, coslat * sinlon, sinlat, time)
    val une = Vector(-sinlat * coslon, -sinlat * sinlon, coslat, time)
    val uwe = Vector(sinlon, -coslon, 0.0, time)

    // Multiply sidereal hours by -15 to convert to degrees and flip eastward
    // rotation of the Earth to westward apparent movement of objects with time.

    val angle = -15.0 * siderealTime(time)
    val uz = spin(angle, uze)
    val un = spin(angle, une)
    val uw = spin(angle, uwe)

    return RotationMatrix(
        un.x, uw.x, uz.x,
        un.y, uw.y, uz.y,
        un.z, uw.z, uz.z
    )
}

/**
 * Calculates a rotation matrix from horizontal (HOR) to equatorial of-date (EQD).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: HOR = horizontal system (x=North, y=West, z=Zenith).
 * Target: EQD = equatorial system, using equator of the specified date/time.
 *
 * @param time
 * The date and time at which the Earth's equator applies.
 *
 * @param observer
 * A location near the Earth's mean sea level that defines the observer's horizon.
 *
 * @return A rotation matrix that converts HOR to EQD at `time` and for `observer`.
 */
fun rotationHorEqd(time: Time, observer: Observer): RotationMatrix =
    rotationEqdHor(time, observer).inverse()

/**
 * Calculates a rotation matrix from horizontal (HOR) to J2000 equatorial (EQJ).
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: HOR = horizontal system (x=North, y=West, z=Zenith).
 * Target: EQJ = equatorial system, using equator at the J2000 epoch.
 *
 * @param time
 * The date and time of the observation.
 *
 * @param observer
 * A location near the Earth's mean sea level that defines the observer's horizon.
 *
 * @return A rotation matrix that converts HOR to EQJ at `time` and for `observer`.
 */
fun rotationHorEqj(time: Time, observer: Observer): RotationMatrix =
    rotationHorEqd(time, observer) combine
    rotationEqdEqj(time)

/**
 * Calculates a rotation matrix from J2000 mean equator (EQJ) to horizontal (HOR).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQJ = equatorial system, using the equator at the J2000 epoch.
 * Target: HOR = horizontal system.
 *
 * @param time
 * The date and time of the observation.
 *
 * @param observer
 * A location near the Earth's mean sea level that defines the observer's horizon.
 *
 * @return
 * A rotation matrix that converts EQJ to HOR at `time` and for `observer`.
 * The components of the horizontal vector are:
 * x = north, y = west, z = zenith (straight up from the observer).
 * These components are chosen so that the "right-hand rule" works for the vector
 * and so that north represents the direction where azimuth = 0.
 */
fun rotationEqjHor(time: Time, observer: Observer): RotationMatrix =
    rotationHorEqj(time, observer).inverse()

/**
 * Calculates a rotation matrix from equatorial of-date (EQD) to J2000 mean ecliptic (ECL).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQD = equatorial system, using equator of date.
 * Target: ECL = ecliptic system, using equator at J2000 epoch.
 *
 * @param time
 * The date and time of the source equator.
 *
 * @return A rotation matrix that converts EQD to ECL.
 */
fun rotationEqdEcl(time: Time): RotationMatrix =
    rotationEqdEqj(time) combine
    rotationEqjEcl()

/**
 * Calculates a rotation matrix from J2000 mean ecliptic (ECL) to equatorial of-date (EQD).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: ECL = ecliptic system, using equator at J2000 epoch.
 * Target: EQD = equatorial system, using equator of date.
 *
 * @param time
 * The date and time of the desired equator.
 *
 * @return A rotation matrix that converts ECL to EQD.
 */
fun rotationEclEqd(time: Time): RotationMatrix =
    rotationEqdEcl(time).inverse()

/**
 * Calculates a rotation matrix from J2000 mean ecliptic (ECL) to horizontal (HOR).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: ECL = ecliptic system, using equator at J2000 epoch.
 * Target: HOR = horizontal system.
 *
 * @param time
 * The date and time of the desired horizontal orientation.
 *
 * @param observer
 * A location near the Earth's mean sea level that defines the observer's horizon.
 *
 * @return
 * A rotation matrix that converts ECL to HOR at `time` and for `observer`.
 * The components of the horizontal vector are:
 * x = north, y = west, z = zenith (straight up from the observer).
 * These components are chosen so that the "right-hand rule" works for the vector
 * and so that north represents the direction where azimuth = 0.
 */
fun rotationEclHor(time: Time, observer: Observer): RotationMatrix =
    rotationEclEqd(time) combine
    rotationEqdHor(time, observer)

/**
 * Calculates a rotation matrix from horizontal (HOR) to J2000 mean ecliptic (ECL).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: HOR = horizontal system.
 * Target: ECL = ecliptic system, using equator at J2000 epoch.
 *
 * @param time
 * The date and time of the horizontal observation.
 *
 * @param observer
 * The location of the horizontal observer.
 *
 * @return
 * A rotation matrix that converts HOR to ECL.
 */
fun rotationHorEcl(time: Time, observer: Observer): RotationMatrix =
    rotationEclHor(time, observer).inverse()

/**
 * Calculates a rotation matrix from galactic (GAL) to J2000 mean equator (EQJ).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: GAL = galactic system (IAU 1958 definition).
 * Target: EQJ = equatorial system, using the equator at the J2000 epoch.
 *
 * @return
 * A rotation matrix that converts GAL to EQJ.
 */
fun rotationEqjGal() =
    // This rotation matrix was calculated by the following script:
    // demo/python/galeqj_matrix.py
    RotationMatrix(
        -0.0548624779711344, +0.4941095946388765, -0.8676668813529025,
        -0.8734572784246782, -0.4447938112296831, -0.1980677870294097,
        -0.4838000529948520, +0.7470034631630423, +0.4559861124470794
    )

/**
 * Calculates a rotation matrix from galactic (GAL) to J2000 mean equator (EQJ).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: GAL = galactic system (IAU 1958 definition).
 * Target: EQJ = equatorial system, using the equator at the J2000 epoch.
 *
 * @return
 * A rotation matrix that converts GAL to EQJ.
 */
fun rotationGalEqj() =
    // This rotation matrix was calculated by the following script:
    // demo/python/galeqj_matrix.py
    RotationMatrix(
        -0.0548624779711344, -0.8734572784246782, -0.4838000529948520,
        +0.4941095946388765, -0.4447938112296831, +0.7470034631630423,
        -0.8676668813529025, -0.1980677870294097, +0.4559861124470794
    )


/**
 * Calculates a rotation matrix from true ecliptic of date (ECT) to equator of date (EQD).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: ECT = true ecliptic of date
 * Target: EQD = equator of date
 *
 * @param time
 * The date and time of the ecliptic/equator conversion.
 *
 * @return
 * A rotation matrix that converts ECT to EQD.
 */
fun rotationEctEqd(time: Time): RotationMatrix {
    val et = earthTilt(time)
    val tobl = et.tobl.degreesToRadians()
    val c = cos(tobl)
    val s = sin(tobl)
    return RotationMatrix(
        1.0, 0.0, 0.0,
        0.0,  +c,  +s,
        0.0,  -s,  +c
    )
}


/**
 * Calculates a rotation matrix from equator of date (EQD) to true ecliptic of date (ECT).
 *
 * This is one of the family of functions that returns a rotation matrix
 * for converting from one orientation to another.
 * Source: EQD = equator of date
 * Target: ECT = true ecliptic of date
 *
 * @param time
 * The date and time of the equator/ecliptic conversion.
 *
 * @return
 * A rotation matrix that converts EQD to ECT.
 */
fun rotationEqdEct(time: Time): RotationMatrix {
    val et = earthTilt(time)
    val tobl = et.tobl.degreesToRadians()
    val c = cos(tobl)
    val s = sin(tobl)
    return RotationMatrix(
        1.0, 0.0, 0.0,
        0.0,  +c,  -s,
        0.0,  +s,  +c
    )
}


/**
 * Determines the constellation that contains the given point in the sky.
 *
 * Given J2000 equatorial (EQJ) coordinates of a point in the sky, determines the
 * constellation that contains that point.
 *
 * @param ra
 * The right ascension (RA) of a point in the sky, using the J2000 equatorial system (EQJ).
 *
 * @param dec
 * The declination (DEC) of a point in the sky, using the J2000 equatorial system (EQJ).
 *
 * @return
 * A structure that contains the 3-letter abbreviation and full name
 * of the constellation that contains the given (ra,dec), along with
 * the converted B1875 (ra,dec) for that point.
 */
fun constellation(ra: Double, dec: Double): ConstellationInfo {
    if (dec < -90.0 || dec > +90.0)
        throw IllegalArgumentException("Invalid declination angle $dec. Must be -90..+90.")

    // Convert right ascension to degrees, and normalize to the range [0, 360).
    val raDeg = (ra * 15.0).withMinDegreeValue(0.0)

    // Convert coordinates from J2000 to B1875.
    val sph2000 = Spherical(dec, raDeg, 1.0)
    val vec2000 = sph2000.toVector(epoch2000)
    val vec1875 = constelRot.rotate(vec2000)
    val equ1875 = vec1875.toEquatorial()

    // Convert DEC from degrees and RA from hours,
    // into compact angle units used in the constelBounds table.
    val xDec = 24.0 * equ1875.dec
    val xRa = (24.0 * 15.0) * equ1875.ra

    // Search for the constellation using the B1875 coordinates.
    for (b in constelBounds)
        if ((b.decLo <= xDec) && (b.raHi > xRa) && (b.raLo <= xRa))
            return ConstellationInfo(constelNames[b.index].symbol, constelNames[b.index].name, equ1875.ra, equ1875.dec)

    // This should never happen! If it does, please report to: https://github.com/cosinekitty/astronomy/issues
    throw InternalError("Unable to find constellation for coordinates RA=$ra, DEC=$dec")
}

//=======================================================================================
// Generic gravity simulator

/**
 * A simulation of zero or more small bodies moving through the Solar System.
 *
 * This class calculates the movement of arbitrary small bodies,
 * such as asteroids or comets, that move through the Solar System.
 * It does so by calculating the gravitational forces on the bodies
 * from the Sun and planets. The user of this class supplies a
 * list of initial positions and velocities for the small bodies.
 * Then the class can update the positions and velocities over small
 * time steps. The gravity simulator also provides access to the
 * positions and velocities of the Sun and planets used in the simulation.
 */
class GravitySimulator {
    /**
     * The origin of the reference frame. See constructor for more info.
     */
    val originBody: Body

    private var prev: GravSimEndpoint;
    private var curr: GravSimEndpoint;

    /**
     * Creates a gravity simulation object.
     *
     * @param originBody
     * Specifies the origin of the reference frame.
     * All position vectors and velocity vectors will use `originBody`
     * as the origin of the coordinate system.
     * This origin applies to all the input vectors provided in the
     * `bodyStates` parameter of this function, along with all
     * output vectors returned by [GravitySimulator.update].
     * Most callers will want to provide one of the following:
     * [Body.Sun] for heliocentric coordinates,
     * [Body.SSB] for solar system barycentric coordinates,
     * or [Body.Earth] for geocentric coordinates. Note that the
     * gravity simulator does not correct for light travel time;
     * all state vectors are tied to a Newtonian "instantaneous" time.
     *
     * @param time
     * The initial time at which to start the simulation.
     *
     * @param bodyStates
     * An array of initial state vectors (positions and velocities) of the small bodies to be simulated.
     * The caller must know the positions and velocities of the small bodies at an initial moment in time.
     * Their positions and velocities are expressed with respect to `originBody`, using equatorial
     * J2000 orientation (EQJ).
     * Positions are expressed in astronomical units (AU).
     * Velocities are expressed in AU/day.
     * All the times embedded within the state vectors must be exactly equal to `time`,
     * or this constructor will throw an exception.
     */
    constructor(
        originBody: Body,
        time: Time,
        bodyStates: List<StateVector>
    ) {
        this.originBody = originBody

        // Verify that all the state vectors have matching times.
        for (b in bodyStates) {
            if (b.t.tt != time.tt) {
                throw IllegalArgumentException("Inconsistent time(s) in bodyStates array.")
            }
        }

        curr = initialEndpoint(time, bodyStates)
        prev = initialEndpoint(time, bodyStates)

        // Calculate the states of the Sun and planets.
        calcSolarSystem()

        // We need to do all the physics calculations in barycentric coordinates.
        // But the caller provides the input vectors with respect to `originBody`.
        // Correct the input body state vectors for the specified origin.
        if (originBody != Body.SSB) {
            // Determine the barycentric state of the origin body.
            val ostate = internalBodyState(originBody)

            // Add barycentric origin to origin-centric bodies to obtain barycentric bodies.
            for (bstate in curr.bodies) {
                bstate.r.x += ostate.r.x
                bstate.r.y += ostate.r.y
                bstate.r.z += ostate.r.z
                bstate.v.x += ostate.v.x
                bstate.v.y += ostate.v.y
                bstate.v.z += ostate.v.z
            }
        }

        // Calculate the net acceleration experienced by the small bodies.
        calcBodyAccelerations()

        // To prepare for a possible swap operation, duplicate the current state into the previous state.
        duplicate()
    }

    /**
     * Returns the time of the current simulation step.
     */
    fun time(): Time = curr.time


    /**
     * Advances the gravity simulation by a small time step.
     *
     * Updates the simulation of the user-supplied small bodies
     * to the time indicated by the `time` parameter.
     * Retuns an updated array of state vectors for the small bodies.
     * The positions and velocities in the returned array are referenced
     * to the `originBody` that was used to construct this simulator.
     *
     * @param time
     * A time that is a small increment away from the current simulation time.
     * It is up to the developer to figure out an appropriate time increment.
     * Depending on the trajectories, a smaller or larger increment
     * may be needed for the desired accuracy. Some experimentation may be needed.
     * Generally, bodies that stay in the outer Solar System and move slowly can
     * use larger time steps.  Bodies that pass into the inner Solar System and
     * move faster will need a smaller time step to maintain accuracy.
     * The `time` value may be after or before the current simulation time
     * to move forward or backward in time.
     */
    fun update(time: Time): Array<StateVector> {
        val dt = time.tt - curr.time.tt
        if (dt == 0.0) {
            // Special case: the time has not changed, so skip the usual physics calculations.
            // This allows another way for the caller to query the current body states.
            // It is also necessary to avoid dividing by `dt` if `dt` is zero.
            // To prepare for a possible swap operation, duplicate the current state into the previous state.
            duplicate()
        } else {
            // Exchange the current state with the previous state. Then calculate the new current state.
            swap()

            // Update the current time.
            curr.time = time

            // Now that the time is set, it is safe to update the Solar System.
            calcSolarSystem()

            // Estimate the positions of the small bodies as if their existing
            // accelerations apply across the whole time interval.
            prev.bodies.forEachIndexed { i, p ->
                curr.bodies[i].r = updatePosition(dt, p.r, p.v, p.a)
            }

            // Calculate the acceleration experienced by the small bodies
            // at their respective approximate next locations.
            calcBodyAccelerations()

            prev.bodies.forEachIndexed { i, p ->
                val c = curr.bodies[i]

                // Calculate the average of the acceleration vectors
                // experienced by the previous body positions and
                // their estimated next positions.
                // These become estimates of the mean effective accelerations over the whole interval.
                val acc = p.a.mean(c.a)

                // Refine the estimates of position and velocity at the next time step,
                // using the mean acceleration as a better approximation of the
                // continuously changing acceleration acting on each body.
                c.tt = time.tt
                c.r = updatePosition(dt, p.r, p.v, acc)
                c.v = updateVelocity(dt, p.v, acc)
            }

            // Re-calculate accelerations experienced by each body.
            // These will be needed for the next simulation step (if any).
            // Also, they will be potentially useful if some day we add
            // a function to query the acceleration vectors for the bodies.
            calcBodyAccelerations()
        }

        // Translate our internal calculations of body positions
        // and velocities into state vectors that the caller can understand.
        // We have to convert the internal type BodyGravCalc to the public
        // type StateVector.
        val bodyStateArray = curr.bodies.map { exportGravCalc(it, time) }.toTypedArray()

        if (originBody != Body.SSB) {
            // Now we have to convert the coordinate system to the caller's chosen origin body.
            // Determine the barycentric state of the origin body.
            val ostate = internalBodyState(originBody)

            // Subtract vectors to convert barycentric states to origin-centric states.
            for (i in bodyStateArray.indices) {
                bodyStateArray[i] = StateVector(
                    bodyStateArray[i].x  - ostate.r.x,
                    bodyStateArray[i].y  - ostate.r.y,
                    bodyStateArray[i].z  - ostate.r.z,
                    bodyStateArray[i].vx - ostate.v.x,
                    bodyStateArray[i].vy - ostate.v.y,
                    bodyStateArray[i].vz - ostate.v.z,
                    time
                )
            }
        }

        return bodyStateArray
    }

    /**
     * Exchange the current time step with the previous time step.
     *
     * Sometimes it is helpful to "explore" various times near a given
     * simulation time step, while repeatedly returning to the original
     * time step. For example, when backdating a position for light travel
     * time, the caller may wish to repeatedly try different amounts of
     * backdating. When the backdating solver has converged, the caller
     * wants to leave the simulation in its original state.
     *
     * This function allows a single "undo" of a simulation, and does so
     * very efficiently.
     *
     * Usually this function will be called immediately after a matching
     * call to [GravitySimulator.update]. It has the effect of rolling
     * back the most recent update. If called twice in a row, it reverts
     * the swap and thus has no net effect.
     *
     * The constructor initializes the current state and previous
     * state to be identical. Both states represent the `time` parameter that was
     * passed into the constructor. Therefore, `swap` will
     * have no effect from the caller's point of view when passed a simulator
     * that has not yet been updated by a call to [GravitySimulator.update].
     */
    fun swap() {
        val s = curr
        curr = prev
        prev = s
    }


    /**
     * Get the position and velocity of a Solar System body included in the simulation.
     *
     * In order to simulate the movement of small bodies through the Solar System,
     * the simulator needs to calculate the state vectors for the Sun and planets.
     *
     * If an application wants to know the positions of one or more of the planets
     * in addition to the small bodies, this function provides a way to obtain
     * their state vectors. This is provided for the sake of efficiency, to avoid
     * redundant calculations.
     *
     * The state vector is returned relative to the position and velocity
     * of the `originBody` parameter that was passed to this object's constructor.
     *
     * @param body
     * The Sun, Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, or Neptune.
     */
    fun solarSystemBodyState(body: Body): StateVector {
        // Subtract the origin's state from the body's barycentric state
        // to get the body in the desired reference frame.
        val bstate = internalBodyState(body)
        val ostate = internalBodyState(originBody)
        return exportState(bstate - ostate, curr.time)
    }

    private fun internalBodyState(body: Body): BodyState =
        // Return the barycentric state of the given solar system body.
        if (body == Body.Sun || (body.ordinal >= Body.Mercury.ordinal && body.ordinal <= Body.Neptune.ordinal))
            curr.gravitators[body.ordinal]
        else if (body == Body.SSB)
            BodyState(curr.time.tt, TerseVector.zero(), TerseVector.zero())
        else
            throw InvalidBodyException(body)

    private fun initialEndpoint(time: Time, bodyStates: List<StateVector>): GravSimEndpoint {
        // Create an initial "endpoint" data structure, but without correcting
        // the coordinate system yet for the caller's chosen originBody.
        // That has to wait until after we have called `calcSolarSystem`
        // to know the positions of the planets.
        // We also make placeholders for the Sun and planet body states,
        // which are calculated later by `calcSolarSystem`.

        val gravitators = Array<BodyState>(Body.Sun.ordinal + 1) {
            BodyState(
                time.tt,
                TerseVector(0.0, 0.0, 0.0),
                TerseVector(0.0, 0.0, 0.0)
            )
        }

        val bodies = bodyStates.map {
            BodyGravCalc(
                time.tt,
                TerseVector(it.x, it.y, it.z),
                TerseVector(it.vx, it.vy, it.vz),
                TerseVector(0.0, 0.0, 0.0)
            )
        }.toTypedArray()

        return GravSimEndpoint(time, gravitators, bodies)
    }

    private fun calcSolarSystem() {
        val tt = curr.time.tt

        // Initialize the Sun's position/velocity as zero vectors, then adjust from pulls from the planets.
        val sun: BodyState = curr.gravitators[Body.Sun.ordinal]
        sun.tt = tt
        sun.r.setToZero()
        sun.v.setToZero()

        // Calculate the state of each planet, and adjust the SSB state accordingly.
        curr.gravitators[Body.Mercury.ordinal] = adjustBarycenterPosVel(sun, tt, Body.Mercury, MERCURY_GM)
        curr.gravitators[Body.Venus.ordinal  ] = adjustBarycenterPosVel(sun, tt, Body.Venus,   VENUS_GM)
        curr.gravitators[Body.Earth.ordinal  ] = adjustBarycenterPosVel(sun, tt, Body.Earth,   EARTH_GM + MOON_GM)
        curr.gravitators[Body.Mars.ordinal   ] = adjustBarycenterPosVel(sun, tt, Body.Mars,    MARS_GM)
        curr.gravitators[Body.Jupiter.ordinal] = adjustBarycenterPosVel(sun, tt, Body.Jupiter, JUPITER_GM)
        curr.gravitators[Body.Saturn.ordinal ] = adjustBarycenterPosVel(sun, tt, Body.Saturn,  SATURN_GM)
        curr.gravitators[Body.Uranus.ordinal ] = adjustBarycenterPosVel(sun, tt, Body.Uranus,  URANUS_GM)
        curr.gravitators[Body.Neptune.ordinal] = adjustBarycenterPosVel(sun, tt, Body.Neptune, NEPTUNE_GM)

        // Convert planet states from heliocentric to barycentric.
        for (bindex in Body.Mercury.ordinal .. Body.Neptune.ordinal) {
            curr.gravitators[bindex].r.decrement(sun.r)
            curr.gravitators[bindex].v.decrement(sun.v)
        }

        // Convert heliocentric SSB to barycentric Sun.
        sun.r.negate()
        sun.v.negate()
    }

    private fun calcBodyAccelerations() {
        // Calculate the gravitational acceleration experienced by the simulated bodies.
        for (calc in curr.bodies) {
            calc.a.setToZero()
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Sun.ordinal    ].r, SUN_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Mercury.ordinal].r, MERCURY_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Venus.ordinal  ].r, VENUS_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Earth.ordinal  ].r, EARTH_GM + MOON_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Mars.ordinal   ].r, MARS_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Jupiter.ordinal].r, JUPITER_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Saturn.ordinal ].r, SATURN_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Uranus.ordinal ].r, URANUS_GM)
            addAcceleration(calc.a, calc.r, curr.gravitators[Body.Neptune.ordinal].r, NEPTUNE_GM)
        }
    }

    private fun addAcceleration(acc: TerseVector, smallPos: TerseVector, majorPos: TerseVector, gm: Double) {
        val dx = majorPos.x - smallPos.x
        val dy = majorPos.y - smallPos.y
        val dz = majorPos.z - smallPos.z
        val r2 = dx*dx + dy*dy + dz*dz
        val pull = gm / (r2 * sqrt(r2))
        acc.x += dx * pull
        acc.y += dy * pull
        acc.z += dz * pull
    }

    private fun duplicate() {
        // Copy the current state into the previous state, so that both become the same moment in time.

        prev.time = curr.time

        for (i in curr.gravitators.indices) {
            prev.gravitators[i].copyFrom(curr.gravitators[i])
        }

        for (i in curr.bodies.indices) {
            prev.bodies[i].copyFrom(curr.bodies[i])
        }
    }
}


private class GravSimEndpoint(
    var time: Time,
    var gravitators: Array<BodyState>,
    var bodies: Array<BodyGravCalc>
)

//=======================================================================================
// Generated code goes to the bottom of the source file,
// so that most line numbers are consistent between template code and target code.

//---------------------------------------------------------------------------------------
// Table of coefficients for calculating nutation using the IAU2000b model.

private class IauRow(
    val nals0: Int,
    val nals1: Int,
    val nals2: Int,
    val nals3: Int,
    val nals4: Int,
    val cls0: Double,
    val cls1: Double,
    val cls2: Double,
    val cls3: Double,
    val cls4: Double,
    val cls5: Double
)

private val iauRow: Array<IauRow> = arrayOf(
    IauRow( 0,  0,  0,  0,  1, -172064161.0,    -174666.0,      33386.0,   92052331.0,       9086.0,      15377.0),
    IauRow( 0,  0,  2, -2,  2,  -13170906.0,      -1675.0,     -13696.0,    5730336.0,      -3015.0,      -4587.0),
    IauRow( 0,  0,  2,  0,  2,   -2276413.0,       -234.0,       2796.0,     978459.0,       -485.0,       1374.0),
    IauRow( 0,  0,  0,  0,  2,    2074554.0,        207.0,       -698.0,    -897492.0,        470.0,       -291.0),
    IauRow( 0,  1,  0,  0,  0,    1475877.0,      -3633.0,      11817.0,      73871.0,       -184.0,      -1924.0)
)

//---------------------------------------------------------------------------------------
// VSOP87 coefficients, for calculating major planet state vectors.

private val vsopLonMercury0 = VsopSeries(arrayOf(
    VsopTerm(4.40250710144, 0.00000000000, 0.00000000000),
    VsopTerm(0.40989414977, 1.48302034195, 26087.90314157420),
    VsopTerm(0.05046294200, 4.47785489551, 52175.80628314840),
    VsopTerm(0.00855346844, 1.16520322459, 78263.70942472259),
    VsopTerm(0.00165590362, 4.11969163423, 104351.61256629678),
    VsopTerm(0.00034561897, 0.77930768443, 130439.51570787099),
    VsopTerm(0.00007583476, 3.71348404924, 156527.41884944518)
))

private val vsopLonMercury1 = VsopSeries(arrayOf(
    VsopTerm(26087.90313685529, 0.00000000000, 0.00000000000),
    VsopTerm(0.01131199811, 6.21874197797, 26087.90314157420),
    VsopTerm(0.00292242298, 3.04449355541, 52175.80628314840),
    VsopTerm(0.00075775081, 6.08568821653, 78263.70942472259),
    VsopTerm(0.00019676525, 2.80965111777, 104351.61256629678)
))

private val vsopLonMercury = VsopFormula(arrayOf(
    vsopLonMercury0,
    vsopLonMercury1
))

private val vsopLatMercury0 = VsopSeries(arrayOf(
    VsopTerm(0.11737528961, 1.98357498767, 26087.90314157420),
    VsopTerm(0.02388076996, 5.03738959686, 52175.80628314840),
    VsopTerm(0.01222839532, 3.14159265359, 0.00000000000),
    VsopTerm(0.00543251810, 1.79644363964, 78263.70942472259),
    VsopTerm(0.00129778770, 4.83232503958, 104351.61256629678),
    VsopTerm(0.00031866927, 1.58088495658, 130439.51570787099),
    VsopTerm(0.00007963301, 4.60972126127, 156527.41884944518)
))

private val vsopLatMercury1 = VsopSeries(arrayOf(
    VsopTerm(0.00274646065, 3.95008450011, 26087.90314157420),
    VsopTerm(0.00099737713, 3.14159265359, 0.00000000000)
))

private val vsopLatMercury = VsopFormula(arrayOf(
    vsopLatMercury0,
    vsopLatMercury1
))

private val vsopRadMercury0 = VsopSeries(arrayOf(
    VsopTerm(0.39528271651, 0.00000000000, 0.00000000000),
    VsopTerm(0.07834131818, 6.19233722598, 26087.90314157420),
    VsopTerm(0.00795525558, 2.95989690104, 52175.80628314840),
    VsopTerm(0.00121281764, 6.01064153797, 78263.70942472259),
    VsopTerm(0.00021921969, 2.77820093972, 104351.61256629678),
    VsopTerm(0.00004354065, 5.82894543774, 130439.51570787099)
))

private val vsopRadMercury1 = VsopSeries(arrayOf(
    VsopTerm(0.00217347740, 4.65617158665, 26087.90314157420),
    VsopTerm(0.00044141826, 1.42385544001, 52175.80628314840)
))

private val vsopRadMercury = VsopFormula(arrayOf(
    vsopRadMercury0,
    vsopRadMercury1
))


private val vsopLonVenus0 = VsopSeries(arrayOf(
    VsopTerm(3.17614666774, 0.00000000000, 0.00000000000),
    VsopTerm(0.01353968419, 5.59313319619, 10213.28554621100),
    VsopTerm(0.00089891645, 5.30650047764, 20426.57109242200),
    VsopTerm(0.00005477194, 4.41630661466, 7860.41939243920),
    VsopTerm(0.00003455741, 2.69964447820, 11790.62908865880),
    VsopTerm(0.00002372061, 2.99377542079, 3930.20969621960),
    VsopTerm(0.00001317168, 5.18668228402, 26.29831979980),
    VsopTerm(0.00001664146, 4.25018630147, 1577.34354244780),
    VsopTerm(0.00001438387, 4.15745084182, 9683.59458111640),
    VsopTerm(0.00001200521, 6.15357116043, 30639.85663863300)
))

private val vsopLonVenus1 = VsopSeries(arrayOf(
    VsopTerm(10213.28554621638, 0.00000000000, 0.00000000000),
    VsopTerm(0.00095617813, 2.46406511110, 10213.28554621100),
    VsopTerm(0.00007787201, 0.62478482220, 20426.57109242200)
))

private val vsopLonVenus = VsopFormula(arrayOf(
    vsopLonVenus0,
    vsopLonVenus1
))

private val vsopLatVenus0 = VsopSeries(arrayOf(
    VsopTerm(0.05923638472, 0.26702775812, 10213.28554621100),
    VsopTerm(0.00040107978, 1.14737178112, 20426.57109242200),
    VsopTerm(0.00032814918, 3.14159265359, 0.00000000000)
))

private val vsopLatVenus1 = VsopSeries(arrayOf(
    VsopTerm(0.00287821243, 1.88964962838, 10213.28554621100)
))

private val vsopLatVenus = VsopFormula(arrayOf(
    vsopLatVenus0,
    vsopLatVenus1
))

private val vsopRadVenus0 = VsopSeries(arrayOf(
    VsopTerm(0.72334820891, 0.00000000000, 0.00000000000),
    VsopTerm(0.00489824182, 4.02151831717, 10213.28554621100),
    VsopTerm(0.00001658058, 4.90206728031, 20426.57109242200),
    VsopTerm(0.00001378043, 1.12846591367, 11790.62908865880),
    VsopTerm(0.00001632096, 2.84548795207, 7860.41939243920),
    VsopTerm(0.00000498395, 2.58682193892, 9683.59458111640),
    VsopTerm(0.00000221985, 2.01346696541, 19367.18916223280),
    VsopTerm(0.00000237454, 2.55136053886, 15720.83878487840)
))

private val vsopRadVenus1 = VsopSeries(arrayOf(
    VsopTerm(0.00034551041, 0.89198706276, 10213.28554621100)
))

private val vsopRadVenus = VsopFormula(arrayOf(
    vsopRadVenus0,
    vsopRadVenus1
))


private val vsopLonEarth0 = VsopSeries(arrayOf(
    VsopTerm(1.75347045673, 0.00000000000, 0.00000000000),
    VsopTerm(0.03341656453, 4.66925680415, 6283.07584999140),
    VsopTerm(0.00034894275, 4.62610242189, 12566.15169998280),
    VsopTerm(0.00003417572, 2.82886579754, 3.52311834900),
    VsopTerm(0.00003497056, 2.74411783405, 5753.38488489680),
    VsopTerm(0.00003135899, 3.62767041756, 77713.77146812050),
    VsopTerm(0.00002676218, 4.41808345438, 7860.41939243920),
    VsopTerm(0.00002342691, 6.13516214446, 3930.20969621960),
    VsopTerm(0.00001273165, 2.03709657878, 529.69096509460),
    VsopTerm(0.00001324294, 0.74246341673, 11506.76976979360),
    VsopTerm(0.00000901854, 2.04505446477, 26.29831979980),
    VsopTerm(0.00001199167, 1.10962946234, 1577.34354244780),
    VsopTerm(0.00000857223, 3.50849152283, 398.14900340820),
    VsopTerm(0.00000779786, 1.17882681962, 5223.69391980220),
    VsopTerm(0.00000990250, 5.23268072088, 5884.92684658320),
    VsopTerm(0.00000753141, 2.53339052847, 5507.55323866740),
    VsopTerm(0.00000505267, 4.58292599973, 18849.22754997420),
    VsopTerm(0.00000492392, 4.20505711826, 775.52261132400),
    VsopTerm(0.00000356672, 2.91954114478, 0.06731030280),
    VsopTerm(0.00000284125, 1.89869240932, 796.29800681640),
    VsopTerm(0.00000242879, 0.34481445893, 5486.77784317500),
    VsopTerm(0.00000317087, 5.84901948512, 11790.62908865880),
    VsopTerm(0.00000271112, 0.31486255375, 10977.07880469900),
    VsopTerm(0.00000206217, 4.80646631478, 2544.31441988340),
    VsopTerm(0.00000205478, 1.86953770281, 5573.14280143310),
    VsopTerm(0.00000202318, 2.45767790232, 6069.77675455340),
    VsopTerm(0.00000126225, 1.08295459501, 20.77539549240),
    VsopTerm(0.00000155516, 0.83306084617, 213.29909543800)
))

private val vsopLonEarth1 = VsopSeries(arrayOf(
    VsopTerm(6283.07584999140, 0.00000000000, 0.00000000000),
    VsopTerm(0.00206058863, 2.67823455808, 6283.07584999140),
    VsopTerm(0.00004303419, 2.63512233481, 12566.15169998280)
))

private val vsopLonEarth2 = VsopSeries(arrayOf(
    VsopTerm(0.00008721859, 1.07253635559, 6283.07584999140)
))

private val vsopLonEarth = VsopFormula(arrayOf(
    vsopLonEarth0,
    vsopLonEarth1,
    vsopLonEarth2
))

private val vsopLatEarth0 = VsopSeries(arrayOf(
))

private val vsopLatEarth1 = VsopSeries(arrayOf(
    VsopTerm(0.00227777722, 3.41376620530, 6283.07584999140),
    VsopTerm(0.00003805678, 3.37063423795, 12566.15169998280)
))

private val vsopLatEarth = VsopFormula(arrayOf(
    vsopLatEarth0,
    vsopLatEarth1
))

private val vsopRadEarth0 = VsopSeries(arrayOf(
    VsopTerm(1.00013988784, 0.00000000000, 0.00000000000),
    VsopTerm(0.01670699632, 3.09846350258, 6283.07584999140),
    VsopTerm(0.00013956024, 3.05524609456, 12566.15169998280),
    VsopTerm(0.00003083720, 5.19846674381, 77713.77146812050),
    VsopTerm(0.00001628463, 1.17387558054, 5753.38488489680),
    VsopTerm(0.00001575572, 2.84685214877, 7860.41939243920),
    VsopTerm(0.00000924799, 5.45292236722, 11506.76976979360),
    VsopTerm(0.00000542439, 4.56409151453, 3930.20969621960),
    VsopTerm(0.00000472110, 3.66100022149, 5884.92684658320),
    VsopTerm(0.00000085831, 1.27079125277, 161000.68573767410),
    VsopTerm(0.00000057056, 2.01374292245, 83996.84731811189),
    VsopTerm(0.00000055736, 5.24159799170, 71430.69561812909),
    VsopTerm(0.00000174844, 3.01193636733, 18849.22754997420),
    VsopTerm(0.00000243181, 4.27349530790, 11790.62908865880)
))

private val vsopRadEarth1 = VsopSeries(arrayOf(
    VsopTerm(0.00103018607, 1.10748968172, 6283.07584999140),
    VsopTerm(0.00001721238, 1.06442300386, 12566.15169998280)
))

private val vsopRadEarth2 = VsopSeries(arrayOf(
    VsopTerm(0.00004359385, 5.78455133808, 6283.07584999140)
))

private val vsopRadEarth = VsopFormula(arrayOf(
    vsopRadEarth0,
    vsopRadEarth1,
    vsopRadEarth2
))


private val vsopLonMars0 = VsopSeries(arrayOf(
    VsopTerm(6.20347711581, 0.00000000000, 0.00000000000),
    VsopTerm(0.18656368093, 5.05037100270, 3340.61242669980),
    VsopTerm(0.01108216816, 5.40099836344, 6681.22485339960),
    VsopTerm(0.00091798406, 5.75478744667, 10021.83728009940),
    VsopTerm(0.00027744987, 5.97049513147, 3.52311834900),
    VsopTerm(0.00010610235, 2.93958560338, 2281.23049651060),
    VsopTerm(0.00012315897, 0.84956094002, 2810.92146160520),
    VsopTerm(0.00008926784, 4.15697846427, 0.01725365220),
    VsopTerm(0.00008715691, 6.11005153139, 13362.44970679920),
    VsopTerm(0.00006797556, 0.36462229657, 398.14900340820),
    VsopTerm(0.00007774872, 3.33968761376, 5621.84292321040),
    VsopTerm(0.00003575078, 1.66186505710, 2544.31441988340),
    VsopTerm(0.00004161108, 0.22814971327, 2942.46342329160),
    VsopTerm(0.00003075252, 0.85696614132, 191.44826611160),
    VsopTerm(0.00002628117, 0.64806124465, 3337.08930835080),
    VsopTerm(0.00002937546, 6.07893711402, 0.06731030280),
    VsopTerm(0.00002389414, 5.03896442664, 796.29800681640),
    VsopTerm(0.00002579844, 0.02996736156, 3344.13554504880),
    VsopTerm(0.00001528141, 1.14979301996, 6151.53388830500),
    VsopTerm(0.00001798806, 0.65634057445, 529.69096509460),
    VsopTerm(0.00001264357, 3.62275122593, 5092.15195811580),
    VsopTerm(0.00001286228, 3.06796065034, 2146.16541647520),
    VsopTerm(0.00001546404, 2.91579701718, 1751.53953141600),
    VsopTerm(0.00001024902, 3.69334099279, 8962.45534991020),
    VsopTerm(0.00000891566, 0.18293837498, 16703.06213349900),
    VsopTerm(0.00000858759, 2.40093811940, 2914.01423582380),
    VsopTerm(0.00000832715, 2.46418619474, 3340.59517304760),
    VsopTerm(0.00000832720, 4.49495782139, 3340.62968035200),
    VsopTerm(0.00000712902, 3.66335473479, 1059.38193018920),
    VsopTerm(0.00000748723, 3.82248614017, 155.42039943420),
    VsopTerm(0.00000723861, 0.67497311481, 3738.76143010800),
    VsopTerm(0.00000635548, 2.92182225127, 8432.76438481560),
    VsopTerm(0.00000655162, 0.48864064125, 3127.31333126180),
    VsopTerm(0.00000550474, 3.81001042328, 0.98032106820),
    VsopTerm(0.00000552750, 4.47479317037, 1748.01641306700),
    VsopTerm(0.00000425966, 0.55364317304, 6283.07584999140),
    VsopTerm(0.00000415131, 0.49662285038, 213.29909543800),
    VsopTerm(0.00000472167, 3.62547124025, 1194.44701022460),
    VsopTerm(0.00000306551, 0.38052848348, 6684.74797174860),
    VsopTerm(0.00000312141, 0.99853944405, 6677.70173505060),
    VsopTerm(0.00000293198, 4.22131299634, 20.77539549240),
    VsopTerm(0.00000302375, 4.48618007156, 3532.06069281140),
    VsopTerm(0.00000274027, 0.54222167059, 3340.54511639700),
    VsopTerm(0.00000281079, 5.88163521788, 1349.86740965880),
    VsopTerm(0.00000231183, 1.28242156993, 3870.30339179440),
    VsopTerm(0.00000283602, 5.76885434940, 3149.16416058820),
    VsopTerm(0.00000236117, 5.75503217933, 3333.49887969900),
    VsopTerm(0.00000274033, 0.13372524985, 3340.67973700260),
    VsopTerm(0.00000299395, 2.78323740866, 6254.62666252360)
))

private val vsopLonMars1 = VsopSeries(arrayOf(
    VsopTerm(3340.61242700512, 0.00000000000, 0.00000000000),
    VsopTerm(0.01457554523, 3.60433733236, 3340.61242669980),
    VsopTerm(0.00168414711, 3.92318567804, 6681.22485339960),
    VsopTerm(0.00020622975, 4.26108844583, 10021.83728009940),
    VsopTerm(0.00003452392, 4.73210393190, 3.52311834900),
    VsopTerm(0.00002586332, 4.60670058555, 13362.44970679920),
    VsopTerm(0.00000841535, 4.45864030426, 2281.23049651060)
))

private val vsopLonMars2 = VsopSeries(arrayOf(
    VsopTerm(0.00058152577, 2.04961712429, 3340.61242669980),
    VsopTerm(0.00013459579, 2.45738706163, 6681.22485339960)
))

private val vsopLonMars = VsopFormula(arrayOf(
    vsopLonMars0,
    vsopLonMars1,
    vsopLonMars2
))

private val vsopLatMars0 = VsopSeries(arrayOf(
    VsopTerm(0.03197134986, 3.76832042431, 3340.61242669980),
    VsopTerm(0.00298033234, 4.10616996305, 6681.22485339960),
    VsopTerm(0.00289104742, 0.00000000000, 0.00000000000),
    VsopTerm(0.00031365539, 4.44651053090, 10021.83728009940),
    VsopTerm(0.00003484100, 4.78812549260, 13362.44970679920)
))

private val vsopLatMars1 = VsopSeries(arrayOf(
    VsopTerm(0.00217310991, 6.04472194776, 3340.61242669980),
    VsopTerm(0.00020976948, 3.14159265359, 0.00000000000),
    VsopTerm(0.00012834709, 1.60810667915, 6681.22485339960)
))

private val vsopLatMars = VsopFormula(arrayOf(
    vsopLatMars0,
    vsopLatMars1
))

private val vsopRadMars0 = VsopSeries(arrayOf(
    VsopTerm(1.53033488271, 0.00000000000, 0.00000000000),
    VsopTerm(0.14184953160, 3.47971283528, 3340.61242669980),
    VsopTerm(0.00660776362, 3.81783443019, 6681.22485339960),
    VsopTerm(0.00046179117, 4.15595316782, 10021.83728009940),
    VsopTerm(0.00008109733, 5.55958416318, 2810.92146160520),
    VsopTerm(0.00007485318, 1.77239078402, 5621.84292321040),
    VsopTerm(0.00005523191, 1.36436303770, 2281.23049651060),
    VsopTerm(0.00003825160, 4.49407183687, 13362.44970679920),
    VsopTerm(0.00002306537, 0.09081579001, 2544.31441988340),
    VsopTerm(0.00001999396, 5.36059617709, 3337.08930835080),
    VsopTerm(0.00002484394, 4.92545639920, 2942.46342329160),
    VsopTerm(0.00001960195, 4.74249437639, 3344.13554504880),
    VsopTerm(0.00001167119, 2.11260868341, 5092.15195811580),
    VsopTerm(0.00001102816, 5.00908403998, 398.14900340820),
    VsopTerm(0.00000899066, 4.40791133207, 529.69096509460),
    VsopTerm(0.00000992252, 5.83861961952, 6151.53388830500),
    VsopTerm(0.00000807354, 2.10217065501, 1059.38193018920),
    VsopTerm(0.00000797915, 3.44839203899, 796.29800681640),
    VsopTerm(0.00000740975, 1.49906336885, 2146.16541647520)
))

private val vsopRadMars1 = VsopSeries(arrayOf(
    VsopTerm(0.01107433345, 2.03250524857, 3340.61242669980),
    VsopTerm(0.00103175887, 2.37071847807, 6681.22485339960),
    VsopTerm(0.00012877200, 0.00000000000, 0.00000000000),
    VsopTerm(0.00010815880, 2.70888095665, 10021.83728009940)
))

private val vsopRadMars2 = VsopSeries(arrayOf(
    VsopTerm(0.00044242249, 0.47930604954, 3340.61242669980),
    VsopTerm(0.00008138042, 0.86998389204, 6681.22485339960)
))

private val vsopRadMars = VsopFormula(arrayOf(
    vsopRadMars0,
    vsopRadMars1,
    vsopRadMars2
))


private val vsopLonJupiter0 = VsopSeries(arrayOf(
    VsopTerm(0.59954691494, 0.00000000000, 0.00000000000),
    VsopTerm(0.09695898719, 5.06191793158, 529.69096509460),
    VsopTerm(0.00573610142, 1.44406205629, 7.11354700080),
    VsopTerm(0.00306389205, 5.41734730184, 1059.38193018920),
    VsopTerm(0.00097178296, 4.14264726552, 632.78373931320),
    VsopTerm(0.00072903078, 3.64042916389, 522.57741809380),
    VsopTerm(0.00064263975, 3.41145165351, 103.09277421860),
    VsopTerm(0.00039806064, 2.29376740788, 419.48464387520),
    VsopTerm(0.00038857767, 1.27231755835, 316.39186965660),
    VsopTerm(0.00027964629, 1.78454591820, 536.80451209540),
    VsopTerm(0.00013589730, 5.77481040790, 1589.07289528380),
    VsopTerm(0.00008246349, 3.58227925840, 206.18554843720),
    VsopTerm(0.00008768704, 3.63000308199, 949.17560896980),
    VsopTerm(0.00007368042, 5.08101194270, 735.87651353180),
    VsopTerm(0.00006263150, 0.02497628807, 213.29909543800),
    VsopTerm(0.00006114062, 4.51319998626, 1162.47470440780),
    VsopTerm(0.00004905396, 1.32084470588, 110.20632121940),
    VsopTerm(0.00005305285, 1.30671216791, 14.22709400160),
    VsopTerm(0.00005305441, 4.18625634012, 1052.26838318840),
    VsopTerm(0.00004647248, 4.69958103684, 3.93215326310),
    VsopTerm(0.00003045023, 4.31676431084, 426.59819087600),
    VsopTerm(0.00002609999, 1.56667394063, 846.08283475120),
    VsopTerm(0.00002028191, 1.06376530715, 3.18139373770),
    VsopTerm(0.00001764763, 2.14148655117, 1066.49547719000),
    VsopTerm(0.00001722972, 3.88036268267, 1265.56747862640),
    VsopTerm(0.00001920945, 0.97168196472, 639.89728631400),
    VsopTerm(0.00001633223, 3.58201833555, 515.46387109300),
    VsopTerm(0.00001431999, 4.29685556046, 625.67019231240),
    VsopTerm(0.00000973272, 4.09764549134, 95.97922721780)
))

private val vsopLonJupiter1 = VsopSeries(arrayOf(
    VsopTerm(529.69096508814, 0.00000000000, 0.00000000000),
    VsopTerm(0.00489503243, 4.22082939470, 529.69096509460),
    VsopTerm(0.00228917222, 6.02646855621, 7.11354700080),
    VsopTerm(0.00030099479, 4.54540782858, 1059.38193018920),
    VsopTerm(0.00020720920, 5.45943156902, 522.57741809380),
    VsopTerm(0.00012103653, 0.16994816098, 536.80451209540),
    VsopTerm(0.00006067987, 4.42422292017, 103.09277421860),
    VsopTerm(0.00005433968, 3.98480737746, 419.48464387520),
    VsopTerm(0.00004237744, 5.89008707199, 14.22709400160)
))

private val vsopLonJupiter2 = VsopSeries(arrayOf(
    VsopTerm(0.00047233601, 4.32148536482, 7.11354700080),
    VsopTerm(0.00030649436, 2.92977788700, 529.69096509460),
    VsopTerm(0.00014837605, 3.14159265359, 0.00000000000)
))

private val vsopLonJupiter = VsopFormula(arrayOf(
    vsopLonJupiter0,
    vsopLonJupiter1,
    vsopLonJupiter2
))

private val vsopLatJupiter0 = VsopSeries(arrayOf(
    VsopTerm(0.02268615702, 3.55852606721, 529.69096509460),
    VsopTerm(0.00109971634, 3.90809347197, 1059.38193018920),
    VsopTerm(0.00110090358, 0.00000000000, 0.00000000000),
    VsopTerm(0.00008101428, 3.60509572885, 522.57741809380),
    VsopTerm(0.00006043996, 4.25883108339, 1589.07289528380),
    VsopTerm(0.00006437782, 0.30627119215, 536.80451209540)
))

private val vsopLatJupiter1 = VsopSeries(arrayOf(
    VsopTerm(0.00078203446, 1.52377859742, 529.69096509460)
))

private val vsopLatJupiter = VsopFormula(arrayOf(
    vsopLatJupiter0,
    vsopLatJupiter1
))

private val vsopRadJupiter0 = VsopSeries(arrayOf(
    VsopTerm(5.20887429326, 0.00000000000, 0.00000000000),
    VsopTerm(0.25209327119, 3.49108639871, 529.69096509460),
    VsopTerm(0.00610599976, 3.84115365948, 1059.38193018920),
    VsopTerm(0.00282029458, 2.57419881293, 632.78373931320),
    VsopTerm(0.00187647346, 2.07590383214, 522.57741809380),
    VsopTerm(0.00086792905, 0.71001145545, 419.48464387520),
    VsopTerm(0.00072062974, 0.21465724607, 536.80451209540),
    VsopTerm(0.00065517248, 5.97995884790, 316.39186965660),
    VsopTerm(0.00029134542, 1.67759379655, 103.09277421860),
    VsopTerm(0.00030135335, 2.16132003734, 949.17560896980),
    VsopTerm(0.00023453271, 3.54023522184, 735.87651353180),
    VsopTerm(0.00022283743, 4.19362594399, 1589.07289528380),
    VsopTerm(0.00023947298, 0.27458037480, 7.11354700080),
    VsopTerm(0.00013032614, 2.96042965363, 1162.47470440780),
    VsopTerm(0.00009703360, 1.90669633585, 206.18554843720),
    VsopTerm(0.00012749023, 2.71550286592, 1052.26838318840),
    VsopTerm(0.00007057931, 2.18184839926, 1265.56747862640),
    VsopTerm(0.00006137703, 6.26418240033, 846.08283475120),
    VsopTerm(0.00002616976, 2.00994012876, 1581.95934828300)
))

private val vsopRadJupiter1 = VsopSeries(arrayOf(
    VsopTerm(0.01271801520, 2.64937512894, 529.69096509460),
    VsopTerm(0.00061661816, 3.00076460387, 1059.38193018920),
    VsopTerm(0.00053443713, 3.89717383175, 522.57741809380),
    VsopTerm(0.00031185171, 4.88276958012, 536.80451209540),
    VsopTerm(0.00041390269, 0.00000000000, 0.00000000000)
))

private val vsopRadJupiter = VsopFormula(arrayOf(
    vsopRadJupiter0,
    vsopRadJupiter1
))


private val vsopLonSaturn0 = VsopSeries(arrayOf(
    VsopTerm(0.87401354025, 0.00000000000, 0.00000000000),
    VsopTerm(0.11107659762, 3.96205090159, 213.29909543800),
    VsopTerm(0.01414150957, 4.58581516874, 7.11354700080),
    VsopTerm(0.00398379389, 0.52112032699, 206.18554843720),
    VsopTerm(0.00350769243, 3.30329907896, 426.59819087600),
    VsopTerm(0.00206816305, 0.24658372002, 103.09277421860),
    VsopTerm(0.00079271300, 3.84007056878, 220.41264243880),
    VsopTerm(0.00023990355, 4.66976924553, 110.20632121940),
    VsopTerm(0.00016573588, 0.43719228296, 419.48464387520),
    VsopTerm(0.00014906995, 5.76903183869, 316.39186965660),
    VsopTerm(0.00015820290, 0.93809155235, 632.78373931320),
    VsopTerm(0.00014609559, 1.56518472000, 3.93215326310),
    VsopTerm(0.00013160301, 4.44891291899, 14.22709400160),
    VsopTerm(0.00015053543, 2.71669915667, 639.89728631400),
    VsopTerm(0.00013005299, 5.98119023644, 11.04570026390),
    VsopTerm(0.00010725067, 3.12939523827, 202.25339517410),
    VsopTerm(0.00005863206, 0.23656938524, 529.69096509460),
    VsopTerm(0.00005227757, 4.20783365759, 3.18139373770),
    VsopTerm(0.00006126317, 1.76328667907, 277.03499374140),
    VsopTerm(0.00005019687, 3.17787728405, 433.71173787680),
    VsopTerm(0.00004592550, 0.61977744975, 199.07200143640),
    VsopTerm(0.00004005867, 2.24479718502, 63.73589830340),
    VsopTerm(0.00002953796, 0.98280366998, 95.97922721780),
    VsopTerm(0.00003873670, 3.22283226966, 138.51749687070),
    VsopTerm(0.00002461186, 2.03163875071, 735.87651353180),
    VsopTerm(0.00003269484, 0.77492638211, 949.17560896980),
    VsopTerm(0.00001758145, 3.26580109940, 522.57741809380),
    VsopTerm(0.00001640172, 5.50504453050, 846.08283475120),
    VsopTerm(0.00001391327, 4.02333150505, 323.50541665740),
    VsopTerm(0.00001580648, 4.37265307169, 309.27832265580),
    VsopTerm(0.00001123498, 2.83726798446, 415.55249061210),
    VsopTerm(0.00001017275, 3.71700135395, 227.52618943960),
    VsopTerm(0.00000848642, 3.19150170830, 209.36694217490)
))

private val vsopLonSaturn1 = VsopSeries(arrayOf(
    VsopTerm(213.29909521690, 0.00000000000, 0.00000000000),
    VsopTerm(0.01297370862, 1.82834923978, 213.29909543800),
    VsopTerm(0.00564345393, 2.88499717272, 7.11354700080),
    VsopTerm(0.00093734369, 1.06311793502, 426.59819087600),
    VsopTerm(0.00107674962, 2.27769131009, 206.18554843720),
    VsopTerm(0.00040244455, 2.04108104671, 220.41264243880),
    VsopTerm(0.00019941774, 1.27954390470, 103.09277421860),
    VsopTerm(0.00010511678, 2.74880342130, 14.22709400160),
    VsopTerm(0.00006416106, 0.38238295041, 639.89728631400),
    VsopTerm(0.00004848994, 2.43037610229, 419.48464387520),
    VsopTerm(0.00004056892, 2.92133209468, 110.20632121940),
    VsopTerm(0.00003768635, 3.64965330780, 3.93215326310)
))

private val vsopLonSaturn2 = VsopSeries(arrayOf(
    VsopTerm(0.00116441330, 1.17988132879, 7.11354700080),
    VsopTerm(0.00091841837, 0.07325195840, 213.29909543800),
    VsopTerm(0.00036661728, 0.00000000000, 0.00000000000),
    VsopTerm(0.00015274496, 4.06493179167, 206.18554843720)
))

private val vsopLonSaturn = VsopFormula(arrayOf(
    vsopLonSaturn0,
    vsopLonSaturn1,
    vsopLonSaturn2
))

private val vsopLatSaturn0 = VsopSeries(arrayOf(
    VsopTerm(0.04330678039, 3.60284428399, 213.29909543800),
    VsopTerm(0.00240348302, 2.85238489373, 426.59819087600),
    VsopTerm(0.00084745939, 0.00000000000, 0.00000000000),
    VsopTerm(0.00030863357, 3.48441504555, 220.41264243880),
    VsopTerm(0.00034116062, 0.57297307557, 206.18554843720),
    VsopTerm(0.00014734070, 2.11846596715, 639.89728631400),
    VsopTerm(0.00009916667, 5.79003188904, 419.48464387520),
    VsopTerm(0.00006993564, 4.73604689720, 7.11354700080),
    VsopTerm(0.00004807588, 5.43305312061, 316.39186965660)
))

private val vsopLatSaturn1 = VsopSeries(arrayOf(
    VsopTerm(0.00198927992, 4.93901017903, 213.29909543800),
    VsopTerm(0.00036947916, 3.14159265359, 0.00000000000),
    VsopTerm(0.00017966989, 0.51979431110, 426.59819087600)
))

private val vsopLatSaturn = VsopFormula(arrayOf(
    vsopLatSaturn0,
    vsopLatSaturn1
))

private val vsopRadSaturn0 = VsopSeries(arrayOf(
    VsopTerm(9.55758135486, 0.00000000000, 0.00000000000),
    VsopTerm(0.52921382865, 2.39226219573, 213.29909543800),
    VsopTerm(0.01873679867, 5.23549604660, 206.18554843720),
    VsopTerm(0.01464663929, 1.64763042902, 426.59819087600),
    VsopTerm(0.00821891141, 5.93520042303, 316.39186965660),
    VsopTerm(0.00547506923, 5.01532618980, 103.09277421860),
    VsopTerm(0.00371684650, 2.27114821115, 220.41264243880),
    VsopTerm(0.00361778765, 3.13904301847, 7.11354700080),
    VsopTerm(0.00140617506, 5.70406606781, 632.78373931320),
    VsopTerm(0.00108974848, 3.29313390175, 110.20632121940),
    VsopTerm(0.00069006962, 5.94099540992, 419.48464387520),
    VsopTerm(0.00061053367, 0.94037691801, 639.89728631400),
    VsopTerm(0.00048913294, 1.55733638681, 202.25339517410),
    VsopTerm(0.00034143772, 0.19519102597, 277.03499374140),
    VsopTerm(0.00032401773, 5.47084567016, 949.17560896980),
    VsopTerm(0.00020936596, 0.46349251129, 735.87651353180),
    VsopTerm(0.00009796004, 5.20477537945, 1265.56747862640),
    VsopTerm(0.00011993338, 5.98050967385, 846.08283475120),
    VsopTerm(0.00020839300, 1.52102476129, 433.71173787680),
    VsopTerm(0.00015298404, 3.05943814940, 529.69096509460),
    VsopTerm(0.00006465823, 0.17732249942, 1052.26838318840),
    VsopTerm(0.00011380257, 1.73105427040, 522.57741809380),
    VsopTerm(0.00003419618, 4.94550542171, 1581.95934828300)
))

private val vsopRadSaturn1 = VsopSeries(arrayOf(
    VsopTerm(0.06182981340, 0.25843511480, 213.29909543800),
    VsopTerm(0.00506577242, 0.71114625261, 206.18554843720),
    VsopTerm(0.00341394029, 5.79635741658, 426.59819087600),
    VsopTerm(0.00188491195, 0.47215589652, 220.41264243880),
    VsopTerm(0.00186261486, 3.14159265359, 0.00000000000),
    VsopTerm(0.00143891146, 1.40744822888, 7.11354700080)
))

private val vsopRadSaturn2 = VsopSeries(arrayOf(
    VsopTerm(0.00436902572, 4.78671677509, 213.29909543800)
))

private val vsopRadSaturn = VsopFormula(arrayOf(
    vsopRadSaturn0,
    vsopRadSaturn1,
    vsopRadSaturn2
))


private val vsopLonUranus0 = VsopSeries(arrayOf(
    VsopTerm(5.48129294297, 0.00000000000, 0.00000000000),
    VsopTerm(0.09260408234, 0.89106421507, 74.78159856730),
    VsopTerm(0.01504247898, 3.62719260920, 1.48447270830),
    VsopTerm(0.00365981674, 1.89962179044, 73.29712585900),
    VsopTerm(0.00272328168, 3.35823706307, 149.56319713460),
    VsopTerm(0.00070328461, 5.39254450063, 63.73589830340),
    VsopTerm(0.00068892678, 6.09292483287, 76.26607127560),
    VsopTerm(0.00061998615, 2.26952066061, 2.96894541660),
    VsopTerm(0.00061950719, 2.85098872691, 11.04570026390),
    VsopTerm(0.00026468770, 3.14152083966, 71.81265315070),
    VsopTerm(0.00025710476, 6.11379840493, 454.90936652730),
    VsopTerm(0.00021078850, 4.36059339067, 148.07872442630),
    VsopTerm(0.00017818647, 1.74436930289, 36.64856292950),
    VsopTerm(0.00014613507, 4.73732166022, 3.93215326310),
    VsopTerm(0.00011162509, 5.82681796350, 224.34479570190),
    VsopTerm(0.00010997910, 0.48865004018, 138.51749687070),
    VsopTerm(0.00009527478, 2.95516862826, 35.16409022120),
    VsopTerm(0.00007545601, 5.23626582400, 109.94568878850),
    VsopTerm(0.00004220241, 3.23328220918, 70.84944530420),
    VsopTerm(0.00004051900, 2.27755017300, 151.04766984290),
    VsopTerm(0.00003354596, 1.06549007380, 4.45341812490),
    VsopTerm(0.00002926718, 4.62903718891, 9.56122755560),
    VsopTerm(0.00003490340, 5.48306144511, 146.59425171800),
    VsopTerm(0.00003144069, 4.75199570434, 77.75054398390),
    VsopTerm(0.00002922333, 5.35235361027, 85.82729883120),
    VsopTerm(0.00002272788, 4.36600400036, 70.32818044240),
    VsopTerm(0.00002051219, 1.51773566586, 0.11187458460),
    VsopTerm(0.00002148602, 0.60745949945, 38.13303563780),
    VsopTerm(0.00001991643, 4.92437588682, 277.03499374140),
    VsopTerm(0.00001376226, 2.04283539351, 65.22037101170),
    VsopTerm(0.00001666902, 3.62744066769, 380.12776796000),
    VsopTerm(0.00001284107, 3.11347961505, 202.25339517410),
    VsopTerm(0.00001150429, 0.93343589092, 3.18139373770),
    VsopTerm(0.00001533221, 2.58594681212, 52.69019803950),
    VsopTerm(0.00001281604, 0.54271272721, 222.86032299360),
    VsopTerm(0.00001372139, 4.19641530878, 111.43016149680),
    VsopTerm(0.00001221029, 0.19900650030, 108.46121608020),
    VsopTerm(0.00000946181, 1.19253165736, 127.47179660680),
    VsopTerm(0.00001150989, 4.17898916639, 33.67961751290)
))

private val vsopLonUranus1 = VsopSeries(arrayOf(
    VsopTerm(74.78159860910, 0.00000000000, 0.00000000000),
    VsopTerm(0.00154332863, 5.24158770553, 74.78159856730),
    VsopTerm(0.00024456474, 1.71260334156, 1.48447270830),
    VsopTerm(0.00009258442, 0.42829732350, 11.04570026390),
    VsopTerm(0.00008265977, 1.50218091379, 63.73589830340),
    VsopTerm(0.00009150160, 1.41213765216, 149.56319713460)
))

private val vsopLonUranus = VsopFormula(arrayOf(
    vsopLonUranus0,
    vsopLonUranus1
))

private val vsopLatUranus0 = VsopSeries(arrayOf(
    VsopTerm(0.01346277648, 2.61877810547, 74.78159856730),
    VsopTerm(0.00062341400, 5.08111189648, 149.56319713460),
    VsopTerm(0.00061601196, 3.14159265359, 0.00000000000),
    VsopTerm(0.00009963722, 1.61603805646, 76.26607127560),
    VsopTerm(0.00009926160, 0.57630380333, 73.29712585900)
))

private val vsopLatUranus1 = VsopSeries(arrayOf(
    VsopTerm(0.00034101978, 0.01321929936, 74.78159856730)
))

private val vsopLatUranus = VsopFormula(arrayOf(
    vsopLatUranus0,
    vsopLatUranus1
))

private val vsopRadUranus0 = VsopSeries(arrayOf(
    VsopTerm(19.21264847206, 0.00000000000, 0.00000000000),
    VsopTerm(0.88784984413, 5.60377527014, 74.78159856730),
    VsopTerm(0.03440836062, 0.32836099706, 73.29712585900),
    VsopTerm(0.02055653860, 1.78295159330, 149.56319713460),
    VsopTerm(0.00649322410, 4.52247285911, 76.26607127560),
    VsopTerm(0.00602247865, 3.86003823674, 63.73589830340),
    VsopTerm(0.00496404167, 1.40139935333, 454.90936652730),
    VsopTerm(0.00338525369, 1.58002770318, 138.51749687070),
    VsopTerm(0.00243509114, 1.57086606044, 71.81265315070),
    VsopTerm(0.00190522303, 1.99809394714, 1.48447270830),
    VsopTerm(0.00161858838, 2.79137786799, 148.07872442630),
    VsopTerm(0.00143706183, 1.38368544947, 11.04570026390),
    VsopTerm(0.00093192405, 0.17437220467, 36.64856292950),
    VsopTerm(0.00071424548, 4.24509236074, 224.34479570190),
    VsopTerm(0.00089806014, 3.66105364565, 109.94568878850),
    VsopTerm(0.00039009723, 1.66971401684, 70.84944530420),
    VsopTerm(0.00046677296, 1.39976401694, 35.16409022120),
    VsopTerm(0.00039025624, 3.36234773834, 277.03499374140),
    VsopTerm(0.00036755274, 3.88649278513, 146.59425171800),
    VsopTerm(0.00030348723, 0.70100838798, 151.04766984290),
    VsopTerm(0.00029156413, 3.18056336700, 77.75054398390),
    VsopTerm(0.00022637073, 0.72518687029, 529.69096509460),
    VsopTerm(0.00011959076, 1.75043392140, 984.60033162190),
    VsopTerm(0.00025620756, 5.25656086672, 380.12776796000)
))

private val vsopRadUranus1 = VsopSeries(arrayOf(
    VsopTerm(0.01479896629, 3.67205697578, 74.78159856730)
))

private val vsopRadUranus = VsopFormula(arrayOf(
    vsopRadUranus0,
    vsopRadUranus1
))


private val vsopLonNeptune0 = VsopSeries(arrayOf(
    VsopTerm(5.31188633046, 0.00000000000, 0.00000000000),
    VsopTerm(0.01798475530, 2.90101273890, 38.13303563780),
    VsopTerm(0.01019727652, 0.48580922867, 1.48447270830),
    VsopTerm(0.00124531845, 4.83008090676, 36.64856292950),
    VsopTerm(0.00042064466, 5.41054993053, 2.96894541660),
    VsopTerm(0.00037714584, 6.09221808686, 35.16409022120),
    VsopTerm(0.00033784738, 1.24488874087, 76.26607127560),
    VsopTerm(0.00016482741, 0.00007727998, 491.55792945680),
    VsopTerm(0.00009198584, 4.93747051954, 39.61750834610),
    VsopTerm(0.00008994250, 0.27462171806, 175.16605980020)
))

private val vsopLonNeptune1 = VsopSeries(arrayOf(
    VsopTerm(38.13303563957, 0.00000000000, 0.00000000000),
    VsopTerm(0.00016604172, 4.86323329249, 1.48447270830),
    VsopTerm(0.00015744045, 2.27887427527, 38.13303563780)
))

private val vsopLonNeptune = VsopFormula(arrayOf(
    vsopLonNeptune0,
    vsopLonNeptune1
))

private val vsopLatNeptune0 = VsopSeries(arrayOf(
    VsopTerm(0.03088622933, 1.44104372644, 38.13303563780),
    VsopTerm(0.00027780087, 5.91271884599, 76.26607127560),
    VsopTerm(0.00027623609, 0.00000000000, 0.00000000000),
    VsopTerm(0.00015355489, 2.52123799551, 36.64856292950),
    VsopTerm(0.00015448133, 3.50877079215, 39.61750834610)
))

private val vsopLatNeptune = VsopFormula(arrayOf(
    vsopLatNeptune0
))

private val vsopRadNeptune0 = VsopSeries(arrayOf(
    VsopTerm(30.07013205828, 0.00000000000, 0.00000000000),
    VsopTerm(0.27062259632, 1.32999459377, 38.13303563780),
    VsopTerm(0.01691764014, 3.25186135653, 36.64856292950),
    VsopTerm(0.00807830553, 5.18592878704, 1.48447270830),
    VsopTerm(0.00537760510, 4.52113935896, 35.16409022120),
    VsopTerm(0.00495725141, 1.57105641650, 491.55792945680),
    VsopTerm(0.00274571975, 1.84552258866, 175.16605980020),
    VsopTerm(0.00012012320, 1.92059384991, 1021.24889455140),
    VsopTerm(0.00121801746, 5.79754470298, 76.26607127560),
    VsopTerm(0.00100896068, 0.37702724930, 73.29712585900),
    VsopTerm(0.00135134092, 3.37220609835, 39.61750834610),
    VsopTerm(0.00007571796, 1.07149207335, 388.46515523820)
))

private val vsopRadNeptune = VsopFormula(arrayOf(
    vsopRadNeptune0
))



//---------------------------------------------------------------------------------------
// Geocentric Moon

private fun addSolarTerms(context: MoonContext) {
    context.addSol(    13.9020,    14.0600,    -0.0010,     0.2607, 0, 0, 0, 4)
    context.addSol(     0.4030,    -4.0100,     0.3940,     0.0023, 0, 0, 0, 3)
    context.addSol(  2369.9120,  2373.3600,     0.6010,    28.2333, 0, 0, 0, 2)
    context.addSol(  -125.1540,  -112.7900,    -0.7250,    -0.9781, 0, 0, 0, 1)
    context.addSol(     1.9790,     6.9800,    -0.4450,     0.0433, 1, 0, 0, 4)
    context.addSol(   191.9530,   192.7200,     0.0290,     3.0861, 1, 0, 0, 2)
    context.addSol(    -8.4660,   -13.5100,     0.4550,    -0.1093, 1, 0, 0, 1)
    context.addSol( 22639.5000, 22609.0700,     0.0790,   186.5398, 1, 0, 0, 0)
    context.addSol(    18.6090,     3.5900,    -0.0940,     0.0118, 1, 0, 0,-1)
    context.addSol( -4586.4650, -4578.1300,    -0.0770,    34.3117, 1, 0, 0,-2)
    context.addSol(     3.2150,     5.4400,     0.1920,    -0.0386, 1, 0, 0,-3)
    context.addSol(   -38.4280,   -38.6400,     0.0010,     0.6008, 1, 0, 0,-4)
    context.addSol(    -0.3930,    -1.4300,    -0.0920,     0.0086, 1, 0, 0,-6)
    context.addSol(    -0.2890,    -1.5900,     0.1230,    -0.0053, 0, 1, 0, 4)
    context.addSol(   -24.4200,   -25.1000,     0.0400,    -0.3000, 0, 1, 0, 2)
    context.addSol(    18.0230,    17.9300,     0.0070,     0.1494, 0, 1, 0, 1)
    context.addSol(  -668.1460,  -126.9800,    -1.3020,    -0.3997, 0, 1, 0, 0)
    context.addSol(     0.5600,     0.3200,    -0.0010,    -0.0037, 0, 1, 0,-1)
    context.addSol(  -165.1450,  -165.0600,     0.0540,     1.9178, 0, 1, 0,-2)
    context.addSol(    -1.8770,    -6.4600,    -0.4160,     0.0339, 0, 1, 0,-4)
    context.addSol(     0.2130,     1.0200,    -0.0740,     0.0054, 2, 0, 0, 4)
    context.addSol(    14.3870,    14.7800,    -0.0170,     0.2833, 2, 0, 0, 2)
    context.addSol(    -0.5860,    -1.2000,     0.0540,    -0.0100, 2, 0, 0, 1)
    context.addSol(   769.0160,   767.9600,     0.1070,    10.1657, 2, 0, 0, 0)
    context.addSol(     1.7500,     2.0100,    -0.0180,     0.0155, 2, 0, 0,-1)
    context.addSol(  -211.6560,  -152.5300,     5.6790,    -0.3039, 2, 0, 0,-2)
    context.addSol(     1.2250,     0.9100,    -0.0300,    -0.0088, 2, 0, 0,-3)
    context.addSol(   -30.7730,   -34.0700,    -0.3080,     0.3722, 2, 0, 0,-4)
    context.addSol(    -0.5700,    -1.4000,    -0.0740,     0.0109, 2, 0, 0,-6)
    context.addSol(    -2.9210,   -11.7500,     0.7870,    -0.0484, 1, 1, 0, 2)
    context.addSol(     1.2670,     1.5200,    -0.0220,     0.0164, 1, 1, 0, 1)
    context.addSol(  -109.6730,  -115.1800,     0.4610,    -0.9490, 1, 1, 0, 0)
    context.addSol(  -205.9620,  -182.3600,     2.0560,     1.4437, 1, 1, 0,-2)
    context.addSol(     0.2330,     0.3600,     0.0120,    -0.0025, 1, 1, 0,-3)
    context.addSol(    -4.3910,    -9.6600,    -0.4710,     0.0673, 1, 1, 0,-4)
    context.addSol(     0.2830,     1.5300,    -0.1110,     0.0060, 1,-1, 0, 4)
    context.addSol(    14.5770,    31.7000,    -1.5400,     0.2302, 1,-1, 0, 2)
    context.addSol(   147.6870,   138.7600,     0.6790,     1.1528, 1,-1, 0, 0)
    context.addSol(    -1.0890,     0.5500,     0.0210,     0.0000, 1,-1, 0,-1)
    context.addSol(    28.4750,    23.5900,    -0.4430,    -0.2257, 1,-1, 0,-2)
    context.addSol(    -0.2760,    -0.3800,    -0.0060,    -0.0036, 1,-1, 0,-3)
    context.addSol(     0.6360,     2.2700,     0.1460,    -0.0102, 1,-1, 0,-4)
    context.addSol(    -0.1890,    -1.6800,     0.1310,    -0.0028, 0, 2, 0, 2)
    context.addSol(    -7.4860,    -0.6600,    -0.0370,    -0.0086, 0, 2, 0, 0)
    context.addSol(    -8.0960,   -16.3500,    -0.7400,     0.0918, 0, 2, 0,-2)
    context.addSol(    -5.7410,    -0.0400,     0.0000,    -0.0009, 0, 0, 2, 2)
    context.addSol(     0.2550,     0.0000,     0.0000,     0.0000, 0, 0, 2, 1)
    context.addSol(  -411.6080,    -0.2000,     0.0000,    -0.0124, 0, 0, 2, 0)
    context.addSol(     0.5840,     0.8400,     0.0000,     0.0071, 0, 0, 2,-1)
    context.addSol(   -55.1730,   -52.1400,     0.0000,    -0.1052, 0, 0, 2,-2)
    context.addSol(     0.2540,     0.2500,     0.0000,    -0.0017, 0, 0, 2,-3)
    context.addSol(     0.0250,    -1.6700,     0.0000,     0.0031, 0, 0, 2,-4)
    context.addSol(     1.0600,     2.9600,    -0.1660,     0.0243, 3, 0, 0, 2)
    context.addSol(    36.1240,    50.6400,    -1.3000,     0.6215, 3, 0, 0, 0)
    context.addSol(   -13.1930,   -16.4000,     0.2580,    -0.1187, 3, 0, 0,-2)
    context.addSol(    -1.1870,    -0.7400,     0.0420,     0.0074, 3, 0, 0,-4)
    context.addSol(    -0.2930,    -0.3100,    -0.0020,     0.0046, 3, 0, 0,-6)
    context.addSol(    -0.2900,    -1.4500,     0.1160,    -0.0051, 2, 1, 0, 2)
    context.addSol(    -7.6490,   -10.5600,     0.2590,    -0.1038, 2, 1, 0, 0)
    context.addSol(    -8.6270,    -7.5900,     0.0780,    -0.0192, 2, 1, 0,-2)
    context.addSol(    -2.7400,    -2.5400,     0.0220,     0.0324, 2, 1, 0,-4)
    context.addSol(     1.1810,     3.3200,    -0.2120,     0.0213, 2,-1, 0, 2)
    context.addSol(     9.7030,    11.6700,    -0.1510,     0.1268, 2,-1, 0, 0)
    context.addSol(    -0.3520,    -0.3700,     0.0010,    -0.0028, 2,-1, 0,-1)
    context.addSol(    -2.4940,    -1.1700,    -0.0030,    -0.0017, 2,-1, 0,-2)
    context.addSol(     0.3600,     0.2000,    -0.0120,    -0.0043, 2,-1, 0,-4)
    context.addSol(    -1.1670,    -1.2500,     0.0080,    -0.0106, 1, 2, 0, 0)
    context.addSol(    -7.4120,    -6.1200,     0.1170,     0.0484, 1, 2, 0,-2)
    context.addSol(    -0.3110,    -0.6500,    -0.0320,     0.0044, 1, 2, 0,-4)
    context.addSol(     0.7570,     1.8200,    -0.1050,     0.0112, 1,-2, 0, 2)
    context.addSol(     2.5800,     2.3200,     0.0270,     0.0196, 1,-2, 0, 0)
    context.addSol(     2.5330,     2.4000,    -0.0140,    -0.0212, 1,-2, 0,-2)
    context.addSol(    -0.3440,    -0.5700,    -0.0250,     0.0036, 0, 3, 0,-2)
    context.addSol(    -0.9920,    -0.0200,     0.0000,     0.0000, 1, 0, 2, 2)
    context.addSol(   -45.0990,    -0.0200,     0.0000,    -0.0010, 1, 0, 2, 0)
    context.addSol(    -0.1790,    -9.5200,     0.0000,    -0.0833, 1, 0, 2,-2)
    context.addSol(    -0.3010,    -0.3300,     0.0000,     0.0014, 1, 0, 2,-4)
    context.addSol(    -6.3820,    -3.3700,     0.0000,    -0.0481, 1, 0,-2, 2)
    context.addSol(    39.5280,    85.1300,     0.0000,    -0.7136, 1, 0,-2, 0)
    context.addSol(     9.3660,     0.7100,     0.0000,    -0.0112, 1, 0,-2,-2)
    context.addSol(     0.2020,     0.0200,     0.0000,     0.0000, 1, 0,-2,-4)
    context.addSol(     0.4150,     0.1000,     0.0000,     0.0013, 0, 1, 2, 0)
    context.addSol(    -2.1520,    -2.2600,     0.0000,    -0.0066, 0, 1, 2,-2)
    context.addSol(    -1.4400,    -1.3000,     0.0000,     0.0014, 0, 1,-2, 2)
    context.addSol(     0.3840,    -0.0400,     0.0000,     0.0000, 0, 1,-2,-2)
    context.addSol(     1.9380,     3.6000,    -0.1450,     0.0401, 4, 0, 0, 0)
    context.addSol(    -0.9520,    -1.5800,     0.0520,    -0.0130, 4, 0, 0,-2)
    context.addSol(    -0.5510,    -0.9400,     0.0320,    -0.0097, 3, 1, 0, 0)
    context.addSol(    -0.4820,    -0.5700,     0.0050,    -0.0045, 3, 1, 0,-2)
    context.addSol(     0.6810,     0.9600,    -0.0260,     0.0115, 3,-1, 0, 0)
    context.addSol(    -0.2970,    -0.2700,     0.0020,    -0.0009, 2, 2, 0,-2)
    context.addSol(     0.2540,     0.2100,    -0.0030,     0.0000, 2,-2, 0,-2)
    context.addSol(    -0.2500,    -0.2200,     0.0040,     0.0014, 1, 3, 0,-2)
    context.addSol(    -3.9960,     0.0000,     0.0000,     0.0004, 2, 0, 2, 0)
    context.addSol(     0.5570,    -0.7500,     0.0000,    -0.0090, 2, 0, 2,-2)
    context.addSol(    -0.4590,    -0.3800,     0.0000,    -0.0053, 2, 0,-2, 2)
    context.addSol(    -1.2980,     0.7400,     0.0000,     0.0004, 2, 0,-2, 0)
    context.addSol(     0.5380,     1.1400,     0.0000,    -0.0141, 2, 0,-2,-2)
    context.addSol(     0.2630,     0.0200,     0.0000,     0.0000, 1, 1, 2, 0)
    context.addSol(     0.4260,     0.0700,     0.0000,    -0.0006, 1, 1,-2,-2)
    context.addSol(    -0.3040,     0.0300,     0.0000,     0.0003, 1,-1, 2, 0)
    context.addSol(    -0.3720,    -0.1900,     0.0000,    -0.0027, 1,-1,-2, 2)
    context.addSol(     0.4180,     0.0000,     0.0000,     0.0000, 0, 0, 4, 0)
    context.addSol(    -0.3300,    -0.0400,     0.0000,     0.0000, 3, 0, 2, 0)
}

//---------------------------------------------------------------------------------------
// Pluto state table

private const val PLUTO_NUM_STATES = 51
private const val PLUTO_TIME_STEP  = 29200
private const val PLUTO_DT         = 146
private const val PLUTO_NSTEPS     = 201

private val plutoStateTable: Array<BodyState> = arrayOf(
    BodyState( -730000.0, TerseVector(-26.118207232108, -14.376168177825,   3.384402515299), TerseVector( 1.6339372163656e-03, -2.7861699588508e-03, -1.3585880229445e-03))
,   BodyState( -700800.0, TerseVector( 41.974905202127,  -0.448502952929, -12.770351505989), TerseVector( 7.3458569351457e-04,  2.2785014891658e-03,  4.8619778602049e-04))
,   BodyState( -671600.0, TerseVector( 14.706930780744,  44.269110540027,   9.353698474772), TerseVector(-2.1000147999800e-03,  2.2295915939915e-04,  7.0143443551414e-04))
,   BodyState( -642400.0, TerseVector(-29.441003929957,  -6.430161530570,   6.858481011305), TerseVector( 8.4495803960544e-04, -3.0783914758711e-03, -1.2106305981192e-03))
,   BodyState( -613200.0, TerseVector( 39.444396946234,  -6.557989760571, -13.913760296463), TerseVector( 1.1480029005873e-03,  2.2400006880665e-03,  3.5168075922288e-04))
,   BodyState( -584000.0, TerseVector( 20.230380950700,  43.266966657189,   7.382966091923), TerseVector(-1.9754081700585e-03,  5.3457141292226e-04,  7.5929169129793e-04))
,   BodyState( -554800.0, TerseVector(-30.658325364620,   2.093818874552,   9.880531138071), TerseVector( 6.1010603013347e-05, -3.1326500935382e-03, -9.9346125151067e-04))
,   BodyState( -525600.0, TerseVector( 35.737703251673, -12.587706024764, -14.677847247563), TerseVector( 1.5802939375649e-03,  2.1347678412429e-03,  1.9074436384343e-04))
,   BodyState( -496400.0, TerseVector( 25.466295188546,  41.367478338417,   5.216476873382), TerseVector(-1.8054401046468e-03,  8.3283083599510e-04,  8.0260156912107e-04))
,   BodyState( -467200.0, TerseVector(-29.847174904071,  10.636426313081,  12.297904180106), TerseVector(-6.3257063052907e-04, -2.9969577578221e-03, -7.4476074151596e-04))
,   BodyState( -438000.0, TerseVector( 30.774692107687, -18.236637015304, -14.945535879896), TerseVector( 2.0113162005465e-03,  1.9353827024189e-03, -2.0937793168297e-06))
,   BodyState( -408800.0, TerseVector( 30.243153324028,  38.656267888503,   2.938501750218), TerseVector(-1.6052508674468e-03,  1.1183495337525e-03,  8.3333973416824e-04))
,   BodyState( -379600.0, TerseVector(-27.288984772533,  18.643162147874,  14.023633623329), TerseVector(-1.1856388898191e-03, -2.7170609282181e-03, -4.9015526126399e-04))
,   BodyState( -350400.0, TerseVector( 24.519605196774, -23.245756064727, -14.626862367368), TerseVector( 2.4322321483154e-03,  1.6062008146048e-03, -2.3369181613312e-04))
,   BodyState( -321200.0, TerseVector( 34.505274805875,  35.125338586954,   0.557361475637), TerseVector(-1.3824391637782e-03,  1.3833397561817e-03,  8.4823598806262e-04))
,   BodyState( -292000.0, TerseVector(-23.275363915119,  25.818514298769,  15.055381588598), TerseVector(-1.6062295460975e-03, -2.3395961498533e-03, -2.4377362639479e-04))
,   BodyState( -262800.0, TerseVector( 17.050384798092, -27.180376290126, -13.608963321694), TerseVector( 2.8175521080578e-03,  1.1358749093955e-03, -4.9548725258825e-04))
,   BodyState( -233600.0, TerseVector( 38.093671910285,  30.880588383337,  -1.843688067413), TerseVector(-1.1317697153459e-03,  1.6128814698472e-03,  8.4177586176055e-04))
,   BodyState( -204400.0, TerseVector(-18.197852930878,  31.932869934309,  15.438294826279), TerseVector(-1.9117272501813e-03, -1.9146495909842e-03, -1.9657304369835e-05))
,   BodyState( -175200.0, TerseVector(  8.528924039997, -29.618422200048, -11.805400994258), TerseVector( 3.1034370787005e-03,  5.1393633292430e-04, -7.7293066202546e-04))
,   BodyState( -146000.0, TerseVector( 40.946857258640,  25.904973592021,  -4.256336240499), TerseVector(-8.3652705194051e-04,  1.8129497136404e-03,  8.1564228273060e-04))
,   BodyState( -116800.0, TerseVector(-12.326958895325,  36.881883446292,  15.217158258711), TerseVector(-2.1166103705038e-03, -1.4814420035990e-03,  1.7401209844705e-04))
,   BodyState(  -87600.0, TerseVector( -0.633258375909, -30.018759794709,  -9.171932874950), TerseVector( 3.2016994581737e-03, -2.5279858672148e-04, -1.0411088271861e-03))
,   BodyState(  -58400.0, TerseVector( 42.936048423883,  20.344685584452,  -6.588027007912), TerseVector(-5.0525450073192e-04,  1.9910074335507e-03,  7.7440196540269e-04))
,   BodyState(  -29200.0, TerseVector( -5.975910552974,  40.611809958460,  14.470131723673), TerseVector(-2.2184202156107e-03, -1.0562361130164e-03,  3.3652250216211e-04))
,   BodyState(       0.0, TerseVector( -9.875369580774, -27.978926224737,  -5.753711824704), TerseVector( 3.0287533248818e-03, -1.1276087003636e-03, -1.2651326732361e-03))
,   BodyState(   29200.0, TerseVector( 43.958831986165,  14.214147973292,  -8.808306227163), TerseVector(-1.4717608981871e-04,  2.1404187242141e-03,  7.1486567806614e-04))
,   BodyState(   58400.0, TerseVector(  0.678136763520,  43.094461639362,  13.243238780721), TerseVector(-2.2358226110718e-03, -6.3233636090933e-04,  4.7664798895648e-04))
,   BodyState(   87600.0, TerseVector(-18.282602096834, -23.305039586660,  -1.766620508028), TerseVector( 2.5567245263557e-03, -1.9902940754171e-03, -1.3943491701082e-03))
,   BodyState(  116800.0, TerseVector( 43.873338744526,   7.700705617215, -10.814273666425), TerseVector( 2.3174803055677e-04,  2.2402163127924e-03,  6.2988756452032e-04))
,   BodyState(  146000.0, TerseVector(  7.392949027906,  44.382678951534,  11.629500214854), TerseVector(-2.1932815453830e-03, -2.1751799585364e-04,  5.9556516201114e-04))
,   BodyState(  175200.0, TerseVector(-24.981690229261, -16.204012851426,   2.466457544298), TerseVector( 1.8193989149580e-03, -2.6765419531201e-03, -1.3848283502247e-03))
,   BodyState(  204400.0, TerseVector( 42.530187039511,   0.845935508021, -12.554907527683), TerseVector( 6.5059779150669e-04,  2.2725657282262e-03,  5.1133743202822e-04))
,   BodyState(  233600.0, TerseVector( 13.999526486822,  44.462363044894,   9.669418486465), TerseVector(-2.1079296569252e-03,  1.7533423831993e-04,  6.9128485798076e-04))
,   BodyState(  262800.0, TerseVector(-29.184024803031,  -7.371243995762,   6.493275957928), TerseVector( 9.3581363109681e-04, -3.0610357109184e-03, -1.2364201089345e-03))
,   BodyState(  292000.0, TerseVector( 39.831980671753,  -6.078405766765, -13.909815358656), TerseVector( 1.1117769689167e-03,  2.2362097830152e-03,  3.6230548231153e-04))
,   BodyState(  321200.0, TerseVector( 20.294955108476,  43.417190420251,   7.450091985932), TerseVector(-1.9742157451535e-03,  5.3102050468554e-04,  7.5938408813008e-04))
,   BodyState(  350400.0, TerseVector(-30.669992302160,   2.318743558955,   9.973480913858), TerseVector( 4.5605107450676e-05, -3.1308219926928e-03, -9.9066533301924e-04))
,   BodyState(  379600.0, TerseVector( 35.626122155983, -12.897647509224, -14.777586508444), TerseVector( 1.6015684949743e-03,  2.1171931182284e-03,  1.8002516202204e-04))
,   BodyState(  408800.0, TerseVector( 26.133186148561,  41.232139187599,   5.006401326220), TerseVector(-1.7857704419579e-03,  8.6046232702817e-04,  8.0614690298954e-04))
,   BodyState(  438000.0, TerseVector(-29.576740229230,  11.863535943587,  12.631323039872), TerseVector(-7.2292830060955e-04, -2.9587820140709e-03, -7.0824296450300e-04))
,   BodyState(  467200.0, TerseVector( 29.910805787391, -19.159019294000, -15.013363865194), TerseVector( 2.0871080437997e-03,  1.8848372554514e-03, -3.8528655083926e-05))
,   BodyState(  496400.0, TerseVector( 31.375957451819,  38.050372720763,   2.433138343754), TerseVector(-1.5546055556611e-03,  1.1699815465629e-03,  8.3565439266001e-04))
,   BodyState(  525600.0, TerseVector(-26.360071336928,  20.662505904952,  14.414696258958), TerseVector(-1.3142373118349e-03, -2.6236647854842e-03, -4.2542017598193e-04))
,   BodyState(  554800.0, TerseVector( 22.599441488648, -24.508879898306, -14.484045731468), TerseVector( 2.5454108304806e-03,  1.4917058755191e-03, -3.0243665086079e-04))
,   BodyState(  584000.0, TerseVector( 35.877864013014,  33.894226366071,  -0.224524636277), TerseVector(-1.2941245730845e-03,  1.4560427668319e-03,  8.4762160640137e-04))
,   BodyState(  613200.0, TerseVector(-21.538149762417,  28.204068269761,  15.321973799534), TerseVector(-1.7312117409010e-03, -2.1939631314577e-03, -1.6316913275180e-04))
,   BodyState(  642400.0, TerseVector( 13.971521374415, -28.339941764789, -13.083792871886), TerseVector( 2.9334630526035e-03,  9.1860931752944e-04, -5.9939422488627e-04))
,   BodyState(  671600.0, TerseVector( 39.526942044143,  28.939897360110,  -2.872799527539), TerseVector(-1.0068481658095e-03,  1.7021132888090e-03,  8.3578230511981e-04))
,   BodyState(  700800.0, TerseVector(-15.576200701394,  34.399412961275,  15.466033737854), TerseVector(-2.0098814612884e-03, -1.7191109825989e-03,  7.0414782780416e-05))
,   BodyState(  730000.0, TerseVector(  4.243252837090, -30.118201690825, -10.707441231349), TerseVector( 3.1725847067411e-03,  1.6098461202270e-04, -9.0672150593868e-04))
)
private val plutoCache = hashMapOf<Int, List<BodyGravCalc>>()
private val plutoCacheLock = KLock()

//---------------------------------------------------------------------------------------
// Models for Jupiter's four largest moons.

private val rotationJupEqj = RotationMatrix(
     9.99432765338654e-01, -3.36771074697641e-02,  0.00000000000000e+00,
     3.03959428906285e-02,  9.02057912352809e-01,  4.30543388542295e-01,
    -1.44994559663353e-02, -4.30299169409101e-01,  9.02569881273754e-01
)

private val jupiterMoonModel: Array<JupiterMoon> = arrayOf(
    // [0] Io
    JupiterMoon(
         2.8248942843381399e-07,
         1.4462132960212239e+00,
         3.5515522861824000e+00,
        arrayOf(  // a
            VsopTerm( 0.0028210960212903,  0.0000000000000000e+00,  0.0000000000000000e+00)
        ),
        arrayOf(  // l
            VsopTerm(-0.0001925258348666,  4.9369589722644998e+00,  1.3584836583050000e-02),
            VsopTerm(-0.0000970803596076,  4.3188796477322002e+00,  1.3034138432430000e-02),
            VsopTerm(-0.0000898817416500,  1.9080016428616999e+00,  3.0506486715799999e-03),
            VsopTerm(-0.0000553101050262,  1.4936156681568999e+00,  1.2938928911549999e-02)
        ),
        arrayOf(  // z
            VsopTerm( 0.0041510849668155,  4.0899396355450000e+00, -1.2906864146660001e-02),
            VsopTerm( 0.0006260521444113,  1.4461888986270000e+00,  3.5515522949801999e+00),
            VsopTerm( 0.0000352747346169,  2.1256287034577999e+00,  1.2727416566999999e-04)
        ),
        arrayOf(  // zeta
            VsopTerm( 0.0003142172466014,  2.7964219722923001e+00, -2.3150960980000000e-03),
            VsopTerm( 0.0000904169207946,  1.0477061879627001e+00, -5.6920638196000003e-04)
        )
    ),

    // [1] Europa
    JupiterMoon(
         2.8248327439289299e-07,
        -3.7352634374713622e-01,
         1.7693227111234699e+00,
        arrayOf(  // a
            VsopTerm( 0.0044871037804314,  0.0000000000000000e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0000004324367498,  1.8196456062910000e+00,  1.7822295777568000e+00)
        ),
        arrayOf(  // l
            VsopTerm( 0.0008576433172936,  4.3188693178264002e+00,  1.3034138308049999e-02),
            VsopTerm( 0.0004549582875086,  1.4936531751079001e+00,  1.2938928819619999e-02),
            VsopTerm( 0.0003248939825174,  1.8196494533458001e+00,  1.7822295777568000e+00),
            VsopTerm(-0.0003074250079334,  4.9377037005910998e+00,  1.3584832867240000e-02),
            VsopTerm( 0.0001982386144784,  1.9079869054759999e+00,  3.0510121286900001e-03),
            VsopTerm( 0.0001834063551804,  2.1402853388529000e+00,  1.4500978933800000e-03),
            VsopTerm(-0.0001434383188452,  5.6222140366630002e+00,  8.9111478887838003e-01),
            VsopTerm(-0.0000771939140944,  4.3002724372349999e+00,  2.6733443704265998e+00)
        ),
        arrayOf(  // z
            VsopTerm(-0.0093589104136341,  4.0899396509038999e+00, -1.2906864146660001e-02),
            VsopTerm( 0.0002988994545555,  5.9097265185595003e+00,  1.7693227079461999e+00),
            VsopTerm( 0.0002139036390350,  2.1256289300016000e+00,  1.2727418406999999e-04),
            VsopTerm( 0.0001980963564781,  2.7435168292649998e+00,  6.7797343008999997e-04),
            VsopTerm( 0.0001210388158965,  5.5839943711203004e+00,  3.2056614899999997e-05),
            VsopTerm( 0.0000837042048393,  1.6094538368039000e+00, -9.0402165808846002e-01),
            VsopTerm( 0.0000823525166369,  1.4461887708689001e+00,  3.5515522949801999e+00)
        ),
        arrayOf(  // zeta
            VsopTerm( 0.0040404917832303,  1.0477063169425000e+00, -5.6920640539999997e-04),
            VsopTerm( 0.0002200421034564,  3.3368857864364001e+00, -1.2491307306999999e-04),
            VsopTerm( 0.0001662544744719,  2.4134862374710999e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0000590282470983,  5.9719930968366004e+00, -3.0561602250000000e-05)
        )
    ),

    // [2] Ganymede
    JupiterMoon(
         2.8249818418472298e-07,
         2.8740893911433479e-01,
         8.7820792358932798e-01,
        arrayOf(  // a
            VsopTerm( 0.0071566594572575,  0.0000000000000000e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0000013930299110,  1.1586745884981000e+00,  2.6733443704265998e+00)
        ),
        arrayOf(  // l
            VsopTerm( 0.0002310797886226,  2.1402987195941998e+00,  1.4500978438400001e-03),
            VsopTerm(-0.0001828635964118,  4.3188672736968003e+00,  1.3034138282630000e-02),
            VsopTerm( 0.0001512378778204,  4.9373102372298003e+00,  1.3584834812520000e-02),
            VsopTerm(-0.0001163720969778,  4.3002659861490002e+00,  2.6733443704265998e+00),
            VsopTerm(-0.0000955478069846,  1.4936612842567001e+00,  1.2938928798570001e-02),
            VsopTerm( 0.0000815246854464,  5.6222137132535002e+00,  8.9111478887838003e-01),
            VsopTerm(-0.0000801219679602,  1.2995922951532000e+00,  1.0034433456728999e+00),
            VsopTerm(-0.0000607017260182,  6.4978769669238001e-01,  5.0172167043264004e-01)
        ),
        arrayOf(  // z
            VsopTerm( 0.0014289811307319,  2.1256295942738999e+00,  1.2727413029000001e-04),
            VsopTerm( 0.0007710931226760,  5.5836330003496002e+00,  3.2064341100000001e-05),
            VsopTerm( 0.0005925911780766,  4.0899396636447998e+00, -1.2906864146660001e-02),
            VsopTerm( 0.0002045597496146,  5.2713683670371996e+00, -1.2523544076106000e-01),
            VsopTerm( 0.0001785118648258,  2.8743156721063001e-01,  8.7820792442520001e-01),
            VsopTerm( 0.0001131999784893,  1.4462127277818000e+00,  3.5515522949801999e+00),
            VsopTerm(-0.0000658778169210,  2.2702423990985001e+00, -1.7951364394536999e+00),
            VsopTerm( 0.0000497058888328,  5.9096792204858000e+00,  1.7693227129285001e+00)
        ),
        arrayOf(  // zeta
            VsopTerm( 0.0015932721570848,  3.3368862796665000e+00, -1.2491307058000000e-04),
            VsopTerm( 0.0008533093128905,  2.4133881688166001e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0003513347911037,  5.9720789850126996e+00, -3.0561017709999999e-05),
            VsopTerm(-0.0001441929255483,  1.0477061764435001e+00, -5.6920632124000004e-04)
        )
    ),

    // [3] Callisto
    JupiterMoon(
         2.8249214488990899e-07,
        -3.6203412913757038e-01,
         3.7648623343382798e-01,
        arrayOf(  // a
            VsopTerm( 0.0125879701715314,  0.0000000000000000e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0000035952049470,  6.4965776007116005e-01,  5.0172168165034003e-01),
            VsopTerm( 0.0000027580210652,  1.8084235781510001e+00,  3.1750660413359002e+00)
        ),
        arrayOf(  // l
            VsopTerm( 0.0005586040123824,  2.1404207189814999e+00,  1.4500979323100001e-03),
            VsopTerm(-0.0003805813868176,  2.7358844897852999e+00,  2.9729650620000000e-05),
            VsopTerm( 0.0002205152863262,  6.4979652596399995e-01,  5.0172167243580001e-01),
            VsopTerm( 0.0001877895151158,  1.8084787604004999e+00,  3.1750660413359002e+00),
            VsopTerm( 0.0000766916975242,  6.2720114319754998e+00,  1.3928364636651001e+00),
            VsopTerm( 0.0000747056855106,  1.2995916202344000e+00,  1.0034433456728999e+00)
        ),
        arrayOf(  // z
            VsopTerm( 0.0073755808467977,  5.5836071576083999e+00,  3.2065099140000001e-05),
            VsopTerm( 0.0002065924169942,  5.9209831565786004e+00,  3.7648624194703001e-01),
            VsopTerm( 0.0001589869764021,  2.8744006242622999e-01,  8.7820792442520001e-01),
            VsopTerm(-0.0001561131605348,  2.1257397865089001e+00,  1.2727441285000001e-04),
            VsopTerm( 0.0001486043380971,  1.4462134301023000e+00,  3.5515522949801999e+00),
            VsopTerm( 0.0000635073108731,  5.9096803285953996e+00,  1.7693227129285001e+00),
            VsopTerm( 0.0000599351698525,  4.1125517584797997e+00, -2.7985797954588998e+00),
            VsopTerm( 0.0000540660842731,  5.5390350845569003e+00,  2.8683408228299999e-03),
            VsopTerm(-0.0000489596900866,  4.6218149483337996e+00, -6.2695712529518999e-01)
        ),
        arrayOf(  // zeta
            VsopTerm( 0.0038422977898495,  2.4133922085556998e+00,  0.0000000000000000e+00),
            VsopTerm( 0.0022453891791894,  5.9721736773277003e+00, -3.0561255249999997e-05),
            VsopTerm(-0.0002604479450559,  3.3368746306408998e+00, -1.2491309972000001e-04),
            VsopTerm( 0.0000332112143230,  5.5604137742336999e+00,  2.9003768850700000e-03)
        )
    )
)

//---------------------------------------------------------------------------------------
// Constellation lookup table.

internal val constelNames: Array<ConstellationText> = arrayOf(
    ConstellationText("And", "Andromeda"           )    //  0
,   ConstellationText("Ant", "Antila"              )    //  1
,   ConstellationText("Aps", "Apus"                )    //  2
,   ConstellationText("Aql", "Aquila"              )    //  3
,   ConstellationText("Aqr", "Aquarius"            )    //  4
,   ConstellationText("Ara", "Ara"                 )    //  5
,   ConstellationText("Ari", "Aries"               )    //  6
,   ConstellationText("Aur", "Auriga"              )    //  7
,   ConstellationText("Boo", "Bootes"              )    //  8
,   ConstellationText("Cae", "Caelum"              )    //  9
,   ConstellationText("Cam", "Camelopardis"        )    // 10
,   ConstellationText("Cap", "Capricornus"         )    // 11
,   ConstellationText("Car", "Carina"              )    // 12
,   ConstellationText("Cas", "Cassiopeia"          )    // 13
,   ConstellationText("Cen", "Centaurus"           )    // 14
,   ConstellationText("Cep", "Cepheus"             )    // 15
,   ConstellationText("Cet", "Cetus"               )    // 16
,   ConstellationText("Cha", "Chamaeleon"          )    // 17
,   ConstellationText("Cir", "Circinus"            )    // 18
,   ConstellationText("CMa", "Canis Major"         )    // 19
,   ConstellationText("CMi", "Canis Minor"         )    // 20
,   ConstellationText("Cnc", "Cancer"              )    // 21
,   ConstellationText("Col", "Columba"             )    // 22
,   ConstellationText("Com", "Coma Berenices"      )    // 23
,   ConstellationText("CrA", "Corona Australis"    )    // 24
,   ConstellationText("CrB", "Corona Borealis"     )    // 25
,   ConstellationText("Crt", "Crater"              )    // 26
,   ConstellationText("Cru", "Crux"                )    // 27
,   ConstellationText("Crv", "Corvus"              )    // 28
,   ConstellationText("CVn", "Canes Venatici"      )    // 29
,   ConstellationText("Cyg", "Cygnus"              )    // 30
,   ConstellationText("Del", "Delphinus"           )    // 31
,   ConstellationText("Dor", "Dorado"              )    // 32
,   ConstellationText("Dra", "Draco"               )    // 33
,   ConstellationText("Equ", "Equuleus"            )    // 34
,   ConstellationText("Eri", "Eridanus"            )    // 35
,   ConstellationText("For", "Fornax"              )    // 36
,   ConstellationText("Gem", "Gemini"              )    // 37
,   ConstellationText("Gru", "Grus"                )    // 38
,   ConstellationText("Her", "Hercules"            )    // 39
,   ConstellationText("Hor", "Horologium"          )    // 40
,   ConstellationText("Hya", "Hydra"               )    // 41
,   ConstellationText("Hyi", "Hydrus"              )    // 42
,   ConstellationText("Ind", "Indus"               )    // 43
,   ConstellationText("Lac", "Lacerta"             )    // 44
,   ConstellationText("Leo", "Leo"                 )    // 45
,   ConstellationText("Lep", "Lepus"               )    // 46
,   ConstellationText("Lib", "Libra"               )    // 47
,   ConstellationText("LMi", "Leo Minor"           )    // 48
,   ConstellationText("Lup", "Lupus"               )    // 49
,   ConstellationText("Lyn", "Lynx"                )    // 50
,   ConstellationText("Lyr", "Lyra"                )    // 51
,   ConstellationText("Men", "Mensa"               )    // 52
,   ConstellationText("Mic", "Microscopium"        )    // 53
,   ConstellationText("Mon", "Monoceros"           )    // 54
,   ConstellationText("Mus", "Musca"               )    // 55
,   ConstellationText("Nor", "Norma"               )    // 56
,   ConstellationText("Oct", "Octans"              )    // 57
,   ConstellationText("Oph", "Ophiuchus"           )    // 58
,   ConstellationText("Ori", "Orion"               )    // 59
,   ConstellationText("Pav", "Pavo"                )    // 60
,   ConstellationText("Peg", "Pegasus"             )    // 61
,   ConstellationText("Per", "Perseus"             )    // 62
,   ConstellationText("Phe", "Phoenix"             )    // 63
,   ConstellationText("Pic", "Pictor"              )    // 64
,   ConstellationText("PsA", "Pisces Austrinus"    )    // 65
,   ConstellationText("Psc", "Pisces"              )    // 66
,   ConstellationText("Pup", "Puppis"              )    // 67
,   ConstellationText("Pyx", "Pyxis"               )    // 68
,   ConstellationText("Ret", "Reticulum"           )    // 69
,   ConstellationText("Scl", "Sculptor"            )    // 70
,   ConstellationText("Sco", "Scorpius"            )    // 71
,   ConstellationText("Sct", "Scutum"              )    // 72
,   ConstellationText("Ser", "Serpens"             )    // 73
,   ConstellationText("Sex", "Sextans"             )    // 74
,   ConstellationText("Sge", "Sagitta"             )    // 75
,   ConstellationText("Sgr", "Sagittarius"         )    // 76
,   ConstellationText("Tau", "Taurus"              )    // 77
,   ConstellationText("Tel", "Telescopium"         )    // 78
,   ConstellationText("TrA", "Triangulum Australe" )    // 79
,   ConstellationText("Tri", "Triangulum"          )    // 80
,   ConstellationText("Tuc", "Tucana"              )    // 81
,   ConstellationText("UMa", "Ursa Major"          )    // 82
,   ConstellationText("UMi", "Ursa Minor"          )    // 83
,   ConstellationText("Vel", "Vela"                )    // 84
,   ConstellationText("Vir", "Virgo"               )    // 85
,   ConstellationText("Vol", "Volans"              )    // 86
,   ConstellationText("Vul", "Vulpecula"           )    // 87
)

internal val constelBounds: Array<ConstellationBoundary> = arrayOf(
    ConstellationBoundary(83,    0.0, 8640.0,  2112.0)    // UMi
,   ConstellationBoundary(83, 2880.0, 5220.0,  2076.0)    // UMi
,   ConstellationBoundary(83, 7560.0, 8280.0,  2068.0)    // UMi
,   ConstellationBoundary(83, 6480.0, 7560.0,  2064.0)    // UMi
,   ConstellationBoundary(15,    0.0, 2880.0,  2040.0)    // Cep
,   ConstellationBoundary(10, 3300.0, 3840.0,  1968.0)    // Cam
,   ConstellationBoundary(15,    0.0, 1800.0,  1920.0)    // Cep
,   ConstellationBoundary(10, 3840.0, 5220.0,  1920.0)    // Cam
,   ConstellationBoundary(83, 6300.0, 6480.0,  1920.0)    // UMi
,   ConstellationBoundary(33, 7260.0, 7560.0,  1920.0)    // Dra
,   ConstellationBoundary(15,    0.0, 1263.0,  1848.0)    // Cep
,   ConstellationBoundary(10, 4140.0, 4890.0,  1848.0)    // Cam
,   ConstellationBoundary(83, 5952.0, 6300.0,  1800.0)    // UMi
,   ConstellationBoundary(15, 7260.0, 7440.0,  1800.0)    // Cep
,   ConstellationBoundary(10, 2868.0, 3300.0,  1764.0)    // Cam
,   ConstellationBoundary(33, 3300.0, 4080.0,  1764.0)    // Dra
,   ConstellationBoundary(83, 4680.0, 5952.0,  1680.0)    // UMi
,   ConstellationBoundary(13, 1116.0, 1230.0,  1632.0)    // Cas
,   ConstellationBoundary(33, 7350.0, 7440.0,  1608.0)    // Dra
,   ConstellationBoundary(33, 4080.0, 4320.0,  1596.0)    // Dra
,   ConstellationBoundary(15,    0.0,  120.0,  1584.0)    // Cep
,   ConstellationBoundary(83, 5040.0, 5640.0,  1584.0)    // UMi
,   ConstellationBoundary(15, 8490.0, 8640.0,  1584.0)    // Cep
,   ConstellationBoundary(33, 4320.0, 4860.0,  1536.0)    // Dra
,   ConstellationBoundary(33, 4860.0, 5190.0,  1512.0)    // Dra
,   ConstellationBoundary(15, 8340.0, 8490.0,  1512.0)    // Cep
,   ConstellationBoundary(10, 2196.0, 2520.0,  1488.0)    // Cam
,   ConstellationBoundary(33, 7200.0, 7350.0,  1476.0)    // Dra
,   ConstellationBoundary(15, 7393.2, 7416.0,  1462.0)    // Cep
,   ConstellationBoundary(10, 2520.0, 2868.0,  1440.0)    // Cam
,   ConstellationBoundary(82, 2868.0, 3030.0,  1440.0)    // UMa
,   ConstellationBoundary(33, 7116.0, 7200.0,  1428.0)    // Dra
,   ConstellationBoundary(15, 7200.0, 7393.2,  1428.0)    // Cep
,   ConstellationBoundary(15, 8232.0, 8340.0,  1418.0)    // Cep
,   ConstellationBoundary(13,    0.0,  876.0,  1404.0)    // Cas
,   ConstellationBoundary(33, 6990.0, 7116.0,  1392.0)    // Dra
,   ConstellationBoundary(13,  612.0,  687.0,  1380.0)    // Cas
,   ConstellationBoundary(13,  876.0, 1116.0,  1368.0)    // Cas
,   ConstellationBoundary(10, 1116.0, 1140.0,  1368.0)    // Cam
,   ConstellationBoundary(15, 8034.0, 8232.0,  1350.0)    // Cep
,   ConstellationBoundary(10, 1800.0, 2196.0,  1344.0)    // Cam
,   ConstellationBoundary(82, 5052.0, 5190.0,  1332.0)    // UMa
,   ConstellationBoundary(33, 5190.0, 6990.0,  1332.0)    // Dra
,   ConstellationBoundary(10, 1140.0, 1200.0,  1320.0)    // Cam
,   ConstellationBoundary(15, 7968.0, 8034.0,  1320.0)    // Cep
,   ConstellationBoundary(15, 7416.0, 7908.0,  1316.0)    // Cep
,   ConstellationBoundary(13,    0.0,  612.0,  1296.0)    // Cas
,   ConstellationBoundary(50, 2196.0, 2340.0,  1296.0)    // Lyn
,   ConstellationBoundary(82, 4350.0, 4860.0,  1272.0)    // UMa
,   ConstellationBoundary(33, 5490.0, 5670.0,  1272.0)    // Dra
,   ConstellationBoundary(15, 7908.0, 7968.0,  1266.0)    // Cep
,   ConstellationBoundary(10, 1200.0, 1800.0,  1260.0)    // Cam
,   ConstellationBoundary(13, 8232.0, 8400.0,  1260.0)    // Cas
,   ConstellationBoundary(33, 5670.0, 6120.0,  1236.0)    // Dra
,   ConstellationBoundary(62,  735.0,  906.0,  1212.0)    // Per
,   ConstellationBoundary(33, 6120.0, 6564.0,  1212.0)    // Dra
,   ConstellationBoundary(13,    0.0,  492.0,  1200.0)    // Cas
,   ConstellationBoundary(62,  492.0,  600.0,  1200.0)    // Per
,   ConstellationBoundary(50, 2340.0, 2448.0,  1200.0)    // Lyn
,   ConstellationBoundary(13, 8400.0, 8640.0,  1200.0)    // Cas
,   ConstellationBoundary(82, 4860.0, 5052.0,  1164.0)    // UMa
,   ConstellationBoundary(13,    0.0,  402.0,  1152.0)    // Cas
,   ConstellationBoundary(13, 8490.0, 8640.0,  1152.0)    // Cas
,   ConstellationBoundary(39, 6543.0, 6564.0,  1140.0)    // Her
,   ConstellationBoundary(33, 6564.0, 6870.0,  1140.0)    // Dra
,   ConstellationBoundary(30, 6870.0, 6900.0,  1140.0)    // Cyg
,   ConstellationBoundary(62,  600.0,  735.0,  1128.0)    // Per
,   ConstellationBoundary(82, 3030.0, 3300.0,  1128.0)    // UMa
,   ConstellationBoundary(13,   60.0,  312.0,  1104.0)    // Cas
,   ConstellationBoundary(82, 4320.0, 4350.0,  1080.0)    // UMa
,   ConstellationBoundary(50, 2448.0, 2652.0,  1068.0)    // Lyn
,   ConstellationBoundary(30, 7887.0, 7908.0,  1056.0)    // Cyg
,   ConstellationBoundary(30, 7875.0, 7887.0,  1050.0)    // Cyg
,   ConstellationBoundary(30, 6900.0, 6984.0,  1044.0)    // Cyg
,   ConstellationBoundary(82, 3300.0, 3660.0,  1008.0)    // UMa
,   ConstellationBoundary(82, 3660.0, 3882.0,   960.0)    // UMa
,   ConstellationBoundary( 8, 5556.0, 5670.0,   960.0)    // Boo
,   ConstellationBoundary(39, 5670.0, 5880.0,   960.0)    // Her
,   ConstellationBoundary(50, 3330.0, 3450.0,   954.0)    // Lyn
,   ConstellationBoundary( 0,    0.0,  906.0,   882.0)    // And
,   ConstellationBoundary(62,  906.0,  924.0,   882.0)    // Per
,   ConstellationBoundary(51, 6969.0, 6984.0,   876.0)    // Lyr
,   ConstellationBoundary(62, 1620.0, 1689.0,   864.0)    // Per
,   ConstellationBoundary(30, 7824.0, 7875.0,   864.0)    // Cyg
,   ConstellationBoundary(44, 7875.0, 7920.0,   864.0)    // Lac
,   ConstellationBoundary( 7, 2352.0, 2652.0,   852.0)    // Aur
,   ConstellationBoundary(50, 2652.0, 2790.0,   852.0)    // Lyn
,   ConstellationBoundary( 0,    0.0,  720.0,   840.0)    // And
,   ConstellationBoundary(44, 7920.0, 8214.0,   840.0)    // Lac
,   ConstellationBoundary(44, 8214.0, 8232.0,   828.0)    // Lac
,   ConstellationBoundary( 0, 8232.0, 8460.0,   828.0)    // And
,   ConstellationBoundary(62,  924.0,  978.0,   816.0)    // Per
,   ConstellationBoundary(82, 3882.0, 3960.0,   816.0)    // UMa
,   ConstellationBoundary(29, 4320.0, 4440.0,   816.0)    // CVn
,   ConstellationBoundary(50, 2790.0, 3330.0,   804.0)    // Lyn
,   ConstellationBoundary(48, 3330.0, 3558.0,   804.0)    // LMi
,   ConstellationBoundary( 0,  258.0,  507.0,   792.0)    // And
,   ConstellationBoundary( 8, 5466.0, 5556.0,   792.0)    // Boo
,   ConstellationBoundary( 0, 8460.0, 8550.0,   770.0)    // And
,   ConstellationBoundary(29, 4440.0, 4770.0,   768.0)    // CVn
,   ConstellationBoundary( 0, 8550.0, 8640.0,   752.0)    // And
,   ConstellationBoundary(29, 5025.0, 5052.0,   738.0)    // CVn
,   ConstellationBoundary(80,  870.0,  978.0,   736.0)    // Tri
,   ConstellationBoundary(62,  978.0, 1620.0,   736.0)    // Per
,   ConstellationBoundary( 7, 1620.0, 1710.0,   720.0)    // Aur
,   ConstellationBoundary(51, 6543.0, 6969.0,   720.0)    // Lyr
,   ConstellationBoundary(82, 3960.0, 4320.0,   696.0)    // UMa
,   ConstellationBoundary(30, 7080.0, 7530.0,   696.0)    // Cyg
,   ConstellationBoundary( 7, 1710.0, 2118.0,   684.0)    // Aur
,   ConstellationBoundary(48, 3558.0, 3780.0,   684.0)    // LMi
,   ConstellationBoundary(29, 4770.0, 5025.0,   684.0)    // CVn
,   ConstellationBoundary( 0,    0.0,   24.0,   672.0)    // And
,   ConstellationBoundary(80,  507.0,  600.0,   672.0)    // Tri
,   ConstellationBoundary( 7, 2118.0, 2352.0,   672.0)    // Aur
,   ConstellationBoundary(37, 2838.0, 2880.0,   672.0)    // Gem
,   ConstellationBoundary(30, 7530.0, 7824.0,   672.0)    // Cyg
,   ConstellationBoundary(30, 6933.0, 7080.0,   660.0)    // Cyg
,   ConstellationBoundary(80,  690.0,  870.0,   654.0)    // Tri
,   ConstellationBoundary(25, 5820.0, 5880.0,   648.0)    // CrB
,   ConstellationBoundary( 8, 5430.0, 5466.0,   624.0)    // Boo
,   ConstellationBoundary(25, 5466.0, 5820.0,   624.0)    // CrB
,   ConstellationBoundary(51, 6612.0, 6792.0,   624.0)    // Lyr
,   ConstellationBoundary(48, 3870.0, 3960.0,   612.0)    // LMi
,   ConstellationBoundary(51, 6792.0, 6933.0,   612.0)    // Lyr
,   ConstellationBoundary(80,  600.0,  690.0,   600.0)    // Tri
,   ConstellationBoundary(66,  258.0,  306.0,   570.0)    // Psc
,   ConstellationBoundary(48, 3780.0, 3870.0,   564.0)    // LMi
,   ConstellationBoundary(87, 7650.0, 7710.0,   564.0)    // Vul
,   ConstellationBoundary(77, 2052.0, 2118.0,   548.0)    // Tau
,   ConstellationBoundary( 0,   24.0,   51.0,   528.0)    // And
,   ConstellationBoundary(73, 5730.0, 5772.0,   528.0)    // Ser
,   ConstellationBoundary(37, 2118.0, 2238.0,   516.0)    // Gem
,   ConstellationBoundary(87, 7140.0, 7290.0,   510.0)    // Vul
,   ConstellationBoundary(87, 6792.0, 6930.0,   506.0)    // Vul
,   ConstellationBoundary( 0,   51.0,  306.0,   504.0)    // And
,   ConstellationBoundary(87, 7290.0, 7404.0,   492.0)    // Vul
,   ConstellationBoundary(37, 2811.0, 2838.0,   480.0)    // Gem
,   ConstellationBoundary(87, 7404.0, 7650.0,   468.0)    // Vul
,   ConstellationBoundary(87, 6930.0, 7140.0,   460.0)    // Vul
,   ConstellationBoundary( 6, 1182.0, 1212.0,   456.0)    // Ari
,   ConstellationBoundary(75, 6792.0, 6840.0,   444.0)    // Sge
,   ConstellationBoundary(59, 2052.0, 2076.0,   432.0)    // Ori
,   ConstellationBoundary(37, 2238.0, 2271.0,   420.0)    // Gem
,   ConstellationBoundary(75, 6840.0, 7140.0,   388.0)    // Sge
,   ConstellationBoundary(77, 1788.0, 1920.0,   384.0)    // Tau
,   ConstellationBoundary(39, 5730.0, 5790.0,   384.0)    // Her
,   ConstellationBoundary(75, 7140.0, 7290.0,   378.0)    // Sge
,   ConstellationBoundary(77, 1662.0, 1788.0,   372.0)    // Tau
,   ConstellationBoundary(77, 1920.0, 2016.0,   372.0)    // Tau
,   ConstellationBoundary(23, 4620.0, 4860.0,   360.0)    // Com
,   ConstellationBoundary(39, 6210.0, 6570.0,   344.0)    // Her
,   ConstellationBoundary(23, 4272.0, 4620.0,   336.0)    // Com
,   ConstellationBoundary(37, 2700.0, 2811.0,   324.0)    // Gem
,   ConstellationBoundary(39, 6030.0, 6210.0,   308.0)    // Her
,   ConstellationBoundary(61,    0.0,   51.0,   300.0)    // Peg
,   ConstellationBoundary(77, 2016.0, 2076.0,   300.0)    // Tau
,   ConstellationBoundary(37, 2520.0, 2700.0,   300.0)    // Gem
,   ConstellationBoundary(61, 7602.0, 7680.0,   300.0)    // Peg
,   ConstellationBoundary(37, 2271.0, 2496.0,   288.0)    // Gem
,   ConstellationBoundary(39, 6570.0, 6792.0,   288.0)    // Her
,   ConstellationBoundary(31, 7515.0, 7578.0,   284.0)    // Del
,   ConstellationBoundary(61, 7578.0, 7602.0,   284.0)    // Peg
,   ConstellationBoundary(45, 4146.0, 4272.0,   264.0)    // Leo
,   ConstellationBoundary(59, 2247.0, 2271.0,   240.0)    // Ori
,   ConstellationBoundary(37, 2496.0, 2520.0,   240.0)    // Gem
,   ConstellationBoundary(21, 2811.0, 2853.0,   240.0)    // Cnc
,   ConstellationBoundary(61, 8580.0, 8640.0,   240.0)    // Peg
,   ConstellationBoundary( 6,  600.0, 1182.0,   238.0)    // Ari
,   ConstellationBoundary(31, 7251.0, 7308.0,   204.0)    // Del
,   ConstellationBoundary( 8, 4860.0, 5430.0,   192.0)    // Boo
,   ConstellationBoundary(61, 8190.0, 8580.0,   180.0)    // Peg
,   ConstellationBoundary(21, 2853.0, 3330.0,   168.0)    // Cnc
,   ConstellationBoundary(45, 3330.0, 3870.0,   168.0)    // Leo
,   ConstellationBoundary(58, 6570.0, 6718.4,   150.0)    // Oph
,   ConstellationBoundary( 3, 6718.4, 6792.0,   150.0)    // Aql
,   ConstellationBoundary(31, 7500.0, 7515.0,   144.0)    // Del
,   ConstellationBoundary(20, 2520.0, 2526.0,   132.0)    // CMi
,   ConstellationBoundary(73, 6570.0, 6633.0,   108.0)    // Ser
,   ConstellationBoundary(39, 5790.0, 6030.0,    96.0)    // Her
,   ConstellationBoundary(58, 6570.0, 6633.0,    72.0)    // Oph
,   ConstellationBoundary(61, 7728.0, 7800.0,    66.0)    // Peg
,   ConstellationBoundary(66,    0.0,  720.0,    48.0)    // Psc
,   ConstellationBoundary(73, 6690.0, 6792.0,    48.0)    // Ser
,   ConstellationBoundary(31, 7308.0, 7500.0,    48.0)    // Del
,   ConstellationBoundary(34, 7500.0, 7680.0,    48.0)    // Equ
,   ConstellationBoundary(61, 7680.0, 7728.0,    48.0)    // Peg
,   ConstellationBoundary(61, 7920.0, 8190.0,    48.0)    // Peg
,   ConstellationBoundary(61, 7800.0, 7920.0,    42.0)    // Peg
,   ConstellationBoundary(20, 2526.0, 2592.0,    36.0)    // CMi
,   ConstellationBoundary(77, 1290.0, 1662.0,     0.0)    // Tau
,   ConstellationBoundary(59, 1662.0, 1680.0,     0.0)    // Ori
,   ConstellationBoundary(20, 2592.0, 2910.0,     0.0)    // CMi
,   ConstellationBoundary(85, 5280.0, 5430.0,     0.0)    // Vir
,   ConstellationBoundary(58, 6420.0, 6570.0,     0.0)    // Oph
,   ConstellationBoundary(16,  954.0, 1182.0,   -42.0)    // Cet
,   ConstellationBoundary(77, 1182.0, 1290.0,   -42.0)    // Tau
,   ConstellationBoundary(73, 5430.0, 5856.0,   -78.0)    // Ser
,   ConstellationBoundary(59, 1680.0, 1830.0,   -96.0)    // Ori
,   ConstellationBoundary(59, 2100.0, 2247.0,   -96.0)    // Ori
,   ConstellationBoundary(73, 6420.0, 6468.0,   -96.0)    // Ser
,   ConstellationBoundary(73, 6570.0, 6690.0,   -96.0)    // Ser
,   ConstellationBoundary( 3, 6690.0, 6792.0,   -96.0)    // Aql
,   ConstellationBoundary(66, 8190.0, 8580.0,   -96.0)    // Psc
,   ConstellationBoundary(45, 3870.0, 4146.0,  -144.0)    // Leo
,   ConstellationBoundary(85, 4146.0, 4260.0,  -144.0)    // Vir
,   ConstellationBoundary(66,    0.0,  120.0,  -168.0)    // Psc
,   ConstellationBoundary(66, 8580.0, 8640.0,  -168.0)    // Psc
,   ConstellationBoundary(85, 5130.0, 5280.0,  -192.0)    // Vir
,   ConstellationBoundary(58, 5730.0, 5856.0,  -192.0)    // Oph
,   ConstellationBoundary( 3, 7200.0, 7392.0,  -216.0)    // Aql
,   ConstellationBoundary( 4, 7680.0, 7872.0,  -216.0)    // Aqr
,   ConstellationBoundary(58, 6180.0, 6468.0,  -240.0)    // Oph
,   ConstellationBoundary(54, 2100.0, 2910.0,  -264.0)    // Mon
,   ConstellationBoundary(35, 1770.0, 1830.0,  -264.0)    // Eri
,   ConstellationBoundary(59, 1830.0, 2100.0,  -264.0)    // Ori
,   ConstellationBoundary(41, 2910.0, 3012.0,  -264.0)    // Hya
,   ConstellationBoundary(74, 3450.0, 3870.0,  -264.0)    // Sex
,   ConstellationBoundary(85, 4260.0, 4620.0,  -264.0)    // Vir
,   ConstellationBoundary(58, 6330.0, 6360.0,  -280.0)    // Oph
,   ConstellationBoundary( 3, 6792.0, 7200.0,  -288.8)    // Aql
,   ConstellationBoundary(35, 1740.0, 1770.0,  -348.0)    // Eri
,   ConstellationBoundary( 4, 7392.0, 7680.0,  -360.0)    // Aqr
,   ConstellationBoundary(73, 6180.0, 6570.0,  -384.0)    // Ser
,   ConstellationBoundary(72, 6570.0, 6792.0,  -384.0)    // Sct
,   ConstellationBoundary(41, 3012.0, 3090.0,  -408.0)    // Hya
,   ConstellationBoundary(58, 5856.0, 5895.0,  -438.0)    // Oph
,   ConstellationBoundary(41, 3090.0, 3270.0,  -456.0)    // Hya
,   ConstellationBoundary(26, 3870.0, 3900.0,  -456.0)    // Crt
,   ConstellationBoundary(71, 5856.0, 5895.0,  -462.0)    // Sco
,   ConstellationBoundary(47, 5640.0, 5730.0,  -480.0)    // Lib
,   ConstellationBoundary(28, 4530.0, 4620.0,  -528.0)    // Crv
,   ConstellationBoundary(85, 4620.0, 5130.0,  -528.0)    // Vir
,   ConstellationBoundary(41, 3270.0, 3510.0,  -576.0)    // Hya
,   ConstellationBoundary(16,  600.0,  954.0,  -585.2)    // Cet
,   ConstellationBoundary(35,  954.0, 1350.0,  -585.2)    // Eri
,   ConstellationBoundary(26, 3900.0, 4260.0,  -588.0)    // Crt
,   ConstellationBoundary(28, 4260.0, 4530.0,  -588.0)    // Crv
,   ConstellationBoundary(47, 5130.0, 5370.0,  -588.0)    // Lib
,   ConstellationBoundary(58, 5856.0, 6030.0,  -590.0)    // Oph
,   ConstellationBoundary(16,    0.0,  600.0,  -612.0)    // Cet
,   ConstellationBoundary(11, 7680.0, 7872.0,  -612.0)    // Cap
,   ConstellationBoundary( 4, 7872.0, 8580.0,  -612.0)    // Aqr
,   ConstellationBoundary(16, 8580.0, 8640.0,  -612.0)    // Cet
,   ConstellationBoundary(41, 3510.0, 3690.0,  -636.0)    // Hya
,   ConstellationBoundary(35, 1692.0, 1740.0,  -654.0)    // Eri
,   ConstellationBoundary(46, 1740.0, 2202.0,  -654.0)    // Lep
,   ConstellationBoundary(11, 7200.0, 7680.0,  -672.0)    // Cap
,   ConstellationBoundary(41, 3690.0, 3810.0,  -700.0)    // Hya
,   ConstellationBoundary(41, 4530.0, 5370.0,  -708.0)    // Hya
,   ConstellationBoundary(47, 5370.0, 5640.0,  -708.0)    // Lib
,   ConstellationBoundary(71, 5640.0, 5760.0,  -708.0)    // Sco
,   ConstellationBoundary(35, 1650.0, 1692.0,  -720.0)    // Eri
,   ConstellationBoundary(58, 6030.0, 6336.0,  -720.0)    // Oph
,   ConstellationBoundary(76, 6336.0, 6420.0,  -720.0)    // Sgr
,   ConstellationBoundary(41, 3810.0, 3900.0,  -748.0)    // Hya
,   ConstellationBoundary(19, 2202.0, 2652.0,  -792.0)    // CMa
,   ConstellationBoundary(41, 4410.0, 4530.0,  -792.0)    // Hya
,   ConstellationBoundary(41, 3900.0, 4410.0,  -840.0)    // Hya
,   ConstellationBoundary(36, 1260.0, 1350.0,  -864.0)    // For
,   ConstellationBoundary(68, 3012.0, 3372.0,  -882.0)    // Pyx
,   ConstellationBoundary(35, 1536.0, 1650.0,  -888.0)    // Eri
,   ConstellationBoundary(76, 6420.0, 6900.0,  -888.0)    // Sgr
,   ConstellationBoundary(65, 7680.0, 8280.0,  -888.0)    // PsA
,   ConstellationBoundary(70, 8280.0, 8400.0,  -888.0)    // Scl
,   ConstellationBoundary(36, 1080.0, 1260.0,  -950.0)    // For
,   ConstellationBoundary( 1, 3372.0, 3960.0,  -954.0)    // Ant
,   ConstellationBoundary(70,    0.0,  600.0,  -960.0)    // Scl
,   ConstellationBoundary(36,  600.0, 1080.0,  -960.0)    // For
,   ConstellationBoundary(35, 1392.0, 1536.0,  -960.0)    // Eri
,   ConstellationBoundary(70, 8400.0, 8640.0,  -960.0)    // Scl
,   ConstellationBoundary(14, 5100.0, 5370.0, -1008.0)    // Cen
,   ConstellationBoundary(49, 5640.0, 5760.0, -1008.0)    // Lup
,   ConstellationBoundary(71, 5760.0, 5911.5, -1008.0)    // Sco
,   ConstellationBoundary( 9, 1740.0, 1800.0, -1032.0)    // Cae
,   ConstellationBoundary(22, 1800.0, 2370.0, -1032.0)    // Col
,   ConstellationBoundary(67, 2880.0, 3012.0, -1032.0)    // Pup
,   ConstellationBoundary(35, 1230.0, 1392.0, -1056.0)    // Eri
,   ConstellationBoundary(71, 5911.5, 6420.0, -1092.0)    // Sco
,   ConstellationBoundary(24, 6420.0, 6900.0, -1092.0)    // CrA
,   ConstellationBoundary(76, 6900.0, 7320.0, -1092.0)    // Sgr
,   ConstellationBoundary(53, 7320.0, 7680.0, -1092.0)    // Mic
,   ConstellationBoundary(35, 1080.0, 1230.0, -1104.0)    // Eri
,   ConstellationBoundary( 9, 1620.0, 1740.0, -1116.0)    // Cae
,   ConstellationBoundary(49, 5520.0, 5640.0, -1152.0)    // Lup
,   ConstellationBoundary(63,    0.0,  840.0, -1156.0)    // Phe
,   ConstellationBoundary(35,  960.0, 1080.0, -1176.0)    // Eri
,   ConstellationBoundary(40, 1470.0, 1536.0, -1176.0)    // Hor
,   ConstellationBoundary( 9, 1536.0, 1620.0, -1176.0)    // Cae
,   ConstellationBoundary(38, 7680.0, 7920.0, -1200.0)    // Gru
,   ConstellationBoundary(67, 2160.0, 2880.0, -1218.0)    // Pup
,   ConstellationBoundary(84, 2880.0, 2940.0, -1218.0)    // Vel
,   ConstellationBoundary(35,  870.0,  960.0, -1224.0)    // Eri
,   ConstellationBoundary(40, 1380.0, 1470.0, -1224.0)    // Hor
,   ConstellationBoundary(63,    0.0,  660.0, -1236.0)    // Phe
,   ConstellationBoundary(12, 2160.0, 2220.0, -1260.0)    // Car
,   ConstellationBoundary(84, 2940.0, 3042.0, -1272.0)    // Vel
,   ConstellationBoundary(40, 1260.0, 1380.0, -1276.0)    // Hor
,   ConstellationBoundary(32, 1380.0, 1440.0, -1276.0)    // Dor
,   ConstellationBoundary(63,    0.0,  570.0, -1284.0)    // Phe
,   ConstellationBoundary(35,  780.0,  870.0, -1296.0)    // Eri
,   ConstellationBoundary(64, 1620.0, 1800.0, -1296.0)    // Pic
,   ConstellationBoundary(49, 5418.0, 5520.0, -1296.0)    // Lup
,   ConstellationBoundary(84, 3042.0, 3180.0, -1308.0)    // Vel
,   ConstellationBoundary(12, 2220.0, 2340.0, -1320.0)    // Car
,   ConstellationBoundary(14, 4260.0, 4620.0, -1320.0)    // Cen
,   ConstellationBoundary(49, 5100.0, 5418.0, -1320.0)    // Lup
,   ConstellationBoundary(56, 5418.0, 5520.0, -1320.0)    // Nor
,   ConstellationBoundary(32, 1440.0, 1560.0, -1356.0)    // Dor
,   ConstellationBoundary(84, 3180.0, 3960.0, -1356.0)    // Vel
,   ConstellationBoundary(14, 3960.0, 4050.0, -1356.0)    // Cen
,   ConstellationBoundary( 5, 6300.0, 6480.0, -1368.0)    // Ara
,   ConstellationBoundary(78, 6480.0, 7320.0, -1368.0)    // Tel
,   ConstellationBoundary(38, 7920.0, 8400.0, -1368.0)    // Gru
,   ConstellationBoundary(40, 1152.0, 1260.0, -1380.0)    // Hor
,   ConstellationBoundary(64, 1800.0, 1980.0, -1380.0)    // Pic
,   ConstellationBoundary(12, 2340.0, 2460.0, -1392.0)    // Car
,   ConstellationBoundary(63,    0.0,  480.0, -1404.0)    // Phe
,   ConstellationBoundary(35,  480.0,  780.0, -1404.0)    // Eri
,   ConstellationBoundary(63, 8400.0, 8640.0, -1404.0)    // Phe
,   ConstellationBoundary(32, 1560.0, 1650.0, -1416.0)    // Dor
,   ConstellationBoundary(56, 5520.0, 5911.5, -1440.0)    // Nor
,   ConstellationBoundary(43, 7320.0, 7680.0, -1440.0)    // Ind
,   ConstellationBoundary(64, 1980.0, 2160.0, -1464.0)    // Pic
,   ConstellationBoundary(18, 5460.0, 5520.0, -1464.0)    // Cir
,   ConstellationBoundary( 5, 5911.5, 5970.0, -1464.0)    // Ara
,   ConstellationBoundary(18, 5370.0, 5460.0, -1526.0)    // Cir
,   ConstellationBoundary( 5, 5970.0, 6030.0, -1526.0)    // Ara
,   ConstellationBoundary(64, 2160.0, 2460.0, -1536.0)    // Pic
,   ConstellationBoundary(12, 2460.0, 3252.0, -1536.0)    // Car
,   ConstellationBoundary(14, 4050.0, 4260.0, -1536.0)    // Cen
,   ConstellationBoundary(27, 4260.0, 4620.0, -1536.0)    // Cru
,   ConstellationBoundary(14, 4620.0, 5232.0, -1536.0)    // Cen
,   ConstellationBoundary(18, 4860.0, 4920.0, -1560.0)    // Cir
,   ConstellationBoundary( 5, 6030.0, 6060.0, -1560.0)    // Ara
,   ConstellationBoundary(40,  780.0, 1152.0, -1620.0)    // Hor
,   ConstellationBoundary(69, 1152.0, 1650.0, -1620.0)    // Ret
,   ConstellationBoundary(18, 5310.0, 5370.0, -1620.0)    // Cir
,   ConstellationBoundary( 5, 6060.0, 6300.0, -1620.0)    // Ara
,   ConstellationBoundary(60, 6300.0, 6480.0, -1620.0)    // Pav
,   ConstellationBoundary(81, 7920.0, 8400.0, -1620.0)    // Tuc
,   ConstellationBoundary(32, 1650.0, 2370.0, -1680.0)    // Dor
,   ConstellationBoundary(18, 4920.0, 5310.0, -1680.0)    // Cir
,   ConstellationBoundary(79, 5310.0, 6120.0, -1680.0)    // TrA
,   ConstellationBoundary(81,    0.0,  480.0, -1800.0)    // Tuc
,   ConstellationBoundary(42, 1260.0, 1650.0, -1800.0)    // Hyi
,   ConstellationBoundary(86, 2370.0, 3252.0, -1800.0)    // Vol
,   ConstellationBoundary(12, 3252.0, 4050.0, -1800.0)    // Car
,   ConstellationBoundary(55, 4050.0, 4920.0, -1800.0)    // Mus
,   ConstellationBoundary(60, 6480.0, 7680.0, -1800.0)    // Pav
,   ConstellationBoundary(43, 7680.0, 8400.0, -1800.0)    // Ind
,   ConstellationBoundary(81, 8400.0, 8640.0, -1800.0)    // Tuc
,   ConstellationBoundary(81,  270.0,  480.0, -1824.0)    // Tuc
,   ConstellationBoundary(42,    0.0, 1260.0, -1980.0)    // Hyi
,   ConstellationBoundary(17, 2760.0, 4920.0, -1980.0)    // Cha
,   ConstellationBoundary( 2, 4920.0, 6480.0, -1980.0)    // Aps
,   ConstellationBoundary(52, 1260.0, 2760.0, -2040.0)    // Men
,   ConstellationBoundary(57,    0.0, 8640.0, -2160.0)    // Oct
)

//---------------------------------------------------------------------------------------

internal val vsopModelMercury = VsopModel(vsopLonMercury, vsopLatMercury, vsopRadMercury)
internal val vsopModelVenus = VsopModel(vsopLonVenus, vsopLatVenus, vsopRadVenus)
internal val vsopModelEarth = VsopModel(vsopLonEarth, vsopLatEarth, vsopRadEarth)
internal val vsopModelMars = VsopModel(vsopLonMars, vsopLatMars, vsopRadMars)
internal val vsopModelJupiter = VsopModel(vsopLonJupiter, vsopLatJupiter, vsopRadJupiter)
internal val vsopModelSaturn = VsopModel(vsopLonSaturn, vsopLatSaturn, vsopRadSaturn)
internal val vsopModelUranus = VsopModel(vsopLonUranus, vsopLatUranus, vsopRadUranus)
internal val vsopModelNeptune = VsopModel(vsopLonNeptune, vsopLatNeptune, vsopRadNeptune)

// Create a rotation matrix for converting J2000 to B1875.
// Need to calculate the B1875 epoch. Based on this:
// https://en.wikipedia.org/wiki/Epoch_(astronomy)#Besselian_years
// B = 1900 + (JD - 2415020.31352) / 365.242198781
// I'm interested in using TT instead of JD, giving:
// B = 1900 + ((TT+2451545) - 2415020.31352) / 365.242198781
// B = 1900 + (TT + 36524.68648) / 365.242198781
// TT = 365.242198781*(B - 1900) - 36524.68648 = -45655.741449525
// But the Time constructor wants UT, not TT.
// Near that date, I get a historical correction of ut-tt = 3.2 seconds.
// That gives UT = -45655.74141261017 for the B1875 epoch,
// or 1874-12-31T18:12:21.950Z.
private val constelRot: RotationMatrix = rotationEqjEqd(Time(-45655.74141261017))
