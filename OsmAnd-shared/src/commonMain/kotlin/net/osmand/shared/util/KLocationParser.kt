package net.osmand.shared.util

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.openmap.MGRSPoint
import net.osmand.shared.util.openmap.UTMPoint
import kotlin.math.abs

/**
 * A copy of `LocationParser` in OsmAnd-java: reads a place typed as coordinates, in degrees with or
 * without minutes and seconds, UTM, MGRS or an Open Location Code.
 *
 * It trims as java's `String.trim` does, only the characters up to the space.
 */
object KLocationParser {

	private const val LTR_MARK = "‎"
	private const val RTL_MARK = "‏"
	private const val ORIGINAL_DIRECTION_MARK = "‬"

	private const val NO_HEMISPHERE = '\u0000'

	private val NEGATIVE_ZERO_BITS = (-0.0).toBits()

	class ParsedOpenLocationCode internal constructor(private val text: String) {
		private var code: String? = null
		private var full = false
		private var placeName: String? = null

		private var olc: OpenLocationCode? = null
		private var latLon: KLatLon? = null

		init {
			parse()
		}

		private fun parse() {
			if (!KAlgorithms.isEmpty(text)) {
				val split = text.split(" ")
				if (split.isNotEmpty()) {
					val code = split[0]
					this.code = code
					try {
						val olc = OpenLocationCode(code)
						this.olc = olc
						full = olc.isFull()
						if (full) {
							val codeArea = olc.decode()
							latLon = KLatLon(codeArea.centerLatitude, codeArea.centerLongitude)
						} else {
							if (split.size > 1) {
								placeName = text.substring(code.length + 1)
							}
						}
					} catch (e: IllegalArgumentException) {
						this.code = null
					}
				}
			}
		}

		fun recover(searchLocation: KLatLon): KLatLon? {
			val olc = olc
			if (olc != null) {
				val codeArea = olc.recover(searchLocation.latitude, searchLocation.longitude).decode()
				latLon = KLatLon(codeArea.centerLatitude, codeArea.centerLongitude)
			}
			return latLon
		}

		internal fun isValidCode(): Boolean = !KAlgorithms.isEmpty(code)

		fun getText(): String = text

		fun getCode(): String? = code

		fun isFull(): Boolean = full

		fun getPlaceName(): String? = placeName

		fun getLatLon(): KLatLon? = latLon
	}

	fun isValidOLC(code: String): Boolean = OpenLocationCode.isValidCode(code)

	fun parseOpenLocationCode(locPhrase: String): ParsedOpenLocationCode? {
		val parsedCode = ParsedOpenLocationCode(trimLikeJava(locPhrase))
		return if (!parsedCode.isValidCode()) null else parsedCode
	}

