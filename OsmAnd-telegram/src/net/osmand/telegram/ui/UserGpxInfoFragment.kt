package net.osmand.telegram.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
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
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.helpers.LocationMessages
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.*
import net.osmand.util.Algorithms
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UserGpxInfoFragment : BaseDialogFragment() {

	private val log = PlatformUtil.getLog(UserGpxInfoFragment::class.java)

	private val uiUtils get() = app.uiUtils

	private var gpxFile = GPXUtilities.GPXFile()

	private lateinit var mainView: View
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

	private var locationMessages = emptyList<LocationMessages.LocationMessage>()

	private var userId = -1
	private var chatId = -1L
	private var deviceName = ""

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(R.layout.fragment_user_gpx_info, parent)
		AndroidUtils.addStatusBarPadding19v(context!!, mainView)

		readFromBundle(savedInstanceState ?: arguments)

		val user = app.telegramHelper.getUser(userId)
		if (deviceName.isNotEmpty()) {
			mainView.findViewById<TextView>(R.id.title).text = deviceName
			TelegramUiHelper.setupPhoto(app, mainView.findViewById<ImageView>(R.id.user_icon),
				null, R.drawable.img_user_placeholder, false)
		} else if (user != null) {
			mainView.findViewById<TextView>(R.id.title).text = TelegramUiHelper.getUserName(user)
			TelegramUiHelper.setupPhoto(app, mainView.findViewById<ImageView>(R.id.user_icon),
				telegramHelper.getUserPhotoPath(user), R.drawable.img_user_placeholder, false)
		}
		val openGpxListener = View.OnClickListener {
			val gpx = gpxFile
			if (gpx.path.isNotEmpty()) {
				openGpx(gpx.path)
			} else {
				saveCurrentGpxToFile(object :
					OsmandLocationUtils.SaveGpxListener {

					override fun onSavingGpxFinish(path: String) {
						openGpx(path)
					}

					override fun onSavingGpxError(warnings: List<String>?) {
						Toast.makeText(app, warnings?.firstOrNull(), Toast.LENGTH_LONG).show()
					}
				})
			}
		}
		val iconMap = mainView.findViewById<ImageView>(R.id.gpx_map)
		app.osmandAidlHelper.setGpxBitmapCreatedListener(
			object : OsmandAidlHelper.GpxBitmapCreatedListener {
				override fun onGpxBitmapCreated(bitmap: AGpxBitmap) {
					activity?.runOnUiThread {
						iconMap.setImageDrawable(BitmapDrawable(app.resources, bitmap.bitmap))
						iconMap.setOnClickListener(openGpxListener)
					}
				}
			})

		mainView.findViewById<ImageView>(R.id.back_button).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_arrow_back))
			setOnClickListener {
				dismiss()
			}
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
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_sort_by_distance))
		}
		mainView.findViewById<ImageView>(R.id.duration_icon).apply {
			setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_time_span))
		}

		updateGPXStatisticRow()

		val imageRes = if (app.isOsmAndInstalled()) {
			TelegramSettings.AppConnect.getIconId(settings.appToConnectPackage)
		} else {
			R.drawable.ic_logo_osmand_free
		}
		mainView.findViewById<ImageView>(R.id.open_in_osmand_icon).setImageResource(imageRes)
		mainView.findViewById<LinearLayout>(R.id.open_in_osmand_btn).setOnClickListener(openGpxListener)

		mainView.findViewById<TextView>(R.id.open_in_osmand_title).setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.primary_text_light, R.color.ctrl_light))
		mainView.findViewById<TextView>(R.id.share_gpx_title).setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.primary_text_light, R.color.ctrl_light))

		mainView.findViewById<ImageView>(R.id.share_gpx_icon).setImageDrawable(getShareIcon())
		mainView.findViewById<LinearLayout>(R.id.share_gpx_btn).apply {
			setOnClickListener {
				val gpx = gpxFile
				if (gpx.path.isNotEmpty()) {
					(activity as MainActivity).shareGpx(gpx.path)
				} else {
					saveCurrentGpxToFile(object :
						OsmandLocationUtils.SaveGpxListener {
						override fun onSavingGpxFinish(path: String) {
							(activity as MainActivity).shareGpx(path)
						}

						override fun onSavingGpxError(warnings: List<String>?) {
							Toast.makeText(app, warnings?.firstOrNull(), Toast.LENGTH_LONG).show()
						}
					})
				}
			}
		}

		updateGpxInfo()

		return mainView
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLong(START_KEY, startCalendar.timeInMillis)
		outState.putLong(END_KEY, endCalendar.timeInMillis)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			ChooseOsmAndBottomSheet.OSMAND_CHOSEN_REQUEST_CODE -> updateGPXMap()
		}
	}

	private fun canOsmandCreateBitmap(): Boolean {
		val version = AndroidUtils.getAppVersionCode(app, app.settings.appToConnectPackage)
		return version >= MIN_OSMAND_BITMAP_VERSION_CODE
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

	private fun saveCurrentGpxToFile(listener: OsmandLocationUtils.SaveGpxListener) {
		if (!gpxFile.isEmpty) {
			val file = File(app.getExternalFilesDir(null), TRACKS_DIR)
			OsmandLocationUtils.saveGpx(app, listener, file, gpxFile)
		}
	}

	private fun readFromBundle(bundle: Bundle?) {
		bundle?.also {
			userId = it.getInt(USER_ID_KEY)
			chatId = it.getLong(CHAT_ID_KEY)
			deviceName = it.getString(DEVICE_NAME_KEY, "")
			startCalendar.timeInMillis = it.getLong(START_KEY)
			endCalendar.timeInMillis = it.getLong(END_KEY)
		}
	}

	private fun setupBtnTextColor(textView: TextView) {
		textView.setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
	}

	private fun getShareIcon(): Drawable? {
		val normal = app.uiUtils.getActiveIcon(R.drawable.ic_action_share)
		if (Build.VERSION.SDK_INT >= 21) {
			val active = app.uiUtils.getIcon(R.drawable.ic_action_share, R.color.ctrl_light)
			if (normal != null && active != null) {
				return AndroidUtils.createPressedStateListDrawable(normal, active)
			}
		}
		return normal
	}

	private fun updateGpxInfo() {
		checkTime()
		locationMessages = app.locationMessages.getMessagesForUserInChat(userId, chatId,deviceName, startCalendar.timeInMillis, endCalendar.timeInMillis)

		gpxFile = OsmandLocationUtils.convertLocationMessagesToGpxFiles(locationMessages).firstOrNull()?:GPXUtilities.GPXFile()
		updateGPXStatisticRow()
		updateDateAndTimeButtons()
		updateGPXMap()
	}

	private fun checkTime() {
		if (startCalendar.timeInMillis > endCalendar.timeInMillis) {
			val time = startCalendar.timeInMillis
			startCalendar.timeInMillis = endCalendar.timeInMillis
			endCalendar.timeInMillis = time
		}
	}

	private fun updateDateAndTimeButtons() {
		dateStartBtn.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(startCalendar.timeInMillis)
		dateEndBtn.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(endCalendar.timeInMillis)

		timeStartBtn.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startCalendar.timeInMillis)
		timeEndBtn.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endCalendar.timeInMillis)
	}

	private fun updateGPXStatisticRow() {
		val analysis: GPXUtilities.GPXTrackAnalysis = gpxFile.getAnalysis(0)

		avgElevationTv.text = if (analysis.avgElevation != 0.0) OsmandFormatter.getFormattedAlt(analysis.avgElevation, app) else "-"
		avgSpeedTv.text = if (analysis.isSpeedSpecified) OsmandFormatter.getFormattedSpeed(analysis.avgSpeed, app) else "-"
		totalDistanceTv.text = if (analysis.totalDistance != 0.0f) OsmandFormatter.getFormattedDistance(analysis.totalDistance, app)  else  "-"
		timeSpanTv.text = if (analysis.timeSpan != 0L)  Algorithms.formatDuration((analysis.timeSpan / 1000).toInt(), true) else  "-"
	}

	private fun updateGPXMap() {
		if (!app.isAnyOsmAndInstalled()) {
			activity?.let {
				MainActivity.OsmandMissingDialogFragment().show(it.supportFragmentManager, null)
			}
		} else if (!app.isOsmAndChosen() || (app.isOsmAndChosen() && !app.isOsmAndInstalled())) {
			fragmentManager?.also { ChooseOsmAndBottomSheet.showInstance(it, this) }
		} else if (!canOsmandCreateBitmap()) {
			 val snackbar = Snackbar.make(mainView, R.string.please_update_osmand, Snackbar.LENGTH_LONG).setAction(R.string.shared_string_update) {
					val packageName = if (app.settings.appToConnectPackage == OsmandAidlHelper.OSMAND_NIGHTLY_PACKAGE_NAME)
							OsmandAidlHelper.OSMAND_FREE_PACKAGE_NAME else app.settings.appToConnectPackage
					startActivity(AndroidUtils.getPlayMarketIntent(app, packageName))
				}
			AndroidUtils.setSnackbarTextColor(snackbar, R.color.ctrl_active_dark)
			snackbar.show()
		} else {
			saveCurrentGpxToFile(object :
				OsmandLocationUtils.SaveGpxListener {
				override fun onSavingGpxFinish(path: String) {
					val mgr = activity?.getSystemService(Context.WINDOW_SERVICE)
					if (mgr != null) {
						val dm = DisplayMetrics()
						(mgr as WindowManager).defaultDisplay.getMetrics(dm)
						val widthPixels = dm.widthPixels - (2 * app.resources.getDimensionPixelSize(R.dimen.content_padding_standard))
						val heightPixels = AndroidUtils.dpToPx(app, 152f)
						val gpxUri = AndroidUtils.getUriForFile(app, File(path))
						app.osmandAidlHelper.execOsmandApi {
							app.osmandAidlHelper.getBitmapForGpx(gpxUri, dm.density, widthPixels, heightPixels, GPX_TRACK_COLOR)
						}
					}
				}

				override fun onSavingGpxError(warnings: List<String>?) {
					log.debug("onSavingGpxError ${warnings?.firstOrNull()}")
				}
			})
		}
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
		private const val USER_ID_KEY = "user_id_key"
		private const val CHAT_ID_KEY = "chat_id_key"
		private const val DEVICE_NAME_KEY = "device_name_key"

		private const val GPX_TRACK_COLOR = -65536
		private	const val MIN_OSMAND_BITMAP_VERSION_CODE = 330

		fun showInstance(fm: FragmentManager,userId:Int,chatId:Long,deviceName:String, start: Long, end: Long): Boolean {
			return try {
				val fragment = UserGpxInfoFragment().apply {
					arguments = Bundle().apply {
						putInt(USER_ID_KEY, userId)
						putLong(CHAT_ID_KEY, chatId)
						putString(DEVICE_NAME_KEY, deviceName)
						putLong(START_KEY, start)
						putLong(END_KEY, end)
					}
				}
				fragment.show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}