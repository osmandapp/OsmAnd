package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.quickaction.QuickActionIds.TAKE_VIDEO_NOTE_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakeVideoNoteAction extends QuickAction {
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
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null)
			plugin.recordVideo(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_take_video_note_descr);
		parent.addView(view);
	}
}
