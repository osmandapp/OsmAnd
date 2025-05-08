package net.osmand.plus.shared

import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.gpx.GPXFile
import net.osmand.gpx.GPXUtilities
import net.osmand.shared.KException
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.jFile
import net.osmand.shared.extensions.kFile
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.primitives.Author
import net.osmand.shared.gpx.primitives.Bounds
import net.osmand.shared.gpx.primitives.Copyright
import net.osmand.shared.gpx.primitives.GpxExtensions
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsReader
import net.osmand.shared.gpx.primitives.Metadata
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.shared.gpx.primitives.Route
import net.osmand.shared.gpx.GpxUtilities.RouteSegment
import net.osmand.shared.gpx.GpxUtilities.RouteType
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.util.IProgress
import net.osmand.shared.util.PlatformUtil
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object SharedUtil {

	@JvmStatic
	fun kLatLon(latLon: LatLon): KLatLon {
		return KLatLon(latLon.latitude, latLon.longitude)
	}

	@JvmStatic
	fun jLatLon(latLon: KLatLon): LatLon {
		return LatLon(latLon.latitude, latLon.longitude)
	}

	@JvmStatic
	fun kQuadRect(rect: QuadRect): KQuadRect {
		return KQuadRect(rect.left, rect.top, rect.right, rect.bottom)
	}

	@JvmStatic
	fun jQuadRect(rect: KQuadRect): QuadRect {
		return QuadRect(rect.left, rect.top, rect.right, rect.bottom)
	}

	@JvmStatic
	fun kFile(file: File): KFile = file.kFile()

	@JvmStatic
	fun jFile(file: KFile): File = file.jFile()

	@JvmStatic
	fun kException(e: Exception): KException {
		return PlatformUtil.getKotlinException(e)
	}

	@JvmStatic
	fun jException(ke: KException): Exception {
		return PlatformUtil.getJavaException(ke)
	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> castGpxParameter(parameter: GpxParameter, value: Any?): T? {
		return if (parameter.typeClass.isInstance(value)) value as T else null
	}

	@JvmStatic
	fun isGpxParameterClass(parameter: GpxParameter, javaClass: Class<*>): Boolean {
		return parameter.typeClass.javaObjectType == javaClass
	}

	@JvmStatic
	fun loadGpxFile(file: File): GpxFile {
		return loadGpxFile(kFile(file), null, true)
	}

	@JvmStatic
	fun loadGpxFile(file: KFile): GpxFile {
		return loadGpxFile(file, null, true)
	}

	@JvmStatic
	fun loadGpxFile(
		file: File,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile =
		GpxUtilities.loadGpxFile(kFile(file), extensionsReader, addGeneralTrack)

	@JvmStatic
	fun loadGpxFile(
		file: KFile,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile =
		GpxUtilities.loadGpxFile(file, extensionsReader, addGeneralTrack)

	@JvmStatic
	fun loadGpxFile(source: Source): GpxFile {
		return loadGpxFile(source, null, true)
	}

	@JvmStatic
	fun loadGpxFile(inputStream: InputStream): GpxFile {
		return loadGpxFile(inputStream, null, true)
	}

	@JvmStatic
	fun loadGpxFile(
		inputStream: InputStream,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile =
		GpxUtilities.loadGpxFile(null, inputStream.source(), extensionsReader, addGeneralTrack)

	@JvmStatic
	fun loadGpxFile(
		source: Source,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile =
		GpxUtilities.loadGpxFile(null, source, extensionsReader, addGeneralTrack)

	@JvmStatic
	fun writeGpxFile(fout: File, file: GpxFile): Exception? =
		writeGpxFile(kFile(fout), file)

	@JvmStatic
	fun writeGpxFile(fout: KFile, file: GpxFile): Exception? =
		GpxUtilities.writeGpxFile(fout, file)

	@JvmStatic
	fun writeGpx(output: OutputStream, file: GpxFile, progress: IProgress?): Exception? =
		GpxUtilities.writeGpx(null, output.sink(), file, progress)

	@JvmStatic
	fun writeGpx(output: Sink, file: GpxFile, progress: IProgress?): Exception? =
		GpxUtilities.writeGpx(null, output, file, progress)

	@JvmStatic
	fun kIProgress(progress: net.osmand.IProgress?) : IProgress? {
		if (progress == null) {
			return null;
		}
		return object : IProgress {
			override fun startTask(taskName: String, work: Int) = progress.startTask(taskName, work)

			override fun startWork(work: Int) = progress.startWork(work)

			override fun progress(deltaWork: Int) = progress.progress(deltaWork)

			override fun remaining(remainingWork: Int) = progress.remaining(remainingWork)

			override fun finishTask() = progress.finishTask()

			override fun isIndeterminate(): Boolean = progress.isIndeterminate

			override fun isInterrupted(): Boolean = progress.isInterrupted

			override fun setGeneralProgress(genProgress: String) = progress.setGeneralProgress(genProgress)
		}
	}

	@JvmStatic
	fun jGpxFile(gpxFile: GpxFile): GPXFile {
		val jGpxFile = GPXFile(gpxFile.author)
		jGpxFile.path = gpxFile.path
		jGpxFile.metadata = jMetadata(gpxFile.metadata)
		val jTracks = jGpxFile.tracks
		for (track in gpxFile.tracks) {
			jTracks.add(jTrack(track))
		}
		val jRoutes = jGpxFile.routes
		for (route in gpxFile.routes) {
			jRoutes.add(jRoute(route))
		}
		for (point in gpxFile.getPointsList()) {
			jGpxFile.addParsedPoint(jWptPt(point))
		}
		val jPointsGroups = jGpxFile.pointsGroups
		for ((key, value) in gpxFile.pointsGroups) {
			jPointsGroups[key] = jPointsGroup(value)
		}
		jGpxFile.pointsGroups = jPointsGroups
		jGpxFile.addRouteKeyTags(gpxFile.getRouteKeyTags())
		return jGpxFile
	}

	fun jTrack(track: Track): GPXUtilities.Track {
		val jTrack = GPXUtilities.Track()
		jTrack.name = track.name
		jTrack.desc = track.desc
		jTrack.generalTrack = track.generalTrack
		val jTrkSegments: MutableList<GPXUtilities.TrkSegment> = ArrayList()
		for (segment in track.segments) {
			jTrkSegments.add(jTrkSegment(segment))
		}
		jTrack.segments = jTrkSegments
		copyExtensions(track, jTrack)
		return jTrack
	}

	@JvmStatic
	fun jRoute(route: Route): GPXUtilities.Route {
		val jRoute = GPXUtilities.Route()
		jRoute.name = route.name
		jRoute.desc = route.desc
		val jPoints: MutableList<GPXUtilities.WptPt> = ArrayList()
		for (point in route.points) {
			jPoints.add(jWptPt(point))
		}
		jRoute.points = jPoints
		copyExtensions(route, jRoute)
		return jRoute
	}

	@JvmStatic
	fun jPointsGroup(group: PointsGroup): GPXUtilities.PointsGroup {
		val jGroup =
			GPXUtilities.PointsGroup(group.name, group.iconName, group.backgroundType, group.color)
		jGroup.hidden = group.hidden
		val jPoints: MutableList<GPXUtilities.WptPt> = ArrayList()
		for (point in group.points) {
			jPoints.add(jWptPt(point))
		}
		jGroup.points = jPoints
		return jGroup
	}

	@JvmStatic
	fun jTrkSegment(segment: TrkSegment): GPXUtilities.TrkSegment {
		val jSegment = GPXUtilities.TrkSegment()
		jSegment.name = segment.name
		jSegment.generalSegment = segment.generalSegment
		val jPoints: MutableList<GPXUtilities.WptPt> = ArrayList()
		for (point in segment.points) {
			jPoints.add(jWptPt(point))
		}
		jSegment.points = jPoints
		val jRouteSegments: MutableList<GPXUtilities.RouteSegment> = ArrayList()
		for (rs in segment.routeSegments) {
			jRouteSegments.add(jRouteSegment(rs))
		}
		jSegment.routeSegments = jRouteSegments
		val jRouteTypes: MutableList<GPXUtilities.RouteType> = ArrayList()
		for (rt in segment.routeTypes) {
			jRouteTypes.add(jRouteType(rt))
		}
		jSegment.routeTypes = jRouteTypes
		copyExtensions(segment, jSegment)
		return jSegment
	}

	@JvmStatic
	fun jWptPtList(points: List<WptPt>): List<GPXUtilities.WptPt> {
		val res = mutableListOf<GPXUtilities.WptPt>()
		for (point in points) {
			res.add(jWptPt(point));
		}
		return res
	}

	@JvmStatic
	fun kWptPtList(points: List<GPXUtilities.WptPt>): List<WptPt> {
		val res = mutableListOf<WptPt>()
		for (point in points) {
			res.add(kWptPt(point));
		}
		return res
	}

	@JvmStatic
	fun jWptPt(point: WptPt): GPXUtilities.WptPt {
		val jPoint = GPXUtilities.WptPt()
		jPoint.firstPoint = point.firstPoint
		jPoint.lastPoint = point.lastPoint
		jPoint.lat = point.lat
		jPoint.lon = point.lon
		jPoint.name = point.name
		jPoint.link = point.link
		jPoint.category = point.category
		jPoint.desc = point.desc
		jPoint.comment = point.comment
		jPoint.time = point.time
		jPoint.ele = point.ele
		jPoint.speed = point.speed
		jPoint.hdop = point.hdop
		jPoint.heading = point.heading
		jPoint.bearing = point.bearing
		jPoint.deleted = point.deleted
		jPoint.speedColor = point.speedColor
		jPoint.altitudeColor = point.altitudeColor
		jPoint.slopeColor = point.slopeColor
		jPoint.colourARGB = point.colourARGB
		jPoint.distance = point.distance
		copyExtensions(point, jPoint)
		return jPoint
	}

	@JvmStatic
	fun jRouteSegment(rs: RouteSegment): GPXUtilities.RouteSegment {
		val jRs = GPXUtilities.RouteSegment()
		jRs.id = rs.id
		jRs.length = rs.length
		jRs.startTrackPointIndex = rs.startTrackPointIndex
		jRs.segmentTime = rs.segmentTime
		jRs.speed = rs.speed
		jRs.turnType = rs.turnType
		jRs.turnLanes = rs.turnLanes
		jRs.turnAngle = rs.turnAngle
		jRs.skipTurn = rs.skipTurn
		jRs.types = rs.types
		jRs.pointTypes = rs.pointTypes
		jRs.names = rs.names
		return jRs
	}

	@JvmStatic
	fun jRouteType(rt: RouteType): GPXUtilities.RouteType {
		val jRt = GPXUtilities.RouteType()
		jRt.tag = rt.tag
		jRt.value = rt.value
		return jRt
	}

	@JvmStatic
	fun jMetadata(metadata: Metadata): GPXUtilities.Metadata {
		val jMetadata = GPXUtilities.Metadata()
		jMetadata.name = metadata.name
		jMetadata.link = metadata.link
		jMetadata.keywords = metadata.keywords
		jMetadata.time = metadata.time
		val author = metadata.author
		if (author != null) {
			jMetadata.author = jAuthor(author)
		}
		val copyright = metadata.copyright
		if (copyright != null) {
			jMetadata.copyright = jCopyright(copyright)
		}
		val bounds = metadata.bounds
		if (bounds != null) {
			jMetadata.bounds = jBounds(bounds)
		}
		copyExtensions(metadata, jMetadata)
		return jMetadata
	}

	@JvmStatic
	fun jAuthor(author: Author): GPXUtilities.Author {
		val jAuthor = GPXUtilities.Author()
		jAuthor.name = author.name
		jAuthor.link = author.link
		jAuthor.email = author.email
		copyExtensions(author, jAuthor)
		return jAuthor
	}

	@JvmStatic
	fun jCopyright(copyright: Copyright): GPXUtilities.Copyright {
		val jCopyright = GPXUtilities.Copyright()
		jCopyright.author = copyright.author
		jCopyright.year = copyright.year
		jCopyright.license = copyright.license
		copyExtensions(copyright, jCopyright)
		return jCopyright
	}

	@JvmStatic
	fun jBounds(bounds: Bounds): GPXUtilities.Bounds {
		val jBounds = GPXUtilities.Bounds()
		jBounds.minlat = bounds.minlat
		jBounds.minlon = bounds.minlon
		jBounds.maxlat = bounds.maxlat
		jBounds.maxlon = bounds.maxlon
		copyExtensions(bounds, jBounds)
		return jBounds
	}

	@JvmStatic
	fun copyExtensions(source: GpxExtensions, destination: GPXUtilities.GPXExtensions) {
		val extensionsToRead = source.getExtensionsToRead()
		if (extensionsToRead.isNotEmpty()) {
			destination.extensionsToWrite.putAll(extensionsToRead)
		}
	}

	@JvmStatic
	fun kGpxFile(gpxFile: GPXFile): GpxFile {
		val kGpxFile = GpxFile(gpxFile.author)
		kGpxFile.path = gpxFile.path
		if (gpxFile.metadata != null) {
			kGpxFile.metadata = kMetadata(gpxFile.metadata)
		}
		val kTracks = kGpxFile.tracks
		for (track in gpxFile.tracks) {
			kTracks.add(kTrack(track))
		}
		val kRoutes = kGpxFile.routes
		for (route in gpxFile.routes) {
			kRoutes.add(kRoute(route))
		}
		val kPoints: MutableList<WptPt> = ArrayList()
		for (point in gpxFile.points) {
			kPoints.add(kWptPt(point))
		}
		kGpxFile.setPointsList(kPoints)
		val kPointsGroups = kGpxFile.pointsGroups
		for ((key, value) in gpxFile.pointsGroups) {
			kPointsGroups[key] = kPointsGroup(value)
		}
		kGpxFile.pointsGroups = kPointsGroups
		kGpxFile.addRouteKeyTags(gpxFile.routeKeyTags)
		return kGpxFile
	}

	@JvmStatic
	fun kTrack(track: GPXUtilities.Track): Track {
		val kTrack = Track()
		kTrack.name = track.name
		kTrack.desc = track.desc
		kTrack.generalTrack = track.generalTrack
		val kTrkSegments: MutableList<TrkSegment> = ArrayList()
		for (segment in track.segments) {
			kTrkSegments.add(kTrkSegment(segment))
		}
		kTrack.segments = kTrkSegments
		copyExtensions(track, kTrack)
		return kTrack
	}

	@JvmStatic
	fun kRoute(route: GPXUtilities.Route): Route {
		val kRoute = Route()
		kRoute.name = route.name
		kRoute.desc = route.desc
		val kPoints: MutableList<WptPt> = ArrayList()
		for (point in route.points) {
			kPoints.add(kWptPt(point))
		}
		kRoute.points = kPoints
		copyExtensions(route, kRoute)
		return kRoute
	}

	@JvmStatic
	fun kPointsGroup(group: GPXUtilities.PointsGroup): PointsGroup {
		val kGroup =
			PointsGroup(group.name, group.iconName, group.backgroundType, group.color)
		kGroup.hidden = group.hidden
		val kPoints: MutableList<WptPt> = ArrayList()
		for (point in group.points) {
			kPoints.add(kWptPt(point))
		}
		kGroup.points = kPoints
		return kGroup
	}

	@JvmStatic
	fun kTrkSegment(segment: GPXUtilities.TrkSegment): TrkSegment {
		val kSegment = TrkSegment()
		kSegment.name = segment.name
		kSegment.generalSegment = segment.generalSegment
		val kPoints: MutableList<WptPt> = ArrayList()
		for (point in segment.points) {
			kPoints.add(kWptPt(point))
		}
		kSegment.points = kPoints
		val kRouteSegments: MutableList<RouteSegment> = ArrayList()
		for (rs in segment.routeSegments) {
			kRouteSegments.add(kRouteSegment(rs))
		}
		kSegment.routeSegments = kRouteSegments
		val kRouteTypes: MutableList<RouteType> = ArrayList()
		for (rt in segment.routeTypes) {
			kRouteTypes.add(kRouteType(rt))
		}
		kSegment.routeTypes = kRouteTypes
		copyExtensions(segment, kSegment)
		return kSegment
	}

	@JvmStatic
	fun kWptPt(point: GPXUtilities.WptPt): WptPt {
		val kPoint = WptPt()
		kPoint.firstPoint = point.firstPoint
		kPoint.lastPoint = point.lastPoint
		kPoint.lat = point.lat
		kPoint.lon = point.lon
		kPoint.name = point.name
		kPoint.link = point.link
		kPoint.category = point.category
		kPoint.desc = point.desc
		kPoint.comment = point.comment
		kPoint.time = point.time
		kPoint.ele = point.ele
		kPoint.speed = point.speed
		kPoint.hdop = point.hdop
		kPoint.heading = point.heading
		kPoint.bearing = point.bearing
		kPoint.deleted = point.deleted
		kPoint.speedColor = point.speedColor
		kPoint.altitudeColor = point.altitudeColor
		kPoint.slopeColor = point.slopeColor
		kPoint.colourARGB = point.colourARGB
		kPoint.distance = point.distance
		copyExtensions(point, kPoint)
		return kPoint
	}

	@JvmStatic
	fun kRouteSegment(rs: GPXUtilities.RouteSegment): RouteSegment {
		val kRs = RouteSegment()
		kRs.id = rs.id
		kRs.length = rs.length
		kRs.startTrackPointIndex = rs.startTrackPointIndex
		kRs.segmentTime = rs.segmentTime
		kRs.speed = rs.speed
		kRs.turnType = rs.turnType
		kRs.turnLanes = rs.turnLanes
		kRs.turnAngle = rs.turnAngle
		kRs.skipTurn = rs.skipTurn
		kRs.types = rs.types
		kRs.pointTypes = rs.pointTypes
		kRs.names = rs.names
		return kRs
	}

	@JvmStatic
	fun kRouteType(rt: GPXUtilities.RouteType): RouteType {
		val kRt = RouteType()
		kRt.tag = rt.tag
		kRt.value = rt.value
		return kRt
	}

	@JvmStatic
	fun kMetadata(metadata: GPXUtilities.Metadata): Metadata {
		val kMetadata = Metadata()
		kMetadata.name = metadata.name
		kMetadata.link = metadata.link
		kMetadata.keywords = metadata.keywords
		kMetadata.time = metadata.time
		if (metadata.author != null) {
			kMetadata.author = kAuthor(metadata.author)
		}
		if (metadata.copyright != null) {
			kMetadata.copyright = kCopyright(metadata.copyright)
		}
		if (metadata.bounds != null) {
			kMetadata.bounds = kBounds(metadata.bounds)
		}
		copyExtensions(metadata, kMetadata)
		return kMetadata
	}

	@JvmStatic
	fun kAuthor(author: GPXUtilities.Author): Author {
		val kAuthor = Author()
		kAuthor.name = author.name
		kAuthor.link = author.link
		kAuthor.email = author.email
		copyExtensions(author, kAuthor)
		return kAuthor
	}

	@JvmStatic
	fun kCopyright(copyright: GPXUtilities.Copyright): Copyright {
		val kCopyright = Copyright()
		kCopyright.author = copyright.author
		kCopyright.year = copyright.year
		kCopyright.license = copyright.license
		copyExtensions(copyright, kCopyright)
		return kCopyright
	}

	@JvmStatic
	fun kBounds(bounds: GPXUtilities.Bounds): Bounds {
		val kBounds = Bounds()
		kBounds.minlat = bounds.minlat
		kBounds.minlon = bounds.minlon
		kBounds.maxlat = bounds.maxlat
		kBounds.maxlon = bounds.maxlon
		copyExtensions(bounds, kBounds)
		return kBounds
	}

	@JvmStatic
	fun copyExtensions(source: GPXUtilities.GPXExtensions, destination: GpxExtensions) {
		val extensionsToRead = source.extensionsToRead
		if (extensionsToRead.isNotEmpty()) {
			destination.getExtensionsToWrite().putAll(extensionsToRead)
		}
	}
}