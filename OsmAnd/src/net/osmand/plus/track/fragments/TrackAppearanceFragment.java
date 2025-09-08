package net.osmand.plus.track.fragments;

import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.UPDATE_TRACK_ICON;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;
import static net.osmand.plus.track.GpxAppearanceAdapter.TRACK_WIDTH_BOLD;
import static net.osmand.plus.track.GpxAppearanceAdapter.TRACK_WIDTH_MEDIUM;
import static net.osmand.plus.track.cards.ActionsCard.RESET_BUTTON_INDEX;
import static net.osmand.plus.track.cards.Track3DCard.WALL_HEIGHT_BUTTON_INDEX;
import static net.osmand.shared.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.shared.gpx.GpxParameter.ELEVATION_METERS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_LINE_POSITION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_WALL_COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.WIDTH;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.card.base.headed.HeadedContentCard;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.configmap.MapOptionSliderFragment.MapOptionSliderListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet;
import net.osmand.plus.plugins.monitoring.TripRecordingStartingBottomSheet;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.SplitTrackAsyncTask.SplitTrackListener;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.cards.ActionsCard;
import net.osmand.plus.track.cards.DirectionArrowsCard;
import net.osmand.plus.track.cards.ShowStartFinishCard;
import net.osmand.plus.track.cards.SplitIntervalCard;
import net.osmand.plus.track.cards.Track3DCard;
import net.osmand.plus.track.fragments.controller.TrackColorController;
import net.osmand.plus.track.fragments.controller.TrackWidthController;
import net.osmand.plus.track.fragments.controller.TrackWidthController.ITrackWidthSelectedListener;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.io.KFile;
import net.osmand.shared.routing.ColoringType;

import java.util.ArrayList;
import java.util.List;

