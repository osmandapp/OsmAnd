package net.osmand.plus.settings.controllers;

import static net.osmand.router.RouteStatisticsHelper.ROUTE_INFO_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringCardController;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteCard;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController.OnPaletteModeSelectedListener;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DayNightHelper.MapThemeProvider;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RouteLineColorController extends ColoringCardController implements MapThemeProvider, IDialogController {

	public static final String PROCESS_ID = "select_route_line_color";

	private static final int PALETTE_MODE_ID_DAY = 0;
	private static final int PALETTE_MODE_ID_NIGHT = 1;

	private final PreviewRouteLineInfo routeLinePreview;

	private ModedColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;
	private boolean initialNightMode;

	private RouteLineColorController(@NonNull OsmandApplication app,
	                                 @NonNull PreviewRouteLineInfo routeLinePreview) {
		super(app, routeLinePreview.getRouteColoringStyle());
		this.routeLinePreview = routeLinePreview;
	}

	@NonNull
	public ModedColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			OsmandSettings settings = app.getSettings();
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.predefinedColors = getPredefinedColors();
			bundle.palettePreference = settings.ROUTE_LINE_COLORS_PALETTE;
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			colorsPaletteController = new ModedColorsPaletteController(app, colorsCollection) {

				private PaletteMode paletteModeDay;
				private PaletteMode paletteModeNight;

				@Override
				@NonNull
				protected List<PaletteMode> collectAvailablePaletteModes() {
					paletteModeDay = createPaletteMode(false);
					paletteModeNight = createPaletteMode(true);
					return Arrays.asList(paletteModeDay, paletteModeNight);
				}

				@NonNull
				@Override
				protected PaletteMode getInitialPaletteMode() {
					return initialNightMode ? paletteModeNight : paletteModeDay;
				}

				@Override
				protected PaletteColor provideSelectedColorForPaletteMode(@NonNull PaletteMode paletteMode) {
					boolean useNightMap = Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
					return colorsCollection.findPaletteColor(routeLinePreview.getCustomColor(useNightMap));
				}

				@NonNull
				private PaletteMode createPaletteMode(boolean night) {
					String title = app.getString(night ? R.string.daynight_mode_night : R.string.daynight_mode_day);
					int tag = night ? PALETTE_MODE_ID_NIGHT : PALETTE_MODE_ID_DAY;
					return new PaletteMode(title, tag);
				}
			};
		}
		colorsPaletteController.setPaletteListener(getControllerListener());
		colorsPaletteController.setPaletteModeSelectedListener((OnPaletteModeSelectedListener) getControllerListener());
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			ColoringStyle selectedColoringStyle = routeLinePreview.getRouteColoringStyle();
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedColoringStyle, null) {
				@Nullable
				@Override
				public String getTypeDescription() {
					ColoringType coloringType = coloringStyle.getType();
					String routeInfoAttribute = coloringStyle.getRouteInfoAttribute();
					if (coloringType.isDefault()) {
						String pattern = app.getString(R.string.route_line_use_map_style_color);
						return String.format(pattern, app.getRendererRegistry().getSelectedRendererName());
					} else if (routeInfoAttribute != null) {
						String key = routeInfoAttribute.replaceAll(ROUTE_INFO_PREFIX, "");
						return AndroidUtils.getStringRouteInfoPropertyDescription(app, key);
					} else if (coloringType.isGradient()) {
						return app.getString(R.string.route_line_use_gradient_coloring);
					}
					return null;
				}

				@Override
				public boolean shouldShowBottomSpace() {
					return true;
				}
			};
		}
		return coloringStyleDetailsController;
	}

	@NonNull
	@Override
	protected BaseCard getContentCardForSelectedState(@NonNull FragmentActivity activity) {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();
		if (!isAvailableInSubscription(coloringStyle)) {
			return new PromoBannerCard(activity, isUsedOnMap());
		} else if (coloringType.isCustomColor()) {
			return new ModedColorsPaletteCard(activity, getColorsPaletteController(), isUsedOnMap());
		} else {
			return new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController(), isUsedOnMap());
		}
	}

	@Override
	protected void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
		styleDetailsController.setColoringStyle(coloringStyle);
	}

	@Override
	public boolean shouldShowMultiStateCardHeader() {
		return false;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public boolean isSelectedColoringStyleAvailable() {
		return isAvailableInSubscription(getSelectedColoringStyle());
	}

	@Override
	protected boolean isDataAvailableForColoringStyle(@NonNull ColoringStyle coloringStyle) {
		// We can use any of available map data types to draw route line
		return true;
	}

	@Override
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(ColoringPurpose.ROUTE_LINE);
	}

	@Override
	public DayNightMode getMapTheme() {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		if (coloringStyle.getType().isCustomColor()) {
			return isNightMap() ? DayNightMode.NIGHT : DayNightMode.DAY;
		}
		return null;
	}

	public boolean isNightMap() {
		ModedColorsPaletteController paletteController = getColorsPaletteController();
		PaletteMode paletteMode = paletteController.getSelectedPaletteMode();
		return Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	public void onResume() {
		initialNightMode = app.getDaynightHelper().isNightModeForMapControls();
		setMapThemeProvider(this);
	}

	public void onPause() {
		setMapThemeProvider(null);
	}

	private void setMapThemeProvider(@Nullable MapThemeProvider provider) {
		DayNightHelper helper = app.getDaynightHelper();
		helper.setMapThemeProvider(provider);
	}

	@NonNull
	public static List<PaletteColor> getPredefinedColors() {
		return Arrays.asList(DefaultColors.values());
	}

	@NonNull
	public static RouteLineColorController getInstance(@NonNull OsmandApplication app,
	                                                   @NonNull PreviewRouteLineInfo routeLinePreview,
	                                                   @NonNull IColorCardControllerListener listener) {
		DialogManager dialogManager = app.getDialogManager();
		RouteLineColorController controller = (RouteLineColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new RouteLineColorController(app, routeLinePreview);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setListener(listener);
		return controller;
	}

	public interface IRouteLineColorControllerListener
			extends IColorCardControllerListener, OnPaletteModeSelectedListener { }
}
