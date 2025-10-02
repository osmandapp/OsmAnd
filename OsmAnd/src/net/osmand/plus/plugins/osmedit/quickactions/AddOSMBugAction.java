package net.osmand.plus.plugins.osmedit.quickactions;

import static net.osmand.plus.quickaction.QuickActionIds.ADD_OSM_BUG_ACTION_ID;

import android.os.Bundle;
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
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.SelectMapLocationAction;
import net.osmand.plus.utils.UiUtilities;

import java.util.Objects;

public class AddOSMBugAction extends SelectMapLocationAction {

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
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			super.execute(mapActivity, params);
		}
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			double lat = latLon.getLatitude();
			double lon = latLon.getLongitude();
			plugin.openOsmNote(mapActivity, lat, lon, getMessage(), !shouldShowDialog());
		}
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		OsmEditsLayer layer = plugin != null ? plugin.getOsmEditsLayer() : null;
		return layer != null ? layer.createOsmNoteIcon() : null;
	}

	@NonNull
	private String getMessage() {
		return getParams().isEmpty() ? "" : Objects.requireNonNull(getParams().get(KEY_MESSAGE));
	}

	private boolean shouldShowDialog() {
		return !getParams().isEmpty() && Boolean.parseBoolean(getParams().get(KEY_SHO_DIALOG));
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_add_bug, parent, false);
		setupPointLocationView(view.findViewById(R.id.point_location_container), mapActivity);

		SwitchCompat swShowDialog = view.findViewById(R.id.dialogSwitch);
		EditText etMessage = view.findViewById(R.id.message_edit);

		if (!getParams().isEmpty()) {
			swShowDialog.setChecked(shouldShowDialog());
			etMessage.setText(getMessage());
		}
		parent.addView(view);
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		SwitchCompat swShowDialog = root.findViewById(R.id.dialogSwitch);
		EditText etMessage = root.findViewById(R.id.message_edit);
		getParams().put(KEY_SHO_DIALOG, String.valueOf(swShowDialog.isChecked()));
		getParams().put(KEY_MESSAGE, etMessage.getText().toString());
		return super.fillParams(root, mapActivity);
	}
}
