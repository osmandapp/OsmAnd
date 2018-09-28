package net.osmand.telegram.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.TelegramSettings.DurationPref
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils

class SettingsDialogFragment : BaseDialogFragment() {

	private val uiUtils get() = app.uiUtils
	private lateinit var mainView: View

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(R.layout.fragement_settings_dialog, parent)

		val appBarLayout = mainView.findViewById<View>(R.id.app_bar_layout)
		AndroidUtils.addStatusBarPadding19v(context!!, appBarLayout)

		mainView.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener { dismiss() }
		}

		var container = mainView.findViewById<ViewGroup>(R.id.gps_and_loc_container)
		for (pref in settings.gpsAndLocPrefs) {
			inflater.inflate(R.layout.item_with_desc_and_right_value, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(pref.iconId))
				findViewById<TextView>(R.id.title).setText(pref.titleId)
				findViewById<TextView>(R.id.description).setText(pref.descriptionId)
				val valueView = findViewById<TextView>(R.id.value)
				valueView.text = pref.getCurrentValue()
				setOnClickListener {
					showPopupMenu(pref, valueView)
				}
				container.addView(this)
			}
		}

		container = mainView.findViewById(R.id.osmand_connect_container)
		for (appConn in TelegramSettings.AppConnect.values()) {
			val pack = appConn.appPackage
			val installed = AndroidUtils.isAppInstalled(context!!, pack)
			if (!installed && appConn.showOnlyInstalled) {
				continue
			}
			inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getIcon(appConn.iconId))
				findViewById<TextView>(R.id.title).text = appConn.title
				if (installed) {
					findViewById<View>(R.id.primary_btn).visibility = View.GONE
					findViewById<RadioButton>(R.id.radio_button).apply {
						visibility = View.VISIBLE
						isChecked = pack == settings.appToConnectPackage
					}
					setOnClickListener {
						if (settings.appToConnectPackage != appConn.appPackage) {
							settings.updateAppToConnect(appConn.appPackage)
							updateSelectedAppConn()
						}
					}
				} else {
					findViewById<RadioButton>(R.id.radio_button).visibility = View.GONE
					findViewById<TextView>(R.id.primary_btn).apply {
						setText(R.string.shared_string_install)
						setOnClickListener {
							context?.also { ctx ->
								startActivity(AndroidUtils.getPlayMarketIntent(ctx, pack))
							}
						}
					}
					setOnClickListener(null)
					isClickable = false
				}
				tag = pack
				container.addView(this)
			}
		}
		updateSelectedAppConn()

		container = mainView.findViewById(R.id.share_as_container)
		if (settings.shareDevicesIds.isEmpty()) {
			val user = telegramHelper.getCurrentUser()
			if (user != null) {
				settings.addSharingDevice(TelegramUiHelper.getUserName(user))
//				settings.currentSharingMode = TelegramUiHelper.getUserName(user)
			}
		}
		val user = telegramHelper.getCurrentUser()
		settings.shareDevicesIds.forEach {
			val title = it
			inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
				findViewById<TextView>(R.id.title).text = title
				findViewById<View>(R.id.primary_btn).visibility = View.GONE
				findViewById<RadioButton>(R.id.radio_button).apply {
					visibility = View.VISIBLE
					isChecked = it == settings.currentSharingMode
				}
				setOnClickListener {
					if (user != null && TelegramUiHelper.getUserName(user) == title) {
						settings.currentSharingMode = null
					} else {
						settings.currentSharingMode = title
					}
					updateSelectedSharingMode()
				}
				tag = it
				container.addView(this)
			}
		}

		mainView.findViewById<TextView>(R.id.add_device).setOnClickListener {
			val fm = fragmentManager
			if (fm != null) {
				AddDeviceBottomSheet.showInstance(fm, this)
			}
		}

		if (user != null) {
			TelegramUiHelper.setupPhoto(
				app,
				mainView.findViewById(R.id.user_icon),
				telegramHelper.getUserPhotoPath(user),
				R.drawable.img_user_picture,
				false
			)
			mainView.findViewById<TextView>(R.id.username).text = TelegramUiHelper.getUserName(user)
		} else {
			mainView.findViewById<View>(R.id.user_row).visibility = View.GONE
		}

		mainView.findViewById<View>(R.id.logout_btn).setOnClickListener {
			fragmentManager?.also { fm ->
				LogoutBottomSheet.showInstance(fm, this)
			}

		}

		mainView.findViewById<ImageView>(R.id.help_icon)
			.setImageDrawable(uiUtils.getActiveIcon(R.drawable.ic_action_help))
		mainView.findViewById<View>(R.id.help_row).setOnClickListener {
			DisconnectTelegramBottomSheet.showInstance(childFragmentManager)
		}

		return mainView
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			LogoutBottomSheet.LOGOUT_REQUEST_CODE -> {
				logoutTelegram()
				dismiss()
			}
			AddDeviceBottomSheet.ADD_DEVICE_REQUEST_CODE -> {
				if (data != null && data.hasExtra(AddDeviceBottomSheet.NEW_DEVICE_ID)) {
					addNewSharingDevice(data.getStringExtra(AddDeviceBottomSheet.NEW_DEVICE_ID))
				}
			}
		}
	}
	
	private fun showPopupMenu(pref: DurationPref, valueView: TextView) {
		val menuList = pref.getMenuItems()
		val ctx = valueView.context
		ListPopupWindow(ctx).apply {
			isModal = true
			anchorView = valueView
			setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
			height = AndroidUtils.getPopupMenuHeight(ctx)
			setDropDownGravity(Gravity.END or Gravity.TOP)
			setAdapter(ArrayAdapter(ctx, R.layout.popup_list_text_item, menuList))
			setOnItemClickListener { _, _, position, _ ->
				pref.setCurrentValue(position)
				valueView.text = pref.getCurrentValue()
				dismiss()
			}
			show()
		}
	}

	private fun updateSelectedAppConn() {
		view?.findViewById<ViewGroup>(R.id.osmand_connect_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					findViewById<RadioButton>(R.id.radio_button).isChecked =
							tag == settings.appToConnectPackage
				}
			}
		}
	}

	private fun updateSelectedSharingMode() {
		view?.findViewById<ViewGroup>(R.id.share_as_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					findViewById<RadioButton>(R.id.radio_button).isChecked =
							tag == settings.currentSharingMode
				}
			}
		}
	}

	private fun addNewSharingDevice(title: String) {
		val inflater = LayoutInflater.from(context)
		val container = mainView.findViewById<ViewGroup>(R.id.share_as_container)
		inflater.inflate(R.layout.item_with_rb_and_btn, null, false).apply {
			findViewById<TextView>(R.id.title).text = title
			findViewById<View>(R.id.primary_btn).visibility = View.GONE
			findViewById<RadioButton>(R.id.radio_button).apply {
				visibility = View.VISIBLE
				isChecked = title == settings.currentSharingMode
			}
			setOnClickListener {
				settings.currentSharingMode = title
				updateSelectedSharingMode()
			}
			tag = title
			container.addView(this)
		}
	}

	private fun logoutTelegram() {
		val act = activity ?: return
		(act as MainActivity).logoutTelegram()
	}

	companion object {

		private const val TAG = "SettingsDialogFragment"

		fun showInstance(fm: FragmentManager): Boolean {
			return try {
				SettingsDialogFragment().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
