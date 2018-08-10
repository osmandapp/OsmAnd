package net.osmand.telegram.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import net.osmand.Location
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramLocationProvider.TelegramCompassListener
import net.osmand.telegram.TelegramLocationProvider.TelegramLocationListener
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.helpers.TelegramUiHelper.ChatItem
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.helpers.TelegramUiHelper.LocationItem
import net.osmand.telegram.ui.LiveNowTabFragment.LiveNowListAdapter.BaseViewHolder
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.UiUtils.UpdateLocationViewCache
import net.osmand.util.MapUtils
import org.drinkless.td.libcore.telegram.TdApi

private const val CHAT_VIEW_TYPE = 0
private const val LOCATION_ITEM_VIEW_TYPE = 1

class LiveNowTabFragment : Fragment(), TelegramListener, TelegramIncomingMessagesListener,
	FullInfoUpdatesListener, TelegramLocationListener, TelegramCompassListener {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val osmandAidlHelper get() = app.osmandAidlHelper
	private val settings get() = app.settings

	private lateinit var adapter: LiveNowListAdapter
	private lateinit var locationViewCache: UpdateLocationViewCache

	private lateinit var openOsmAndBtn: TextView

	private var location: Location? = null
	private var heading: Float? = null
	private var locationUiUpdateAllowed: Boolean = true

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.fragment_live_now_tab, container, false)
		val appBarLayout = mainView.findViewById<View>(R.id.app_bar_layout)
		AndroidUtils.addStatusBarPadding19v(context!!, appBarLayout)
		adapter = LiveNowListAdapter()
		mainView.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@LiveNowTabFragment.adapter
			addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
					super.onScrollStateChanged(recyclerView, newState)
					locationUiUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE
					when (newState) {
						RecyclerView.SCROLL_STATE_DRAGGING -> animateOpenOsmAndBtn(false)
						RecyclerView.SCROLL_STATE_IDLE -> animateOpenOsmAndBtn(true)
					}
				}
			})
		}

		(activity as MainActivity).setupOptionsBtn(mainView.findViewById<ImageView>(R.id.options))

		openOsmAndBtn = mainView.findViewById<TextView>(R.id.open_osmand_btn).apply {
			setOnClickListener {
				activity?.packageManager?.getLaunchIntentForPackage(settings.appToConnectPackage)
					?.also { intent ->
						startActivity(intent)
					}
			}
		}
		return mainView
	}

	override fun onResume() {
		super.onResume()
		locationViewCache = app.uiUtils.getUpdateLocationViewCache()
		updateList()
		telegramHelper.addIncomingMessagesListener(this)
		telegramHelper.addFullInfoUpdatesListener(this)
		startLocationUpdate()
		updateOpenOsmAndIcon()
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.removeIncomingMessagesListener(this)
		telegramHelper.removeFullInfoUpdatesListener(this)
		stopLocationUpdate()
	}

	override fun onTelegramStatusChanged(
		prevTelegramAuthorizationState: TelegramAuthorizationState,
		newTelegramAuthorizationState: TelegramAuthorizationState
	) {
		when (newTelegramAuthorizationState) {
			TelegramAuthorizationState.READY -> {
				updateList()
			}
			TelegramAuthorizationState.LOGGING_OUT,
			TelegramAuthorizationState.CLOSED,
			TelegramAuthorizationState.UNKNOWN -> {
				adapter.items = emptyList()
			}
			else -> Unit
		}
	}

	override fun onTelegramChatsRead() {
		updateList()
	}

	override fun onTelegramChatsChanged() {
		updateList()
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		updateList()
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		updateList()
	}

	override fun onTelegramError(code: Int, message: String) {}

	override fun onSendLiveLocationError(code: Int, message: String) {}

	override fun onReceiveChatLocationMessages(chatId: Long, vararg messages: TdApi.Message) {
		app.runInUIThread { updateList() }
	}

	override fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>) {
		app.runInUIThread { updateList() }
	}

	override fun updateLocationMessages() {}

	override fun onBasicGroupFullInfoUpdated(groupId: Int, info: TdApi.BasicGroupFullInfo) {
		app.runInUIThread { updateList() }
	}

	override fun onSupergroupFullInfoUpdated(groupId: Int, info: TdApi.SupergroupFullInfo) {
		app.runInUIThread { updateList() }
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

	fun tabOpened() {
		startLocationUpdate()
		updateOpenOsmAndIcon()
	}

	fun tabClosed() {
		stopLocationUpdate()
	}

	private fun updateOpenOsmAndIcon() {
		val ic = SettingsDialogFragment.AppConnect.getWhiteIconId(settings.appToConnectPackage)
		openOsmAndBtn.setCompoundDrawablesWithIntrinsicBounds(ic, 0, 0, 0)
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

	private fun updateList() {
		val res = mutableListOf<ListItem>()
		for ((id, messages) in telegramHelper.getMessagesByChatIds()) {
			telegramHelper.getChat(id)?.also { chat ->
				res.add(TelegramUiHelper.chatToChatItem(telegramHelper, chat, messages))
				if (needLocationItems(chat.type)) {
					res.addAll(convertToLocationItems(chat, messages))
				}
			}
		}
		adapter.items = res
	}

	private fun needLocationItems(type: TdApi.ChatType): Boolean {
		return when (type) {
			is TdApi.ChatTypeBasicGroup -> true
			is TdApi.ChatTypeSupergroup -> true
			is TdApi.ChatTypePrivate -> telegramHelper.isOsmAndBot(type.userId)
			else -> false
		}
	}

	private fun convertToLocationItems(
		chat: TdApi.Chat,
		messages: List<TdApi.Message>
	): List<LocationItem> {
		val res = mutableListOf<LocationItem>()
		messages.forEach { message ->
			TelegramUiHelper.messageToLocationItem(telegramHelper, chat, message)?.also {
				res.add(it)
			}
		}
		return res
	}

	private fun showOsmAndMissingDialog() {
		activity?.let {
			MainActivity.OsmandMissingDialogFragment().show(it.supportFragmentManager, null)
		}
	}

	private fun animateOpenOsmAndBtn(show: Boolean) {
		val scale = if (show) 1f else 0f
		openOsmAndBtn.animate()
			.scaleX(scale)
			.scaleY(scale)
			.setDuration(200)
			.setInterpolator(LinearInterpolator())
			.start()
	}

	inner class LiveNowListAdapter : RecyclerView.Adapter<BaseViewHolder>() {

		private val menuList =
			listOf(getString(R.string.shared_string_off), getString(R.string.shared_string_all))

		var items: List<ListItem> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun getItemViewType(position: Int): Int {
			return when (items[position]) {
				is ChatItem -> CHAT_VIEW_TYPE
				else -> LOCATION_ITEM_VIEW_TYPE
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			return when (viewType) {
				CHAT_VIEW_TYPE -> ChatViewHolder(
					inflater.inflate(R.layout.live_now_chat_card, parent, false)
				)
				else -> ContactViewHolder(
					inflater.inflate(R.layout.user_list_item, parent, false)
				)
			}
		}

		override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
			val lastItem = position == itemCount - 1
			val item = items[position]
			val canBeOpenedOnMap = item.canBeOpenedOnMap()
			val openOnMapView = holder.getOpenOnMapClickView()

			TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, item.placeholderId, false)
			holder.title?.text = item.getVisibleName()
			openOnMapView?.isEnabled = canBeOpenedOnMap
			if (canBeOpenedOnMap) {
				openOnMapView?.setOnClickListener {
					if (osmandAidlHelper.isOsmandNotInstalled()) {
						showOsmAndMissingDialog()
					} else {
						app.showLocationHelper.showLocationOnMap(item)
					}
				}
			} else {
				openOnMapView?.setOnClickListener(null)
			}
			if (location != null && item.latLon != null) {
				holder.locationViewContainer?.visibility = View.VISIBLE
				locationViewCache.outdatedLocation = System.currentTimeMillis() / 1000 - item.lastUpdated > settings.staleLocTime
				app.uiUtils.updateLocationView(
					holder.directionIcon,
					holder.distanceText,
					item.latLon,
					locationViewCache
				)
			} else {
				holder.locationViewContainer?.visibility = View.GONE
			}
			holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE

			if (item is ChatItem && holder is ChatViewHolder) {
				val nextIsLocation = !lastItem && items[position + 1] is LocationItem
				val chatId = item.chatId
				val stateTextInd = if (settings.isShowingChatOnMap(chatId)) 1 else 0

				holder.description?.text = getChatItemDescription(item)
				holder.imageButton?.visibility = View.GONE
				holder.showOnMapRow?.setOnClickListener { showPopupMenu(holder, chatId) }
				holder.showOnMapState?.text = menuList[stateTextInd]
				holder.bottomDivider?.visibility = if (nextIsLocation) View.VISIBLE else View.GONE
			} else if (item is LocationItem && holder is ContactViewHolder) {
				holder.description?.visibility = View.GONE
			}
		}

		override fun getItemCount() = items.size

		private fun getChatItemDescription(item: ChatItem): String {
			return when {
				item.chatWithBot -> getString(R.string.shared_string_bot)
				item.privateChat -> "" // FIXME
				else -> {
					val live = getString(R.string.shared_string_live)
					val all = getString(R.string.shared_string_all)
					val liveStr = "$live ${item.liveMembersCount}"
					if (item.membersCount > 0) "$liveStr • $all ${item.membersCount}" else liveStr
				}
			}
		}

		private fun showPopupMenu(holder: ChatViewHolder, chatId: Long) {
			val ctx = holder.itemView.context

			ListPopupWindow(ctx).apply {
				isModal = true
				anchorView = holder.showOnMapState
				setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
				setDropDownGravity(Gravity.END or Gravity.TOP)
				setAdapter(ArrayAdapter(ctx, R.layout.popup_list_text_item, menuList))
				setOnItemClickListener { _, _, position, _ ->
					val allSelected = position == 1

					settings.showChatOnMap(chatId, allSelected)
					if (settings.hasAnyChatToShowOnMap()) {
						if (osmandAidlHelper.isOsmandNotInstalled()) {
							if (allSelected) {
								showOsmAndMissingDialog()
							}
						} else {
							if (allSelected) {
								app.showLocationHelper.showChatMessages(chatId)
							} else {
								app.showLocationHelper.hideChatMessages(chatId)
							}
							app.showLocationHelper.startShowingLocation()
						}
					} else {
						app.showLocationHelper.stopShowingLocation()
						if (!allSelected) {
							app.showLocationHelper.hideChatMessages(chatId)
						}
					}

					holder.showOnMapState?.text = menuList[position]
					dismiss()
				}
				show()
			}
		}

		abstract inner class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val locationViewContainer: View? = view.findViewById(R.id.location_view_container)
			val directionIcon: ImageView? = view.findViewById(R.id.direction_icon)
			val distanceText: TextView? = view.findViewById(R.id.distance_text)
			val description: TextView? = view.findViewById(R.id.description)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)

			abstract fun getOpenOnMapClickView(): View?
		}

		inner class ContactViewHolder(view: View) : BaseViewHolder(view) {
			val mainView: View? = view.findViewById(R.id.main_view)

			override fun getOpenOnMapClickView() = mainView
		}

		inner class ChatViewHolder(view: View) : BaseViewHolder(view) {
			val userRow: View? = view.findViewById(R.id.user_row)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val showOnMapState: TextView? = view.findViewById(R.id.show_on_map_state)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)

			override fun getOpenOnMapClickView() = userRow
		}
	}
}
