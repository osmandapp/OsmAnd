package net.osmand.plus.plugins.monitoring.actions;

import androidx.annotation.Nullable;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

abstract class BaseMonitoringAction extends QuickAction {

	public BaseMonitoringAction(QuickActionType actionType) {
		super(actionType);
	}

	public BaseMonitoringAction(QuickAction quickAction) {
		super(quickAction);
	}

	protected boolean isRecordingTrack() {
		OsmandMonitoringPlugin plugin = getPlugin();
		return plugin != null && plugin.isRecordingTrack();
	}

	protected boolean hasDataToSave() {
		OsmandMonitoringPlugin plugin = getPlugin();
		return plugin != null && plugin.hasDataToSave();
	}

	@Nullable
	protected OsmandMonitoringPlugin getPlugin() {
		return PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
	}
}
