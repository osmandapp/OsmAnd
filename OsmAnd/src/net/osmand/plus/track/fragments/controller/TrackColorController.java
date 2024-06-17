package net.osmand.plus.track.fragments.controller;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableForDrawingTrack;
import static net.osmand.plus.routing.ColoringType.ATTRIBUTE;
import static net.osmand.plus.routing.ColoringType.TRACK_SOLID;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.IControlsColorProvider;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PredefinedPaletteColor;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class TrackColorController extends ColoringStyleCardController implements IDialogController, IControlsColorProvider {

	private static final String PROCESS_ID = "select_track_color";

	private final SelectedGpxFile selectedGpx;
	private final TrackDrawInfo drawInfo;

	private IColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;

	public TrackColorController(@NonNull OsmandApplication app,
	                            @Nullable SelectedGpxFile selectedGpx,
	                            @NonNull TrackDrawInfo drawInfo) {
		super(app);
		this.selectedGpx = selectedGpx;
		this.drawInfo = drawInfo;
		this.selectedState = findCardState(drawInfo.getColoringStyle());
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		ColoringStyle coloringStyle = requireSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();

		if (!isAvailableInSubscription(coloringStyle)) {
			container.addView(new PromoBannerCard(activity).build());
		} else if (coloringType.isTrackSolid()) {
			container.addView(new ColorsPaletteCard(activity, getColorsPaletteController()).build());
		} else {
			container.addView(new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController()).build());
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
		colorsPaletteController.setPaletteListener(getExternalListener());
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
	public void askSelectColoringStyle(@NonNull ColoringStyle coloringStyle) {
		if (isDataAvailable(coloringStyle)) {
			super.askSelectColoringStyle(coloringStyle);
		} else {
			showUnavailableColoringStyleSnackBar(card.getActivity(), coloringStyle, card.getSelectorView());
		}
	}

	@Override
	protected void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		if (coloringStyle != null) {
			IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
			styleDetailsController.setColoringStyle(coloringStyle);
		}
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	@Override
	public int getSelectedControlsColor() {
		ColoringStyle style = requireSelectedColoringStyle();
		Integer color = style.getType() == TRACK_SOLID ? drawInfo.getColor() : null;
		return color != null ? color : GpxAppearanceAdapter.getTrackColor(app);
	}

	@Override
	protected boolean isCardStateAvailable(@NonNull CardState cardState) {
		return isDataAvailable((ColoringStyle) cardState.getTag());
	}

	protected boolean isDataAvailable(@Nullable ColoringStyle coloringStyle) {
		if (coloringStyle == null) {
			return false;
		}
		if (selectedGpx == null || coloringStyle.getType() != ATTRIBUTE && drawInfo.isCurrentRecording()) {
			return true;
		}
		return isAvailableForDrawingTrack(app, coloringStyle, selectedGpx);
	}

	private void showUnavailableColoringStyleSnackBar(@NonNull FragmentActivity activity,
	                                                  @NonNull ColoringStyle coloringStyle,
	                                                  @NonNull View view) {
		ColoringType coloringType = coloringStyle.getType();
		String text = "";
		if (coloringType == ColoringType.SPEED) {
			text = app.getString(R.string.track_has_no_speed);
		} else if (CollectionUtils.equalsToAny(coloringType, ColoringType.ALTITUDE, ColoringType.SLOPE)) {
			text = app.getString(R.string.track_has_no_altitude);
		} else if (coloringType.isRouteInfoAttribute()) {
			text = app.getString(R.string.track_has_no_needed_data);
		}
		text += " " + app.getString(R.string.select_another_colorization);
		Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
				.setAnchorView(activity.findViewById(R.id.dismiss_button));
		UiUtilities.setupSnackbar(snackbar, card.isNightMode());
		snackbar.show();
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(ColoringPurpose.TRACK);
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
			Integer color = dataItem.getParameter(COLOR);
			if (Algorithms.objectEquals(prevColor, color)) {
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

	@NonNull
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