public class TrackAppearanceFragment extends ContextMenuScrollFragment implements CardListener,
		IColorCardControllerListener, ITrackWidthSelectedListener, MapOptionSliderListener {

	public static final String TAG = TrackAppearanceFragment.class.getName();

	private static final String SHOW_START_FINISH_ICONS_INITIAL_VALUE_KEY = "showStartFinishIconsInitialValueKey";

	private GpxDbHelper gpxDbHelper;

	@Nullable
	private GpxDataItem gpxDataItem;
	private TrackDrawInfo trackDrawInfo;
	private SelectedGpxFile selectedGpxFile;
	private List<GpxDisplayGroup> displayGroups;

	private int menuTitleHeight;
	private long modifiedTime = -1;

	private SplitIntervalCard splitIntervalCard;
	private boolean showStartFinishIconsInitialValue;

	private final List<BaseCard> cards = new ArrayList<>();

	private ImageView trackIcon;
	private View buttonsShadow;
	private View routeMenuTopShadowAll;
	private View controlButtons;
	private View view;
	private Track3DCard track3DCard;

	@Override
	public int getMainLayoutId() {
		return R.layout.track_appearance;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	@Override
	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	public TrackDrawInfo getTrackDrawInfo() {
		return trackDrawInfo;
	}

	public SelectedGpxFile getSelectedGpxFile() {
		return selectedGpxFile;
	}

	public void setSelectedGpxFile(SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxDbHelper = app.getGpxDbHelper();

		if (savedInstanceState != null) {
			trackDrawInfo = new TrackDrawInfo(savedInstanceState);
			if (selectedGpxFile == null) {
				restoreSelectedGpxFile(trackDrawInfo.getFilePath(), trackDrawInfo.isCurrentRecording());
			}
			if (!trackDrawInfo.isCurrentRecording()) {
				gpxDataItem = gpxDbHelper.getItem(new KFile(trackDrawInfo.getFilePath()));
			}
			showStartFinishIconsInitialValue = savedInstanceState.getBoolean(SHOW_START_FINISH_ICONS_INITIAL_VALUE_KEY,
					settings.CURRENT_TRACK_SHOW_START_FINISH.get());
		} else {
			showStartFinishIconsInitialValue = settings.CURRENT_TRACK_SHOW_START_FINISH.get();

			if (selectedGpxFile.isShowCurrentTrack()) {
				trackDrawInfo = new TrackDrawInfo(app, TrackDrawInfo.CURRENT_RECORDING);
			} else {
				GpxDataItemCallback callback = new GpxDataItemCallback() {
					@Override
					public boolean isCancelled() {
						return !isAdded();
					}

					@Override
					public void onGpxDataItemReady(@NonNull GpxDataItem item) {
						gpxDataItem = item;
						trackDrawInfo.updateParams(app, item);
						if (view != null) {
							initContent();
						}
					}
				};
				String filePath = selectedGpxFile.getGpxFile().getPath();
				gpxDataItem = gpxDbHelper.getItem(new KFile(filePath), callback);
				trackDrawInfo = new TrackDrawInfo(app, filePath, gpxDataItem);
			}
		}
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	private void restoreSelectedGpxFile(String gpxFilePath, boolean isCurrentRecording) {
		TrackMenuFragment.loadSelectedGpxFile(requireMapActivity(), gpxFilePath, isCurrentRecording, (gpxFile) -> {
			setSelectedGpxFile(gpxFile);
			if (view != null) {
				initContent();
			}
			return true;
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			trackIcon = view.findViewById(R.id.track_icon);
			buttonsShadow = view.findViewById(R.id.buttons_shadow);
			controlButtons = view.findViewById(R.id.control_buttons);
			routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);

			if (isPortrait()) {
				updateCardsLayout();
			} else {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				controlButtons.setLayoutParams(params);
			}

			if (selectedGpxFile != null) {
				initContent();
			}
		}
		return view;
	}

	private void initContent() {
		setupCards();
		setupButtons();
		setupScrollShadow();
		enterTrackAppearanceMode();
		runLayoutListener();
		updateColorItems();
	}

	private void updateContent() {
		for (BaseCard card : cards) {
			card.update();
		}
		updateAppearanceIcon();
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = routeMenuTopShadowAll.getHeight()
				+ controlButtons.getHeight() - buttonsShadow.getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateStatusBarColor();
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateStatusBarColor();
	}

	@Override
	protected void setupControlButtons(@NonNull View view) {
		if (isPortrait()) {
			super.setupControlButtons(view);
		} else {
			View mapHudControls = view.findViewById(R.id.map_hud_controls);
			AndroidUiHelper.updateVisibility(mapHudControls, false);
			setupMapRulerWidget(view, requireMapActivity().getMapLayers());
		}
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY
				|| menuState == MenuState.HALF_SCREEN
				|| !isPortrait();
	}

	@Override
	public void updateMapControlsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		if (isPortrait()) {
			super.updateMapControlsPos(fragment, y, animated);
		} else {
			View mainView = getMainView();
			View mapBottomHudButtons = getMapBottomHudButtons();
			if (mainView != null && mapBottomHudButtons != null) {
				int bottomPadding = getResources().getDimensionPixelSize(R.dimen.map_button_margin);
				AndroidUtils.setPadding(mapBottomHudButtons, mainView.getWidth(), 0, 0, bottomPadding);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getOsmandMap().getMapLayers().getGpxLayer().setTrackDrawInfo(trackDrawInfo);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getOsmandMap().getMapLayers().getGpxLayer().setTrackDrawInfo(null);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		Fragment fragment = getTargetFragment();
		if (!(fragment instanceof TrackMenuFragment)) {
			adjustMapPosition(getHeight());
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitTrackAppearanceMode();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		getColorCardController().onDestroy(activity);
		getWidthCardController().onDestroy(activity);
	}

	private void enterTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		trackDrawInfo.saveToBundle(outState);
		outState.putBoolean(SHOW_START_FINISH_ICONS_INITIAL_VALUE_KEY, showStartFinishIconsInitialValue);
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (!nightMode) {
					AndroidUiHelper.setStatusBarContentColor(view, true);
				}
				return ColorUtilities.getDividerColorId(nightMode);
			} else if (!nightMode) {
				AndroidUiHelper.setStatusBarContentColor(view, false);
			}
		}
		return -1;
	}

	private void updateStatusBarColor() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.updateStatusBarColor();
		}
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			if (card instanceof SplitIntervalCard) {
				FragmentManager manager = activity.getSupportFragmentManager();
				SplitIntervalBottomSheet.showInstance(manager, this);
			} else if (card instanceof DirectionArrowsCard) {
				refreshMap();
				updateAppearanceIcon();
			} else {
				refreshMap();
			}
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		if (card instanceof ActionsCard) {
			if (buttonIndex == RESET_BUTTON_INDEX) {
				trackDrawInfo.resetParams(app, selectedGpxFile.getGpxFile());

				TrackColorController colorController = getColorCardController();
				colorController.askSelectColoringStyle(trackDrawInfo.getColoringStyle());

				colorController.getColorsPaletteController().selectColor(trackDrawInfo.getColor());

				WidthComponentController widthController = getWidthCardController().getWidthComponentController();
				widthController.askSelectWidthMode(trackDrawInfo.getWidth());

				applySplit(GpxSplitType.NO_SPLIT, 0, 0);
				updateContent();
				refreshMap();
			}
		} else if (card instanceof Track3DCard) {
			if (buttonIndex == WALL_HEIGHT_BUTTON_INDEX) {
				FragmentActivity activity = requireActivity();
				TrackWallHeightFragment.showInstance(activity.getSupportFragmentManager(), this, trackDrawInfo);
			}
		}
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		if (coloringStyle != null) {
			trackDrawInfo.setColoringStyle(coloringStyle);
			View saveButton = view.findViewById(R.id.right_bottom_button);
			saveButton.setEnabled(isAvailableInSubscription(app, coloringStyle));
			updateColorItems();
			updateGradientPalette(coloringStyle);
		}
	}

	private void updateGradientPalette(@NonNull ColoringStyle coloringStyle) {
		if (coloringStyle.getType().isGradient() && gpxDataItem != null) {
			ColoringType coloringType = ColoringType.Companion.requireValueOf(ColoringPurpose.TRACK, gpxDataItem.getParameter(COLORING_TYPE));
			trackDrawInfo.setGradientColorName(coloringStyle.getType() == coloringType ? gpxDataItem.getParameter(COLOR_PALETTE) : PaletteGradientColor.DEFAULT_NAME);
		} else {
			trackDrawInfo.setGradientColorName(PaletteGradientColor.DEFAULT_NAME);
		}
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		if (paletteColor instanceof PaletteGradientColor) {
			PaletteGradientColor paletteGradientColor = (PaletteGradientColor) paletteColor;
			trackDrawInfo.setGradientColorName(paletteGradientColor.getPaletteName());
			refreshMap();
		} else {
			trackDrawInfo.setColor(paletteColor.getColor());
			trackDrawInfo.setGradientColorName(PaletteGradientColor.DEFAULT_NAME);
			updateColorItems();
		}
	}

	@Override
	public void onColorAddedToPalette(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		if (oldColor != null) {
			TrackColorController.saveCustomColorsToTracks(app, oldColor.getColor(), newColor.getColor());
		}
		updateColorItems();
	}

	@Override
	public void onTrackWidthSelected(@Nullable String width) {
		updateAppearanceIcon();
	}

	@Override
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	@Override
	public void onContextMenuDismiss(@NonNull ContextMenuFragment fragment) {
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingStartingBottomSheet) {
			((TripRecordingStartingBottomSheet) target).show();
		} else if (target instanceof TripRecordingBottomSheet) {
			((TripRecordingBottomSheet) target).show(UPDATE_TRACK_ICON);
		}
	}

	private void updateAppearanceIcon() {
		int color = getColorCardController().getSelectedControlsColor();
		trackIcon.setImageDrawable(getTrackIcon(app, trackDrawInfo.getWidth(), trackDrawInfo.isShowArrows(), color));
	}

	@Override
	protected void onHeaderClick() {
		adjustMapPosition(getViewY());
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView();
			GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
			KQuadRect r = gpxFile.getRect();

			RotatedTileBox tb = mapActivity.getMapView().getRotatedTileBox();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			int marginLeftPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
				marginLeftPx = getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(mapActivity);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (r.getLeft() != 0 && r.getRight() != 0) {
				mapActivity.getMapView().fitRectToMap(r.getLeft(), r.getRight(), r.getTop(), r.getBottom(),
						tileBoxWidthPx, tileBoxHeightPx, 0, marginLeftPx);
			}
		}
	}

	public static Drawable getTrackIcon(OsmandApplication app, String widthAttr, boolean showArrows, @ColorInt int color) {
		int widthIconId = getWidthIconId(widthAttr);
		Drawable widthIcon = app.getUIUtilities().getPaintedIcon(widthIconId, color);

		int strokeIconId = getStrokeIconId(widthAttr);
		int strokeColor = ColorUtilities.getColorWithAlpha(Color.BLACK, 0.7f);
		Drawable strokeIcon = app.getUIUtilities().getPaintedIcon(strokeIconId, strokeColor);

		Drawable transparencyIcon = getTransparencyIcon(app, widthAttr, color);
		if (showArrows) {
			int arrowsIconId = getArrowsIconId(widthAttr);
			int contrastColor = ColorUtilities.getContrastColor(app, color, false);
			Drawable arrows = app.getUIUtilities().getPaintedIcon(arrowsIconId, contrastColor);
			return UiUtilities.getLayeredIcon(transparencyIcon, widthIcon, strokeIcon, arrows);
		}
		return UiUtilities.getLayeredIcon(transparencyIcon, widthIcon, strokeIcon);
	}

	private static Drawable getTransparencyIcon(OsmandApplication app, String widthAttr, @ColorInt int color) {
		int transparencyIconId = getTransparencyIconId(widthAttr);
		int colorWithoutAlpha = ColorUtilities.removeAlpha(color);
		int transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f);
		return app.getUIUtilities().getPaintedIcon(transparencyIconId, transparencyColor);
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (bottomContainer == null) {
				return;
			}
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackground(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int listBgColor = ColorUtilities.getListBgColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, listBgColor);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, listBgColor);
			}
		}
	}

	private void setupButtons() {
		View buttonsContainer = view.findViewById(R.id.bottom_buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));
		DialogButton saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_apply);
		saveButton.setOnClickListener(v -> onSaveButtonClicked());

		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> {
			discardSplitChanges();
			discardShowStartFinishChanges();
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		AndroidUiHelper.updateVisibility(saveButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void onSaveButtonClicked() {
		getColorCardController().getColorsPaletteController().refreshLastUsedTime();
		GradientColorsPaletteController gradientColorsPaletteController = getColorCardController().getGradientPaletteController();
		if (gradientColorsPaletteController != null) {
			gradientColorsPaletteController.refreshLastUsedTime();
		}
		saveTrackInfo();
		dismiss();
	}

	private void setupScrollShadow() {
		View scrollView = getBottomScrollView();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean scrollToTopAvailable = scrollView.canScrollVertically(-1);
			boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
			if (scrollToTopAvailable) {
				showHeaderShadow();
			} else {
				hideHeaderShadow();
			}
			if (scrollToBottomAvailable) {
				showButtonsShadow();
			} else {
				hideButtonsShadow();
			}
		});
	}

	private void showHeaderShadow() {
		if (getBottomContainer() != null) {
			getBottomContainer().setForeground(getIcon(R.drawable.bg_contextmenu_shadow));
		}
	}

	private void hideHeaderShadow() {
		if (getBottomContainer() != null) {
			getBottomContainer().setForeground(null);
		}
	}

	private void showButtonsShadow() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200);
	}

	private void hideButtonsShadow() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);
	}

	private void updateColorItems() {
		updateAppearanceIcon();
		refreshMap();
	}

	public void refreshMap() {
		MapActivity mapActivity = getMapActivity();
		if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			mapActivity.refreshMap();
		}
	}

	private void saveTrackInfo() {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		if (gpxFile.isShowCurrentTrack()) {
			settings.CURRENT_TRACK_COLOR.set(trackDrawInfo.getColor());
			settings.CURRENT_TRACK_COLORING_TYPE.set(trackDrawInfo.getColoringType());
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.set(trackDrawInfo.getRouteInfoAttribute());
			settings.CURRENT_TRACK_WIDTH.set(trackDrawInfo.getWidth());
			settings.CURRENT_TRACK_SHOW_ARROWS.set(trackDrawInfo.isShowArrows());
			settings.CURRENT_TRACK_SHOW_START_FINISH.set(trackDrawInfo.isShowStartFinish());
			settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.set(trackDrawInfo.getTrackVisualizationType().getTypeName());
			settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.set(trackDrawInfo.getTrackWallColorType().getTypeName());
			settings.CURRENT_TRACK_3D_LINE_POSITION_TYPE.set(trackDrawInfo.getTrackLinePositionType().getTypeName());
			settings.CURRENT_TRACK_ADDITIONAL_EXAGGERATION.set(trackDrawInfo.getAdditionalExaggeration());
			settings.CURRENT_TRACK_ELEVATION_METERS.set(trackDrawInfo.getElevationMeters());
			settings.CURRENT_GRADIENT_PALETTE.set(trackDrawInfo.getGradientColorName());
		} else if (gpxDataItem != null) {
			gpxDataItem.setParameter(COLOR, trackDrawInfo.getColor());
			gpxDataItem.setParameter(WIDTH, trackDrawInfo.getWidth());
			gpxDataItem.setParameter(SHOW_ARROWS, trackDrawInfo.isShowArrows());
			gpxDataItem.setParameter(SHOW_START_FINISH, trackDrawInfo.isShowStartFinish());
			gpxDataItem.setParameter(SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(trackDrawInfo.getSplitType()).getType());
			gpxDataItem.setParameter(SPLIT_INTERVAL, trackDrawInfo.getSplitInterval());
			gpxDataItem.setParameter(COLORING_TYPE, trackDrawInfo.getColoringTypeName());
			gpxDataItem.setParameter(TRACK_VISUALIZATION_TYPE, trackDrawInfo.getTrackVisualizationType().getTypeName());
			gpxDataItem.setParameter(TRACK_3D_WALL_COLORING_TYPE, trackDrawInfo.getTrackWallColorType().getTypeName());
			gpxDataItem.setParameter(TRACK_3D_LINE_POSITION_TYPE, trackDrawInfo.getTrackLinePositionType().getTypeName());
			gpxDataItem.setParameter(ADDITIONAL_EXAGGERATION, (double) trackDrawInfo.getAdditionalExaggeration());
			gpxDataItem.setParameter(ELEVATION_METERS, (double) trackDrawInfo.getElevationMeters());
			gpxDataItem.setParameter(COLOR_PALETTE, trackDrawInfo.getGradientColorName());
			gpxDbHelper.updateDataItem(gpxDataItem);
		}
	}

	private void discardSplitChanges() {
		if (gpxDataItem != null) {
			GpxAppearanceHelper appearanceHelper = new GpxAppearanceHelper(app);
			int type = appearanceHelper.getParameter(gpxDataItem, SPLIT_TYPE);
			double interval = appearanceHelper.getParameter(gpxDataItem, SPLIT_INTERVAL);
			if (type != trackDrawInfo.getSplitType() || interval != trackDrawInfo.getSplitInterval()) {
				applySplit(GpxSplitType.getSplitTypeByTypeId(type), (int) interval, interval);
			}
		}
	}

	private void discardShowStartFinishChanges() {
		settings.CURRENT_TRACK_SHOW_START_FINISH.set(showStartFinishIconsInitialValue);
	}

	void applySplit(GpxSplitType splitType, int timeSplit, double distanceSplit) {
		if (splitIntervalCard != null) {
			splitIntervalCard.updateContent();
		}
		List<GpxDisplayGroup> groups = getGpxDisplayGroups();
		SplitTrackListener listener = getSplitTrackListener();

		double splitInterval = splitType == GpxSplitType.DISTANCE ? distanceSplit : timeSplit;
		GpxSplitParams params = new GpxSplitParams(splitType, splitInterval, trackDrawInfo.isJoinSegments());

		app.getGpxDisplayHelper().splitTrackAsync(selectedGpxFile, groups, params, listener);
	}

	@NonNull
	private SplitTrackListener getSplitTrackListener() {
		return new SplitTrackListener() {
			@Override
			public void trackSplittingFinished(boolean success) {
				if (success && selectedGpxFile != null) {
					List<GpxDisplayGroup> groups = getGpxDisplayGroups();
					selectedGpxFile.setSplitGroups(groups, app);
					refreshMap();
				}
			}
		};
	}

	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup container = getCardsContainer();
			container.removeAllViews();
			cards.clear();

			inflate(R.layout.list_item_divider_with_padding_basic, container, true);

			if (!selectedGpxFile.isShowCurrentTrack()) {
				splitIntervalCard = new SplitIntervalCard(mapActivity, trackDrawInfo);
				addCard(container, splitIntervalCard);
			}
			addCard(container, new DirectionArrowsCard(mapActivity, trackDrawInfo));
			addCard(container, new ShowStartFinishCard(mapActivity, trackDrawInfo));

			inflate(R.layout.list_item_divider_basic, container, true);

			TrackColorController trackColorController = getColorCardController();
			addCard(container, new MultiStateCard(mapActivity, trackColorController));

			inflate(R.layout.list_item_divider_basic, container, true);

			TrackWidthController trackWidthController = getWidthCardController();
			addCard(container, new HeadedContentCard(mapActivity, trackWidthController));

			inflate(R.layout.list_item_divider_basic, container, true);

			track3DCard = new Track3DCard(mapActivity, selectedGpxFile.getTrackAnalysis(app), trackDrawInfo);
			addCard(container, track3DCard);
			addCard(container, new ActionsCard(mapActivity));
		}
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build(container.getContext()));
	}

	private TrackColorController getColorCardController() {
		return TrackColorController.getInstance(app, selectedGpxFile, trackDrawInfo, this);
	}

	private TrackWidthController getWidthCardController() {
		OnNeedScrollListener onNeedScrollListener = y -> {
			int bottomVisibleY = getBottomVisibleY();
			if (y > bottomVisibleY) {
				ScrollView scrollView = (ScrollView) getBottomScrollView();
				int diff = y - bottomVisibleY;
				int scrollY = scrollView.getScrollY();
				scrollView.smoothScrollTo(0, scrollY + diff);
			}
		};
		return TrackWidthController.getInstance(app, trackDrawInfo, onNeedScrollListener, this);
	}

	public List<GpxDisplayGroup> getGpxDisplayGroups() {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		if (gpxFile.getModifiedTime() != modifiedTime) {
			modifiedTime = gpxFile.getModifiedTime();
			GpxDisplayHelper displayHelper = app.getGpxDisplayHelper();
			displayGroups = displayHelper.collectDisplayGroups(selectedGpxFile, gpxFile, true, true);
		}
		return displayGroups;
	}

	private int getBottomVisibleY() {
		return controlButtons.getTop();
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @Nullable Fragment target) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackAppearanceFragment fragment = new TrackAppearanceFragment();
			fragment.setRetainInstance(true);
			fragment.setSelectedGpxFile(selectedGpxFile);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	public static int getTransparencyIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_transparency;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_transparency;
		} else {
			return R.drawable.ic_action_track_line_thin_transparency;
		}
	}

	public static int getWidthIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_color;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_color;
		} else {
			return R.drawable.ic_action_track_line_thin_color;
		}
	}

	public static int getStrokeIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_stroke;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_stroke;
		} else {
			return R.drawable.ic_action_track_line_thin_stroke;
		}
	}

	public static int getArrowsIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_direction;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_direction;
		} else {
			return R.drawable.ic_action_track_line_thin_direction;
		}
	}

	@Override
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onMapOptionChanged(float value) {
		if (trackDrawInfo.isFixedHeight()) {
			trackDrawInfo.setElevationMeters((int) value);
		} else {
			trackDrawInfo.setAdditionalExaggeration(value);
		}
	}

	public interface OnNeedScrollListener {
		void onVerticalScrollNeeded(int y);
	}
}