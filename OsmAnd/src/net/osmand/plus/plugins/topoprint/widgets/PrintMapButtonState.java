package net.osmand.plus.plugins.topoprint.widgets;

import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.shared.grid.ButtonPositionSize;

/**
 * State of the map-hud print button: defines its default placement (right
 * edge, stacked above the zoom cluster), appearance and the per-profile
 * visibility preference.
 */
public class PrintMapButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public PrintMapButtonState(@NonNull OsmandApplication app) {
		super(app, "print_map_button");
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.topoprint_plugin_name);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.topoprint_map_button_descr);
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.map_print_button;
	}

	@NonNull
	@Override
	public String getDefaultIconName(Boolean nightMode) {
		return "ic_action_gprint_dark";
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, false, true);
	}
}
