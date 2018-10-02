package net.osmand.telegram.ui

import android.animation.*
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.TelegramListener
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import org.drinkless.td.libcore.telegram.TdApi

private const val SELECTED_CHATS_KEY = "selected_chats"
private const val SHARE_LOCATION_CHAT = 1
private const val DEFAULT_CHAT = 0

private const val ADAPTER_UPDATE_INTERVAL_MIL = 5 * 1000L // 5 sec

class MyLocationTabFragment : Fragment(), TelegramListener {

	private var textMarginSmall: Int = 0
	private var textMarginBig: Int = 0
	private var searchBoxHeight: Int = 0
	private var searchBoxSidesMargin: Int = 0

	private var appBarScrollRange: Int = -1

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val shareLocationHelper get() = app.shareLocationHelper
	private val settings get() = app.settings

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var imageContainer: FrameLayout
	private lateinit var currentUserIcon: ImageView
	private lateinit var textContainer: LinearLayout
	private lateinit var titleContainer: LinearLayout
	private lateinit var optionsBtn: ImageView
	private lateinit var title: TextView
	private lateinit var description: TextView
	private lateinit var searchBox: FrameLayout
	private lateinit var stopSharingSwitcher: Switch
	private lateinit var startSharingBtn: View

	private lateinit var searchBoxBg: GradientDrawable

	private val adapter = MyLocationListAdapter()

	private var appBarCollapsed = false
	private lateinit var appBarOutlineProvider: ViewOutlineProvider

	private val selectedChats = HashSet<Long>()

	private var actionButtonsListener: ActionButtonsListener? = null

	private var sharingMode = false
	
	private var updateEnable: Boolean = false

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val activity = activity
		if (activity is ActionButtonsListener) {
			actionButtonsListener = activity
		}

		textMarginSmall = resources.getDimensionPixelSize(R.dimen.content_padding_standard)
		textMarginBig = resources.getDimensionPixelSize(R.dimen.my_location_text_sides_margin)
		searchBoxHeight = resources.getDimensionPixelSize(R.dimen.search_box_height)
		searchBoxSidesMargin = resources.getDimensionPixelSize(R.dimen.content_padding_half)

		sharingMode = settings.hasAnyChatToShareLocation()
				
		savedInstanceState?.apply {
			selectedChats.addAll(getLongArray(SELECTED_CHATS_KEY).toSet())
			if (selectedChats.isNotEmpty()) {
				actionButtonsListener?.switchButtonsVisibility(true)
			}
		}

		val mainView = inflater.inflate(R.layout.fragment_my_location_tab, container, false)

