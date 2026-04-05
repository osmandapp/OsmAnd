package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.PointAttributes

internal interface TrackPointSource {
	val size: Int

	fun copyPointTo(index: Int, point: WptPt)
	fun getLat(index: Int): Double
	fun getLon(index: Int): Double
	fun getTime(index: Int): Long
	fun getEle(index: Int): Double
}

internal class MutableTrackPointSource(initialCapacity: Int = 0) : TrackPointSource {
	companion object {
		private const val FLAG_DELETED = 1
		private const val FLAG_FIRST = 1 shl 1
		private const val FLAG_LAST = 1 shl 2
	}

	override var size: Int = 0
		private set

	private var latitudes = DoubleArray(initialCapacity.coerceAtLeast(16))
	private var longitudes = DoubleArray(latitudes.size)
	private var times = LongArray(latitudes.size)
	private var distances = DoubleArray(latitudes.size)
	private var elevations = DoubleArray(latitudes.size) { Double.NaN }
	private var speeds = FloatArray(latitudes.size)
	private var hdops = FloatArray(latitudes.size) { Float.NaN }
	private var bearings = FloatArray(latitudes.size) { Float.NaN }
	private var headings = FloatArray(latitudes.size) { Float.NaN }
	private var colorArgb = IntArray(latitudes.size)
	private var altitudeColors = IntArray(latitudes.size)
	private var speedColors = IntArray(latitudes.size)
	private var slopeColors = IntArray(latitudes.size)
	private var flags = IntArray(latitudes.size)

	private var extras: MutableMap<Int, TrackPointExtras>? = null

	fun addPoint(lat: Double, lon: Double): Int {
		ensureCapacity(size + 1)
		val index = size
		latitudes[index] = lat
		longitudes[index] = lon
		times[index] = 0L
		distances[index] = 0.0
		elevations[index] = Double.NaN
		speeds[index] = 0.0f
		hdops[index] = Float.NaN
		bearings[index] = Float.NaN
		headings[index] = Float.NaN
		colorArgb[index] = 0
		altitudeColors[index] = 0
		speedColors[index] = 0
		slopeColors[index] = 0
		flags[index] = 0
		size++
		return index
	}

	fun addPoint(point: WptPt): Int {
		val index = addPoint(point.lat, point.lon)
		setTime(index, point.time)
		setDistance(index, point.distance)
		setEle(index, point.ele)
		setSpeed(index, point.speed)
		setHdop(index, point.hdop)
		setBearing(index, point.bearing)
		setHeading(index, point.heading)
		setColourArgb(index, point.colourARGB)
		setAltitudeColor(index, point.altitudeColor)
		setSpeedColor(index, point.speedColor)
		setSlopeColor(index, point.slopeColor)
		setDeleted(index, point.deleted)
		setFirstPoint(index, point.firstPoint)
		setLastPoint(index, point.lastPoint)
		setName(index, point.name)
		setDesc(index, point.desc)
		setCategory(index, point.category)
		setComment(index, point.comment)
		setLink(index, point.link?.let { Link(it) })
		point.attributes?.let { getOrCreateExtras(index).attributes = it }
		val pointExtensions = point.getExtensionsToRead()
		if (pointExtensions.isNotEmpty()) {
			getOrCreateExtras(index).extensions = pointExtensions.toMutableMap()
		}
		return index
	}

	override fun copyPointTo(index: Int, point: WptPt) {
		point.lat = latitudes[index]
		point.lon = longitudes[index]
		point.time = times[index]
		point.distance = distances[index]
		point.ele = elevations[index]
		point.speed = speeds[index]
		point.hdop = hdops[index]
		point.bearing = bearings[index]
		point.heading = headings[index]
		point.colourARGB = colorArgb[index]
		point.altitudeColor = altitudeColors[index]
		point.speedColor = speedColors[index]
		point.slopeColor = slopeColors[index]
		point.deleted = flags[index] and FLAG_DELETED != 0
		point.firstPoint = flags[index] and FLAG_FIRST != 0
		point.lastPoint = flags[index] and FLAG_LAST != 0
		extras?.get(index)?.copyTo(point)
	}

	override fun getLat(index: Int): Double = latitudes[index]

	override fun getLon(index: Int): Double = longitudes[index]

	override fun getTime(index: Int): Long = times[index]

	override fun getEle(index: Int): Double = elevations[index]

	fun setTime(index: Int, value: Long) {
		times[index] = value
	}

	fun setDistance(index: Int, value: Double) {
		distances[index] = value
	}

	fun setEle(index: Int, value: Double) {
		elevations[index] = value
	}

	fun setSpeed(index: Int, value: Float) {
		speeds[index] = value
	}

	fun setHdop(index: Int, value: Float) {
		hdops[index] = value
	}

	fun setBearing(index: Int, value: Float) {
		bearings[index] = value
	}

	fun setHeading(index: Int, value: Float) {
		headings[index] = value
	}

	fun setColourArgb(index: Int, value: Int) {
		colorArgb[index] = value
	}

	fun setAltitudeColor(index: Int, value: Int) {
		altitudeColors[index] = value
	}

	fun setSpeedColor(index: Int, value: Int) {
		speedColors[index] = value
	}

	fun setSlopeColor(index: Int, value: Int) {
		slopeColors[index] = value
	}

	fun setDeleted(index: Int, value: Boolean) {
		flags[index] = flags[index].setFlag(FLAG_DELETED, value)
	}

	fun setFirstPoint(index: Int, value: Boolean) {
		flags[index] = flags[index].setFlag(FLAG_FIRST, value)
	}