	fun parseLocation(phrase: String): KLatLon? {
		var locPhrase = trimLikeJava(phrase)
		locPhrase = clearDirectionMarks(locPhrase)
		var valid = isValidLocPhrase(locPhrase)
		if (!valid) {
			// java's split drops trailing empty parts
			val split = locPhrase.split(" ").dropLastWhile { it.isEmpty() }
			if (split.size == 4 && split[1].contains(".") && split[3].contains(".")) {
				locPhrase = split[1] + " " + split[3]
				valid = isValidLocPhrase(locPhrase)
			}
		}
		if (!valid) {
			return null
		}
		locPhrase = prepareLatLonWithDecimalCommas(locPhrase)
		val d = ArrayList<Double>()
		val all = ArrayList<Any>()
		val strings = ArrayList<String>()
		splitObjects(locPhrase, d, all, strings)
		if (d.size == 0) {
			return null
		}
		// detect UTM
		val second = all.getOrNull(1)
		if (all.size == 4 && d.size == 3 && second is String && second.length == 1) {
			val ch = second[0]
			if (ch.isLetter()) {
				// UTMPoint accepts only 'N' or 'S'; UTM band letters C-X must be mapped to hemisphere
				val hemisphere = utmZoneLetterToHemisphere(ch)
				if (hemisphere != NO_HEMISPHERE) {
					// UTMPoint(northing, easting, zoneNumber, zoneLetter) - zoneLetter is N or S
					val uPoint = UTMPoint(d[2], d[1], d[0].toInt(), hemisphere)
					val ll = uPoint.toLatLonPoint() // null for a zone out of 0-60
					if (ll != null) {
						return validateAndCreateLatLon(ll.latitude.toDouble(), ll.longitude.toDouble())
					}
				}
			}
		}

		if (all.size == 3 && d.size == 2 && second is String && second.length == 1) {
			val ch = second[0]
			val combined = strings[2]
			if (ch.isLetter()) {
				try {
					val east = combined.substring(0, combined.length / 2)
					val north = combined.substring(combined.length / 2)
					val upoint = UTMPoint(north.toDouble(), east.toDouble(), d[0].toInt(), ch)
					val ll = upoint.toLatLonPoint()
					if (ll != null) {
						return validateAndCreateLatLon(ll.latitude.toDouble(), ll.longitude.toDouble())
					}
				} catch (e: NumberFormatException) {
				}
			}
		}

		//detect MGRS
		if (all.size >= 3 && (d.size == 2 || d.size == 3) && second is String) {
			try {
				val mgrsPoint = MGRSPoint(locPhrase)
				val ll = mgrsPoint.toLatLonPoint()!!
				return validateAndCreateLatLon(ll.latitude.toDouble(), ll.longitude.toDouble())
			} catch (e: NumberFormatException) {
				//do nothing
			}
		}
		// try to find split lat/lon position
		var jointNumbers = 0
		var lastJoin = 0
		var degSplit = -1
		var degType = -1 // 0 - degree, 1 - minutes, 2 - seconds
		var finishDegSplit = false
		var northSplit = -1
		var eastSplit = -1
		for (i in 1 until all.size) {
			if (all[i - 1] is Double && all[i] is Double) {
				jointNumbers++
				lastJoin = i
			}
			if (all[i] == "n" || all[i] == "s" ||
				all[i] == "N" || all[i] == "S"
			) {
				northSplit = i + 1
			}
			if (all[i] == "e" || all[i] == "w" ||
				all[i] == "E" || all[i] == "W"
			) {
				eastSplit = i
			}
			var dg = -1
			if (all[i] == "°") {
				dg = 0
			} else if (all[i] == "'" || all[i] == "′") {
				dg = 1
			} else if (all[i] == "″" || all[i] == "\"") {
				dg = 2
			}
			if (dg != -1) {
				if (!finishDegSplit) {
					if (degType < dg) {
						degSplit = i + 1
						degType = dg
					} else {
						finishDegSplit = true
						degType = dg
					}
				} else {
					if (degType < dg) {
						degType = dg
					} else {
						// reject delimiter
						degSplit = -1
					}
				}
			}
		}
		var split = -1
		if (jointNumbers == 1) {
			split = lastJoin
		}
		if (northSplit != -1 && northSplit < all.size - 1) {
			split = northSplit
		} else if (eastSplit != -1 && eastSplit < all.size - 1) {
			split = eastSplit
		} else if (degSplit != -1 && degSplit < all.size - 1) {
			split = degSplit
		}

		if (split != -1) {
			val lat = parse1Coordinate(all, 0, split)
			val lon = parse1Coordinate(all, split, all.size)
			return validateAndCreateLatLon(lat, lon)
		}
		if (d.size == 2) {
			return validateAndCreateLatLon(d[0], d[1])
		}
		// simple url case
		if (locPhrase.contains("://")) {
			var lat = 0.0
			var lon = 0.0
			var only2decimals = true
			for (i in d.indices) {
				if (d[i] != d[i].toInt().toDouble()) {
					if (lat == 0.0) {
						lat = d[i]
					} else if (lon == 0.0) {
						lon = d[i]
					} else {
						only2decimals = false
					}
				}
			}
			if (lat != 0.0 && lon != 0.0 && only2decimals) {
				return validateAndCreateLatLon(lat, lon)
			}
		}
		// split by equal number of digits
		if (d.size > 2 && d.size % 2 == 0) {
			var ind = d.size / 2 + 1
			var splitEq = -1
			for (i in all.indices) {
				if (all[i] is Double) {
					ind--
				}
				if (ind == 0) {
					splitEq = i
					break
				}
			}
			if (splitEq != -1) {
				val lat = parse1Coordinate(all, 0, splitEq)
				val lon = parse1Coordinate(all, splitEq, all.size)
				return validateAndCreateLatLon(lat, lon)
			}
		}
		return null
	}

