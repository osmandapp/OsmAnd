package net.osmand.plus.track.fragments.controller;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableForDrawingTrack;
import static net.osmand.plus.routing.ColoringType.ATTRIBUTE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringCardController;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PredefinedPaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.util.ArrayList;
import java.util.List;

public class TrackColorController extends ColoringCardController implements IDialogController {

	private static final String PROCESS_ID = "select_track_color";

	private final SelectedGpxFile selectedGpx;
	private final TrackDrawInfo drawInfo;

	private IColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;

	public TrackColorController(@NonNull OsmandApplication app,
	                            @Nullable SelectedGpxFile selectedGpx,
	                            @NonNull TrackDrawInfo drawInfo) {
		super(app, drawInfo.getColoringStyle());
		this.selectedGpx = selectedGpx;
		this.drawInfo = drawInfo;
	}

	@NonNull
	@Override
	protected BaseCard getContentCardForSelectedState(@NonNull FragmentActivity activity) {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();
		if (!isAvailableInSubscription(coloringStyle)) {
			return new PromoBannerCard(activity, isUsedOnMap());
		} else if (coloringType.isTrackSolid()) {
			return new ColorsPaletteCard(activity, getColorsPaletteController(), isUsedOnMap());
		} else {
			return new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController(), isUsedOnMap());
		}
	}

	@NonNull
	public IColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			OsmandSettings settings = app.getSettings();
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.predefinedColors = getPredefinedColors(app);
			bundle.palettePreference = settings.TRACK_COLORS_PALETTE;
			bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, drawInfo.getColor());
		}
		colorsPaletteController.setPaletteListener(getControllerListener());
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			GPXTrackAnalysis analysis = selectedGpx != null ? selectedGpx.getTrackAnalysis(app) : null;
			ColoringStyle selectedColoringStyle = drawInfo.getColoringStyle();
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedColoringStyle, analysis);
		}
		return coloringStyleDetailsController;
	}

	@Override
	protected void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
		styleDetailsController.setColoringStyle(coloringStyle);
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	protected boolean isDataAvailableForColoringStyle(@NonNull ColoringStyle coloringStyle) {
		if (selectedGpx == null || coloringStyle.getType() != ATTRIBUTE && drawInfo.isCurrentRecording()) {
			return true;
		}
		return isAvailableForDrawingTrack(app, coloringStyle, selectedGpx);
	}

	@Override
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(ColoringPurpose.TRACK);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	public static List<PaletteColor> getPredefinedColors(@NonNull OsmandApplication app) {
		List<PaletteColor> predefinedColors = new ArrayList<>();
		for (AppearanceListItem item : GpxAppearanceAdapter.getUniqueTrackColorItems(app)) {
			String id = item.getValue();
			int colorInt = item.getColor();
			String name = item.getLocalizedValue();
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, name));
		}
		return predefinedColors;
	}

	public static void saveCustomColorsToTracks(@NonNull OsmandApplication app, int prevColor, int newColor) {
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		List<GpxDataItem> gpxDataItems = gpxDbHelper.getItems();
		for (GpxDataItem dataItem : gpxDataItems) {
			int color = dataItem.getParameter(COLOR);
			if (prevColor == color) {
				dataItem.setParameter(COLOR, newColor);
				gpxDbHelper.updateDataItem(dataItem);
			}
		}
		List<SelectedGpxFile> files = app.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selectedGpxFile : files) {
			if (prevColor == selectedGpxFile.getGpxFile().getColor(0)) {
				selectedGpxFile.getGpxFile().setColor(newColor);
			}
		}
	}

	public static TrackColorController getInstance(
			@NonNull OsmandApplication app, @Nullable SelectedGpxFile selectedGpx,
			@NonNull TrackDrawInfo drawInfo, @NonNull IColorCardControllerListener listener
	) {
		DialogManager dialogManager = app.getDialogManager();
		TrackColorController controller = (TrackColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new TrackColorController(app, selectedGpx, drawInfo);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setListener(listener);
		return controller;
	}

}
