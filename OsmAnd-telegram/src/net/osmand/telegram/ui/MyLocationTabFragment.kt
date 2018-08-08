package net.osmand.telegram.ui

import android.animation.*
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.TelegramListener
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.TimeUnit
import java.util.*

private const val SELECTED_CHATS_KEY = "selected_chats"
private const val SHARE_LOCATION_CHAT = 1
private const val DEFAULT_CHAT = 0

class MyLocationTabFragment : Fragment(), TelegramListener {

	private var textMarginSmall: Int = 0
	private var textMarginBig: Int = 0
	private var searchBoxHeight: Int = 0
	private var searchBoxSidesMargin: Int = 0

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val settings get() = app.settings

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var userImage: ImageView
	private lateinit var imageContainer: FrameLayout
	private lateinit var titleContainer: LinearLayout
	private lateinit var optionsBtn: ImageView
	private lateinit var optionsBtnTitle: ImageView
	private lateinit var textContainer: LinearLayout
	private lateinit var title: TextView
	private lateinit var description: TextView
	private lateinit var searchBox: FrameLayout
	private lateinit var stopSharingSwitcher: Switch

	private lateinit var searchBoxBg: GradientDrawable

	private val adapter = MyLocationListAdapter()

	private var appBarCollapsed = false
	private lateinit var appBarOutlineProvider: ViewOutlineProvider

	private val selectedChats = HashSet<Long>()

