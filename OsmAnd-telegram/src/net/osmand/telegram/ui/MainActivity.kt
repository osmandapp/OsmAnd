package net.osmand.telegram.ui

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.ui.LoginDialogFragment.LoginDialogType
import net.osmand.telegram.ui.MyLocationTabFragment.ActionButtonsListener
import net.osmand.telegram.ui.views.LockableViewPager
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_DIR
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_EXT
import net.osmand.telegram.utils.OsmandApiUtils
import org.drinkless.tdlib.TdApi
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

const val OPEN_MY_LOCATION_TAB_KEY = "open_my_location_tab"

private const val PERMISSION_REQUEST_LOCATION = 1

private const val MY_LOCATION_TAB_POS = 0
private const val LIVE_NOW_TAB_POS = 1
private const val TIMELINE_TAB_POS = 2

class MainActivity : AppCompatActivity(), TelegramListener, ActionButtonsListener, TelegramIncomingMessagesListener {

	private val log = PlatformUtil.getLog(MainActivity::class.java)

	private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null
	private var paused: Boolean = false

	private val app: TelegramApplication
		get() = application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val osmandAidlHelper get() = app.osmandAidlHelper
	private val settings get() = app.settings

	private val listeners: MutableList<WeakReference<TelegramListener>> = mutableListOf()

	private var myLocationTabFragment: MyLocationTabFragment? = null
	private var liveNowTabFragment: LiveNowTabFragment? = null
	private var timelineTabFragment: TimelineTabFragment? = null

	private lateinit var buttonsBar: LinearLayout
	private lateinit var bottomNav: BottomNavigationView
	private lateinit var coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout
	private lateinit var viewPager: androidx.viewpager.widget.ViewPager

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		if (Build.VERSION.SDK_INT >= 23) {
			AndroidUtils.enterToTransparentFullScreen(this)
		} else if (Build.VERSION.SDK_INT >= 19) {
			AndroidUtils.enterToTranslucentFullScreen(this)
		}
		
		paused = false

