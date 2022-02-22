package net.osmand.plus.views.mapwidgets;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.configure.WidgetItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetsRegister {

	private static Map<WidgetsPanel, List<WidgetItem>> map;

	static {
		add(WidgetsPanel.LEFT, R.drawable.ic_action_next_turn, "Next turn", true);
		add(WidgetsPanel.LEFT, R.drawable.ic_action_next_turn, "Second next turn");
		add(WidgetsPanel.LEFT, R.drawable.ic_action_next_turn, "Next small");

		add(WidgetsPanel.RIGHT, R.drawable.widget_intermediate_day, "Intermediate destination", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_time_to_distance_day, "Intermediate arrival time", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_target_day, "Destination", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_time_day, "Arrival time", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_bearing_day, "Relative bearing", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_speed_day, "Speed", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_max_speed_day, "Speed limit", true);
		add(WidgetsPanel.RIGHT, R.drawable.widget_altitude_day, "Altitude");
		add(WidgetsPanel.RIGHT, R.drawable.widget_gps_info_day, "GPS info");
		add(WidgetsPanel.RIGHT, R.drawable.widget_monitoring_rec_big_day, "Trip recording");
		add(WidgetsPanel.RIGHT, R.drawable.widget_av_audio_day, "Audio/Video notes");
		add(WidgetsPanel.RIGHT, R.drawable.widget_battery_charging_day, "Battery level");
		add(WidgetsPanel.RIGHT, R.drawable.widget_bearing_day, "Radius ruller");
		add(WidgetsPanel.RIGHT, R.drawable.widget_fps_day, "FPS Debug info");

		add(WidgetsPanel.TOP, R.drawable.widget_coordinates_latitude_day, "Street name", true);
		add(WidgetsPanel.TOP, R.drawable.ic_action_lanes, "Lanes", true);
		add(WidgetsPanel.TOP, R.drawable.ic_action_marker_dark, "Map markers bar");
		add(WidgetsPanel.TOP, R.drawable.widget_coordinates_latitude_north_day, "Coordinates widget");

//		add(WidgetsGroup.BOTTOM, R.drawable.ic_action_elevation, "Elevation widget");
	}

	private static void add(@NonNull WidgetsPanel panel,
	                        @NonNull Integer iconId,
	                        @NonNull String title) {
		add(panel, iconId, title, false);
	}

	private static void add(@NonNull WidgetsPanel panel,
	                        @NonNull Integer iconId,
	                        @NonNull String title,
	                        boolean enabled) {
		if (map == null) {
			map = new HashMap<>();
		}

		List<WidgetItem> list = map.get(panel);
		if (list == null) {
			list = new ArrayList<>();
			map.put(panel, list);
		}

		WidgetItem widget = new WidgetItem(iconId, title);
		widget.setActive(enabled);
		widget.setPriority(list.size());
		list.add(widget);
	}

	@NonNull
	public static List<WidgetItem> getSortedWidgets(@NonNull ApplicationMode appMode,
	                                                @NonNull WidgetsPanel panel) {
		return getSortedWidgets(appMode, panel, false);
	}

	@NonNull
	public static List<WidgetItem> getSortedWidgets(@NonNull ApplicationMode appMode,
	                                                @NonNull WidgetsPanel panel,
	                                                boolean defaultState) {
		List<WidgetItem> widgets = map.get(panel);
		return widgets != null ? widgets : new ArrayList<>();
	}

}
