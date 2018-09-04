package net.osmand.telegram.ui

import android.content.Intent
import android.graphics.drawable.Drawable
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
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramLocationProvider.TelegramCompassListener
import net.osmand.telegram.TelegramLocationProvider.TelegramLocationListener
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.helpers.TelegramUiHelper.ChatItem
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.helpers.TelegramUiHelper.LocationItem
import net.osmand.telegram.ui.LiveNowTabFragment.LiveNowListAdapter.BaseViewHolder
import net.osmand.telegram.ui.SortByBottomSheet.Companion.CURRENT_SORT_TYPE_KEY
import net.osmand.telegram.ui.SortByBottomSheet.Companion.SORT_BY_KEY
import net.osmand.telegram.ui.SortByBottomSheet.SortType.*
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
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
	private lateinit var sortByBtn: TextView

	private var location: Location? = null
	private var heading: Float? = null
	private var locationUiUpdateAllowed: Boolean = true

	private var sortBy = SORT_BY_GROUP

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		savedInstanceState?.getString(CURRENT_SORT_TYPE_KEY)?.also {
			sortBy = valueOf(it)
		}
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

		sortByBtn = mainView.findViewById<TextView>(R.id.sort_button)
		updateSortBtn()

		mainView.findViewById<LinearLayout>(R.id.sort_by_container).apply {
			setOnClickListener {
				fragmentManager?.also { fm ->
					SortByBottomSheet.showInstance(fm, this@LiveNowTabFragment, sortBy)
				}
			}
		}

		openOsmAndBtn = mainView.findViewById<TextView>(R.id.open_osmand_btn).apply {
			setOnClickListener {
				val pack = settings.appToConnectPackage
				if (AndroidUtils.isAppInstalled(context, pack)) {
					activity?.packageManager?.getLaunchIntentForPackage(pack)?.also { intent ->
						startActivity(intent)
					}
				} else {
					chooseOsmAnd()
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

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			ChooseOsmAndBottomSheet.OSMAND_CHOSEN_REQUEST_CODE -> updateOpenOsmAndIcon()
			SortByBottomSheet.SORT_BY_REQUEST_CODE -> {
				if (data != null && data.extras != null) {
					val newSortBy = data.extras.getString(SORT_BY_KEY, "")
					if (!newSortBy.isNullOrEmpty()) {
						sortBy = valueOf(newSortBy)
						updateSortBtn()
						updateList()
					}
				}
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(CURRENT_SORT_TYPE_KEY, sortBy.toString())
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

	private fun chooseOsmAnd() {
		val ctx = context ?: return
		val installedApps = TelegramSettings.AppConnect.getInstalledApps(ctx)
		when {
			installedApps.isEmpty() -> showOsmAndMissingDialog()
			installedApps.size == 1 -> {
				settings.updateAppToConnect(installedApps.first().appPackage)
				updateOpenOsmAndIcon()
			}
			installedApps.size > 1 -> {
				fragmentManager?.also { ChooseOsmAndBottomSheet.showInstance(it, this) }
			}
		}
	}

	private fun updateOpenOsmAndIcon() {
		val ic = TelegramSettings.AppConnect.getWhiteIconId(settings.appToConnectPackage)
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
			app.runInUIThread { updateList() }
		}
	}

	private fun updateList() {
		val res = mutableListOf<ListItem>()
		for ((id, messages) in telegramHelper.getMessagesByChatIds(settings.locHistoryTime)) {
			telegramHelper.getChat(id)?.also { chat ->
				if (sortBy.isSortByGroup()) {
					res.add(TelegramUiHelper.chatToChatItem(telegramHelper, chat, messages))
				}
				val type = chat.type
				if (type is TdApi.ChatTypeBasicGroup || type is TdApi.ChatTypeSupergroup) {
					res.addAll(convertToListItems(chat, messages))
				} else if (type is TdApi.ChatTypePrivate) {
					when {
						telegramHelper.isOsmAndBot(type.userId) -> res.addAll(convertToListItems(chat, messages))
						messages.firstOrNull { it.viaBotUserId != 0 } != null -> res.addAll(convertToListItems(chat, messages, true))
						!sortBy.isSortByGroup() -> res.add(TelegramUiHelper.chatToChatItem(telegramHelper, chat, messages))
					}
				}
			}
		}
		sortAdapterItems(res)

		adapter.items = res
	}

	private fun sortAdapterItems(list: MutableList<ListItem>): MutableList<ListItem> {
		if (sortBy == SORT_BY_DISTANCE) {
			list.sortWith(java.util.Comparator<ListItem> { lhs, rhs ->
				if (location == null) {
					return@Comparator 0
				}
				val loc = LatLon(location!!.latitude, location!!.longitude)
				val ld = MapUtils.getDistance(loc, lhs.latLon!!.latitude, lhs.latLon!!.longitude)
				val rd = MapUtils.getDistance(loc, rhs.latLon!!.latitude, rhs.latLon!!.longitude)
				java.lang.Double.compare(ld, rd)
			})
		} else if (sortBy == SORT_BY_NAME) {
			list.sortWith(Comparator<ListItem> { o1, o2 -> o1.name.compareTo(o2.name) })
		}
		return list
	}

	private fun convertToListItems(
		chat: TdApi.Chat,
		messages: List<TdApi.Message>,
		addOnlyViaBotMessages: Boolean = false
	): List<ListItem> {
		return if (sortBy.isSortByGroup()) {
			convertToLocationItems(chat, messages, addOnlyViaBotMessages)
		} else {
			convertToChatItems(chat, messages, addOnlyViaBotMessages)
		}
	}

	private fun convertToLocationItems(
		chat: TdApi.Chat,
		messages: List<TdApi.Message>,
		addOnlyViaBotMessages: Boolean = false
	): List<LocationItem> {
		val res = mutableListOf<LocationItem>()
		messages.forEach { message ->
			if (!addOnlyViaBotMessages || message.viaBotUserId != 0) {
				TelegramUiHelper.messageToLocationItem(telegramHelper, chat, message)?.also {
					res.add(it)
				}
			}
		}
		return res
	}

	private fun convertToChatItems(
		chat: TdApi.Chat,
		messages: List<TdApi.Message>,
		addOnlyViaBotMessages: Boolean = false
	): List<ChatItem> {
		val res = mutableListOf<ChatItem>()
		messages.forEach { message ->
			if (!addOnlyViaBotMessages || message.viaBotUserId != 0) {
				TelegramUiHelper.messageToChatItem(telegramHelper, chat, message).also {
					if (it != null) {
						res.add(it)
					}
				}
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

	private fun isOsmAndInstalled(): Boolean {
		val ctx = context ?: return false
		return AndroidUtils.isAppInstalled(ctx, settings.appToConnectPackage)
	}

	private fun updateSortBtn() {
		var text = ""
		var icon: Drawable? = null
		when (sortBy) {
			SortByBottomSheet.SortType.SORT_BY_NAME -> {
				text = getString(R.string.by_name)
				icon = app.uiUtils.getActiveIcon(sortBy.iconId)
			}
			SortByBottomSheet.SortType.SORT_BY_DISTANCE -> {
				text = getString(R.string.by_distance)
				icon = app.uiUtils.getActiveIcon(sortBy.iconId)
			}
			SortByBottomSheet.SortType.SORT_BY_GROUP -> {
				text = getString(R.string.by_group)
				icon = app.uiUtils.getActiveIcon(sortBy.iconId)
			}
		}
		sortByBtn.text = text
		sortByBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
	}

	inner class LiveNowListAdapter : RecyclerView.Adapter<BaseViewHolder>() {

		private var lastResponseStr = getString(R.string.last_response) + ": "

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

			val staleLocation = System.currentTimeMillis() / 1000 - item.lastUpdated > settings.staleLocTime
			if (staleLocation && item.userId != 0) {
				TelegramUiHelper.setupPhoto(app, holder.icon, item.grayscalePhotoPath, item.placeholderId, false)
			} else {
				TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, R.drawable.img_user_picture_active, false)
			}

			holder.title?.text = if (sortBy.isSortByGroup()) item.getVisibleName() else item.name
			openOnMapView?.isEnabled = canBeOpenedOnMap
			if (canBeOpenedOnMap) {
				openOnMapView?.setOnClickListener {
					if (!isOsmAndInstalled()) {
						showOsmAndMissingDialog()
					} else {
						app.showLocationHelper.showLocationOnMap(item, staleLocation)
					}
				}
			} else {
				openOnMapView?.setOnClickListener(null)
			}
			if (location != null && item.latLon != null) {
				holder.locationViewContainer?.visibility = if (item.lastUpdated > 0) View.VISIBLE else View.GONE
				locationViewCache.outdatedLocation = staleLocation
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
				val nextIsLocation = !lastItem && (items[position + 1] is LocationItem || !sortBy.isSortByGroup())
				val chatId = item.chatId
				val stateTextInd = if (settings.isShowingChatOnMap(chatId)) 1 else 0
				val groupDescrRowVisible = !sortBy.isSortByGroup() && (!item.privateChat || item.chatWithBot)

				if (groupDescrRowVisible) {
					holder.groupDescrContainer?.visibility = View.VISIBLE
					holder.groupTitle?.text = item.getVisibleName()
					TelegramUiHelper.setupPhoto(app, holder.groupImage, item.groupPhotoPath, item.placeholderId, false)
				} else {
					holder.groupDescrContainer?.visibility = View.GONE
				}

				holder.description?.text = getChatItemDescription(item)
				holder.imageButton?.visibility = View.GONE
				holder.showOnMapRow?.setOnClickListener { showPopupMenu(holder, chatId) }
				holder.showOnMapState?.text = menuList[stateTextInd]
				holder.bottomDivider?.visibility = if (nextIsLocation) View.VISIBLE else View.GONE
				holder.topDivider?.visibility = if (!sortBy.isSortByGroup() && position != 0) View.GONE else View.VISIBLE
			} else if (item is LocationItem && holder is ContactViewHolder) {
				holder.description?.text =  OsmandFormatter.getListItemLiveTimeDescr(app, item.lastUpdated, lastResponseStr)
			}
		}

		override fun getItemCount() = items.size

		private fun getChatItemDescription(item: ChatItem): String {
			return when {
				item.chatWithBot -> getString(R.string.shared_string_bot)
				item.privateChat -> OsmandFormatter.getListItemLiveTimeDescr(app, item.lastUpdated, lastResponseStr)
				else -> {
					if (sortBy.isSortByGroup()) {
						val live = getString(R.string.shared_string_live)
						val all = getString(R.string.shared_string_all)
						val liveStr = "$live ${item.liveMembersCount}"
						if (item.membersCount > 0) "$liveStr • $all ${item.membersCount}" else liveStr
					} else {
						OsmandFormatter.getListItemLiveTimeDescr(app, item.lastUpdated, lastResponseStr)
					}
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
						if (!isOsmAndInstalled()) {
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
			val groupDescrContainer: View? = view.findViewById(R.id.group_container)
			val groupImage: ImageView? = view.findViewById(R.id.group_icon)
			val groupTitle: TextView? = view.findViewById(R.id.group_title)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val showOnMapState: TextView? = view.findViewById(R.id.show_on_map_state)
			val topDivider: View? = view.findViewById(R.id.top_divider)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)

			override fun getOpenOnMapClickView() = userRow
		}
	}
}
