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
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.DirectionArrowsController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ShowStartFinishController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ColorController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.WidthController;
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

	private final DirectionArrowsController directionArrowsCardController;
	private final ShowStartFinishController showStartAndFinishIconsCardController;
	private final ColorController colorCardController;
	private final WidthController widthCardController;

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

		directionArrowsCardController = new DirectionArrowsController(app, appearanceData);
		showStartAndFinishIconsCardController = new ShowStartFinishController(app, appearanceData);

		colorCardController = new ColorController(app, appearanceData, new ColoringStyle(ColoringType.UNCHANGED));
		colorCardController.setListener(this);

		widthCardController = new WidthController(app, appearanceData);
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
	public DirectionArrowsController getDirectionArrowsCardController() {
		return directionArrowsCardController;
	}

	@NonNull
	public ShowStartFinishController getShowStartAndFinishIconsCardController() {
		return showStartAndFinishIconsCardController;
	}

	@NonNull
	public ColorController getColorCardController() {
		return colorCardController;
	}

	@NonNull
	public WidthController getWidthCardController() {
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
