package net.osmand.telegram.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramLocationProvider.TelegramCompassListener
import net.osmand.telegram.TelegramLocationProvider.TelegramLocationListener
import net.osmand.telegram.helpers.ShareLocationHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.ui.SetTimeDialogFragment.SetTimeListAdapter.ChatViewHolder
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.UiUtils
import net.osmand.util.MapUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*
import java.util.concurrent.TimeUnit

class SetTimeDialogFragment : BaseDialogFragment(), TelegramLocationListener, TelegramCompassListener {

	private lateinit var locationViewCache: UiUtils.UpdateLocationViewCache
	private val adapter = SetTimeListAdapter()

	private lateinit var timeForAllTitle: TextView
	private lateinit var timeForAllValue: TextView

	private val chatLivePeriods = HashMap<Long, Long>()
	private val userLivePeriods = HashMap<Long, Long>()

	private var location: Location? = null
	private var heading: Float? = null
	private var locationUiUpdateAllowed: Boolean = true

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		readFromBundle(savedInstanceState ?: arguments)

		val view = inflater.inflate(R.layout.fragment_set_time_dialog, container)

		view.findViewById<View>(R.id.time_for_all_row).apply {
			findViewById<ImageView>(R.id.time_for_all_icon).setImageDrawable(
				app.uiUtils.getIcon(R.drawable.ic_action_time_span, R.color.ctrl_active_light)
			)
			timeForAllTitle = findViewById(R.id.time_for_all_title)
			timeForAllValue = findViewById(R.id.time_for_all_value)
			setOnClickListener {
				selectDuration()
			}
		}

		updateTimeForAllRow()

