package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

public class MyLocationButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public MyLocationButtonState(@NonNull OsmandApplication app) {
		super(app, BACK_TO_LOC_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.shared_string_my_location);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.my_location_action_descr);
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.my_location_button;
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		return new ButtonAppearanceParams("ic_my_location", BIG_SIZE_DP, OPAQUE_ALPHA, ROUND_RADIUS_DP);
	}
}