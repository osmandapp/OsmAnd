package net.osmand.plus.settings.backend.preferences;

import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Arrays;
import java.util.List;

public class FabMarginPreference {

	private static final String X_PORTRAIT_MARGIN = "_x_portrait_margin";
	private static final String Y_PORTRAIT_MARGIN = "_y_portrait_margin";
	private static final String X_LANDSCAPE_MARGIN = "_x_landscape_margin";
	private static final String Y_LANDSCAPE_MARGIN = "_y_landscape_margin";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final CommonPreference<Integer> fabMarginXPortrait;
	private final CommonPreference<Integer> fabMarginYPortrait;
	private final CommonPreference<Integer> fabMarginXLandscape;
	private final CommonPreference<Integer> fabMarginYLandscape;
	private Pair<Integer, Integer> defaultPortraitMargins;
	private Pair<Integer, Integer> defaultLandscapeMargins;

	public FabMarginPreference(@NonNull OsmandApplication app, @NonNull String prefix) {
		this.app = app;
		this.settings = app.getSettings();
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
	public Pair<Integer, Integer> getPortraitFabMargins() {
		return getPortraitFabMargins(settings.getApplicationMode());
	}

	@NonNull
	public Pair<Integer, Integer> getLandscapeFabMargin() {
		return getLandscapeFabMargin(settings.getApplicationMode());
	}

	@NonNull
	public Pair<Integer, Integer> getPortraitFabMargins(@NonNull ApplicationMode mode) {
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

	public static void setFabButtonMargin(@NonNull MapActivity activity, @NonNull View view,
	                                      @Nullable Pair<Integer, Integer> margins,
	                                      int defRightMargin, int defBottomMargin) {
		int screenHeight = AndroidUtils.getScreenHeight(activity);
		int screenWidth = AndroidUtils.getScreenWidth(activity);
		int btnHeight = view.getHeight();
		int btnWidth = view.getWidth();
		int maxRightMargin = screenWidth - btnWidth;
		int maxBottomMargin = screenHeight - btnHeight;

		int rightMargin = margins != null ? margins.first : defRightMargin;
		int bottomMargin = margins != null ? margins.second : defBottomMargin;
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

		MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
		params.rightMargin = rightMargin;
		params.bottomMargin = bottomMargin;
		view.setLayoutParams(params);
	}

	@NonNull
	public Pair<Integer, Integer> getDefaultPortraitMargins() {
		return defaultPortraitMargins;
	}

	@NonNull
	public Pair<Integer, Integer> getDefaultLandscapeMargins() {
		return defaultLandscapeMargins;
	}

	public void setDefaultPortraitMargins(@NonNull Pair<Integer, Integer> margins) {
		this.defaultPortraitMargins = margins;
	}

	public void setDefaultLandscapeMargins(@NonNull Pair<Integer, Integer> margins) {
		this.defaultLandscapeMargins = margins;
	}

	@NonNull
	public List<CommonPreference<?>> getInternalPrefs() {
		return Arrays.asList(fabMarginXPortrait, fabMarginYPortrait, fabMarginXLandscape, fabMarginYLandscape);
	}
}
