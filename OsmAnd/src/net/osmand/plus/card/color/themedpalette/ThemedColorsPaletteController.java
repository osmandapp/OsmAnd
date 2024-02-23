package net.osmand.plus.card.color.themedpalette;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.ColorsPaletteController;
import net.osmand.plus.card.color.palette.data.ColorsCollection;
import net.osmand.plus.card.color.palette.data.PaletteColor;
import net.osmand.plus.helpers.DayNightHelper;

public abstract class ThemedColorsPaletteController extends ColorsPaletteController {

	private OnMapThemeChangeListener mapThemeChangeListener;
	private boolean useNightMap;

	public ThemedColorsPaletteController(@NonNull OsmandApplication app,
	                                     @NonNull ColorsCollection colorsCollection,
	                                     int selectedColor) {
		super(app, colorsCollection, selectedColor);
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

	@Nullable
	protected abstract PaletteColor getSelectedColor(boolean nightMode);

	public interface OnMapThemeChangeListener {
		void onMapThemeChanged();
	}
}