	fun setLastPoint(index: Int, value: Boolean) {
		flags[index] = flags[index].setFlag(FLAG_LAST, value)
	}

	fun setName(index: Int, value: String?) {
		setExtrasValue(index, value) { name = it }
	}

	fun setDesc(index: Int, value: String?) {
		setExtrasValue(index, value) { desc = it }
	}

	fun setCategory(index: Int, value: String?) {
		setExtrasValue(index, value) { category = it }
	}

	fun setComment(index: Int, value: String?) {
		setExtrasValue(index, value) { comment = it }
	}

	fun setLink(index: Int, value: Link?) {
		setExtrasValue(index, value) { link = it }
	}

	fun getCategory(index: Int): String? = extras?.get(index)?.category

	fun putExtension(index: Int, key: String, value: String) {
		val pointExtras = getOrCreateExtras(index)
		val extensions = pointExtras.extensions ?: LinkedHashMap<String, String>().also {
			pointExtras.extensions = it
		}
		extensions[key] = value
	}

	private fun ensureCapacity(requiredSize: Int) {
		if (requiredSize <= latitudes.size) {
			return
		}
		val oldSize = latitudes.size
		val newSize = (latitudes.size * 2).coerceAtLeast(requiredSize)
		latitudes = latitudes.copyOf(newSize)
		longitudes = longitudes.copyOf(newSize)
		times = times.copyOf(newSize)
		distances = distances.copyOf(newSize)
		elevations = elevations.copyOf(newSize)
		for (index in oldSize until elevations.size) {
			elevations[index] = Double.NaN
		}
		speeds = speeds.copyOf(newSize)
		hdops = hdops.copyOf(newSize)
		for (index in oldSize until hdops.size) {
			hdops[index] = Float.NaN
		}
		bearings = bearings.copyOf(newSize)
		for (index in oldSize until bearings.size) {
			bearings[index] = Float.NaN
		}
		headings = headings.copyOf(newSize)
		for (index in oldSize until headings.size) {
			headings[index] = Float.NaN
		}
		colorArgb = colorArgb.copyOf(newSize)
		altitudeColors = altitudeColors.copyOf(newSize)
		speedColors = speedColors.copyOf(newSize)
		slopeColors = slopeColors.copyOf(newSize)
		flags = flags.copyOf(newSize)
	}

	private fun getOrCreateExtras(index: Int): TrackPointExtras {
		val currentExtras = extras ?: LinkedHashMap<Int, TrackPointExtras>().also { extras = it }
		return currentExtras[index] ?: TrackPointExtras().also { currentExtras[index] = it }
	}

	private inline fun <T> setExtrasValue(index: Int, value: T?, setter: TrackPointExtras.(T?) -> Unit) {
		val currentExtras = extras?.get(index)
		if (value != null || currentExtras != null) {
			(currentExtras ?: getOrCreateExtras(index)).setter(value)
		}
	}

	private fun Int.setFlag(flag: Int, value: Boolean): Int {
		return if (value) this or flag else this and flag.inv()
	}
}

internal class JoinedTrackPointSource(
	private val segments: List<JoinedSegmentSource>,
	private val markInnerSegments: Boolean
) : TrackPointSource {
	private val prefixSizes = IntArray(segments.size)

	override val size: Int

	init {
		var total = 0
		for (index in segments.indices) {
			total += segments[index].segment.getPointsSize()
			prefixSizes[index] = total
		}
		size = total
	}

	override fun copyPointTo(index: Int, point: WptPt) {
		val resolved = resolve(index)
		resolved.segment.copyPointTo(resolved.localIndex, point)
		if (markInnerSegments) {
			point.firstPoint = resolved.localIndex == 0
			point.lastPoint = resolved.localIndex == resolved.segment.getPointsSize() - 1
		}
	}

	override fun getLat(index: Int): Double {
		val resolved = resolve(index)
		return resolved.segment.getPointLat(resolved.localIndex)
	}

	override fun getLon(index: Int): Double {
		val resolved = resolve(index)
		return resolved.segment.getPointLon(resolved.localIndex)
	}

	override fun getTime(index: Int): Long {
		val resolved = resolve(index)
		return resolved.segment.getPointTime(resolved.localIndex)
	}

	override fun getEle(index: Int): Double {
		val resolved = resolve(index)
		return resolved.segment.getPointEle(resolved.localIndex)
	}

	private fun resolve(index: Int): ResolvedPoint {
		var segmentIndex = 0
		while (segmentIndex < prefixSizes.size && index >= prefixSizes[segmentIndex]) {
			segmentIndex++
		}
		val segmentStart = if (segmentIndex == 0) 0 else prefixSizes[segmentIndex - 1]
		return ResolvedPoint(segments[segmentIndex].segment, index - segmentStart)
	}

	private data class ResolvedPoint(val segment: TrkSegment, val localIndex: Int)
}

internal data class JoinedSegmentSource(val segment: TrkSegment)

private class TrackPointExtras {
	var name: String? = null
	var desc: String? = null
	var category: String? = null
	var comment: String? = null
	var link: Link? = null
	var attributes: PointAttributes? = null
	var extensions: MutableMap<String, String>? = null

	fun copyTo(point: WptPt) {
		point.name = name
		point.desc = desc
		point.category = category
		point.comment = comment
		point.link = link?.let { Link(it) }
		point.attributes = attributes
		val pointExtensions = extensions
		if (!pointExtensions.isNullOrEmpty()) {
			point.getExtensionsToWrite().putAll(pointExtensions)
		}
	}
}
