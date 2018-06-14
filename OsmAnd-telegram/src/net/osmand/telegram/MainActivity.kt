package net.osmand.telegram

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.view.*
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.telegram.LoginDialogFragment.LoginDialogType
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi


class MainActivity : AppCompatActivity(), TelegramListener {

	companion object {
		private const val PERMISSION_REQUEST_LOCATION = 1

		private const val LOGIN_MENU_ID = 0
		private const val LOGOUT_MENU_ID = 1
		private const val PROGRESS_MENU_ID = 2
	}

	private val log = PlatformUtil.getLog(TelegramHelper::class.java)

	private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null
	private var paused: Boolean = false

	private lateinit var chatsView: RecyclerView
	private lateinit var chatViewAdapter: ChatsAdapter
	private lateinit var chatViewManager: RecyclerView.LayoutManager

	private val app: TelegramApplication
		get() = application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val osmandHelper get() = app.osmandHelper
	private val settings get() = app.settings

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		paused = false

		chatViewManager = LinearLayoutManager(this)
		chatViewAdapter = ChatsAdapter()

		chatsView = findViewById<RecyclerView>(R.id.groups_view).apply {
			//setHasFixedSize(true)

			// use a linear layout manager
			layoutManager = chatViewManager

			// specify an viewAdapter (see also next example)
			adapter = chatViewAdapter

		}

		telegramAuthorizationRequestHandler = telegramHelper.setTelegramAuthorizationRequestHandler(object : TelegramAuthorizationRequestListener {
			override fun onRequestTelegramAuthenticationParameter(parameterType: TelegramAuthenticationParameterType) {
				runOnUi {
					showLoginDialog(parameterType)
				}
			}

			override fun onTelegramAuthorizationRequestError(code: Int, message: String) {
				runOnUi {
					Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
				}
			}
		})
		telegramHelper.listener = this
		if (!telegramHelper.isInit()) {
			telegramHelper.init()
		}

