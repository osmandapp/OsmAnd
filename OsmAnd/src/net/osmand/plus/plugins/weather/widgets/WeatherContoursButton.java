package net.osmand.plus.plugins.weather.widgets;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherContour;
import net.osmand.plus.plugins.weather.WeatherPlugin;
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

public class WeatherContoursButton extends MapButton {

	private final WeatherPlugin plugin;

	public WeatherContoursButton(@NonNull Context context) {
		this(context, null);
	}

	public WeatherContoursButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherContoursButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.plugin = PluginsHelper.getPlugin(WeatherPlugin.class);

		setOnClickListener(v -> chooseContours());
	}

	@NonNull
	@NotNull
	@Override
	public String getButtonId() {
		return "choose_contours";
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
		boolean contourSelected = plugin.getSelectedForecastContoursType() != null;
		Context context = getContext();
		if (nightMode) {
			setIconColor(ColorUtilities.getColor(context, contourSelected ? R.color.icon_color_active_dark : R.color.icon_color_default_dark));
		} else {
			setIconColor(ColorUtilities.getColor(context, contourSelected ? R.color.icon_color_active_light : R.color.icon_color_default_light));
		}
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(context, nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode));
	}

	private void chooseContours() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_none)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_thermometer))
				.showCompoundBtn(activeColor)
				.setOnClickListener(v -> {
					plugin.setSelectedForecastContoursType(null);
					mapActivity.refreshMap();
				})
				.setSelected(plugin.getSelectedForecastContoursType() == null)
				.create()
		);

		for (WeatherContour weatherContour : WeatherContour.values()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(weatherContour.getTitleId())
					.setIcon(uiUtilities.getThemedIcon(weatherContour.getIconId()))
					.showCompoundBtn(activeColor)
					.setOnClickListener(v -> {
						plugin.setSelectedForecastContoursType(weatherContour);
						mapActivity.refreshMap();
					})
					.setSelected(weatherContour == plugin.getSelectedForecastContoursType())
					.create()
			);
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = this;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		boolean contourSelected = plugin.getSelectedForecastContoursType() != null;
		String iconName = contourSelected ? "ic_plugin_srtm" : "ic_action_contour_lines_disable";
		return new ButtonAppearanceParams(iconName, 52, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}
}
