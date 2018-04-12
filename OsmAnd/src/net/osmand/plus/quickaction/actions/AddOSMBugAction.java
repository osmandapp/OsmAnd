package net.osmand.plus.quickaction.actions;

import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickAction;

public class AddOSMBugAction extends QuickAction {

	public static final int TYPE = 12;

	private static final String KEY_MESSAGE = "message";
	private static final String KEY_SHO_DIALOG = "dialog";

	public AddOSMBugAction() {
		super(TYPE);
	}

	public AddOSMBugAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		if (plugin != null) {

			LatLon latLon = activity.getMapView()
					.getCurrentRotatedTileBox()
					.getCenterLatLon();

			if (getParams().isEmpty()) {

				plugin.openOsmNote(activity, latLon.getLatitude(), latLon.getLongitude(), "", true);

			} else {

				plugin.openOsmNote(activity, latLon.getLatitude(), latLon.getLongitude(),
						getParams().get(KEY_MESSAGE),
						!Boolean.valueOf(getParams().get(KEY_SHO_DIALOG)));
			}
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

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
	public boolean fillParams(View root, MapActivity activity) {

		SwitchCompat showDialog = (SwitchCompat) root.findViewById(R.id.dialogSwitch);
		EditText message = (EditText) root.findViewById(R.id.message_edit);

		getParams().put(KEY_SHO_DIALOG, String.valueOf(showDialog.isChecked()));
		getParams().put(KEY_MESSAGE, message.getText().toString());

		return true;
	}
}
