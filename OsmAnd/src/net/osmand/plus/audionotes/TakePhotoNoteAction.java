package net.osmand.plus.audionotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakePhotoNoteAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(10,
			"photo.note", TakePhotoNoteAction .class).
			nameRes(R.string.quick_action_take_photo_note).iconRes(R.drawable.ic_action_photo_dark).nonEditable().
			category(QuickActionType.CREATE_CATEGORY);

	public TakePhotoNoteAction() {
		super(TYPE);
	}

	public TakePhotoNoteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		LatLon latLon = activity.getMapView()
				.getCurrentRotatedTileBox()
				.getCenterLatLon();

		AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null)
			plugin.takePhoto(latLon.getLatitude(), latLon.getLongitude(), activity, false, false);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_take_photo_note_descr);

		parent.addView(view);
	}
}
