package net.osmand.plus.plugins.audionotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

public class AudioVideoNotesWidget extends TextInfoWidget {

	private Boolean cachedRecording;

	public AudioVideoNotesWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType, int actionId) {
		super(mapActivity, widgetType);

		updateInfo(null);
		setOnClickListener(v -> {
			AudioVideoNotesPlugin plugin = getPlugin();
			if (plugin != null) {
				if (plugin.isRecording()) {
					plugin.stopRecording(mapActivity, false);
				} else {
					plugin.makeAction(mapActivity, actionId);
				}
			}
		});
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		AudioVideoNotesPlugin plugin = getPlugin();
		if (plugin == null) {
			setText(null, null);
			return;
		}

		boolean recording = plugin.isRecording();
		if (!Algorithms.objectEquals(recording, cachedRecording)) {
			cachedRecording = recording;
			if (recording) {
				setText(getString(R.string.shared_string_control_stop), null);
				setIcons(R.drawable.widget_icon_av_active, R.drawable.widget_icon_av_active_night);
			} else {
				setText(getString(R.string.shared_string_control_start), null);
				setIcons(widgetType);
			}
		}
	}

	@Nullable
	private AudioVideoNotesPlugin getPlugin() {
		return PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
	}
}