	private fun prepareLatLonWithDecimalCommas(ll: String): String {
		val DIGITS_BEFORE_COMMA = 1
		val DIGITS_AFTER_COMMA = 3 // see testCommaLatLonSearch
		var first = -1
		for (i in DIGITS_BEFORE_COMMA until ll.length - DIGITS_AFTER_COMMA) {
			if (ll[i] == ',') {
				var before = 0
				var after = 0
				for (j in i - 1 downTo i - DIGITS_BEFORE_COMMA) {
					if (ll[j].isDigit()) {
						before++
					}
				}
				var j = i + 1
				while (j <= i + DIGITS_AFTER_COMMA && before >= DIGITS_BEFORE_COMMA) {
					if (ll[j].isDigit()) {
						after++
					}
					j++
				}
				if (before >= DIGITS_BEFORE_COMMA && after >= DIGITS_AFTER_COMMA) {
					if (first != -1) {
						return ll.substring(0, first) + "." + ll.substring(first + 1, i) + "." + ll.substring(i + 1)
					} else {
						first = i // first suitable comma found
					}
				}
			}
		}
		return ll
	}

	/**
	 * Converts UTM zone letter to hemisphere 'N' or 'S'.
	 * https://www.maptools.com/tutorials/grid_zone_details
	 * @return 'N' for northern bands (N,P,Q,R,S,T,U,V,W,X), 'S' for southern (C,D,E,F,G,H,J,K,L,M), or 0 if invalid
	 */
	private fun utmZoneLetterToHemisphere(zoneLetter: Char): Char {
		val upper = zoneLetter.uppercaseChar()
		if (upper in 'C'..'X' && upper != 'I' && upper != 'O') {
			return if (upper >= 'N') 'N' else 'S'
		}
		return NO_HEMISPHERE
	}

	private fun validateAndCreateLatLon(lat: Double, lon: Double): KLatLon? {
		if (abs(lat) <= 90 && abs(lon) <= 180) {
			return KLatLon(lat, lon)
		}
		return null
	}

	private fun isValidLocPhrase(locPhrase: String): Boolean {
		if (locPhrase.isNotEmpty()) {
			var ch = locPhrase[0].lowercaseChar()
			if (ch == '(' && locPhrase.length > 1) {
				ch = locPhrase[1].lowercaseChar() // (0.1234,5.6789)
			}
			var cntLetter = 0
			var cntDigits = 0
			for (i in locPhrase.indices) {
				val c = locPhrase[i].lowercaseChar()
				if (c.isLetter() && c != 's' && c != 'n' && c != 'w' && c != 'e')
					cntLetter++
				if (c.isDigit())
					cntDigits++
			}
			if (!locPhrase.contains("://") && cntLetter > cntDigits) {
				// 5c Hazelmere road, nw6 6
				return false
			}
			return ch == '-' || ch.isDigit() || ch == 's' || ch == 'n' || locPhrase.contains("://")
		}
		return false
	}

