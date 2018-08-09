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
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.OsmandAidlHelper
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
			inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(appConn.iconId))
				findViewById<TextView>(R.id.title).text = appConn.title
				// FIXME
				container.addView(this)
			}
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

	// FIXME
	private inner class SendMyLocPref : DurationPref(
		R.drawable.ic_action_share_location,
		R.string.send_my_location,
		R.string.send_my_location_desc,
		listOf(30 * 60, 60 * 60, 90 * 60)
	) {

		override fun getCurrentValue() = OsmandFormatter.getFormattedDuration(app, values[0],false)

		override fun setCurrentValue(index: Int) {
			val value = OsmandFormatter.getFormattedDuration(app, values[index],false)
			Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
		}
	}

	// FIXME
	private inner class StaleLocPref : DurationPref(
		R.drawable.ic_action_share_location,
		R.string.stale_location,
		R.string.stale_location_desc,
		listOf(30 * 60, 60 * 60, 90 * 60)
	) {

		override fun getCurrentValue() = OsmandFormatter.getFormattedDuration(app, values[0],false)

		override fun setCurrentValue(index: Int) {
			val value = OsmandFormatter.getFormattedDuration(app, values[index],false)
			Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
		}
	}

	// FIXME
	private inner class LocHistoryPref : DurationPref(
		R.drawable.ic_action_time_span,
		R.string.location_history,
		R.string.location_history_desc,
		listOf(30 * 60, 60 * 60, 90 * 60)
	) {

		override fun getCurrentValue() = OsmandFormatter.getFormattedDuration(app, values[0],false)

		override fun setCurrentValue(index: Int) {
			val value = OsmandFormatter.getFormattedDuration(app, values[index],false)
			Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
		}
	}

	private abstract inner class DurationPref(
		@DrawableRes val iconId: Int,
		@StringRes val titleId: Int,
		@StringRes val descriptionId: Int,
		val values: List<Int>
	) {

		abstract fun getCurrentValue(): String

		abstract fun setCurrentValue(index: Int)

		fun getMenuItems() = values.map { OsmandFormatter.getFormattedDuration(app, it, false) }
	}

	private enum class AppConnect(
		@DrawableRes val iconId: Int,
		val title: String,
		val appPackage: String
	) {
		OSMAND_PLUS(
			R.drawable.ic_action_osmand_plus,
			"OsmAnd+",
			OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME
		),
		OSMAND_FREE(
			R.drawable.ic_action_osmand_free,
			"OsmAnd",
			OsmandAidlHelper.OSMAND_FREE_PACKAGE_NAME
		)
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
