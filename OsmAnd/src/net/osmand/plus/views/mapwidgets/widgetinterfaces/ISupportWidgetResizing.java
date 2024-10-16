package net.osmand.plus.views.mapwidgets.widgetinterfaces;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;

public interface ISupportWidgetResizing {
	@NonNull
	OsmandPreference<WidgetSize> getWidgetSizePref();

	void recreateView();
}