	/**
	 * One coordinate from the objects of [splitObjects] between [begin] and [end]: degrees,
	 * minutes and seconds added up, negative for 'S' or 'W' or a leading minus.
	 */
	fun parse1Coordinate(all: List<Any>, begin: Int, end: Int): Double {
		var neg = false
		var d = 0.0
		var type = 0 // degree - 0, minutes - 1, seconds = 2
		var prevDouble: Double? = null
		for (i in begin..end) {
			val o: Any = if (i == end) "" else all[i]
			if (o == "S" || o == "s" || o == "W" || o == "w" || isNegativeZero(o)) {
				neg = !neg
			}
			if (prevDouble != null) {
				if (o == "°") {
					type = 0
				} else if (o == "′" /*o.equals("'")*/) {
					// ' can be used as delimiter ignore it
					type = 1
				} else if (o == "\"" || o == "″") {
					type = 2
				}
				if (type == 0) {
					var ld = prevDouble
					if (ld < 0) {
						ld = -ld
						neg = true
					}
					d += ld
				} else if (type == 1) {
					d += prevDouble / 60f
				} else /*if (type == 1) */ {
					d += prevDouble / 3600f
				}
				type++
			}
			prevDouble = o as? Double
		}
		if (neg) {
			d = -d
		}
		return d
	}

	/** java's `o.equals(-0.0)`: -0.0 only, not 0.0, which `==` on a double would take too. */
	private fun isNegativeZero(o: Any): Boolean = o is Double && o.toBits() == NEGATIVE_ZERO_BITS

	fun splitObjects(s: String, d: MutableList<Double>, all: MutableList<Any>, strings: MutableList<String>) {
		splitObjects(s, d, all, strings, booleanArrayOf(false))
	}

	/**
	 * Splits [s] into numbers, which go to [d] and [all], and the words and signs between them,
	 * which go to [all]; [strings] gets the text of each. [partial] is set when nothing follows the
	 * first number.
	 */
	fun splitObjects(
		s: String,
		d: MutableList<Double>,
		all: MutableList<Any>,
		strings: MutableList<String>,
		partial: BooleanArray
	) {
		var digit = false
		var word = -1
		var firstNumeralIdx = -1
		for (i in 0..s.length) {
			val ch = if (i == s.length) ' ' else s[i]
			val dg = ch.isDigit()
			val nonwh = ch != ',' && ch != ' ' && ch != ';'
			if (ch == '.' || dg || ch == '-') {
				if (!digit) {
					if (word != -1) {
						all.add(s.substring(word, i))
						strings.add(s.substring(word, i))
					}
					digit = true
					word = i
				} else {
					if (word == -1) {
						word = i
					}
					// if digit
					// continue
				}
			} else {
				if (digit) {
					if (word != -1) {
						try {
							val dl = s.substring(word, i).toDouble()
							d.add(dl)
							all.add(dl)
							if (firstNumeralIdx == -1) {
								firstNumeralIdx = all.size - 1
							}
							strings.add(s.substring(word, i))
							digit = false
							word = -1
						} catch (e: NumberFormatException) {
						}
					}
				}
				if (nonwh) {
					if (!ch.isLetter()) {
						if (word != -1) {
							all.add(s.substring(word, i))
							strings.add(s.substring(word, i))
						}
						all.add(s.substring(i, i + 1))
						strings.add(s.substring(i, i + 1))
						word = -1
					} else if (word == -1) {
						word = i
					}
				} else {
					if (word != -1) {
						all.add(s.substring(word, i))
						strings.add(s.substring(word, i))
					}
					word = -1
				}
			}
		}

		partial[0] = false
		if (firstNumeralIdx != -1) {
			val nextTokenIdx = firstNumeralIdx + 1
			if (all.size <= nextTokenIdx) {
				partial[0] = true
			}
		}
	}

	private fun clearDirectionMarks(text: String): String =
		text.replace(ORIGINAL_DIRECTION_MARK, "")
			.replace(LTR_MARK, "")
			.replace(RTL_MARK, "")

	private fun trimLikeJava(s: String): String = s.trim { it <= ' ' }
}
