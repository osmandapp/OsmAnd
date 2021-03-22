package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ValueHolder;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.enums.TracksSortByMode;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.OnGpxImportCompleteListener;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.cards.AttachTrackToRoadsCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.ImportTrackCard;
import net.osmand.plus.routepreparationmenu.cards.NavigateTrackOptionsCard;
import net.osmand.plus.routepreparationmenu.cards.ReverseTrackCard;
import net.osmand.plus.routepreparationmenu.cards.SelectTrackCard;
import net.osmand.plus.routepreparationmenu.cards.TrackEditCard;
import net.osmand.plus.routepreparationmenu.cards.TracksToFollowCard;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.TrackSelectSegmentBottomSheet;
import net.osmand.plus.track.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.views.layers.MapControlsLayer.MapControlsThemeInfoProvider;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FollowTrackFragment extends ContextMenuScrollFragment implements CardListener,
		IRouteInformationListener, MapControlsThemeInfoProvider, OnSegmentSelectedListener {

	public static final String TAG = FollowTrackFragment.class.getName();

	private static final Log log = PlatformUtil.getLog(FollowTrackFragment.class);

	private static final String SELECTING_TRACK = "selecting_track";

	private OsmandApplication app;
	private ImportHelper importHelper;

	private GPXFile gpxFile;

	private View buttonsShadow;
	private ImageButton sortButton;

	private TracksToFollowCard tracksCard;
	private TracksSortByMode sortByMode = TracksSortByMode.BY_DATE;

	private boolean editingTrack;
	private boolean selectingTrack;
	private int menuTitleHeight;

	@Override
	public int getMainLayoutId() {
		return R.layout.follow_track_options;
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

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		MapActivity mapActivity = requireMapActivity();
		importHelper = new ImportHelper(mapActivity, getMyApplication(), null);

		GPXRouteParamsBuilder routeParamsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (routeParamsBuilder != null) {
			gpxFile = routeParamsBuilder.getFile();
		}

		if (savedInstanceState != null) {
			selectingTrack = savedInstanceState.getBoolean(SELECTING_TRACK, gpxFile == null);
		} else {
			selectingTrack = gpxFile == null;
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			ImageButton closeButton = view.findViewById(R.id.close_button);
			buttonsShadow = view.findViewById(R.id.buttons_shadow);
			sortButton = view.findViewById(R.id.sort_button);
			closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dismiss();
				}
			});

			if (isPortrait()) {
				updateCardsLayout();
			}
			setupCards();
			setupButtons(view);
			setupSortButton(view);
			setupScrollShadow();
			if (!isPortrait()) {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
			}
			enterTrackAppearanceMode();
			runLayoutListener();
		}
		return view;
	}

	private void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	private void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);

	}


	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			if (gpxFile == null || selectingTrack) {
				setupTracksCard();
			} else {
				sortButton.setVisibility(View.GONE);
				TrackEditCard importTrackCard = new TrackEditCard(mapActivity, gpxFile);
				importTrackCard.setListener(this);
				cardsContainer.addView(importTrackCard.build(mapActivity));

				SelectTrackCard selectTrackCard = new SelectTrackCard(mapActivity);
				selectTrackCard.setListener(this);
				cardsContainer.addView(selectTrackCard.build(mapActivity));

				ApplicationMode mode = app.getRoutingHelper().getAppMode();

				RoutingHelper routingHelper = app.getRoutingHelper();
				GPXRouteParamsBuilder rparams = routingHelper.getCurrentGPXRoute();
				boolean osmandRouter = mode.getRouteService() == RouteService.OSMAND;
				if (rparams != null && osmandRouter) {
					cardsContainer.addView(buildDividerView(cardsContainer, false));

					ReverseTrackCard reverseTrackCard = new ReverseTrackCard(mapActivity, rparams.isReverse());
					reverseTrackCard.setListener(this);
					cardsContainer.addView(reverseTrackCard.build(mapActivity));

					if (!gpxFile.hasRtePt() && !gpxFile.hasRoute()) {
						cardsContainer.addView(buildDividerView(cardsContainer, true));

						AttachTrackToRoadsCard attachTrackCard = new AttachTrackToRoadsCard(mapActivity);
						attachTrackCard.setListener(this);
						cardsContainer.addView(attachTrackCard.build(mapActivity));
					}
					if (!rparams.isUseIntermediatePointsRTE()) {
						setupNavigateOptionsCard(rparams);
					}
				}
			}
		}
	}

	private void setupTracksCard() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			List<String> selectedTrackNames = GpxUiHelper.getSelectedTrackPaths(app);
			List<GPXInfo> list = GpxUiHelper.getSortedGPXFilesInfo(dir, selectedTrackNames, false);
			if (list.size() > 0) {
				String defaultCategory = app.getString(R.string.shared_string_all);
				tracksCard = new TracksToFollowCard(mapActivity, list, defaultCategory);
				tracksCard.setListener(FollowTrackFragment.this);
				getCardsContainer().addView(tracksCard.build(mapActivity));
				sortButton.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupNavigateOptionsCard(GPXRouteParamsBuilder rparams) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int passRouteId = R.string.gpx_option_from_start_point;
			LocalRoutingParameter passWholeRoute = new OtherLocalRoutingParameter(passRouteId,
					app.getString(passRouteId), rparams.isPassWholeRoute());

			int navigationTypeId = R.string.gpx_option_calculate_first_last_segment;
			LocalRoutingParameter navigationType = new OtherLocalRoutingParameter(navigationTypeId,
					app.getString(navigationTypeId), rparams.isCalculateOsmAndRouteParts());

			NavigateTrackOptionsCard navigateTrackCard = new NavigateTrackOptionsCard(mapActivity, passWholeRoute, navigationType);
			navigateTrackCard.setListener(this);
			getCardsContainer().addView(navigateTrackCard.build(mapActivity));
		}
	}

	public View buildDividerView(@NonNull ViewGroup view, boolean needMargin) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), isNightMode());
		View divider = themedInflater.inflate(R.layout.simple_divider_item, view, false);

		LayoutParams params = divider.getLayoutParams();
		if (needMargin && params instanceof MarginLayoutParams) {
			AndroidUtils.setMargins((MarginLayoutParams) params, dpToPx(64), 0, 0, 0);
			divider.setLayoutParams(params);
		}

		return divider;
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getRoutingHelper().addListener(this);
		MapRouteInfoMenu.followTrackVisible = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getRoutingHelper().removeListener(this);
		MapRouteInfoMenu.followTrackVisible = false;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
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
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRoutePlanningMode() && mapActivity.getMapView() != null) {
			QuadRect rect = mapActivity.getMapRouteInfoMenu().getRouteRect(mapActivity);

			if (gpxFile != null) {
				QuadRect gpxRect = gpxFile.getRect();

				rect.left = Math.min(rect.left, gpxRect.left);
				rect.right = Math.max(rect.right, gpxRect.right);
				rect.top = Math.max(rect.top, gpxRect.top);
				rect.bottom = Math.min(rect.bottom, gpxRect.bottom);
			}

			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(app);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (rect.left != 0 && rect.right != 0) {
				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (!editingTrack) {
			exitTrackAppearanceMode();
		}
		onDismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SELECTING_TRACK, selectingTrack);
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
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
				return nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
			} else {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
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
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof ImportTrackCard) {
				importTrack();
			} else if (card instanceof AttachTrackToRoadsCard) {
				openPlanRoute(true);
				close();
			} else if (card instanceof TrackEditCard) {
				openPlanRoute(false);
				close();
			} else if (card instanceof SelectTrackCard) {
				updateSelectionMode(true);
			} else if (card instanceof ReverseTrackCard
					|| card instanceof NavigateTrackOptionsCard) {
				updateMenu();
			}
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof TracksToFollowCard) {
				if (buttonIndex >= 0) {
					loadAndFollowTrack((TracksToFollowCard) card, buttonIndex);
				}
			}
		}
	}

	private void loadAndFollowTrack(TracksToFollowCard card, int index) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && index < card.getGpxInfoList().size()) {
			GPXInfo gpxInfo = card.getGpxInfoList().get(index);
			String fileName = gpxInfo.getFileName();
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(fileName);
			if (selectedGpxFile != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				if (gpxFile.getNonEmptySegmentsCount() > 1) {
					TrackSelectSegmentBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), gpxFile, this);
				} else {
					selectTrackToFollow(gpxFile);
					updateSelectionMode(false);
				}
			} else {
				CallbackWithObject<GPXFile[]> callback = new CallbackWithObject<GPXFile[]>() {
					@Override
					public boolean processResult(GPXFile[] result) {
						MapActivity mapActivity = getMapActivity();
						if (mapActivity != null) {
							if (result[0].getNonEmptySegmentsCount() > 1) {
								TrackSelectSegmentBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), result[0], FollowTrackFragment.this);
							} else {
								selectTrackToFollow(result[0]);
								updateSelectionMode(false);
							}
						}
						return true;
					}
				};
				File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
				GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, callback, dir, null, fileName);
			}
		}
	}

	private void selectTrackToFollow(GPXFile gpxFile) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			this.gpxFile = gpxFile;
			List<WptPt> points = gpxFile.getRoutePoints();
			if (!points.isEmpty()) {
				ApplicationMode mode = ApplicationMode.valueOfStringKey(points.get(0).getProfileType(), null);
				if (mode != null) {
					app.getRoutingHelper().setAppMode(mode);
					app.initVoiceCommandPlayer(mapActivity, mode, true, null, false, false, true);
				}
			}
			mapActivity.getMapActions().setGPXRouteParams(gpxFile);
			app.getTargetPointsHelper().updateRouteAndRefresh(true);
			app.getRoutingHelper().onSettingsChanged(true);
		}
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	private void updateSelectionMode(boolean selecting) {
		this.selectingTrack = selecting;
		setupCards();
	}

	public void importTrack() {
		Intent intent = ImportHelper.getImportTrackIntent();
		try {
			startActivityForResult(intent, ImportHelper.IMPORT_FILE_REQUEST);
		} catch (ActivityNotFoundException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ImportHelper.IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				importHelper.setGpxImportCompleteListener(new OnGpxImportCompleteListener() {
					@Override
					public void onImportComplete(boolean success) {

					}

					@Override
					public void onSaveComplete(boolean success, GPXFile result) {
						if (success) {
							selectTrackToFollow(result);
							updateSelectionMode(false);
						} else {
							app.showShortToastMessage(app.getString(R.string.error_occurred_loading_gpx));
						}
						importHelper.setGpxImportCompleteListener(null);
					}
				});
				importHelper.handleGpxImport(uri, true, false);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void openPlanRoute(boolean showSnapWarning) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && gpxFile != null) {
			editingTrack = true;
			GpxData gpxData = new GpxData(gpxFile);
			MeasurementEditingContext editingContext = new MeasurementEditingContext();
			editingContext.setGpxData(gpxData);
			editingContext.setAppMode(app.getRoutingHelper().getAppMode());
			editingContext.setSelectedSegment(app.getSettings().GPX_ROUTE_SEGMENT.get());
			MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), editingContext, true, showSnapWarning);
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
			}
		}
	}

	private void setupSortButton(View view) {
		final ImageButton sortButton = view.findViewById(R.id.sort_button);
		int colorId = isNightMode() ? R.color.inactive_buttons_and_links_bg_dark : R.color.inactive_buttons_and_links_bg_light;
		Drawable background = app.getUIUtilities().getIcon(R.drawable.bg_dash_line_dark, colorId);
		sortButton.setImageResource(sortByMode.getIconId());
		AndroidUtils.setBackground(sortButton, background);
		sortButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				List<PopUpMenuItem> items = new ArrayList<>();
				for (final TracksSortByMode mode : TracksSortByMode.values()) {
					items.add(new PopUpMenuItem.Builder(app)
							.setTitleId(mode.getNameId())
							.setIcon(app.getUIUtilities().getThemedIcon(mode.getIconId()))
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									sortByMode = mode;
									sortButton.setImageResource(mode.getIconId());
									if (tracksCard != null) {
										tracksCard.setSortByMode(mode);
									}
								}
							})
							.setSelected(sortByMode == mode)
							.create()
					);
				}
				new PopUpMenuHelper.Builder(v, items, isNightMode()).show();
			}
		});
	}

	private void setupButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_close);
	}

	private void setupScrollShadow() {
		final View scrollView = getBottomScrollView();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener() {

			@Override
			public void onScrollChanged() {
				boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
				if (scrollToBottomAvailable) {
					showShadowButton();
				} else {
					hideShadowButton();
				}
			}
		});
	}

	private void close() {
		try {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	private void onDismiss() {
		try {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && !editingTrack) {
				if (!mapActivity.isChangingConfigurations()) {
					mapActivity.getMapRouteInfoMenu().cancelSelectionFromTracks();
				}
				mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoControlDialog();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && newRoute && app.getRoutingHelper().isRoutePlanningMode()) {
			adjustMapPosition(getHeight());
		}
	}

	@Override
	public void routeWasCancelled() {

	}

	@Override
	public void routeWasFinished() {

	}

	@Override
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onSegmentSelect(GPXFile gpxFile, int selectedSegment) {
		app.getSettings().GPX_ROUTE_SEGMENT.set(selectedSegment);
		selectTrackToFollow(gpxFile);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedSegment(selectedSegment);
			app.getRoutingHelper().onSettingsChanged(true);
		}
		updateSelectionMode(false);
	}
}