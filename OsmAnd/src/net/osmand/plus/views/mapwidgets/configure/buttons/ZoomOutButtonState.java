package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_RIGHT;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;

public class ZoomOutButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public ZoomOutButtonState(@NonNull OsmandApplication app) {
		super(app, ZOOM_OUT_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.zoomOut);
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
		return R.layout.map_zoom_out_button;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		return "ic_zoom_out";
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, false, true);
	}
}