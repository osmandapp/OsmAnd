package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData.OnAppearanceModifiedListener;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.DirectionArrowsCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.StartFinishCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.WidthCardController;
import net.osmand.plus.configmap.tracks.appearance.tasks.ChangeAppearanceTask;
import net.osmand.plus.track.fragments.controller.TrackWidthController.ITrackWidthSelectedListener;

import java.util.Objects;
import java.util.Set;

public class ChangeAppearanceController implements IChangeAppearanceController,
		IColorCardControllerListener, ITrackWidthSelectedListener, OnAppearanceModifiedListener {

	public static final String PROCESS_ID = "change_tracks_appearance";

	private final OsmandApplication app;

	private final DirectionArrowsCardController directionArrowsCardController;
	private final StartFinishCardController showStartAndFinishIconsCardController;
	private final ColorCardController colorCardController;
	private final WidthCardController widthCardController;

	private final AppearanceData initialAppearanceData;
	private final AppearanceData appearanceData;
	private final Set<TrackItem> selectedTrackItems;
	private boolean isAppearanceSaved = false;

	private ChangeAppearanceController(@NonNull OsmandApplication app,
	                                   @NonNull Set<TrackItem> trackItems) {
		this.app = app;
		this.selectedTrackItems = trackItems;
		this.initialAppearanceData = new AppearanceData();
		this.appearanceData = new AppearanceData(initialAppearanceData).setModifiedListener(this);

		directionArrowsCardController = new DirectionArrowsCardController(app, appearanceData);
		showStartAndFinishIconsCardController = new StartFinishCardController(app, appearanceData);

		colorCardController = new ColorCardController(app, appearanceData);
		colorCardController.setListener(this);

		widthCardController = new WidthCardController(app, appearanceData);
		widthCardController.setListener(this);
		widthCardController.setControlsColorProvider(colorCardController);
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		appearanceData.setColoringStyle(coloringStyle);
		updateColorItems();
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		appearanceData.setCustomColor(paletteColor.getColor());
		updateColorItems();
	}

	private void updateColorItems() {
		widthCardController.updateColorItems();
	}

	@Override
	public boolean hasAnyChangesToCommit() {
		return !Objects.equals(initialAppearanceData, appearanceData);
	}

	@Override
	public void saveChanges(@NonNull FragmentActivity activity) {
		colorCardController.getColorsPaletteController().refreshLastUsedTime();
		ChangeAppearanceTask.execute(activity, appearanceData, selectedTrackItems, result -> {
			isAppearanceSaved = true;
			onAppearanceSaved();
			return true;
		});
	}

	private void onAppearanceSaved() {
		app.getDialogManager().askDismissDialog(PROCESS_ID);
	}

	@Override
	public int getEditedItemsCount() {
		return selectedTrackItems.size();
	}

	@Override
	public boolean isAppearanceSaved() {
		return isAppearanceSaved;
	}

	@NonNull
	public DirectionArrowsCardController getDirectionArrowsCardController() {
		return directionArrowsCardController;
	}

	@NonNull
	public StartFinishCardController getShowStartAndFinishIconsCardController() {
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

	@Override
	public void onAppearanceModified() {
		app.getDialogManager().askRefreshDialogCompletely(PROCESS_ID);
	}

	@Override
	public void onTrackWidthSelected(@Nullable String width) {

	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull Fragment targetFragment,
	                              @NonNull Set<TrackItem> selectedItems) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new ChangeAppearanceController(app, selectedItems));
		ChangeAppearanceFragment.showInstance(activity.getSupportFragmentManager(), targetFragment);
	}
}
