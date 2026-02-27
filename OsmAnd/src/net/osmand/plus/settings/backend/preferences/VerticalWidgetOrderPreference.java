package net.osmand.plus.settings.backend.preferences;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.PAGE_SEPARATOR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.WIDGET_SEPARATOR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class VerticalWidgetOrderPreference extends ListStringPreference {

	public VerticalWidgetOrderPreference(@NonNull OsmandSettings settings, @NonNull String id,
			@Nullable String defaultValue, @NonNull String delimiter) {
		super(settings, id, defaultValue, delimiter);
	}

	@Override
	public String get() {
		String value = super.get();
		if (!Algorithms.isEmpty(value)) {
			return getPagedWidgetIds(Arrays.asList(value.split(getDelimiter())));
		}
		return value;
	}

	@Override
	public String getModeValue(ApplicationMode mode) {
		String value = super.getModeValue(mode);
		if (!Algorithms.isEmpty(value)) {
			return getPagedWidgetIds(Arrays.asList(value.split(getDelimiter())));
		}
		return value;
	}

	@NonNull
	private String getPagedWidgetIds(@NonNull List<String> pages) {
		StringBuilder builder = new StringBuilder();

		Iterator<String> iterator = pages.iterator();
		while (iterator.hasNext()) {
			boolean pageSeparatorAdded = false;
			String page = iterator.next();
			for (String id : page.split(WIDGET_SEPARATOR)) {
				if (WidgetType.isComplexWidget(id)) {
					pageSeparatorAdded = true;
					builder.append(id).append(PAGE_SEPARATOR);
				} else {
					pageSeparatorAdded = false;
					builder.append(id).append(WIDGET_SEPARATOR);
				}
			}
			if (iterator.hasNext() && !pageSeparatorAdded) {
				builder.append(PAGE_SEPARATOR);
			}
		}
		return builder.toString();
	}


	@Override
	public CommonPreference<String> copyWithId(@NonNull String newId) {
		return setupCopy(new VerticalWidgetOrderPreference(getSettings(), newId, getDefaultValue(), getDelimiter()));
	}
}