		appBarLayout = mainView.findViewById<AppBarLayout>(R.id.app_bar_layout).apply {
			if (Build.VERSION.SDK_INT >= 21) {
				appBarOutlineProvider = outlineProvider
				outlineProvider = null
			}
			addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
				if (appBarScrollRange == -1) {
					appBarScrollRange = appBar.totalScrollRange
				}
				val collapsed = Math.abs(offset) == appBarScrollRange
				if (collapsed != appBarCollapsed) {
					appBarCollapsed = collapsed
					adjustText()
					adjustAppbar()
					optionsBtn.visibility = if (collapsed) View.VISIBLE else View.GONE
				}
			})
		}

		currentUserIcon = mainView.findViewById(R.id.user_icon)

		optionsBtn = mainView.findViewById<ImageView>(R.id.options)
		with(activity as MainActivity) {
			setupOptionsBtn(optionsBtn)
			setupOptionsBtn(mainView.findViewById<ImageView>(R.id.options_title))
		}
		
		imageContainer = mainView.findViewById<FrameLayout>(R.id.image_container)
		titleContainer = mainView.findViewById<LinearLayout>(R.id.title_container).apply {
			AndroidUtils.addStatusBarPadding19v(context, this)
		}

		textContainer = mainView.findViewById<LinearLayout>(R.id.text_container).apply {
			if (Build.VERSION.SDK_INT >= 16) {
				layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
			}
			AndroidUtils.addStatusBarPadding19v(context!!, this)
			title = findViewById(R.id.title)
			description = findViewById(R.id.description)
		}

		searchBoxBg = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(ContextCompat.getColor(context!!, R.color.screen_bg_light))
			cornerRadius = (searchBoxHeight / 2).toFloat()
		}

		searchBox = mainView.findViewById<FrameLayout>(R.id.search_box).apply {
			if (Build.VERSION.SDK_INT >= 16) {
				background = searchBoxBg
			} else {
				@Suppress("DEPRECATION")
				setBackgroundDrawable(searchBoxBg)
			}
			findViewById<View>(R.id.search_button).setOnClickListener {
				Toast.makeText(context, "Search", Toast.LENGTH_SHORT).show()
			}
			findViewById<ImageView>(R.id.search_icon)
				.setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_search_dark))
		}

		mainView.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@MyLocationTabFragment.adapter
			addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
					super.onScrollStateChanged(recyclerView, newState)
					when (newState) {
						RecyclerView.SCROLL_STATE_DRAGGING -> animateStartSharingBtn(false)
						RecyclerView.SCROLL_STATE_IDLE -> animateStartSharingBtn(true)
					}
				}
			})
		}

		mainView.findViewById<View>(R.id.stop_all_sharing_row).setOnClickListener {
			fragmentManager?.also { fm ->
				DisableSharingBottomSheet.showInstance(fm, this, adapter.chats.size)
			}
		}

		stopSharingSwitcher = mainView.findViewById(R.id.stop_all_sharing_switcher)

		startSharingBtn = mainView.findViewById<View>(R.id.start_sharing_btn).apply {
			visibility = if (sharingMode) View.VISIBLE else View.GONE
			setOnClickListener {
				sharingMode = false
				actionButtonsListener?.switchButtonsVisibility(true)
				updateContent()
			}
		}
		
		return mainView
	}

	override fun onResume() {
		super.onResume()
		updateCurrentUserPhoto()
		telegramHelper.getActiveLiveLocationMessages(null)
		updateContent()
		updateEnable = true
		startHandler()
	}

	override fun onPause() {
		super.onPause()
		updateEnable = false
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLongArray(SELECTED_CHATS_KEY, selectedChats.toLongArray())
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			SetTimeDialogFragment.LOCATION_SHARED_REQUEST_CODE -> {
				sharingMode = settings.hasAnyChatToShareLocation()
				clearSelection()
				updateContent()
			}
			DisableSharingBottomSheet.SHARING_DISABLED_REQUEST_CODE -> {
				sharingMode = false
				app.stopSharingLocation()
				updateContent()
			}
		}
	}

	override fun onTelegramStatusChanged(
		prevTelegramAuthorizationState: TelegramHelper.TelegramAuthorizationState,
		newTelegramAuthorizationState: TelegramHelper.TelegramAuthorizationState
	) {
		when (newTelegramAuthorizationState) {
			TelegramHelper.TelegramAuthorizationState.READY -> {
				updateContent()
			}
			TelegramHelper.TelegramAuthorizationState.LOGGING_OUT,
			TelegramHelper.TelegramAuthorizationState.CLOSED,
			TelegramHelper.TelegramAuthorizationState.UNKNOWN -> {
				adapter.chats = mutableListOf()
			}
			else -> Unit
		}
	}

	override fun onTelegramChatsRead() {
		updateContent()
	}

	override fun onTelegramChatsChanged() {
		updateContent()
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		updateContent()
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		if (user.id == telegramHelper.getCurrentUser()?.id) {
			updateCurrentUserPhoto()
		}
		updateContent()
	}

	override fun onTelegramError(code: Int, message: String) {
	}

	override fun onSendLiveLocationError(code: Int, message: String) {
	}

	fun onPrimaryBtnClick() {
		if (selectedChats.isNotEmpty()) {
			val fm = fragmentManager ?: return
			SetTimeDialogFragment.showInstance(fm, selectedChats, this)
		}
	}

	fun onSecondaryBtnClick() {
		clearSelection()
		if (settings.hasAnyChatToShareLocation()) {
			sharingMode = true
			updateContent()
		}
	}

	private fun updateCurrentUserPhoto() {
		TelegramUiHelper.setupPhoto(
			app,
			currentUserIcon,
			telegramHelper.getUserPhotoPath(telegramHelper.getCurrentUser()),
			R.drawable.img_user_placeholder,
			false
		)
	}

	private fun startHandler() {
		val updateAdapter = Handler()
		updateAdapter.postDelayed({
			if (updateEnable) {
				if (sharingMode) {
					updateExistingLiveMessages()
					val iterator = adapter.chats.iterator()
					while (iterator.hasNext()) {
						val chat = iterator.next()
						if (settings.getChatLiveMessageExpireTime(chat.id) <= 0) {
							settings.shareLocationToChat(chat.id, false)
							iterator.remove()
						}
					}
					if (adapter.chats.isNotEmpty()) {
						adapter.chats = sortAdapterItems(adapter.chats)
						adapter.notifyDataSetChanged()
					} else {
						sharingMode = false
						updateContent()
					}
				}
				startHandler()
			}
		}, ADAPTER_UPDATE_INTERVAL_MIL)
	}
	
	private fun animateStartSharingBtn(show: Boolean) {
		if (startSharingBtn.visibility == View.VISIBLE) {
			val scale = if (show) 1f else 0f
			startSharingBtn.animate()
				.scaleX(scale)
				.scaleY(scale)
				.setDuration(200)
				.setInterpolator(LinearInterpolator())
				.start()
		}
	}
	
	private fun clearSelection() {
		selectedChats.clear()
		adapter.notifyDataSetChanged()
		actionButtonsListener?.switchButtonsVisibility(false)
	}

	private fun adjustText() {
		val gravity = if (appBarCollapsed) Gravity.START else Gravity.CENTER
		val padding = if (appBarCollapsed) textMarginSmall else textMarginBig
		textContainer.apply {
			setPadding(padding, paddingTop, padding, paddingBottom)
		}
		title.gravity = gravity
		description.gravity = gravity
	}

	private fun adjustAppbar() {
		updateTitleTextColor()
		if (Build.VERSION.SDK_INT >= 21) {
			if (appBarCollapsed) {
				appBarLayout.outlineProvider = appBarOutlineProvider
			} else {
				appBarLayout.outlineProvider = null
			}
		}
	}

	private fun adjustSearchBox() {
		val cornerRadiusFrom = if (appBarCollapsed) searchBoxHeight / 2 else 0
		val cornerRadiusTo = if (appBarCollapsed) 0 else searchBoxHeight / 2
		val marginFrom = if (appBarCollapsed) searchBoxSidesMargin else 0
		val marginTo = if (appBarCollapsed) 0 else searchBoxSidesMargin

		val cornerAnimator = ObjectAnimator.ofFloat(
			searchBoxBg,
			"cornerRadius",
			cornerRadiusFrom.toFloat(),
			cornerRadiusTo.toFloat()
		)

		val marginAnimator = ValueAnimator.ofInt(marginFrom, marginTo)
		marginAnimator.addUpdateListener {
			val value = it.animatedValue as Int
			val params = searchBox.layoutParams as LinearLayout.LayoutParams
			params.setMargins(value, params.topMargin, value, params.bottomMargin)
			searchBox.layoutParams = params
		}

		AnimatorSet().apply {
			duration = 200
			playTogether(cornerAnimator, marginAnimator)
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator?) {
					updateTitleTextColor()
					if (appBarCollapsed && Build.VERSION.SDK_INT >= 21) {
						appBarLayout.outlineProvider = appBarOutlineProvider
					}
				}
			})
			start()
		}

		if (!appBarCollapsed && Build.VERSION.SDK_INT >= 21) {
			appBarLayout.outlineProvider = null
		}
	}

	private fun updateTitleTextColor() {
		val color = if (appBarCollapsed) R.color.app_bar_title_light else R.color.ctrl_active_light
		context?.also {
			title.setTextColor(ContextCompat.getColor(it, color))
		}
	}

	private fun updateContent() {
		updateSharingMode()
		updateList()
	}

	private fun updateSharingMode() {
		val headerParams = imageContainer.layoutParams as AppBarLayout.LayoutParams
		imageContainer.visibility = if (sharingMode) View.GONE else View.VISIBLE
		textContainer.visibility = if (sharingMode) View.GONE else View.VISIBLE
		titleContainer.visibility = if (sharingMode) View.VISIBLE else View.GONE
		startSharingBtn.visibility = if (sharingMode) View.VISIBLE else View.GONE
		headerParams.scrollFlags = if (sharingMode) 0 else AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
		stopSharingSwitcher.isChecked = true
		appBarScrollRange = -1
	}

	private fun updateList() {
		val chats: MutableList<TdApi.Chat> = mutableListOf()
		val currentUser = telegramHelper.getCurrentUser()
		val chatList = if (sharingMode && settings.hasAnyChatToShareLocation()) {
			settings.getShareLocationChats()
		} else {
			telegramHelper.getChatListIds()
		}
		for (chatId in chatList) {
			val chat = telegramHelper.getChat(chatId)
			if (chat != null) {
				if (settings.isSharingLocationToChat(chatId)) {
					if (sharingMode) {
						val message = telegramHelper.getChatLiveMessages()[chat.id]
						if (message != null) {
							settings.updateChatShareLocStartSec(chatId, message.date.toLong())
						}
					} else {
						continue
					}
				} else if (telegramHelper.isPrivateChat(chat)) {
					if ((chat.type as TdApi.ChatTypePrivate).userId == currentUser?.id) {
						continue
					}
				}
				chats.add(chat)
			}
		}
		if (sharingMode && settings.hasAnyChatToShareLocation()) {
			adapter.chats = sortAdapterItems(chats)
		} else {
			adapter.chats = chats
		}
	}

	private fun updateExistingLiveMessages() {
		telegramHelper.getChatLiveMessages().values.forEach {
			if (settings.isSharingLocationToChat(it.chatId)
				&& (settings.getChatShareLocStartSec(it.chatId) == null || settings.getChatLivePeriod(it.chatId) == null)) {
				settings.shareLocationToChat(it.chatId, true, (it.content as TdApi.MessageLocation).livePeriod.toLong())
				settings.updateChatShareLocStartSec(it.chatId, it.date.toLong())
			}
		}
		sharingMode = settings.hasAnyChatToShareLocation()
		if (!shareLocationHelper.sharingLocation && sharingMode && AndroidUtils.isLocationPermissionAvailable(app)) {
			shareLocationHelper.startSharingLocation()
		}
	}

	private fun sortAdapterItems(list: MutableList<TdApi.Chat>): MutableList<TdApi.Chat> {
		list.sortWith(Comparator<TdApi.Chat> { o1, o2 -> o1.title.compareTo(o2.title) })
		return list
	}
	
	inner class MyLocationListAdapter : RecyclerView.Adapter<MyLocationListAdapter.BaseViewHolder>() {
		var chats = mutableListOf<TdApi.Chat>()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun getItemViewType(position: Int): Int {
			return if (settings.isSharingLocationToChat(chats[position].id) && sharingMode) {
				SHARE_LOCATION_CHAT
			} else {
				DEFAULT_CHAT
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
			return when (viewType) {
				SHARE_LOCATION_CHAT -> {
					val view = LayoutInflater.from(parent.context)
						.inflate(R.layout.my_location_sharing_chat, parent, false)
					SharingChatViewHolder(view)
				}
				DEFAULT_CHAT -> {
					val view = LayoutInflater.from(parent.context)
						.inflate(R.layout.user_list_item, parent, false)
					ChatViewHolder(view)
				}
				else -> throw RuntimeException("Unsupported view type: $viewType")
			}
		}

		@SuppressLint("SetTextI18n")
		override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
			val chat = chats[position]
			val lastItem = position == itemCount - 1
			val placeholderId = if (telegramHelper.isGroup(chat)) R.drawable.img_group_picture else R.drawable.img_user_picture
			val live = settings.isSharingLocationToChat(chat.id)

			TelegramUiHelper.setupPhoto(app, holder.icon, chat.photo?.small?.local?.path, placeholderId, false)
			holder.title?.text = chat.title

			if (holder is ChatViewHolder) {
				holder.description?.visibility = View.GONE
				if (live) {
					holder.checkBox?.visibility = View.GONE
				} else {
					holder.checkBox?.apply {
						visibility = View.VISIBLE
						setOnCheckedChangeListener(null)
						isChecked = selectedChats.contains(chat.id)
						setOnCheckedChangeListener { _, isChecked ->
							if (isChecked) {
								selectedChats.add(chat.id)
							} else {
								selectedChats.remove(chat.id)
							}
							actionButtonsListener?.switchButtonsVisibility(selectedChats.isNotEmpty())
						}
					}
				}
				holder.bottomShadow?.visibility = if (lastItem) View.VISIBLE else View.GONE
				holder.itemView.setOnClickListener {
					if (live) {
						settings.shareLocationToChat(chat.id, false)
						shareLocationHelper.stopSharingLocation()
						notifyItemChanged(position)
					} else {
						holder.checkBox?.apply {
							isChecked = !isChecked
						}
					}
				}
			} else if (holder is SharingChatViewHolder) {
				holder.switcher?.apply {
					isChecked = live
					setOnCheckedChangeListener { _, isChecked ->
						if (!isChecked) {
							settings.shareLocationToChat(chat.id, false)
							telegramHelper.stopSendingLiveLocationToChat(chat.id)
							removeItem(chat)
						}
					}
				}

				val duration = settings.getChatLivePeriod(chat.id)
				if (duration != null && duration > 0) {
					holder.descriptionDuration?.text = OsmandFormatter.getFormattedDuration(context!!, duration)
					holder.description?.apply {
						visibility = View.VISIBLE
						text = "${getText(R.string.sharing_time)}:"
					}
				}

				val expiresIn = settings.getChatLiveMessageExpireTime(chat.id)
				
				holder.textInArea?.apply {
					visibility = View.VISIBLE
					text = "${getText(R.string.plus)} ${OsmandFormatter.getFormattedDuration(
						context!!, settings.getChatAddActiveTime(chat.id))}"
					setOnClickListener {
						val chatNextAddTime = settings.getChatNextAddActiveTime(chat.id)
						val newLivePeriod = settings.getChatLiveMessageExpireTime(chat.id) + settings.getChatAddActiveTime(chat.id)
						settings.shareLocationToChat(chat.id, false)
						telegramHelper.stopSendingLiveLocationToChat(chat.id)
						settings.shareLocationToChat(chat.id, true, newLivePeriod, chatNextAddTime)
						app.forceUpdateMyLocation()
						notifyItemChanged(position)
					}
				}

				holder.stopSharingDescr?.apply {
					visibility = getStopSharingVisibility(expiresIn)
					text = "${getText(R.string.expire_in)}:"
				}

				holder.stopSharingFirstPart?.apply {
					visibility = getStopSharingVisibility(expiresIn)
					text = OsmandFormatter.getFormattedTime(expiresIn)
				}

				holder.stopSharingSecondPart?.apply {
					visibility = getStopSharingVisibility(expiresIn)
					text = "(${getString(R.string.in_time,
						OsmandFormatter.getFormattedDuration(context!!, expiresIn, true))})"
				}
			}
		}

		private fun getStopSharingVisibility(expiresIn: Long) = if (expiresIn > 0) View.VISIBLE else View.INVISIBLE

		private fun removeItem(chat: TdApi.Chat) {
			chats.remove(chat)
			if (chats.isEmpty()) {
				sharingMode = false
				updateContent()
				shareLocationHelper.stopSharingLocation()
			} else {
				adapter.notifyDataSetChanged()
			}
		}

		override fun getItemCount() = chats.size

		abstract inner class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
		}

		inner class ChatViewHolder(val view: View) : BaseViewHolder(view) {
			val checkBox: CheckBox? = view.findViewById(R.id.check_box)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}

		inner class SharingChatViewHolder(val view: View) : BaseViewHolder(view) {
			val descriptionDuration: TextView? = view.findViewById(R.id.duration)
			val switcher: Switch? = view.findViewById(R.id.switcher)
			val stopSharingDescr: TextView? = view.findViewById(R.id.stop_in)
			val stopSharingFirstPart: TextView? = view.findViewById(R.id.ending_in_first_part)
			val stopSharingSecondPart: TextView? = view.findViewById(R.id.ending_in_second_part)
		}
	}

	interface ActionButtonsListener {
		fun switchButtonsVisibility(visible: Boolean)
	}
}
