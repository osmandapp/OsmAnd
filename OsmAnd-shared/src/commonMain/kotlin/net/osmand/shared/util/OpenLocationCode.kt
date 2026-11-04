// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Modified by OsmAnd: a Kotlin copy of com.google.openlocationcode.OpenLocationCode 1.0.4, the
// library OsmAnd-java uses, with only the parts LocationParser calls.

package net.osmand.shared.util

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * An Open Location Code: a place as a short string of letters and digits, "8FVC9G8F+6X". A full
 * code is a place on its own; a short one, "9G8F+6X", is recovered from a place near it.
 *
 * Left out of the java library: `encode`, `shorten`, `contains`, the static forms of `decode`,
 * `isFull`, `isShort` and `isPadded`, and `isShortCode`.
 */
internal class OpenLocationCode {

	/** Coordinates of a decoded Open Location Code: the south-west corner, the size, and the center. */
	class CodeArea(
		val southLatitude: Double,
		val westLongitude: Double,
		val northLatitude: Double,
		val eastLongitude: Double,
		val length: Int
	) {
		val latitudeHeight: Double get() = northLatitude - southLatitude

		val longitudeWidth: Double get() = eastLongitude - westLongitude

		val centerLatitude: Double get() = (southLatitude + northLatitude) / 2

		val centerLongitude: Double get() = (westLongitude + eastLongitude) / 2
	}

	/** The code, upper case. */
	val code: String

	/** Throws [IllegalArgumentException] when [code] is not a valid code, full or short. */
	constructor(code: String) {
		if (!isValidCode(code.uppercase())) {
			throw IllegalArgumentException("The provided code '$code' is not a valid Open Location Code.")
		}
		this.code = code.uppercase()
	}

	/** Encodes a place into a code of [codeLength] digits. */
	constructor(latitude: Double, longitude: Double, codeLength: Int = CODE_PRECISION_NORMAL) {
		val length = min(codeLength, MAX_DIGIT_COUNT)
		if (length < PAIR_CODE_LENGTH && length % 2 == 1 || length < 4) {
			throw IllegalArgumentException("Illegal code length $length")
		}
		var lat = clipLatitude(latitude)
		val lng = normalizeLongitude(longitude)

		// Latitude 90 needs to be adjusted to be just less, so the returned code can also be decoded.
		if (lat == LATITUDE_MAX.toDouble()) {
			lat -= 0.9 * computeLatitudePrecision(length)
		}

		val revCodeBuilder = StringBuilder()

		// Compute the code.
		// This approach converts each value to an integer after multiplying it by
		// the final precision. This allows us to use only integer operations, so
		// avoiding any accumulation of floating point representation errors.

		// Multiply values by their precision and convert to positive without any
		// floating point operations.
		var latVal = (round((lat + LATITUDE_MAX) * LAT_INTEGER_MULTIPLIER * 1e6) / 1e6).toLong()
		var lngVal = (round((lng + LONGITUDE_MAX) * LNG_INTEGER_MULTIPLIER * 1e6) / 1e6).toLong()

		// Compute the grid part of the code if necessary.
		if (length > PAIR_CODE_LENGTH) {
			for (i in 0 until GRID_CODE_LENGTH) {
				val latDigit = latVal % GRID_ROWS
				val lngDigit = lngVal % GRID_COLUMNS
				val ndx = (latDigit * GRID_COLUMNS + lngDigit).toInt()
				revCodeBuilder.append(CODE_ALPHABET[ndx])
				latVal /= GRID_ROWS
				lngVal /= GRID_COLUMNS
			}
		} else {
			latVal = (latVal / GRID_ROWS.toDouble().pow(GRID_CODE_LENGTH)).toLong()
			lngVal = (lngVal / GRID_COLUMNS.toDouble().pow(GRID_CODE_LENGTH)).toLong()
		}
		// Compute the pair section of the code.
		for (i in 0 until PAIR_CODE_LENGTH / 2) {
			revCodeBuilder.append(CODE_ALPHABET[(lngVal % ENCODING_BASE).toInt()])
			revCodeBuilder.append(CODE_ALPHABET[(latVal % ENCODING_BASE).toInt()])
			latVal /= ENCODING_BASE
			lngVal /= ENCODING_BASE
			// If we are at the separator position, add the separator.
			if (i == 0) {
				revCodeBuilder.append(SEPARATOR)
			}
		}
		// Reverse the code.
		val codeBuilder = revCodeBuilder.reverse()

		// If we need to pad the code, replace some of the digits.
		if (length < SEPARATOR_POSITION) {
			for (i in length until SEPARATOR_POSITION) {
				codeBuilder[i] = PADDING_CHARACTER
			}
		}
		this.code = codeBuilder.substring(0, max(SEPARATOR_POSITION + 1, length + 1))
	}

