package net.osmand.telegram.ui

import android.app.DatePickerDialog
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.ui.TimelineTabFragment.LiveNowListAdapter.BaseViewHolder
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import java.util.*


class TimelineTabFragment : Fragment() {

	private val log = PlatformUtil.getLog(TimelineTabFragment::class.java)

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val settings get() = app.settings

	private lateinit var adapter: LiveNowListAdapter

	private lateinit var dateStartBtn: TextView
	private lateinit var dateEndBtn: TextView
	private lateinit var mainView: View

	private var start = 0L
	private var end = 0L

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		mainView = inflater.inflate(R.layout.fragment_timeline_tab, container, false)
		val appBarLayout = mainView.findViewById<View>(R.id.app_bar_layout)

		start = System.currentTimeMillis()
		end = System.currentTimeMillis()

		AndroidUtils.addStatusBarPadding19v(context!!, appBarLayout)
		adapter = LiveNowListAdapter()
		mainView.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@TimelineTabFragment.adapter
		}

		val switcher = mainView.findViewById<Switch>(R.id.monitoring_switcher)
		val monitoringTv = mainView.findViewById<TextView>(R.id.monitoring_title)
		monitoringTv.setText(if (settings.monitoringEnabled) R.string.monitoring_is_enabled else R.string.monitoring_is_disabled)

		mainView.findViewById<View>(R.id.monitoring_container).setOnClickListener {
			val monitoringEnabled = !settings.monitoringEnabled
			settings.monitoringEnabled = monitoringEnabled
			switcher.isChecked = monitoringEnabled
			monitoringTv.setText(if (monitoringEnabled) R.string.monitoring_is_enabled else R.string.monitoring_is_disabled)
		}

		dateStartBtn = mainView.findViewById<TextView>(R.id.date_start_btn).apply {
			setOnClickListener {
				selectStartDate()
			}
			setCompoundDrawablesWithIntrinsicBounds(getPressedStateIcon(R.drawable.ic_action_date_start), null, null, null)
		}
		dateEndBtn = mainView.findViewById<TextView>(R.id.date_end_btn).apply {
			setOnClickListener {
				selectEndDate()
			}
			setCompoundDrawablesWithIntrinsicBounds(getPressedStateIcon(R.drawable.ic_action_date_add), null, null, null)
		}

		setupBtnTextColor(dateStartBtn)
		setupBtnTextColor(dateEndBtn)

		return mainView
	}

	private fun setupBtnTextColor(textView: TextView) {
		textView.setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))
	}

	private fun selectStartDate() {
		val dateFromDialog =
			DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
				val from = Calendar.getInstance()
				from.set(Calendar.YEAR, year)
				from.set(Calendar.MONTH, monthOfYear)
				from.set(Calendar.DAY_OF_MONTH, dayOfMonth)
				start = from.timeInMillis
				updateList()
				updateDateButtons()
			}
		val startCalendar = Calendar.getInstance()
		startCalendar.timeInMillis = start
		DatePickerDialog(context, dateFromDialog,
			startCalendar.get(Calendar.YEAR),
			startCalendar.get(Calendar.MONTH),
			startCalendar.get(Calendar.DAY_OF_MONTH)
		).show()
	}

	private fun selectEndDate() {
		val dateFromDialog =
			DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
				val from = Calendar.getInstance()
				from.set(Calendar.YEAR, year)
				from.set(Calendar.MONTH, monthOfYear)
				from.set(Calendar.DAY_OF_MONTH, dayOfMonth)
				end = from.timeInMillis
				updateList()
				updateDateButtons()
			}
		val endCalendar = Calendar.getInstance()
		endCalendar.timeInMillis = end
		DatePickerDialog(context, dateFromDialog,
			endCalendar.get(Calendar.YEAR),
			endCalendar.get(Calendar.MONTH),
			endCalendar.get(Calendar.DAY_OF_MONTH)
		).show()
	}

	private fun updateDateButtons() {
		dateStartBtn.text = OsmandFormatter.getFormattedDate(start / 1000)
		dateEndBtn.text = OsmandFormatter.getFormattedDate(end / 1000)
		dateEndBtn.setCompoundDrawablesWithIntrinsicBounds(getPressedStateIcon(R.drawable.ic_action_date_end), null, null, null)
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

	private fun updateList() {
		val res = mutableListOf<ListItem>()
		val s = System.currentTimeMillis()
		log.debug("updateList $start")
		val ignoredUsersIds = ArrayList<Int>()
		val currentUserId = telegramHelper.getCurrentUser()?.id
		if (currentUserId != null) {
			val currentUserGpx = app.savingTracksDbHelper.collectRecordedDataForUser(currentUserId, 0, start, end)
			TelegramUiHelper.gpxToChatItem(telegramHelper, currentUserGpx, true)?.also {
				res.add(it)
			}
			ignoredUsersIds.add(currentUserId)
		}
		val gpxFiles = app.savingTracksDbHelper.collectRecordedDataForUsers(start, end, ignoredUsersIds)
		val e = System.currentTimeMillis()

		gpxFiles.forEach {
			TelegramUiHelper.gpxToChatItem(telegramHelper, it,false)?.also { chatItem ->
				res.add(chatItem)
			}
		}

		adapter.items = sortAdapterItems(res)
		log.debug("updateList $s dif: ${e - s}")
	}

	private fun sortAdapterItems(list: MutableList<ListItem>): MutableList<ListItem> {
		val currentUserId = telegramHelper.getCurrentUser()?.id ?: 0
		list.sortWith(java.util.Comparator { lhs, rhs ->
			when (currentUserId) {
				lhs.userId -> return@Comparator 1
				rhs.userId -> return@Comparator 1
				else -> return@Comparator lhs.name.compareTo(rhs.name)
			}
		})
		return list
	}

	inner class LiveNowListAdapter : RecyclerView.Adapter<BaseViewHolder>() {

		var items: List<ListItem> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			return BaseViewHolder(inflater.inflate(R.layout.live_now_chat_card, parent, false))
		}

		override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
			val lastItem = position == itemCount - 1
			val item = items[position]
			val currentUserId = telegramHelper.getCurrentUser()?.id ?: 0

			TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, R.drawable.img_user_picture_active, false)
			holder.title?.text = item.name
			holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
			holder.lastTelegramUpdateTime?.visibility = View.GONE

			if (item is TelegramUiHelper.GpxChatItem) {
				val gpx = item.gpxFile
				val groupDescrRowVisible = (!item.privateChat || item.chatWithBot) && item.userId != currentUserId
				if (groupDescrRowVisible) {
					holder.groupDescrContainer?.visibility = View.VISIBLE
					holder.groupTitle?.text = item.getVisibleName()
					TelegramUiHelper.setupPhoto(app, holder.groupImage, item.groupPhotoPath, item.placeholderId, false)
				} else {
					holder.groupDescrContainer?.visibility = View.GONE
				}
				holder.userRow?.setOnClickListener {
					if (gpx != null) {
						childFragmentManager.also {
							UserGpxInfoFragment.showInstance(it, gpx, start, end)
						}
					}
				}

				holder.imageButton?.visibility = View.GONE
				holder.showOnMapRow?.visibility = View.GONE
				holder.bottomDivider?.visibility = if (lastItem) View.GONE else View.VISIBLE
				holder.topDivider?.visibility = if (position != 0) View.GONE else View.VISIBLE
			}
		}

		override fun getItemCount() = items.size

		inner class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
			val lastTelegramUpdateTime: TextView? = view.findViewById(R.id.last_telegram_update_time)

			val userRow: View? = view.findViewById(R.id.user_row)
			val groupDescrContainer: View? = view.findViewById(R.id.group_container)
			val groupImage: ImageView? = view.findViewById(R.id.group_icon)
			val groupTitle: TextView? = view.findViewById(R.id.group_title)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val topDivider: View? = view.findViewById(R.id.top_divider)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)
		}
	}
}