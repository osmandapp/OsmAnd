package net.osmand.plus.plugins.weather.widgets;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
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

	public WeatherLayersButton(@NonNull Context context) {
		this(context, null);
	}

	public WeatherLayersButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherLayersButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.weatherHelper = app.getWeatherHelper();

		setOnClickListener(v -> chooseLayers());
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
		for (WeatherBand band : weatherHelper.getWeatherBands()) {
			boolean selected = band.isForecastBandVisible();
			Drawable icon = selected ? uiUtilities.getIcon(band.getIconId(),
					ColorUtilities.getActiveColorId(nightMode)) : uiUtilities.getThemedIcon(band.getIconId());
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(band.getMeasurementName())
					.setIcon(icon)
					.showCompoundBtn(activeColor)
					.setOnClickListener(v -> {
						boolean visible = !band.isForecastBandVisible();
						band.setForecastBandVisible(visible);
						mapActivity.refreshMap();
					})
					.showTopDivider(band.getBandIndex() == WeatherBand.WEATHER_BAND_WIND_ANIMATION)
					.setSelected(selected)
					.create()
			);
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