	/**
	 * Decodes a full code into the area it stands for. Throws [IllegalStateException] on a short
	 * code, which [recover] makes full first.
	 */
	fun decode(): CodeArea {
		if (!isFullCode(code)) {
			throw IllegalStateException(
				"Method decode() could only be called on valid full codes, code was $code."
			)
		}
		// Strip padding and separator characters out of the code.
		val clean = code.replace(SEPARATOR.toString(), "").replace(PADDING_CHARACTER.toString(), "")

		// Initialise the values. We work them out as integers and convert them to doubles at the end.
		var latVal = -LATITUDE_MAX * LAT_INTEGER_MULTIPLIER
		var lngVal = -LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER
		// This will be used to compute the value of each digit.
		var latPlaceVal = LAT_MSP_VALUE
		var lngPlaceVal = LNG_MSP_VALUE
		var i = 0
		while (i < min(clean.length, PAIR_CODE_LENGTH)) {
			latPlaceVal /= ENCODING_BASE
			lngPlaceVal /= ENCODING_BASE
			latVal += CODE_ALPHABET.indexOf(clean[i]) * latPlaceVal
			lngVal += CODE_ALPHABET.indexOf(clean[i + 1]) * lngPlaceVal
			i += 2
		}
		for (j in PAIR_CODE_LENGTH until min(clean.length, MAX_DIGIT_COUNT)) {
			latPlaceVal /= GRID_ROWS
			lngPlaceVal /= GRID_COLUMNS
			val digit = CODE_ALPHABET.indexOf(clean[j])
			val row = digit / GRID_COLUMNS
			val col = digit % GRID_COLUMNS
			latVal += row * latPlaceVal
			lngVal += col * lngPlaceVal
		}
		val latitudeLo = latVal.toDouble() / LAT_INTEGER_MULTIPLIER
		val longitudeLo = lngVal.toDouble() / LNG_INTEGER_MULTIPLIER
		val latitudeHi = (latVal + latPlaceVal).toDouble() / LAT_INTEGER_MULTIPLIER
		val longitudeHi = (lngVal + lngPlaceVal).toDouble() / LNG_INTEGER_MULTIPLIER
		return CodeArea(latitudeLo, longitudeLo, latitudeHi, longitudeHi, min(clean.length, MAX_DIGIT_COUNT))
	}

	/** A full code holds the separator after its first 8 digits. */
	fun isFull(): Boolean = code.indexOf(SEPARATOR) == SEPARATOR_POSITION

	/**
	 * The full code nearest to the reference place with this code's last digits; a full code is
	 * returned as it is.
	 */
	fun recover(referenceLatitude: Double, referenceLongitude: Double): OpenLocationCode {
		if (isFull()) {
			// Note: each code is either full xor short, no other option.
			return this
		}
		val refLatitude = clipLatitude(referenceLatitude)
		val refLongitude = normalizeLongitude(referenceLongitude)

		val digitsToRecover = SEPARATOR_POSITION - code.indexOf(SEPARATOR)
		// The precision (height and width) of the missing prefix in degrees.
		val prefixPrecision = ENCODING_BASE.toDouble().pow(2 - (digitsToRecover / 2))

		// Use the reference location to generate the prefix.
		val recoveredPrefix = OpenLocationCode(refLatitude, refLongitude).code.substring(0, digitsToRecover)
		// Combine the prefix with the short code and decode it.
		val recovered = OpenLocationCode(recoveredPrefix + code)
		val recoveredCodeArea = recovered.decode()
		// Work out whether the new code area is too far from the reference location. If it is, we
		// move it. It can only be out by a single precision step.
		var recoveredLatitude = recoveredCodeArea.centerLatitude
		var recoveredLongitude = recoveredCodeArea.centerLongitude

		// Move the recovered latitude by one precision up or down if it is too far from the reference,
		// unless doing so would lead to an invalid latitude.
		val latitudeDiff = recoveredLatitude - refLatitude
		if (latitudeDiff > prefixPrecision / 2 && recoveredLatitude - prefixPrecision > -LATITUDE_MAX) {
			recoveredLatitude -= prefixPrecision
		} else if (latitudeDiff < -prefixPrecision / 2 && recoveredLatitude + prefixPrecision < LATITUDE_MAX) {
			recoveredLatitude += prefixPrecision
		}

		// Move the recovered longitude by one precision up or down if it is too far from the
		// reference.
		val longitudeDiff = recoveredCodeArea.centerLongitude - refLongitude
		if (longitudeDiff > prefixPrecision / 2) {
			recoveredLongitude -= prefixPrecision
		} else if (longitudeDiff < -prefixPrecision / 2) {
			recoveredLongitude += prefixPrecision
		}

		return OpenLocationCode(recoveredLatitude, recoveredLongitude, recovered.code.length - 1)
	}

