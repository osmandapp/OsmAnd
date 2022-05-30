package net.osmand.plus.plugins.osmedit.quickactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class AddOSMBugAction extends QuickAction {


	private static final String KEY_MESSAGE = "message";
	private static final String KEY_SHO_DIALOG = "dialog";

	public static final QuickActionType TYPE = new QuickActionType(12,
			"osmbug.add", AddOSMBugAction.class).
			nameRes(R.string.quick_action_add_osm_bug).iconRes(R.drawable.ic_action_osm_note_add)
			.category(QuickActionType.CREATE_CATEGORY);

	public AddOSMBugAction() {
		super(TYPE);
	}

	public AddOSMBugAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {

		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		if (plugin != null) {

			LatLon latLon = mapActivity.getMapView()
					.getCurrentRotatedTileBox()
					.getCenterLatLon();

			if (getParams().isEmpty()) {

				plugin.openOsmNote(mapActivity, latLon.getLatitude(), latLon.getLongitude(), "", true);

			} else {

				plugin.openOsmNote(mapActivity, latLon.getLatitude(), latLon.getLongitude(),
						getParams().get(KEY_MESSAGE),
						!Boolean.valueOf(getParams().get(KEY_SHO_DIALOG)));
			}
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_bug, parent, false);

		SwitchCompat showDialog = (SwitchCompat) view.findViewById(R.id.dialogSwitch);
		EditText message = (EditText) view.findViewById(R.id.message_edit);

		if (!getParams().isEmpty()) {

			showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_SHO_DIALOG)));
			message.setText(getParams().get(KEY_MESSAGE));
		}

		parent.addView(view);
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {

		SwitchCompat showDialog = (SwitchCompat) root.findViewById(R.id.dialogSwitch);
		EditText message = (EditText) root.findViewById(R.id.message_edit);

		getParams().put(KEY_SHO_DIALOG, String.valueOf(showDialog.isChecked()));
		getParams().put(KEY_MESSAGE, message.getText().toString());

		return true;
	}
}
