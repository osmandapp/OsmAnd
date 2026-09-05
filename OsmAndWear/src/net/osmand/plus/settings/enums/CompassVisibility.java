package net.osmand.plus.settings.enums;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_COMPASS;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;

public enum CompassVisibility {

	ALWAYS_VISIBLE(R.string.compass_always_visible, 0, R.drawable.ic_action_compass_north, R.id.always_visible_mode),
	ALWAYS_HIDDEN(R.string.compass_always_hidden, 0, R.drawable.ic_action_compass_hidden, R.id.always_hidden_mode),
	VISIBLE_IF_MAP_ROTATED(R.string.compass_visible_if_map_rotated, R.string.compass_visible_if_map_rotated_desc, R.drawable.ic_action_compass_rotated, R.id.visible_if_map_rotated_mode);

	@StringRes
	public final int titleId;
	@StringRes
	public final int descId;
	@DrawableRes
	public final int iconId;
	@IdRes
	public final int containerId;

	CompassVisibility(@StringRes int titleId, @StringRes int descId, @DrawableRes int iconId, @IdRes int containerId) {
		this.titleId = titleId;
		this.descId = descId;
		this.iconId = iconId;
		this.containerId = containerId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@Nullable
	public String getDescription(@NonNull Context context) {
		return descId == 0 ? null : context.getString(descId);
	}

	@Nullable
	public static CompassVisibility getFromCustomization(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		OsmAndAppCustomization customization = app.getAppCustomization();
		if (customization.areWidgetsCustomized()) {
			return customization.isWidgetVisible(WIDGET_COMPASS, appMode) ? ALWAYS_VISIBLE : ALWAYS_HIDDEN;
		}
		return null;
	}
}
