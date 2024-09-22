package net.osmand.plus.mapcontextmenu.builders;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class ContextMenuUIComponents {

	private final MapActivity mapActivity;
	private final OsmandApplication app;

	public ContextMenuUIComponents(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
	}

	@ColorInt
	public int getColor(@ColorRes int resId) {
		return ColorUtilities.getColor(mapActivity, resId);
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}
}
