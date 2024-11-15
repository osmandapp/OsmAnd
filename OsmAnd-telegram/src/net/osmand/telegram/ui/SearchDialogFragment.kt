package net.osmand.telegram.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramLocationProvider.TelegramCompassListener
import net.osmand.telegram.TelegramLocationProvider.TelegramLocationListener
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.ui.views.EmptyStateRecyclerView
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.UiUtils
import net.osmand.util.MapUtils
import org.drinkless.tdlib.TdApi

class SearchDialogFragment : BaseDialogFragment(), TelegramHelper.TelegramSearchListener,
	TelegramLocationListener, TelegramCompassListener {

	private val log = PlatformUtil.getLog(SearchDialogFragment::class.java)

	private val uiUtils get() = app.uiUtils

	private val adapter = SearchAdapter()

	private lateinit var locationViewCache: UiUtils.UpdateLocationViewCache

	private lateinit var searchEditText: EditText
	private lateinit var buttonsBar: LinearLayout

	private var searchedChatsIds = mutableSetOf<Long>()
	private var searchedPublicChatsIds = mutableSetOf<Long>()
	private var searchedContactsIds = mutableSetOf<Long>()

	private val selectedChats = HashSet<Long>()
	private val selectedUsers = HashSet<Long>()

	private var searchQuery: String = ""

	private var location: Location? = null
	private var heading: Float? = null
	private var locationUiUpdateAllowed: Boolean = true

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		readFromBundle(savedInstanceState ?: arguments)

		val mainView = inflater.inflate(R.layout.fragment_search_dialog, parent)

		mainView.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener { dismiss() }
		}
		val window = dialog?.window
		if (window != null && Build.VERSION.SDK_INT >= 21) {
			window.statusBarColor = ContextCompat.getColor(app, R.color.card_bg_light)
		}
		searchEditText = mainView.findViewById<EditText>(R.id.searchEditText).apply {
			addTextChangedListener(object : TextWatcher {

				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

				override fun afterTextChanged(s: Editable) {
					val newQueryText = s.toString()
					if (!searchQuery.equals(newQueryText, true)) {
						searchQuery = newQueryText
						clearSearchedItems()
						if (searchQuery.isNotBlank()) {
							runSearch()
						} else {
							updateList()
						}
					}
				}
			})
		}
		mainView.findViewById<ImageView>(R.id.search_icon).setOnClickListener {
			runSearch()
		}
		val emptyView = mainView.findViewById<LinearLayout>(R.id.empty_view)
		mainView.findViewById<EmptyStateRecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@SearchDialogFragment.adapter
			setEmptyView(emptyView)
			addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
					super.onScrollStateChanged(recyclerView, newState)
					val scrolling = newState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
					locationUiUpdateAllowed = !scrolling
					if (scrolling) {
						hideKeyboard()
					}
				}
			})
		}

		buttonsBar = mainView.findViewById<LinearLayout>(R.id.buttons_bar).apply {
			findViewById<TextView>(R.id.primary_btn).apply {
				text = getString(R.string.shared_string_continue)
				setOnClickListener {
					onPrimaryBtnClick()
				}
			}
			findViewById<TextView>(R.id.secondary_btn).apply {
				text = getString(R.string.shared_string_cancel)
				setOnClickListener {
					onSecondaryBtnClick()
				}
			}
		}

		return mainView
	}

	private fun hideKeyboard() {
		val mainActivity = activity
		if (mainActivity != null && searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(mainActivity, searchEditText)
		}
	}

	private fun clearSearchedItems() {
		searchedChatsIds.clear()
		searchedPublicChatsIds.clear()
		searchedContactsIds.clear()
	}

	private fun runSearch() {
		if (searchQuery.isNotBlank()) {
			runSearch(searchQuery)
		}
	}

	private fun runSearch(text: String) {
		if (getSavedMessagesChatTitle().startsWith(text, true)) {
			val savedMessages = telegramHelper.getChat(telegramHelper.getCurrentUserId().toLong())
			if (savedMessages != null) {
				telegramHelper.searchChats(savedMessages.title)
			}
		}
		telegramHelper.searchChats(text)
		telegramHelper.searchChatsOnServer(text)
		telegramHelper.searchContacts(text)
		if (text.length > 4 && !getSavedMessagesChatTitle().startsWith(text, true)) {
			telegramHelper.searchPublicChats(text)
		}
	}

	private fun getSavedMessagesChatTitle() = getString(R.string.saved_messages)

	override fun onResume() {
		super.onResume()
		telegramHelper.addSearchListener(this)
		locationViewCache = app.uiUtils.getUpdateLocationViewCache()
		startLocationUpdate()
		searchEditText.requestFocus()
		AndroidUtils.softKeyboardDelayed(searchEditText)
		updateList()
		switchButtonsVisibility(selectedChats.isNotEmpty() || selectedUsers.isNotEmpty())
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.removeSearchListener(this)
		stopLocationUpdate()
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

	private fun updateList() {
		val items: MutableList<TdApi.Object> = mutableListOf()
		val chats: MutableList<TdApi.Chat> = mutableListOf()
		val publicChats: MutableList<TdApi.Chat> = mutableListOf()
		val users: MutableList<TdApi.User> = mutableListOf()
		val currentUserId = telegramHelper.getCurrentUserId()

		selectedChats.forEach {
			val chat = telegramHelper.getChat(it)
			if (chat != null) {
				if (!telegramHelper.isChannel(chat)) {
					items.add(chat)
				}
			} else {
				telegramHelper.requestChat(it)
			}
		}
		selectedUsers.forEach {
			val user = telegramHelper.getUser(it)
			if (user != null) {
				if (user.id != currentUserId)
					items.add(user)
			} else {
				telegramHelper.requestUser(it)
			}
		}
		searchedChatsIds.forEach {
			val chat = telegramHelper.getChat(it)
			if (chat != null && !selectedChats.contains(it)) {
				if (!telegramHelper.isChannel(chat)) {
					chats.add(chat)
				}
			} else {
				telegramHelper.requestChat(it)
			}
		}
		items.addAll(chats)

		searchedContactsIds.forEach { userId ->
			val user = telegramHelper.getUser(userId)
			if (user != null && !selectedUsers.contains(userId.toLong())) {
				if (user.id != currentUserId && !chats.any { telegramHelper.getUserIdFromChatType(it.type) == user.id })
					users.add(user)
			} else {
				telegramHelper.requestUser(userId)
			}
		}
		items.addAll(sortUsers(users))

		searchedPublicChatsIds.forEach {
			val chat = telegramHelper.getChat(it)
			if (chat != null && !selectedChats.contains(it) && !searchedChatsIds.contains(it)) {
				if (!telegramHelper.isChannel(chat) && telegramHelper.getUserIdFromChatType(chat.type) != currentUserId) {
					publicChats.add(chat)
				}
			} else {
				telegramHelper.requestChat(it)
			}
		}
		items.addAll(publicChats)

		adapter.items = items
	}

	private fun sortUsers(list: MutableList<TdApi.User>): MutableList<TdApi.User> {
		list.sortWith(Comparator { o1, o2 ->
			val title1 = TelegramUiHelper.getUserName(o1)
			val title2 = TelegramUiHelper.getUserName(o2)
			title1.compareTo(title2)
		})
		return list
	}

	override fun onSearchContactsFinished(obj: TdApi.Users) {
		log.debug("searchContactsFinished $obj")
		val ids = obj.userIds
		if (ids.isNotEmpty()) {
			searchedContactsIds = ids.toMutableSet()
			app.runInUIThread { updateList() }
		}
	}

	override fun onSearchChatsFinished(obj: TdApi.Chats) {
		log.debug("searchChatsFinished $obj")
		val ids = obj.chatIds
		if (ids.isNotEmpty()) {
			searchedChatsIds = ids.toMutableSet()
			app.runInUIThread { updateList() }
		}
	}

	override fun onSearchPublicChatsFinished(obj: TdApi.Chats) {
		log.debug("onSearchPublicChatsFinished $obj")
		val ids = obj.chatIds
		if (ids.isNotEmpty()) {
			searchedPublicChatsIds = ids.toMutableSet()
			app.runInUIThread { updateList() }
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			LogoutBottomSheet.LOGOUT_REQUEST_CODE -> {
				dismiss()
			}
			SetTimeDialogFragment.LOCATION_SHARED_REQUEST_CODE -> {
				if (resultCode == SetTimeDialogFragment.LOCATION_SHARED_REQUEST_CODE) {
					targetFragment?.also {
						it.onActivityResult(targetRequestCode, resultCode, null)
					}
					dismiss()
				}
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLongArray(SELECTED_CHATS_KEY, selectedChats.toLongArray())
		outState.putLongArray(SELECTED_USERS_KEY, selectedUsers.toLongArray())
	}

	inner class SearchAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<SearchAdapter.ChatViewHolder>() {

		var items = mutableListOf<TdApi.Object>()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.user_list_item, parent, false)
			return ChatViewHolder(view)
		}

		@SuppressLint("SetTextI18n")
		override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
			val item = items[position]
			val isChat = item is TdApi.Chat
			val itemId = if (isChat) {
				(item as TdApi.Chat).id
			} else {
				(item as TdApi.User).id.toLong()
			}
			val latLon = getItemLastLocation(item)
			val lastUpdate = getLastUpdateTime(item)

			val lastItem = position == itemCount - 1
			val placeholderId = if (isChat && telegramHelper.isGroup(item as TdApi.Chat)) R.drawable.img_group_picture else R.drawable.img_user_picture
			val live = (isChat && settings.isSharingLocationToChat(itemId))
			val shareInfo = if (isChat) settings.getChatsShareInfo()[itemId] else null

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
						getSavedMessagesChatTitle()
					} else {
						item.title
					}
				}
				is TdApi.User -> {
					if (item.id == currentUserId) getSavedMessagesChatTitle() else TelegramUiHelper.getUserName(item)
				}
				else -> null
			}

			holder.title?.text = title

			holder.checkBox?.apply {
				visibility = if (live) View.GONE else View.VISIBLE
				setOnCheckedChangeListener(null)
				isChecked = if (isChat) {
					selectedChats.contains(itemId)
				} else {
					selectedUsers.contains(itemId)
				}
				setOnCheckedChangeListener { _, isChecked ->
					if (isChecked) {
						if (isChat) {
							selectedChats.add(itemId)
						} else {
							selectedUsers.add(itemId)
						}
					} else {
						if (isChat) {
							selectedChats.remove(itemId)
						} else {
							selectedUsers.remove(itemId)
						}
						if (!(searchedChatsIds.contains(itemId) || searchedPublicChatsIds.contains(itemId) || searchedContactsIds.contains(itemId))) {
							updateList()
						}
					}
					switchButtonsVisibility(selectedChats.isNotEmpty() || selectedUsers.isNotEmpty())
				}
			}
			holder.topShadowDivider?.visibility = if (position == 0) View.VISIBLE else View.GONE
			holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
			holder.itemView.setOnClickListener {
				if (!live) {
					holder.checkBox?.apply {
						isChecked = !isChecked
					}
				}
			}

			if (location != null && latLon != null && lastUpdate != null) {
				val staleLocation = System.currentTimeMillis() / 1000 - lastUpdate > settings.staleLocTime

				holder.locationViewContainer?.visibility = if (lastUpdate > 0) View.VISIBLE else View.GONE
				locationViewCache.outdatedLocation = staleLocation
				app.uiUtils.updateLocationView(holder.directionIcon, holder.distanceText, latLon, locationViewCache)
			} else {
				holder.locationViewContainer?.visibility = View.GONE
			}

			val expiresIn = shareInfo?.getChatLiveMessageExpireTime() ?: 0
			holder.textInArea?.apply {
				visibility = if (live) View.VISIBLE else View.GONE
				text = OsmandFormatter.getFormattedDuration(app, expiresIn)
			}

			holder.description?.apply {
				val description = getItemDescription(item, lastUpdate)
				text = description
				visibility = if (description != null) View.VISIBLE else View.GONE
			}
		}

		private fun getItemLastMessage(item: TdApi.Object): TdApi.Message? {
			when (item) {
				is TdApi.User -> {
					return telegramHelper.getUserMessage(item)
				}
				is TdApi.Chat -> {
					return telegramHelper.getChatMessages(item.id).firstOrNull() ?: item.lastMessage
				}
			}
			return null
		}

		private fun getItemLastLocation(item: TdApi.Object): LatLon? {
			val message = getItemLastMessage(item)
			if (message != null && OsmandLocationUtils.getSenderMessageId(message) != telegramHelper.getCurrentUserId()) {
				val messageLocation = OsmandLocationUtils.parseMessageContent(message, telegramHelper)
				if (messageLocation != null) {
					return LatLon(messageLocation.lat, messageLocation.lon)
				}
			}
			return null
		}

		private fun getLastUpdateTime(item: TdApi.Object): Int? {
			val message = getItemLastMessage(item)
			if (message != null && OsmandLocationUtils.getSenderMessageId(message) != telegramHelper.getCurrentUserId()) {
				return OsmandLocationUtils.getLastUpdatedTime(message)
			}

			return null
		}

		private fun getItemDescription(item: TdApi.Object, lastUpdateTime: Int?): String? {
			if (lastUpdateTime != null) {
				return OsmandFormatter.getListItemShortLiveTimeDescr(app, lastUpdateTime, R.string.duration_ago)
			}
			if (item is TdApi.Chat && telegramHelper.isGroup(item)) {
				return getString(R.string.shared_string_group)
			}

			return null
		}

		override fun getItemCount() = items.size

		inner class ChatViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val locationViewContainer: View? = view.findViewById(R.id.location_view_container)
			val directionIcon: ImageView? = view.findViewById(R.id.direction_icon)
			val distanceText: TextView? = view.findViewById(R.id.distance_text)
			val description: TextView? = view.findViewById(R.id.description)
			val checkBox: CheckBox? = view.findViewById(R.id.check_box)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
			val topShadowDivider: View? = view.findViewById(R.id.top_divider)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}
	}

	private fun readFromBundle(bundle: Bundle?) {
		selectedChats.clear()
		selectedUsers.clear()
		bundle?.getLongArray(SELECTED_CHATS_KEY)?.also {
			selectedChats.addAll(it.toHashSet())
		}
		bundle?.getLongArray(SELECTED_USERS_KEY)?.also {
			selectedUsers.addAll(it.toHashSet())
		}
	}

	private fun onPrimaryBtnClick() {
		if (selectedChats.isNotEmpty() || selectedUsers.isNotEmpty()) {
			fragmentManager?.also {
				SetTimeDialogFragment.showInstance(it, selectedChats, selectedUsers, this)
			}
		}
	}

	private fun onSecondaryBtnClick() {
		clearSelection()
		updateList()
		switchButtonsVisibility(false)
		targetFragment?.also {
			it.onActivityResult(targetRequestCode, CLEAR_SELECTED_ITEMS_REQUEST_CODE, null)
		}
	}

	private fun clearSelection() {
		selectedChats.clear()
		selectedUsers.clear()
	}

	private fun switchButtonsVisibility(visible: Boolean) {
		val buttonsVisibility = if (visible) View.VISIBLE else View.GONE
		if (buttonsBar.visibility != buttonsVisibility) {
			buttonsBar.visibility = buttonsVisibility
		}
	}

	companion object {

		const val TAG = "SearchDialogFragment"
		private const val SELECTED_CHATS_KEY = "selected_chats_key"
		private const val SELECTED_USERS_KEY = "selected_users_key"
		const val SEARCH_ITEMS_REQUEST_CODE = 3
		const val CLEAR_SELECTED_ITEMS_REQUEST_CODE = 4

		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment?, selectedChats: Set<Long>, selectedUsers: Set<Long>): Boolean {
			return try {
				SearchDialogFragment().apply {
					arguments = Bundle().apply {
						if (selectedChats.isNotEmpty()) {
							putLongArray(SELECTED_CHATS_KEY, selectedChats.toLongArray())
						}
						if (selectedUsers.isNotEmpty()) {
							putLongArray(SELECTED_USERS_KEY, selectedUsers.toLongArray())
						}
					}
					if (target != null) {
						setTargetFragment(target, SEARCH_ITEMS_REQUEST_CODE)
					}
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}