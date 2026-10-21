// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************
//
// Modified by OsmAnd: a Kotlin copy of com.jwetherell.openmap.common.MGRSPoint in OsmAnd-java,
// with only the parts LocationParser calls: from an MGRS string to lat/lon. It extends UTMPoint
// directly, as ZonedUTMPoint, which it extends in java, adds nothing to that.

package net.osmand.shared.util.openmap

import kotlin.math.pow

/** A point of the MGRS grid, "33UXP0450005500", read from its string. */
internal class MGRSPoint : UTMPoint {

	/** The set origin column letters to use. */
	private val originColumnLetters = SET_ORIGIN_COLUMN_LETTERS

	/** The set origin row letters to use. */
	private val originRowLetters = SET_ORIGIN_ROW_LETTERS

	/** Throws [NumberFormatException] when [mgrsString] is not an MGRS coordinate. */
	constructor(mgrsString: String) : super() {
		setMGRS(mgrsString)
	}

	private fun setMGRS(mgrsString: String) {
		try {
			decode(mgrsString.uppercase()) // Just to make sure.
		} catch (e: IndexOutOfBoundsException) {
			throw NumberFormatException("MGRSPoint has bad string: $mgrsString")
		}
	}

	/** Sets the UTM parameters from an upper case MGRS string. */
	private fun decode(mgrs: String) {
		// the parts java splits it into by spaces, joined
		var mgrsString = mgrs.replace(" ", "")

		if (mgrsString.isEmpty()) {
			throw NumberFormatException("MGRSPoint coverting from nothing")
		}

		// Ensure an upper-case string
		mgrsString = mgrsString.uppercase()

		val length = mgrsString.length

		val sb = StringBuilder()
		var i = 0

		// get Zone number
		while (true) {
			val testChar = mgrsString[i]
			if (testChar.isLetter()) {
				break
			}
			if (i >= 2) {
				throw NumberFormatException("MGRSPoint bad conversion from: $mgrsString, first two characters need to be a number between 1-60.")
			}
			sb.append(testChar)
			i++
		}

		zoneNumber = sb.toString().toInt()

		if (zoneNumber < 1 || zoneNumber > 60) {
			throw NumberFormatException("MGRSPoint bad conversion from: $mgrsString, first two characters need to be a number between 1-60.")
		}

		if (i == 0 || i + 3 > length) {
			// A good MGRS string has to be 4-5 digits long,
			// ##AAA/#AAA at least.
			throw NumberFormatException("MGRSPoint bad conversion from: $mgrsString, MGRS string must be at least 4-5 digits long")
		}

		zoneLetter = mgrsString[i++]

		// Should we check the zone letter here? Why not.
		if (zoneLetter <= 'A' || zoneLetter == 'B' || zoneLetter == 'Y' || zoneLetter >= 'Z' || zoneLetter == 'I' || zoneLetter == 'O') {
			throw NumberFormatException("MGRSPoint zone letter $zoneLetter not handled: $mgrsString")
		}

		val hunK = mgrsString.substring(i, i + 2)
		i += 2

		// Validate, check the zone, make sure each letter is between A-Z, not I
		// or O
		val char1 = hunK[0]
		val char2 = hunK[1]
		if (char1 < 'A' || char2 < 'A' || char1 > 'Z' || char2 > 'Z' || char1 == 'I' || char2 == 'I' || char1 == 'O' || char2 == 'O') {
			throw NumberFormatException("MGRSPoint bad conversion from $mgrsString, invalid 100k designator")
		}

		val set = get100kSetForZone(zoneNumber)

		val east100k = getEastingFromChar(char1, set)
		var north100k = getNorthingFromChar(char2, set)

		// We have a bug where the northing may be 2000000 too low.
		// How do we know when to roll over?

		while (north100k < getMinNorthing(zoneLetter)) {
			north100k += 2000000
		}

		// calculate the char index for easting/northing separator
		val remainder = length - i

		if (remainder % 2 != 0) {
			throw NumberFormatException(
				"MGRSPoint has to have an even number \nof digits after the zone letter and two 100km letters - front \nhalf for easting meters, second half for \nnorthing meters" +
						mgrsString
			)
		}

		val sep = remainder / 2

		var sepEasting = 0f
		var sepNorthing = 0f

		if (sep > 0) {
			val accuracyBonus = 100000f / 10.0.pow(sep).toFloat()
			val sepEastingString = mgrsString.substring(i, i + sep)
			sepEasting = sepEastingString.toFloat() * accuracyBonus
			val sepNorthingString = mgrsString.substring(i + sep)
			sepNorthing = sepNorthingString.toFloat() * accuracyBonus
		}

		easting = (sepEasting + east100k).toDouble()
		northing = (sepNorthing + north100k).toDouble()
	}

