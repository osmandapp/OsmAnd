package net.osmand.plus.base.dialog.utils;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.CONTROLS_COLOR;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.utils.ColorUtilities;

public class ParametersExtractor {

	private final OsmandApplication app;

	public ParametersExtractor(OsmandApplication app) {
		this.app = app;
	}

	@ColorInt
	public int getControlsColor(@NonNull DisplayData displayData, @NonNull DisplayItem item,
	                            boolean nightMode) {
		Integer color = item.getControlsColor();
		if (color == null) {
			color = (Integer) displayData.getExtra(CONTROLS_COLOR);
		}
		if (color == null) {
			color = ColorUtilities.getActiveColor(app, nightMode);
		}
		return color;
	}

	@ColorInt
	@Nullable
	public Integer getBackgroundColor(@NonNull DisplayData displayData, @NonNull DisplayItem item) {
		Integer color = item.getBackgroundColor();
		if (color == null) {
			color = (Integer) displayData.getExtra(BACKGROUND_COLOR);
		}
		return color;
	}

}
