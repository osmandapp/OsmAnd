package net.osmand.telegram.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.aidl.gpx.AGpxBitmap
import net.osmand.telegram.R
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.SavingTracksDbHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.GPXUtilities
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.util.Algorithms
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UserGpxInfoFragment : BaseDialogFragment() {

	private val log = PlatformUtil.getLog(UserGpxInfoFragment::class.java)

	private val uiUtils get() = app.uiUtils

	private lateinit var gpxFile: GPXUtilities.GPXFile

	private lateinit var dateStartBtn: TextView
	private lateinit var timeStartBtn: TextView
	private lateinit var dateEndBtn: TextView
	private lateinit var timeEndBtn: TextView

	private lateinit var avgElevationTv: TextView
	private lateinit var avgSpeedTv: TextView
	private lateinit var totalDistanceTv: TextView
	private lateinit var timeSpanTv: TextView

	private var startCalendar = Calendar.getInstance()
	private var endCalendar = Calendar.getInstance()

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val mainView = inflater.inflate(R.layout.fragment_user_gpx_info, parent)
		AndroidUtils.addStatusBarPadding19v(context!!, mainView)

		readFromBundle(savedInstanceState ?: arguments)

		val userId = gpxFile.userId
		val chatId = gpxFile.chatId

		val user = app.telegramHelper.getUser(userId)
		if (user != null) {
			mainView.findViewById<TextView>(R.id.title).text = TelegramUiHelper.getUserName(user)
			TelegramUiHelper.setupPhoto(app, mainView.findViewById<ImageView>(R.id.user_icon),
				telegramHelper.getUserPhotoPath(user), R.drawable.img_user_placeholder, false)
		}
		app.osmandAidlHelper.setGpxBitmapCreatedListener(
			object : OsmandAidlHelper.GpxBitmapCreatedListener {
				override fun onGpxBitmapCreated(bitmap: AGpxBitmap) {
					activity?.runOnUiThread {
						mainView.findViewById<ImageView>(R.id.gpx_map).setImageDrawable(BitmapDrawable(app.resources, bitmap.bitmap))
					}
				}
			})

		updateGPXMap()

		val backBtn = mainView.findViewById<ImageView>(R.id.back_button)
		backBtn.setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_arrow_back))
		backBtn.setOnClickListener {
			dismiss()
		}

		dateStartBtn = mainView.findViewById<TextView>(R.id.date_start_btn)
		timeStartBtn = mainView.findViewById<TextView>(R.id.time_start_btn)
		dateEndBtn = mainView.findViewById<TextView>(R.id.date_end_btn)
		timeEndBtn = mainView.findViewById<TextView>(R.id.time_end_btn)

		dateStartBtn.setOnClickListener { selectStartDate() }
		timeStartBtn.setOnClickListener { selectStartTime() }
		dateEndBtn.setOnClickListener { selectEndDate() }
		timeEndBtn.setOnClickListener { selectEndTime() }

		setupBtnTextColor(dateStartBtn)
		setupBtnTextColor(timeStartBtn)
		setupBtnTextColor(dateEndBtn)
		setupBtnTextColor(timeEndBtn)

		updateDateAndTimeButtons()

		avgElevationTv = mainView.findViewById<TextView>(R.id.average_altitude_text)
		avgSpeedTv = mainView.findViewById<TextView>(R.id.average_speed_text)
		totalDistanceTv = mainView.findViewById<TextView>(R.id.distance_text)
		timeSpanTv = mainView.findViewById<TextView>(R.id.duration_text)

		mainView.findViewById<ImageView>(R.id.average_altitude_icon).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_altitude_range))
		}
		mainView.findViewById<ImageView>(R.id.average_speed_icon).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_speed_average))
		}
		mainView.findViewById<ImageView>(R.id.distance_icon).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_altitude_range))
		}
		mainView.findViewById<ImageView>(R.id.duration_icon).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_altitude_range))
		}

		updateGPXStatisticRow()

		mainView.findViewById<ImageView>(R.id.open_in_osmand_icon).setImageResource(R.drawable.ic_logo_osmand_free)

		mainView.findViewById<LinearLayout>(R.id.open_in_osmand_btn).apply {
			setOnClickListener {
				val gpx = gpxFile
				if (gpx.path.isNotEmpty()) {
					openGpx(gpx.path)
				} else {
					saveCurrentGpxToFile(object :
						SavingTracksDbHelper.SaveGpxListener {
						override fun onSavingGpxFinish(path: String) {
							openGpx(path)
						}

						override fun onSavingGpxError(warnings: MutableList<String>?) {
							Toast.makeText(app, warnings?.firstOrNull(), Toast.LENGTH_LONG).show()
						}
					})
				}
			}
		}

		mainView.findViewById<ImageView>(R.id.share_gpx_icon).setImageDrawable(uiUtils.getActiveIcon(R.drawable.ic_action_share))

		mainView.findViewById<LinearLayout>(R.id.share_gpx_btn).apply {
			setOnClickListener {
				val gpx = gpxFile
				if (gpx.path.isNotEmpty()) {
					(activity as MainActivity).shareGpx(gpx.path)
				} else {
					saveCurrentGpxToFile(object :
						SavingTracksDbHelper.SaveGpxListener {
						override fun onSavingGpxFinish(path: String) {
							(activity as MainActivity).shareGpx(path)
						}

						override fun onSavingGpxError(warnings: MutableList<String>?) {
							Toast.makeText(app, warnings?.firstOrNull(), Toast.LENGTH_LONG).show()
						}
					})
				}
			}
		}

		return mainView
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLong(START_KEY, startCalendar.timeInMillis)
		outState.putLong(END_KEY, endCalendar.timeInMillis)
	}

	private fun openGpx(path: String) {
		val fileUri = AndroidUtils.getUriForFile(app, File(path))
		val openGpxIntent = Intent(Intent.ACTION_VIEW)
		openGpxIntent.setDataAndType(fileUri, "application/gpx+xml")
		openGpxIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val resolved = activity?.packageManager?.resolveActivity(openGpxIntent, PackageManager.MATCH_DEFAULT_ONLY)
		if (resolved != null) {
			startActivity(openGpxIntent)
		}
	}

	private fun saveCurrentGpxToFile(listener: SavingTracksDbHelper.SaveGpxListener) {
		app.savingTracksDbHelper.saveGpx(listener, app.getExternalFilesDir(null), gpxFile)
	}

	private fun readFromBundle(bundle: Bundle?) {
		bundle?.also {
			startCalendar.timeInMillis = it.getLong(START_KEY)
			endCalendar.timeInMillis = it.getLong(END_KEY)
		}
	}

	private fun setupBtnTextColor(textView: TextView) {
		textView.setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
	}

	private fun updateGpxInfo() {
		gpxFile = app.savingTracksDbHelper.collectRecordedDataForUser(gpxFile.userId, gpxFile.chatId, startCalendar.timeInMillis, endCalendar.timeInMillis)
		updateGPXStatisticRow()
		updateDateAndTimeButtons()
		updateGPXMap()
	}

	private fun updateDateAndTimeButtons() {
		dateStartBtn.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(startCalendar.timeInMillis)
		dateEndBtn.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(endCalendar.timeInMillis)

		timeStartBtn.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startCalendar.timeInMillis)
		timeEndBtn.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endCalendar.timeInMillis)
	}

	private fun updateGPXStatisticRow() {
		val analysis: GPXUtilities.GPXTrackAnalysis = gpxFile.getAnalysis(0)
		avgElevationTv.text = OsmandFormatter.getFormattedAlt(analysis.avgElevation, app)
		avgSpeedTv.text = if (analysis.isSpeedSpecified) OsmandFormatter.getFormattedSpeed(analysis.avgSpeed, app) else ""
		totalDistanceTv.text = OsmandFormatter.getFormattedDistance(analysis.totalDistance, app)
		timeSpanTv.text = Algorithms.formatDuration((analysis.timeSpan / 1000).toInt(), true)
	}

	private fun updateGPXMap() {
		saveCurrentGpxToFile(object :
			SavingTracksDbHelper.SaveGpxListener {
			override fun onSavingGpxFinish(path: String) {
				val mgr = activity?.getSystemService(Context.WINDOW_SERVICE)
				if (mgr != null) {
					val dm = DisplayMetrics()
					(mgr as WindowManager).defaultDisplay.getMetrics(dm)
					val widthPixels = dm.widthPixels - (2 * app.resources.getDimensionPixelSize(R.dimen.content_padding_standard))
					val heightPixels = AndroidUtils.dpToPx(app, 152f)
					val gpxUri = AndroidUtils.getUriForFile(app, File(path))
					app.osmandAidlHelper.getBitmapForGpx(gpxUri, dm.density , widthPixels, heightPixels, GPX_TRACK_COLOR)
				}
			}

			override fun onSavingGpxError(warnings: MutableList<String>?) {
				log.debug("onSavingGpxError ${warnings?.firstOrNull()}")
			}
		})
	}

	private fun selectStartDate() {
		val dateFromDialog =
			DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
				startCalendar.set(Calendar.YEAR, year)
				startCalendar.set(Calendar.MONTH, monthOfYear)
				startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
				updateGpxInfo()
			}
		DatePickerDialog(context, dateFromDialog,
			startCalendar.get(Calendar.YEAR),
			startCalendar.get(Calendar.MONTH),
			startCalendar.get(Calendar.DAY_OF_MONTH)).show()
	}

	private fun selectStartTime() {
		TimePickerDialog(context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				startCalendar.set(Calendar.HOUR_OF_DAY, hours)
				startCalendar.set(Calendar.MINUTE, minutes)
				updateGpxInfo()
			}, 0, 0, true).show()
	}

	private fun selectEndDate() {
		val dateFromDialog =
			DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
				endCalendar.set(Calendar.YEAR, year)
				endCalendar.set(Calendar.MONTH, monthOfYear)
				endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
				updateGpxInfo()
			}
		DatePickerDialog(context, dateFromDialog,
			endCalendar.get(Calendar.YEAR),
			endCalendar.get(Calendar.MONTH),
			endCalendar.get(Calendar.DAY_OF_MONTH)).show()
	}

	private fun selectEndTime() {
		TimePickerDialog(context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				endCalendar.set(Calendar.HOUR_OF_DAY, hours)
				endCalendar.set(Calendar.MINUTE, minutes)
				updateGpxInfo()
			}, 0, 0, true).show()
	}

	companion object {

		private const val TAG = "UserGpxInfoFragment"
		private const val START_KEY = "start_key"
		private const val END_KEY = "end_key"

		private const val GPX_TRACK_COLOR = -65536

		fun showInstance(fm: FragmentManager, gpxFile: GPXUtilities.GPXFile, start: Long, end: Long): Boolean {
			return try {
				val fragment = UserGpxInfoFragment().apply {
					arguments = Bundle().apply {
						putLong(START_KEY, start)
						putLong(END_KEY, end)
					}
				}
				fragment.gpxFile = gpxFile
				fragment.show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}