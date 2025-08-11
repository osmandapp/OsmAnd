package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

public class ZoomInButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public ZoomInButtonState(@NonNull OsmandApplication app) {
		super(app, ZOOM_IN_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.zoomIn);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.change_zoom_action_descr);
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
		return R.layout.map_zoom_in_button;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		return "ic_zoom_in";
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, false, true);
	}
}