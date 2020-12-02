package net.osmand.telegram.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.LocationMessages
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.ui.TimelineTabFragment.LiveNowListAdapter.BaseViewHolder
import net.osmand.telegram.ui.views.EmptyStateRecyclerView
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import java.util.*


class TimelineTabFragment : Fragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val settings get() = app.settings

	private lateinit var adapter: LiveNowListAdapter

	private lateinit var dateBtn: TextView
	private lateinit var previousDateBtn: ImageView
	private lateinit var nextDateBtn: ImageView
	private lateinit var mainView: View
	private lateinit var switcher: Switch

	private lateinit var calendar: Calendar

	private var updateEnable: Boolean = false

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(R.layout.fragment_timeline_tab, container, false)
		val appBarLayout = mainView.findViewById<View>(R.id.app_bar_layout)

		calendar = Calendar.getInstance()

		AndroidUtils.addStatusBarPadding19v(context!!, appBarLayout)
		adapter = LiveNowListAdapter()

		val emptyView = mainView.findViewById<LinearLayout>(R.id.empty_view)
		mainView.findViewById<EmptyStateRecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@TimelineTabFragment.adapter
			setEmptyView(emptyView)
		}

		switcher = mainView.findViewById<Switch>(R.id.monitoring_switcher)
		val monitoringTv = mainView.findViewById<TextView>(R.id.monitoring_title)
		switcher.isChecked = settings.monitoringEnabled
		monitoringTv.setText(if (settings.monitoringEnabled) R.string.monitoring_is_enabled else R.string.monitoring_is_disabled)

		mainView.findViewById<View>(R.id.monitoring_container).setOnClickListener {
			settings.monitoringEnabled = !settings.monitoringEnabled
			app.showLocationHelper.changeUpdatesType()
			switcher.isChecked = settings.monitoringEnabled
			monitoringTv.setText(if (settings.monitoringEnabled) R.string.monitoring_is_enabled else R.string.monitoring_is_disabled)
		}

		dateBtn = mainView.findViewById<TextView>(R.id.date_btn).apply {
			setOnClickListener {
				selectDate()
			}
			setCompoundDrawablesWithIntrinsicBounds(getPressedStateIcon(R.drawable.ic_action_date_start), null, null, null)
			setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
		}
		updateDateButton()

		previousDateBtn = mainView.findViewById<ImageView>(R.id.date_btn_previous).apply {
			setImageDrawable(getPressedStateIcon(R.drawable.ic_arrow_back))
			setOnClickListener {
				calendar.add(Calendar.DAY_OF_MONTH, -1)
				updateList()
				updateDateButton()
			}
		}

		nextDateBtn = mainView.findViewById<ImageView>(R.id.date_btn_next).apply {
			setImageDrawable(getPressedStateIcon(R.drawable.ic_arrow_forward))
			setOnClickListener {
				calendar.add(Calendar.DAY_OF_MONTH, 1)
				updateList()
				updateDateButton()
			}
		}

		mainView.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh).apply {
			setOnRefreshListener {
				updateList()
				isRefreshing = false
			}
			setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN)
		}
		updateList()

		return mainView
	}

	override fun onResume() {
		super.onResume()
		updateEnable = true
		startHandler()
	}

	override fun onPause() {
		super.onPause()
		updateEnable = false
	}

	fun tabOpened() {
		switcher.isChecked = settings.monitoringEnabled
		updateList()
	}

	fun tabClosed() {}

	private fun selectDate() {
		context?.let {
			val dateSetListener =
				DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
					calendar = Calendar.getInstance()
					calendar.set(Calendar.YEAR, year)
					calendar.set(Calendar.MONTH, monthOfYear)
					calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

					updateList()
					updateDateButton()
				}
			DatePickerDialog(
				it, dateSetListener,
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH)
			).show()
		}
	}

	private fun getStartOfDay(calendar: Calendar): Long {
		calendar.set(Calendar.HOUR_OF_DAY, 0)
		calendar.clear(Calendar.MINUTE)
		calendar.clear(Calendar.SECOND)
		calendar.clear(Calendar.MILLISECOND)
		return calendar.timeInMillis
	}

	private fun getEndOfDay(calendar: Calendar): Long {
		calendar.set(Calendar.HOUR_OF_DAY, 23)
		calendar.set(Calendar.MINUTE, 59)
		calendar.set(Calendar.SECOND, 59)
		calendar.set(Calendar.MILLISECOND, 999)
		return calendar.timeInMillis
	}

	private fun updateDateButton() {
		dateBtn.text = OsmandFormatter.getFormattedDate(getStartOfDay(calendar) / 1000)
	}

	private fun getPressedStateIcon(@DrawableRes iconId: Int): Drawable? {
		val normal = app.uiUtils.getActiveIcon(iconId)
		if (Build.VERSION.SDK_INT >= 21) {
			val active = app.uiUtils.getIcon(iconId, R.color.ctrl_light)
			if (normal != null && active != null) {
				return AndroidUtils.createPressedStateListDrawable(normal, active)
			}
		}
		return normal
	}

	private fun startHandler() {
		val updateAdapter = Handler()
		updateAdapter.postDelayed({
			if (updateEnable) {
				updateList()
				startHandler()
			}
		}, ADAPTER_UPDATE_INTERVAL_MIL)
	}

	private fun updateList() {
		val res = mutableListOf<ListItem>()
		val start = getStartOfDay(calendar)
		val end = getEndOfDay(calendar)
		app.locationMessages.getIngoingUserLocations(start, end).forEach {
			TelegramUiHelper.userLocationsToChatItem(telegramHelper, it)?.also { chatItem ->
				res.add(chatItem)
			}
		}
		adapter.items = sortAdapterItems(res)
	}

	private fun sortAdapterItems(list: MutableList<ListItem>): MutableList<ListItem> {
		val currentUserId = telegramHelper.getCurrentUserId()
		list.sortWith(java.util.Comparator { lhs, rhs ->
			when (currentUserId) {
				lhs.userId -> return@Comparator -1
				rhs.userId -> return@Comparator 1
				else -> return@Comparator lhs.name.compareTo(rhs.name)
			}
		})
		return list
	}

	inner class LiveNowListAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<BaseViewHolder>() {

		var items: List<ListItem> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			return BaseViewHolder(inflater.inflate(R.layout.live_now_chat_card, parent, false))
		}

		@SuppressLint("SetTextI18n")
		override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
			val lastItem = position == itemCount - 1
			val item = items[position]
			val currentUserId = telegramHelper.getCurrentUserId()
			TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, R.drawable.img_user_picture_active, false)
			holder.title?.text = item.name
			holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
			holder.lastTelegramUpdateTime?.visibility = View.GONE

			if (item is TelegramUiHelper.LocationMessagesChatItem) {
				val userLocations = item.userLocations
				if (userLocations != null) {
					holder.title?.text = if (item.chatWithBot) userLocations.deviceName else item.name
					val trackData = getDistanceAndCountedPoints(userLocations)
					val distance = OsmandFormatter.getFormattedDistance(trackData.dist, app)
					val groupDescrRowVisible = (!item.privateChat || item.chatWithBot) && item.userId != currentUserId
					if (groupDescrRowVisible) {
						holder.groupDescrContainer?.visibility = View.VISIBLE
						holder.groupTitle?.text = item.getVisibleName()
						TelegramUiHelper.setupPhoto(app, holder.groupImage, item.groupPhotoPath, item.placeholderId, false)
					} else {
						holder.groupDescrContainer?.visibility = View.GONE
					}
					holder.locationAndDescrContainer?.visibility = View.GONE
					holder.distanceAndPointsContainer?.visibility = View.VISIBLE
					holder.distanceImage?.setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_distance_16dp))
					val bullet = if (groupDescrRowVisible) " • " else ""
					val points = if (app.settings.showGpsPoints) "(${getString(R.string.points_size, trackData.points)})" else ""
					holder.distanceAndPointsTitle?.text = "$distance $points $bullet "
					holder.userRow?.setOnClickListener {
						childFragmentManager.also {
							UserGpxInfoFragment.showInstance(it, item.userId, item.chatId, userLocations.deviceName ,trackData.minTime, trackData.maxTime)
						}
					}
				}

				holder.imageButton?.visibility = View.GONE
				holder.showOnMapRow?.visibility = View.GONE
				holder.bottomDivider?.visibility = if (lastItem) View.GONE else View.VISIBLE
				holder.topDivider?.visibility = if (position != 0) View.GONE else View.VISIBLE
			}
		}


		private fun getDistanceAndCountedPoints(userLocations: LocationMessages.UserLocations): UITrackData {
			val uiTrackData = UITrackData(0.0f, 0, 0, 0)

			userLocations.getUniqueSegments().forEach {
				if (uiTrackData.minTime == 0L) {
					uiTrackData.minTime = it.minTime
				}
				uiTrackData.dist += it.distance.toFloat()
				uiTrackData.points += it.points.size
				uiTrackData.maxTime = it.maxTime
			}
			return uiTrackData
		}

		override fun getItemCount() = items.size

		inner class BaseViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
			val lastTelegramUpdateTime: TextView? = view.findViewById(R.id.last_telegram_update_time)

			val userRow: View? = view.findViewById(R.id.user_row)
			val distanceAndPointsContainer: View? = view.findViewById(R.id.distance_and_points_container)
			val locationAndDescrContainer: View? = view.findViewById(R.id.location_with_descr_container)
			val distanceImage: ImageView? = view.findViewById(R.id.distance_icon)
			val distanceAndPointsTitle: TextView? = view.findViewById(R.id.distance_and_points_text)
			val groupDescrContainer: View? = view.findViewById(R.id.group_container)
			val groupImage: ImageView? = view.findViewById(R.id.group_icon)
			val groupTitle: TextView? = view.findViewById(R.id.group_title)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val topDivider: View? = view.findViewById(R.id.top_divider)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)
		}
	}

	data class UITrackData(
		var dist: Float,
		var points: Int,
		var minTime: Long,
		var maxTime: Long
	)

	companion object {
		private const val ADAPTER_UPDATE_INTERVAL_MIL = 15 * 1000L // 15 sec
	}
}