package net.osmand.telegram.ui

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.osmand.telegram.*
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter

class SettingsDialogFragment : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val uiUtils get() = app.uiUtils
	private val telegramHelper get() = app.telegramHelper
	private val settings get() = app.settings

	private val gpsAndLocPrefs = listOf(SendMyLocPref(), StaleLocPref(), LocHistoryPref())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(android.support.v4.app.DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
	}

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

		var container = mainView.findViewById<ViewGroup>(R.id.gps_and_loc_container)
		for (pref in gpsAndLocPrefs) {
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
		for (appConn in AppConnect.values()) {
			val pack = appConn.appPackage
			val installed = AndroidUtils.isAppInstalled(context!!, pack)
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
						settings.appToConnectPackage = appConn.appPackage
						app.osmandAidlHelper.reconnectOsmand()
						updateSelectedAppConn()
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

		val user = telegramHelper.getCurrentUser()
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
			logoutTelegram()
			dismiss()
		}

		mainView.findViewById<ImageView>(R.id.help_icon)
			.setImageDrawable(uiUtils.getActiveIcon(R.drawable.ic_action_help))
		mainView.findViewById<View>(R.id.help_row).setOnClickListener {
			DisconnectTelegramBottomSheet.showInstance(childFragmentManager)
		}

		return mainView
	}

	private fun showPopupMenu(pref: DurationPref, valueView: TextView) {
		val menuList = pref.getMenuItems()
		val ctx = valueView.context
		ListPopupWindow(ctx).apply {
			isModal = true
			anchorView = valueView
			setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
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

	private fun logoutTelegram(silent: Boolean = false) {
		if (telegramHelper.getTelegramAuthorizationState() == TelegramHelper.TelegramAuthorizationState.READY) {
			telegramHelper.logout()
		} else if (!silent) {
			Toast.makeText(context, R.string.not_logged_in, Toast.LENGTH_SHORT).show()
		}
	}

	private inner class SendMyLocPref : DurationPref(
		R.drawable.ic_action_share_location,
		R.string.send_my_location,
		R.string.send_my_location_desc,
		SEND_MY_LOC_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, settings.sendMyLocInterval)

		override fun setCurrentValue(index: Int) {
			settings.sendMyLocInterval = values[index]
			app.updateSendLocationInterval()
		}
	}

	private inner class StaleLocPref : DurationPref(
		R.drawable.ic_action_time_span,
		R.string.stale_location,
		R.string.stale_location_desc,
		STALE_LOC_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, settings.staleLocTime)

		override fun setCurrentValue(index: Int) {
			settings.staleLocTime = values[index]
		}
	}

	private inner class LocHistoryPref : DurationPref(
		R.drawable.ic_action_location_history,
		R.string.location_history,
		R.string.location_history_desc,
		LOC_HISTORY_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, settings.locHistoryTime)

		override fun setCurrentValue(index: Int) {
			val value = values[index]
			settings.locHistoryTime = value
			telegramHelper.messageActiveTimeSec = value
		}
	}

	private abstract inner class DurationPref(
		@DrawableRes val iconId: Int,
		@StringRes val titleId: Int,
		@StringRes val descriptionId: Int,
		val values: List<Long>
	) {

		abstract fun getCurrentValue(): String

		abstract fun setCurrentValue(index: Int)

		fun getMenuItems() = values.map { OsmandFormatter.getFormattedDuration(app, it) }
	}

	enum class AppConnect(
		@DrawableRes val iconId: Int,
		@DrawableRes val whiteIconId: Int,
		val title: String,
		val appPackage: String
	) {
		OSMAND_PLUS(
			R.drawable.ic_logo_osmand_plus,
			R.drawable.ic_action_osmand_plus,
			"OsmAnd+",
			OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME
		),
		OSMAND_FREE(
			R.drawable.ic_logo_osmand_free,
			R.drawable.ic_action_osmand_free,
			"OsmAnd",
			OsmandAidlHelper.OSMAND_FREE_PACKAGE_NAME
		);

		companion object {

			@DrawableRes
			fun getWhiteIconId(appPackage: String): Int {
				for (item in values()) {
					if (item.appPackage == appPackage) {
						return item.whiteIconId
					}
				}
				return 0
			}
		}
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
