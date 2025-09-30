package net.osmand.plus.plugins.weather.widgets;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WeatherLayersButton extends MapButton {

	private final WeatherHelper weatherHelper;
	private final WeatherPlugin weatherPlugin;
	private WeatherPlugin.WeatherSourceChangeListener weatherSourceChangeListener;

	public WeatherLayersButton(@NonNull Context context) {
		this(context, null);
	}

	public WeatherLayersButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherLayersButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.weatherHelper = app.getWeatherHelper();
		this.weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);

		setOnClickListener(v -> chooseLayers());
		
		if (weatherPlugin != null) {
			weatherSourceChangeListener = newSource -> {
				updateColors(nightMode);
				if (newSource == WeatherSource.ECMWF) {
					for (WeatherBand band : weatherHelper.getWeatherBands()) {
						boolean isWindOrCloud = band.getBandIndex() == WeatherBand.WEATHER_BAND_WIND_SPEED || 
											   band.getBandIndex() == WeatherBand.WEATHER_BAND_CLOUD;
						if (isWindOrCloud && band.isForecastBandVisible()) {
							band.setForecastBandVisible(false);
						}
					}

					if (mapActivity != null) {
						mapActivity.refreshMap();
					}
				}
			};
			weatherPlugin.addWeatherSourceChangeListener(weatherSourceChangeListener);
		}
	}

	@NonNull
	@NotNull
	@Override
	public String getButtonId() {
		return "choose_layer";
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return null;
	}

	@Override
	protected boolean shouldShow() {
		return true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (weatherSourceChangeListener != null && weatherPlugin != null) {
			weatherPlugin.removeWeatherSourceChangeListener(weatherSourceChangeListener);
			weatherSourceChangeListener = null;
		}
	}

	@Override
	protected void updateColors(boolean nightMode) {
		Context context = getContext();
		boolean anySelected = weatherHelper.getVisibleForecastBands().size() > 0;
		if (nightMode) {
			setIconColor(ColorUtilities.getColor(context, anySelected ? R.color.icon_color_active_dark : R.color.icon_color_default_dark));
		} else {
			setIconColor(ColorUtilities.getColor(context, anySelected ? R.color.icon_color_active_light : R.color.icon_color_default_light));
		}
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(context, nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode));
	}

	private void chooseLayers() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		WeatherSource currentSource = weatherPlugin != null ? weatherPlugin.getWeatherSource() : WeatherSource.GFS;
		boolean isECMWF = currentSource == WeatherSource.ECMWF;
		
		for (WeatherBand band : weatherHelper.getWeatherBands()) {
			boolean selected = band.isForecastBandVisible();
			boolean isWindOrCloud = band.getBandIndex() == WeatherBand.WEATHER_BAND_WIND_SPEED || 
								   band.getBandIndex() == WeatherBand.WEATHER_BAND_CLOUD;
			boolean shouldDisable = isECMWF && isWindOrCloud;

			if (shouldDisable) {
				continue;
			}
			
			Drawable icon = selected ? uiUtilities.getIcon(band.getIconId(),
					ColorUtilities.getActiveColorId(nightMode)) : uiUtilities.getThemedIcon(band.getIconId());
			
			PopUpMenuItem.Builder builder = new PopUpMenuItem.Builder(app)
					.setTitle(band.getMeasurementName())
					.setIcon(icon)
					.showCompoundBtn(activeColor)
					.showTopDivider(band.getBandIndex() == WeatherBand.WEATHER_BAND_WIND_ANIMATION)
					.setSelected(selected);
			
			if (!shouldDisable) {
				builder.setOnClickListener(v -> {
					boolean visible = !band.isForecastBandVisible();
					band.setForecastBandVisible(visible);
					mapActivity.refreshMap();
				});
			}
			
			menuItems.add(builder.create());
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = this;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider_check_box;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		return new ButtonAppearanceParams("ic_layer_top", 52, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}
}