package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.DirectionArrowsCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.ShowStartAndFinishIconsCardController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.TracksAppearanceColorController;
import net.osmand.plus.configmap.tracks.appearance.subcontrollers.TracksAppearanceWidthController;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.fragments.controller.TrackWidthController.OnTrackWidthSelectedListener;

public class ChangeAppearanceController
		implements IChangeAppearanceController, IColorCardControllerListener, OnTrackWidthSelectedListener {

	private static final String PROCESS_ID = "change_tracks_appearance";

	private final OsmandApplication app;

	private final DirectionArrowsCardController directionArrowsCardController;
	private final ShowStartAndFinishIconsCardController showStartAndFinishIconsCardController;
	private final TracksAppearanceColorController colorCardController;
	private final TracksAppearanceWidthController widthCardController;
	private final AppearanceData appearanceData;

	private ChangeAppearanceController(@NonNull OsmandApplication app,
	                                   @NonNull AppearanceData appearanceData) {
		this.app = app;
		this.appearanceData = appearanceData;
		directionArrowsCardController = new DirectionArrowsCardController(app);
		showStartAndFinishIconsCardController = new ShowStartAndFinishIconsCardController(app);

		colorCardController = new TracksAppearanceColorController(app, appearanceData, new ColoringStyle(ColoringType.UNCHANGED));
		colorCardController.setListener(this);

		widthCardController = new TracksAppearanceWidthController(app, null);
		widthCardController.setListener(this);
		widthCardController.setColorProvider(colorCardController);
	}

	@Override
	public void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
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
		return true;
	}

	@Override
	public void onApplyButtonClicked() {
		app.getDialogManager().askDismissDialog(PROCESS_ID);
	}

	@Override
	public int getEditedItemsCount() {
		return 3;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public DirectionArrowsCardController getDirectionArrowsCardController() {
		return directionArrowsCardController;
	}

	@NonNull
	public ShowStartAndFinishIconsCardController getShowStartAndFinishIconsCardController() {
		return showStartAndFinishIconsCardController;
	}

	@NonNull
	public TracksAppearanceColorController getColorCardController() {
		return colorCardController;
	}

	@NonNull
	public TracksAppearanceWidthController getWidthCardController() {
		return widthCardController;
	}

	@Override
	public void onTrackWidthSelected(@Nullable String width) {

	}

	@NonNull
	public static ChangeAppearanceController getInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		ChangeAppearanceController controller = (ChangeAppearanceController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new ChangeAppearanceController(app, new AppearanceData());
			dialogManager.register(PROCESS_ID, controller);
		}
		return controller;
	}
}
