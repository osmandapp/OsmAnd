package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_3D_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.HIDDEN;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.VISIBLE;
import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;

public class Map3DButtonState extends MapButtonState {

	public final FabMarginPreference fabMarginPref;
	public final CommonPreference<Map3DModeVisibility> visibilityPref;
	private float elevationAngle = DEFAULT_ELEVATION_ANGLE;


	public Map3DButtonState(@NonNull OsmandApplication app) {
		super(app, MAP_3D_HUD_ID);
		fabMarginPref = addPreference(new FabMarginPreference(app, "map_3d_mode_margin"));
		visibilityPref = addPreference(settings.registerEnumStringPreference("map_3d_mode_visibility", VISIBLE, Map3DModeVisibility.values(), Map3DModeVisibility.class)).makeProfile().cache();

		int portraitMargin = AndroidUtils.calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing);
		int landscapeMargin = AndroidUtils.calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land);
		fabMarginPref.setDefaultPortraitMargins(Pair.create(portraitMargin, portraitMargin));
		fabMarginPref.setDefaultLandscapeMargins(Pair.create(landscapeMargin, landscapeMargin));
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.map_3d_mode_action);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.map_3d_mode_action_descr);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.map_3d_button;
	}

	@Override
	public boolean isEnabled() {
		return getVisibility() != HIDDEN;
	}

	public float getElevationAngle() {
		return elevationAngle;
	}

	public void setElevationAngle(float angle) {
		elevationAngle = angle;
	}

	@NonNull
	public Map3DModeVisibility getVisibility() {
		return visibilityPref.get();
	}

	@NonNull
	public Map3DModeVisibility getVisibility(@NonNull ApplicationMode mode) {
		return visibilityPref.getModeValue(mode);
	}

	@NonNull
	@Override
	public FabMarginPreference getFabMarginPref() {
		return fabMarginPref;
	}

	@NonNull
	@Override
	public CommonPreference<Map3DModeVisibility> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		String iconName = isFlatMapMode() ? "ic_action_3d" : "ic_action_2d";
		return new ButtonAppearanceParams(iconName, BIG_SIZE_DP, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}

	public boolean isFlatMapMode() {
		return app.getOsmandMap().getMapView().getElevationAngle() == DEFAULT_ELEVATION_ANGLE;
	}
}