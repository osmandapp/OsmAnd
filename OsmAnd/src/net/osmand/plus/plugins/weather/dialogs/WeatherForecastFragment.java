package net.osmand.plus.plugins.weather.dialogs;

import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.BACK_TO_LOC_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_IN_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_OUT_BUTTON_ID;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.LabelFormatter;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherContour;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.widgets.WeatherWidgetsPanel;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.TimeFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuHelper.PopUpMenuWidthType;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherForecastFragment extends BaseOsmAndFragment {

	public static final String TAG = WeatherForecastFragment.class.getSimpleName();

	private static final String PREVIOUS_WEATHER_CONTOUR_KEY = "previous_weather_contour";

	private OsmandApplication app;
	private WeatherHelper weatherHelper;
	private WeatherPlugin plugin;

	private TimeSlider timeSlider;
	private RulerWidget rulerWidget;
	private WeatherWidgetsPanel widgetsPanel;

	private final Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	private final Calendar selectedDate = getDefaultCalendar();

	private final TimeFormatter timeShortFormatter = new TimeFormatter(Locale.getDefault(), "HH", "h a", TimeZone.getTimeZone("UTC"));
	private final SimpleDateFormat simpleHoursFormat = new SimpleDateFormat(" K", Locale.getDefault());

	private WeatherContour previousWeatherContour;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		weatherHelper = app.getWeatherHelper();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		selectedDate.setTime(currentDate.getTime());
		simpleHoursFormat.getCalendar().setTimeZone(TimeZone.getTimeZone("UTC"));

		if (savedInstanceState != null) {
			previousWeatherContour = WeatherContour.valueOf(savedInstanceState.getString(PREVIOUS_WEATHER_CONTOUR_KEY, WeatherContour.TEMPERATURE.name()));
		} else {
			previousWeatherContour = plugin.getSelectedContoursType();
		}
		plugin.setContoursType(plugin.getSelectedForecastContoursType());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity activity = requireMapActivity();
		View view = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.fragment_weather_forecast, container, false);
		AndroidUtils.addStatusBarPadding21v(activity, view);

		widgetsPanel = view.findViewById(R.id.weather_widgets_panel);
		widgetsPanel.setupWidgets(activity);

		setupToolBar(view);
		setupDatesView(view);
		setupTimeSlider(view);
		setupTimeLegend(view);
		buildZoomButtons(view);

		return view;
	}

	private void setupTimeSlider(@NonNull View view) {
		timeSlider = view.findViewById(R.id.time_slider);
		timeSlider.setValueTo(24);
		timeSlider.setValueFrom(0);
		timeSlider.setLabelFormatter(getLabelFormatter());

		Calendar calendar = getDefaultCalendar();
		timeSlider.addOnChangeListener((slider, value, fromUser) -> {
			calendar.setTime(selectedDate.getTime());
			calendar.set(Calendar.HOUR_OF_DAY, (int) value);

			updateSelectedDate(calendar.getTime());
		});
		UiUtilities.setupSlider(timeSlider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);
		timeSlider.setLabelBehavior(LABEL_FLOATING);
		timeSlider.setTrackActiveTintList(timeSlider.getTrackInactiveTintList());

		updateTimeSlider();
	}

	private void setupTimeLegend(@NonNull View view) {
		Calendar calendar = getDefaultCalendar();
		boolean twelveHoursFormat = !DateFormat.is24HourFormat(app);

		int hours = 0;
		ViewGroup timeLegend = view.findViewById(R.id.time_legend);
		for (int i = 0; i <= timeLegend.getChildCount(); i++) {
			View child = timeLegend.getChildAt(i);
			if (child instanceof TextView) {
				TextView textView = (TextView) child;
				textView.setText(getFormattedHours(calendar, hours, twelveHoursFormat));
				hours += 3;
			}
		}
	}

	private String getFormattedHours(@NonNull Calendar calendar, int hours, boolean twelveHoursFormat) {
		calendar.set(Calendar.HOUR_OF_DAY, hours);

		String formattedHours;
		if (twelveHoursFormat && hours != 12) {
			formattedHours = simpleHoursFormat.format(calendar.getTime());
		} else {
			formattedHours = timeShortFormatter.format(calendar.getTime(), twelveHoursFormat);
		}
		return formattedHours;
	}

	private LabelFormatter getLabelFormatter() {
		Calendar calendar = getDefaultCalendar();
		boolean twelveHoursFormat = !DateFormat.is24HourFormat(app);
		return value -> getFormattedHours(calendar, (int) value, twelveHoursFormat);
	}

	private void updateTimeSlider() {
		boolean today = OsmAndFormatter.isSameDay(selectedDate, currentDate);
		timeSlider.setValue(today ? currentDate.get(Calendar.HOUR_OF_DAY) : 12);
		timeSlider.setStepSize(today ? 1 : 3);
		timeSlider.setCurrentDate(today ? currentDate : null);
	}

	private void buildZoomButtons(@NonNull View view) {
		View zoomButtonsView = view.findViewById(R.id.map_hud_controls);

		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();

		layer.addMapButton(new ZoomInButton(activity, view.findViewById(R.id.map_zoom_in_button), ZOOM_IN_BUTTON_ID));
		layer.addMapButton(new ZoomOutButton(activity, view.findViewById(R.id.map_zoom_out_button), ZOOM_OUT_BUTTON_ID));
		layer.addMapButton(new MyLocationButton(activity, view.findViewById(R.id.map_my_location_button), BACK_TO_LOC_BUTTON_ID, false));

		AndroidUiHelper.updateVisibility(zoomButtonsView, true);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout));
	}

	private void setupDatesView(@NonNull View view) {
		ForecastAdapter adapter = new ForecastAdapter(view.getContext(), date -> {
			selectedDate.setTime(date);
			updateSelectedDate(date);
			updateTimeSlider();

			requireMapActivity().refreshMap();
			return true;
		}, nightMode);
		adapter.initDates(currentDate, selectedDate);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
	}

	private void setupToolBar(@NonNull View view) {
		ImageView backButton = view.findViewById(R.id.back_button);
		backButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(requireMapActivity())));
		backButton.setOnClickListener(v -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		view.findViewById(R.id.raster_layers).setOnClickListener(this::chooseLayers);
		view.findViewById(R.id.contour_layers).setOnClickListener(this::chooseContour);
	}

	public void updateSelectedDate(@Nullable Date date) {
		widgetsPanel.setSelectedDate(date);
		plugin.updateWeatherDate(date);
		requireMapActivity().refreshMap();
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = requireMapActivity();
		mapActivity.disableDrawer();
		mapActivity.getMapLayers().getMapInfoLayer().addSideWidgetsPanel(widgetsPanel);
		updateWidgetsVisibility(mapActivity, View.GONE);
		updateSelectedDate(selectedDate.getTime());
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = requireMapActivity();
		mapActivity.enableDrawer();
		mapActivity.getMapLayers().getMapInfoLayer().removeSideWidgetsPanel(widgetsPanel);
		updateWidgetsVisibility(mapActivity, View.VISIBLE);
		updateSelectedDate(null);
	}

	private void updateWidgetsVisibility(@NonNull MapActivity activity, int visibility) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapLayers mapLayers = mapActivity.getMapLayers();

			MapControlsLayer layer = mapLayers.getMapControlsLayer();
			layer.removeMapButtons(Arrays.asList(ZOOM_IN_BUTTON_ID, ZOOM_OUT_BUTTON_ID, BACK_TO_LOC_BUTTON_ID));

			if (rulerWidget != null) {
				MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
				mapInfoLayer.removeRulerWidgets(Collections.singletonList(rulerWidget));
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PREVIOUS_WEATHER_CONTOUR_KEY, previousWeatherContour.name());
	}

	@Override
	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			plugin.setSelectedContoursType(previousWeatherContour);
		}
		super.onDestroy();
	}

	private void chooseContour(@NonNull View view) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		List<PopUpMenuItem> items = new ArrayList<>();
		for (WeatherContour weatherContour : WeatherContour.values()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(weatherContour.getTitleId())
					.setIcon(getContentIcon(weatherContour.getIconId()))
					.showCompoundBtn(activeColor)
					.setOnClickListener(v -> {
						plugin.setSelectedForecastContoursType(weatherContour);
						requireMapActivity().refreshMap();
					})
					.setSelected(weatherContour == plugin.getSelectedForecastContoursType())
					.create()
			);
		}
		new PopUpMenuHelper.Builder(view, items, nightMode).setWidthType(PopUpMenuWidthType.STANDARD).show();
	}

	private void chooseLayers(@NonNull View view) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		List<PopUpMenuItem> items = new ArrayList<>();

		for (WeatherBand band : weatherHelper.getWeatherBands()) {
			boolean selected = band.isForecastBandVisible();
			Drawable icon = selected ? getIcon(band.getIconId(), ColorUtilities.getActiveColorId(nightMode)) : getContentIcon(band.getIconId());
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(band.getMeasurementName())
					.setIcon(icon)
					.showCompoundBtn(activeColor)
					.setOnClickListener(v -> {
						boolean visible = !band.isForecastBandVisible();
						band.setForecastBandVisible(visible);
						requireMapActivity().refreshMap();
					})
					.setSelected(selected)
					.create()
			);
		}
		new PopUpMenuHelper.Builder(view, items, nightMode, R.layout.popup_menu_item_checkbox).setWidthType(PopUpMenuWidthType.STANDARD).show();
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}

	@NonNull
	protected static Calendar getDefaultCalendar() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new WeatherForecastFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}