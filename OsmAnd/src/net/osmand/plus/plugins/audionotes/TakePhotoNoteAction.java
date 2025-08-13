package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.QuickActionIds.TAKE_PHOTO_NOTE_ACTION_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakePhotoNoteAction extends TakeMediaNoteAction {

	public static final QuickActionType TYPE = new QuickActionType(TAKE_PHOTO_NOTE_ACTION_ID,
			"photo.note", TakePhotoNoteAction .class).
			nameRes(R.string.quick_action_photo_note).iconRes(R.drawable.ic_action_photo_dark).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);

	public TakePhotoNoteAction() {
		super(TYPE);
	}

	public TakePhotoNoteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void takeNote(@NonNull MapActivity mapActivity,
	                        @NonNull AudioVideoNotesPlugin plugin, @NonNull LatLon latLon) {
		plugin.takePhoto(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false, false);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		AudioNotesLayer layer = plugin != null ? plugin.getAudioNotesLayer() : null;
		return layer != null ? layer.getPhotoNoteIcon() : null;
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_take_photo_note_descr);
	}
}
