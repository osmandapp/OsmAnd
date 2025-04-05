package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.COORDINATE_GRID;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController;
import net.osmand.plus.views.layers.CoordinatesGridLayerSettings;
import net.osmand.plus.settings.backend.ApplicationMode;

public class GridColorController extends MapColorPaletteController {

	private final CoordinatesGridLayerSettings gridLayerSettings;
	private final ApplicationMode appMode;
	private boolean applyChanges = false;

	public GridColorController(@NonNull OsmandApplication app,
	                           @NonNull CoordinatesGridLayerSettings gridLayerSettings) {
		super(app, gridLayerSettings.getGridColorDay(), gridLayerSettings.getGridColorNight());
		this.appMode = app.getSettings().getApplicationMode();
		this.gridLayerSettings = gridLayerSettings;
	}

	@NonNull
	@Override
	public String getDialogTitle() {
		return getString(R.string.grid_color);
	}

	@Override
	public void onCloseScreen(@NonNull MapActivity activity) {
		setSavedColors(applyChanges);
		activity.getSupportFragmentManager().popBackStack();
		activity.getDashboard().setDashboardVisibility(true, COORDINATE_GRID, false);
	}

	@Override
	public void onResetToDefault() {
		gridLayerSettings.resetGridColors(appMode);
		loadSavedColors();
		refreshSelectedPaletteColor();
	}

	private void refreshSelectedPaletteColor() {
		ModedColorsPaletteController paletteController = getColorsPaletteController();
		PaletteMode selectedMode = paletteController.getSelectedPaletteMode();
		PaletteColor newPaletteColor = paletteController.provideSelectedColorForPaletteMode(selectedMode);
		if (newPaletteColor != null) {
			paletteController.onSelectColorFromPalette(newPaletteColor, false);
		}
	}

	@Override
	protected void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		setSavedColor(paletteColor.getColor(), isNightMap());
		loadSavedColors();
		externalListener.onColorSelectedFromPalette(paletteColor);
	}

	@Override
	protected void onColorsPaletteModeChanged() {
		externalListener.onColorsPaletteModeChanged();
	}

	@Override
	public void onApplyChanges() {
		applyChanges = true;
		loadSavedColors();
	}

	private void loadSavedColors() {
		colorDay = getSavedColor(false);
		colorNight = getSavedColor(true);
	}

	@Override
	protected void setSavedColor(@ColorInt int color, boolean nightMode) {
		gridLayerSettings.setGridColor(appMode, color, nightMode);
	}

	@Override
	@ColorInt
	protected int getSavedColor(boolean nightMode) {
		return gridLayerSettings.getGridColor(appMode, nightMode);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull CoordinatesGridLayerSettings gridLayerSettings) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GridColorController controller = new GridColorController(app, gridLayerSettings);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		MapColorPaletteFragment.showInstance(manager);
	}
}

