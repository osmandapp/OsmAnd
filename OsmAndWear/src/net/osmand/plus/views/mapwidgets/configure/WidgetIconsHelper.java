package net.osmand.plus.views.mapwidgets.configure;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class WidgetIconsHelper {

	private final OsmandApplication app;
	@ColorInt
	private final int profileColor;
	private final boolean nightMode;

	public WidgetIconsHelper(@NonNull OsmandApplication app, @ColorInt int profileColor, boolean nightMode) {
		this.app = app;
		this.profileColor = profileColor;
		this.nightMode = nightMode;
	}

	public void updateWidgetIcon(@NonNull ImageView icon, @NonNull MapWidgetInfo widgetInfo) {
		int mapIconId = widgetInfo.getMapIconId(nightMode);
		int settingsIconId = widgetInfo.getSettingsIconId(nightMode);
		boolean shouldPaintIcon = !widgetInfo.isIconPainted();
		updateWidgetIcon(icon, mapIconId, settingsIconId, shouldPaintIcon);
	}

	public void updateWidgetIcon(@NonNull ImageView icon,
	                             @DrawableRes int mapIconId,
	                             @DrawableRes int settingsIconId,
	                             boolean shouldPaintIcon) {
		if (mapIconId != 0) {
			icon.setImageResource(mapIconId);
			if (shouldPaintIcon) {
				setImageFilter(icon);
			}
		} else {
			UiUtilities iconsCache = app.getUIUtilities();
			Drawable drawable;
			if (shouldPaintIcon) {
				drawable = iconsCache.getPaintedIcon(settingsIconId, profileColor);
			} else {
				drawable = iconsCache.getIcon(settingsIconId);
			}
			icon.setImageDrawable(drawable);
		}
	}

	private void setImageFilter(@NonNull ImageView imageView) {
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);
		imageView.setColorFilter(colorFilter);
	}
}