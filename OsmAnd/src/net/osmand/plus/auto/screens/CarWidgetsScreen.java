package net.osmand.plus.auto.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.CarWidgetsPanel;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lets the driver pick which widgets are drawn over the map on the car screen. The selection is
 * kept per profile in {@link OsmandSettings#AA_WIDGETS} and is independent of the widgets
 * configured for the phone panels.
 */
public class CarWidgetsScreen extends BaseAndroidAutoScreen {

	@NonNull
	private final OsmandSettings settings;

	public CarWidgetsScreen(@NonNull CarContext carContext) {
		super(carContext);
		settings = ((OsmandApplication) carContext.getApplicationContext()).getSettings();
	}

	@NonNull
	@Override
	public Template getTemplate() {
		OsmandApplication app = getApp();
		ApplicationMode appMode = settings.getApplicationMode();
		List<String> selected = CarWidgetsPanel.getSelectedWidgetIds(settings.AA_WIDGETS.getModeValue(appMode));

		ItemList.Builder listBuilder = new ItemList.Builder();
		int count = 0;
		for (WidgetType widgetType : getAvailableWidgets(app, appMode)) {
			if (count++ >= getContentLimit()) {
				break;
			}
			boolean checked = selected.contains(widgetType.id);
			listBuilder.addItem(new Row.Builder()
					.setTitle(app.getString(widgetType.titleId))
					.setImage(new CarIcon.Builder(IconCompat.createWithResource(app, widgetType.dayIconId)).build())
					.setToggle(new Toggle.Builder(value -> onWidgetToggled(appMode, widgetType, value))
							.setChecked(checked)
							.build())
					.build());
		}
		return new ListTemplate.Builder()
				.setSingleList(listBuilder.build())
				.setHeaderAction(Action.BACK)
				.setTitle(app.getString(R.string.shared_string_widgets))
				.build();
	}

	private void onWidgetToggled(@NonNull ApplicationMode appMode, @NonNull WidgetType widgetType,
			boolean enabled) {
		List<String> ids = new ArrayList<>(
				CarWidgetsPanel.getSelectedWidgetIds(settings.AA_WIDGETS.getModeValue(appMode)));
		ids.remove(widgetType.id);
		if (enabled) {
			ids.add(widgetType.id);
		}
		settings.AA_WIDGETS.setModeValue(appMode,
				Algorithms.encodeCollection(ids, CarWidgetsPanel.WIDGETS_SEPARATOR));
	}

	@NonNull
	private List<WidgetType> getAvailableWidgets(@NonNull OsmandApplication app,
			@NonNull ApplicationMode appMode) {
		List<WidgetsPanel> panels = Collections.singletonList(WidgetsPanel.RIGHT);
		List<WidgetType> widgets = new ArrayList<>();
		for (WidgetType widgetType : WidgetType.values()) {
			if (widgetType.isAllowed() && widgetType.isPanelsAllowed(panels)
					&& WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetType.id, appMode)) {
				widgets.add(widgetType);
			}
		}
		return widgets;
	}
}
