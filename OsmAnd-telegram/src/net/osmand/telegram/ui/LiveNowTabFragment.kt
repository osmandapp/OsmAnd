package net.osmand.telegram.ui

import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import net.osmand.telegram.helpers.TelegramUiHelper.LocationItem
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.UiUtils.UpdateLocationViewCache
import net.osmand.util.MapUtils
import org.drinkless.td.libcore.telegram.TdApi

private const val CHAT_VIEW_TYPE = 0
private const val LOCATION_ITEM_VIEW_TYPE = 1

class LiveNowTabFragment : Fragment(), TelegramListener, TelegramIncomingMessagesListener,
	TelegramLocationListener, TelegramCompassListener {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val osmandHelper get() = app.osmandHelper
	private val settings get() = app.settings

	private lateinit var adapter: LiveNowListAdapter
	private lateinit var locationViewCache: UpdateLocationViewCache

	private var location: Location? = null
	private var heading: Float? = null
	private var locationUiUpdateAllowed: Boolean = true

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.fragment_live_now_tab, container, false)
		adapter = LiveNowListAdapter()
		mainView.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@LiveNowTabFragment.adapter
			addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
					super.onScrollStateChanged(recyclerView, newState)
					locationUiUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE
				}
			})
		}
		return mainView
	}

	override fun onResume() {
		super.onResume()
		locationViewCache = app.uiUtils.getUpdateLocationViewCache()
		updateList()
		telegramHelper.addIncomingMessagesListener(this)
		startLocationUpdate()
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.removeIncomingMessagesListener(this)
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

	override fun onReceiveChatLocationMessages(chatTitle: String, vararg messages: TdApi.Message) {
		app.runInUIThread { updateList() }
	}

	override fun updateLocationMessages() {}

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

	fun startLocationUpdate() {
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		updateLocationUi()
	}

	fun stopLocationUpdate() {
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
	}

	private fun updateLocationUi() {
		if (locationUiUpdateAllowed) {
			app.runInUIThread { adapter.notifyDataSetChanged() }
		}
	}

	private fun updateList() {
		val res = mutableListOf<Any>()
		for ((id, messages) in telegramHelper.getMessagesByChatIds()) {
			telegramHelper.getChat(id)?.also { chat ->
				res.add(TelegramUiHelper.chatToChatItem(telegramHelper, chat, messages))
				if (needLocationItems(chat.type)) {
					res.addAll(convertToLocationItems(messages))
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

	private fun convertToLocationItems(messages: List<TdApi.Message>): List<LocationItem> {
		val res = mutableListOf<LocationItem>()
		messages.forEach { message ->
			TelegramUiHelper.messageToLocationItem(telegramHelper, message)?.also { res.add(it) }
		}
		return res
	}

	inner class LiveNowListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

		private val menuList =
			listOf(getString(R.string.shared_string_off), getString(R.string.shared_string_all))

		var items: List<Any> = emptyList()
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

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val lastItem = position == itemCount - 1
			val item = items[position]
			if (item is ChatItem && holder is ChatViewHolder) {
				val nextIsLocation = !lastItem && items[position + 1] is LocationItem
				val chatTitle = item.title
				val stateTextInd = if (settings.isShowingChatOnMap(chatTitle)) 1 else 0

				TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, item.placeholderId)
				holder.title?.text = chatTitle
				if (location != null && item.latLon != null) {
					holder.locationViewContainer?.visibility = View.VISIBLE
					// TODO: locationViewCache.outdatedLocation
					app.uiUtils.updateLocationView(
						holder.directionIcon,
						holder.distanceText,
						item.latLon,
						locationViewCache
					)
				} else {
					holder.locationViewContainer?.visibility = View.GONE
				}
				holder.description?.text = "Chat description" // FIXME
				holder.imageButton?.visibility = View.GONE
				holder.showOnMapRow?.setOnClickListener { showPopupMenu(holder, chatTitle) }
				holder.showOnMapState?.text = menuList[stateTextInd]
				holder.bottomDivider?.visibility = if (nextIsLocation) View.VISIBLE else View.GONE
				holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
			} else if (item is LocationItem && holder is ContactViewHolder) {
				TelegramUiHelper.setupPhoto(app, holder.icon, item.photoPath, item.placeholderId)
				holder.title?.text = item.name
				if (location != null && item.latLon != null) {
					holder.locationViewContainer?.visibility = View.VISIBLE
					// TODO: locationViewCache.outdatedLocation
					app.uiUtils.updateLocationView(
						holder.directionIcon,
						holder.distanceText,
						item.latLon,
						locationViewCache
					)
				} else {
					holder.locationViewContainer?.visibility = View.GONE
				}
				holder.description?.text = "Some description"
				holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
			}
		}

		override fun getItemCount() = items.size

		private fun showPopupMenu(holder: ChatViewHolder, chatTitle: String) {
			val ctx = holder.itemView.context

			val paint = Paint()
			paint.textSize =
					resources.getDimensionPixelSize(R.dimen.list_item_title_text_size).toFloat()
			val textWidth = Math.max(paint.measureText(menuList[0]), paint.measureText(menuList[1]))
			val itemWidth = textWidth.toInt() + AndroidUtils.dpToPx(ctx, 32F)
			val minWidth = AndroidUtils.dpToPx(ctx, 100F)

			ListPopupWindow(ctx).apply {
				isModal = true
				anchorView = holder.showOnMapState
				setContentWidth(Math.max(minWidth, itemWidth))
				setDropDownGravity(Gravity.END or Gravity.TOP)
				setAdapter(ArrayAdapter(ctx, R.layout.popup_list_text_item, menuList))
				setOnItemClickListener { _, _, position, _ ->
					val allSelected = position == 1

					settings.showChatOnMap(chatTitle, allSelected)
					if (settings.hasAnyChatToShowOnMap()) {
						if (osmandHelper.isOsmandNotInstalled()) {
							if (allSelected) {
								activity?.let {
									MainActivity.OsmandMissingDialogFragment()
										.show(it.supportFragmentManager, null)
								}
							}
						} else {
							if (allSelected) {
								app.showLocationHelper.showChatMessages(chatTitle)
							} else {
								app.showLocationHelper.hideChatMessages(chatTitle)
							}
							app.showLocationHelper.startShowingLocation()
						}
					} else {
						app.showLocationHelper.stopShowingLocation()
						if (!allSelected) {
							app.showLocationHelper.hideChatMessages(chatTitle)
						}
					}

					holder.showOnMapState?.text = menuList[position]
					dismiss()
				}
				show()
			}
		}

		inner class ContactViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val locationViewContainer: View? = view.findViewById(R.id.location_view_container)
			val directionIcon: ImageView? = view.findViewById(R.id.direction_icon)
			val distanceText: TextView? = view.findViewById(R.id.distance_text)
			val description: TextView? = view.findViewById(R.id.description)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}

		inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val locationViewContainer: View? = view.findViewById(R.id.location_view_container)
			val directionIcon: ImageView? = view.findViewById(R.id.direction_icon)
			val distanceText: TextView? = view.findViewById(R.id.distance_text)
			val description: TextView? = view.findViewById(R.id.description)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val showOnMapState: TextView? = view.findViewById(R.id.show_on_map_state)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}
	}
}
