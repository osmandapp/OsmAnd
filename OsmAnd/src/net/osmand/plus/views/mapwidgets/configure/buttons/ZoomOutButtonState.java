package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

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
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		return new ButtonAppearanceParams("ic_zoom_out", BIG_SIZE_DP, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}
}