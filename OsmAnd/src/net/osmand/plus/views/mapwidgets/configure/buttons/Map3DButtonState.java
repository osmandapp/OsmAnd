package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_3D_HUD_ID;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.HIDDEN;
import static net.osmand.plus.settings.enums.Map3DModeVisibility.VISIBLE;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.settings.enums.Map3DModeVisibility;

public class Map3DButtonState extends MapButtonState {

	public final FabMarginPreference fabMarginPref;
	public final CommonPreference<Float> elevationAnglePref;
	public final CommonPreference<Map3DModeVisibility> visibilityPref;


	public Map3DButtonState(@NonNull OsmandApplication app) {
		super(app, MAP_3D_HUD_ID);
		fabMarginPref = new FabMarginPreference(settings, "map_3d_mode_margin");
		elevationAnglePref = settings.registerFloatPreference("map_3d_mode_elevation_angle", 90).makeProfile();
		visibilityPref = settings.registerEnumStringPreference("map_3d_mode_visibility", VISIBLE, Map3DModeVisibility.values(), Map3DModeVisibility.class).makeProfile().cache();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.map_3d_mode_action);
	}

	@Override
	public boolean isEnabled() {
		return getVisibility() != HIDDEN;
	}

	@Nullable
	@Override
	public Drawable getIcon(boolean nightMode, boolean mapIcon, @ColorInt int colorId) {
		return uiUtilities.getPaintedIcon(getVisibility().getIconId(), colorId);
	}

	public float getElevationAngle() {
		return elevationAnglePref.get();
	}

	public void setElevationAngle(float angle) {
		elevationAnglePref.set(angle);
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
	public FabMarginPreference getFabMarginPref() {
		return fabMarginPref;
	}

	@NonNull
	public CommonPreference<Float> getElevationAnglePref() {
		return elevationAnglePref;
	}

	@NonNull
	public CommonPreference<Map3DModeVisibility> getVisibilityPref() {
		return visibilityPref;
	}
}