	companion object {

		/** Provides a normal precision code, approximately 14x14 meters. */
		const val CODE_PRECISION_NORMAL = 10

		/** The character set used to encode the digit values. */
		const val CODE_ALPHABET = "23456789CFGHJMPQRVWX"

		/** A separator used to break the code into two parts to aid memorability. */
		const val SEPARATOR = '+'

		/** The character used to pad codes. */
		const val PADDING_CHARACTER = '0'

		/** The number of characters to place before the separator. */
		private const val SEPARATOR_POSITION = 8

		/** The max number of digits to process in a plus code. */
		const val MAX_DIGIT_COUNT = 15

		/** Maximum code length using just lat/lng pair encoding. */
		private const val PAIR_CODE_LENGTH = 10

		/** Number of digits in the grid coding section. */
		private const val GRID_CODE_LENGTH = MAX_DIGIT_COUNT - PAIR_CODE_LENGTH

		/** The base to use to convert numbers to/from. */
		private const val ENCODING_BASE = CODE_ALPHABET.length

		/** The maximum value for latitude in degrees. */
		private const val LATITUDE_MAX = 90L

		/** The maximum value for longitude in degrees. */
		private const val LONGITUDE_MAX = 180L

		/** Number of columns in the grid refinement method. */
		private const val GRID_COLUMNS = 4

		/** Number of rows in the grid refinement method. */
		private const val GRID_ROWS = 5

		/** Value to multiple latitude degrees to convert it to an integer with the maximum encoding precision. */
		private const val LAT_INTEGER_MULTIPLIER = 8000L * 3125

		/** Value to multiple longitude degrees to convert it to an integer with the maximum encoding precision. */
		private const val LNG_INTEGER_MULTIPLIER = 8000L * 1024

		/** Value of the most significant latitude digit after it has been converted to an integer. */
		private const val LAT_MSP_VALUE = LAT_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE

		/** Value of the most significant longitude digit after it has been converted to an integer. */
		private const val LNG_MSP_VALUE = LNG_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE

		/** Whether [code] is a valid code, full or short. Letters may be of either case. */
		fun isValidCode(code: String?): Boolean {
			if (code == null || code.length < 2) {
				return false
			}
			val upper = code.uppercase()

			// There must be exactly one separator.
			val separatorPosition = upper.indexOf(SEPARATOR)
			if (separatorPosition == -1) {
				return false
			}
			if (separatorPosition != upper.lastIndexOf(SEPARATOR)) {
				return false
			}
			// There must be an even number of at most 8 characters before the separator.
			if (separatorPosition % 2 != 0 || separatorPosition > SEPARATOR_POSITION) {
				return false
			}

			// Check first two characters: only some values from the alphabet are permitted.
			if (separatorPosition == SEPARATOR_POSITION) {
				// First latitude character can only have first 9 values.
				if (CODE_ALPHABET.indexOf(upper[0]) > 8) {
					return false
				}
				// First longitude character can only have first 18 values.
				if (CODE_ALPHABET.indexOf(upper[1]) > 17) {
					return false
				}
			}

			// Check the characters before the separator.
			var paddingStarted = false
			for (i in 0 until separatorPosition) {
				if (CODE_ALPHABET.indexOf(upper[i]) == -1 && upper[i] != PADDING_CHARACTER) {
					// Invalid character.
					return false
				}
				if (paddingStarted) {
					// Once padding starts, there must not be anything but padding.
					if (upper[i] != PADDING_CHARACTER) {
						return false
					}
				} else if (upper[i] == PADDING_CHARACTER) {
					paddingStarted = true
					// Short codes cannot have padding
					if (separatorPosition < SEPARATOR_POSITION) {
						return false
					}
					// Padding can start on even character: 2, 4 or 6.
					if (i != 2 && i != 4 && i != 6) {
						return false
					}
				}
			}

			// Check the characters after the separator.
			if (upper.length > separatorPosition + 1) {
				if (paddingStarted) {
					return false
				}
				// Only one character after separator is forbidden.
				if (upper.length == separatorPosition + 2) {
					return false
				}
				for (i in separatorPosition + 1 until upper.length) {
					if (CODE_ALPHABET.indexOf(upper[i]) == -1) {
						return false
					}
				}
			}

			return true
		}

		/** Whether [code] is a valid full code. */
		fun isFullCode(code: String): Boolean =
			try {
				OpenLocationCode(code).isFull()
			} catch (e: IllegalArgumentException) {
				false
			}

		/** java's `Math.round`: 0 for NaN, where [roundToLong] throws. */
		private fun round(x: Double): Long = if (x.isNaN()) 0L else x.roundToLong()

		private fun clipLatitude(latitude: Double): Double =
			min(max(latitude, -LATITUDE_MAX.toDouble()), LATITUDE_MAX.toDouble())

		private fun normalizeLongitude(longitude: Double): Double {
			var lng = longitude
			while (lng < -LONGITUDE_MAX) {
				lng += LONGITUDE_MAX * 2
			}
			while (lng >= LONGITUDE_MAX) {
				lng -= LONGITUDE_MAX * 2
			}
			return lng
		}

		/**
		 * Compute the latitude precision value for a given code length. Lengths <= 10 have the same
		 * precision for latitude and longitude, but lengths > 10 have different precisions due to
		 * the grid method having fewer columns than rows. Copied from the JS implementation.
		 */
		private fun computeLatitudePrecision(codeLength: Int): Double {
			if (codeLength <= CODE_PRECISION_NORMAL) {
				return ENCODING_BASE.toDouble().pow(floor((codeLength / -2 + 2).toDouble()))
			}
			return ENCODING_BASE.toDouble().pow(-3) / GRID_ROWS.toDouble().pow(codeLength - PAIR_CODE_LENGTH)
		}
	}
}
