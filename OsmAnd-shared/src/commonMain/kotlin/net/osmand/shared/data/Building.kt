package net.osmand.shared.data

import net.osmand.shared.util.KAlgorithms

/**
 * A house of a [Street], or a range of them when the address section stores an interpolation line
 * from this number to [getName2].
 *
 * A copy of `net.osmand.data.Building` in OsmAnd-java, which stays there for android and tools.
 * Left out: the JSON export, which only writes test data from OsmAnd-java.
 */
class Building : MapObject() {

	private var postcode: String? = null
	private var latLon2: KLatLon? = null
	private var interpolationType: BuildingInterpolation? = null
	private var interpolationInterval: Int = 0
	private var name2: String? = null
	private var entrances: MutableMap<String, KLatLon>? = null

	enum class BuildingInterpolation(private val v: Int) {
		ALL(-1), EVEN(-2), ODD(-3), ALPHABETIC(-4);

		fun getValue(): Int = v

		companion object {
			fun fromValue(i: Int): BuildingInterpolation? {
				for (b in entries) {
					if (b.v == i) {
						return b
					}
				}
				return null
			}
		}
	}

	fun getPostcode(): String? = postcode

	fun getEntrances(): Map<String, KLatLon> = entrances ?: emptyMap()

	fun addEntrance(ref: String, location: KLatLon) {
		var entrances = this.entrances
		if (entrances == null) {
			entrances = LinkedHashMap()
			this.entrances = entrances
		}
		entrances[ref] = location
	}

	fun getInterpolationInterval(): Int = interpolationInterval

	fun setInterpolationInterval(interpolationNumber: Int) {
		this.interpolationInterval = interpolationNumber
	}

	fun getInterpolationType(): BuildingInterpolation? = interpolationType

	fun setInterpolationType(interpolationType: BuildingInterpolation?) {
		this.interpolationType = interpolationType
	}

	fun getLatLon2(): KLatLon? = latLon2

	fun setLatLon2(latlon2: KLatLon?) {
		this.latLon2 = latlon2
	}

	fun getName2(): String? = name2

	fun setName2(name2: String?) {
		this.name2 = name2
	}

	fun setPostcode(postcode: String?) {
		this.postcode = postcode
	}

	/** The name in [lang] only for an interpolation; a single house answers with its plain name. */
	override fun getName(lang: String?): String {
		val fname = super.getName(lang)
		if (interpolationInterval != 0) {
			return fname + "-" + name2 + " (+" + interpolationInterval + ") "
		} else if (interpolationType != null) {
			return fname + "-" + name2 + " (" + interpolationType.toString().lowercase() + ") "
		}
		return name ?: ""
	}

	fun getFullName(): String? {
		val fname = this.name
		if (interpolationInterval != 0) {
			return fname + "-" + name2 + " (+" + interpolationInterval + ") "
		} else if (interpolationType != null) {
			return fname + "-" + name2 + " (" + interpolationType.toString().lowercase() + ") "
		}
		return name
	}

	fun isInterpolation(): Boolean = getInterpolationType() != null || getInterpolationInterval() > 0

	/**
	 * Where [hno] falls along the interpolation line, from 0 at this number to 1 at [getName2], or -1
	 * when it is not on the line.
	 */
	fun interpolation(hno: String): Float {
		if (getInterpolationType() != null || getInterpolationInterval() > 0
			// || checkNameAsInterpolation() // disable due to situation in NL #4284
		) {
			val num = KAlgorithms.extractFirstIntegerNumber(hno)
			val fname = super.getName()
			val numB = KAlgorithms.extractFirstIntegerNumber(fname)
			var numT = numB
			var sname = getName2()
			if (getInterpolationType() == BuildingInterpolation.ALPHABETIC) {
				if (num != numB) {
					// currently not supported
					return -1f
				}
				val hint = hno[hno.length - 1].code
				val fch = fname[fname.length - 1].code
				val sch = sname!![sname.length - 1].code
				if (fch == sch) {
					return -1f
				}
				val res = (hint.toFloat() - fch) / (sch.toFloat() - fch)
				if (res > 1 || res < 0) {
					return -1f
				}
				return res
			}
			if (num >= numB) {
				if (fname.contains("-") && sname == null) {
					val l = fname.indexOf('-')
					sname = fname.substring(l + 1, fname.length)
				}
				if (sname != null) {
					numT = KAlgorithms.extractFirstIntegerNumber(sname)
					if (numT < num) {
						return -1f
					}
				}
				if (getInterpolationType() == BuildingInterpolation.EVEN && num % 2 == 1) {
					return -1f
				}
				if (getInterpolationType() == BuildingInterpolation.ODD && num % 2 == 0) {
					return -1f
				}
				if (getInterpolationInterval() != 0 && (num - numB) % getInterpolationInterval() != 0) {
					return -1f
				}
			} else {
				return -1f
			}
			if (numT > numB) {
				return (num.toFloat() - numB) / (numT.toFloat() - numB)
			}
			return 0f
		}
		return -1f
	}

	internal fun checkNameAsInterpolation(): Boolean {
		val nm = super.getName()
		var interpolation = nm.contains("-")
		if (interpolation) {
			for (i in nm.indices) {
				if (!(nm[i] in '0'..'9') && nm[i] != '-') {
					interpolation = false
					break
				}
			}
		}
		return interpolation
	}

	fun belongsToInterpolation(hno: String): Boolean = interpolation(hno) > 0

	override fun toString(): String {
		if (interpolationInterval != 0) {
			return name + "-" + name2 + " (+" + interpolationInterval + ") "
		} else if (interpolationType != null) {
			return name + "-" + name2 + " (" + interpolationType + ") "
		}
		return name.toString()
	}

	fun getLocation(interpolation: Float): KLatLon? {
		val loc = getLocation()
		val latLon2 = getLatLon2()
		if (latLon2 != null) {
			val lat1 = loc!!.latitude
			val lat2 = latLon2.latitude
			val lon1 = loc.longitude
			val lon2 = latLon2.longitude
			return KLatLon(interpolation * (lat2 - lat1) + lat1, interpolation * (lon2 - lon1) + lon1)
		}
		return loc
	}

	override fun equals(other: Any?): Boolean {
		val res = super.equals(other)
		if (res && other is Building) {
			return KAlgorithms.stringsEqual(other.getFullName(), getFullName())
		}
		return res
	}

	override fun hashCode(): Int = super.hashCode()

	fun getInterpolationName(coeff: Double): String {
		if (!KAlgorithms.isEmpty(getName2())) {
			val fi = KAlgorithms.extractFirstIntegerNumber(getName())
			val si = KAlgorithms.extractFirstIntegerNumber(getName2()!!)
			if (si != 0 && fi != 0) {
				var num = (fi + (si - fi) * coeff).toInt()
				val type = getInterpolationType()
				if (type == BuildingInterpolation.EVEN || type == BuildingInterpolation.ODD) {
					if (num % 2 == (if (type == BuildingInterpolation.EVEN) 1 else 0)) {
						num--
					}
				} else if (getInterpolationInterval() > 0) {
					val intv = getInterpolationInterval()
					if ((num - fi) % intv != 0) {
						num = ((num - fi) / intv) * intv + fi
					}
				}
				return num.toString()
			}
		}
		return ""
	}
}
