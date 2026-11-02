package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class ClimbsCountWidgetState extends ResizableWidgetState {

	private static final int DEFAULT_MIN_CLIMB_ELEVATION = 30; // meters
	private static final float DEFAULT_MIN_CLIMB_GRADE = 3.0f; // percentage
	private static final boolean DEFAULT_SHOW_OVERLAY = true;

	private final CommonPreference<Integer> minClimbElevationPref;
	private final CommonPreference<Float> minClimbGradePref;
	private final CommonPreference<Boolean> showOverlayPref;

	public ClimbsCountWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app, customId, WidgetType.CLIMBS_COUNT, WidgetSize.SMALL);
		this.minClimbElevationPref = registerMinClimbElevationPref(customId);
		this.minClimbGradePref = registerMinClimbGradePref(customId);
		this.showOverlayPref = registerShowOverlayPref(customId);
	}

	@NonNull
	private CommonPreference<Integer> registerMinClimbElevationPref(@Nullable String customId) {
		String prefId = "climbs_count_min_elevation";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerIntPreference(prefId, DEFAULT_MIN_CLIMB_ELEVATION)
				.makeProfile();
	}

	@NonNull
	private CommonPreference<Float> registerMinClimbGradePref(@Nullable String customId) {
		String prefId = "climbs_count_min_grade";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerFloatPreference(prefId, DEFAULT_MIN_CLIMB_GRADE)
				.makeProfile();
	}

	@NonNull
	private CommonPreference<Boolean> registerShowOverlayPref(@Nullable String customId) {
		String prefId = "climbs_count_show_overlay";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, DEFAULT_SHOW_OVERLAY)
				.makeProfile();
	}

	@NonNull
	public CommonPreference<Integer> getMinClimbElevationPref() {
		return minClimbElevationPref;
	}

	@NonNull
	public CommonPreference<Float> getMinClimbGradePref() {
		return minClimbGradePref;
	}

	public int getMinClimbElevation() {
		return minClimbElevationPref.get();
	}

	public float getMinClimbGrade() {
		return minClimbGradePref.get();
	}

	@NonNull
	public CommonPreference<Boolean> getShowOverlayPref() {
		return showOverlayPref;
	}

	public boolean shouldShowOverlay() {
		return showOverlayPref.get();
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
		super.copyPrefs(appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerMinClimbElevationPref(customId).setModeValue(appMode, minClimbElevationPref.getModeValue(sourceAppMode));
		registerMinClimbGradePref(customId).setModeValue(appMode, minClimbGradePref.getModeValue(sourceAppMode));
		registerShowOverlayPref(customId).setModeValue(appMode, showOverlayPref.getModeValue(sourceAppMode));
		super.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}
}
