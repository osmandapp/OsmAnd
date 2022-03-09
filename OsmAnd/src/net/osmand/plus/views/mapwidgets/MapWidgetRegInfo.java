package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.LinkedHashSet;
import java.util.Set;

public class MapWidgetRegInfo implements Comparable<MapWidgetRegInfo> {

	public static final int INVALID_ID = 0;

	public final String key;
	public final TextInfoWidget widget;
	public final boolean left;
	public final int priority;
	@DrawableRes
	private final int settingsIconId;
	@StringRes
	private final int messageId;
	private final String message;
	private final WidgetState widgetState;
	private final Set<ApplicationMode> visibleCollapsible = new LinkedHashSet<>();
	private final Set<ApplicationMode> visibleModes = new LinkedHashSet<>();

	public MapWidgetRegInfo(@NonNull String key,
	                        TextInfoWidget widget,
	                        @Nullable WidgetState widgetState,
	                        @DrawableRes int settingsIconId,
	                        @StringRes int messageId,
	                        @Nullable String message,
	                        int priority,
	                        boolean left) {
		this.key = key;
		this.widget = widget;
		this.widgetState = widgetState;
		this.settingsIconId = settingsIconId;
		this.messageId = messageId;
		this.message = message;
		this.priority = priority;
		this.left = left;
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

	public boolean isVisibleCollapsed(ApplicationMode mode) {
		return visibleCollapsible.contains(mode);
	}

	public boolean isVisible(ApplicationMode mode) {
		return visibleModes.contains(mode);
	}

	public void addVisible(ApplicationMode mode) {
		visibleModes.add(mode);
	}

	public void addVisibleCollapsible(ApplicationMode mode) {
		visibleCollapsible.add(mode);
	}

	public void removeVisible(ApplicationMode mode) {
		visibleModes.remove(mode);
	}

	public void removeVisibleCollapsible(ApplicationMode mode) {
		visibleCollapsible.remove(mode);
	}

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
		MapWidgetRegInfo other = (MapWidgetRegInfo) obj;
		if (getMessageId() == 0 && other.getMessageId() == 0) {
			return key.equals(other.key);
		}
		return getMessageId() == other.getMessageId();
	}

	@Override
	public int compareTo(@NonNull MapWidgetRegInfo another) {
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
