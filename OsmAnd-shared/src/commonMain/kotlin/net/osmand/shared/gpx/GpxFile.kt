package net.osmand.shared.gpx

import net.osmand.shared.KException
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.shared.gpx.GpxUtilities.RouteSegment
import net.osmand.shared.gpx.GpxUtilities.RouteType
import net.osmand.shared.gpx.GpxUtilities.updateQR
import net.osmand.shared.gpx.primitives.GpxExtensions
import net.osmand.shared.gpx.primitives.Metadata
import net.osmand.shared.gpx.primitives.Route
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.util.KMapUtils
import kotlin.collections.set


class GpxFile : GpxExtensions {
	var author: String? = null
	var metadata = Metadata()
	var tracks: MutableList<Track> = mutableListOf()
	var routes: MutableList<Route> = mutableListOf()
	private var points: MutableList<WptPt> = mutableListOf()
	var pointsGroups: MutableMap<String, PointsGroup> = LinkedHashMap()
	val networkRouteKeyTags: MutableMap<String, String> = LinkedHashMap()

	var error: KException? = null
	var path: String = ""
	var showCurrentTrack: Boolean = false
	var hasAltitude: Boolean = false
	var modifiedTime: Long = 0
	var pointsModifiedTime: Long = 0

	private var generalTrack: Track? = null
	private var generalSegment: TrkSegment? = null

	constructor(author: String?) {
		this.author = author
	}

	constructor(title: String?, lang: String?, description: String?) {
		if (description != null) {
			metadata.desc = description
		}
		if (lang != null) {
			metadata.getExtensionsToWrite()["article_lang"] = lang
		}
		if (title != null) {
			metadata.getExtensionsToWrite()["article_title"] = title
		}
	}

	fun isShowCurrentTrack() = showCurrentTrack

	fun hasAltitude() = hasAltitude

	fun hasRoute(): Boolean {
		return getNonEmptyTrkSegments(true).isNotEmpty()
	}

	fun getAllPoints(): List<WptPt> {
		val total = mutableListOf<WptPt>()
		total.addAll(getPointsList())
		total.addAll(getAllSegmentsPoints())
		return total
	}

	fun getPointsList(): List<WptPt> {
		return points.toList()
	}

	fun setPointsList(points: List<WptPt>) {
		this.points = points.toMutableList()
	}

	fun getAllSegmentsPoints(): List<WptPt> {
		val points = mutableListOf<WptPt>()
		for (track in tracks) {
			if (track.generalTrack) continue
			for (segment in track.segments) {
				if (segment.generalSegment) continue
				points.addAll(segment.points)
			}
		}
		return points
	}

	fun isPointsEmpty(): Boolean {
		return points.isEmpty()
	}

	fun getPointsSize(): Int {
		return points.size
	}

	fun containsPoint(point: WptPt): Boolean {
		return points.contains(point)
	}

