package net.osmand.plus.plugins.osmedit.quickactions;

import static net.osmand.plus.quickaction.QuickActionIds.ADD_OSM_BUG_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsLayer;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.AddMapObjectAction;
import net.osmand.plus.views.PointImageDrawable;

public class AddOSMBugAction extends AddMapObjectAction {

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
	protected void addMapObject(@NonNull MapActivity mapActivity, @NonNull LatLon latLon) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			double lat = latLon.getLatitude();
			double lon = latLon.getLongitude();
			plugin.openOsmNote(mapActivity, lat, lon, getMessage(), shouldAutofill());
		}
	}

	private String getMessage() {
		return getParams().isEmpty() ? "" : getParams().get(KEY_MESSAGE);
	}

	private boolean shouldAutofill() {
		return getParams().isEmpty() || !Boolean.valueOf(getParams().get(KEY_SHO_DIALOG));
	}

	@Nullable
	@Override
	protected PointImageDrawable getMapObjectDrawable() {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		OsmEditsLayer layer = plugin != null ? plugin.getOsmEditsLayer() : null;
		return layer != null ? layer.createDrawableForOsmPoint(new OsmNotesPoint()) : null;
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
