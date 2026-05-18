package net.osmand.plus.views.mapwidgets.widgetstates;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class MapMarkerSideWidgetState extends WidgetState {

	private final OsmandPreference<SideMarkerMode> mapMarkerModePref;
	private final OsmandPreference<MarkerClickBehaviour> markerClickBehaviourPref;
	private final OsmandPreference<Long> averageSpeedIntervalPref;
	private final boolean firstMarker;

	public MapMarkerSideWidgetState(@NonNull OsmandApplication app, @Nullable String customId, boolean firstMarker) {
		super(app);
		this.firstMarker = firstMarker;
		this.mapMarkerModePref = registerModePref(customId);
		this.averageSpeedIntervalPref = registerAverageSpeedIntervalPref(customId);
		this.markerClickBehaviourPref = registerMarkerClickBehaviourPref(customId);
	}

	@NonNull
	private OsmandPreference<SideMarkerMode> registerModePref(@Nullable String customId) {
		String prefId = firstMarker ? "first_map_marker_mode" : "second_map_marker_mode";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, SideMarkerMode.DISTANCE, SideMarkerMode.values(), SideMarkerMode.class)
				.makeProfile();
	}

	@NonNull
	private OsmandPreference<Long> registerAverageSpeedIntervalPref(@Nullable String customId) {
		String prefId = firstMarker ? "first_map_marker_interval" : "second_map_marker_interval";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerLongPreference(prefId, AverageSpeedComputer.DEFAULT_INTERVAL_MILLIS).makeProfile();
	}

	@NonNull
	private OsmandPreference<MarkerClickBehaviour> registerMarkerClickBehaviourPref(@Nullable String customId) {
		String prefId = firstMarker ? "first_map_marker_click_behaviour" : "second_map_marker_click_behaviour";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, MarkerClickBehaviour.SWITCH_MODE, MarkerClickBehaviour.values(), MarkerClickBehaviour.class).makeProfile();
	}

	@NonNull
	public OsmandPreference<SideMarkerMode> getMapMarkerModePref() {
		return mapMarkerModePref;
	}

	@NonNull
	public OsmandPreference<Long> getAverageSpeedIntervalPref() {
		return averageSpeedIntervalPref;
	}

	@NonNull
	public OsmandPreference<MarkerClickBehaviour> getMarkerClickBehaviourPref() {
		return markerClickBehaviourPref;
	}

	@NonNull
	@Override
	public String getTitle() {
		WidgetType widgetType = firstMarker ? WidgetType.SIDE_MARKER_1 : WidgetType.SIDE_MARKER_2;
		String title = app.getString(widgetType.titleId);
		String subtitle = mapMarkerModePref.get().getTitle(app);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, title, subtitle);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return mapMarkerModePref.get().getIconId(nightMode);
	}

	@Override
	public void changeToNextState() {
		mapMarkerModePref.set(mapMarkerModePref.get().next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerModePref(customId).setModeValue(appMode, mapMarkerModePref.getModeValue(sourceAppMode));
		registerAverageSpeedIntervalPref(customId).setModeValue(appMode, averageSpeedIntervalPref.getModeValue(sourceAppMode));
		registerMarkerClickBehaviourPref(customId).setModeValue(appMode, markerClickBehaviourPref.getModeValue(sourceAppMode));
	}

	public boolean isFirstMarker() {
		return firstMarker;
	}

	public enum SideMarkerMode {

		DISTANCE(R.string.distance, R.drawable.widget_marker_day, R.drawable.widget_marker_night, R.drawable.widget_marker_triangle),
		ESTIMATED_ARRIVAL_TIME(R.string.side_marker_eta, R.drawable.widget_marker_eta_day, R.drawable.widget_marker_eta_night, R.drawable.widget_marker_eta_triangle);

		@StringRes
		public final int titleId;
		@DrawableRes
		public final int dayIconId;
		@DrawableRes
		public final int nightIconId;
		@DrawableRes
		public final int foregroundIconId;

		SideMarkerMode(@StringRes int titleId,
		               @DrawableRes int dayIconId,
		               @DrawableRes int nightIconId,
		               @DrawableRes int foregroundIconId) {
			this.titleId = titleId;
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
			this.foregroundIconId = foregroundIconId;
		}

		@NonNull
		public String getTitle(@NonNull Context context) {
			return context.getString(titleId);
		}

		@DrawableRes
		public int getIconId(boolean nightMode) {
			return nightMode ? nightIconId : dayIconId;
		}

		@NonNull
		public SideMarkerMode next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}

	public enum MarkerClickBehaviour {
		SWITCH_MODE(R.string.shared_string_switch_mode),
		GO_TO_MARKER_LOCATION(R.string.go_to_marker_location);

		@StringRes
		public final int titleId;

		MarkerClickBehaviour(@StringRes int titleId) {
			this.titleId = titleId;
		}

		@NonNull
		public String getTitle(@NonNull Context context) {
			return context.getString(titleId);
		}
	}
}