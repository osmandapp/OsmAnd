package net.osmand.shared.gpx.primitives

import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxFormatter
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.routing.RouteColorize
import net.osmand.shared.util.KAlgorithms

class WptPt : GpxExtensions {
	var firstPoint = false
	var lastPoint = false
	var lat: Double = 0.0
	var lon: Double = 0.0
	var name: String? = null
	var link: Link? = null
	var category: String? = null
	var desc: String? = null
	var comment: String? = null
	var time: Long = 0
	var ele = Double.NaN
	var speed = 0.0
	var hdop = Double.NaN
	var heading = Float.NaN
	var bearing = Float.NaN
	var deleted = false
	var speedColor = 0
	var altitudeColor = 0
	var slopeColor = 0
	var colourARGB = 0
	var distance = 0.0
	var attributes: PointAttributes? = null

	constructor()

	constructor(wptPt: WptPt) {
		lat = wptPt.lat
		lon = wptPt.lon
		name = wptPt.name
		category = wptPt.category
		desc = wptPt.desc
		comment = wptPt.comment
		time = wptPt.time
		ele = wptPt.ele
		speed = wptPt.speed
		hdop = wptPt.hdop
		heading = wptPt.heading
		deleted = wptPt.deleted
		speedColor = wptPt.speedColor
		altitudeColor = wptPt.altitudeColor
		slopeColor = wptPt.slopeColor
		colourARGB = wptPt.colourARGB
		distance = wptPt.distance
		link = wptPt.link?.let { Link(it) }
		getExtensionsToWrite().putAll(wptPt.getExtensionsToWrite())
	}

	fun getColor(): Int {
		return getColor(0)!!
	}

	fun getLatitude(): Double {
		return lat
	}

	fun getLongitude(): Double {
		return lon
	}

	constructor(lat: Double, lon: Double) {
		this.lat = lat
		this.lon = lon
	}

	constructor(
		lat: Double,
		lon: Double,
		time: Long,
		ele: Double,
		speed: Double,
		hdop: Double
	) : this(lat, lon, time, ele, speed, hdop, Float.NaN)

	constructor(
		lat: Double,
		lon: Double,
		time: Long,
		ele: Double,
		speed: Double,
		hdop: Double,
		heading: Float
	) {
		this.lat = lat
		this.lon = lon
		this.time = time
		this.ele = ele
		this.speed = speed
		this.hdop = hdop
		this.heading = heading
	}

	constructor(
		lat: Double,
		lon: Double,
		desc: String?,
		name: String?,
		category: String?,
		color: String?,
		icon: String?,
		background: String?
	) {
		this.lat = lat
		this.lon = lon
		this.desc = desc
		this.name = name
		this.category = category
		setColor(color)
		setIconName(icon)
		setBackgroundType(background)
	}

	fun getIconName(): String? {
		return getExtensionsToRead()[GpxUtilities.ICON_NAME_EXTENSION]
	}

	fun isHidden(): Boolean {
		return getExtensionsToRead()[GpxUtilities.HIDDEN_EXTENSION]?.toBoolean() ?: false
	}

	fun getIconNameOrDefault(): String {
		var iconName = getIconName()
		if (iconName == null) {
			iconName = GpxUtilities.DEFAULT_ICON_NAME
		}
		return iconName
	}

	fun setIconName(iconName: String?) {
		val extensionsToWrite = getExtensionsToWrite()
		if (iconName == null) {
			extensionsToWrite.remove(GpxUtilities.ICON_NAME_EXTENSION)
		} else {
			extensionsToWrite[GpxUtilities.ICON_NAME_EXTENSION] = iconName
		}
	}

	fun getAmenityOriginName(): String? {
		val extensionsToRead = getExtensionsToRead()
		var amenityOrigin = extensionsToRead[GpxUtilities.AMENITY_ORIGIN_EXTENSION]
		val comment = this.comment
		if (amenityOrigin == null && comment != null && comment.startsWith("Amenity")) {
			amenityOrigin = comment
		}
		return amenityOrigin
	}

	fun setAmenityOriginName(originName: String) {
		getExtensionsToWrite()[GpxUtilities.AMENITY_ORIGIN_EXTENSION] = originName
	}

	fun getColor(type: RouteColorize.ColorizationType?): Int {
		return when (type) {
			RouteColorize.ColorizationType.SPEED -> speedColor
			RouteColorize.ColorizationType.ELEVATION -> altitudeColor
			else -> slopeColor
		}
	}

	fun setColor(type: RouteColorize.ColorizationType, color: Int) {
		when (type) {
			RouteColorize.ColorizationType.SPEED -> speedColor = color
			RouteColorize.ColorizationType.ELEVATION -> altitudeColor = color
			RouteColorize.ColorizationType.SLOPE -> slopeColor = color
			else -> {}
		}
	}

	fun getBackgroundType(): String? {
		return getExtensionsToRead()[GpxUtilities.BACKGROUND_TYPE_EXTENSION]
	}

