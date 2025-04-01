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
import net.osmand.plus.helpers.CoordinatesGridHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

public class GridColorController extends MapColorPaletteController {

	private final CoordinatesGridHelper gridHelper;
	private final ApplicationMode appMode;
	private boolean applyChanges = false;

	public GridColorController(@NonNull OsmandApplication app, @NonNull CoordinatesGridHelper gridHelper,
	                           @NonNull ApplicationMode appMode, @ColorInt int initialColorDay,
	                           @ColorInt int initialColorNight) {
		super(app, initialColorDay, initialColorNight);
		this.appMode = appMode;
		this.gridHelper = gridHelper;
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
		gridHelper.resetGridColors(appMode);
		loadSavedColors();
	}

	@Override
	protected void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		setSavedColor(paletteColor.getColor(), isNightMap());
		loadSavedColors();
		externalListener.onColorSelectedFromPalette(paletteColor);
	}

	@Override
	protected void onColorsPaletteModeChanged() {
		gridHelper.updateGridSettings();
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
		gridHelper.setGridColor(appMode, color, nightMode);
	}

	@Override
	@ColorInt
	protected int getSavedColor(boolean nightMode) {
		return gridHelper.getGridColor(appMode, nightMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		gridHelper.updateGridSettings();
	}

	public static void showDialog(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();

		CoordinatesGridHelper gridHelper = app.getOsmandMap().getMapView().getGridHelper();
		int colorDay = gridHelper.getGridColor(appMode, false);
		int colorNight = gridHelper.getGridColor(appMode, true);
		GridColorController controller = new GridColorController(app, gridHelper, appMode, colorDay, colorNight);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		MapColorPaletteFragment.showInstance(manager);
	}
}

