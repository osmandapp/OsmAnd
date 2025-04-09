package net.osmand.plus.configmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController.OnPaletteModeSelectedListener;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DayNightHelper.MapThemeProvider;
import net.osmand.plus.settings.enums.DayNightMode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class MapColorPaletteController extends BaseDialogController implements MapThemeProvider {

	private static final String PROCESS_ID = "select_color_with_preview";

	private static final int PALETTE_MODE_ID_DAY = 0;
	private static final int PALETTE_MODE_ID_NIGHT = 1;

	private ModedColorsPaletteController colorsPaletteController;
	protected IMapColorPaletteControllerListener externalListener;
	private boolean initialNightMode;

	@ColorInt protected final int initialColorDay;
	@ColorInt protected final int initialColorNight;
	@ColorInt protected int colorDay;
	@ColorInt protected int colorNight;

	public interface IMapColorPaletteControllerListener extends OnColorsPaletteListener, OnPaletteModeSelectedListener {
		void updateStatusBar();
	}

	public MapColorPaletteController(@NonNull OsmandApplication app,
	                                 @ColorInt int initialColorDay,
	                                 @ColorInt int initialColorNight) {
		super(app);
		this.colorDay = this.initialColorDay = initialColorDay;
		this.colorNight = this.initialColorNight = initialColorNight;
		initialNightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setListener(@NonNull IMapColorPaletteControllerListener externalListener) {
		this.externalListener = externalListener;
	}

	public abstract void onCloseScreen(@NonNull MapActivity activity);

	@NonNull
	public abstract String getDialogTitle();

	public abstract void onApplyChanges();

	public abstract void onResetToDefault();

	public boolean hasChanges() {
		return initialColorDay != colorDay || initialColorNight != colorNight;
	}

	public void onResume() {
		setMapThemeProvider(this);
	}

	public void onPause() {
		setMapThemeProvider(null);
	}

	private void setMapThemeProvider(@Nullable MapThemeProvider provider) {
		DayNightHelper helper = app.getDaynightHelper();
		helper.setExternalMapThemeProvider(provider);
	}

	@Override
	public DayNightMode getMapTheme() {
		return isNightMap() ? DayNightMode.NIGHT : DayNightMode.DAY;
	}

	public boolean isNightMap() {
		ModedColorsPaletteController paletteController = getColorsPaletteController();
		PaletteMode paletteMode = paletteController.getSelectedPaletteMode();
		return Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
	}

	private void notifyAllColorsScreenClosed() {
		externalListener.updateStatusBar();
	}

	protected void setSavedColors(boolean applyChanges) {
		setSavedColor(applyChanges ? this.colorDay : this.initialColorDay, false);
		setSavedColor(applyChanges ? this.colorNight : this.initialColorNight, true);
	}

	protected abstract void setSavedColor(@ColorInt int color, boolean nightMode);

	@ColorInt
	protected abstract int getSavedColor(boolean nightMode);

	protected abstract void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor);

	protected abstract void onColorsPaletteModeChanged();

	@NonNull
	public ModedColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			colorsPaletteController = new ModedColorsPaletteController(app, colorsCollection) {

				private PaletteMode paletteModeDay;
				private PaletteMode paletteModeNight;

				@Override
				@NonNull
				protected List<PaletteMode> collectAvailablePaletteModes() {
					paletteModeDay = createPaletteMode(false);
					paletteModeNight = createPaletteMode(true);
					return Arrays.asList(paletteModeDay, paletteModeNight);
				}

				@NonNull
				@Override
				protected PaletteMode getInitialPaletteMode() {
					return initialNightMode ? paletteModeNight : paletteModeDay;
				}

				@Override
				public PaletteColor provideSelectedColorForPaletteMode(@NonNull PaletteMode paletteMode) {
					boolean useNightMap = Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
					return collection.findPaletteColor(useNightMap ? colorNight : colorDay, true);
				}

				@NonNull
				private PaletteMode createPaletteMode(boolean night) {
					String title = app.getString(night ? R.string.daynight_mode_night : R.string.daynight_mode_day);
					int tag = night ? PALETTE_MODE_ID_NIGHT : PALETTE_MODE_ID_DAY;
					return new PaletteMode(title, tag);
				}

				@Override
				public void onAllColorsScreenClosed() {
					notifyAllColorsScreenClosed();
				}
			};
		}
		colorsPaletteController.setPaletteListener(this::onColorSelectedFromPalette);
		colorsPaletteController.setPaletteModeSelectedListener(this::onColorsPaletteModeChanged);
		return colorsPaletteController;
	}

	@Nullable
	public static MapColorPaletteController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (MapColorPaletteController) dialogManager.findController(PROCESS_ID);
	}
}
