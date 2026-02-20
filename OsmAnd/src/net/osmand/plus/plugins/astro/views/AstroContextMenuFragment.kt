package net.osmand.plus.plugins.astro.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialBottomSheetDialogFragment
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.StarMapFragment
import net.osmand.plus.plugins.astro.StarWatcherPlugin
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import java.util.Calendar
import java.util.Locale

class AstroContextMenuFragment : BaseMaterialBottomSheetDialogFragment() {

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var recyclerView: RecyclerView
	private lateinit var metricsList: RecyclerView
	private lateinit var metricsAdapter: MetricsAdapter
	private lateinit var headerCard: MaterialCardView
	private lateinit var collapsedToolbar: View

	private lateinit var headerTitle: TextView
	private lateinit var headerType: TextView
	private lateinit var headerCloseButton: View
	private lateinit var collapsedTitle: TextView
	private lateinit var collapsedCloseButton: View


	private lateinit var saveButton: View
	private lateinit var saveTitle: TextView
	private lateinit var saveIcon: ImageView

	private lateinit var locationButton: View
	private lateinit var locationTitle: TextView
	private lateinit var locationIcon: ImageView

	private lateinit var directionButton: View
	private lateinit var directionTitle: TextView
	private lateinit var directionIcon: ImageView

	private lateinit var pathButton: View
	private lateinit var pathTitle: TextView
	private lateinit var pathIcon: ImageView

	var galleryController: GalleryController? = null

	private var skyObject: SkyObject? = null
	private val parent: StarMapFragment
		get() = requireParentFragment() as StarMapFragment


	companion object {
		val TAG: String = AstroContextMenuFragment::class.java.simpleName

		fun newInstance(skyObject: SkyObject): AstroContextMenuFragment {
			val fragment = AstroContextMenuFragment()
			val args = Bundle()
			args.putString("skyObjectName", skyObject.name)
			fragment.arguments = args
			return fragment
		}
	}

	override fun getInsetTargets(): InsetTargetsCollection? {
		val collection = super.getInsetTargets()
		collection.replace(
			InsetTarget.createScrollable(R.id.cardsRecyclerView)
		)
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val dialogManager = app.dialogManager
		galleryController =
			dialogManager.findController(GalleryController.PROCESS_ID) as GalleryController?
		if (galleryController == null) {
			dialogManager.register(GalleryController.PROCESS_ID, GalleryController(app))
			galleryController =
				dialogManager.findController(GalleryController.PROCESS_ID) as GalleryController?
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.astro_context_menu_fragment, container, false)

		initializeViews(view)
		setupRecyclerView()

		arguments?.getString("skyObjectName")?.let { name ->
			parent.viewModel.skyObjects.value?.find { it.name == name }?.let {
				updateObjectInfo(it)
			}
		}

		return view
	}

	fun updateObjectInfo(obj: SkyObject) {
		this.skyObject = obj
		if (!isAdded) {
			return
		}

		setTitle(obj.localizedName ?: obj.name)
		val typeName = AstroUtils.getAstroTypeName(app, obj.type.titleKey)
		var parentGroup: String = app.getString(R.string.astro_deep_sky)
		val type = obj.type
		if (type == SkyObject.Type.MOON) {
			parentGroup = app.getString(R.string.astro_type_earth)
		} else if (type.isSunSystem()) {
			parentGroup = app.getString(R.string.astro_solar_system)
		} else if (type == SkyObject.Type.STAR) {
			parentGroup = parent.viewModel.constellations.value
				?.firstOrNull { constellation ->
					constellation.lines.any { (a, b) -> a == obj.hip || b == obj.hip }
				}
				?.let { it.localizedName ?: it.name }.toString()
		}
		if (parentGroup == null || parentGroup.isEmpty() || parentGroup == "null") {
			parentGroup = app.getString(R.string.astro_deep_sky)
		}

		headerType.text =
			app.getString(R.string.ltr_or_rtl_combine_via_bold_point, typeName, parentGroup)

		updateMetrics(obj)
		updateButtons(obj)
	}

