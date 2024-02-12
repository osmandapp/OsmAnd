package net.osmand.plus.card.color.themedpalette;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.BaseColorsPaletteController;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;

import java.util.List;

public abstract class ThemedColorsPaletteController extends BaseColorsPaletteController {

	private OnMapThemeChangeListener mapThemeChangeListener;
	private boolean useNightMap;

	public ThemedColorsPaletteController(@NonNull OsmandApplication app,
	                                     @NonNull List<Integer> defaultColors,
	                                     @NonNull ListStringPreference customColorsPreference,
	                                     @Nullable ApplicationMode appMode, int selectedColor) {
		super(app, defaultColors, customColorsPreference, appMode, selectedColor);
		DayNightHelper dayNightHelper = app.getDaynightHelper();
		useNightMap = dayNightHelper.isNightModeForMapControls();
	}

	public void setMapThemeChangeListener(@Nullable OnMapThemeChangeListener mapThemeChangeListener) {
		this.mapThemeChangeListener = mapThemeChangeListener;
	}

	public boolean isUseNightMap() {
		return useNightMap;
	}

	public void setUseNightMap(boolean useNightMap) {
		if (this.useNightMap != useNightMap) {
			this.useNightMap = useNightMap;
			notifyMapThemeChanged();
		}
	}

	protected void notifyMapThemeChanged() {
		if (mapThemeChangeListener != null) {
			mapThemeChangeListener.onMapThemeChanged();
		}
		selectColor(getSelectedColor(useNightMap));
	}

	@ColorInt
	protected abstract int getSelectedColor(boolean nightMode);

	public interface OnMapThemeChangeListener {
		void onMapThemeChanged();
	}
}