	fun setBackgroundType(backType: String?) {
		val extensionsToWrite = getExtensionsToWrite()
		if (backType == null) {
			extensionsToWrite.remove(GpxUtilities.BACKGROUND_TYPE_EXTENSION)
		} else {
			extensionsToWrite[GpxUtilities.BACKGROUND_TYPE_EXTENSION] = backType
		}
	}

	fun getProfileType(): String? {
		return getExtensionsToRead()[GpxUtilities.PROFILE_TYPE_EXTENSION]
	}

	fun getAddress(): String? {
		return getExtensionsToRead()[GpxUtilities.ADDRESS_EXTENSION]
	}

	fun setAddress(address: String?) {
		if (address.isNullOrEmpty()) {
			getExtensionsToWrite().remove(GpxUtilities.ADDRESS_EXTENSION)
		} else {
			getExtensionsToWrite()[GpxUtilities.ADDRESS_EXTENSION] = address
		}
	}

	fun setHidden(hidden: String?) {
		val extensionsToWrite = getExtensionsToWrite()
		if (hidden == "true") {
			extensionsToWrite[GpxUtilities.HIDDEN_EXTENSION] = hidden
		} else {
			extensionsToWrite.remove(GpxUtilities.HIDDEN_EXTENSION)
		}
	}

	fun setProfileType(profileType: String) {
		getExtensionsToWrite()[GpxUtilities.PROFILE_TYPE_EXTENSION] = profileType
	}

	fun hasProfile(): Boolean {
		val profileType = getProfileType()
		return profileType != null && GpxUtilities.GAP_PROFILE_TYPE != profileType
	}

	fun isGap(): Boolean {
		val profileType = getProfileType()
		return GpxUtilities.GAP_PROFILE_TYPE == profileType
	}

	fun setGap() {
		setProfileType(GpxUtilities.GAP_PROFILE_TYPE)
	}

	fun removeProfileType() {
		getExtensionsToWrite().remove(GpxUtilities.PROFILE_TYPE_EXTENSION)
	}

	fun getTrkPtIndex(): Int {
		return try {
			getExtensionsToRead()[GpxUtilities.TRKPT_INDEX_EXTENSION]?.toInt() ?: -1
		} catch (e: NumberFormatException) {
			-1
		}
	}

	fun setTrkPtIndex(index: Int) {
		getExtensionsToWrite()[GpxUtilities.TRKPT_INDEX_EXTENSION] = index.toString()
	}

	override fun hashCode(): Int {
		return KAlgorithms.hash(name, category, desc, comment, lat, lon)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || other !is WptPt) return false
		return other.name == name &&
				other.category == category &&
				other.lat == lat && other.lon == lon &&
				other.desc == desc
	}

	fun hasLocation(): Boolean {
		return lat != 0.0 && lon != 0.0
	}

	companion object {
		fun createAdjustedPoint(
			lat: Double,
			lon: Double,
			description: String?,
			name: String?,
			category: String?,
			color: Int,
			iconName: String?,
			backgroundType: String?,
			amenityOriginName: String?,
			amenityExtensions: Map<String, String>?
		): WptPt {
			val latAdjusted = GpxFormatter.formatLatLon(lat).toDouble()
			val lonAdjusted = GpxFormatter.formatLatLon(lon).toDouble()
			val point = WptPt(
				latAdjusted,
				lonAdjusted,
				currentTimeMillis(),
				Double.NaN,
				0.0,
				Double.NaN
			)
			point.name = name
			point.category = category
			point.desc = description

			if (color != 0) {
				point.setColor(color)
			}
			if (iconName != null) {
				point.setIconName(iconName)
			}
			if (backgroundType != null) {
				point.setBackgroundType(backgroundType)
			}
			if (amenityOriginName != null) {
				point.setAmenityOriginName(amenityOriginName)
			}
			if (amenityExtensions != null) {
				point.getExtensionsToWrite().putAll(amenityExtensions)
			}
			return point
		}
	}

	fun updatePoint(pt: WptPt) {
		lat = GpxFormatter.formatLatLon(pt.lat).toDouble()
		lon = GpxFormatter.formatLatLon(pt.lon).toDouble()
		time = currentTimeMillis()
		desc = pt.desc
		name = pt.name
		category = pt.category

		val extensions = pt.getExtensionsToRead()

		val color = extensions[GpxUtilities.COLOR_NAME_EXTENSION]
		setColor(color)

		val iconName = extensions[GpxUtilities.ICON_NAME_EXTENSION]
		setIconName(iconName)

		val backgroundType = extensions[GpxUtilities.BACKGROUND_TYPE_EXTENSION]
		setBackgroundType(backgroundType)

		val address = extensions[GpxUtilities.ADDRESS_EXTENSION]
		setAddress(address)

		val hidden = extensions[GpxUtilities.HIDDEN_EXTENSION]
		setHidden(hidden)
	}

	fun getSpecialPointType(): String? {
		return getExtensionsToRead()[GpxUtilities.POINT_TYPE_EXTENSION]
	}

	fun setSpecialPointType(type: String?) {
		if (type != null) {
			getExtensionsToWrite()[GpxUtilities.POINT_TYPE_EXTENSION] = type
		}
	}
}