		if (osmandHelper.isOsmandBound() && !osmandHelper.isOsmandConnected()) {
			osmandHelper.connectOsmand()
		}
	}

	override fun onResume() {
		super.onResume()
		paused = false

		invalidateOptionsMenu()
		updateTitle()
		updateChatsList()

		if (settings.hasAnyChatToShareLocation() && !AndroidUtils.isLocationPermissionAvailable(this)) {
			requestLocationPermission()
		} else if (settings.hasAnyChatToShowOnMap() && osmandHelper.isOsmandNotInstalled()) {
			showOsmandMissingDialog()
		}
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.listener = null

		paused = true
	}

	override fun onStop() {
		super.onStop()
		settings.save()
	}

	override fun onDestroy() {
		super.onDestroy()

		if (app.telegramService == null) {
			app.cleanupResources()
		}
	}

	override fun onTelegramStatusChanged(prevTelegramAuthorizationState: TelegramAuthorizationState,
										 newTelegramAuthorizationState: TelegramAuthorizationState) {
		runOnUi {
			val fm = supportFragmentManager
			when (newTelegramAuthorizationState) {
				TelegramAuthorizationState.READY,
				TelegramAuthorizationState.CLOSED,
				TelegramAuthorizationState.UNKNOWN -> LoginDialogFragment.dismiss(fm)
				else -> Unit
			}
			invalidateOptionsMenu()
			updateTitle()

			when (newTelegramAuthorizationState) {
				TelegramAuthorizationState.READY -> {
					updateChatsList()
				}
				TelegramAuthorizationState.CLOSED,
				TelegramAuthorizationState.UNKNOWN -> {
					chatViewAdapter.chats = emptyList()
				}
				else -> Unit
			}
		}
	}

	override fun onTelegramChatsRead() {
		runOnUi {
			removeNonexistingChatsFromSettings()
			updateChatsList()
		}
	}

	override fun onTelegramChatsChanged() {
		runOnUi {
			updateChatsList()
		}
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		runOnUi {
			updateChat(chat)
		}
	}

	override fun onTelegramError(code: Int, message: String) {
		runOnUi {
			Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
		}
	}

	override fun onSendLiveLicationError(code: Int, message: String) {
		log.error("Send live location error: $code - $message")
		app.isInternetConnectionAvailable(true)
	}

	private fun removeNonexistingChatsFromSettings() {
		val presentChatTitles = telegramHelper.getChatTitles()
		settings.removeNonexistingChats(presentChatTitles)
	}

	private fun updateChatsList() {
		val chatList = telegramHelper.getChatList()
		val chats: MutableList<TdApi.Chat> = mutableListOf()
		for (orderedChat in chatList) {
			val chat = telegramHelper.getChat(orderedChat.chatId)
			if (chat != null) {
				chats.add(chat)
			}
		}
		chatViewAdapter.chats = chats
	}

	private fun updateChat(chat: TdApi.Chat) {
		val chatIndex = telegramHelper.getChatIndex(chat.id)
		if (chatIndex != -1) {
			chatViewAdapter.notifyItemChanged(chatIndex)
		} else {
			updateChatsList()
		}
	}

	fun logoutTelegram(silent: Boolean = false) {
		if (telegramHelper.getTelegramAuthorizationState() == TelegramAuthorizationState.READY) {
			telegramHelper.logout()
		} else {
			invalidateOptionsMenu()
			updateTitle()
			if (!silent) {
				Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show()
			}
		}
	}

	fun closeTelegram() {
		telegramHelper.close()
	}

	private fun runOnUi(action: (() -> Unit)) {
		if (!paused) {
			runOnUiThread(action)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		return when (item?.itemId) {
			LOGIN_MENU_ID -> {
				telegramHelper.init()
				true
			}
			LOGOUT_MENU_ID -> {
				logoutTelegram()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		if (menu != null) {
			menu.clear()
			when (telegramHelper.getTelegramAuthorizationState()) {
				TelegramAuthorizationState.UNKNOWN,
				TelegramAuthorizationState.WAIT_PARAMETERS,
				TelegramAuthorizationState.WAIT_PHONE_NUMBER,
				TelegramAuthorizationState.WAIT_CODE,
				TelegramAuthorizationState.WAIT_PASSWORD,
				TelegramAuthorizationState.LOGGING_OUT,
				TelegramAuthorizationState.CLOSING -> createProgressMenuItem(menu)
				TelegramAuthorizationState.READY -> createMenuItem(menu, LOGOUT_MENU_ID, R.string.shared_string_logout,
						MenuItem.SHOW_AS_ACTION_WITH_TEXT or MenuItem.SHOW_AS_ACTION_ALWAYS)
				TelegramAuthorizationState.CLOSED -> createMenuItem(menu, LOGIN_MENU_ID, R.string.shared_string_login,
						MenuItem.SHOW_AS_ACTION_WITH_TEXT or MenuItem.SHOW_AS_ACTION_ALWAYS)
			}
		}
		return super.onCreateOptionsMenu(menu)
	}

	private fun createMenuItem(m: Menu, id: Int, titleRes: Int, menuItemType: Int): MenuItem {
		val menuItem = m.add(0, id, 0, titleRes)
		menuItem.setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
		menuItem.setShowAsAction(menuItemType)
		return menuItem
	}

	private fun createProgressMenuItem(m: Menu): MenuItem {

		val menuItem = m.add(0, PROGRESS_MENU_ID, 0, "")
		menuItem.actionView = layoutInflater.inflate(R.layout.action_progress_bar, null)
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		return menuItem
	}

	private fun updateTitle() {
		title = when (telegramHelper.getTelegramAuthorizationState()) {

			TelegramAuthorizationState.UNKNOWN,
			TelegramAuthorizationState.WAIT_PHONE_NUMBER,
			TelegramAuthorizationState.WAIT_CODE,
			TelegramAuthorizationState.WAIT_PASSWORD,
			TelegramAuthorizationState.READY,
			TelegramAuthorizationState.CLOSED -> getString(R.string.app_name)

			TelegramAuthorizationState.WAIT_PARAMETERS -> getString(R.string.initialization) + "..."
			TelegramAuthorizationState.LOGGING_OUT -> getString(R.string.logging_out) + "..."
			TelegramAuthorizationState.CLOSING -> getString(R.string.closing) + "..."
		}
	}

	private fun showLoginDialog(telegramAuthenticationParameterType: TelegramAuthenticationParameterType) {
		when (telegramAuthenticationParameterType) {
			TelegramAuthenticationParameterType.PHONE_NUMBER -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PHONE_NUMBER)
			TelegramAuthenticationParameterType.CODE -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_CODE)
			TelegramAuthenticationParameterType.PASSWORD -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PASSWORD)
		}
	}

	fun applyAuthParam(loginDialogFragment: LoginDialogFragment?, loginDialogType: LoginDialogType, text: String) {
		loginDialogFragment?.updateDialog(LoginDialogType.SHOW_PROGRESS)
		when (loginDialogType) {
			LoginDialogType.ENTER_PHONE_NUMBER -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PHONE_NUMBER, text)
			LoginDialogType.ENTER_CODE -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.CODE, text)
			LoginDialogType.ENTER_PASSWORD -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PASSWORD, text)
			else -> Unit
		}
	}

	private fun requestLocationPermission() {
		ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		when (requestCode) {
			PERMISSION_REQUEST_LOCATION -> {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if (settings.hasAnyChatToShareLocation()) {
						app.shareLocationHelper.startSharingLocation()
					}
				}
				if (settings.hasAnyChatToShowOnMap() && osmandHelper.isOsmandNotInstalled()) {
					showOsmandMissingDialog()
				}
			}
		}
	}

	fun showOsmandMissingDialog() {
		OsmandMissingDialogFragment().show(supportFragmentManager, null)
	}

	class OsmandMissingDialogFragment : DialogFragment() {

		override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
			val builder = AlertDialog.Builder(requireContext())
			builder.setView(R.layout.install_osmand_dialog)
					.setNegativeButton("Cancel", null)
					.setPositiveButton("Install", { _, _ ->
						val intent = Intent()
						intent.data = Uri.parse("market://details?id=net.osmand.plus")
						startActivity(intent)
					})
			return builder.create()
		}
	}

	inner class ChatsAdapter :
			RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

		var chats: List<TdApi.Chat> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: AppCompatImageView? = view.findViewById(R.id.icon)
			val groupName: AppCompatTextView? = view.findViewById(R.id.name)
			val shareLocationSwitch: SwitchCompat? = view.findViewById(R.id.share_location_switch)
			val showOnMapSwitch: SwitchCompat? = view.findViewById(R.id.show_on_map_switch)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_list_item, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val chat = chats[position]
			val chatTitle = chat.title
			holder.groupName?.text = chatTitle

			var drawable: Drawable? = null
			var bitmap: Bitmap? = null
			val chatPhoto = chat.photo?.small
			if (chatPhoto != null && chatPhoto.local.path.isNotEmpty()) {
				bitmap = app.uiUtils.getCircleBitmap(chatPhoto.local.path)
			}
			if (bitmap == null) {
				drawable = app.uiUtils.getThemedIcon(R.drawable.ic_group)
			}
			if (bitmap != null) {
				holder.icon?.setImageBitmap(bitmap)
			} else {
				holder.icon?.setImageDrawable(drawable)
			}
			holder.shareLocationSwitch?.setOnCheckedChangeListener(null)
			holder.shareLocationSwitch?.isChecked = settings.isSharingLocationToChat(chatTitle)
			holder.shareLocationSwitch?.setOnCheckedChangeListener { view, isChecked ->
				settings.shareLocationToChat(chatTitle, isChecked)
				if (settings.hasAnyChatToShareLocation()) {
					if (!AndroidUtils.isLocationPermissionAvailable(view.context)) {
						if (isChecked) {
							requestLocationPermission()
						}
					} else {
						app.shareLocationHelper.startSharingLocation()
					}
				} else {
					app.shareLocationHelper.stopSharingLocation()
				}
			}

			holder.showOnMapSwitch?.setOnCheckedChangeListener(null)
			holder.showOnMapSwitch?.isChecked = settings.isShowingChatOnMap(chatTitle)
			holder.showOnMapSwitch?.setOnCheckedChangeListener { _, isChecked ->
				settings.showChatOnMap(chatTitle, isChecked)
				if (settings.hasAnyChatToShowOnMap()) {
					if (osmandHelper.isOsmandNotInstalled()) {
						if (isChecked) {
							showOsmandMissingDialog()
						}
					} else {
						if (isChecked) {
							app.showLocationHelper.showChatMessages(chatTitle)
						} else {
							app.showLocationHelper.hideChatMessages(chatTitle)
						}
						app.showLocationHelper.startShowingLocation()
					}
				} else {
					app.showLocationHelper.stopShowingLocation()
					if (!isChecked) {
						app.showLocationHelper.hideChatMessages(chatTitle)
					}
				}
			}
		}

		override fun getItemCount() = chats.size
	}
}
