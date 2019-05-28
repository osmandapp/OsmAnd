package net.osmand.telegram.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.TelegramSettings.NumericPref
import net.osmand.telegram.helpers.TelegramHelper.Companion.OSMAND_BOT_USERNAME
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandApiUtils
import org.drinkless.td.libcore.telegram.TdApi
import org.json.JSONObject

class SettingsDialogFragment : BaseDialogFragment() {

	private val uiUtils get() = app.uiUtils
	private lateinit var shareAsContainer: ViewGroup
	private lateinit var shareAsDescription: TextView

	private var shareAsDescriptionHidden = true

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val mainView = inflater.inflate(R.layout.fragement_settings_dialog, parent)

		mainView.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener { dismiss() }
		}
		val window = dialog.window
		if (window != null && Build.VERSION.SDK_INT >= 21) {
			window.statusBarColor = ContextCompat.getColor(app, R.color.card_bg_light)
		}
		var container = mainView.findViewById<ViewGroup>(R.id.gps_and_loc_container)
		settings.gpsAndLocPrefs.forEach {
			createNumericPref(inflater, container, it)
		}

		if (Build.VERSION.SDK_INT >= 26) {
			inflater.inflate(R.layout.item_with_desc_and_right_value, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_background_work))
				findViewById<TextView>(R.id.title).text = getText(R.string.background_work)
				findViewById<TextView>(R.id.description).text = getText(R.string.background_work_description)
				findViewById<TextView>(R.id.value).visibility = View.GONE
				setOnClickListener {
					fragmentManager?.also { BatteryOptimizationBottomSheet.showInstance(it) }
				}
				container.addView(this)
			}
		}

		container = mainView.findViewById<ViewGroup>(R.id.gps_points_container)
		inflater.inflate(R.layout.item_with_descr_and_right_switch, container, false).apply {
			findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_connect))
			findViewById<TextView>(R.id.title).text = getText(R.string.show_gps_points)
			findViewById<TextView>(R.id.description).text = getText(R.string.show_gps_points_descr)
			val switcher = findViewById<Switch>(R.id.switcher).apply {
				isChecked = app.settings.showGpsPoints
			}
			setOnClickListener {
				val checked = !app.settings.showGpsPoints
				app.settings.showGpsPoints = checked
				switcher.isChecked = checked
			}
			container.addView(this)
		}

		container = mainView.findViewById<ViewGroup>(R.id.proxy_settings_container)
		inflater.inflate(R.layout.item_with_descr_and_right_switch, container, false).apply {
			findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_proxy))
			findViewById<ImageView>(R.id.icon_right).apply {
				visibility = View.VISIBLE
				setImageDrawable(uiUtils.getThemedIcon(R.drawable.ic_action_additional_option))
				setOnClickListener {
					activity?.supportFragmentManager?.also { ProxySettingsDialogFragment.showInstance(it, this@SettingsDialogFragment) }
				}
			}
			findViewById<TextView>(R.id.title).text = getText(R.string.proxy)
			val description = findViewById<TextView>(R.id.description).apply {
				text = if (settings.proxyEnabled) getText(R.string.proxy_connected) else getText(R.string.proxy_disconnected)
			}
			val switcher = findViewById<Switch>(R.id.switcher).apply {
				isChecked = app.settings.proxyEnabled
			}
			setOnClickListener {
				val checked = !app.settings.proxyEnabled
				switcher.isChecked = checked
				settings.updateProxySetting(checked)
				description.text = if (checked) getText(R.string.proxy_connected) else getText(R.string.proxy_disconnected)
			}
			container.addView(this)
		}

		shareAsDescription = mainView.findViewById<TextView>(R.id.share_as_description).apply {
			text = getText(R.string.share_location_as_description)
			setOnClickListener {
				updateShareAsDescription()
			}
		}

		shareAsContainer = mainView.findViewById(R.id.share_as_container)
		val user = telegramHelper.getCurrentUser()
		if (user != null) {
			addItemToContainer(inflater, shareAsContainer, user.id.toString(),  TelegramUiHelper.getUserName(user))
		}
		settings.getShareDevices().forEach {
			addItemToContainer(inflater, shareAsContainer, it.externalId, it.deviceName)
		}

		mainView.findViewById<TextView>(R.id.add_new_device_title)
			.setTextColor(AndroidUtils.createPressedColorStateList(app, true, R.color.ctrl_active_light, R.color.ctrl_light))

		mainView.findViewById<ImageView>(R.id.add_new_device_icon)
			.setImageDrawable(getAddNewDeviceIcon())

		mainView.findViewById<LinearLayout>(R.id.add_new_device_btn).apply {
			setOnClickListener {
				fragmentManager?.also { fm ->
					AddNewDeviceBottomSheet.showInstance(fm, this@SettingsDialogFragment)
				}
			}
		}

		container = mainView.findViewById<ViewGroup>(R.id.gpx_settings_container)
		settings.gpxLoggingPrefs.forEach {
			createNumericPref(inflater, container, it)
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
			AddNewDeviceBottomSheet.NEW_DEVICE_REQUEST_CODE -> {
				val user = app.telegramHelper.getCurrentUser()
				if (user != null && data != null && data.hasExtra(AddNewDeviceBottomSheet.DEVICE_JSON)) {
					val deviceJson = data.getStringExtra(AddNewDeviceBottomSheet.DEVICE_JSON)
					val device = OsmandApiUtils.parseDeviceBot(JSONObject(deviceJson))
					if (device != null) {
						app.settings.addShareDevice(device)
						val inflater = activity?.layoutInflater
						if (inflater != null) {
							addItemToContainer(inflater, shareAsContainer, device.externalId, device.deviceName)
							Toast.makeText(app, getString(R.string.device_added_successfully, device.deviceName), Toast.LENGTH_SHORT).show()
						}
					}
				}
			}
			ProxySettingsDialogFragment.PROXY_PREFERENCES_UPDATED_REQUEST_CODE -> {
				view?.findViewById<ViewGroup>(R.id.proxy_settings_container)?.apply {
					findViewById<TextView>(R.id.description)?.text = if (settings.proxyEnabled) getText(R.string.proxy_connected) else getText(R.string.proxy_disconnected)
					findViewById<Switch>(R.id.switcher)?.isChecked = app.settings.proxyEnabled
				}
			}
		}
	}

	private fun createNumericPref(inflater: LayoutInflater, container: ViewGroup, pref: NumericPref) {
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

	private fun addItemToContainer(inflater: LayoutInflater, container: ViewGroup, tag: String, title: String) {
		inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
			val checked = tag == settings.currentSharingMode

			setupSharingModeIcon(this, checked, telegramHelper.getCurrentUser(), tag)

			findViewById<TextView>(R.id.title).text = title
			findViewById<View>(R.id.primary_btn).visibility = View.GONE
			findViewById<RadioButton>(R.id.radio_button).apply {
				visibility = View.VISIBLE
				isChecked = checked
			}
			setOnClickListener {
				settings.updateCurrentSharingMode(tag)
				updateSelectedSharingMode()
			}
			this.tag = tag
			container.addView(this)
		}
	}
	
	private fun showPopupMenu(pref: NumericPref, valueView: TextView) {
		val menuList = pref.getMenuItems()
		val ctx = valueView.context
		ListPopupWindow(ctx).apply {
			isModal = true
			anchorView = valueView
			setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
			height = if (menuList.size < 6) {
				ListPopupWindow.WRAP_CONTENT
			} else {
				AndroidUtils.getPopupMenuHeight(ctx)
			}
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

	private fun setupSharingModeIcon(view: View, checked: Boolean, user: TdApi.User?, tag: String) {
		if (tag == user?.id.toString()) {
			val path = if (checked) {
				telegramHelper.getUserPhotoPath(user)
			} else {
				telegramHelper.getUserGreyPhotoPath(user)
			}
			TelegramUiHelper.setupPhoto(app, view.findViewById<ImageView>(R.id.icon), path, R.drawable.img_user_picture, false)
		} else {
			val icon = if (checked) {
				uiUtils.getActiveIcon(R.drawable.ic_device_picture)
			} else {
				uiUtils.getThemedIcon(R.drawable.ic_device_picture)
			}
			view.findViewById<ImageView>(R.id.icon).setImageDrawable(icon)
		}
	}

	private fun updateSelectedSharingMode() {
		view?.findViewById<ViewGroup>(R.id.share_as_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					val checked = tag == app.settings.currentSharingMode
					setupSharingModeIcon(this, checked, telegramHelper.getCurrentUser(), tag.toString())
					findViewById<RadioButton>(R.id.radio_button).isChecked = checked
				}
			}
		}
	}

	private fun updateShareAsDescription() {
		if (shareAsDescriptionHidden) {
			shareAsDescription.text = getFullShareAsDescriptionText()
		} else {
			shareAsDescription.text = getText(R.string.share_location_as_description)
		}
		shareAsDescriptionHidden = !shareAsDescriptionHidden
	}

	private fun getFullShareAsDescriptionText(): CharSequence {
		val textHide = "${getString(R.string.shared_string_hide)}."
		val spannableString = SpannableStringBuilder(getText(R.string.share_location_as_description))
		val newSpannable = SpannableStringBuilder(getString(R.string.share_location_as_description_second_line, OSMAND_BOT_USERNAME, textHide))

		spannableString.append("\n\n")

		var startIndex = newSpannable.indexOf(OSMAND_BOT_USERNAME)
		var endIndex = startIndex + OSMAND_BOT_USERNAME.length
		newSpannable.setSpan(ForegroundColorSpan(app.uiUtils.getActiveColor()), startIndex, endIndex, 0)

		startIndex = newSpannable.indexOf(textHide)
		endIndex = startIndex + textHide.length
		newSpannable.setSpan(ForegroundColorSpan(app.uiUtils.getActiveColor()), startIndex, endIndex, 0)

		spannableString.append(newSpannable)

		return spannableString
	}

	private fun getAddNewDeviceIcon(): Drawable? {
		val normal = app.uiUtils.getActiveIcon(R.drawable.ic_action_add)
		if (Build.VERSION.SDK_INT >= 21) {
			val active = app.uiUtils.getIcon(R.drawable.ic_action_add, R.color.ctrl_light)
			if (normal != null && active != null) {
				return AndroidUtils.createPressedStateListDrawable(normal, active)
			}
		}
		return normal
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
