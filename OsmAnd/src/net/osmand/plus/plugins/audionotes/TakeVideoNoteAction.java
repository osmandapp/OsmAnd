package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.QuickActionIds.TAKE_VIDEO_NOTE_ACTION_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakeVideoNoteAction extends TakeMediaNoteAction {

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
	protected void takeNote(@NonNull MapActivity mapActivity,
	                        @NonNull AudioVideoNotesPlugin plugin, @NonNull LatLon latLon) {
		plugin.recordVideo(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false);
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
