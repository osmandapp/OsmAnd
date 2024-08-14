package net.osmand.plus.configmap.tracks.appearance;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData.AppearanceChangedListener;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ArrowsCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.SplitCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.StartFinishCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.WidthCardController;
import net.osmand.plus.myplaces.tracks.tasks.ChangeTracksAppearanceTask;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDirItem;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class DefaultAppearanceController implements IDialogController, IColorCardControllerListener,
		AppearanceChangedListener {

	public static final String PROCESS_ID = "edit_tracks_folder_default_appearance";

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;

	private final ArrowsCardController arrowsCardController;
	private final StartFinishCardController iconsCardController;
	private final ColorCardController colorCardController;
	private final WidthCardController widthCardController;
	private final SplitCardController splitCardController;

	private final GpxDirItem dirItem;
	private final TrackFolder folder;
	private final AppearanceData data;
	private final AppearanceData initialData;
	private boolean isAppearanceSaved = false;

	public DefaultAppearanceController(@NonNull OsmandApplication app, @NonNull TrackFolder folder) {
		this.app = app;
		this.folder = folder;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.dirItem = gpxDbHelper.getGpxDirItem(folder.getDirFile());
		this.initialData = buildAppearanceData(dirItem);
		this.data = new AppearanceData(initialData).setListener(this);

		arrowsCardController = new ArrowsCardController(app, data, false);
		iconsCardController = new StartFinishCardController(app, data, false);

		colorCardController = new ColorCardController(app, data, false);
		colorCardController.setListener(this);

		widthCardController = new WidthCardController(app, data, false);
		widthCardController.setControlsColorProvider(colorCardController);

		splitCardController = new SplitCardController(app, data, false);
	}

	@NonNull
	public TrackFolder getFolder() {
		return folder;
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		data.setParameter(COLORING_TYPE, coloringStyle != null ? coloringStyle.getId() : null);
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		data.setParameter(COLOR, paletteColor.getColor());
	}

	public boolean hasAnyChangesToSave() {
		return !Algorithms.objectEquals(initialData, data);
	}

	public void saveChanges(@NonNull FragmentActivity activity, boolean updateExisting) {
		colorCardController.getColorsPaletteController().refreshLastUsedTime();

		for (GpxParameter parameter : GpxParameter.getAppearanceParameters()) {
			dirItem.setParameter(parameter, data.getParameter(parameter));
		}
		gpxDbHelper.updateDataItem(dirItem);

		if (updateExisting) {
			Set<TrackItem> items = new HashSet<>(folder.getTrackItems());
			ChangeTracksAppearanceTask task = new ChangeTracksAppearanceTask(activity, data, items, result -> {
				onAppearanceSaved();
				return true;
			});
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			onAppearanceSaved();
		}
	}

	private void onAppearanceSaved() {
		isAppearanceSaved = true;
		app.getDialogManager().askDismissDialog(PROCESS_ID);
	}

	public boolean isAppearanceSaved() {
		return isAppearanceSaved;
	}

	@NonNull
	public ArrowsCardController getArrowsCardController() {
		return arrowsCardController;
	}

	@NonNull
	public StartFinishCardController getIconsCardController() {
		return iconsCardController;
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
	private AppearanceData buildAppearanceData(@NonNull GpxDirItem item) {
		AppearanceData data = new AppearanceData();
		for (GpxParameter parameter : GpxParameter.getAppearanceParameters()) {
			data.setParameter(parameter, item.getParameter(parameter));
		}
		return data;
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull TrackFolder folder) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new DefaultAppearanceController(app, folder));
		DefaultAppearanceFragment.showInstance(activity.getSupportFragmentManager());
	}
}
