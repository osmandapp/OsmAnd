package net.osmand.plus.views.mapwidgets;

import android.content.Context;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ALTITUDE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BEARING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_GPS_INFO;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_1;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_2;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_TIME;

public abstract class MapWidgetInfo implements Comparable<MapWidgetInfo> {

	public static final int INVALID_ID = 0;

	public final String key;
	public final MapWidget widget;
	public final WidgetsPanel widgetPanel;
	public int priority;
	public int pageIndex = 0;

	@DrawableRes
	private final int settingsIconId;
	@StringRes
	private final int messageId;
	private final String message;
	private final WidgetState widgetState;

	public MapWidgetInfo(@NonNull String key,
	                     @NonNull MapWidget widget,
	                     @Nullable WidgetState widgetState,
	                     @DrawableRes int settingsIconId,
	                     @StringRes int messageId,
	                     @Nullable String message,
	                     int priority,
	                     @NonNull WidgetsPanel widgetPanel) {
		this.key = key;
		this.widget = widget;
		this.widgetState = widgetState;
		this.settingsIconId = settingsIconId;
		this.messageId = messageId;
		this.message = message;
		this.priority = priority;
		this.widgetPanel = widgetPanel;
	}

	@Nullable
	public WidgetState getWidgetState() {
		return widgetState;
	}

	@DrawableRes
	public int getSettingsIconId() {
		return widgetState != null
				? widgetState.getSettingsIconId()
				: settingsIconId;
	}

	@DrawableRes
	public int getMapIconId(boolean nightMode) {
		if (widget instanceof TextInfoWidget) {
			TextInfoWidget textInfoWidget = (TextInfoWidget) widget;
			return textInfoWidget.getIconId(nightMode);
		}
		return 0;
	}

	@NonNull
	public String getTitle(@NonNull Context ctx) {
		return message != null ? message : ctx.getString(getMessageId());
	}

	@Nullable
	public String getMessage() {
		return message;
	}

	@StringRes
	public int getMessageId() {
		return widgetState != null
				? widgetState.getMenuTitleId()
				: messageId;
	}

	public boolean isSelected(@NonNull ApplicationMode appMode) {
		OsmandPreference<Boolean> visibilityPref = widget.getWidgetVisibilityPref();
		return visibilityPref != null ? visibilityPref.getModeValue(appMode) : isVisibleForAppMode(appMode);
	}

	public abstract boolean isVisibleForAppMode(@NonNull ApplicationMode appMode);

	public abstract void showHideForAppMode(@NonNull ApplicationMode appMode, boolean show);

	@Override
	public int hashCode() {
		String message = getMessage();
		return message != null ? message.hashCode() : getMessageId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		}
		MapWidgetInfo other = (MapWidgetInfo) obj;
		if (getMessageId() == 0 && other.getMessageId() == 0) {
			return key.equals(other.key);
		}
		return getMessageId() == other.getMessageId();
	}

	@Override
	public int compareTo(@NonNull MapWidgetInfo another) {
		if (getMessageId() == 0 && another.getMessageId() == 0) {
			if (key.equals(another.key)) {
				return 0;
			}
		} else if (getMessageId() == another.getMessageId()) {
			return 0;
		}
		if (priority == another.priority) {
			if (getMessageId() == 0 && another.getMessageId() == 0) {
				return key.compareTo(another.key);
			} else {
				return getMessageId() - another.getMessageId();
			}
		}
		return priority - another.priority;
	}
}