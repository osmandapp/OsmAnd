package net.osmand.plus.track

import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageButton
import java.util.function.BooleanSupplier
import java.util.function.Supplier
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.OsmandMapTileView
import kotlin.math.abs

class TrackMapSync @JvmOverloads constructor(
	private val app: OsmandApplication,
	private val fitTrack: Runnable,
	syncOnStart: Boolean,
	private val hasTrack: BooleanSupplier = BooleanSupplier { true }
) : OsmandMapTileView.TouchListener, OsmandMapTileView.MapZoomChangeListener,
	OsmandMapTileView.ManualMapMoveListener {

	constructor(app: OsmandApplication, fitTrack: Runnable, owner: Any?, selectedGpxFile: Supplier<SelectedGpxFile?>) : this(
		app, fitTrack, owner !is TrackMapSyncOwner || owner.isMapInSync(),
		BooleanSupplier { selectedGpxFile.get()?.gpxFileToDisplay?.isEmpty() == false }
	)

	private val mapView: OsmandMapTileView = app.osmandMap.mapView
	private val doubleTapTimeout: Long = ViewConfiguration.getDoubleTapTimeout().toLong()

	private val linkedListener = Runnable { disable() }

	private val viewportListener = OsmandMapTileView.ViewportListener {
		if (isInSync) {
			app.runInUIThread { fitIfInSync() }
		}
	}

	private var touchSlop = 0
	private var doubleTapSlop = 0

	private var button: ImageButton? = null
	private var nightMode = false
	private var downX = 0f
	private var downY = 0f
	private var lastUpTime = 0L

	var isInSync = syncOnStart
		private set

	fun attach(button: ImageButton, nightMode: Boolean) {
		detach()
		this.button = button
		this.nightMode = nightMode
		val configuration = ViewConfiguration.get(button.context)
		touchSlop = configuration.scaledTouchSlop
		doubleTapSlop = configuration.scaledDoubleTapSlop / 2
		val background = if (nightMode) {
			R.drawable.btn_circle_night
		} else {
			R.drawable.btn_circle
		}
		button.setBackgroundResource(background)
		button.contentDescription = app.getString(R.string.track_map_sync)
		button.setOnClickListener { enable() }
		updateButton()
		mapView.addTouchListener(this)
		mapView.addMapZoomChangeListener(this)
		mapView.addManualMapMoveListener(this)
		mapView.addViewportListener(viewportListener)
		app.mapViewTrackingUtilities.addMapLinkedToLocationListener(linkedListener)
	}

	fun detach() {
		if (button != null) {
			button = null
			mapView.removeTouchListener(this)
			mapView.removeMapZoomChangeListener(this)
			mapView.removeManualMapMoveListener(this)
			mapView.removeViewportListener(viewportListener)
			app.mapViewTrackingUtilities.removeMapLinkedToLocationListener(linkedListener)
		}
	}

	fun detach(owner: Any?) {
		detach()
		if (owner is TrackMapSyncOwner) {
			owner.setMapInSync(isInSync)
		}
	}

	fun setInSync(inSync: Boolean) {
		if (isInSync != inSync) {
			isInSync = inSync
			updateButton()
		}
	}

	private fun enable() {
		if (!hasTrack.asBoolean) {
			return
		}
		app.mapViewTrackingUtilities.setMapLinkedToLocation(false)
		setInSync(true)
		fitTrack.run()
	}

	fun fitIfInSync() {
		if (button?.isShown == true && isInSync && hasTrack.asBoolean) {
			fitTrack.run()
		}
	}

	private fun disable() {
		setInSync(false)
	}

	private fun updateButton() {
		val button = button ?: return
		val iconColorId = if (isInSync) {
			R.color.color_myloc_distance
		} else {
			ColorUtilities.getMapButtonIconColorId(nightMode)
		}
		val icon = app.uiUtilities.getIcon(R.drawable.ic_action_center_on_track, iconColorId)
		button.setImageDrawable(AndroidUtils.getDrawableForDirection(app, icon))
		button.isSelected = isInSync
	}

	override fun onTouchEvent(event: MotionEvent) {
		if (!isInSync) {
			return
		}
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				val thread = mapView.animatedDraggingThread
				if (thread.isFitting || thread.isAnimatingMapMove || thread.isAnimatingMapZoom || isDoubleTap(event)) {
					disable()
				}
				downX = event.x
				downY = event.y
			}

			MotionEvent.ACTION_UP -> {
				lastUpTime = event.eventTime
			}

			MotionEvent.ACTION_POINTER_DOWN -> {
				disable()
			}

			MotionEvent.ACTION_MOVE -> {
				if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) {
					disable()
				}
			}
		}
	}

	private fun isDoubleTap(event: MotionEvent): Boolean {
		val dx = event.x - downX
		val dy = event.y - downY
		return event.eventTime - lastUpTime <= doubleTapTimeout && dx * dx + dy * dy < doubleTapSlop * doubleTapSlop
	}

	override fun onMapZoomChanged(manual: Boolean) {
		if (manual) {
			disable()
		}
	}

	override fun onManualMapMove() {
		disable()
	}
}
