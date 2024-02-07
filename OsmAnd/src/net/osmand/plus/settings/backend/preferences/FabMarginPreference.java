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
	private final CommonPreference<Integer> FAB_MARGIN_X_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_Y_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_X_LANDSCAPE;
	private final CommonPreference<Integer> FAB_MARGIN_Y_LANDSCAPE;

	public FabMarginPreference(@NonNull OsmandSettings settings, @NonNull String prefix) {
		this.settings = settings;
		FAB_MARGIN_X_PORTRAIT = settings.registerIntPreference(prefix + X_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_PORTRAIT = settings.registerIntPreference(prefix + Y_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_X_LANDSCAPE = settings.registerIntPreference(prefix + X_LANDSCAPE_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_LANDSCAPE = settings.registerIntPreference(prefix + Y_LANDSCAPE_MARGIN, 0).makeProfile();
	}

	public void setPortraitFabMargin(int x, int y) {
		FAB_MARGIN_X_PORTRAIT.set(x);
		FAB_MARGIN_Y_PORTRAIT.set(y);
	}

	public void setLandscapeFabMargin(int x, int y) {
		FAB_MARGIN_X_LANDSCAPE.set(x);
		FAB_MARGIN_Y_LANDSCAPE.set(y);
	}

	public void setPortraitFabMargin(@NonNull ApplicationMode mode, int x, int y) {
		FAB_MARGIN_X_PORTRAIT.setModeValue(mode, x);
		FAB_MARGIN_Y_PORTRAIT.setModeValue(mode, y);
	}

	public void setLandscapeFabMargin(@NonNull ApplicationMode mode, int x, int y) {
		FAB_MARGIN_X_LANDSCAPE.setModeValue(mode, x);
		FAB_MARGIN_Y_LANDSCAPE.setModeValue(mode, y);
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
		return new Pair<>(FAB_MARGIN_X_PORTRAIT.getModeValue(mode), FAB_MARGIN_Y_PORTRAIT.getModeValue(mode));
	}

	@NonNull
	public Pair<Integer, Integer> getLandscapeFabMargin(@NonNull ApplicationMode mode) {
		return new Pair<>(FAB_MARGIN_X_LANDSCAPE.getModeValue(mode), FAB_MARGIN_Y_LANDSCAPE.getModeValue(mode));
	}

	public void resetModeToDefault(@NonNull ApplicationMode appMode) {
		FAB_MARGIN_X_PORTRAIT.resetModeToDefault(appMode);
		FAB_MARGIN_Y_PORTRAIT.resetModeToDefault(appMode);
		FAB_MARGIN_X_LANDSCAPE.resetModeToDefault(appMode);
		FAB_MARGIN_Y_LANDSCAPE.resetModeToDefault(appMode);
	}

	public void copyForMode(@NonNull ApplicationMode fromAppMode, @NonNull ApplicationMode toAppMode) {
		FAB_MARGIN_X_PORTRAIT.setModeValue(toAppMode, FAB_MARGIN_X_PORTRAIT.getModeValue(fromAppMode));
		FAB_MARGIN_Y_PORTRAIT.setModeValue(toAppMode, FAB_MARGIN_Y_PORTRAIT.getModeValue(fromAppMode));
		FAB_MARGIN_X_LANDSCAPE.setModeValue(toAppMode, FAB_MARGIN_X_LANDSCAPE.getModeValue(fromAppMode));
		FAB_MARGIN_Y_LANDSCAPE.setModeValue(toAppMode, FAB_MARGIN_Y_LANDSCAPE.getModeValue(fromAppMode));
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
