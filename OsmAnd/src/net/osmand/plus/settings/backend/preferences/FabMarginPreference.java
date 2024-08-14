package net.osmand.plus.settings.backend.preferences;

import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public class FabMarginPreference {

	private static final String X_PORTRAIT_MARGIN = "_x_portrait_margin";
	private static final String Y_PORTRAIT_MARGIN = "_y_portrait_margin";
	private static final String X_LANDSCAPE_MARGIN = "_x_landscape_margin";
	private static final String Y_LANDSCAPE_MARGIN = "_y_landscape_margin";

	private final OsmandSettings settings;
	private final CommonPreference<Integer> fabMarginXPortrait;
	private final CommonPreference<Integer> fabMarginYPortrait;
	private final CommonPreference<Integer> fabMarginXLandscape;
	private final CommonPreference<Integer> fabMarginYLandscape;

	public FabMarginPreference(@NonNull OsmandSettings settings, @NonNull String prefix) {
		this.settings = settings;
		fabMarginXPortrait = settings.registerIntPreference(prefix + X_PORTRAIT_MARGIN, 0).makeProfile();
		fabMarginYPortrait = settings.registerIntPreference(prefix + Y_PORTRAIT_MARGIN, 0).makeProfile();
		fabMarginXLandscape = settings.registerIntPreference(prefix + X_LANDSCAPE_MARGIN, 0).makeProfile();
		fabMarginYLandscape = settings.registerIntPreference(prefix + Y_LANDSCAPE_MARGIN, 0).makeProfile();
	}

	public void setPortraitFabMargin(int x, int y) {
		fabMarginXPortrait.set(x);
		fabMarginYPortrait.set(y);
	}

	public void setLandscapeFabMargin(int x, int y) {
		fabMarginXLandscape.set(x);
		fabMarginYLandscape.set(y);
	}

	public void setPortraitFabMargin(@NonNull ApplicationMode mode, int x, int y) {
		fabMarginXPortrait.setModeValue(mode, x);
		fabMarginYPortrait.setModeValue(mode, y);
	}

	public void setLandscapeFabMargin(@NonNull ApplicationMode mode, int x, int y) {
		fabMarginXLandscape.setModeValue(mode, x);
		fabMarginYLandscape.setModeValue(mode, y);
	}

	@NonNull
	public Pair<Integer, Integer> getPortraitFabMargin() {
		return getPortraitFabMargin(settings.getApplicationMode());
	}

	@NonNull
	public Pair<Integer, Integer> getLandscapeFabMargin() {
		return getLandscapeFabMargin(settings.getApplicationMode());
	}

	@NonNull
	public Pair<Integer, Integer> getPortraitFabMargin(@NonNull ApplicationMode mode) {
		return new Pair<>(fabMarginXPortrait.getModeValue(mode), fabMarginYPortrait.getModeValue(mode));
	}

	@NonNull
	public Pair<Integer, Integer> getLandscapeFabMargin(@NonNull ApplicationMode mode) {
		return new Pair<>(fabMarginXLandscape.getModeValue(mode), fabMarginYLandscape.getModeValue(mode));
	}

	public void resetModeToDefault(@NonNull ApplicationMode appMode) {
		fabMarginXPortrait.resetModeToDefault(appMode);
		fabMarginYPortrait.resetModeToDefault(appMode);
		fabMarginXLandscape.resetModeToDefault(appMode);
		fabMarginYLandscape.resetModeToDefault(appMode);
	}

	public void copyForMode(@NonNull ApplicationMode fromAppMode, @NonNull ApplicationMode toAppMode) {
		fabMarginXPortrait.setModeValue(toAppMode, fabMarginXPortrait.getModeValue(fromAppMode));
		fabMarginYPortrait.setModeValue(toAppMode, fabMarginYPortrait.getModeValue(fromAppMode));
		fabMarginXLandscape.setModeValue(toAppMode, fabMarginXLandscape.getModeValue(fromAppMode));
		fabMarginYLandscape.setModeValue(toAppMode, fabMarginYLandscape.getModeValue(fromAppMode));
	}

	public CommonPreference<Integer> getFabMarginXPortrait() {
		return fabMarginXPortrait;
	}

	public CommonPreference<Integer> getFabMarginYPortrait() {
		return fabMarginYPortrait;
	}

	public CommonPreference<Integer> getFabMarginXLandscape() {
		return fabMarginXLandscape;
	}

	public CommonPreference<Integer> getFabMarginYLandscape() {
		return fabMarginYLandscape;
	}

	public static void setFabButtonMargin(@Nullable MapActivity mapActivity, @NonNull ImageView fabButton, FrameLayout.LayoutParams params,
	                                      @Nullable Pair<Integer, Integer> fabMargin,
	                                      int defRightMargin, int defBottomMargin) {
		if (mapActivity == null) {
			return;
		}
		int screenHeight = AndroidUtils.getScreenHeight(mapActivity);
		int screenWidth = AndroidUtils.getScreenWidth(mapActivity);
		int btnHeight = fabButton.getHeight();
		int btnWidth = fabButton.getWidth();
		int maxRightMargin = screenWidth - btnWidth;
		int maxBottomMargin = screenHeight - btnHeight;

		int rightMargin = fabMargin != null ? fabMargin.first : defRightMargin;
		int bottomMargin = fabMargin != null ? fabMargin.second : defBottomMargin;
		// check limits
		if (rightMargin <= 0) {
			rightMargin = defRightMargin;
		} else if (rightMargin > maxRightMargin) {
			rightMargin = maxRightMargin;
		}
		if (bottomMargin <= 0) {
			bottomMargin = defBottomMargin;
		} else if (bottomMargin > maxBottomMargin) {
			bottomMargin = maxBottomMargin;
		}

		params.rightMargin = rightMargin;
		params.bottomMargin = bottomMargin;
		fabButton.setLayoutParams(params);
	}
}
