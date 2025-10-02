package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.MapButtonsHelper.KEY_EVENT_KEY;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.SelectMapLocationAction;

public abstract class TakeMediaNoteAction extends SelectMapLocationAction {

	public TakeMediaNoteAction(@NonNull QuickActionType type) {
		super(type);
	}

	public TakeMediaNoteAction(@NonNull QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			if (plugin.isRecording()) {
				boolean showContextMenu = params == null || !params.containsKey(KEY_EVENT_KEY);
				plugin.stopRecording(mapActivity, false, showContextMenu);
			} else {
				super.execute(mapActivity, params);
			}
		}
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			takeNote(mapActivity, plugin, latLon);
		}
	}

	protected abstract void takeNote(@NonNull MapActivity mapActivity,
	                                 @NonNull AudioVideoNotesPlugin plugin, @NonNull LatLon latLon);
}
