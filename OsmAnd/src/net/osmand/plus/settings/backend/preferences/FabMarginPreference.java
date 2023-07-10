package net.osmand.plus.settings.backend.preferences;

import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public class FabMarginPreference {
	private static final String X_PORTRAIT_MARGIN = "_x_portrait_margin";
	private static final String Y_PORTRAIT_MARGIN = "_y_portrait_margin";
	private static final String X_LANDSCAPE_MARGIN = "_x_landscape_margin";
	private static final String Y_LANDSCAPE_MARGIN = "_y_landscape_margin";

	private final CommonPreference<Integer> FAB_MARGIN_X_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_Y_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_X_LANDSCAPE;
	private final CommonPreference<Integer> FAB_MARGIN_Y_LANDSCAPE;

	public FabMarginPreference(OsmandSettings settings, String prefix) {
		FAB_MARGIN_X_PORTRAIT = new IntPreference(settings, prefix + X_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_PORTRAIT = new IntPreference(settings, prefix + Y_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_X_LANDSCAPE = new IntPreference(settings, prefix + X_LANDSCAPE_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_LANDSCAPE = new IntPreference(settings, prefix + Y_LANDSCAPE_MARGIN, 0).makeProfile();
	}

	public void setPortraitFabMargin(int x, int y) {
		FAB_MARGIN_X_PORTRAIT.set(x);
		FAB_MARGIN_Y_PORTRAIT.set(y);
	}

	public void setLandscapeFabMargin(int x, int y) {
		FAB_MARGIN_X_LANDSCAPE.set(x);
		FAB_MARGIN_Y_LANDSCAPE.set(y);
	}

	@Nullable
	public Pair<Integer, Integer> getPortraitFabMargin() {
		return new Pair<>(FAB_MARGIN_X_PORTRAIT.get(), FAB_MARGIN_Y_PORTRAIT.get());
	}

	@Nullable
	public Pair<Integer, Integer> getLandscapeFabMargin() {
		return new Pair<>(FAB_MARGIN_X_LANDSCAPE.get(), FAB_MARGIN_Y_LANDSCAPE.get());

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
