package net.osmand.telegram.ui

import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ListPopupWindow
import android.view.Gravity
import android.view.View
import android.widget.*
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.ui.LoginDialogFragment.LoginDialogType
import net.osmand.telegram.ui.MyLocationTabFragment.ActionButtonsListener
import net.osmand.telegram.ui.views.LockableViewPager
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.lang.ref.WeakReference

private const val PERMISSION_REQUEST_LOCATION = 1

private const val MY_LOCATION_TAB_POS = 0
private const val LIVE_NOW_TAB_POS = 1

class MainActivity : AppCompatActivity(), TelegramListener, ActionButtonsListener {

	private val log = PlatformUtil.getLog(TelegramHelper::class.java)

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

	private lateinit var buttonsBar: LinearLayout
	private lateinit var bottomNav: BottomNavigationView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		paused = false

		val viewPager = findViewById<LockableViewPager>(R.id.view_pager).apply {
			swipeLocked = true
			offscreenPageLimit = 2
			adapter = ViewPagerAdapter(supportFragmentManager)
		}

		bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation).apply {
			setOnNavigationItemSelectedListener {
				var pos = -1
				when (it.itemId) {
					R.id.action_my_location -> pos = MY_LOCATION_TAB_POS
					R.id.action_live_now -> pos = LIVE_NOW_TAB_POS
				}
				if (pos != -1 && pos != viewPager.currentItem) {
					when (pos) {
						MY_LOCATION_TAB_POS -> liveNowTabFragment?.tabClosed()
						LIVE_NOW_TAB_POS -> liveNowTabFragment?.tabOpened()
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
		telegramHelper.requestAuthorizationState()

		if (osmandAidlHelper.isOsmandBound() && !osmandAidlHelper.isOsmandConnected()) {
			osmandAidlHelper.connectOsmand()
		}
	}

	override fun onAttachFragment(fragment: Fragment?) {
		if (fragment is TelegramListener) {
			listeners.add(WeakReference(fragment))
		}
		if (fragment is MyLocationTabFragment) {
			myLocationTabFragment = fragment
		} else if (fragment is LiveNowTabFragment) {
			liveNowTabFragment = fragment
		}
	}

	override fun onResume() {
		super.onResume()
		paused = false

		if (telegramHelper.listener != this) {
			telegramHelper.listener = this
		}

		app.locationProvider.checkIfLastKnownLocationIsValid()

		if (AndroidUtils.isLocationPermissionAvailable(this)) {
			app.locationProvider.resumeAllUpdates()
		} else {
			AndroidUtils.requestLocationPermission(this)
		}
		if (settings.hasAnyChatToShowOnMap() && osmandAidlHelper.isOsmandNotInstalled()) {
			showOsmandMissingDialog()
		}
	}

	override fun onPause() {
		super.onPause()
		telegramHelper.listener = null

		app.locationProvider.pauseAllUpdates()

		paused = true
	}

	override fun onStop() {
		super.onStop()
		settings.save()
		app.messagesDbHelper.saveMessages()
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
				TelegramAuthorizationState.READY -> LoginDialogFragment.dismiss(fm)
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
		runOnUi {
			listeners.forEach { it.get()?.onTelegramChatsChanged() }
		}
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		runOnUi {
			listeners.forEach { it.get()?.onTelegramChatChanged(chat) }
		}
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		val message = telegramHelper.getUserMessage(user)
		if (message != null) {
			app.showLocationHelper.addLocationToMap(message)
		}
		runOnUi {
			listeners.forEach { it.get()?.onTelegramUserChanged(user) }
		}
	}

	override fun onTelegramError(code: Int, message: String) {
		runOnUi {
			Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
			listeners.forEach { it.get()?.onTelegramError(code, message) }
		}
	}

	override fun onSendLiveLocationError(code: Int, message: String) {
		log.error("Send live location error: $code - $message")
		app.isInternetConnectionAvailable(true)
		runOnUi {
			listeners.forEach { it.get()?.onSendLiveLocationError(code, message) }
		}
	}

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

	fun loginTelegram() {
		if (telegramHelper.getTelegramAuthorizationState() != TelegramAuthorizationState.CLOSED) {
			telegramHelper.logout()
		}
		// FIXME: update UI
	}
	
	private fun logoutTelegram(silent: Boolean = false) {
		if (telegramHelper.getTelegramAuthorizationState() == TelegramHelper.TelegramAuthorizationState.READY) {
			telegramHelper.logout()
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

	fun showOptionsPopupMenu(anchor: View) {
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
					menuList.indexOf(logout) -> logoutTelegram()
					menuList.indexOf(login) -> loginTelegram()
				}
				dismiss()
			}
			show()
		}
	}
	
	private fun runOnUi(action: (() -> Unit)) {
		if (!paused) {
			runOnUiThread(action)
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
		loginDialogFragment?.showProgress()
		when (loginDialogType) {
			LoginDialogType.ENTER_PHONE_NUMBER -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PHONE_NUMBER, text)
			LoginDialogType.ENTER_CODE -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.CODE, text)
			LoginDialogType.ENTER_PASSWORD -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PASSWORD, text)
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
				if (settings.hasAnyChatToShowOnMap() && osmandAidlHelper.isOsmandNotInstalled()) {
					showOsmandMissingDialog()
				}
			}
		}
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

	class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

		private val fragments = listOf<Fragment>(MyLocationTabFragment(), LiveNowTabFragment())

		override fun getItem(position: Int) = fragments[position]

		override fun getCount() = fragments.size
	}
}
