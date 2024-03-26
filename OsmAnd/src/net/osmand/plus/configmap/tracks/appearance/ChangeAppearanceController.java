package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ShowStartFinishCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.WidthCardController;
import net.osmand.plus.configmap.tracks.appearance.tasks.ChangeAppearanceTask;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.fragments.controller.TrackWidthController.OnTrackWidthSelectedListener;

import java.util.Objects;
import java.util.Set;

public class ChangeAppearanceController implements IChangeAppearanceController,
		IColorCardControllerListener, OnTrackWidthSelectedListener, OnAppearanceModifiedListener {

	private static final String PROCESS_ID = "change_tracks_appearance";

	private final OsmandApplication app;

	private final DirectionArrowsCardController directionArrowsCardController;
	private final ShowStartFinishCardController showStartAndFinishIconsCardController;
	private final ColorCardController colorCardController;
	private final WidthCardController widthCardController;

	private final AppearanceData initialAppearanceData;
	private final AppearanceData appearanceData;
	private final ItemsSelectionHelper<TrackItem> selectionHelper;
	private boolean isAppearanceSaved = false;

	private ChangeAppearanceController(@NonNull OsmandApplication app,
									   @NonNull ItemsSelectionHelper<TrackItem> selectionHelper,
	                                   @NonNull AppearanceData initialAppearanceData) {
		this.app = app;
		this.selectionHelper = selectionHelper;
		this.initialAppearanceData = initialAppearanceData;
		this.appearanceData = new AppearanceData(initialAppearanceData).setModifiedListener(this);

		directionArrowsCardController = new DirectionArrowsCardController(app, appearanceData);
		showStartAndFinishIconsCardController = new ShowStartFinishCardController(app, appearanceData);

		colorCardController = new ColorCardController(app, appearanceData, new ColoringStyle(ColoringType.UNCHANGED));
		colorCardController.setListener(this);

		widthCardController = new WidthCardController(app, appearanceData);
		widthCardController.setListener(this);
		widthCardController.setColorProvider(colorCardController);
	}

	@Override
	public void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
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
		Set<TrackItem> selectedItems = selectionHelper.getSelectedItems();
		ChangeAppearanceTask.execute(activity, appearanceData, selectedItems, result -> {
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
		return selectionHelper.getSelectedItemsSize();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
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
	public ShowStartFinishCardController getShowStartAndFinishIconsCardController() {
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

	@NonNull
	public static ChangeAppearanceController getInstance(@NonNull OsmandApplication app,
	                                                     @NonNull ItemsSelectionHelper<TrackItem> selectionHelper) {
		DialogManager dialogManager = app.getDialogManager();
		ChangeAppearanceController controller = (ChangeAppearanceController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new ChangeAppearanceController(app, selectionHelper, new AppearanceData());
			dialogManager.register(PROCESS_ID, controller);
		}
		return controller;
	}
}