	fun clearPoints() {
		points.clear()
		pointsGroups.clear()
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun addPoint(point: WptPt) {
		points.add(point)
		addPointsToGroups(setOf(point))
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun addParsedPoint(point: WptPt) {
		points.add(point)
	}

	fun addPoint(position: Int, point: WptPt) {
		points.add(position, point)
		addPointsToGroups(setOf(point))
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun addPoints(collection: Collection<WptPt>) {
		points.addAll(collection)
		addPointsToGroups(collection)
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun addPointsGroup(group: PointsGroup) {
		points.addAll(group.points)
		pointsGroups[group.name] = group
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	private fun addPointsToGroups(collection: Collection<WptPt>) {
		for (point in collection) {
			val pointsGroup = getOrCreateGroup(point)
			pointsGroup.points.add(point)
		}
	}

	private fun getOrCreateGroup(point: WptPt): PointsGroup {
		val groupName = point.category ?: DEFAULT_WPT_GROUP_NAME
		return pointsGroups[groupName] ?: PointsGroup(point).also { pointsGroups[it.name] = it }
	}

	fun deleteWptPt(point: WptPt): Boolean {
		removePointFromGroup(point)
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
		return points.remove(point)
	}

	fun deleteWptPt(wptName: String, index: Int): Boolean {
		val currentWpt = getWptPt(wptName, index)
		return currentWpt?.let { deleteWptPt(it) } ?: false
	}

	private fun removePointFromGroup(point: WptPt) {
		removePointFromGroup(point, point.category)
	}

	private fun removePointFromGroup(point: WptPt, groupName: String?) {
		val group = pointsGroups[groupName]
		group?.points?.remove(point)
	}

	fun updateWptPt(wptName: String, wptIndex: Int, newWpt: WptPt, updateTimestamp: Boolean) {
		val currentWpt = getWptPt(wptName, wptIndex)
		if (currentWpt != null) {
			updateWptPt(currentWpt, newWpt, updateTimestamp)
		} else {
			addPoint(newWpt)
		}
	}

	private fun getWptPt(wptName: String, wptIndex: Int): WptPt? {
		return if (wptIndex in points.indices && points[wptIndex].name == wptName) {
			points[wptIndex]
		} else null
	}

	fun updateWptPt(existingPoint: WptPt, newWpt: WptPt, updateTimestamp: Boolean = true) {
		val index = points.indexOf(existingPoint)
		if (index == -1) return

		val prevGroupName = existingPoint.category ?: DEFAULT_WPT_GROUP_NAME
		val prevTime = existingPoint.time
		existingPoint.updatePoint(newWpt)
		if (!updateTimestamp) {
			existingPoint.time = prevTime
		}
		if (newWpt.category != prevGroupName || (newWpt.category == null && prevGroupName != DEFAULT_WPT_GROUP_NAME)) {
			removePointFromGroup(existingPoint, prevGroupName)
			val pointsGroup = getOrCreateGroup(existingPoint)
			pointsGroup.points.add(existingPoint)
		}
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun updatePointsGroup(prevGroupName: String, pointsGroup: PointsGroup) {
		pointsGroups.remove(prevGroupName)
		pointsGroups[pointsGroup.name] = pointsGroup
		modifiedTime = currentTimeMillis()
	}

	fun isCloudmadeRouteFile(): Boolean {
		return author.equals("cloudmade", ignoreCase = true)
	}

	fun hasGeneralTrack(): Boolean {
		return generalTrack != null
	}

	fun addGeneralTrack() {
		val generalTrack = getGeneralTrack()
		if (generalTrack != null && !tracks.contains(generalTrack)) {
			tracks.add(0, generalTrack)
		}
	}

	fun getGeneralTrack(): Track? {
		if (generalTrack == null && getGeneralSegment() != null) {
			generalTrack = Track().apply {
				segments = mutableListOf(getGeneralSegment()!!)
				generalTrack = true
			}
		}
		return generalTrack
	}

	fun getGeneralSegment(): TrkSegment? {
		if (generalSegment == null && getNonEmptySegmentsCount() > 1) {
			buildGeneralSegment()
		}
		return generalSegment
	}

	private fun buildGeneralSegment() {
		val segment = TrkSegment()
		for (track in tracks) {
			for (trkSegment in track.segments) {
				if (trkSegment.points.isNotEmpty()) {
					val waypoints = trkSegment.points.map { WptPt(it) }.toMutableList()
					waypoints.first().firstPoint = true
					waypoints.last().lastPoint = true
					segment.points.addAll(waypoints)
				}
			}
		}
		if (segment.points.isNotEmpty()) {
			segment.generalSegment = true
			generalSegment = segment
		}
	}

	fun getAnalysis(fileTimestamp: Long): GpxTrackAnalysis {
		return getAnalysis(fileTimestamp, null, null, null)
	}

	fun getAnalysis(
		fileTimestamp: Long,
		fromDistance: Double?,
		toDistance: Double?,
		pointsAnalyzer: GpxTrackAnalysis.TrackPointsAnalyser?
	): GpxTrackAnalysis {
		val analysis = GpxTrackAnalysis()
		analysis.name = path
		analysis.wptPoints = points.size
		analysis.setWptCategoryNames(getWaypointCategories())

		val segments = getSplitSegments(analysis, fromDistance, toDistance)
		analysis.prepareInformation(fileTimestamp, pointsAnalyzer, *segments.toTypedArray())
		return analysis
	}

	private fun getSplitSegments(
		analysis: GpxTrackAnalysis,
		fromDistance: Double?,
		toDistance: Double?
	): List<SplitSegment> {
		val splitSegments = mutableListOf<SplitSegment>()
		for (subtrack in tracks) {
			for (segment in subtrack.segments) {
				if (!segment.generalSegment) {
					analysis.totalTracks += 1
					if (segment.points.size > 1) {
						splitSegments.add(createSplitSegment(segment, fromDistance, toDistance))
					}
				}
			}
		}
		return splitSegments
	}

	private fun createSplitSegment(
		segment: TrkSegment,
		fromDistance: Double?,
		toDistance: Double?
	): SplitSegment {
		return if (fromDistance != null && toDistance != null) {
			val startInd = getPointIndexByDistance(segment.points, fromDistance)
			val endInd = getPointIndexByDistance(segment.points, toDistance)
			SplitSegment(startInd, endInd, segment)
		} else {
			SplitSegment(segment)
		}
	}

	fun getPointIndexByDistance(points: List<WptPt>, distance: Double): Int {
		return points.indexOf(points.minByOrNull { kotlin.math.abs(it.distance - distance) })
	}

	fun containsRoutePoint(point: WptPt): Boolean {
		return getRoutePoints().contains(point)
	}

	fun getRoutePoints(): List<WptPt> {
		val points = mutableListOf<WptPt>()
		for (route in routes) {
			points.addAll(route.points)
		}
		return points
	}

	fun getRoutePoints(routeIndex: Int): List<WptPt> {
		val points = mutableListOf<WptPt>()
		if (routes.size > routeIndex) {
			points.addAll(routes[routeIndex].points)
		}
		return points
	}

	fun isAttachedToRoads(): Boolean {
		val points = getRoutePoints()
		if (points.isNotEmpty()) {
			for (wptPt in points) {
				if (wptPt.getProfileType().isNullOrEmpty()) {
					return false
				}
			}
			return true
		}
		return false
	}

	fun hasRtePt(): Boolean {
		for (route in routes) {
			if (route.points.isNotEmpty()) {
				return true
			}
		}
		return false
	}

	fun hasWptPt(): Boolean {
		return points.isNotEmpty()
	}

	fun hasTrkPt(): Boolean {
		for (track in tracks) {
			for (segment in track.segments) {
				if (segment.points.isNotEmpty()) {
					return true
				}
			}
		}
		return false
	}

	fun getNonEmptyTrkSegments(routesOnly: Boolean): List<TrkSegment> {
		val segments = mutableListOf<TrkSegment>()
		for (track in tracks) {
			for (segment in track.segments) {
				if (!segment.generalSegment && segment.points.isNotEmpty() && (!routesOnly || segment.hasRoute())) {
					segments.add(segment)
				}
			}
		}
		return segments
	}

	fun addTrkSegment(points: List<WptPt>) {
		removeGeneralTrackIfExists()

		val segment = TrkSegment()
		segment.points.addAll(points)

		if (tracks.isEmpty()) {
			tracks.add(Track())
		}
		val lastTrack = tracks.last()
		lastTrack.segments.add(segment)

		modifiedTime = currentTimeMillis()
	}

	fun replaceSegment(oldSegment: TrkSegment, newSegment: TrkSegment): Boolean {
		removeGeneralTrackIfExists()

		for (currentTrack in tracks) {
			val segmentIndex = currentTrack.segments.indexOf(oldSegment)
			if (segmentIndex != -1) {
				currentTrack.segments[segmentIndex] = newSegment
				addGeneralTrack()
				modifiedTime = currentTimeMillis()
				return true
			}
		}

		addGeneralTrack()
		return false
	}

	fun addRoutePoints(points: List<WptPt>, addRoute: Boolean) {
		if (routes.isEmpty() || addRoute) {
			routes.add(Route())
		}

		val lastRoute = routes.last()
		lastRoute.points.addAll(points)
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	fun replaceRoutePoints(points: List<WptPt>) {
		routes.clear()
		routes.add(Route())
		val currentRoute = routes.last()
		currentRoute.points.addAll(points)
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
	}

	private fun removeGeneralTrackIfExists() {
		if (generalTrack != null) {
			tracks.remove(generalTrack)
			generalTrack = null
			generalSegment = null
		}
	}

	fun removeTrkSegment(segment: TrkSegment): Boolean {
		removeGeneralTrackIfExists()

		for (currentTrack in tracks) {
			if (currentTrack.segments.remove(segment)) {
				addGeneralTrack()
				modifiedTime = currentTimeMillis()
				return true
			}
		}
		addGeneralTrack()
		return false
	}

	fun deleteRtePt(pt: WptPt): Boolean {
		modifiedTime = currentTimeMillis()
		pointsModifiedTime = modifiedTime
		for (route in routes) {
			if (route.points.remove(pt)) {
				return true
			}
		}
		return false
	}

	fun processRoutePoints(): List<TrkSegment> {
		val tpoints = mutableListOf<TrkSegment>()
		if (routes.isNotEmpty()) {
			for (route in routes) {
				val routeColor = route.getColor(getColor(0))
				if (route.points.isNotEmpty()) {
					val ts = TrkSegment()
					tpoints.add(ts)
					ts.points.addAll(route.points)
					ts.setColor(routeColor)
				}
			}
		}
		return tpoints
	}

	fun processPoints(): List<TrkSegment> {
		val tpoints = mutableListOf<TrkSegment>()
		for (track in tracks) {
			val trackColor = track.getColor(getColor(0))
			for (segment in track.segments) {
				if (!segment.generalSegment && segment.points.isNotEmpty()) {
					val ts = TrkSegment()
					tpoints.add(ts)
					ts.points.addAll(segment.points)
					ts.setColor(trackColor)
				}
			}
		}
		return tpoints
	}

	fun getLastPoint(): WptPt? {
		return tracks.lastOrNull()?.segments?.lastOrNull()?.points?.lastOrNull()
	}

	fun findPointToShow(): WptPt? {
		for (track in tracks) {
			for (segment in track.segments) {
				if (segment.points.isNotEmpty()) {
					return segment.points[0]
				}
			}
		}
		for (route in routes) {
			if (route.points.isNotEmpty()) {
				return route.points[0]
			}
		}
		return points.firstOrNull()
	}

	fun isEmpty(): Boolean {
		for (track in tracks) {
			for (segment in track.segments) {
				if (segment.points.isNotEmpty()) {
					return false
				}
			}
		}
		return points.isEmpty() && routes.isEmpty()
	}

	fun getTracks(includeGeneralTrack: Boolean): List<Track> {
		return tracks.filter { includeGeneralTrack || !it.generalTrack }
	}

	fun getSegments(includeGeneralTrack: Boolean): List<TrkSegment> {
		return tracks.flatMap { track -> track.segments.filter { includeGeneralTrack || !track.generalTrack } }
	}

	fun getTracksCount(): Int {
		return tracks.count { !it.generalTrack }
	}

	fun getNonEmptyTracksCount(): Int {
		return tracks.count { track -> track.segments.any { it.points.isNotEmpty() } }
	}

	fun getNonEmptySegmentsCount(): Int {
		return tracks.sumOf { track -> track.segments.count { it.points.isNotEmpty() } }
	}

	fun getWaypointCategories(): Set<String> {
		return pointsGroups.keys
	}

	fun getRect(): KQuadRect {
		return getBounds(0.0, 0.0)
	}

	fun getBounds(defaultMissingLat: Double, defaultMissingLon: Double): KQuadRect {
		val qr =
			KQuadRect(defaultMissingLon, defaultMissingLat, defaultMissingLon, defaultMissingLat)
		for (track in tracks) {
			for (segment in track.segments) {
				for (p in segment.points) {
					updateQR(qr, p, defaultMissingLat, defaultMissingLon)
				}
			}
		}
		for (p in points) {
			updateQR(qr, p, defaultMissingLat, defaultMissingLon)
		}
		for (route in routes) {
			for (p in route.points) {
				updateQR(qr, p, defaultMissingLat, defaultMissingLon)
			}
		}
		return qr
	}

	fun getColoringType(): String? {
		return extensions?.get("coloring_type")
	}

	fun getGradientScaleType(): String? {
		return extensions?.get("gradient_scale_type")
	}

	fun setColoringType(coloringType: String) {
		getExtensionsToWrite()["coloring_type"] = coloringType
	}

	fun removeGradientScaleType() {
		getExtensionsToWrite().remove("gradient_scale_type")
	}

	fun getSplitType(): String? {
		return extensions?.get("split_type")
	}

	fun setSplitType(gpxSplitType: String) {
		getExtensionsToWrite()["split_type"] = gpxSplitType
	}

	fun getSplitInterval(): Double {
		val splitIntervalStr = extensions?.get("split_interval")
		return splitIntervalStr?.toDoubleOrNull() ?: 0.0
	}

	fun setSplitInterval(splitInterval: Double) {
		getExtensionsToWrite()["split_interval"] = splitInterval.toString()
	}

	fun getWidth(defWidth: String?): String? {
		return extensions?.get("width") ?: defWidth
	}

	fun setWidth(width: String) {
		getExtensionsToWrite()["width"] = width
	}

	fun isShowArrowsSet(): Boolean {
		return extensions?.containsKey("show_arrows") ?: false
	}

	fun isShowArrows(): Boolean {
		return extensions?.get("show_arrows")?.toBoolean() ?: false
	}

	fun setShowArrows(showArrows: Boolean) {
		getExtensionsToWrite()["show_arrows"] = showArrows.toString()
	}

	fun get3DVisualizationType(): String? {
		return extensions?.get("line_3d_visualization_by_type")
	}

	fun set3DVisualizationType(visualizationType: String) {
		getExtensionsToWrite()["line_3d_visualization_by_type"] = visualizationType
	}

	fun get3DWallColoringType(): String? {
		return extensions?.get("line_3d_visualization_wall_color_type")
	}

	fun set3DWallColoringType(trackWallColoringType: String) {
		getExtensionsToWrite()["line_3d_visualization_wall_color_type"] = trackWallColoringType
	}

	fun get3DLinePositionType(): String? {
		return extensions?.get("line_3d_visualization_position_type")
	}

	fun set3DLinePositionType(trackLinePositionType: String) {
		getExtensionsToWrite()["line_3d_visualization_position_type"] = trackLinePositionType
	}

	fun setAdditionalExaggeration(additionalExaggeration: Float) {
		getExtensionsToWrite()["vertical_exaggeration_scale"] = additionalExaggeration.toString()
	}

	fun getAdditionalExaggeration(): Float {
		return extensions?.get("vertical_exaggeration_scale")?.toFloatOrNull() ?: 1f
	}

	fun setElevationMeters(elevation: Float) {
		getExtensionsToWrite()["elevation_meters"] = elevation.toString()
	}

	fun getElevationMeters(): Float {
		return extensions?.get("elevation_meters")?.toFloatOrNull() ?: 1000f
	}

	fun isShowStartFinishSet(): Boolean {
		return extensions?.containsKey("show_start_finish") ?: false
	}

	fun isShowStartFinish(): Boolean {
		return extensions?.get("show_start_finish")?.toBoolean() ?: true
	}

	fun setShowStartFinish(showStartFinish: Boolean) {
		getExtensionsToWrite()["show_start_finish"] = showStartFinish.toString()
	}

	fun addRouteKeyTags(routeKey: Map<String, String>) {
		networkRouteKeyTags.putAll(routeKey)
	}

	fun getRouteKeyTags(): Map<String, String> {
		return networkRouteKeyTags
	}

	fun setRef(ref: String) {
		getExtensionsToWrite()["ref"] = ref
	}

	fun getRef(): String? {
		return extensions?.get("ref")
	}

	fun getOuterRadius(): String {
		val rect = getRect()
		val radius = KMapUtils.getDistance(rect.bottom, rect.left, rect.top, rect.right).toInt()
		return KMapUtils.convertDistToChar(
			radius,
			GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER,
			GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST,
			GpxUtilities.TRAVEL_GPX_CONVERT_MULT_1,
			GpxUtilities.TRAVEL_GPX_CONVERT_MULT_2
		)
	}

	fun getArticleTitle(): String? {
		return metadata.getArticleTitle()
	}

	fun getItemsToWriteSize(): Int {
		var size = getPointsSize()
		for (route in routes) {
			size += route.points.size
		}
		for (segment in getNonEmptyTrkSegments(false)) {
			size += segment.points.size
		}

		size++ // metadata
		if (metadata.author != null) size++
		if (metadata.copyright != null) size++
		if (metadata.bounds != null) size++
		size += getExtensionsToWrite().size
		size += extensionsWriters?.size ?: 0
		return size
	}

	fun getLastPointTime(): Long {
		return listOf(
			getLastPointTime(getAllSegmentsPoints()),
			getLastPointTime(getRoutePoints()),
			getLastPointTime(getPointsList())
		).firstOrNull { it > 0 } ?: 0
	}

	private fun getLastPointTime(points: List<WptPt>): Long {
		return points.asReversed().firstOrNull { it.time > 0 }?.time ?: 0
	}

	fun clone(): GpxFile {
		val dest = GpxFile(this.author)
		dest.metadata = Metadata(this.metadata)
		val tracks = dest.tracks
		for (track in this.tracks) {
			tracks.add(cloneTrack(track))
		}
		val routes = dest.routes
		for (route in this.routes) {
			routes.add(cloneRoute(route))
		}
		val points = mutableListOf<WptPt>()
		for (point in this.points) {
			points.add(WptPt(point))
		}
		dest.setPointsList(points)

		dest.pointsGroups = LinkedHashMap()
		val pointsGroups = dest.pointsGroups
		for ((key, value) in this.pointsGroups.entries) {
			pointsGroups[key] = clonePointsGroup(value)
		}
		dest.pointsGroups = pointsGroups

		dest.addRouteKeyTags(this.getRouteKeyTags())

		dest.path = this.path
		dest.showCurrentTrack = this.showCurrentTrack
		dest.hasAltitude = this.hasAltitude
		dest.modifiedTime = currentTimeMillis()
		dest.pointsModifiedTime = dest.modifiedTime
		dest.copyExtensions(this)

		return dest
	}

	private fun cloneTrack(source: Track): Track {
		val dest = Track()
		dest.name = source.name
		dest.desc = source.desc
		dest.generalTrack = source.generalTrack
		val trkSegments = mutableListOf<TrkSegment>()
		for (segment in source.segments) {
			trkSegments.add(cloneTrkSegment(segment))
		}
		dest.segments = trkSegments
		copyExtensions(source)
		return dest
	}

	private fun cloneRoute(source: Route): Route {
		val dest = Route()
		dest.name = source.name
		dest.desc = source.desc
		val points = mutableListOf<WptPt>()
		for (point in source.points) {
			points.add(WptPt(point))
		}
		dest.points = points
		copyExtensions(source)
		return dest
	}

	private fun clonePointsGroup(source: PointsGroup): PointsGroup {
		val dest = PointsGroup(source.name, source.iconName, source.backgroundType, source.color)
		dest.hidden = source.hidden
		val points = mutableListOf<WptPt>()
		for (point in source.points) {
			points.add(WptPt(point))
		}
		dest.points = points
		return dest
	}

	private fun cloneTrkSegment(source: TrkSegment): TrkSegment {
		val dest = TrkSegment()
		dest.name = source.name
		dest.generalSegment = source.generalSegment
		val kPoints = mutableListOf<WptPt>()
		for (point in source.points) {
			kPoints.add(WptPt(point))
		}
		dest.points = kPoints
		val routeSegments = mutableListOf<RouteSegment>()
		for (rs in source.routeSegments) {
			routeSegments.add(cloneRouteSegment(rs))
		}
		dest.routeSegments = routeSegments
		val routeTypes = mutableListOf<RouteType>()
		for (rt in source.routeTypes) {
			routeTypes.add(cloneRouteType(rt))
		}
		dest.routeTypes = routeTypes
		copyExtensions(source)
		return dest
	}

	private fun cloneRouteSegment(source: RouteSegment): RouteSegment {
		val dest = RouteSegment()
		dest.id = source.id
		dest.length = source.length
		dest.startTrackPointIndex = source.startTrackPointIndex
		dest.segmentTime = source.segmentTime
		dest.speed = source.speed
		dest.turnType = source.turnType
		dest.turnLanes = source.turnLanes
		dest.turnAngle = source.turnAngle
		dest.skipTurn = source.skipTurn
		dest.types = source.types
		dest.pointTypes = source.pointTypes
		dest.names = source.names
		return dest
	}

	private fun cloneRouteType(source: RouteType): RouteType {
		val dest = RouteType()
		dest.tag = source.tag
		dest.value = source.value
		return dest
	}

	fun getGradientColorPalette(): String? {
		return extensions?.get("color_palette")
	}

	fun setGradientColorPalette(gradientColorPaletteName: String) {
		getExtensionsToWrite()["color_palette"] = gradientColorPaletteName
	}

	fun getRouteByName(name: String?): Route? {
		for (route in routes) {
			if (route.name == name) {
				return route
			}
		}
		return null
	}

	fun isOsmAndOrigin() = author?.startsWith(OSMAND_AUTHOR_PREFIX, ignoreCase = true) ?: false

	companion object {
		const val OSMAND_AUTHOR_PREFIX = "OsmAnd"
		const val DEFAULT_WPT_GROUP_NAME = ""
	}
}
