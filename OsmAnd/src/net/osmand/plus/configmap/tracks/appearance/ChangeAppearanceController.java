package net.osmand.plus.configmap.tracks.appearance;

import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.shared.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData.AppearanceChangedListener;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ArrowsCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.SplitCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.StartFinishCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.WidthCardController;
import net.osmand.plus.myplaces.tracks.tasks.ChangeTracksAppearanceTask;
import net.osmand.util.Algorithms;

import java.util.Set;

public class ChangeAppearanceController implements IDialogController, IColorCardControllerListener,
		AppearanceChangedListener {

	public static final String PROCESS_ID = "change_tracks_appearance";

	private final OsmandApplication app;

	private final ArrowsCardController arrowsCardController;
	private final StartFinishCardController showStartAndFinishIconsCardController;
	private final ColorCardController colorCardController;
	private final WidthCardController widthCardController;
	private final SplitCardController splitCardController;

	private final AppearanceData data;
	private final AppearanceData initialData;
	private final Set<TrackItem> items;
	private boolean isAppearanceSaved = false;

	private ChangeAppearanceController(@NonNull OsmandApplication app, @NonNull Set<TrackItem> items) {
		this.app = app;
		this.items = items;
		this.initialData = buildAppearanceData();
		this.data = new AppearanceData(initialData).setListener(this);

		arrowsCardController = new ArrowsCardController(app, data, true);
		showStartAndFinishIconsCardController = new StartFinishCardController(app, data, true);

		colorCardController = new ColorCardController(app, data, true);
		colorCardController.setListener(this);

		widthCardController = new WidthCardController(app, data, true);
		widthCardController.setControlsColorProvider(colorCardController);

		splitCardController = new SplitCardController(app, data, true);
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		data.setParameter(COLORING_TYPE, coloringStyle != null ? coloringStyle.getId() : null);
		data.setParameter(COLOR_PALETTE, PaletteGradientColor.DEFAULT_NAME);
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		if (paletteColor instanceof PaletteGradientColor) {
			data.setParameter(COLOR_PALETTE, ((PaletteGradientColor) paletteColor).getPaletteName());
		} else {
			data.setParameter(COLOR_PALETTE, PaletteGradientColor.DEFAULT_NAME);
			data.setParameter(COLOR, paletteColor.getColor());
		}
	}

	public boolean hasAnyChangesToCommit() {
		return !Algorithms.objectEquals(initialData, data);
	}

	public void saveChanges(@NonNull FragmentActivity activity) {
		colorCardController.getColorsPaletteController().refreshLastUsedTime();

		ChangeTracksAppearanceTask task = new ChangeTracksAppearanceTask(activity, data, items, result -> {
			isAppearanceSaved = true;
			onAppearanceSaved();
			return true;
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void onAppearanceSaved() {
		app.getDialogManager().askDismissDialog(PROCESS_ID);
	}

	public int getEditedItemsCount() {
		return items.size();
	}

	public boolean isAppearanceSaved() {
		return isAppearanceSaved;
	}

	@NonNull
	public ArrowsCardController getArrowsCardController() {
		return arrowsCardController;
	}

	@NonNull
	public StartFinishCardController getStartAndFinishIconsCardController() {
		return showStartAndFinishIconsCardController;
	}

	@NonNull
	public ColorCardController getColorCardController() {
		return colorCardController;
	}

	@NonNull
	public WidthCardController getWidthCardController() {
		return widthCardController;
	}

	@NonNull
	public SplitCardController getSplitCardController() {
		return splitCardController;
	}

	@Override
	public void onAppearanceChanged() {
		app.getDialogManager().askRefreshDialogCompletely(PROCESS_ID);
	}

	@NonNull
	private AppearanceData buildAppearanceData() {
		AppearanceData data = new AppearanceData();
		for (GpxParameter parameter : GpxParameter.Companion.getAppearanceParameters()) {
			data.setParameter(parameter, null);
		}
		return data;
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull Fragment fragment, @NonNull Set<TrackItem> items) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new ChangeAppearanceController(app, items));
		ChangeAppearanceFragment.showInstance(activity.getSupportFragmentManager(), fragment);
	}
}
