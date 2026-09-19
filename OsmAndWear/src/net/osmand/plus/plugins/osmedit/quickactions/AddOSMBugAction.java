package net.osmand.plus.plugins.osmedit.quickactions;

import static net.osmand.plus.quickaction.QuickActionIds.ADD_OSM_BUG_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class AddOSMBugAction extends QuickAction {

	private static final String KEY_MESSAGE = "message";
	private static final String KEY_SHO_DIALOG = "dialog";

	public static final QuickActionType TYPE = new QuickActionType(ADD_OSM_BUG_ACTION_ID,
			"osmbug.add", AddOSMBugAction.class)
			.nameRes(R.string.osn_bug_name).iconRes(R.drawable.ic_action_osm_note_add)
			.category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add)
			.forceUseExtendedName();

	public AddOSMBugAction() {
		super(TYPE);
	}

	public AddOSMBugAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			LatLon latLon = getMapLocation(mapActivity);
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

		SwitchCompat showDialog = view.findViewById(R.id.dialogSwitch);
		EditText message = view.findViewById(R.id.message_edit);

		if (!getParams().isEmpty()) {

			showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_SHO_DIALOG)));
			message.setText(getParams().get(KEY_MESSAGE));
		}

		parent.addView(view);
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {

		SwitchCompat showDialog = root.findViewById(R.id.dialogSwitch);
		EditText message = root.findViewById(R.id.message_edit);

		getParams().put(KEY_SHO_DIALOG, String.valueOf(showDialog.isChecked()));
		getParams().put(KEY_MESSAGE, message.getText().toString());

		return true;
	}
}
