package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.QuickActionIds.TAKE_AUDIO_NOTE_ACTION_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakeAudioNoteAction extends TakeMediaNoteAction {

	public static final QuickActionType TYPE = new QuickActionType(TAKE_AUDIO_NOTE_ACTION_ID,
			"audio.note", TakeAudioNoteAction.class).
			nameRes(R.string.quick_action_audio_note).iconRes(R.drawable.ic_action_micro_dark).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);

	public TakeAudioNoteAction() {
		super(TYPE);
	}

	public TakeAudioNoteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void takeNote(@NonNull MapActivity mapActivity,
	                        @NonNull AudioVideoNotesPlugin plugin, @NonNull LatLon latLon) {
		plugin.recordAudio(latLon.getLatitude(), latLon.getLongitude(), mapActivity);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		AudioNotesLayer layer = plugin != null ? plugin.getAudioNotesLayer() : null;
		return layer != null ? layer.getAudioNoteIcon() : null;
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_take_audio_note_descr);
	}
}
