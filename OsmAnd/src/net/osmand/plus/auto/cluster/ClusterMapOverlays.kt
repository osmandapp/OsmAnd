package net.osmand.plus.auto.cluster

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import net.osmand.Location
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.QVectorPointI
import net.osmand.core.jni.SWIGTYPE_p_void
import net.osmand.core.jni.SwigUtilities
import net.osmand.core.jni.TimingFunction
import net.osmand.core.jni.Utilities
import net.osmand.core.jni.VectorLine
import net.osmand.core.jni.VectorLineBuilder
import net.osmand.core.jni.VectorLinesCollection
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.profiles.LocationIcon
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.NativeUtilities

/**
 * My location and the route ahead, drawn by the cluster's own renderer.
 * The map layers of the app are bound to the renderer of the main screen and can't draw here.
 */
class ClusterMapOverlays(
	private val app: OsmandApplication,
	private val view: MapRendererView,
	private val density: Float
) {

	private val markers = MapMarkersCollection()
	private val lines = VectorLinesCollection()
	private var myLocationMarker: MapMarker? = null
	private var myLocationIconKey: SWIGTYPE_p_void? = null
	private var routeLine: VectorLine? = null
	private var routeStartIndex = -1
	private var nightMode = false
	private var lastLocation: Location? = null
	private var lastLocation31: PointI? = null

	init {
		view.addSymbolsProvider(lines)
		view.addSymbolsProvider(markers)
	}

	fun setNightMode(nightMode: Boolean) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode
			recreateMyLocationMarker()
		}
	}

	/**
	 * Moves my location to [location31], gliding there over [animationSeconds] or jumping when it is 0.
	 */
	fun updateMyLocation(location: Location, location31: PointI, animationSeconds: Float) {
		lastLocation = location
		lastLocation31 = location31
		val marker = myLocationMarker ?: recreateMyLocationMarker() ?: return
		val animator = view.mapMarkersAnimator
		animator.cancelAnimations(marker)
		if (animationSeconds > 0) {
			animator.animatePositionTo(marker, location31, animationSeconds, TimingFunction.Linear)
			if (location.hasBearing()) {
				val direction = Utilities.normalizedAngleDegrees(location.bearing.toDouble()).toFloat()
				animator.animateDirectionTo(marker, myLocationIconKey, direction, animationSeconds, TimingFunction.Linear)
			}
			view.resumeMapMarkersAnimation()
		} else {
			marker.setPosition(location31)
			if (location.hasBearing()) {
				marker.setOnMapSurfaceIconDirection(myLocationIconKey, location.bearing)
			}
		}
		marker.setIsHidden(false)
		updateRouteStart()
	}

	/**
	 * Redraws my location with the icon and color of the current profile.
	 */
	fun updateProfile() {
		recreateMyLocationMarker()
		val location = lastLocation
		val location31 = lastLocation31
		if (location != null && location31 != null) {
			updateMyLocation(location, location31, 0f)
		}
	}

	fun updateRoute() {
		routeStartIndex = -1
		updateRouteStart()
	}

	fun release() {
		view.removeSymbolsProvider(markers)
		view.removeSymbolsProvider(lines)
		markers.removeAllMarkers()
		lines.removeAllLines()
		myLocationMarker = null
		routeLine = null
	}

	/**
	 * Shows the route from the current position on, so the passed part disappears behind the car.
	 */
	private fun updateRouteStart() {
		val routingHelper = app.routingHelper
		val route = routingHelper.route
		val locations = route.immutableAllLocations
		val startIndex = route.currentRoute
		if (!routingHelper.isRouteCalculated || startIndex < 0 || locations.size - startIndex < 2) {
			routeLine?.setIsHidden(true)
			routeStartIndex = -1
			return
		}
		if (startIndex == routeStartIndex) {
			return
		}
		routeStartIndex = startIndex
		val points = QVectorPointI()
		for (i in startIndex until locations.size) {
			val location = locations[i]
			points.add(NativeUtilities.getPoint31FromLatLon(location.latitude, location.longitude))
		}
		val line = routeLine ?: createRouteLine(points)
		line.setPoints(points)
		line.setIsHidden(false)
	}

	private fun createRouteLine(points: QVectorPointI): VectorLine {
		val color = ContextCompat.getColor(app, R.color.nav_track)
		val line = VectorLineBuilder()
			.setBaseOrder(app.osmandMap.mapLayers.routeLayer.baseOrder)
			.setLineId(ROUTE_LINE_ID)
			.setLineWidth((ROUTE_LINE_WIDTH_DP * density).toDouble())
			.setFillColor(NativeUtilities.createFColorARGB(color))
			.setPoints(points)
			.buildAndAddToCollection(lines)
		routeLine = line
		return line
	}

	private fun recreateMyLocationMarker(): MapMarker? {
		myLocationMarker?.let { markers.removeMarker(it) }
		myLocationMarker = null
		val icon = createMyLocationIcon() ?: return null
		val iconKey = SwigUtilities.getOnSurfaceIconKey(1)
		val marker = MapMarkerBuilder()
			.setMarkerId(MY_LOCATION_MARKER_ID)
			.setBaseOrder(app.osmandMap.mapLayers.locationLayer.pointsOrder)
			.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
			.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
			.setIsHidden(true)
			.addOnMapSurfaceIcon(iconKey, NativeUtilities.createSkImageFromBitmap(icon))
			.buildAndAddToCollection(markers)
		myLocationIconKey = iconKey
		myLocationMarker = marker
		return marker
	}

	private fun createMyLocationIcon(): Bitmap? {
		val appMode = app.settings.applicationMode
		val iconName = appMode.navigationIcon
		val locationIcon = if (LocationIcon.isModelRepresented(iconName)) {
			LocationIcon.MOVEMENT_DEFAULT
		} else {
			LocationIcon.fromName(iconName, false)
		}
		val drawable = AppCompatResources.getDrawable(app, locationIcon.iconId) as? LayerDrawable ?: return null
		DrawableCompat.setTint(drawable.getDrawable(1), appMode.getProfileColor(nightMode))
		val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
		AndroidUtils.drawScaledLayerDrawable(Canvas(bitmap), drawable, bitmap.width / 2, bitmap.height / 2, 1f)
		return bitmap
	}

	companion object {
		private const val MY_LOCATION_MARKER_ID = 1
		private const val ROUTE_LINE_ID = 1
		private const val ROUTE_LINE_WIDTH_DP = 8f
	}
}