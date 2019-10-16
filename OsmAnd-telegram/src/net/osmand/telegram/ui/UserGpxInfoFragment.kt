package net.osmand.telegram.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.*
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import net.osmand.GPXUtilities
import net.osmand.PlatformUtil
import net.osmand.aidlapi.gpx.AGpxBitmap
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.util.Algorithms
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UserGpxInfoFragment : BaseDialogFragment() {

	private val log = PlatformUtil.getLog(UserGpxInfoFragment::class.java)

	private val uiUtils get() = app.uiUtils

	private var gpxDataItem: GpxDataItem? = null

	private var loadGpxAsyncTask: LoadGpxAsyncTask? = null
	private lateinit var loadGpxListener: LoadGpxListener

	private lateinit var mainView: View
	private lateinit var dateTimeBtn: TextView
	private lateinit var liveBtn: TextView

	private lateinit var iconMap: ImageView

	private lateinit var avgElevationTv: TextView
	private lateinit var avgSpeedTv: TextView
	private lateinit var totalDistanceTv: TextView
	private lateinit var timeSpanTv: TextView

	private var startCalendar = Calendar.getInstance()
	private var endCalendar = Calendar.getInstance()

	private var userId = -1
	private var chatId = -1L
	private var deviceName = ""

	private var handler: Handler = Handler()

	private var endTimeChanged = false
	private var snackbarShown = false

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

		loadGpxListener = object : LoadGpxListener {
			override fun onLoadGpxFinish(dataItem: GpxDataItem) {
				gpxDataItem = dataItem
				updateGPXStatisticRow()
				updateDateAndTimeButton()
				updateGPXMap()
			}

			override fun onLoadGpxError(error: String) {
				log.error(error)
			}
		}

		val openGpxListener = View.OnClickListener {
			val gpx = gpxDataItem?.gpxFile
			if (gpx != null) {
				if (gpx.path.isNotEmpty()) {
					openGpx(gpx.path)
				} else {
					saveCurrentGpxToFile(object :
						OsmandLocationUtils.SaveGpxListener {

						override fun onSavingGpxFinish(path: String) {
							openGpx(path)
						}

						override fun onSavingGpxError(error: Exception) {
							Toast.makeText(app, error.message, Toast.LENGTH_LONG).show()
						}
					})
				}
			}
		}

		iconMap = mainView.findViewById<ImageView>(R.id.gpx_map)
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

		dateTimeBtn = mainView.findViewById<TextView>(R.id.date_time_btn).apply {
			setOnClickListener {
				fragmentManager?.also { fm ->
					SetTimeBottomSheet.showInstance(fm, this@UserGpxInfoFragment, startCalendar.timeInMillis, endCalendar.timeInMillis)
				}
			}
			setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
		}
		updateDateAndTimeButton()

		liveBtn = mainView.findViewById<TextView>(R.id.live_btn).apply {
			setOnClickListener {
				val enabled = !liveTrackEnabled()
				settings.updateLiveTrack(userId, chatId, deviceName, enabled)
				updateLiveTrackBtn()
				if (enabled) {
					startHandler()
				}
			}
		}
		updateLiveTrackBtn()

		avgElevationTv = mainView.findViewById<TextView>(R.id.average_altitude_text)
		avgSpeedTv = mainView.findViewById<TextView>(R.id.average_speed_text)
		totalDistanceTv = mainView.findViewById<TextView>(R.id.distance_text)
		timeSpanTv = mainView.findViewById<TextView>(R.id.duration_text)

		updateGPXStatisticRow()

		val imageRes = if (app.isOsmAndInstalled()) {
			TelegramSettings.AppConnect.getIconId(settings.appToConnectPackage)
		} else {
			R.drawable.ic_logo_osmand_free
		}
		mainView.findViewById<ImageView>(R.id.open_in_osmand_icon).setImageResource(imageRes)
		mainView.findViewById<LinearLayout>(R.id.open_in_osmand_btn).setOnClickListener(openGpxListener)

		mainView.findViewById<TextView>(R.id.open_in_osmand_title).setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
		mainView.findViewById<TextView>(R.id.share_gpx_title).setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))

		mainView.findViewById<ImageView>(R.id.share_gpx_icon).setImageDrawable(getShareIcon())
		mainView.findViewById<LinearLayout>(R.id.share_gpx_btn).apply {
			setOnClickListener {
				val gpx = gpxDataItem?.gpxFile
				if (gpx != null) {
					if (gpx.path.isNotEmpty()) {
						(activity as MainActivity).shareGpx(gpx.path)
					} else {
						saveCurrentGpxToFile(object :
							OsmandLocationUtils.SaveGpxListener {
							override fun onSavingGpxFinish(path: String) {
								(activity as MainActivity).shareGpx(path)
							}

							override fun onSavingGpxError(error: Exception) {
								Toast.makeText(app, error.message, Toast.LENGTH_LONG).show()
							}
						})
					}
				}
			}
		}

		updateGpxInfo()

		return mainView
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(USER_ID_KEY, userId)
		outState.putLong(CHAT_ID_KEY, chatId)
		outState.putString(DEVICE_NAME_KEY, deviceName)
		outState.putLong(START_KEY, startCalendar.timeInMillis)
		outState.putLong(END_KEY, endCalendar.timeInMillis)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			ChooseOsmAndBottomSheet.OSMAND_CHOSEN_REQUEST_CODE -> updateGPXMap()
			SetTimeBottomSheet.SET_TIME_REQUEST_CODE -> {
				if (data != null) {
					val startTime = data.getLongExtra(SetTimeBottomSheet.START_TIME, -1)
					val endTime = data.getLongExtra(SetTimeBottomSheet.END_TIME, -1)
					if (startTime != -1L && endTime != -1L) {
						endTimeChanged = endCalendar.timeInMillis != endTime
						startCalendar.timeInMillis = startTime
						endCalendar.timeInMillis = endTime
						updateGpxInfo()
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if (liveTrackEnabled()) {
			startHandler()
		}
	}

	private fun startHandler() {
		if (!handler.hasMessages(TRACK_UPDATE_MSG_ID)) {
			val msg = Message.obtain(handler) {
				if (isResumed && liveTrackEnabled()) {
					updateGpxInfo()
					startHandler()
				}
			}
			msg.what = TRACK_UPDATE_MSG_ID
			handler.sendMessageDelayed(msg, UPDATE_TRACK_INTERVAL_MS)
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
		val gpx = gpxDataItem?.gpxFile
		if (gpx != null) {
			OsmandLocationUtils.saveGpx(app, gpx, listener)
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

	private fun liveTrackEnabled() = settings.isLiveTrackEnabled(userId, chatId, deviceName)

	private fun updateLiveTrackBtn() {
		val enabled = liveTrackEnabled()
		val icon = getLiveTrackBtnIcon(enabled)
		val normalTextColor = if (enabled) R.color.ctrl_active_light else R.color.secondary_text_light

		liveBtn.setTextColor(AndroidUtils.createPressedColorStateList(app, true, normalTextColor, R.color.ctrl_light))
		liveBtn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
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

	private fun getLiveTrackBtnIcon(enabled: Boolean): Drawable? {
		val iconColor = if (enabled) R.color.live_track_active_icon else R.color.icon_light

		val layers = arrayOfNulls<Drawable>(2)
		layers[0] = app.uiUtils.getIcon(R.drawable.ic_action_round_shape)
		layers[1] = app.uiUtils.getIcon(R.drawable.ic_action_record, iconColor)

		if (Build.VERSION.SDK_INT >= 21 && !enabled) {
			val normal = layers[1]
			val active = app.uiUtils.getIcon(R.drawable.ic_action_record, R.color.live_track_active_icon)
			if (normal != null && active != null) {
				layers[1] = AndroidUtils.createPressedStateListDrawable(normal, active)
			}
		}

		return LayerDrawable(layers)
	}

	private fun updateGpxInfo() {
		checkTime()
		stopLoadGpxAsyncTask()
		loadGpxAsyncTask = LoadGpxAsyncTask(app, userId, chatId, deviceName, startCalendar.timeInMillis, endCalendar.timeInMillis, loadGpxListener)
		loadGpxAsyncTask!!.execute()
	}

	private fun stopLoadGpxAsyncTask() {
		val asyncTask = loadGpxAsyncTask
		if (asyncTask != null && asyncTask.status == AsyncTask.Status.RUNNING) {
			asyncTask.cancel(false)
		}
	}

	private fun checkTime() {
		val enabled = liveTrackEnabled()
		if (enabled && !endTimeChanged) {
			val locationMessage = app.locationMessages.getLastLocationInfoForUserInChat(userId, chatId, deviceName)
			if (locationMessage != null && locationMessage.time > endCalendar.timeInMillis) {
				endCalendar.timeInMillis = locationMessage.time
			}
		}
		if (startCalendar.timeInMillis > endCalendar.timeInMillis) {
			val time = startCalendar.timeInMillis
			startCalendar.timeInMillis = endCalendar.timeInMillis
			endCalendar.timeInMillis = time
		}
	}

	private fun updateDateAndTimeButton() {
		val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
		val start = dateFormat.format(startCalendar.timeInMillis)
		val end = dateFormat.format(endCalendar.timeInMillis)
		val text = "$start — $end"
		dateTimeBtn.text = SpannableString(text).apply {
			val index = text.indexOf("—")
			if (index != -1) {
				setSpan(ForegroundColorSpan(ContextCompat.getColor(app, R.color.secondary_text_light)), index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
		}
	}

	private fun updateGPXStatisticRow() {
		val analysis = gpxDataItem?.analysis
		avgElevationTv.text = if (analysis != null && analysis.avgElevation != 0.0) OsmandFormatter.getFormattedAlt(analysis.avgElevation, app) else "-"
		avgSpeedTv.text = if (analysis != null && analysis.isSpeedSpecified) OsmandFormatter.getFormattedSpeed(analysis.avgSpeed, app) else "-"
		totalDistanceTv.text = if (analysis != null && analysis.totalDistance != 0.0f) OsmandFormatter.getFormattedDistance(analysis.totalDistance, app) else "-"
		timeSpanTv.text = if (analysis != null && analysis.timeSpan != 0L) Algorithms.formatDuration((analysis.timeSpan / 1000).toInt(), true) else "-"
	}

	private fun updateGPXMap() {
		if (!app.isAnyOsmAndInstalled()) {
			activity?.let {
				MainActivity.OsmandMissingDialogFragment().show(it.supportFragmentManager, null)
			}
		} else if (!app.isOsmAndChosen() || (app.isOsmAndChosen() && !app.isOsmAndInstalled())) {
			fragmentManager?.also { ChooseOsmAndBottomSheet.showInstance(it, this) }
		} else if (!canOsmandCreateBitmap()) {
			if (!snackbarShown) {
				val snackbar = Snackbar.make(mainView, R.string.please_update_osmand, Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_update) {
							val packageName = if (app.settings.appToConnectPackage == OsmandAidlHelper.OSMAND_NIGHTLY_PACKAGE_NAME)
									OsmandAidlHelper.OSMAND_FREE_PACKAGE_NAME else app.settings.appToConnectPackage
							startActivity(AndroidUtils.getPlayMarketIntent(app, packageName))
						}
				AndroidUtils.setSnackbarTextColor(snackbar, R.color.ctrl_active_dark)
				snackbar.show()
				snackbarShown = true
			}
		} else {
			saveCurrentGpxToFile(object :
				OsmandLocationUtils.SaveGpxListener {
				override fun onSavingGpxFinish(path: String) {
					val mgr = activity?.getSystemService(Context.WINDOW_SERVICE)
					if (mgr != null) {
						val dm = DisplayMetrics()
						(mgr as WindowManager).defaultDisplay.getMetrics(dm)
						val widthPixels = iconMap.width
						val heightPixels = iconMap.height
						val gpxUri = AndroidUtils.getUriForFile(app, File(path))
						app.osmandAidlHelper.execOsmandApi {
							app.osmandAidlHelper.getBitmapForGpx(gpxUri, dm.density, widthPixels, heightPixels, GPX_TRACK_COLOR)
						}
					}
				}

				override fun onSavingGpxError(error: Exception) {
					log.error(error)
				}
			})
		}
	}

	private class LoadGpxAsyncTask internal constructor(
		private val app: TelegramApplication,
		private val userId: Int,
		private val chatId: Long,
		private val deviceName: String,
		private val start: Long,
		private val end: Long,
		private val listener: LoadGpxListener
	) :
		AsyncTask<Void, Void, GpxDataItem>() {

		override fun doInBackground(vararg params: Void): GpxDataItem? {
			val locationMessages = app.locationMessages.getMessagesForUserInChat(userId, chatId, deviceName, start, end)
			if (locationMessages.isNotEmpty() && !isCancelled) {
				val items = OsmandLocationUtils.convertLocationMessagesToGpxFiles(app, locationMessages)
				if (items.isNotEmpty() && !isCancelled) {
					val gpx = items.firstOrNull()
					if (gpx != null) {
						val analysis = gpx.getAnalysis(0)
						return if (!isCancelled) GpxDataItem(gpx, analysis) else null
					}
				}
			}
			return null
		}

		override fun onPostExecute(gpxDataItem: GpxDataItem?) {
			if (gpxDataItem != null) {
				listener.onLoadGpxFinish(gpxDataItem)
			} else {
				listener.onLoadGpxError("Cant create gpx for $userId $chatId $deviceName $start $end")
			}
		}
	}

	interface LoadGpxListener {

		fun onLoadGpxFinish(dataItem: GpxDataItem)

		fun onLoadGpxError(error: String)

	}

	data class GpxDataItem(
		val gpxFile: GPXUtilities.GPXFile,
		val analysis: GPXUtilities.GPXTrackAnalysis
	)

	companion object {

		private const val TAG = "UserGpxInfoFragment"
		private const val START_KEY = "start_key"
		private const val END_KEY = "end_key"
		private const val USER_ID_KEY = "user_id_key"
		private const val CHAT_ID_KEY = "chat_id_key"
		private const val DEVICE_NAME_KEY = "device_name_key"
		private const val DATE_FORMAT = "dd MMM HH:mm"

		private const val GPX_TRACK_COLOR = -65536
		private	const val MIN_OSMAND_BITMAP_VERSION_CODE = 330
		private const val UPDATE_TRACK_INTERVAL_MS = 30 * 1000L // 30 sec
		private const val TRACK_UPDATE_MSG_ID = 1001

		fun showInstance(fm: FragmentManager, userId: Int, chatId: Long, deviceName: String, start: Long, end: Long): Boolean {
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