		view.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@SetTimeDialogFragment.adapter
			addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
					super.onScrollStateChanged(recyclerView, newState)
					locationUiUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE
				}
			})
		}

		view.findViewById<TextView>(R.id.secondary_btn).apply {
			text = getString(R.string.shared_string_back)
			setOnClickListener {
				targetFragment?.also {
					it.onActivityResult(targetRequestCode, LOCATION_SHARING_CANCELED_CODE, null)
				}
				dismiss()
			}
		}

		view.findViewById<TextView>(R.id.primary_btn).apply {
			text = getString(R.string.shared_string_share)
			setOnClickListener {
				if (!AndroidUtils.isLocationPermissionAvailable(view.context)) {
					AndroidUtils.requestLocationPermission(activity!!)
				} else {
					chatLivePeriods.forEach { (chatId, livePeriod) ->
						settings.shareLocationToChat(chatId, true, livePeriod)
					}
					userLivePeriods.forEach { (userId, livePeriod) ->
						settings.shareLocationToUser(userId.toInt(),  livePeriod)
					}
					app.shareLocationHelper.startSharingLocation()
					targetFragment?.also {
						it.onActivityResult(targetRequestCode, LOCATION_SHARED_REQUEST_CODE, null)
					}
					dismiss()
				}
			}
		}

		return view
	}

	override fun onResume() {
		super.onResume()
		locationViewCache = app.uiUtils.getUpdateLocationViewCache()
		startLocationUpdate()
		updateList()
	}

	override fun onPause() {
		super.onPause()
		stopLocationUpdate()
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val chats = mutableListOf<Long>()
		for ((id, livePeriod) in chatLivePeriods) {
			chats.add(id)
			chats.add(livePeriod)
		}
		val users = mutableListOf<Long>()
		for ((id, livePeriod) in userLivePeriods) {
			users.add(id)
			users.add(livePeriod)
		}
		outState.putLongArray(CHATS_KEY, chats.toLongArray())
		outState.putLongArray(USERS_KEY, users.toLongArray())
	}

	override fun updateLocation(location: Location?) {
		val loc = this.location
		val newLocation = loc == null && location != null
		val locationChanged = loc != null && location != null
				&& loc.latitude != location.latitude
				&& loc.longitude != location.longitude
		if (newLocation || locationChanged) {
			this.location = location
			updateLocationUi()
		}
	}

	override fun updateCompassValue(value: Float) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		val lastHeading = heading ?: 99f
		heading = value
		if (Math.abs(MapUtils.degreesDiff(lastHeading.toDouble(), value.toDouble())) > 5) {
			updateLocationUi()
		} else {
			heading = lastHeading
		}
	}

	private fun startLocationUpdate() {
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		updateLocationUi()
	}

	private fun stopLocationUpdate() {
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
	}

	private fun updateLocationUi() {
		if (locationUiUpdateAllowed) {
			app.runInUIThread { adapter.notifyDataSetChanged() }
		}
	}
	
	private fun readFromBundle(bundle: Bundle?) {
		chatLivePeriods.clear()
		userLivePeriods.clear()
		bundle?.getLongArray(CHATS_KEY)?.also {
			for (i in 0 until it.size step 2) {
				val livePeriod = settings.getChatLivePeriod(it[i])
				chatLivePeriods[it[i]] = livePeriod ?: it[i + 1]
			}
		}
		bundle?.getLongArray(USERS_KEY)?.also {
			for (j in 0 until it.size step 2) {
				val livePeriod = settings.getChatLivePeriod(it[j])
				userLivePeriods[it[j]] = livePeriod ?: it[j + 1]
			}
		}
	}

	private fun getTimeForAll(useDefValue: Boolean = false): Long {
		val returnVal = if (useDefValue) DEFAULT_VISIBLE_TIME_SECONDS else NO_VALUE
		val allTime = mutableListOf<Long>()
		allTime.addAll(chatLivePeriods.values)
		allTime.addAll(userLivePeriods.values)
		val iterator = allTime.iterator()
		if (!iterator.hasNext()) {
			return returnVal
		}
		val first = iterator.next()
		while (iterator.hasNext()) {
			if (first != iterator.next()) {
				return returnVal
			}
		}
		return first
	}

	private fun updateTimeForAllRow() {
		val timeForAll = getTimeForAll()
		if (timeForAll != NO_VALUE) {
			timeForAllTitle.text = getString(R.string.visible_time_for_all)
			timeForAllValue.visibility = View.VISIBLE
			timeForAllValue.text = formatLivePeriod(timeForAll)
		} else {
			timeForAllTitle.text = getString(R.string.set_visible_time_for_all)
			timeForAllValue.visibility = View.GONE
		}
	}

	private fun selectDuration(id: Long? = null, isChat: Boolean = true) {
		val timeForAll = getTimeForAll(true)
		val defSeconds = if (id == null) timeForAll else chatLivePeriods[id] ?: timeForAll
		val (defHours, defMinutes) = secondsToHoursAndMinutes(defSeconds)
		TimePickerDialog(
			context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				val seconds = TimeUnit.HOURS.toSeconds(hours.toLong()) +
						TimeUnit.MINUTES.toSeconds(minutes.toLong())
				if (seconds >= ShareLocationHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC) {
					if (id != null) {
						if (isChat) {
							chatLivePeriods[id] = seconds
						} else {
							userLivePeriods[id] = seconds
						}
					} else {
						chatLivePeriods.keys.forEach {
							chatLivePeriods[it] = seconds
						}
						userLivePeriods.keys.forEach {
							userLivePeriods[it] = seconds
						}
					}
					updateTimeForAllRow()
					adapter.notifyDataSetChanged()
				}
			}, defHours, defMinutes, true
		).show()
	}

	private fun secondsToHoursAndMinutes(seconds: Long): Pair<Int, Int> {
		val hours = TimeUnit.SECONDS.toHours(seconds)
		val minutes = TimeUnit.SECONDS.toMinutes(seconds - TimeUnit.HOURS.toSeconds(hours))
		return Pair(hours.toInt(), minutes.toInt())
	}

	private fun formatLivePeriod(seconds: Long): String {
		val (hours, minutes) = secondsToHoursAndMinutes(seconds)
		return when {
			hours != 0 && minutes == 0 -> getString(R.string.hours_format, hours)
			hours == 0 && minutes != 0 -> getString(R.string.minutes_format, minutes)
			else -> getString(R.string.hours_and_minutes_format, hours, minutes)
		}
	}

	private fun updateList() {
		val items: MutableList<TdApi.Object> = mutableListOf()
		chatLivePeriods.keys.forEach {
			val chat = telegramHelper.getChat(it)
			if (chat != null) {
				items.add(chat)
			}
		}
		userLivePeriods.keys.forEach {
			val user = telegramHelper.getUser(it.toInt())
			if (user != null) {
				items.add(user)
			}
		}
		adapter.items = items
	}

	inner class SetTimeListAdapter : RecyclerView.Adapter<ChatViewHolder>() {

		var items: List<TdApi.Object> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.user_list_item, parent, false)
			return ChatViewHolder(view)
		}

		override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
			val item = items[position]
			val isChat = item is TdApi.Chat
			val itemId = if (isChat) {
				(item as TdApi.Chat).id
			} else {
				(item as TdApi.User).id.toLong()
			}

			val placeholderId = if (isChat && telegramHelper.isGroup((item as TdApi.Chat))) R.drawable.img_group_picture else R.drawable.img_user_picture

			val photoPath = when (item) {
				is TdApi.Chat -> item.photo?.small?.local?.path
				is TdApi.User -> item.profilePhoto?.small?.local?.path
				else -> null
			}

			TelegramUiHelper.setupPhoto(app, holder.icon, photoPath, placeholderId, false)

			val currentUserId = telegramHelper.getCurrentUserId()
			val title = when (item) {
				is TdApi.Chat -> {
					if (telegramHelper.isPrivateChat(item) && (item.type as TdApi.ChatTypePrivate).userId == currentUserId) {
						getString(R.string.saved_messages)
					} else {
						item.title
					}
				}
				is TdApi.User -> {
					if (item.id == currentUserId) getString(R.string.saved_messages) else TelegramUiHelper.getUserName(item)
				}
				else -> null
			}

			holder.title?.text = title

			if (isChat && telegramHelper.isGroup((item as TdApi.Chat))) {
				holder.locationViewContainer?.visibility = View.GONE
				holder.description?.visibility = View.VISIBLE
				holder.description?.text = getString(R.string.shared_string_group)
			} else {
				val message = telegramHelper.getChatMessages(itemId).firstOrNull()
				val content = message?.content
				if (message != null && content is TdApi.MessageLocation && (location != null && content.location != null)) {
					val lastUpdated = OsmandLocationUtils.getLastUpdatedTime(message)
					holder.description?.visibility = View.VISIBLE
					holder.description?.text = OsmandFormatter.getListItemLiveTimeDescr(app, lastUpdated)

					holder.locationViewContainer?.visibility = if (lastUpdated > 0) View.VISIBLE else View.GONE
					locationViewCache.outdatedLocation = System.currentTimeMillis() / 1000 -
							lastUpdated > settings.staleLocTime

					app.uiUtils.updateLocationView(
						holder.directionIcon,
						holder.distanceText,
						LatLon(content.location.latitude, content.location.longitude),
						locationViewCache
					)
				} else {
					holder.locationViewContainer?.visibility = View.GONE
					holder.description?.visibility = View.INVISIBLE
				}
			}

			holder.textInArea?.apply {
				visibility = View.VISIBLE
				if (isChat) {
					chatLivePeriods[itemId]?.also { text = formatLivePeriod(it) }
				} else {
					userLivePeriods[itemId]?.also { text = formatLivePeriod(it) }
				}
			}
			holder.topShadowDivider?.visibility = View.GONE
			holder.bottomShadow?.visibility = View.GONE
			holder.itemView.setOnClickListener {
				selectDuration(itemId, isChat)
			}
		}

		override fun getItemCount() = items.size

		inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val directionIcon: ImageView? = view.findViewById(R.id.direction_icon)
			val distanceText: TextView? = view.findViewById(R.id.distance_text)
			val locationViewContainer: View? = view.findViewById(R.id.location_view_container)
			val description: TextView? = view.findViewById(R.id.description)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
			val topShadowDivider: View? = view.findViewById(R.id.top_divider)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}
	}

	companion object {

		const val LOCATION_SHARED_REQUEST_CODE = 0
		const val LOCATION_SHARING_CANCELED_CODE = 1

		private const val TAG = "SetTimeDialogFragment"
		private const val CHATS_KEY = "chats_key"
		private const val USERS_KEY = "users_key"
		private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour
		private const val NO_VALUE = -1L

		fun showInstance(fm: FragmentManager, chatIds: Set<Long>, usersIds: Set<Long>, target: Fragment): Boolean {
			return try {
				val chats = mutableListOf<Long>()
				for (id in chatIds) {
					chats.add(id)
					chats.add(DEFAULT_VISIBLE_TIME_SECONDS)
				}
				val users = mutableListOf<Long>()
				for (id in usersIds) {
					users.add(id)
					users.add(DEFAULT_VISIBLE_TIME_SECONDS)
				}
				SetTimeDialogFragment().apply {
					arguments = Bundle().apply {
						if (chats.isNotEmpty()) {
							putLongArray(CHATS_KEY, chats.toLongArray())
						}
						if (users.isNotEmpty()) {
							putLongArray(USERS_KEY, users.toLongArray())
						}
					}
					setTargetFragment(target, LOCATION_SHARED_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
