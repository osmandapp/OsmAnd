package net.osmand.plus.plugins.astro

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.defineStar
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.plugins.astro.views.DateTimeSelectionView
import net.osmand.plus.plugins.astro.views.SkyObject
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import java.util.Calendar
import java.util.TimeZone
import androidx.core.graphics.toColorInt
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.OsmandMapTileView
import java.util.Locale

class StarMapFragment : BaseFullScreenDialogFragment() {

	private lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var bottomSheet: View
	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var sheetDetails: TextView
	private lateinit var resetTimeButton: Button

	private val skyObjects = mutableListOf<SkyObject>()
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
			val now = Calendar.getInstance(TimeZone.getDefault()) // Use Local Time
			timeSelectionView.setDateTime(now)
			updateTime(now, animate = true)
			resetTimeButton.visibility = View.GONE
		}

		val loc = app.osmandMap.mapView.currentRotatedTileBox.centerLatLon
		starView.setObserverLocation(loc.latitude, loc.longitude, 0.0)

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		initData()

		timeSelectionView.setOnDateTimeChangeListener { calendar ->
			updateTime(calendar, animate = true)
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
	}

	private fun initData() {
		skyObjects.clear()

		// Planets
		val planets = listOf(
			Triple(Body.Sun, "Sun", Color.YELLOW),
			Triple(Body.Moon, "Moon", Color.LTGRAY),
			Triple(Body.Mercury, "Mercury", Color.GRAY),
			Triple(Body.Venus, "Venus", "#FFD700".toColorInt()),
			Triple(Body.Mars, "Mars", Color.RED),
			Triple(Body.Jupiter, "Jupiter", "#D2B48C".toColorInt()),
			Triple(Body.Saturn, "Saturn", "#F4A460".toColorInt()),
			Triple(Body.Uranus, "Uranus", Color.CYAN),
			Triple(Body.Neptune, "Neptune", Color.BLUE)
		)

		planets.forEach { (body, name, color) ->
			skyObjects.add(SkyObject(
				type = if(body == Body.Sun) SkyObject.Type.SUN else SkyObject.Type.PLANET,
				body = body,
				name = name,
				ra = 0.0, dec = 0.0,
				magnitude = -2f, // Placeholder, calculated dynamically later if needed or visual override
				color = color
			))
		}

		// Top 20 Brightest Stars (Approx J2000 RA/Dec)
		val brightStars = listOf(
			Pair("Sirius", Pair(6.75, -16.72)),
			Pair("Canopus", Pair(6.40, -52.70)),
			Pair("Alpha Centauri", Pair(14.66, -60.83)),
			Pair("Arcturus", Pair(14.26, 19.18)),
			Pair("Vega", Pair(18.62, 38.78)),
			Pair("Capella", Pair(5.28, 46.00)),
			Pair("Rigel", Pair(5.24, -8.20)),
			Pair("Procyon", Pair(7.65, 5.21)),
			Pair("Achernar", Pair(1.63, -57.23)),
			Pair("Betelgeuse", Pair(5.92, 7.41)),
			Pair("Hadar", Pair(14.06, -60.37)),
			Pair("Altair", Pair(19.85, 8.87)),
			Pair("Acrux", Pair(12.44, -63.10)),
			Pair("Aldebaran", Pair(4.60, 16.51)),
			Pair("Antares", Pair(16.49, -26.43)),
			Pair("Spica", Pair(13.42, -11.16)),
			Pair("Pollux", Pair(7.76, 28.03)),
			Pair("Fomalhaut", Pair(22.96, -29.62)),
			Pair("Deneb", Pair(20.69, 45.28)),
			Pair("Mimosa", Pair(12.80, -59.69))
		)

		brightStars.forEach { (name, coords) ->
			skyObjects.add(SkyObject(
				type = SkyObject.Type.STAR,
				body = null,
				name = name,
				ra = coords.first,
				dec = coords.second,
				magnitude = 1.0f, // Simplified magnitude for display logic
				color = Color.WHITE
			))
		}

		// Plus Polaris because it's useful
		skyObjects.add(SkyObject(SkyObject.Type.STAR, null, "Polaris", 2.53, 89.26, 2.0f, Color.YELLOW))

		starView.setSkyObjects(skyObjects)

		// Initial Time Sync (Local)
		val now = Calendar.getInstance(TimeZone.getDefault())
		timeSelectionView.setDateTime(now)
		updateTime(now, animate = false)
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

		// --- Rise / Set Calculation ---
		val observer = starView.observer
		val currentTime = starView.currentTime

		// Determine which astronomy Body to use.
		// For manually added Stars, we use a custom star slot (Star1) as a temporary calculation helper.
		val bodyToCheck: Body? = if (obj.type == SkyObject.Type.STAR) {
			// Define the star properties in astronomy engine so we can run searchRiseSet on it
			defineStar(Body.Star1, obj.ra, obj.dec, 1000.0) // Distance doesn't impact rise/set significantly
			Body.Star1
		} else {
			obj.body
		}

		if (bodyToCheck != null) {
			// Search for next rise and next set events starting from current simulation time
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

	private fun updateTime(calendar: Calendar, animate: Boolean) {
		// Convert Local Calendar to Astronomy Time (UTC)
		// Astronomy engine expects UTC year, month, day...
		// We must convert the local calendar timestamp to UTC components manually or use offsets.

		// Easiest way: Get epoch millis, create UTC calendar, extract fields.
		val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
		utcCal.timeInMillis = calendar.timeInMillis

		val t = Time(
			utcCal.get(Calendar.YEAR),
			utcCal.get(Calendar.MONTH) + 1,
			utcCal.get(Calendar.DAY_OF_MONTH),
			utcCal.get(Calendar.HOUR_OF_DAY),
			utcCal.get(Calendar.MINUTE),
			0.0
		)
		starView.setDateTime(t, animate)

		if (selectedObject != null) {
			showObjectInfo(selectedObject!!)
		}
	}

	private fun showFilterDialog() {
		val names = skyObjects.map { it.name }.toTypedArray()
		val checkedItems = skyObjects.map { it.isVisible }.toBooleanArray()

		AlertDialog.Builder(requireContext())
			.setTitle("Visible Objects")
			.setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
				skyObjects[which].isVisible = isChecked
			}
			.setPositiveButton("Apply") { _, _ ->
				starView.updateVisibility()
			}
			.setNegativeButton("Cancel", null)
			.setNeutralButton("All On") { _, _ ->
				skyObjects.forEach { it.isVisible = true }
				starView.updateVisibility()
			}
			.show()
	}
}