	private var actionButtonsListener: ActionButtonsListener? = null

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
			addOnOffsetChangedListener { appBar, offset ->
				val collapsed = Math.abs(offset) == appBar.totalScrollRange
				if (collapsed != appBarCollapsed) {
					appBarCollapsed = collapsed
					adjustText()
					adjustAppbar()
					optionsBtn.visibility = if (collapsed) View.VISIBLE else View.GONE
				}
			}
		}

		userImage = mainView.findViewById<ImageView>(R.id.my_location_user_image).apply {
			setImageResource(R.drawable.img_my_location_user)
		}

		optionsBtn = mainView.findViewById<ImageView>(R.id.options).apply {
			setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_other_menu))
			setOnClickListener { showPopupMenu(optionsBtn) }
		}

		optionsBtnTitle = mainView.findViewById<ImageView>(R.id.options_title).apply {
			setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_other_menu))
			setOnClickListener { showPopupMenu(optionsBtnTitle) }
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
		}

		stopSharingSwitcher = mainView.findViewById<Switch>(R.id.stop_all_sharing_switcher).apply {
			isChecked = settings.hasAnyChatToShareLocation()
			setOnCheckedChangeListener { _, isChecked ->
				if (!isChecked) {
					app.settings.stopSharingLocationToChats()
					if (!app.settings.hasAnyChatToShareLocation()) {
						app.shareLocationHelper.stopSharingLocation()
					}
					updateSharingMode()
					updateList()
				}
			}
		}

		return mainView
	}

	override fun onResume() {
		super.onResume()
		updateSharingMode()
		updateList()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLongArray(SELECTED_CHATS_KEY, selectedChats.toLongArray())
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == SetTimeDialogFragment.LOCATION_SHARED_REQUEST_CODE) {
			clearSelection()
			updateSharingMode()
			updateList()
		}
	}

	override fun onTelegramStatusChanged(
		prevTelegramAuthorizationState: TelegramHelper.TelegramAuthorizationState,
		newTelegramAuthorizationState: TelegramHelper.TelegramAuthorizationState
	) {
		when (newTelegramAuthorizationState) {
			TelegramHelper.TelegramAuthorizationState.READY -> {
				updateSharingMode()
				updateList()
			}
			TelegramHelper.TelegramAuthorizationState.CLOSED,
			TelegramHelper.TelegramAuthorizationState.UNKNOWN -> {
				adapter.chats = mutableListOf()
			}
			else -> Unit
		}
	}

	override fun onTelegramChatsRead() {
		updateSharingMode()
		updateList()
	}

	override fun onTelegramChatsChanged() {
		updateSharingMode()
		updateList()
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		updateSharingMode()
		updateList()
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		updateSharingMode()
		updateList()
	}

	override fun onTelegramError(code: Int, message: String) {
	}

	override fun onSendLiveLocationError(code: Int, message: String) {
	}

	fun onPrimaryBtnClick() {
		val fm = fragmentManager ?: return
		SetTimeDialogFragment.showInstance(fm, selectedChats, this)
	}

	fun onSecondaryBtnClick() {
		clearSelection()
	}

	private fun clearSelection() {
		selectedChats.clear()
		adapter.notifyDataSetChanged()
		actionButtonsListener?.switchButtonsVisibility(false)
	}

	private fun updateSharingMode() {
		val headerParams = imageContainer.layoutParams as AppBarLayout.LayoutParams
		if (!settings.hasAnyChatToShareLocation()) {
			imageContainer.visibility = View.VISIBLE
			textContainer.visibility = View.VISIBLE
			titleContainer.visibility = View.GONE
			headerParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
		} else {
			imageContainer.visibility = View.GONE
			textContainer.visibility = View.GONE
			titleContainer.visibility = View.VISIBLE
			headerParams.scrollFlags = 0
			stopSharingSwitcher.isChecked = true
		}
	}

	private fun showPopupMenu(anchor:View) {
		val ctx = context ?: return

		val menuList = ArrayList<String>()
		val settings = getString(R.string.shared_string_settings)
		val logout = getString(R.string.shared_string_logout)
		val login = getString(R.string.shared_string_login)

		menuList.add(settings)
		@Suppress("NON_EXHAUSTIVE_WHEN")
		when (telegramHelper.getTelegramAuthorizationState()) {
			TelegramHelper.TelegramAuthorizationState.READY -> menuList.add(logout)
			TelegramHelper.TelegramAuthorizationState.CLOSED -> menuList.add(login)
		}

		ListPopupWindow(ctx).apply {
			isModal = true
			anchorView = anchor
			setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
			setDropDownGravity(Gravity.END or Gravity.TOP)
			setAdapter(ArrayAdapter(ctx, R.layout.popup_list_text_item, menuList))
			setOnItemClickListener { _, _, position, _ ->
				when (position) {
					menuList.indexOf(settings) -> {
						fragmentManager?.also { SettingsDialogFragment.showInstance(it) }
					}
					menuList.indexOf(logout) -> logoutTelegram()
					menuList.indexOf(login) -> loginTelegram()
				}
				dismiss()
			}
			show()
		}
	}

	private fun logoutTelegram(silent: Boolean = false) {
		if (telegramHelper.getTelegramAuthorizationState() == TelegramHelper.TelegramAuthorizationState.READY) {
			telegramHelper.logout()
		} else if (!silent) {
			Toast.makeText(context, R.string.not_logged_in, Toast.LENGTH_SHORT).show()
		}
	}

	private fun loginTelegram() {
		if (telegramHelper.getTelegramAuthorizationState() != TelegramHelper.TelegramAuthorizationState.CLOSED) {
			telegramHelper.logout()
		}
		telegramHelper.init()
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

	private fun updateList() {
		val chats: MutableList<TdApi.Chat> = mutableListOf()
		val currentUser = telegramHelper.getCurrentUser()
		val chatList = if (settings.hasAnyChatToShareLocation()) {
			settings.getShareLocationChats()
		} else {
			telegramHelper.getChatListIds()
		}
		for (chatId in chatList) {
			val chat = telegramHelper.getChat(chatId)
			if (chat != null) {
				if (telegramHelper.isPrivateChat(chat)) {
					if ((chat.type as TdApi.ChatTypePrivate).userId == currentUser?.id) {
						continue
					}
				}
				chats.add(chat)
			}
		}
		adapter.chats = chats
	}

	private fun formatTime(timeSec: Long, short: Boolean): String {
		val hours = TimeUnit.SECONDS.toHours(timeSec)
		val minutes = TimeUnit.SECONDS.toMinutes(timeSec) % 60
		return if (short) {
			String.format("%02d:%02d", hours, minutes)
		} else {
			var hoursS = ""
			var minutesS = ""
			if (hours > 0) {
				hoursS = String.format("%2d h ", hours)
			}
			if (minutes > 0) {
				minutesS = String.format("%2d min", minutes)
			}
			hoursS + minutesS
		}
	}

	inner class MyLocationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		var chats = mutableListOf<TdApi.Chat>()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun getItemViewType(position: Int): Int {
			return if (settings.isSharingLocationToChat(chats[position].id)) {
				SHARE_LOCATION_CHAT
			} else {
				DEFAULT_CHAT
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return if (viewType == SHARE_LOCATION_CHAT) {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.my_location_sharing_chat, parent, false)
				SharingChatViewHolder(view)
			} else {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.user_list_item, parent, false)
				ChatViewHolder(view)
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val chat = chats[position]
			val lastItem = position == itemCount - 1
			val placeholderId =
				if (telegramHelper.isGroup(chat)) R.drawable.img_group_picture else R.drawable.img_user_picture
			val live = app.settings.isSharingLocationToChat(chat.id)

			if (holder is ChatViewHolder) { TelegramUiHelper.setupPhoto(app, holder.icon, chat.photo?.small?.local?.path, placeholderId, false)
				holder.title?.text = chat.title
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
						app.settings.shareLocationToChat(chat.id, false)
						if (!app.settings.hasAnyChatToShareLocation()) {
							app.shareLocationHelper.stopSharingLocation()
						}
						notifyItemChanged(position)
					} else {
						holder.checkBox?.apply {
							isChecked = !isChecked
						}
					}
				}
			} else if (holder is SharingChatViewHolder) {
				TelegramUiHelper.setupPhoto(app, holder.icon, chat.photo?.small?.local?.path, placeholderId, false)
				holder.title?.text = chat.title
				holder.switcher?.apply {
					if (live) {
						isChecked = true
					}
					setOnCheckedChangeListener { _, isChecked ->
						run {
							if (!isChecked) {
								app.settings.shareLocationToChat(chat.id, false)
								if (!app.settings.hasAnyChatToShareLocation()) {
									app.shareLocationHelper.stopSharingLocation()
								}
								chats.remove(chat)
								updateSharingMode()
								if (chats.isEmpty()) {
									updateList()
								} else {
									adapter.notifyDataSetChanged()
								}
							}
						}
					}
				}
				val duration = settings.getChatLivePeriod(chat.id)
				if (duration != null && duration > 0) {
					holder.descriptionDuration?.text = OsmandFormatter.getFormattedDuration(context!!, duration.toInt())
				}
				val map = telegramHelper.getChatLiveMessages()
				val message = map[chat.id]
				if (message != null) {
					val content = message.content
					if (content is TdApi.MessageLocation) {
						val currentTime = (System.currentTimeMillis() / 1000 + content.expiresIn).toInt()
						holder.stopSharingFirstPart?.text = OsmandFormatter.getFormattedDuration(context!!, content.expiresIn)
						holder.stopSharingSecondPart?.text = OsmandFormatter.getFormattedDuration(context!!, currentTime)
					}
				} else {
					holder.textInArea?.visibility = View.INVISIBLE
					holder.stopSharingFirstPart?.visibility = View.INVISIBLE
					holder.stopSharingSecondPart?.visibility = View.INVISIBLE
				}
			}
		}

		override fun getItemCount() = chats.size

		inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
			val checkBox: CheckBox? = view.findViewById(R.id.check_box)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}

		inner class SharingChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val descriptionDuration: TextView? = view.findViewById(R.id.duration)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
			val switcher: Switch? = view.findViewById(R.id.switcher)
			val stopSharingDescr: TextView? = view.findViewById(R.id.ending_in)
			val stopSharingFirstPart: TextView? = view.findViewById(R.id.ending_in_first_part)
			val stopSharingSecondPart: TextView? = view.findViewById(R.id.ending_in_second_part)
		}
	}

	interface ActionButtonsListener {
		fun switchButtonsVisibility(visible: Boolean)
	}
}
