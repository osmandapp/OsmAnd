package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.AstroUtils
import net.osmand.plus.plugins.astro.StarObjectsViewModel
import net.osmand.plus.plugins.astro.StarWatcherPlugin
import net.osmand.plus.plugins.astro.StarWatcherSettings
import net.osmand.plus.utils.AndroidUtils
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Collections
import kotlin.math.abs

private const val CHECKBOX_ID = 101
private const val TEXT_VIEW_ID = 102

abstract class StarChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

	protected var skyObjects: List<SkyObject> = emptyList()

	// Coroutine scope for background calculations
	protected val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var calculationJob: Job? = null
	private var isComputing = false
	private var pendingRebuild = false

	protected var config = Config()

	// ---------- Common Paints (Lazy initialization) ----------
	protected val bgPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#E4424242".toColorInt() } }
	protected val nightPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#60000000".toColorInt() } }
	protected val dayPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#57C7F3".toColorInt() } }
	protected val twiAstro by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC2B4C7E".toColorInt() } }
	protected val twiNaut by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC3C7AA6".toColorInt() } }
	protected val twiCivil by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC5BBBF0".toColorInt() } }

	protected enum class Band { DAY, CIVIL, NAUT, ASTRO, NIGHT }

	companion object {
		fun showFilterDialog(context: Context, viewModel: StarObjectsViewModel, onSettingsChanged: () -> Unit) {
			val skyObjects = viewModel.skyObjects.value ?: return
			val dialogObjects = ArrayList(skyObjects.take(30))

			val recyclerView = RecyclerView(context)
			recyclerView.layoutManager = LinearLayoutManager(context)

			val adapter = FilterAdapter(dialogObjects)
			recyclerView.adapter = adapter

			// Setup Drag and Drop
			val callback = object : ItemTouchHelper.SimpleCallback(
				ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
			) {
				override fun onMove(
					recyclerView: RecyclerView,
					viewHolder: RecyclerView.ViewHolder,
					target: RecyclerView.ViewHolder
				): Boolean {
					val fromPos = viewHolder.adapterPosition
					val toPos = target.adapterPosition
					Collections.swap(dialogObjects, fromPos, toPos)
					adapter.notifyItemMoved(fromPos, toPos)
					return true
				}

				override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
					// No swipe actions
				}
			}
			val itemTouchHelper = ItemTouchHelper(callback)
			itemTouchHelper.attachToRecyclerView(recyclerView)

			AlertDialog.Builder(context)
				.setTitle(R.string.visible_layers_and_objects)
				.setView(recyclerView)
				.setPositiveButton(R.string.shared_string_apply) { _, _ ->
					val itemsConfig = dialogObjects.map {
						StarWatcherSettings.SkyObjectConfig(it.id, it.isVisible)
					}

					val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
					val currentConfig = swSettings.getStarChartConfig()
					val newConfig = currentConfig.copy(items = itemsConfig)
					swSettings.setStarChartConfig(newConfig)

					viewModel.loadData()
					onSettingsChanged()
				}
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show()
		}
	}

	fun setChartObjects(objects: List<SkyObject>) {
		this.skyObjects = objects
		triggerAsyncRebuild()
	}

	// Internal Adapter for the Filter/Sort Dialog
	private class FilterAdapter(private val items: List<SkyObject>) :
		RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val checkBox: CheckBox = view.findViewById(CHECKBOX_ID)
			val textView: TextView = view.findViewById(TEXT_VIEW_ID)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			// Programmatically creating the item view to avoid XML dependency
			val context = parent.context
			val layout = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
				gravity = Gravity.CENTER_VERTICAL

				// Add padding
				val p = AndroidUtils.dpToPx(context, 16f)
				setPadding(p, p / 2, p, p / 2)

				// Add ripple effect for feedback
				val attrs = intArrayOf(android.R.attr.selectableItemBackground)
				context.withStyledAttributes(null, attrs) { background = getDrawable(0) }
			}

			val checkBox = CheckBox(context).apply { id = CHECKBOX_ID }
			layout.addView(checkBox)

			val textView = TextView(context).apply {
				id = TEXT_VIEW_ID
				textSize = 16f
				setTextColor(Color.BLACK) // Or use theme color
				val params = LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
				)
				params.marginStart = AndroidUtils.dpToPx(context, 16f)
				layoutParams = params
			}
			layout.addView(textView)

			return ViewHolder(layout)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]
			holder.textView.text = item.name
			// Remove listener before setting state to avoid loop
			holder.checkBox.setOnCheckedChangeListener(null)
			holder.checkBox.isChecked = item.isVisible

			holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
				item.isVisible = isChecked
			}

			// Clicking the text should also toggle checkbox
			holder.itemView.setOnClickListener {
				holder.checkBox.toggle()
			}
		}

		override fun getItemCount() = items.size
	}

	/**
	 * Implementation must compute the model on a background thread.
	 */
	protected abstract suspend fun computeModel(config: Config, width: Int, height: Int): Any?

	/**
	 * Called on Main Thread when the model is ready.
	 */
	protected abstract fun onModelReady(model: Any?)

	fun updateData(latitude: Double, longitude: Double, date: LocalDate = LocalDate.now()) {
		val newConfig = config.copy(
			latitude = latitude,
			longitude = longitude,
			date = date,
			zoneId = ZoneId.systemDefault()
		)
		if (!config.equalsTo(newConfig)) {
			config = newConfig
			triggerAsyncRebuild()
		} else {
			invalidate()
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (w > 0 && h > 0) triggerAsyncRebuild()
	}

	protected fun triggerAsyncRebuild() {
		if (width == 0 || height == 0) return
		pendingRebuild = true
		if (isComputing) return

		calculationJob = viewScope.launch {
			isComputing = true
			while (pendingRebuild && isActive) {
				pendingRebuild = false
				val w = width
				val h = height
				val currentConfig = config
				val result = withContext(Dispatchers.Default) {
					computeModel(currentConfig, w, h)
				}
				if (isActive) {
					onModelReady(result)
					invalidate()
				}
			}
			isComputing = false
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		viewScope.cancel()
	}

	// ---------- Shared Rendering Logic ----------

	/**
	 * Draws the Day/Night/Twilight bands on the background of the chart.
	 * Used by StarVisibility and StarAltitude charts.
	 */
	protected fun drawDayNightBands(
		canvas: Canvas,
		twilight: AstroUtils.Twilight,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		left: Float, top: Float, right: Float, bottom: Float,
		showTwilightBands: Boolean = true
	) {
		// Draw deep night background first (covers everything)
		canvas.drawRect(left, top, right, bottom, nightPaint)

		// 1. Build list of all transitions in the window
		val transitions = ArrayList<Pair<ZonedDateTime, Band>>()

		fun add(t: ZonedDateTime?, nextBand: Band) {
			if (t != null && !t.isBefore(startLocal) && !t.isAfter(endLocal)) {
				transitions.add(t to nextBand)
			}
		}

		// Evening transitions (Sun going down)
		add(twilight.sunset,       Band.CIVIL)
		add(twilight.civilDusk,    Band.NAUT)
		add(twilight.nauticalDusk, Band.ASTRO)
		add(twilight.astroDusk,    Band.NIGHT)

		// Morning transitions (Sun coming up)
		add(twilight.astroDawn,    Band.ASTRO)
		add(twilight.nauticalDawn, Band.NAUT)
		add(twilight.civilDawn,    Band.CIVIL)
		add(twilight.sunrise,      Band.DAY)

		transitions.sortBy { it.first }

		// 2. Determine Initial State (at chart start)
		val startBand: Band
		if (transitions.isNotEmpty()) {
			val firstEvent = transitions[0]
			startBand = when(firstEvent.second) {
				Band.CIVIL -> if (isMorningEvent(firstEvent.first, twilight)) Band.NAUT else Band.DAY
				Band.NAUT  -> if (isMorningEvent(firstEvent.first, twilight)) Band.ASTRO else Band.CIVIL
				Band.ASTRO -> if (isMorningEvent(firstEvent.first, twilight)) Band.NIGHT else Band.NAUT
				Band.NIGHT -> Band.ASTRO
				Band.DAY   -> Band.CIVIL
			}
		} else {
			// Fallback to altitude check for Polar Day/Night
			val obs = Observer(config.latitude, config.longitude, config.elevation)
			val startAlt = AstroUtils.altitude(Body.Sun, startLocal, obs)
			startBand = when {
				startAlt > -0.833 -> Band.DAY
				startAlt > -6.0 -> Band.CIVIL
				startAlt > -12.0 -> Band.NAUT
				startAlt > -18.0 -> Band.ASTRO
				else -> Band.NIGHT
			}
		}

		var currentBand = startBand
		var currentX = left

		// Helper to draw a specific segment
		fun drawSegment(band: Band, x1: Float, x2: Float) {
			if (x2 <= x1) return
			val paint = when(band) {
				Band.DAY   -> dayPaint
				Band.CIVIL -> if (showTwilightBands) twiCivil else null
				Band.NAUT  -> if (showTwilightBands) twiNaut else null
				Band.ASTRO -> if (showTwilightBands) twiAstro else null
				Band.NIGHT -> null
			}
			paint?.let { canvas.drawRect(x1, top, x2, bottom, it) }
		}

		for (trans in transitions) {
			val nextX = timeToX(trans.first, startLocal, endLocal, left, right)
			drawSegment(currentBand, currentX, nextX)
			currentBand = trans.second
			currentX = nextX
		}
		// Draw remaining band to the right edge
		drawSegment(currentBand, currentX, right)
	}

	private fun isMorningEvent(t: ZonedDateTime, tw: AstroUtils.Twilight): Boolean {
		return t == tw.sunrise || t == tw.civilDawn || t == tw.nauticalDawn || t == tw.astroDawn
	}

	/**
	 * Converts a time to an X coordinate on the chart.
	 */
	protected fun timeToX(
		t: ZonedDateTime,
		start: ZonedDateTime, end: ZonedDateTime,
		left: Float, right: Float,
		coerce: Boolean = true
	): Float {
		val total = Duration.between(start, end).toMillis().toDouble()
		val pos = Duration.between(start, t).toMillis().toDouble()
		return if (coerce) {
			(left + (right - left) * (pos.coerceIn(0.0, total) / total)).toFloat()
		} else {
			(left + (right - left) * (pos / total)).toFloat()
		}
	}

	// ---------- Inner Classes ----------

	open class BaseModel(
		open val startLocal: ZonedDateTime,
		open val endLocal: ZonedDateTime
	)

	data class Config(
		val date: LocalDate = LocalDate.now(),
		val zoneId: ZoneId = ZoneId.systemDefault(),
		val latitude: Double = 0.0,
		val longitude: Double = 0.0,
		val elevation: Double = 0.0
	) {
		companion object {
			const val LATLON_EPS = 0.001
			const val ELEVATION_EPS = 10.0
		}
		fun equalsTo(other: Config): Boolean {
			if (zoneId != other.zoneId) return false
			if (abs(latitude - other.latitude) > LATLON_EPS) return false
			if (abs(longitude - other.longitude) > LATLON_EPS) return false
			if (abs(elevation - other.elevation) > ELEVATION_EPS) return false
			return date == other.date
		}
	}

	protected fun dp(v: Float) = AndroidUtils.dpToPxF(context, v)
	protected fun sp(v: Float) = AndroidUtils.spToPxF(context, v)
}