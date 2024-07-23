package net.osmand.plus.plugins.weather.dialogs;

import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.BACK_TO_LOC_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_IN_BUTTON_ID;
import static net.osmand.plus.routepreparationmenu.ChooseRouteFragment.ZOOM_OUT_BUTTON_ID;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.LabelFormatter;

import net.osmand.PlatformUtil;
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
import net.osmand.plus.plugins.weather.WeatherRasterLayer;
import net.osmand.plus.plugins.weather.WeatherUtils;
import net.osmand.plus.plugins.weather.WeatherWebClient.DownloadState;
import net.osmand.plus.plugins.weather.WeatherWebClient.WeatherWebClientListener;
import net.osmand.plus.plugins.weather.widgets.WeatherWidgetsPanel;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.TimeFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.CustomMapButton;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class WeatherForecastFragment extends BaseOsmAndFragment implements WeatherWebClientListener {

	public static final String TAG = WeatherForecastFragment.class.getSimpleName();
	public static final String CHOOSE_LAYER_BTN = "CHOOSE_LAYER";
	public static final String CHOOSE_CONTOURS_BTN = "CHOOSE_CONTOURS";
	private final Log log = PlatformUtil.getLog(WeatherForecastFragment.class);

	private static final String PREVIOUS_WEATHER_CONTOUR_KEY = "previous_weather_contour";
	private static final long MIN_UTC_HOURS_OFFSET = 24 * 60 * 60 * 1000;
	public static final int ANIMATION_FRAME_DELAY = 83;
	public static final int DOWNLOAD_COMPLETE_DELAY = 250;
	public static final int ANIMATION_START_DELAY = 100;
	private static final int MAX_FORECAST_DAYS = 7;
	private static final int NEXT_DAY_START_HOUR = 9;

	private WeatherHelper weatherHelper;
	private WeatherPlugin plugin;

	private TimeSlider timeSlider;
	private RulerWidget rulerWidget;
	private WeatherWidgetsPanel widgetsPanel;
	private Handler progressUpdateHandler;
	private Handler animateForecastHandler;

	private final Calendar currentDate = getDefaultCalendar();
	private final Calendar selectedDate = getDefaultCalendar();
	private final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

	private final TimeFormatter timeFormatter = new TimeFormatter(Locale.getDefault(), "HH:mm", "h:mm a");

	private WeatherContour previousWeatherContour;
	private AnimationState animationState = AnimationState.IDLE;
	private boolean downloading = false;
	private ImageView playForecastBtnIcon;
	private int currentStep;
	private int animationStartStep;
	private int animateStepCount;
	private int animationStartStepCount;

	private ImageView chooseLayersBtn;
	private ImageView chooseContoursBtn;

	private enum AnimationState {
		IDLE,
		STARTED,
		IN_PROGRESS,
		SUSPENDED
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	protected boolean isUsedOnMap() {
		return false;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		progressUpdateHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));
		animateForecastHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));
		weatherHelper = app.getWeatherHelper();
		weatherHelper.addDownloadStateListener(this);
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

	private void showProgressBar(boolean show) {
		if (getView() != null) {
			View progressBar = getView().findViewById(R.id.load_forecast_progress);
			app.runInUIThread(() -> AndroidUiHelper.setVisibility(show ? View.VISIBLE : View.INVISIBLE, progressBar));
		}
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
		widgetsPanel.setupWidgets(activity, nightMode);
		widgetsPanel.nightMode = nightMode;

		setupPLayForecastButton(view);
		setupToolBar(view);
		setupWeatherButtons(view);
		setupDatesView(view);
		setupTimeSlider(view);
		buildZoomButtons(view);
		moveCompassButton(view);

		return view;
	}

	private void setupPLayForecastButton(View view) {
		View playForecastBtn = view.findViewById(R.id.play_forecast_button);
		playForecastBtnIcon = view.findViewById(R.id.play_forecast_button_icon);
		playForecastBtn.setOnClickListener((v) -> onPlayForecastClicked());
		updatePlayForecastButton();
	}

	private void updatePlayForecastButton() {
		int iconResId = animationState == AnimationState.IDLE ? R.drawable.ic_play_dark : R.drawable.ic_pause;
		Drawable iconDrawable = app.getUIUtilities().getIcon(iconResId, ColorUtilities.getActiveIconColorId(nightMode));
		playForecastBtnIcon.setImageDrawable(iconDrawable);
	}

	private void onPlayForecastClicked() {
		AnimationState animationState = this.animationState == AnimationState.IDLE ? AnimationState.STARTED : AnimationState.IDLE;
		this.animationState = animationState;
		if (animationState == AnimationState.STARTED) {
			Calendar calendar = getDefaultCalendar();
			calendar.setTime(selectedDate.getTime());
			int hour = (int) timeSlider.getValue();
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, (int) ((timeSlider.getValue() - (float) hour) * 60.0f));
			plugin.prepareForDayAnimation(calendar.getTime());
			requireMapActivity().refreshMap();
			currentStep = (int) (timeSlider.getValue() / timeSlider.getStepSize());
			animationStartStep = currentStep;
			animateStepCount = (int) (WeatherRasterLayer.FORECAST_ANIMATION_DURATION_HOURS / timeSlider.getStepSize()) - 1;
			animationStartStepCount = animateStepCount;
			updateSliderValue();
			updateSelectedDate(calendar.getTime(), true, true);
			scheduleAnimationStart();
		} else {
			animateForecastHandler.removeCallbacksAndMessages(null);
		}
		updatePlayForecastButton();
	}

	private void stopAnimation() {
		timeSlider.hideLabel();
		animationState = AnimationState.IDLE;
		animateForecastHandler.removeCallbacksAndMessages(null);
		updateProgressBar();
		updatePlayForecastButton();
	}

	private void moveToNextForecastFrame() {
		animateForecastHandler.removeCallbacksAndMessages(null);
		AnimationState animationState = this.animationState;
		if (animationState == AnimationState.IDLE) {
			return;
		}
		if (downloading) {
			this.animationState = AnimationState.SUSPENDED;
			return;
		}
		if (weatherHelper.isProcessingTiles()) {
			this.animationState = AnimationState.SUSPENDED;
			updateProgressBar();
			animateForecastHandler.postDelayed(this::moveToNextForecastFrame, ANIMATION_START_DELAY);
			return;
		}
		if (currentStep + 1 > getStepsCount() || animateStepCount <= 0) {
			currentStep = animationStartStep;
			animateStepCount = animationStartStepCount;
		} else {
			currentStep++;
			animateStepCount--;
		}
		updateProgressBar();
		updateSliderValue();
		if (animationState == AnimationState.STARTED || animationState == AnimationState.SUSPENDED) {
			animationState = AnimationState.IN_PROGRESS;
			this.animationState = animationState;
			updateProgressBar();
		}
		if (animationState == AnimationState.IN_PROGRESS) {
			animateForecastHandler.postDelayed(this::moveToNextForecastFrame, ANIMATION_FRAME_DELAY);
		}
	}

	private void updateSliderValue() {
		float newValue = timeSlider.getValueFrom() + currentStep * timeSlider.getStepSize();
		timeSlider.setValue(Math.min(newValue, timeSlider.getValueTo()));
		timeSlider.showLabel();
	}

	private void updateProgressBar() {
		showProgressBar(downloading || animationState != AnimationState.IDLE && weatherHelper.isProcessingTiles());
	}

	private int getStepsCount() {
		if (timeSlider != null) {
			return (int) ((timeSlider.getValueTo() - timeSlider.getValueFrom()) / timeSlider.getStepSize());
		} else {
			return 0;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		stopAnimation();
	}

	private void setupTimeSlider(@NonNull View view) {
		timeSlider = view.findViewById(R.id.time_slider);
		timeSlider.setValueTo(24);
		timeSlider.setValueFrom(0);
		timeSlider.setLabelFormatter(getLabelFormatter());

		Calendar calendar = getDefaultCalendar();
		timeSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser && animationState != AnimationState.IDLE) {
				stopAnimation();
			}
			calendar.setTime(selectedDate.getTime());
			int hour = (int) value;
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, Math.round((value - (float) hour) * 60.0f));

			updateSelectedDate(calendar.getTime(), !fromUser, false);
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
			int hour = (int) value;
			int minute = Math.round((value - (float) hour) * 60.0f);
			calendar.set(Calendar.MINUTE, minute);
			return timeFormatter.format(calendar.getTime(), twelveHoursFormat);
		};
	}

	private void updateTimeSlider() {
		boolean today = OsmAndFormatter.isSameDay(selectedDate, currentDate);
		timeSlider.setValue(today ? currentDate.get(Calendar.HOUR_OF_DAY) : NEXT_DAY_START_HOUR);
		timeSlider.setStepSize(1.0f / 24.0f); //2.5 minutes
	}

	private void buildZoomButtons(@NonNull View view) {
		View zoomButtonsView = view.findViewById(R.id.map_hud_controls);

		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();

		ImageView zoomInBtn = view.findViewById(R.id.map_zoom_in_button);
		if (zoomInBtn != null) {
			layer.addMapButton(new ZoomInButton(activity, zoomInBtn, ZOOM_IN_BUTTON_ID));
		}
		ImageView zoomOutBtn = view.findViewById(R.id.map_zoom_out_button);
		if (zoomInBtn != null) {
			layer.addMapButton(new ZoomOutButton(activity, zoomOutBtn, ZOOM_OUT_BUTTON_ID));
		}
		ImageView myLocationBtn = view.findViewById(R.id.map_my_location_button);
		if (zoomInBtn != null) {
			layer.addMapButton(new MyLocationButton(activity, myLocationBtn, BACK_TO_LOC_BUTTON_ID, false));
		}

		AndroidUiHelper.updateVisibility(zoomButtonsView, true);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout));
	}

	private void setupDatesView(@NonNull View view) {
		List<ChipItem> chips = createDatesChipItems(currentDate);
		HorizontalChipsView chipsView = view.findViewById(R.id.chips_view);
		chipsView.setItems(chips);
		chipsView.setOnSelectChipListener(chip -> {
			stopAnimation();
			Date date = (Date) chip.tag;
			selectedDate.setTime(date);
			updateSelectedDate(date, false, false);
			updateTimeSlider();
			requireMapActivity().refreshMap();
			return true;
		});
		ChipItem selected = chipsView.findChipByTag(selectedDate.getTime());
		chipsView.setSelected(selected);
	}

	@NonNull
	private List<ChipItem> createDatesChipItems(@NonNull Calendar currentDate) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate.getTime());
		List<ChipItem> chipItems = new ArrayList<>();
		SimpleDateFormat formatter = new SimpleDateFormat("E", Locale.getDefault());
		for (int i = 0; i <= MAX_FORECAST_DAYS; i++) {
			String title;
			switch (i) {
				case 0:
					title = app.getString(R.string.today);
					break;
				case 1:
					title = app.getString(R.string.tomorrow);
					break;
				default:
					title = formatter.format(calendar.getTime());
					break;
			}
			ChipItem chip = new ChipItem(title);
			chip.title = title;
			chip.contentDescription = title;
			chip.tag = calendar.getTime();
			chipItems.add(chip);
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, NEXT_DAY_START_HOUR);
		}
		return chipItems;
	}

	private void setupToolBar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)));
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_arrow_back, ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			requireActivity().onBackPressed();
		});
		toolbar.setTitle(R.string.shared_string_weather);
		toolbar.setBackgroundColor(app.getColor(nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));
		toolbar.getMenu().findItem(R.id.weather_data_source).setVisible(false);
		toolbar.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.weather_data_source) {
				onOptionBtnClicked();
			}
			return false;
		});
	}

	private void onOptionBtnClicked() {
		SelectWeatherSourceBottomSheet.showInstance(requireMapActivity().getSupportFragmentManager(), this);
	}

	private void setupWeatherButtons(@NonNull View view) {
		chooseLayersBtn = view.findViewById(R.id.raster_layers_btn);
		chooseLayersBtn.setOnClickListener(this::chooseLayers);
		chooseContoursBtn = view.findViewById(R.id.contour_layers_btn);
		chooseContoursBtn.setOnClickListener(this::chooseContour);
		updateChooseLayersButton();
		updateChooseContoursButton();
	}

	public void updateSelectedDate(@Nullable Date date, boolean forAnimation, boolean resetPeriod) {
		plugin.setForecastDate(date, forAnimation, resetPeriod);
		if (date != null)
			date.setTime(WeatherUtils.roundForecastTimeToHour(date.getTime()));
		checkDateOffset(date);
		widgetsPanel.setSelectedDate(date);
		requireMapActivity().refreshMap();
	}

	@Override
	protected void updateNightMode() {
		super.updateNightMode();
		if (widgetsPanel != null) {
			widgetsPanel.nightMode = nightMode;
		}
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
		mapActivity.getMapLayers().getMapInfoLayer().addAdditionalWidgetsContainer(widgetsPanel);
		updateWidgetsVisibility(mapActivity, View.GONE);
		updateSelectedDate(selectedDate.getTime(), false, false);
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = requireMapActivity();
		mapActivity.enableDrawer();
		mapActivity.getMapLayers().getMapInfoLayer().removeAdditionalWidgetsContainer(widgetsPanel);
		updateWidgetsVisibility(mapActivity, View.VISIBLE);
		updateSelectedDate(null, false, false);
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
			layer.removeMapButtons(Arrays.asList(ZOOM_IN_BUTTON_ID, ZOOM_OUT_BUTTON_ID, BACK_TO_LOC_BUTTON_ID, CHOOSE_CONTOURS_BTN, CHOOSE_LAYER_BTN));
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
		weatherHelper.removeDownloadStateListener(this);
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
					updateChooseContoursButton();
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
						updateChooseContoursButton();
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
						updateChooseLayersButton();
					})
					.showTopDivider(band.getBandIndex() == WeatherBand.WEATHER_BAND_WIND_ANIMATION)
					.setSelected(selected)
					.create()
			);
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider_check_box;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void updateChooseLayersButton() {
		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();

		boolean anyBandSelected = weatherHelper.getVisibleForecastBands().size() > 0;
		layer.removeMapButtons(Collections.singletonList(CHOOSE_LAYER_BTN));
		layer.addMapButton(new CustomMapButton(requireMapActivity(),
				chooseLayersBtn,
				CHOOSE_LAYER_BTN,
				R.drawable.ic_layer_top, R.drawable.ic_layer_top,
				anyBandSelected ? R.color.icon_color_active_dark : R.color.icon_color_default_dark,
				anyBandSelected ? R.color.icon_color_active_light : R.color.icon_color_default_light));
	}

	private void updateChooseContoursButton() {
		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();
		boolean contourSelected = plugin.getSelectedForecastContoursType() != null;
		int iconResId = contourSelected ? R.drawable.ic_plugin_srtm : R.drawable.ic_action_contour_lines_disable;
		layer.removeMapButtons(Collections.singletonList(CHOOSE_CONTOURS_BTN));
		layer.addMapButton(new CustomMapButton(requireMapActivity(),
				chooseContoursBtn,
				CHOOSE_CONTOURS_BTN,
				iconResId, iconResId,
				contourSelected ? R.color.icon_color_active_dark : R.color.icon_color_default_dark,
				contourSelected ? R.color.icon_color_active_light : R.color.icon_color_default_light));
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

	public void onDownloadStateChanged(@NonNull DownloadState downloadState, int activeRequestsCounter) {
		progressUpdateHandler.removeCallbacksAndMessages(null);
		progressUpdateHandler.post(() -> {
			if (!downloading) {
				downloading = true;
				updateProgressBar();
			}
		});
		progressUpdateHandler.postDelayed(() -> {
			if (weatherHelper.getActiveRequestsCount() == 0) {
				downloading = false;
				if (animationState == AnimationState.STARTED || animationState == AnimationState.SUSPENDED) {
					scheduleAnimationStart();
				}
				updateProgressBar();
			}
		}, DOWNLOAD_COMPLETE_DELAY);
	}

	private void scheduleAnimationStart() {
		progressUpdateHandler.removeCallbacksAndMessages(null);
		progressUpdateHandler.postDelayed(() -> {
			if (!downloading) {
				moveToNextForecastFrame();
			}
		}, ANIMATION_START_DELAY);
	}
}