		viewPager = findViewById<LockableViewPager>(R.id.view_pager).apply {
			swipeLocked = true
			offscreenPageLimit = 3
			adapter = ViewPagerAdapter(supportFragmentManager)
		}
		coordinatorLayout = findViewById(R.id.coordinator)
		bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation).apply {
			setOnNavigationItemSelectedListener {
				var pos = -1
				when (it.itemId) {
					R.id.action_my_location -> pos = MY_LOCATION_TAB_POS
					R.id.action_live_now -> pos = LIVE_NOW_TAB_POS
					R.id.action_timeline -> pos = TIMELINE_TAB_POS
				}
				if (pos != -1 && pos != viewPager.currentItem) {
					when (pos) {
						MY_LOCATION_TAB_POS -> {
							liveNowTabFragment?.tabClosed()
							timelineTabFragment?.tabClosed()
						}
						LIVE_NOW_TAB_POS -> {
							timelineTabFragment?.tabClosed()
							liveNowTabFragment?.tabOpened()
						}
						TIMELINE_TAB_POS -> {
							liveNowTabFragment?.tabClosed()
							timelineTabFragment?.tabOpened()
							if (shouldShowFreeTimelineInfo()) {
								showFreeTimelineInfo()
							}
						}
					}
					viewPager.currentItem = pos
					return@setOnNavigationItemSelectedListener true
				}
				false
			}
		}

		buttonsBar = findViewById<LinearLayout>(R.id.buttons_bar).apply {
			findViewById<TextView>(R.id.primary_btn).apply {
				text = getString(R.string.shared_string_continue)
				setOnClickListener {
					myLocationTabFragment?.onPrimaryBtnClick()
				}
			}
			findViewById<TextView>(R.id.secondary_btn).apply {
				text = getString(R.string.shared_string_cancel)
				setOnClickListener {
					myLocationTabFragment?.onSecondaryBtnClick()
				}
			}
		}

		if (!LoginDialogFragment.welcomeDialogShown) {
			LoginDialogFragment.showWelcomeDialog(supportFragmentManager)
		}

		telegramAuthorizationRequestHandler = telegramHelper.setTelegramAuthorizationRequestHandler(object : TelegramAuthorizationRequestListener {
			override fun onRequestTelegramAuthenticationParameter(parameterType: TelegramAuthParamType) {
				runOnUi {
					showLoginDialog(parameterType)
				}
			}

			override fun onTelegramAuthorizationRequestError(code: Int, message: String) {
				runOnUi {
					Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
				}
			}

			override fun onTelegramUnsupportedAuthorizationState(authorizationState: String) {
				runOnUi {
					val message = "Unsupported authorization state: $authorizationState"
					Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
				}
			}
		})
		telegramHelper.listener = this
		telegramHelper.requestAuthorizationState()

		if (osmandAidlHelper.isOsmandBound() && !osmandAidlHelper.isOsmandConnected()) {
			osmandAidlHelper.connectOsmand()
		}
	}

	override fun onAttachFragment(fragment: androidx.fragment.app.Fragment) {
		if (fragment is TelegramListener) {
			listeners.add(WeakReference(fragment))
		}
		when (fragment) {
			is MyLocationTabFragment -> myLocationTabFragment = fragment
			is LiveNowTabFragment -> liveNowTabFragment = fragment
			is TimelineTabFragment -> timelineTabFragment = fragment
		}
	}

	override fun onResume() {
		super.onResume()
		paused = false

		if (telegramHelper.listener != this) {
			telegramHelper.listener = this
		}
		telegramHelper.addIncomingMessagesListener(this)

		app.locationProvider.checkIfLastKnownLocationIsValid()

		if (AndroidUtils.isLocationPermissionAvailable(this)) {
			app.locationProvider.resumeAllUpdates()
		}
		if (settings.hasAnyChatToShowOnMap() && !app.isAnyOsmAndInstalled()) {
			showOsmandMissingDialog()
		}
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.listener = null
		telegramHelper.removeIncomingMessagesListener(this)

		app.locationProvider.pauseAllUpdates()

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

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		if (intent.getBooleanExtra(OPEN_MY_LOCATION_TAB_KEY, false)) {
			AndroidUtils.dismissAllDialogs(supportFragmentManager)
			bottomNav.selectedItemId = R.id.action_my_location
		}
	}

	override fun onTelegramStatusChanged(prevTelegramAuthorizationState: TelegramAuthorizationState,
										 newTelegramAuthorizationState: TelegramAuthorizationState) {
		runOnUi {
			val fm = supportFragmentManager
			when (newTelegramAuthorizationState) {
				TelegramAuthorizationState.LOGGING_OUT -> LoginDialogFragment.showWelcomeDialog(fm)
				TelegramAuthorizationState.CLOSED -> {
					telegramHelper.init()
					telegramHelper.requestAuthorizationState()
				}
				TelegramAuthorizationState.READY -> {
					LoginDialogFragment.dismiss(fm)
					if (AndroidUtils.isLocationPermissionAvailable(this)) {
						app.locationProvider.resumeAllUpdates()
					} else {
						AndroidUtils.requestLocationPermission(this)
					}
					val user = telegramHelper.getCurrentUser()
					if (user != null) {
						OsmandApiUtils.updateSharingDevices(app, user.id)
						if (settings.currentSharingMode.isEmpty()) {
							settings.updateCurrentSharingMode(user.id.toString())
						}
					}
				}
				else -> Unit
			}

			listeners.forEach {
				it.get()?.onTelegramStatusChanged(prevTelegramAuthorizationState, newTelegramAuthorizationState)
			}
		}
	}

	override fun onTelegramChatsRead() {
		runOnUi {
			removeNonexistingChatsFromSettings()
			listeners.forEach { it.get()?.onTelegramChatsRead() }
		}
	}

	override fun onTelegramChatsChanged() {
		telegramHelper.getMessagesByChatIds(settings.locHistoryTime).forEach {
			addGrayPhoto(it.key)
		}
		runOnUi {
			listeners.forEach { it.get()?.onTelegramChatsChanged() }
		}
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		addGrayPhoto(chat.id)
		runOnUi {
			listeners.forEach { it.get()?.onTelegramChatChanged(chat) }
		}
	}

	override fun onTelegramChatCreated(chat: TdApi.Chat) {
		runOnUi {
			listeners.forEach { it.get()?.onTelegramChatCreated(chat) }
		}
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		val photoPath = telegramHelper.getUserPhotoPath(user)
		if (photoPath != null) {
			addGrayPhoto(user.id, photoPath)
		}
		val message = telegramHelper.getUserMessage(user)
		if (message != null) {
			app.showLocationHelper.addOrUpdateLocationOnMap(message)
		}
		runOnUi {
			listeners.forEach { it.get()?.onTelegramUserChanged(user) }
		}
	}

	override fun onTelegramError(code: Int, message: String) {
		runOnUi {
			Log.e(PlatformUtil.TAG, "Error: $code - $message")
			val error = getString(R.string.shared_string_error)
			val errorMessage = getString(R.string.ltr_or_rtl_combine_via_dash, code.toString(), message)
			val text = 	getString(R.string.ltr_or_rtl_combine_via_colon, error, errorMessage)
			Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
			listeners.forEach { it.get()?.onTelegramError(code, message) }
		}
	}

	override fun onReceiveChatLocationMessages(chatId: Long, vararg messages: TdApi.Message) {
		addGrayPhoto(chatId)
		if (!app.showLocationHelper.showingLocation && settings.hasAnyChatToShowOnMap()) {
			app.showLocationHelper.startShowingLocation()
		}
	}

	override fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>) {}

	override fun updateLocationMessages() {}

	override fun switchButtonsVisibility(visible: Boolean) {
		val buttonsVisibility = if (visible) View.VISIBLE else View.GONE
		if (buttonsBar.visibility != buttonsVisibility) {
			buttonsBar.visibility = buttonsVisibility
			bottomNav.visibility = if (visible) View.GONE else View.VISIBLE
		}
	}

	private fun removeNonexistingChatsFromSettings() {
		val presentChatTitles = telegramHelper.getChatIds()
		settings.removeNonexistingChats(presentChatTitles)
	}

	private fun stopShowingChatsOnMap(forceStop: Boolean) {
		settings.getShowOnMapChats().forEach { app.showLocationHelper.hideChatMessages(it) }
		app.showLocationHelper.stopShowingLocation(forceStop)
	}

	private fun closeApp() {
		app.stopSharingLocation()
		stopShowingChatsOnMap(true)
		finish()
		android.os.Process.killProcess(android.os.Process.myPid())
	}

	fun shareGpx(path: String) {
		val fileUri = AndroidUtils.getUriForFile(app, File(path))
		val sendIntent = Intent(Intent.ACTION_SEND)
		sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
		sendIntent.type = "application/gpx+xml"
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		startActivity(sendIntent)
	}

	fun loginTelegram() {
		if (telegramHelper.getTelegramAuthorizationState() != TelegramAuthorizationState.CLOSED) {
			telegramHelper.logout()
		}
		// FIXME: update UI
	}
	
	fun logoutTelegram(silent: Boolean = false) {
		if (telegramHelper.getTelegramAuthorizationState() == TelegramAuthorizationState.READY) {
			app.stopMonitoring()
			app.stopSharingLocation()
			if (app.isInternetConnectionAvailable) {
				app.locationMessages.clearBufferedMessages()
				settings.clear()
				telegramHelper.logout()
			} else {
				Toast.makeText(this, R.string.logout_no_internet_msg, Toast.LENGTH_SHORT).show()
			}
		} else if (!silent) {
			Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show()
		}
	}
	
	fun closeTelegram() {
		telegramHelper.close()
	}

	fun setupOptionsBtn(imageView: ImageView) {
		imageView.setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_other_menu))
		imageView.setOnClickListener { showOptionsPopupMenu(imageView) }
	}

	private fun shouldShowFreeTimelineInfo(): Boolean {
		val freeTimelineInfoShownTime = settings.freeTimelineInfoShownTime
		if (freeTimelineInfoShownTime != 0L) {
			val cal = Calendar.getInstance()
			val day = cal.get(Calendar.DAY_OF_MONTH)
			cal.timeInMillis = freeTimelineInfoShownTime
			return day != cal.get(Calendar.DAY_OF_MONTH)
		}
		return true
	}

	private fun showFreeTimelineInfo() {
		val snackbar = Snackbar.make(coordinatorLayout, R.string.timeline_available_for_free_now, Snackbar.LENGTH_LONG).setAction(R.string.shared_string_ok) {}
		AndroidUtils.setSnackbarTextColor(snackbar, R.color.ctrl_active_dark)
		snackbar.show()
		settings.freeTimelineInfoShownTime = System.currentTimeMillis()
	}

	private fun showOptionsPopupMenu(anchor: View) {
		val menuList = ArrayList<String>()
		val settings = getString(R.string.shared_string_settings)
		val login = getString(R.string.shared_string_login)
		val exit = getString(R.string.shared_string_exit)

		menuList.add(settings)
		if (telegramHelper.getTelegramAuthorizationState() == TelegramAuthorizationState.CLOSED) {
			menuList.add(login)
		}
		menuList.add(exit)

		ListPopupWindow(this@MainActivity).apply {
			isModal = true
			anchorView = anchor
			setContentWidth(AndroidUtils.getPopupMenuWidth(this@MainActivity, menuList))
			setDropDownGravity(Gravity.END or Gravity.TOP)
			setAdapter(ArrayAdapter(this@MainActivity, R.layout.popup_list_text_item, menuList))
			setOnItemClickListener { _, _, position, _ ->
				when (position) {
					menuList.indexOf(settings) -> {
						supportFragmentManager?.also { SettingsDialogFragment.showInstance(it) }
					}
					menuList.indexOf(login) -> loginTelegram()
					menuList.indexOf(exit) -> closeApp()
				}
				dismiss()
			}
			show()
		}
	}

	private fun addGrayPhoto(chatId: Long) {
		val chat = app.telegramHelper.getChat(chatId)
		val chatIconPath = chat?.photo?.small?.local?.path
		if (chat != null && chatIconPath != null) {
			addGrayPhoto(app.telegramHelper.getUserIdFromChatType(chat.type), chatIconPath)
		}
	}

	private fun addGrayPhoto(userId: Long, originalPhotoPath: String) {
		if (userId != 0L && !app.telegramHelper.hasGrayscaleUserPhoto(userId)) {
			app.uiUtils.convertAndSaveGrayPhoto(
				originalPhotoPath,
				"${app.filesDir.absolutePath}/$GRAYSCALE_PHOTOS_DIR$userId$GRAYSCALE_PHOTOS_EXT"
			)
		}
	}
	
	private fun runOnUi(action: (() -> Unit)) {
		runOnUiThread {
			if (!paused) {
				action()
			}
		}
	}

	private fun showLoginDialog(telegramAuthParamType: TelegramAuthParamType) {
		when (telegramAuthParamType) {
			TelegramAuthParamType.PHONE_NUMBER -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PHONE_NUMBER)
			TelegramAuthParamType.CODE -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_CODE)
			TelegramAuthParamType.PASSWORD -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PASSWORD)
		}
	}

	fun applyAuthParam(loginDialogFragment: LoginDialogFragment?, type: LoginDialogType, text: String) {
		if (TextUtils.isEmpty(text)) {
			Toast.makeText(this@MainActivity, "Authorization parameter is empty.", Toast.LENGTH_LONG).show()
			return
		}
		loginDialogFragment?.showProgress()
		when (type) {
			LoginDialogType.ENTER_PHONE_NUMBER -> telegramAuthorizationRequestHandler?.applyAuthParam(TelegramAuthParamType.PHONE_NUMBER, text)
			LoginDialogType.ENTER_CODE -> telegramAuthorizationRequestHandler?.applyAuthParam(TelegramAuthParamType.CODE, text)
			LoginDialogType.ENTER_PASSWORD -> telegramAuthorizationRequestHandler?.applyAuthParam(TelegramAuthParamType.PASSWORD, text)
			else -> {}
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (grantResults.isEmpty()) {
			return
		}
		when (requestCode) {
			PERMISSION_REQUEST_LOCATION -> {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if (settings.hasAnyChatToShareLocation()) {
						app.shareLocationHelper.startSharingLocation()
					}
					app.locationProvider.resumeAllUpdates()
				} else {
					settings.stopSharingLocationToChats()
					app.shareLocationHelper.stopSharingLocation()
				}
				if (settings.hasAnyChatToShowOnMap() && !app.isAnyOsmAndInstalled()) {
					showOsmandMissingDialog()
				}
			}
		}
	}

	fun refreshPages() {
		viewPager.adapter?.notifyDataSetChanged()
	}

	private fun showOsmandMissingDialog() {
		OsmandMissingDialogFragment().show(supportFragmentManager, null)
	}

	class OsmandMissingDialogFragment : DialogFragment() {

		override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
			val builder = AlertDialog.Builder(requireContext())
			builder.setView(R.layout.install_osmand_dialog)
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_install) { _, _ ->
						context?.also {
							startActivity(AndroidUtils.getPlayMarketIntent(it, OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME))
						}
					}
			return builder.create()
		}
	}

	class ViewPagerAdapter(fm: androidx.fragment.app.FragmentManager) : FragmentPagerAdapter(fm) {

		private val fragments = listOf<androidx.fragment.app.Fragment>(MyLocationTabFragment(), LiveNowTabFragment(), TimelineTabFragment())

		override fun getItem(position: Int) = fragments[position]

		override fun getCount() = fragments.size

		override fun getItemPosition(`object`: Any): Int {
			return androidx.viewpager.widget.PagerAdapter.POSITION_NONE
		}
	}
}
