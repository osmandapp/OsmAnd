package net.osmand.plus.track.fragments;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.activities.MapActivityActions.KEY_LATITUDE;
import static net.osmand.plus.activities.MapActivityActions.KEY_LONGITUDE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.ATTACH_ROADS_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.CALCULATE_HEIGHTMAP_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.CALCULATE_SRTM_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.PLAN_ROUTE_MODE;
import static net.osmand.plus.track.cards.OptionsCard.ALTITUDE_CORRECTION_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.ANALYZE_BY_INTERVALS_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.ANALYZE_ON_MAP_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.CHANGE_FOLDER_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.DELETE_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.GPS_FILTER_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.JOIN_GAPS_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.RENAME_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.SHARE_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.SIMULATE_POSITION_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.UPLOAD_OSM_BUTTON_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.ADD_WAYPOINT_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.DELETE_WAYPOINTS_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.OPEN_WAYPOINT_INDEX;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.isGpxFileSelected;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.controllers.NetworkRouteDrawable;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment.MeasurementToolMode;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.myplaces.tracks.dialogs.SplitSegmentDialogFragment;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.simulation.SimulateLocationFragment;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.cards.AuthorCard;
import net.osmand.plus.track.cards.CopyrightCard;
import net.osmand.plus.track.cards.DescriptionCard;
import net.osmand.plus.track.cards.GpxInfoCard;
import net.osmand.plus.track.cards.OptionsCard;
import net.osmand.plus.track.cards.OverviewCard;
import net.osmand.plus.track.cards.PointsGroupsCard;
import net.osmand.plus.track.cards.TrackTagsInfoCard;
import net.osmand.plus.track.cards.SegmentsCard;
import net.osmand.plus.track.cards.TrackPointsCard;
import net.osmand.plus.track.fragments.DisplayGroupsBottomSheet.DisplayPointGroupsCallback;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnSaveDescriptionCallback;
import net.osmand.plus.track.fragments.GpsFilterFragment.GpsFilterFragmentLister;
import net.osmand.plus.track.fragments.TrackAltitudeBottomSheet.CalculateAltitudeListener;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.fragments.controller.EditGpxDescriptionController;
import net.osmand.plus.track.fragments.controller.SelectRouteActivityController;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper.DisplayGroupsHolder;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.track.helpers.save.SaveGpxListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.IconPopupMenu;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TrackMenuFragment extends ContextMenuScrollFragment implements CardListener,
		SegmentActionsListener, RenameCallback, OnTrackFileMoveListener, OnPointsDeleteListener,
		OsmAndLocationListener, OsmAndCompassListener, OnSegmentSelectedListener, GpsFilterFragmentLister,
		DisplayPointGroupsCallback, CalculateAltitudeListener, OnSaveDescriptionCallback {

	public static final String TAG = TrackMenuFragment.class.getName();

	public static final String TRACK_FILE_NAME = "track_file_name";
	public static final String OPEN_TAB_NAME = "open_tab_name";
	public static final String CHART_TAB_NAME = "chart_tab_name";
	public static final String CURRENT_RECORDING = "current_recording";
	public static final String OPEN_TRACK_MENU = "open_track_menu";
	public static final String TEMPORARY_SELECTED = "temporary_selected";
	public static final String RETURN_SCREEN_NAME = "return_screen_name";
	public static final String CALLING_FRAGMENT_TAG = "calling_fragment_tag";
	public static final String ADJUST_MAP_POSITION = "adjust_map_position";

	private TrackDisplayHelper displayHelper;
	private GpxSelectionHelper gpxSelectionHelper;
	private SelectedGpxFile selectedGpxFile;
	private GpxTrackAnalysis analysis;

	private TrackMenuTab menuType = TrackMenuTab.OVERVIEW;
	private SegmentsCard segmentsCard;
	private OptionsCard optionsCard;
	private DescriptionCard descriptionCard;
	private GpxInfoCard gpxInfoCard;
	private AuthorCard authorCard;
	private CopyrightCard copyrightCard;
	private TrackTagsInfoCard trackTagsInfoCard;
	private OverviewCard overviewCard;
	private TrackPointsCard pointsCard;
	private PointsGroupsCard groupsCard;

	private TextView headerTitle;
	private ImageView headerIcon;
	private View toolbarContainer;
	private View searchContainer;
	private ImageView searchButton;
	private ImageView displayGroupsButton;
	private EditText searchEditText;
	private View backButtonContainer;
	private View displayGroupsWidget;
	private TextView toolbarTextView;
	private ViewGroup headerContainer;
	private View routeMenuTopShadowAll;
	private BottomNavigationView bottomNav;
	private OnScrollChangedListener bottomScrollChangedListener;

	private String gpxTitle;
	private String returnScreenName;
	private String callingFragmentTag;
	private GPXTabItemType chartTabToOpen;
	private SelectedGpxPoint gpxPoint;
	private TrackChartPoints trackChartPoints;
	private RouteActivitySelectionHelper routeActivitySelectionHelper;
	private RouteKey routeKey;
	private boolean temporarySelected;

	private Float heading;
	private Location lastLocation;
	private UpdateLocationViewCache updateLocationViewCache;
	private boolean locationUpdateStarted;
	private LatLon latLon;

	private int menuTitleHeight;
	private int toolbarHeightPx;
	private boolean adjustMapPosition = true;
	private boolean menuTypeChanged;
	private boolean overviewInitialHeight = true;
	private int overviewInitialPosY;

	public enum TrackMenuTab {
		OVERVIEW(R.id.action_overview, R.string.shared_string_overview),
		TRACK(R.id.action_track, R.string.shared_string_gpx_tracks),
		POINTS(R.id.action_points, R.string.shared_string_gpx_points),
		OPTIONS(R.id.action_options, R.string.shared_string_options);

		TrackMenuTab(int menuItemId, @StringRes int titleId) {
			this.menuItemId = menuItemId;
			this.titleId = titleId;
		}

		public final int menuItemId;
		public final int titleId;
	}

	public void setMenuType(TrackMenuTab menuType) {
		this.menuType = menuType;
	}

	@Override
	public int getMainLayoutId() {
		return R.layout.track_menu;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_navigation);
		return ids;
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
		return isPortrait() ? toolbarHeightPx : 0;
	}

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	public int getMinY() {
		return getFullScreenTopPosY();
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	public SelectedGpxFile getSelectedGpxFile() {
		return selectedGpxFile;
	}

	@Override
	public TrackDisplayHelper getDisplayHelper() {
		return displayHelper;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentActivity activity = requireMyActivity();

		displayHelper = new TrackDisplayHelper(app);
		gpxSelectionHelper = app.getSelectedGpxHelper();
		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(activity);

		toolbarHeightPx = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);
		if (selectedGpxFile == null && savedInstanceState != null) {
			String path = savedInstanceState.getString(TRACK_FILE_NAME);
			boolean showCurrentTrack = savedInstanceState.getBoolean(CURRENT_RECORDING);
			MapActivity mapActivity = requireMapActivity();
			loadSelectedGpxFile(mapActivity, path, showCurrentTrack, result -> {
				setSelectedGpxFile(result);
				onSelectedGpxFileAvailable();
				if (getView() != null) {
					initContent(getView());
				}
				return true;
			});
			if (savedInstanceState.containsKey(KEY_LATITUDE) && savedInstanceState.containsKey(KEY_LONGITUDE)) {
				double latitude = savedInstanceState.getDouble(KEY_LATITUDE);
				double longitude = savedInstanceState.getDouble(KEY_LONGITUDE);
				latLon = new LatLon(latitude, longitude);
			}
		} else if (selectedGpxFile != null) {
			onSelectedGpxFileAvailable();
		}

		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				if (getCurrentMenuState() != MenuState.HEADER_ONLY && isPortrait()) {
					openMenuHeaderOnly();
				} else {
					dismiss();
					hideSelectedGpx();

					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						MapContextMenu contextMenu = mapActivity.getContextMenu();
						PointDescription pointDescription = contextMenu.getPointDescription();
						if (pointDescription != null && pointDescription.isGpxPoint()) {
							contextMenu.init(contextMenu.getLatLon(), pointDescription, contextMenu.getObject());
							contextMenu.show();
						} else if (Algorithms.objectEquals(callingFragmentTag, QuickSearchDialogFragment.TAG)) {
							mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.CURRENT, false);
						} else {
							mapActivity.launchPrevActivityIntent();
						}
					}
				}
			}
		});
	}

	private void onSelectedGpxFileAvailable() {
		setupDisplayHelper();
		updateGpxTitle();
	}

	private void setupDisplayHelper() {
		if (!selectedGpxFile.isShowCurrentTrack()) {
			File file = new File(selectedGpxFile.getGpxFile().getPath());
			displayHelper.setFile(file);
			displayHelper.setGpxDataItem(app.getGpxDbHelper().getItem(SharedUtil.kFile(file)));
		}
		displayHelper.setGpx(selectedGpxFile.getGpxFileToDisplay());
		displayHelper.setSelectedGpxFile(selectedGpxFile);
		if (selectedGpxFile.getFilteredSelectedGpxFile() != null) {
			displayHelper.setFilteredGpxFile(selectedGpxFile.getFilteredSelectedGpxFile().getGpxFile());
		}
		if (analysis == null) {
			analysis = selectedGpxFile.getTrackAnalysisToDisplay(app);
		}
	}

	private void updateGpxTitle() {
		GpxFile gpxFile = getGpx();
		if (isCurrentRecordingTrack()) {
			gpxTitle = app.getString(R.string.shared_string_currently_recording_track);
		} else if (!Algorithms.isBlank(gpxFile.getArticleTitle())) {
			gpxTitle = gpxFile.getArticleTitle();
		} else {
			gpxTitle = GpxHelper.INSTANCE.getGpxTitle(Algorithms.getFileWithoutDirs(gpxFile.getPath()));
		}
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public GpxFile getGpx() {
		return displayHelper.getGpx();
	}

	public void setSelectedGpxFile(SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	public void setLatLon(LatLon latLon) {
		this.latLon = latLon;
	}

	public void setReturnScreenName(String returnScreenName) {
		this.returnScreenName = returnScreenName;
	}

	public void setCallingFragmentTag(String callingFragmentTag) {
		this.callingFragmentTag = callingFragmentTag;
	}

	public void setTemporarySelected(boolean temporarySelected) {
		this.temporarySelected = temporarySelected;
	}

	public void setRouteKey(RouteKey routeKey) {
		this.routeKey = routeKey;
	}

	public void setGpxPoint(SelectedGpxPoint point) {
		this.gpxPoint = point;
	}

	public void setAdjustMapPosition(boolean adjustMapPosition) {
		this.adjustMapPosition = adjustMapPosition;
	}

	public void setChartTabToOpen(@Nullable GPXTabItemType chartTabToOpen) {
		this.chartTabToOpen = chartTabToOpen;
	}

	private void setAnalysis(@Nullable GpxTrackAnalysis analysis) {
		this.analysis = analysis;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			bottomNav = view.findViewById(R.id.bottom_navigation);
			routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);
			headerContainer = view.findViewById(R.id.header_container);
			headerTitle = view.findViewById(R.id.title);
			headerIcon = view.findViewById(R.id.icon_view);
			toolbarContainer = view.findViewById(R.id.context_menu_toolbar_container);
			toolbarTextView = view.findViewById(R.id.toolbar_title);
			searchButton = view.findViewById(R.id.search_button);
			displayGroupsButton = view.findViewById(R.id.appbar_display_groups_button);
			searchContainer = view.findViewById(R.id.search_container);
			backButtonContainer = view.findViewById(R.id.back_button_container);
			displayGroupsWidget = view.findViewById(R.id.display_groups_button_container);

			if (isPortrait()) {
				AndroidUiHelper.updateVisibility(getTopShadow(), true);
			} else {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				bottomNav.setLayoutParams(params);
				shiftMapControls(widthNoShadow);
			}
			if (selectedGpxFile != null) {
				initContent(view);
			}
		}
		return view;
	}

	private void shiftMapControls(int shift) {
		MarginLayoutParams params = (MarginLayoutParams) backButtonContainer.getLayoutParams();
		AndroidUtils.setMargins(params, shift, params.topMargin, params.getMarginEnd(), params.bottomMargin);
	}

	private void initContent(@NonNull View view) {
		setupCards(false);
		setupToolbar();
		updateHeader();
		updateHeadersBottomShadow();
		setupButtons(view);
		updateCardsLayout();
		if (menuType == TrackMenuTab.OVERVIEW && isPortrait()) {
			calculateLayoutAndShowOverview();
		} else {
			calculateLayoutAndUpdateMenuState(null);
		}
	}

	private void updateHeader() {
		updateHeaderCard();
		headerTitle.setText(getHeaderTitle());

		if (menuType == TrackMenuTab.POINTS) {
			AndroidUiHelper.updateVisibility(searchButton, true);
		} else {
			AndroidUiHelper.updateVisibility(toolbarTextView, true);
			AndroidUiHelper.updateVisibility(searchButton, false);
			AndroidUiHelper.updateVisibility(searchContainer, false);
		}
		AndroidUiHelper.updateVisibility(displayGroupsButton, hasPointsGroups());
		AndroidUiHelper.updateVisibility(headerIcon, menuType != TrackMenuTab.OPTIONS);

		Drawable icon = routeKey != null ? new NetworkRouteDrawable(app, routeKey, isNightMode())
				: uiUtilities.getThemedIcon(R.drawable.ic_action_polygom_dark);
		headerIcon.setImageDrawable(icon);
	}

	@NonNull
	private CharSequence getHeaderTitle() {
		if (menuType == TrackMenuTab.TRACK) {
			String title = app.getString(R.string.shared_string_gpx_track) + "\n" + gpxTitle;
			return UiUtilities.createCustomFontSpannable(DEFAULT, title, gpxTitle);
		} else if (menuType == TrackMenuTab.OPTIONS) {
			return app.getString(menuType.titleId);
		} else {
			return gpxTitle;
		}
	}

	private void updateHeadersBottomShadow() {
		View scrollView = getBottomScrollView();
		if (menuType == TrackMenuTab.OVERVIEW) {
			updateBottomHeaderShadowVisibility(scrollView.canScrollVertically(-1));
			bottomScrollChangedListener = () -> {
				boolean scrollableToTop = scrollView.canScrollVertically(-1);
				updateBottomHeaderShadowVisibility(scrollableToTop);
			};
			scrollView.getViewTreeObserver().addOnScrollChangedListener(bottomScrollChangedListener);
		} else {
			scrollView.getViewTreeObserver().removeOnScrollChangedListener(bottomScrollChangedListener);
		}

		if (segmentsCard != null) {
			if (menuType == TrackMenuTab.TRACK) {
				updateBottomHeaderShadowVisibility(segmentsCard.isScrollToTopAvailable());
				segmentsCard.setScrollAvailabilityListener(this::updateBottomHeaderShadowVisibility);
			} else {
				segmentsCard.removeScrollAvailabilityListener();
			}
		}

		if (menuType != TrackMenuTab.OVERVIEW && menuType != TrackMenuTab.TRACK) {
			updateBottomHeaderShadowVisibility(true);
		}
	}

	private void updateBottomHeaderShadowVisibility(boolean visible) {
		if (getBottomContainer() != null) {
			Drawable shadow = visible ? getIcon(R.drawable.bg_contextmenu_shadow) : null;
			getBottomContainer().setForeground(shadow);
		}
	}

	private void updateHeaderCard() {
		if (menuType == TrackMenuTab.POINTS && !Algorithms.isEmpty(pointsCard.getGroups())) {
			addPointsGroupsCardToHeader();
		} else {
			removeCardViewFromHeader(groupsCard);
		}
	}

	private void addPointsGroupsCardToHeader() {
		if (groupsCard != null) {
			addCardViewToHeader(groupsCard);
		} else {
			MapActivity mapActivity = requireMapActivity();
			groupsCard = new PointsGroupsCard(mapActivity, pointsCard.getGroups(), selectedGpxFile);
			groupsCard.setListener(this);
			headerContainer.addView(groupsCard.build(mapActivity));
		}
	}

	private void removeCardViewFromHeader(MapBaseCard card) {
		if (card != null && card.getView() != null) {
			headerContainer.removeView(card.getView());
		}
	}

	private void addCardViewToHeader(MapBaseCard card) {
		if (card != null && card.getView() != null) {
			ViewGroup parent = ((ViewGroup) card.getView().getParent());
			if (parent != null) {
				parent.removeView(card.getView());
			}
			headerContainer.addView(card.getView());
		}
	}

	private void setupToolbar() {
		toolbarTextView.setText(gpxTitle);

		ImageView closeButton = toolbarContainer.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> {
			if (menuType == TrackMenuTab.POINTS) {
				AndroidUiHelper.updateVisibility(toolbarTextView, true);
				AndroidUiHelper.updateVisibility(searchButton, true);
				AndroidUiHelper.updateVisibility(displayGroupsButton, hasPointsGroups());
				AndroidUiHelper.updateVisibility(searchContainer, false);
			}
			openMenuHeaderOnly();
		});
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(toolbarContainer.getContext()));

		searchButton.setOnClickListener(v -> {
			AndroidUiHelper.updateVisibility(searchContainer, true);
			AndroidUiHelper.updateVisibility(searchButton, false);
			AndroidUiHelper.updateVisibility(displayGroupsButton, false);
			AndroidUiHelper.updateVisibility(toolbarTextView, false);
		});
		searchEditText = toolbarContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_poi_filter);
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (pointsCard != null) {
					pointsCard.filter(s.toString());
				}
			}
		});
		ImageView clearButton = toolbarContainer.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(v -> {
			if (!Algorithms.isEmpty(searchEditText.getText())) {
				searchEditText.setText("");
				searchEditText.setSelection(0);
			}
			if (pointsCard != null) {
				pointsCard.updateContent();
			}
		});
		backButtonContainer.setOnClickListener(v -> {
			hideSelectedGpx();
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.launchPrevActivityIntent();
			}
			dismiss();
		});
		TextView backButtonText = backButtonContainer.findViewById(R.id.back_button_text);
		ImageView backButtonIcon = backButtonContainer.findViewById(R.id.back_button_icon);
		int backIconId;
		if (!Algorithms.isEmpty(returnScreenName)) {
			backButtonText.setText(returnScreenName);
			backIconId = AndroidUtils.getNavigationIconResId(app);
		} else {
			backButtonText.setVisibility(View.GONE);
			backIconId = R.drawable.ic_action_close;
		}
		backButtonIcon.setImageResource(backIconId);
		setupDisplayGroupsWidget();
	}

	private void setupDisplayGroupsWidget() {
		OnClickListener listener = view -> {
			MapActivity activity = getMapActivity();
			if (activity != null && getDisplayGroupsHolder().getTotalTrackGroupsNumber() > 0) {
				FragmentManager manager = activity.getSupportFragmentManager();
				DisplayGroupsBottomSheet.showInstance(manager, this, true);
			}
		};
		displayGroupsButton.setOnClickListener(listener);
		displayGroupsWidget.setOnClickListener(listener);
		updateDisplayGroupsWidget();
	}

	public void updateDisplayGroupsWidget() {
		boolean widgetVisible = hasPointsGroups() && !shouldShowWidgets();
		if (widgetVisible) {
			DisplayGroupsHolder displayGroupsHolder = getDisplayGroupsHolder();
			int visible = displayGroupsHolder.getVisibleTrackGroupsNumber(selectedGpxFile);
			int total = displayGroupsHolder.getTotalTrackGroupsNumber();
			TextView indication = displayGroupsWidget.findViewById(R.id.visible_display_groups_size);
			boolean hasRouteGroupsOnly = total == 0;
			if (hasRouteGroupsOnly) {
				indication.setText("0");
			} else {
				indication.setText(getString(
						R.string.ltr_or_rtl_combine_via_slash,
						String.valueOf(visible),
						String.valueOf(total)
				));
			}
		}
		AndroidUiHelper.updateVisibility(displayGroupsWidget, widgetVisible);
	}

	private boolean hasPointsGroups() {
		DisplayGroupsHolder holder = getDisplayGroupsHolder();
		return holder.groups.size() > 0;
	}

	private void hideSelectedGpx() {
		if (temporarySelected) {
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.hideFromMap().syncGroup().saveSelection();
			selectedGpxFile = gpxSelectionHelper.selectGpxFile(selectedGpxFile.getGpxFile(), params);
		}
	}

	@Override
	public void onPointGroupsVisibilityChanged() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			gpxSelectionHelper.updateSelectedGpxFile(selectedGpxFile);
			mapActivity.refreshMap();
			updateDisplayGroupsWidget();
			updatePointGroupsCard();
		}
	}

	private void setupCards(boolean shouldReattachCards) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();
			if (menuType == TrackMenuTab.TRACK) {
				if (shouldReattachCards && segmentsCard != null && segmentsCard.getView() != null) {
					reattachCard(cardsContainer, segmentsCard);
				} else {
					segmentsCard = new SegmentsCard(mapActivity, displayHelper, gpxPoint, selectedGpxFile,
							this, chartTabToOpen);
					segmentsCard.setListener(this);
					cardsContainer.addView(segmentsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuTab.OPTIONS) {
				if (shouldReattachCards && optionsCard != null && optionsCard.getView() != null) {
					reattachCard(cardsContainer, optionsCard);
				} else {
					optionsCard = new OptionsCard(mapActivity, displayHelper, selectedGpxFile);
					optionsCard.setListener(this);
					cardsContainer.addView(optionsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuTab.OVERVIEW) {
				setupOverviewCards(mapActivity, cardsContainer, shouldReattachCards);
			} else if (menuType == TrackMenuTab.POINTS) {
				if (shouldReattachCards && pointsCard != null && pointsCard.getView() != null) {
					reattachCard(cardsContainer, pointsCard);
				} else {
					pointsCard = new TrackPointsCard(mapActivity, displayHelper, selectedGpxFile);
					pointsCard.setListener(this);
					cardsContainer.addView(pointsCard.build(mapActivity));
				}
			}
		}
	}

	private void setupOverviewCards(MapActivity mapActivity, ViewGroup cardsContainer, boolean shouldReattachCards) {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		Metadata metadata = gpxFile.getMetadata();
		RouteActivitySelectionHelper activityHelper = getRouteActivitySelectionHelper(gpxFile);

		if (shouldReattachCards && overviewCard != null && overviewCard.getView() != null) {
			reattachCard(cardsContainer, overviewCard);
		} else {
			overviewCard = new OverviewCard(mapActivity, this, selectedGpxFile,
					displayHelper.getGpxDataItem(), activityHelper, this);
			overviewCard.setListener(this);
			cardsContainer.addView(overviewCard.build(mapActivity));
			if (isCurrentRecordingTrack()) {
				overviewCard.getBlockStatisticsBuilder().runUpdatingStatBlocksIfNeeded();
			}
		}

		if (shouldReattachCards && descriptionCard != null && descriptionCard.getView() != null) {
			reattachCard(cardsContainer, descriptionCard);
		} else {
			descriptionCard = new DescriptionCard(mapActivity, this, gpxFile);
			cardsContainer.addView(descriptionCard.build(mapActivity));
		}

		if (shouldReattachCards && trackTagsInfoCard != null && trackTagsInfoCard.getView() != null) {
			reattachCard(cardsContainer, trackTagsInfoCard);
		} else {
			trackTagsInfoCard = new TrackTagsInfoCard(mapActivity, routeKey, gpxFile, activityHelper);
			cardsContainer.addView(trackTagsInfoCard.build(mapActivity));
		}

		if (shouldReattachCards && gpxInfoCard != null && gpxInfoCard.getView() != null) {
			reattachCard(cardsContainer, gpxInfoCard);
		} else {
			gpxInfoCard = new GpxInfoCard(mapActivity, gpxFile);
			cardsContainer.addView(gpxInfoCard.build(mapActivity));
		}

		if (shouldReattachCards && authorCard != null && authorCard.getView() != null) {
			reattachCard(cardsContainer, authorCard);
		} else {
			authorCard = new AuthorCard(mapActivity, metadata);
			cardsContainer.addView(authorCard.build(mapActivity));
		}

		if (shouldReattachCards && copyrightCard != null && copyrightCard.getView() != null) {
			reattachCard(cardsContainer, copyrightCard);
		} else {
			copyrightCard = new CopyrightCard(mapActivity, metadata);
			cardsContainer.addView(copyrightCard.build(mapActivity));
		}

		View cardBottomSpace = inflate(R.layout.list_item_divider, cardsContainer, true);
		cardBottomSpace.findViewById(R.id.topShadowView).setVisibility(View.INVISIBLE);
	}

	private void reattachCard(@NonNull ViewGroup cardsContainer, @NonNull BaseCard card) {
		ViewGroup oldParent = card.getView() == null ? null : (ViewGroup) card.getView().getParent();
		if (oldParent != null) {
			oldParent.removeAllViews();
		}
		cardsContainer.addView(card.getView());
	}

	@NonNull
	private RouteActivitySelectionHelper getRouteActivitySelectionHelper(@NonNull GpxFile gpxFile) {
		Metadata metadata = gpxFile.getMetadata();
		if (routeActivitySelectionHelper == null) {
			SelectRouteActivityController controller = SelectRouteActivityController.getExistedInstance(app);
			if (controller != null) {
				routeActivitySelectionHelper = controller.getRouteActivityHelper();
			}
			if (routeActivitySelectionHelper == null) {
				routeActivitySelectionHelper = new RouteActivitySelectionHelper();
				RouteActivityHelper helper = app.getRouteActivityHelper();
				List<RouteActivity> activities = helper.getActivities();

				RouteActivity selected = metadata.getRouteActivity(activities);
				if (selected == null) {
					if (isCurrentRecordingTrack()) {
						String activityId = settings.CURRENT_TRACK_ROUTE_ACTIVITY.get();
						selected = helper.findRouteActivity(activityId);
					} else if (routeKey != null) {
						selected = helper.findRouteActivity(routeKey.type.getName());
					}
				}
				routeActivitySelectionHelper.setSelectedActivity(selected);
			}
		}
		routeActivitySelectionHelper.setActivitySelectionListener(this::onRouteActivitySelected);
		return routeActivitySelectionHelper;
	}

	private void onRouteActivitySelected(@Nullable RouteActivity activity) {
		if (isCurrentRecordingTrack()) {
			String activityId = activity != null ? activity.getId() : "";
			settings.CURRENT_TRACK_ROUTE_ACTIVITY.set(activityId);
		} else {
			RouteActivityHelper helper = app.getRouteActivityHelper();
			GpxFile gpxFile = displayHelper.getGpx();
			if (gpxFile != null) {
				helper.saveRouteActivity(gpxFile, activity);
			}
		}
		if (overviewCard != null) {
			overviewCard.setupRouteActivity();
		}
		if (trackTagsInfoCard != null) {
			trackTagsInfoCard.updateContent();
		}
	}

	private void updateCardsLayout() {
		FrameLayout bottomContainer = getBottomContainer();
		if (bottomContainer == null) {
			return;
		}
		int colorId;
		if (menuType == TrackMenuTab.OPTIONS) {
			colorId = ColorUtilities.getListBgColorId(isNightMode());
		} else {
			colorId = ColorUtilities.getActivityBgColorId(isNightMode());
		}
		AndroidUtils.setBackgroundColor(app, bottomContainer, colorId);
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = routeMenuTopShadowAll.getHeight() + bottomNav.getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateStatusBarColor();
		updateToolbar(y, animated);
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateStatusBarColor();
		updateToolbar(posY, true);
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY || menuState == MenuState.HALF_SCREEN;
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int currentMenuState, int previousMenuState) {
		super.onContextMenuStateChanged(fragment, currentMenuState, previousMenuState);

		boolean menuStateChanged = currentMenuState != previousMenuState;
		if (menuStateChanged) {
			updateControlsVisibility(true);
		}
		if ((!isPortrait() || currentMenuState != MenuState.FULL_SCREEN)
				&& (menuStateChanged || adjustMapPosition) && !menuTypeChanged) {
			fitTrackOnMap();
		}
		if (menuStateChanged) {
			menuTypeChanged = false;
		}
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated) {
		super.onContextMenuYPosChanged(fragment, y, needMapAdjust, animated);
		if (animated && menuType == TrackMenuTab.OVERVIEW) {
			if (y != overviewInitialPosY) {
				overviewInitialHeight = false;
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		updateStatusBarColor();
	}

	@Override
	public void onResume() {
		super.onResume();
		onHiddenChanged(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		onHiddenChanged(true);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(hidden ? null : trackChartPoints);
		}
		updateControlsVisibility(!hidden);
		if (hidden) {
			stopLocationUpdate();
		} else {
			startLocationUpdate();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		GpxFile gpxFile = getGpx();
		if (gpxFile != null && FileUtils.isTempFile(app, gpxFile.getPath())) {
			FileUtils.removeGpxFile(app, new File(gpxFile.getPath()));
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(lastLocation, location)) {
			lastLocation = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		app.runInUIThread(this::updateDistanceDirection);
	}

	private void updateDistanceDirection() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && overviewCard != null && overviewCard.getView() != null) {
			View view = overviewCard.getView();
			TextView distanceText = view.findViewById(R.id.distance);
			ImageView direction = view.findViewById(R.id.direction);
			UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, direction, distanceText, latLon);
		}
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
		if (overviewCard != null && menuType == TrackMenuTab.OVERVIEW && isCurrentRecordingTrack()) {
			overviewCard.getBlockStatisticsBuilder().runUpdatingStatBlocksIfNeeded();
		}
		if (pointsCard != null) {
			pointsCard.startListeningLocationUpdates();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
		}
		if (overviewCard != null) {
			overviewCard.getBlockStatisticsBuilder().stopUpdatingStatBlocks();
		}
		if (pointsCard != null) {
			pointsCard.stopListeningLocationUpdates();
		}
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
		updateFile(dest);
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		if (src != null) {
			File file = FileUtils.renameGpxFile(app, src, dest);
			if (file != null) {
				updateFile(file);
			} else {
				app.showToastMessage(R.string.file_can_not_be_renamed);
			}
		}
	}

	private void updateFile(@NonNull File file) {
		displayHelper.setFile(file);
		updateGpxTitle();
		toolbarTextView.setText(gpxTitle);
		updateHeader();
		updateContent();
	}

	public void updateControlsVisibility(boolean menuVisible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			updateDisplayGroupsWidget();
			boolean appbarButtonsVisible = (getCurrentMenuState() != MenuState.FULL_SCREEN && !shouldShowWidgets()) || !isPortrait();
			AndroidUiHelper.updateVisibility(backButtonContainer, appbarButtonsVisible);
			AndroidUiHelper.updateVisibility(displayGroupsWidget, hasPointsGroups() && appbarButtonsVisible);

			boolean topControlsVisible = shouldShowTopControls(menuVisible);
			boolean bottomControlsVisible = shouldShowBottomControls(menuVisible);
			mapActivity.getWidgetsVisibilityHelper().updateControlsVisibility(topControlsVisible, bottomControlsVisible);
			mapActivity.refreshMap();
		}
	}

	public boolean shouldShowTopControls() {
		return shouldShowTopControls(isVisible());
	}

	public boolean shouldShowTopControls(boolean menuVisible) {
		return !menuVisible || shouldShowWidgets();
	}

	public boolean shouldShowWidgets() {
		return Algorithms.isEmpty(returnScreenName)
				&& menuType == TrackMenuTab.OVERVIEW
				&& getCurrentMenuState() == MenuState.HEADER_ONLY;
	}

	public boolean shouldShowBottomControls(boolean menuVisible) {
		return !menuVisible || !isPortrait();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (selectedGpxFile != null) {
			outState.putString(TRACK_FILE_NAME, selectedGpxFile.getGpxFile().getPath());
			outState.putBoolean(CURRENT_RECORDING, selectedGpxFile.isShowCurrentTrack());
		}
		if (latLon != null) {
			outState.putDouble(KEY_LATITUDE, latLon.getLatitude());
			outState.putDouble(KEY_LONGITUDE, latLon.getLongitude());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public int getStatusBarColorId() {
		if (getView() != null && getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
			return isNightMode() ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
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
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		GpxFile gpxFile = getGpx();

		if (card instanceof OptionsCard || card instanceof OverviewCard || card instanceof SegmentsCard) {
			if (buttonIndex == SHOW_ON_MAP_BUTTON_INDEX) {
				if (FileUtils.isTempFile(app, getGpx().getPath())) {
					File srcFile = displayHelper.getFile();
					File destFIle = new File(app.getAppPath(IndexConstants.GPX_TRAVEL_DIR), srcFile.getName());
					onFileMove(srcFile, destFIle);
					gpxFile = getGpx();
				} else {
					GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
					if (!isGpxFileSelected(app, gpxFile)) {
						params.showOnMap().selectedByUser().addToHistory().addToMarkers();
					} else {
						params.hideFromMap();
					}
					selectedGpxFile = gpxSelectionHelper.selectGpxFile(gpxFile, params);
					temporarySelected = false;
				}
				updateContent();
				mapActivity.refreshMap();
			} else if (buttonIndex == APPEARANCE_BUTTON_INDEX) {
				TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile, this);
			} else if (buttonIndex == DIRECTIONS_BUTTON_INDEX) {
				GpxFile gpxFileToDisplay = displayHelper.getGpxFileToDisplay();
				if (gpxFileToDisplay != null) {
					if (TrackSelectSegmentBottomSheet.shouldShowForGpxFile(gpxFileToDisplay)) {
						TrackSelectSegmentBottomSheet.showInstance(fragmentManager, gpxFileToDisplay, this);
					} else {
						mapActivity.getMapActions().startNavigationForGpx(gpxFileToDisplay, mapActivity);
						dismiss();
					}
				}
			}
			if (buttonIndex == JOIN_GAPS_BUTTON_INDEX) {
				displayHelper.setJoinSegments(!displayHelper.isJoinSegments());
				mapActivity.refreshMap();

				if (segmentsCard != null) {
					segmentsCard.updateContent();
				}
			} else if (buttonIndex == ANALYZE_ON_MAP_BUTTON_INDEX) {
				OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(mapActivity, gpxFile, null);
				OsmAndTaskManager.executeTask(detailsTask);
				hide();
			} else if (buttonIndex == ANALYZE_BY_INTERVALS_BUTTON_INDEX) {
				TrkSegment segment = gpxFile.getGeneralSegment();
				if (segment == null) {
					List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
					if (!Algorithms.isEmpty(segments)) {
						segment = segments.get(0);
					}
				}
				GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
				List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
				if (segment != null && !Algorithms.isEmpty(items)) {
					SplitSegmentDialogFragment.showInstance(fragmentManager, displayHelper, items.get(0), segment);
				}
			} else if (buttonIndex == SHARE_BUTTON_INDEX) {
				GpxUiHelper.saveAndShareGpxWithAppearance(app, mapActivity, gpxFile);
			} else if (buttonIndex == UPLOAD_OSM_BUTTON_INDEX) {
				OsmEditingPlugin osmEditingPlugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
				if (osmEditingPlugin != null) {
					File file = new File(gpxFile.getPath());
					osmEditingPlugin.sendGPXFiles(mapActivity, this, file);
				}
			} else if (buttonIndex == EDIT_BUTTON_INDEX) {
				GpxSelectionParams params = GpxSelectionParams.newInstance().showOnMap();
				selectedGpxFile = gpxSelectionHelper.selectGpxFile(gpxFile, params);
				dismiss();
				MeasurementToolFragment.showInstance(fragmentManager, gpxFile.getPath(), false);
			} else if (buttonIndex == RENAME_BUTTON_INDEX) {
				FileUtils.renameFile(mapActivity, new File(gpxFile.getPath()), this, true);
			} else if (buttonIndex == CHANGE_FOLDER_BUTTON_INDEX) {
				File file = new File(gpxFile.getPath());
				MoveGpxFileBottomSheet.showInstance(fragmentManager, file, file.getParentFile(), this, true, false);
			} else if (buttonIndex == GPS_FILTER_BUTTON_INDEX) {
				GpsFilterFragment.showInstance(fragmentManager, selectedGpxFile, this);
			} else if (buttonIndex == ALTITUDE_CORRECTION_BUTTON_INDEX) {
				GpxTrackAnalysis analysis = this.analysis != null
						? this.analysis
						: selectedGpxFile.getTrackAnalysis(app);
				if (analysis.hasElevationData()) {
					SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
					if (plugin != null && plugin.is3DReliefAllowed()) {
						calculateOfflineSelected(-1);
					} else {
						calculateOnlineSelected(-1);
					}
				} else {
					showTrackAltitudeDialog(-1);
				}
			} else if (buttonIndex == SIMULATE_POSITION_BUTTON_INDEX) {
				SimulateLocationFragment.showInstance(fragmentManager, gpxFile, true);
			} else if (buttonIndex == DELETE_BUTTON_INDEX) {
				String fileName = Algorithms.getFileWithoutDirs(gpxFile.getPath());

				AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, isNightMode()));
				builder.setTitle(getString(R.string.delete_confirmation_msg, fileName));
				builder.setMessage(R.string.are_you_sure);
				String gpxFilePath = gpxFile.getPath();
				builder.setNegativeButton(R.string.shared_string_cancel, null)
						.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
							if (FileUtils.removeGpxFile(app, new File(gpxFilePath))) {
								dismiss();
							}
						});
				builder.show();
			}
		} else if (card instanceof TrackPointsCard) {
			if (buttonIndex == ADD_WAYPOINT_INDEX) {
				PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, app.getString(R.string.add_waypoint));
				QuadRect rect = displayHelper.getRect();
				NewGpxPoint newGpxPoint = new NewGpxPoint(gpxFile, pointDescription, rect);

				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						(int) rect.width(), (int) rect.height(), 0);
				mapActivity.getMapLayers().getContextMenuLayer().enterAddGpxPointMode(newGpxPoint);

				hide();
			} else if (buttonIndex == DELETE_WAYPOINTS_INDEX) {
				TrackPointsCard pointsCard = (TrackPointsCard) card;
				if (pointsCard.isSelectionMode()) {
					pointsCard.deleteItemsAction();
				} else {
					pointsCard.setSelectionMode(true);
				}
			} else if (buttonIndex == OPEN_WAYPOINT_INDEX) {
				dismiss();
			}
		} else if (card instanceof PointsGroupsCard) {
			PointsGroupsCard groupsCard = (PointsGroupsCard) card;
			GpxDisplayGroup group = groupsCard.getSelectedGroup();
			if (pointsCard != null) {
				pointsCard.setSelectedGroup(group);
				if (group != null) {
					fitSelectedPointsGroupOnMap(group);
				} else {
					fitTrackOnMap();
				}
			}
		}
	}

	private void fitTrackOnMap() {
		GpxFile gpxFile = displayHelper.getGpxFileToDisplay();
		if (gpxFile != null) {
			KQuadRect rect = gpxFile.getRect();
			adjustMapPosition(rect);
		}
	}

	private void fitSelectedPointsGroupOnMap(GpxDisplayGroup group) {
		DisplayGroupsHolder groupsHolder = getDisplayGroupsHolder();
		List<GpxDisplayItem> points = groupsHolder.getItemsByGroupName(group.getName());
		if (points != null) {
			KQuadRect pointsRect = new KQuadRect();
			for (GpxDisplayItem point : points) {
				GpxUtilities.INSTANCE.updateQR(pointsRect, point.locationStart, 0, 0);
			}
			adjustMapPosition(pointsRect);
		}
	}

	@NonNull
	private DisplayGroupsHolder getDisplayGroupsHolder() {
		return DisplayPointsGroupsHelper.getGroups(app, displayHelper.getPointsOriginalGroups(), null);
	}

	public void updateToolbar(int y, boolean animated) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || toolbarContainer == null) {
			return;
		}
		if (isPortrait()) {
			if (animated) {
				float toolbarAlpha = getToolbarAlpha(y);
				if (toolbarAlpha > 0) {
					updateVisibility(toolbarContainer, true);
				}
				toolbarContainer.animate().alpha(toolbarAlpha)
						.setDuration(ContextMenuFragment.ANIMATION_DURATION)
						.setInterpolator(new DecelerateInterpolator())
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								updateVisibility(toolbarContainer, toolbarAlpha);
								mapActivity.updateStatusBarColor();
							}
						})
						.start();
			} else {
				updateToolbarVisibility(toolbarContainer, y);
				mapActivity.updateStatusBarColor();
			}
		} else {
			updateVisibility(toolbarContainer, false);
		}
	}

	@Override
	protected void onHeaderClick() {
		if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
			updateMenuState();
		}
	}

	private void adjustMapPosition(KQuadRect r) {
		int y = getMenuStatePosY(getCurrentMenuState());
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandMapTileView tileView = mapActivity.getMapView();
			RotatedTileBox tb = tileView.getRotatedTileBox();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			int marginStartPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
				marginStartPx = getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(mapActivity);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}

			boolean contains = tileView.fullyContains(tb, r.getLeft(), r.getTop(), r.getRight(), r.getBottom());
			if (!contains && r.getLeft() != 0 && r.getRight() != 0) {
				tileView.fitRectToMap(r.getLeft(), r.getRight(), r.getTop(), r.getBottom(),
						tileBoxWidthPx, tileBoxHeightPx, 0, marginStartPx);
			}
			adjustMapPosition = false;
		}
	}

	private void setupButtons(View view) {
		ColorStateList navColorStateList = AndroidUtils.createBottomNavColorStateList(getContext(), isNightMode());
		BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
		bottomNav.setItemIconTintList(navColorStateList);
		bottomNav.setItemTextColor(navColorStateList);
		bottomNav.setSelectedItemId(menuType.menuItemId);
		bottomNav.setOnItemSelectedListener(item -> {
			for (TrackMenuTab type : TrackMenuTab.values()) {
				if (type.menuItemId == item.getItemId()) {
					TrackMenuTab prevMenuType = menuType;
					menuType = type;
					menuTypeChanged = prevMenuType != type;
					setupCards(true);
					updateHeader();
					updateHeadersBottomShadow();
					updateControlsVisibility(isVisible());
					updateCardsLayout();
					if (type == TrackMenuTab.OVERVIEW && isPortrait() && overviewInitialHeight
							&& getCurrentMenuState() != MenuState.FULL_SCREEN) {
						calculateLayoutAndShowOverview();
					} else {
						calculateLayoutAndUpdateMenuState(prevMenuType);
					}
					break;
				}
			}
			return true;
		});
	}

	private void calculateLayoutAndUpdateMenuState(@Nullable TrackMenuTab prevMenuType) {
		if (getCurrentMenuState() == 2 && overviewInitialHeight && prevMenuType == TrackMenuTab.OVERVIEW) {
			slideDown();
		}
		runLayoutListener(() -> {
			if (getCurrentMenuState() != MenuState.FULL_SCREEN) {
				updateMenuState();
			}
		});
	}

	private void calculateLayoutAndShowOverview() {
		runLayoutListener(() -> {
			if (overviewInitialPosY == 0) {
				int overviewCardHeight = overviewCard != null ? overviewCard.getViewHeight() : 0;
				overviewInitialPosY = getViewHeight() - overviewCardHeight - menuTitleHeight - getShadowHeight();
			}
			if (overviewInitialPosY < getViewY()) {
				updateMainViewLayout(overviewInitialPosY);
			}
			animateMainView(overviewInitialPosY, false, getCurrentMenuState(), getCurrentMenuState());
			updateMapControlsPos(this, overviewInitialPosY, true);
		});
	}

	private void updateMenuState() {
		if (menuType == TrackMenuTab.OPTIONS) {
			openMenuFullScreen();
		} else {
			openMenuHalfScreen();
		}
	}

	@Override
	public void updateContent() {
		if (overviewCard != null) {
			overviewCard.updateContent();
		}
		if (segmentsCard != null) {
			segmentsCard.updateContent();
		}
		if (optionsCard != null) {
			optionsCard.updateContent();
		}
		if (descriptionCard != null) {
			descriptionCard.updateContent();
		}
		if (gpxInfoCard != null) {
			gpxInfoCard.updateContent();
		}
		if (pointsCard != null) {
			pointsCard.updateContent();
		}
		if (authorCard != null) {
			authorCard.updateContent();
		}
		if (copyrightCard != null) {
			copyrightCard.updateContent();
		}
		updatePointGroupsCard();
		setupCards(true);
		updateDisplayGroupsWidget();
	}

	private void updatePointGroupsCard() {
		if (groupsCard != null) {
			groupsCard.updateContent(pointsCard.getGroups());
		}
	}

	@Override
	public void onChartTouch() {
		if (segmentsCard != null) {
			segmentsCard.disallowScrollOnChartTouch();
		}
	}

	@Override
	public void scrollBy(int px) {
	}

	@Override
	public void onPointsDeleted() {
		if (pointsCard != null) {
			pointsCard.onPointsDeleted();
		}
	}

	@Override
	public void onPointSelected(TrkSegment segment, double lat, double lon) {
		if (trackChartPoints == null) {
			trackChartPoints = new TrackChartPoints();
			trackChartPoints.setGpx(getGpx());
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int segmentColor = segment != null ? segment.getColor(0) : 0;
			trackChartPoints.setSegmentColor(segmentColor);
			trackChartPoints.setHighlightedPoint(new LatLon(lat, lon));
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
			mapActivity.refreshMap();
		}
	}

	@Override
	public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null && displayHelper != null) {
			SplitSegmentDialogFragment.showInstance(fragmentManager, displayHelper, gpxItem, trkSegment);
		}
	}

	@Override
	public void openAnalyzeOnMap(@NonNull GpxDisplayItem gpxItem) {
		if (gpxPoint != null) {
			gpxItem.locationOnMap = gpxPoint.getSelectedPoint();
		}
		TrackDetailsMenu trackDetailsMenu = getMapActivity().getTrackDetailsMenu();
		trackDetailsMenu.setGpxItem(gpxItem);
		trackDetailsMenu.setSelectedGpxFile(selectedGpxFile);
		trackDetailsMenu.show();
		hide();
	}

	@Override
	public void openGetAltitudeBottomSheet(@NonNull GpxDisplayItem gpxItem) {
		showTrackAltitudeDialog(getSegmentIndex(gpxItem));
	}

	private void showTrackAltitudeDialog(int segmentIndex) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TrackAltitudeBottomSheet.showInstance(activity.getSupportFragmentManager(), this, segmentIndex);
		}
	}

	private int getSegmentIndex(@NonNull GpxDisplayItem gpxItem) {
		GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
		List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
		int segmentIndex = items.indexOf(gpxItem);
		if (segmentIndex == 0 && getGpx().hasGeneralTrack()) {
			segmentIndex = -1;
		}
		return segmentIndex;
	}

	@Override
	public void showOptionsPopupMenu(View view, TrkSegment segment, boolean confirmDeletion, GpxDisplayItem gpxItem) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			IconPopupMenu optionsPopupMenu = new IconPopupMenu(activity, view.findViewById(R.id.overflow_menu));
			Menu menu = optionsPopupMenu.getMenu();
			optionsPopupMenu.getMenuInflater().inflate(R.menu.track_segment_menu, menu);
			menu.findItem(R.id.action_edit).setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_edit_dark));
			menu.findItem(R.id.action_delete).setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_remove_dark));
			if (getGpx().isShowCurrentTrack()) {
				menu.findItem(R.id.split_interval).setVisible(false);
			} else {
				menu.findItem(R.id.split_interval).setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_split_interval));
			}
			optionsPopupMenu.setOnMenuItemClickListener(item -> {
				int i = item.getItemId();
				if (i == R.id.action_edit) {
					int segmentIndex = getSegmentIndex(gpxItem);
					openPlanRoute(segmentIndex, PLAN_ROUTE_MODE);
					return true;
				} else if (i == R.id.action_delete) {
					FragmentActivity fragmentActivity = getActivity();
					if (!confirmDeletion) {
						deleteAndSaveSegment(segment);
					} else if (AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
						AlertDialog.Builder builder = new AlertDialog.Builder(fragmentActivity);
						builder.setMessage(getString(R.string.delete_confirmation_msg, gpxItem.trackSegmentName));
						builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteAndSaveSegment(segment));
						builder.setNegativeButton(R.string.shared_string_cancel, null);
						builder.show();
					}
					return true;
				} else if (i == R.id.split_interval) {
					openSplitInterval(gpxItem, segment);
				}
				return false;
			});
			optionsPopupMenu.show();
		}
	}

	@Override
	public void onSegmentSelect(@NonNull GpxFile gpxFile, int selectedSegment) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForSegment(gpxFile, selectedSegment, mapActivity);
			dismiss();
		}
	}

	@Override
	public void onRouteSelected(@NonNull GpxFile gpxFile, int selectedRoute) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForRoute(gpxFile, selectedRoute, mapActivity);
			dismiss();
		}
	}

	public void openPlanRoute(int segmentIndex, @MeasurementToolMode int mode) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			GpxFile gpxFile = getGpx();
			MeasurementToolFragment.showInstance(activity, gpxFile, segmentIndex, mode);
		}
		hide();
	}

	private void deleteAndSaveSegment(TrkSegment segment) {
		if (deleteSegment(segment)) {
			GpxFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				boolean showOnMap = GpxSelectionHelper.isGpxFileSelected(app, gpx);
				GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
				if (showOnMap) {
					params.showOnMap().selectedByUser().addToMarkers().addToHistory();
				} else {
					params.hideFromMap();
				}
				selectedGpxFile = gpxSelectionHelper.selectGpxFile(gpx, params);
				saveGpx(gpx, errorMessage -> {
					SelectedGpxFile selectedGpxFile = showOnMap ? this.selectedGpxFile : null;
					if (selectedGpxFile != null) {
						List<GpxDisplayGroup> groups = displayHelper.getDisplayGroups(
								new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT});
						selectedGpxFile.setSplitGroups(groups, app);
						selectedGpxFile.processPoints(app);
					}
					updateContent();
				});
			}
		}
	}

	private boolean deleteSegment(TrkSegment segment) {
		if (segment != null) {
			GpxFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				return gpx.removeTrkSegment(segment);
			}
		}
		return false;
	}

	private void saveGpx(GpxFile gpxFile, SaveGpxListener listener) {
		SaveGpxHelper.saveGpx(new File(gpxFile.getPath()), gpxFile, listener);
	}

	private boolean isCurrentRecordingTrack() {
		return app.getSavingTrackHelper().getCurrentTrack() == selectedGpxFile;
	}

	private void hide() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.hide(this)
					.commitAllowingStateLoss();
		}
	}

	public void show() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.show(this)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void attachToRoadsSelected(int segmentIndex) {
		openPlanRoute(segmentIndex, ATTACH_ROADS_MODE);
	}

	@Override
	public void calculateOnlineSelected(int segmentIndex) {
		openPlanRoute(segmentIndex, CALCULATE_SRTM_MODE);
	}

	@Override
	public void calculateOfflineSelected(int segmentIndex) {
		openPlanRoute(segmentIndex, CALCULATE_HEIGHTMAP_MODE);
	}

	@Override
	public void onFinishFiltering(@NonNull GpxFile filteredGpxFile) {
		displayHelper.setFilteredGpxFile(filteredGpxFile);
		updateContent();
	}

	@Override
	public void onDismissGpsFilterFragment(boolean savedCopy, @Nullable String savedFilePath) {
		if (savedCopy) {
			dismiss();
		}
	}

	@Override
	public boolean onSaveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			EditGpxDescriptionController controller = new EditGpxDescriptionController(getMapActivity());
			controller.saveEditedDescription(editedText, callback);
			return true;
		}
		return false;
	}


	public static void openTrack(@NonNull Context context, @Nullable File file, @Nullable Bundle prevIntentParams) {
		openTrack(context, file, prevIntentParams, null);
	}

	public static void openTrack(@NonNull Context context, @Nullable File file, @Nullable Bundle prevIntentParams,
	                             @Nullable String returnScreenName) {
		openTrack(context, file, prevIntentParams, returnScreenName, TrackMenuTab.OVERVIEW, false);
	}

	public static void openTrack(@NonNull Context context, @Nullable File file, @Nullable Bundle prevIntentParams,
	                             @Nullable String returnScreenName, @NonNull TrackMenuTab tabToOpen, boolean temporarySelected) {
		boolean currentRecording = file == null;
		String path = file != null ? file.getAbsolutePath() : null;
		if (context instanceof MapActivity) {
			showInstance((MapActivity) context, path, currentRecording,
					temporarySelected, returnScreenName, null, tabToOpen.name());
		} else {
			Bundle bundle = new Bundle();
			bundle.putString(TRACK_FILE_NAME, path);
			bundle.putBoolean(OPEN_TRACK_MENU, true);
			bundle.putBoolean(TEMPORARY_SELECTED, temporarySelected);
			bundle.putBoolean(CURRENT_RECORDING, currentRecording);
			bundle.putString(RETURN_SCREEN_NAME, returnScreenName);
			bundle.putString(OPEN_TAB_NAME, tabToOpen.name());
			MapActivity.launchMapActivityMoveToTop(context, prevIntentParams, null, bundle);
		}
	}

	public static void loadSelectedGpxFile(@NonNull MapActivity mapActivity, @Nullable String path,
	                                       boolean showCurrentTrack,
	                                       @NonNull CallbackWithObject<SelectedGpxFile> callback) {
		OsmandApplication app = mapActivity.getApp();
		SelectedGpxFile selectedGpxFile;
		if (showCurrentTrack) {
			selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		} else {
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(path);
		}
		if (selectedGpxFile != null) {
			callback.processResult(selectedGpxFile);
		} else if (!Algorithms.isEmpty(path)) {
			GpxFileLoaderTask.loadGpxFile(new File(path), mapActivity, gpx -> {
				GpxSelectionParams params = GpxSelectionParams.newInstance().showOnMap()
						.syncGroup().selectedByUser().addToHistory().addToMarkers().saveSelection();
				SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(gpx, params);
				if (sf != null) {
					callback.processResult(sf);
				}
				return true;
			});
		}
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @Nullable String path,
	                                boolean showCurrentTrack,
	                                boolean temporarySelected,
	                                @Nullable String returnScreenName,
	                                @Nullable String callingFragmentTag,
	                                @Nullable String tabToOpenName) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		loadSelectedGpxFile(mapActivity, path, showCurrentTrack, selectedGpxFile -> {
			MapActivity activity = mapActivityRef.get();
			if (activity != null && selectedGpxFile != null) {
				Bundle params = new Bundle();
				params.putString(RETURN_SCREEN_NAME, returnScreenName);
				params.putString(CALLING_FRAGMENT_TAG, callingFragmentTag);
				params.putString(OPEN_TAB_NAME, tabToOpenName);
				params.putBoolean(TEMPORARY_SELECTED, temporarySelected);
				showInstance(activity, selectedGpxFile, null, null, null, params);
			}
			return true;
		});
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @Nullable SelectedGpxPoint gpxPoint) {
		Bundle params = new Bundle();
		params.putBoolean(ADJUST_MAP_POSITION, false);
		return showInstance(mapActivity, selectedGpxFile, gpxPoint, null, null, params);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @Nullable SelectedGpxPoint gpxPoint,
	                                   @Nullable GpxTrackAnalysis analyses,
	                                   @Nullable RouteKey routeKey,
	                                   @Nullable Bundle params) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, MenuState.HEADER_ONLY);

			TrackMenuFragment fragment = new TrackMenuFragment();
			fragment.setArguments(args);
			fragment.setRetainInstance(true);
			fragment.setAnalysis(analyses);
			fragment.setSelectedGpxFile(selectedGpxFile);
			routeKey = routeKey == null ? RouteKey.fromGpxFile(selectedGpxFile.getGpxFile()) : routeKey;
			fragment.setRouteKey(routeKey);

			if (params != null) {
				fragment.setReturnScreenName(params.getString(RETURN_SCREEN_NAME, null));
				fragment.setTemporarySelected(params.getBoolean(TEMPORARY_SELECTED, false));
				fragment.setCallingFragmentTag(params.getString(CALLING_FRAGMENT_TAG, null));
				String tabToOpenName = params.getString(OPEN_TAB_NAME, null);
				if (!Algorithms.isEmpty(tabToOpenName)) {
					fragment.setMenuType(TrackMenuTab.valueOf(tabToOpenName));
				}
				fragment.setAdjustMapPosition(params.getBoolean(ADJUST_MAP_POSITION, true));
				String chartTabToOpenName = params.getString(CHART_TAB_NAME);
				if (!Algorithms.isEmpty(chartTabToOpenName)) {
					fragment.setChartTabToOpen(GPXTabItemType.valueOf(chartTabToOpenName));
				}
			}

			if (gpxPoint != null) {
				WptPt wptPt = gpxPoint.getSelectedPoint();
				fragment.setLatLon(new LatLon(wptPt.getLat(), wptPt.getLon()));
				fragment.setGpxPoint(gpxPoint);
			} else {
				KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
				LatLon latLonRect = new LatLon(rect.centerY(), rect.centerX());
				fragment.setLatLon(latLonRect);
			}

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}