	/**
	 * Given the first letter from a two-letter MGRS 100k zone, and given the
	 * MGRS table set for the zone number, figure out the easting value that
	 * should be added to the other, secondary easting value.
	 */
	private fun getEastingFromChar(e: Char, set: Int): Float {
		val baseCol = originColumnLetters
		// colOrigin is the letter at the origin of the set for the
		// column
		var curCol = baseCol[set - 1]
		var eastingValue = 100000f
		var rewindMarker = false

		while (curCol != e.code) {
			curCol++
			if (curCol == I) curCol++
			if (curCol == O) curCol++
			if (curCol > Z) {
				if (rewindMarker) {
					throw NumberFormatException("Bad character: $e")
				}
				curCol = A
				rewindMarker = true
			}
			eastingValue += 100000f
		}

		return eastingValue
	}

	/**
	 * Given the second letter from a two-letter MGRS 100k zone, and given the
	 * MGRS table set for the zone number, figure out the northing value that
	 * should be added to the other, secondary northing value. This does not count
	 * the 2000000 meters of northing each cycle of the letters adds; the zone letter
	 * tells how many to add.
	 */
	private fun getNorthingFromChar(n: Char, set: Int): Float {
		if (n > 'V') {
			throw NumberFormatException("MGRSPoint given invalid Northing $n")
		}

		val baseRow = originRowLetters
		// rowOrigin is the letter at the origin of the set for the
		// column
		var curRow = baseRow[set - 1]
		var northingValue = 0f
		var rewindMarker = false

		while (curRow != n.code) {
			curRow++
			if (curRow == I) curRow++
			if (curRow == O) curRow++
			// fixing a bug making whole application hang in this loop
			// when 'n' is a wrong character
			if (curRow > V) {
				if (rewindMarker) { // making sure that this loop ends
					throw NumberFormatException("Bad character: $n")
				}
				curRow = A
				rewindMarker = true
			}
			northingValue += 100000f
		}

		return northingValue
	}

	/**
	 * The minimum northing value of a MGRS zone, portted from Geotrans' c
	 * Latitude_Band_Value structure table.
	 */
	private fun getMinNorthing(zoneLetter: Char): Float {
		val northing = when (zoneLetter) {
			'C' -> 1100000.0f
			'D' -> 2000000.0f
			'E' -> 2800000.0f
			'F' -> 3700000.0f
			'G' -> 4600000.0f
			'H' -> 5500000.0f
			'J' -> 6400000.0f
			'K' -> 7300000.0f
			'L' -> 8200000.0f
			'M' -> 9100000.0f
			'N' -> 0.0f
			'P' -> 800000.0f
			'Q' -> 1700000.0f
			'R' -> 2600000.0f
			'S' -> 3500000.0f
			'T' -> 4400000.0f
			'U' -> 5300000.0f
			'V' -> 6200000.0f
			'W' -> 7000000.0f
			'X' -> 7900000.0f
			else -> -1.0f
		}
		if (northing >= 0.0) {
			return northing
		}
		throw NumberFormatException("Invalid zone letter: ${this.zoneLetter}")
	}

	/** On a WGS 84 ellipsoid. */
	override fun toLatLonPoint(): LatLonPoint? =
		UTMtoLL(Ellipsoid.WGS_84, northing, easting, zoneNumber, MGRSZoneToUTMZone(zoneLetter))

	/** Given a UTM zone number, figure out the MGRS 100K set it is in. */
	private fun get100kSetForZone(i: Int): Int {
		var set = i % NUM_100K_SETS
		if (set == 0) set = NUM_100K_SETS
		return set
	}

	companion object {
		/** UTM zones are grouped, and assigned to one of a group of 6 sets. */
		private const val NUM_100K_SETS = 6

		/** The column letters (for easting) of the lower left value, per set. */
		private val SET_ORIGIN_COLUMN_LETTERS = intArrayOf('A'.code, 'J'.code, 'S'.code, 'A'.code, 'J'.code, 'S'.code)

		/** The row letters (for northing) of the lower left value, per set. */
		private val SET_ORIGIN_ROW_LETTERS = intArrayOf('A'.code, 'F'.code, 'A'.code, 'F'.code, 'A'.code, 'F'.code)

		private const val A = 'A'.code
		private const val I = 'I'.code
		private const val O = 'O'.code
		private const val V = 'V'.code
		private const val Z = 'Z'.code

		/** 'N' for a zone letter from 'N' on, 'S' below; throws [NumberFormatException] on a letter MGRS has not. */
		fun MGRSZoneToUTMZone(mgrsZone: Char): Char {
			val zone = checkZone(mgrsZone)
			return if (zone.uppercaseChar() >= 'N') 'N' else 'S'
		}

		/** [zone] upper case; throws [NumberFormatException] on a letter MGRS has not. */
		fun checkZone(zone: Char): Char {
			val upper = zone.uppercaseChar()
			if (upper <= 'A' || upper == 'B' || upper == 'Y' || upper >= 'Z' || upper == 'I' || upper == 'O') {
				throw NumberFormatException("Invalid MGRSPoint zone letter: $upper")
			}
			return upper
		}
	}
}
