package net.osmand.plus.plugins.astro

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.plugins.astro.views.DateTimeSelectionView
import net.osmand.plus.plugins.astro.views.SkyObject
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StarMapFragment : BaseFullScreenDialogFragment() {

	private lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var bottomSheet: View
	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var sheetDetails: TextView
	private lateinit var resetTimeButton: Button

	private lateinit var viewModel: StarMapViewModel
	private var selectedObject: SkyObject? = null

	companion object {
		val TAG: String = StarMapFragment::class.java.simpleName

		fun showInstance(mapActivity: MapActivity) {
			val manager: FragmentManager = mapActivity.supportFragmentManager
			manager.findFragmentByTag(TAG)?.let { foundFragment ->
				(foundFragment as StarMapFragment).dialog?.dismiss()
			}
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = StarMapFragment()
				fragment.show(manager, TAG)
			}
		}
	}

	override fun getThemeId(): Int = if (nightMode) R.style.OsmandDarkTheme else R.style.OsmandLightTheme_LightStatusBar

	override fun getStatusBarColorId(): Int = ColorUtilities.getStatusBarSecondaryColorId(nightMode)

	override fun createDialog(savedInstanceState: Bundle?): Dialog {
		return object : Dialog(requireContext(), themeId) {
			override fun onBackPressed() {
				dismiss()
			}
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = themedInflater.inflate(R.layout.fragment_star_map, container, false)

		viewModel = ViewModelProvider(this)[StarMapViewModel::class.java]

		starView = view.findViewById(R.id.star_view)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		resetTimeButton = view.findViewById(R.id.reset_time_button)
		bottomSheet = view.findViewById(R.id.bottom_sheet)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetCoords = view.findViewById(R.id.sheet_coords)
		sheetDetails = view.findViewById(R.id.sheet_details)

		val backButton: ImageView = view.findViewById(R.id.back_button)
		backButton.setOnClickListener { dismiss() }

		val settingsButton: ImageView = view.findViewById(R.id.settings_button)
		settingsButton.setOnClickListener { showFilterDialog() }

		resetTimeButton.setOnClickListener {
			viewModel.resetTime()
			resetTimeButton.visibility = View.GONE
		}

		// Set initial location
		val loc = app.osmandMap.mapView.currentRotatedTileBox.centerLatLon
		starView.setObserverLocation(loc.latitude, loc.longitude, 0.0)

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setupObservers()
		setupListeners()
	}

	private fun setupObservers() {
		// Observe Time changes
		viewModel.currentTime.observe(viewLifecycleOwner) { time ->
			// Pass true for animate if it's a user interaction
			starView.setDateTime(time, animate = true)

			// Update Bottom Sheet if something is selected
			if (selectedObject != null) {
				showObjectInfo(selectedObject!!)
			}
		}

		// Observe Calendar changes to update UI controls
		viewModel.currentCalendar.observe(viewLifecycleOwner) { calendar ->
			timeSelectionView.setDateTime(calendar)
		}

		// Observe Sky Objects
		viewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starView.setSkyObjects(objects)
		}
	}

	private fun setupListeners() {
		timeSelectionView.setOnDateTimeChangeListener { calendar ->
			viewModel.updateTime(calendar)
			resetTimeButton.visibility = View.VISIBLE
		}

		starView.setOnObjectClickListener { obj ->
			selectedObject = obj
			if (obj != null) {
				showObjectInfo(obj)
			} else {
				bottomSheet.visibility = View.GONE
			}
		}

		starView.onAnimationFinished = {
			if (selectedObject != null) {
				showObjectInfo(selectedObject!!)
			}
		}
	}

	private fun showObjectInfo(obj: SkyObject) {
		sheetTitle.text = obj.name
		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		sheetCoords.text = "Azimuth: $az  |  Altitude: $alt"

		var details = "Magnitude: ${obj.magnitude}"
		if (obj.type != SkyObject.Type.STAR) {
			details += "\nDistance: %.3f AU".format(obj.distAu)
		}

		// Rise / Set calculation (Performed in Fragment or could be moved to VM)
		val observer = starView.observer
		val currentTime = starView.currentTime

		val bodyToCheck: Body? = if (obj.type == SkyObject.Type.STAR) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0)
			Body.Star2
		} else {
			obj.body
		}

		if (bodyToCheck != null) {
			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, currentTime, 1.0)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, currentTime, 1.0)

			if (riseTime != null) {
				details += "\nRise: ↑${formatLocalTime(riseTime)}"
			}
			if (setTime != null) {
				details += "\nSet: ↓${formatLocalTime(setTime)}"
			}
		}

		sheetDetails.text = details
		bottomSheet.visibility = View.VISIBLE
	}

	private fun formatLocalTime(astronomyTime: Time): String {
		val calendar = Calendar.getInstance(TimeZone.getDefault())
		calendar.timeInMillis = astronomyTime.toMillisecondsSince1970()
		return String.format(Locale.getDefault(), "%02d:%02d",
			calendar.get(Calendar.HOUR_OF_DAY),
			calendar.get(Calendar.MINUTE))
	}

	private fun showFilterDialog() {
		// This logic relies on the View's internal state, which is acceptable for view-specific toggles
		// ideally, this state would also be in the ViewModel
		val toggleItems = arrayOf("Azimuthal Grid", "Equatorial Grid", "Ecliptic Line")
		val toggleChecked = booleanArrayOf(starView.showAzimuthalGrid, starView.showEquatorialGrid, starView.showEclipticLine)

		val currentObjects = viewModel.skyObjects.value ?: emptyList()
		val objectNames = currentObjects.map { it.name }.toTypedArray()
		val objectChecked = currentObjects.map { it.isVisible }.toBooleanArray()

		val allItems = toggleItems + objectNames
		val allChecked = toggleChecked + objectChecked

		AlertDialog.Builder(requireContext())
			.setTitle("Visible Layers & Objects")
			.setMultiChoiceItems(allItems, allChecked) { _, which, isChecked ->
				if (which < toggleItems.size) {
					when (which) {
						0 -> starView.showAzimuthalGrid = isChecked
						1 -> starView.showEquatorialGrid = isChecked
						2 -> starView.showEclipticLine = isChecked
					}
				} else {
					val objIndex = which - toggleItems.size
					if (objIndex in currentObjects.indices) {
						currentObjects[objIndex].isVisible = isChecked
					}
				}
			}
			.setPositiveButton("Apply") { _, _ ->
				starView.updateVisibility()
			}
			.setNegativeButton("Cancel", null)
			.setNeutralButton("All On") { _, _ ->
				starView.showAzimuthalGrid = true
				starView.showEquatorialGrid = true
				starView.showEclipticLine = true
				currentObjects.forEach { it.isVisible = true }
				starView.updateVisibility()
			}
			.show()
	}
}