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
import net.osmand.plus.card.color.coloringstyle.ColoringStyle;
import net.osmand.plus.card.color.coloringstyle.ColoringStyleCardController;
import net.osmand.plus.card.color.coloringstyle.OnSelectColoringStyleListener;
import net.osmand.plus.card.color.coloringtype.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.coloringtype.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.coloringtype.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.ColorsPaletteController;
import net.osmand.plus.card.color.palette.IColorsPaletteController;
import net.osmand.plus.card.color.palette.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.data.ColorsCollection;
import net.osmand.plus.card.color.palette.data.PredefinedPaletteColor;
import net.osmand.plus.card.color.palette.data.PaletteColor;
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
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class TrackColorController extends ColoringStyleCardController implements IDialogController {

	private static final String PROCESS_ID = "select_track_color";

	private final SelectedGpxFile selectedGpx;
	private final TrackDrawInfo drawInfo;

	private IColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;
	private ITrackColorControllerListener listener;

	public TrackColorController(@NonNull OsmandApplication app,
	                            @Nullable SelectedGpxFile selectedGpx,
	                            @NonNull TrackDrawInfo drawInfo) {
		super(app, new ColoringStyle(drawInfo.getColoringType(), drawInfo.getRouteInfoAttribute()));
		this.selectedGpx = selectedGpx;
		this.drawInfo = drawInfo;
	}

	public void setListener(ITrackColorControllerListener listener) {
		this.listener = listener;
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
			List<PaletteColor> predefinedColors = new ArrayList<>();
			for (AppearanceListItem item : GpxAppearanceAdapter.getUniqueTrackColorItems(app)) {
				String id = item.getValue();
				int colorInt = item.getColor();
				String name = item.getLocalizedValue();
				predefinedColors.add(new PredefinedPaletteColor(id, colorInt, name));
			}
			OsmandSettings settings = app.getSettings();
			ColorsCollection colorsCollection = new ColorsCollection(predefinedColors, settings.TRACK_COLORS_PALETTE);
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, drawInfo.getColor()) {
				@Override
				protected void onColorSelected(@Nullable PaletteColor paletteColor) {
					if (listener != null && paletteColor != null) {
						listener.onColorSelectedFromPalette(paletteColor);
					}
				}
			};
		}
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			GPXTrackAnalysis analysis = selectedGpx.getTrackAnalysis(app);
			ColoringStyle selectedColoringStyle = drawInfo.getColoringStyle();
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedColoringStyle, analysis);
		}
		return coloringStyleDetailsController;
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getMultiSateMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (ColoringStyle coloringStyle : getSupportedColoringStyles()) {
			int titleColor = isAvailableColoringStyle(coloringStyle)
					? ColorUtilities.getPrimaryTextColor(app, nightMode)
					: ColorUtilities.getDisabledTextColor(app, nightMode);
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(coloringStyle.toHumanString(app))
					.setTitleColor(titleColor)
					.setTag(coloringStyle)
					.create()
			);
		}
		return menuItems;
	}

	@Override
	protected void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
		styleDetailsController.setColoringStyle(coloringStyle);
		if (listener != null) {
			listener.onColoringStyleSelected(coloringStyle);
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	protected List<ColoringStyle> collectSupportedColoringStyles() {
		List<ColoringStyle> result = new ArrayList<>();
		for (ColoringType coloringType : getColoringTypes()) {
			if (!coloringType.isRouteInfoAttribute()) {
				result.add(new ColoringStyle(coloringType));
			}
		}
		for (String routeInfoAttribute : collectRouteInfoAttributes()) {
			result.add(new ColoringStyle(routeInfoAttribute));
		}
		return result;
	}

	@Override
	protected boolean isAvailableColoringStyle(@NonNull ColoringStyle coloringStyle) {
		if (selectedGpx == null || coloringStyle.getType() != ATTRIBUTE && drawInfo.isCurrentRecording()) {
			return true;
		}
		return isAvailableForDrawingTrack(app, coloringStyle, selectedGpx);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
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

	public static TrackColorController getOrCreateInstance(
			@NonNull OsmandApplication app, @Nullable SelectedGpxFile selectedGpx,
			@NonNull TrackDrawInfo drawInfo, @NonNull ITrackColorControllerListener listener
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

	@NonNull
	public static ColoringType[] getColoringTypes() {
		return new ColoringType[] {
				ColoringType.TRACK_SOLID, ColoringType.SPEED,
				ColoringType.ALTITUDE, ColoringType.SLOPE, ColoringType.ATTRIBUTE
		};
	}

	public interface ITrackColorControllerListener
			extends OnSelectColoringStyleListener, OnColorsPaletteListener { }
}
