package net.osmand.plus.plugins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.MaterialToolbar
import net.osmand.aidl.AidlPermissionGroup
import net.osmand.aidl.ConnectedApp
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.MainSwitchView
import net.osmand.plus.widgets.ui.ScreenDescriptionView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

/**
 * Permissions of an app connected over the AIDL API (Menu > Plugins): whether OsmAnd answers it
 * at all, and the permission groups it may use. Changes apply to the next call of the app.
 */
class ConnectedAppPermissionsFragment : BaseMaterialFragment() {

	private lateinit var connectedApp: ConnectedApp
	private lateinit var mainSwitch: MainSwitchView
	private lateinit var disabledFooter: GroupFooterView
	private lateinit var content: View
	private val rows = LinkedHashMap<AidlPermissionGroup, SettingRow>()

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pack = requireArguments().getString(PACK_KEY) ?: ""
		connectedApp = osmandApp.aidlApi.getOrCreateConnectedApp(pack)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.fragment_connected_app_permissions, container, false)
		val name = connectedApp.name ?: connectedApp.pack

		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.title = name
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

		view.findViewById<ScreenDescriptionView>(R.id.description)
			.setText(getString(R.string.aidl_permissions_descr, name, connectedApp.pack))

		mainSwitch = view.findViewById(R.id.main_switch)
		mainSwitch.setLabel(name, true)
		mainSwitch.setChecked(connectedApp.isEnabled, false)
		mainSwitch.setOnCheckedChangeListener { checked ->
			// through the Plugins screen: it also turns the app's plugin on/off and refreshes its list
			val pluginsFragment = getPluginsFragment()
			if (pluginsFragment != null) {
				pluginsFragment.setAppEnabled(connectedApp, checked)
			} else {
				osmandApp.aidlApi.setAppEnabled(activity, connectedApp, checked)
			}
			updateContent()
		}
		disabledFooter = view.findViewById(R.id.disabled_footer)
		content = view.findViewById(R.id.content)

		val iconColor = AndroidUtils.getColorFromAttr(view.context, R.attr.colorOnSurfaceVariant)
		val warningColor = AndroidUtils.getColorFromAttr(view.context, R.attr.colorError)
		val groupsView: ViewGroup = view.findViewById(R.id.groups)
		val systemView: ViewGroup = view.findViewById(R.id.system_group)
		for (group in AidlPermissionGroup.values()) {
			val parent = if (group.isSensitive) systemView else groupsView
			val rowView = inflater.inflate(R.layout.item_ui_setting_row, parent, false)
			val row = SettingRow(rowView)
			row.setIcon(group.iconId, if (group.isSensitive) warningColor else iconColor)
			row.setTitle(group.titleId)
			row.setSubtitle(getString(group.descriptionId))
			row.setOnClickListener {
				osmandApp.aidlApi.setGroupGranted(connectedApp, group, !connectedApp.isGroupGranted(group))
				getPluginsFragment()?.onConnectedAppChanged()
				updateContent()
			}
			parent.addView(rowView)
			rows[group] = row
		}
		SegmentedList.apply(groupsView)
		SegmentedList.apply(systemView)
		updateContent()
		return view
	}

	/* every row has the same switch id, so the restored view state would give all of them the
	 * state of the last one - the stored permissions win */
	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)
		updateContent()
	}

	private fun updateContent() {
		val enabled = connectedApp.isEnabled
		mainSwitch.setChecked(enabled, false)
		// off: the groups are hidden, not disabled (ui-guidelines, MainSwitchView)
		content.visibility = if (enabled) View.VISIBLE else View.GONE
		disabledFooter.visibility = if (enabled) View.GONE else View.VISIBLE
		for ((group, row) in rows) {
			row.setChecked(connectedApp.isGroupGranted(group))
		}
	}

	private fun getPluginsFragment(): PluginsFragment? =
		parentFragmentManager.findFragmentByTag(PluginsFragment.TAG) as? PluginsFragment

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		// the app bar below the status bar, the content scrolls under the navigation bar
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	companion object {
		private val TAG = ConnectedAppPermissionsFragment::class.java.simpleName
		private const val PACK_KEY = "pack"

		@JvmStatic
		fun showInstance(manager: FragmentManager, connectedApp: ConnectedApp) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ConnectedAppPermissionsFragment()
				fragment.arguments = Bundle().apply { putString(PACK_KEY, connectedApp.pack) }
				manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss()
			}
		}
	}
}
