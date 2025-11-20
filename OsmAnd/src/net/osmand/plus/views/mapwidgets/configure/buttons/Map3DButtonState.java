package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_3D_HUD_ID;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.HIDDEN;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.VISIBLE;
import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;
import static net.osmand.shared.grid.ButtonPositionSize.CELL_SIZE_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.shared.grid.ButtonPositionSize;

import org.jetbrains.annotations.NotNull;

public class Map3DButtonState extends MapButtonState {

	private final CommonPreference<Map3DModeVisibility> visibilityPref;

	private float elevationAngle = DEFAULT_ELEVATION_ANGLE;


	public Map3DButtonState(@NonNull OsmandApplication app) {
		super(app, MAP_3D_HUD_ID);
		this.visibilityPref = addPreference(settings.registerEnumStringPreference("map_3d_mode_visibility", VISIBLE, Map3DModeVisibility.values(), Map3DModeVisibility.class)).makeProfile().cache();
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
	public CommonPreference<Map3DModeVisibility> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	@Override
	public String getDefaultIconName(@Nullable Boolean nightMode) {
		return isFlatMapMode() ? "ic_action_3d" : "ic_action_2d";
	}

	public boolean isFlatMapMode() {
		return app.getOsmandMap().getMapView().getElevationAngle() == DEFAULT_ELEVATION_ANGLE;
	}

	@Override
	protected void updatePosition(@NonNull @NotNull ButtonPositionSize position) {
		position.setMarginX(CELL_SIZE_DP);
		position.setMarginY(CELL_SIZE_DP);
		super.updatePosition(position);
		position.setXMove(!portrait);
		position.setYMove(portrait);
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, true, true);
	}
}