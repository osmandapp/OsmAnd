package net.osmand.plus.plugins.drone

import android.view.View
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import net.osmand.render.RenderingRuleProperty

class DroneZonesPlugin(app: OsmandApplication) : OsmandPlugin(app) {
	companion object {
		const val ID = "osmand.drone.zones"
		const val RENDER_PROPERTY = "droneZones"
	}

	override fun getId(): String = ID

	override fun getName(): String = app.getString(R.string.drone_zones)

	override fun getDescription(linksEnabled: Boolean): CharSequence =
		app.getString(R.string.drone_zones_plugin_description)

	override fun getLogoResourceId(): Int = R.drawable.ic_action_layers

	override fun isEnableByDefault(): Boolean = true

	override fun getRenderPropertyPrefix(): String = "drone"

	override fun registerLayerContextMenuActions(
		adapter: ContextMenuAdapter,
		mapActivity: MapActivity,
		customRules: MutableList<RenderingRuleProperty>,
	) {
		val property = customRules.firstOrNull { it.attrName == RENDER_PROPERTY } ?: return
		customRules.remove(property)
		val preference = settings.getCustomRenderBooleanProperty(RENDER_PROPERTY)
		val selected = preference.get()
		adapter.addItem(
			ContextMenuItem(ID)
				.setTitleId(R.string.drone_zones, mapActivity)
				.setSecondaryDescription(app.getString(if (selected) R.string.shared_string_on else R.string.shared_string_off))
				.setSelected(selected)
				.setColor(app, if (selected) R.color.osmand_orange else ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_layers)
				.setListener(toggleListener(preference, mapActivity)),
		)
	}

	private fun toggleListener(preference: CommonPreference<Boolean>, mapActivity: MapActivity) = object : OnRowItemClick() {
		override fun onRowItemClick(uiAdapter: OnDataChangeUiAdapter, view: View, item: ContextMenuItem): Boolean = false

		override fun onContextMenuClick(
			uiAdapter: OnDataChangeUiAdapter?,
			view: View?,
			item: ContextMenuItem,
			isChecked: Boolean,
		): Boolean {
			preference.set(isChecked)
			item.setSelected(isChecked)
			item.setColor(app, if (isChecked) R.color.osmand_orange else ContextMenuItem.INVALID_ID)
			item.setSecondaryDescription(app.getString(if (isChecked) R.string.shared_string_on else R.string.shared_string_off))
			uiAdapter?.onDataSetChanged()
			mapActivity.refreshMapComplete()
			return true
		}
	}
}
