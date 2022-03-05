package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MapWidgetRegInfo implements Comparable<MapWidgetRegInfo> {

	public static final int INVALID_ID = 0;

	public final String key;
	public final TextInfoWidget widget;
	public final boolean left;
	public final int priority;
	private final @DrawableRes int drawableMenu;
	private final @StringRes int messageId;
	private final String message;
	private final WidgetState widgetState;
	private final Set<ApplicationMode> visibleCollapsible = new LinkedHashSet<>();
	private final Set<ApplicationMode> visibleModes = new LinkedHashSet<>();
	private Runnable stateChangeListener = null;

	public MapWidgetRegInfo(String key,
	                        TextInfoWidget widget,
	                        WidgetState widgetState,
	                        @DrawableRes int drawableMenu,
	                        @StringRes int messageId,
	                        String message,
	                        int priority,
	                        boolean left) {
		this.key = key;
		this.widget = widget;
		this.widgetState = widgetState;
		this.drawableMenu = drawableMenu;
		this.messageId = messageId;
		this.message = message;
		this.priority = priority;
		this.left = left;
	}

	public WidgetState getWidgetState() {
		return widgetState;
	}

	public int getDrawableMenu() {
		if (widgetState != null) {
			return widgetState.getMenuIconId();
		} else {
			return drawableMenu;
		}
	}

	public String getMessage() {
		return message;
	}

	public int getMessageId() {
		if (widgetState != null) {
			return widgetState.getMenuTitleId();
		} else {
			return messageId;
		}
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

	public MapWidgetRegInfo required(ApplicationMode... modes) {
		Collections.addAll(visibleModes, modes);
		return this;
	}

	public void setStateChangeListener(Runnable stateChangeListener) {
		this.stateChangeListener = stateChangeListener;
	}

	public Runnable getStateChangeListener() {
		return stateChangeListener;
	}

	@Override
	public int hashCode() {
		if (getMessage() != null) {
			return getMessage().hashCode();
		}
		return getMessageId();
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
				return key.compareTo(key);
			} else {
				return getMessageId() - another.getMessageId();
			}
		}
		return priority - another.priority;
	}
}
