package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

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
	public String getDefaultIconName() {
		return "ic_my_location";
	}

	@Override
	public float getDefaultOpacity() {
		return OPAQUE_ALPHA;
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, true, false);
	}
}