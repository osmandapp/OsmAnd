package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.QuickActionIds.TAKE_VIDEO_NOTE_ACTION_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.SelectMapLocationAction;

public class TakeVideoNoteAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(TAKE_VIDEO_NOTE_ACTION_ID,
			"video.note", TakeVideoNoteAction.class).
			nameRes(R.string.quick_action_video_note).iconRes(R.drawable.ic_action_video_dark).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);


	public TakeVideoNoteAction() {
		super(TYPE);
	}

	public TakeVideoNoteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			super.execute(mapActivity, params);
		}
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			plugin.recordVideo(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false);
		}
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		AudioNotesLayer layer = plugin != null ? plugin.getAudioNotesLayer() : null;
		return layer != null ? layer.getVideoNoteIcon() : null;
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_take_video_note_descr);
	}

}