	private fun updateButtons(obj: SkyObject) {
		fun bindButtons() {
			saveIcon.setImageDrawable(
				uiUtilities.getIcon(
					if (obj.isFavorite) R.drawable.ic_action_bookmark_filled else R.drawable.ic_action_bookmark,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			saveTitle.text = app.getString(R.string.shared_string_save)

			locationIcon.setImageDrawable(
				uiUtilities.getIcon(
					R.drawable.ic_action_location_16,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			locationTitle.text = app.getString(R.string.astro_locate)

			directionIcon.setImageDrawable(
				uiUtilities.getIcon(
					if (obj.showDirection) R.drawable.ic_direction_arrow else R.drawable.ic_direction_arrow,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			directionTitle.text = app.getString(R.string.astro_direction)

			pathIcon.setImageDrawable(
				uiUtilities.getIcon(
					if (obj.showCelestialPath) R.drawable.ic_action_target_path_on else R.drawable.ic_action_target_path_off,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			pathTitle.text = app.getString(R.string.astro_path)
		}

		saveButton.setOnClickListener {
			obj.isFavorite = !obj.isFavorite
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.isFavorite) swSettings.addFavorite(obj.id) else swSettings.removeFavorite(obj.id)
			parent.starView.invalidate()

			bindButtons()
		}

		locationButton.setOnClickListener {
			parent.starView.setSelectedObject(obj, center = true, animate = true)
			bindButtons()
		}

		directionButton.setOnClickListener {
			obj.showDirection = !obj.showDirection
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.showDirection) swSettings.addDirection(obj.id) else swSettings.removeDirection(
				obj.id
			)
			parent.starView.invalidate()
			bindButtons()
		}

		pathButton.setOnClickListener {
			obj.showCelestialPath = !obj.showCelestialPath
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.showCelestialPath) swSettings.addCelestialPath(obj.id) else swSettings.removeCelestialPath(
				obj.id
			)
			parent.starView.setObjectPinned(obj, obj.showCelestialPath, true)
			parent.starView.invalidate()
			bindButtons()
		}

		bindButtons()
	}

	private fun updateMetrics(obj: SkyObject) {
		val metrics = ArrayList<MetricsAdapter.MetricUi>()

		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		metrics.add(MetricsAdapter.MetricUi(az, getString(R.string.shared_string_azimuth)))

		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		metrics.add(MetricsAdapter.MetricUi(alt, getString(R.string.altitude)))

		metrics.add(
			MetricsAdapter.MetricUi(
				obj.magnitude.toString(),
				getString(R.string.shared_string_magnitude)
			)
		)

		val observer = parent.starView.observer
		val bodyToCheck: Body? = if (!obj.type.isSunSystem()) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0); Body.Star2
		} else obj.body

		if (bodyToCheck != null) {
			val calendar = (parent.viewModel.currentCalendar.value
				?: Calendar.getInstance()).clone() as Calendar
			calendar.set(Calendar.HOUR_OF_DAY, 0)
			calendar.set(Calendar.MINUTE, 0)
			calendar.set(Calendar.SECOND, 0)
			calendar.set(Calendar.MILLISECOND, 0)
			val searchStart = Time.fromMillisecondsSince1970(calendar.timeInMillis)

			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, searchStart, 1.2)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, searchStart, 1.2)

			if (riseTime != null) {
				metrics.add(
					MetricsAdapter.MetricUi(
						AstroUtils.formatLocalTime(riseTime),
						getString(R.string.astro_rise)
					)
				)
			}

			if (setTime != null) {
				metrics.add(
					MetricsAdapter.MetricUi(
						AstroUtils.formatLocalTime(setTime),
						getString(R.string.astro_set)
					)
				)
			}
		}

		metricsAdapter.submit(metrics)
	}

	private fun setTitle(name: String) {
		collapsedTitle.text = name
		headerTitle.text = name
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->

			val total = appBar.totalScrollRange.toFloat()
			if (total == 0f) return@OnOffsetChangedListener

			val progress = kotlin.math.abs(offset) / total
			val clamped = progress.coerceIn(0f, 1f)

			headerCard.alpha = 1f - clamped

			collapsedToolbar.alpha = clamped

			headerCard.isClickable = clamped < 0.9f
			collapsedToolbar.isClickable = clamped > 0.1f
		})
	}

	private fun initializeViews(view: View) {
		appBarLayout = view.findViewById(R.id.appBarLayout)
		recyclerView = view.findViewById(R.id.cardsRecyclerView)
		headerCard = view.findViewById(R.id.headerCard)
		collapsedToolbar = view.findViewById(R.id.collapsedToolbar)
		metricsList = view.findViewById(R.id.metrics_list)

		headerTitle = view.findViewById(R.id.header_title)
		headerType = view.findViewById(R.id.header_subtitle)
		headerCloseButton = view.findViewById(R.id.header_close_button)
		collapsedTitle = view.findViewById(R.id.collapsed_title)
		collapsedCloseButton = view.findViewById(R.id.collapsed_close_button)

		saveButton = view.findViewById(R.id.save_button)
		saveTitle = view.findViewById(R.id.save_title)
		saveIcon = view.findViewById(R.id.save_icon)

		locationButton = view.findViewById(R.id.locate_button)
		locationTitle = view.findViewById(R.id.locate_title)
		locationIcon = view.findViewById(R.id.locate_icon)

		directionButton = view.findViewById(R.id.direction_button)
		directionTitle = view.findViewById(R.id.direction_title)
		directionIcon = view.findViewById(R.id.direction_icon)

		pathButton = view.findViewById(R.id.path_button)
		pathTitle = view.findViewById(R.id.path_title)
		pathIcon = view.findViewById(R.id.path_icon)

		headerCloseButton.setOnClickListener {
			dismiss()
		}
		collapsedCloseButton.setOnClickListener {
			dismiss()
		}
	}

	override fun onStart() {
		super.onStart()

		val dlg = dialog as? BottomSheetDialog ?: return
		val sheet = dlg.findViewById<FrameLayout>(
			com.google.android.material.R.id.design_bottom_sheet
		) ?: return

		sheet.layoutParams = sheet.layoutParams.apply {
			height = ViewGroup.LayoutParams.MATCH_PARENT
		}

		val behavior = BottomSheetBehavior.from(sheet)
		behavior.apply {
			isHideable = true
			skipCollapsed = false
			isFitToContents = false
			expandedOffset = 0
			halfExpandedRatio = 0.4f
			state = BottomSheetBehavior.STATE_HALF_EXPANDED
		}

		sheet.clipToOutline = true
	}

	private fun setupRecyclerView() {
		metricsList.layoutManager =
			LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
		metricsAdapter = MetricsAdapter()
		metricsList.adapter = metricsAdapter
		metricsList.isNestedScrollingEnabled = false
	}
}
