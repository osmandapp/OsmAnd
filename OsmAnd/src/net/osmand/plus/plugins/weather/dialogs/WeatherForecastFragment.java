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
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.LabelFormatter;

import net.osmand.core.jni.WeatherLayer;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherContour;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.WeatherUtils;
import net.osmand.plus.plugins.weather.widgets.WeatherWidgetsPanel;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.TimeFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

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
	private static final long MIN_UTC_HOURS_OFFSET = 24 * 60 * 60 * 1000;

	private WeatherHelper weatherHelper;
	private WeatherPlugin plugin;

	private TimeSlider timeSlider;
	private RulerWidget rulerWidget;
	private WeatherWidgetsPanel widgetsPanel;

	private final Calendar currentDate = getDefaultCalendar();
	private final Calendar selectedDate = getDefaultCalendar();
	private final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

	private final TimeFormatter timeFormatter = new TimeFormatter(Locale.getDefault(), "HH:mm", "h:mm a");

	private WeatherContour previousWeatherContour;


	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_light;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		weatherHelper = app.getWeatherHelper();
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);

		currentDate.setTimeInMillis(WeatherUtils.roundForecastTimeToHour(System.currentTimeMillis()));
		selectedDate.setTime(currentDate.getTime());

		if (savedInstanceState != null) {
			previousWeatherContour = WeatherContour.valueOf(savedInstanceState.getString(PREVIOUS_WEATHER_CONTOUR_KEY, WeatherContour.TEMPERATURE.name()));
		} else {
			previousWeatherContour = plugin.getSelectedContoursType();
			zoomOutToMaxLayersZoom();
		}
		plugin.setContoursType(plugin.getSelectedForecastContoursType());
	}

	private void zoomOutToMaxLayersZoom() {
		WeatherTileResourcesManager weatherResourcesManager = weatherHelper.getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return;
		}

		int maxWeatherLayerZoom = -1;

		int maxContoursZoom = weatherResourcesManager.getMaxTileZoom(WeatherType.Contour, WeatherLayer.High).swigValue();
		int maxContoursOverZoom = weatherResourcesManager.getMaxMissingDataZoomShift(WeatherType.Contour, WeatherLayer.High);
		maxWeatherLayerZoom = Math.max(maxWeatherLayerZoom, maxContoursZoom + maxContoursOverZoom);

		int maxRasterZoom = weatherResourcesManager.getMaxTileZoom(WeatherType.Raster, WeatherLayer.High).swigValue();
		int maxRasterOverZoom = weatherResourcesManager.getMaxMissingDataZoomShift(WeatherType.Raster, WeatherLayer.High);
		maxWeatherLayerZoom = Math.max(maxWeatherLayerZoom, maxRasterZoom + maxRasterOverZoom);

		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (maxWeatherLayerZoom != -1 && maxWeatherLayerZoom < mapView.getZoom()) {
			mapView.setIntZoom(maxWeatherLayerZoom);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity activity = requireMapActivity();
		View view = themedInflater.inflate(R.layout.fragment_weather_forecast, container, false);
		AndroidUtils.addStatusBarPadding21v(activity, view);

		widgetsPanel = view.findViewById(R.id.weather_widgets_panel);
		widgetsPanel.setupWidgets(activity);

		setupToolBar(view);
		setupDatesView(view);
		setupTimeSlider(view);
		buildZoomButtons(view);
		moveCompassButton(view);

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
			int hour = (int) value;
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, (int) ((value - (float) hour) * 60.0f));

			updateSelectedDate(calendar.getTime());
		});
		UiUtilities.setupSlider(timeSlider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);
		timeSlider.setLabelBehavior(LABEL_FLOATING);
		timeSlider.setTrackActiveTintList(timeSlider.getTrackInactiveTintList());

		updateTimeSlider();
	}

	private LabelFormatter getLabelFormatter() {
		Calendar calendar = getDefaultCalendar();
		boolean twelveHoursFormat = !DateFormat.is24HourFormat(app);
		return value -> {
			calendar.set(Calendar.HOUR_OF_DAY, (int) value);
			return timeFormatter.format(calendar.getTime(), twelveHoursFormat);
		};
	}

	private void updateTimeSlider() {
		boolean today = OsmAndFormatter.isSameDay(selectedDate, currentDate);
		timeSlider.setValue(today ? currentDate.get(Calendar.HOUR_OF_DAY) : 12);
		timeSlider.setStepSize(today ? 0.2f : 0.6f);
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
		plugin.setForecastDate(date);
		if (date != null)
			date.setTime(WeatherUtils.roundForecastTimeToHour(date.getTime()));
		checkDateOffset(date);
		widgetsPanel.setSelectedDate(date);
		requireMapActivity().refreshMap();
	}

	private void checkDateOffset(@Nullable Date date) {
		if (date != null && (date.getTime() - currentDate.getTimeInMillis() >= MIN_UTC_HOURS_OFFSET)) {
			utcCalendar.setTime(date);
			int hours = utcCalendar.get(Calendar.HOUR_OF_DAY);
			int offset = hours % 3;
			if (offset == 2) {
				utcCalendar.set(Calendar.HOUR_OF_DAY, hours + 1);
			} else if (offset == 1) {
				utcCalendar.set(Calendar.HOUR_OF_DAY, hours - 1);
			}
			date.setTime(utcCalendar.getTimeInMillis());
		}
	}

	private void moveCompassButton(@NonNull View view) {
		int btnSizePx = getDimensionPixelSize(R.dimen.map_small_button_size);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(btnSizePx, btnSizePx);
		int toolbarHeight = getDimensionPixelSize(R.dimen.toolbar_height);
		int topMargin = getDimensionPixelSize(R.dimen.map_small_button_margin);
		int startMargin = getDimensionPixelSize(R.dimen.map_button_margin);
		AndroidUtils.setMargins(params, startMargin, topMargin + toolbarHeight, 0, 0);

		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapLayers mapLayers = activity.getMapLayers();
			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.moveCompassButton((ViewGroup) view, params);
		}
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
			layer.restoreCompassButton();

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
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_none)
				.setIcon(getContentIcon(R.drawable.ic_action_thermometer))
				.showCompoundBtn(activeColor)
				.setOnClickListener(v -> {
					plugin.setSelectedForecastContoursType(null);
					requireMapActivity().refreshMap();
				})
				.setSelected(plugin.getSelectedForecastContoursType() == null)
				.create()
		);

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

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void chooseLayers(@NonNull View view) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (WeatherBand band : weatherHelper.getWeatherBands()) {
			boolean selected = band.isForecastBandVisible();
			Drawable icon = selected ? getIcon(band.getIconId(), ColorUtilities.getActiveColorId(nightMode)) : getContentIcon(band.getIconId());
			menuItems.add(new PopUpMenuItem.Builder(app)
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
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_checkbox;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
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
		Calendar calendar = Calendar.getInstance();
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