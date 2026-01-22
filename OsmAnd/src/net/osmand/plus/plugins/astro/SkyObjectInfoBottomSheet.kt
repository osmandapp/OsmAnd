package net.osmand.plus.plugins.astro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.checkbox.MaterialCheckBox
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.utils.AndroidUtils
import java.util.Calendar
import java.util.Locale

class SkyObjectInfoFragment : Fragment() {

	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var sheetPinButton: MaterialCheckBox
	private lateinit var sheetMagnitude: TextView
	private lateinit var sheetDistance: TextView
	private lateinit var sheetRiseTime: TextView
	private lateinit var sheetSetTime: TextView
	private lateinit var sheetWikiButton: View

	private var skyObject: SkyObject? = null

	private val parent: StarMapFragment
		get() = requireParentFragment() as StarMapFragment

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.bottom_sheet_sky_object, container, false)

		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetCoords = view.findViewById(R.id.sheet_coords)
		sheetPinButton = view.findViewById(R.id.sheet_pin_button)
		sheetMagnitude = view.findViewById(R.id.sheet_magnitude)
		sheetDistance = view.findViewById(R.id.sheet_distance)
		sheetRiseTime = view.findViewById(R.id.sheet_rise_time)
		sheetSetTime = view.findViewById(R.id.sheet_set_time)
		sheetWikiButton = view.findViewById(R.id.sheet_wiki_button)

		view.findViewById<View>(R.id.close_button).setOnClickListener {
			parent.hideBottomSheet()
		}

		ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			val basePadding = AndroidUtils.dpToPx(v.context, 16f)
			v.updatePadding(bottom = insets.bottom + basePadding)
			windowInsets
		}

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		arguments?.getString("skyObjectName")?.let { name ->
			parent.viewModel.skyObjects.value?.find { it.name == name }?.let {
				updateObjectInfo(it)
			}
		}
	}

	fun updateObjectInfo(obj: SkyObject) {
		this.skyObject = obj
		if (!isAdded) {
			return
		}

		sheetTitle.text = obj.localizedName ?: obj.name
		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		val coordsText = "${getString(R.string.shared_string_azimuth)}: $az  •  ${getString(R.string.altitude)}: $alt"
		sheetCoords.text = coordsText

		sheetPinButton.setOnCheckedChangeListener(null) // Prevent recursive trigger
		sheetPinButton.isChecked = parent.starView.isObjectPinned(obj)
		sheetPinButton.setOnCheckedChangeListener { _, isChecked ->
			parent.starView.setObjectPinned(obj, isChecked)
		}

		sheetMagnitude.text = "${getString(R.string.shared_string_magnitude)}: ${obj.magnitude}"
		sheetMagnitude.isVisible = true

		if (obj.type.isSunSystem()) {
			sheetDistance.isVisible = true
			sheetDistance.text =
				"${getString(R.string.distance)}: %.3f AU".format(Locale.getDefault(), obj.distAu)
		} else {
			sheetDistance.isVisible = false
		}

		val observer = parent.starView.observer
		val bodyToCheck: Body? = if (!obj.type.isSunSystem()) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0); Body.Star2
		} else obj.body

		if (bodyToCheck != null) {
			val calendar = (parent.viewModel.currentCalendar.value ?: Calendar.getInstance()).clone() as Calendar
			calendar.set(Calendar.HOUR_OF_DAY, 0)
			calendar.set(Calendar.MINUTE, 0)
			calendar.set(Calendar.SECOND, 0)
			calendar.set(Calendar.MILLISECOND, 0)
			val searchStart = Time.fromMillisecondsSince1970(calendar.timeInMillis)

			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, searchStart, 1.2)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, searchStart, 1.2)

			if (riseTime != null) {
				sheetRiseTime.text = "Rise: ↑${AstroUtils.formatLocalTime(riseTime)}"
				sheetRiseTime.isVisible = true
			} else {
				sheetRiseTime.isVisible = false
			}

			if (setTime != null) {
				sheetSetTime.text = "Set: ↓${AstroUtils.formatLocalTime(setTime)}"
				sheetSetTime.isVisible = true
			} else {
				sheetSetTime.isVisible = false
			}
		} else {
			sheetRiseTime.isVisible = false
			sheetSetTime.isVisible = false
		}

		if (obj.wid.isNotEmpty()) {
			sheetWikiButton.isVisible = true
			sheetWikiButton.setOnClickListener {
				val uri = Uri.parse("https://www.wikidata.org/wiki/${obj.wid}")
				val intent = Intent(Intent.ACTION_VIEW, uri)
				try {
					startActivity(intent)
				} catch (_: Exception) {
				}
			}
		} else {
			sheetWikiButton.isVisible = false
		}
	}

	companion object {
		const val TAG = "SkyObjectInfoFragment"
		fun newInstance(skyObject: SkyObject): SkyObjectInfoFragment {
			val fragment = SkyObjectInfoFragment()
			val args = Bundle()
			args.putString("skyObjectName", skyObject.name)
			fragment.arguments = args
			return fragment
		}
	}
}