package net.osmand.plus.plugins.audionotes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_ON_REQUEST;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_RECORD_AUDIO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_RECORD_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_TAKE_PHOTO;

public class AudioVideoNotesWidget extends TextInfoWidget {

	private final int actionId;

	private Boolean cachedRecording;

	public AudioVideoNotesWidget(@NonNull MapActivity mapActivity, int actionId) {
		super(mapActivity);
		this.actionId = actionId;

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
				switch (actionId) {
					case AV_DEFAULT_ACTION_AUDIO:
						setIcons(AV_NOTES_RECORD_AUDIO);
						break;
					case AV_DEFAULT_ACTION_VIDEO:
						setIcons(AV_NOTES_RECORD_VIDEO);
						break;
					case AV_DEFAULT_ACTION_TAKEPICTURE:
						setIcons(AV_NOTES_TAKE_PHOTO);
						break;
					default:
						setIcons(AV_NOTES_ON_REQUEST);
						break;
				}
			}
		}
	}

	@Nullable
	private AudioVideoNotesPlugin getPlugin() {
		return OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
	}
}