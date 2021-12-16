package net.osmand.plus.track.fragments;

import static net.osmand.GPXUtilities.GPXTrackAnalysis;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.isGpxFileSelected;
import static net.osmand.plus.activities.MapActivityActions.KEY_LATITUDE;
import static net.osmand.plus.activities.MapActivityActions.KEY_LONGITUDE;
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
import static net.osmand.plus.track.cards.OptionsCard.UPLOAD_OSM_BUTTON_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.ADD_WAYPOINT_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.DELETE_WAYPOINTS_INDEX;
import static net.osmand.plus.track.cards.TrackPointsCard.OPEN_WAYPOINT_INDEX;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.OpenGpxDetailsTask;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SplitSegmentDialogFragment;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.track.fragments.DisplayGroupsBottomSheet.DisplayPointGroupsCallback;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper.DisplayGroupsHolder;
import net.osmand.plus.track.fragments.GpsFilterFragment.GpsFilterFragmentLister;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.cards.DescriptionCard;
import net.osmand.plus.track.cards.GpxInfoCard;
import net.osmand.plus.track.cards.OptionsCard;
import net.osmand.plus.track.cards.OverviewCard;
import net.osmand.plus.track.cards.PointsGroupsCard;
import net.osmand.plus.track.cards.SegmentsCard;
import net.osmand.plus.track.cards.TrackPointsCard;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.widgets.IconPopupMenu;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class TrackMenuFragment extends ContextMenuScrollFragment implements CardListener,
		SegmentActionsListener, RenameCallback, OnTrackFileMoveListener, OnPointsDeleteListener,
		OsmAndLocationListener, OsmAndCompassListener, OnSegmentSelectedListener, GpsFilterFragmentLister,
		DisplayPointGroupsCallback {

	public static final String TRACK_FILE_NAME = "TRACK_FILE_NAME";
	public static final String OPEN_TAB_NAME = "open_tab_name";
	public static final String CURRENT_RECORDING = "CURRENT_RECORDING";
	public static final String SHOW_TEMPORARILY = "SHOW_TEMPORARILY";
	public static final String OPEN_TRACK_MENU = "open_track_menu";
	public static final String RETURN_SCREEN_NAME = "return_screen_name";
	public static final String TRACK_DELETED_KEY = "track_deleted_key";

	public static final String TAG = TrackMenuFragment.class.getName();
	private static final Log log = PlatformUtil.getLog(TrackMenuFragment.class);

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SelectedGpxFile selectedGpxFile;
	private GPXTrackAnalysis analyses;

	private TrackMenuType menuType = TrackMenuType.OVERVIEW;
	private SegmentsCard segmentsCard;
	private OptionsCard optionsCard;
	private DescriptionCard descriptionCard;
	private GpxInfoCard gpxInfoCard;
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
	private SelectedGpxPoint gpxPoint;
	private TrackChartPoints trackChartPoints;

	private Float heading;
	private Location lastLocation;
	private UpdateLocationViewCache updateLocationViewCache;
	private boolean locationUpdateStarted;
	private LatLon latLon;

	private int menuTitleHeight;
	private int toolbarHeightPx;
	private boolean adjustMapPosition = true;
	private boolean menuTypeChanged = false;
	private boolean overviewInitialHeight = true;
	private int overviewInitialPosY;

	public enum TrackMenuType {
		OVERVIEW(R.id.action_overview, R.string.shared_string_overview),
		TRACK(R.id.action_track, R.string.shared_string_gpx_tracks),
		POINTS(R.id.action_points, R.string.shared_string_gpx_points),
		OPTIONS(R.id.action_options, R.string.shared_string_options);

		TrackMenuType(int menuItemId, @StringRes int titleId) {
			this.menuItemId = menuItemId;
			this.titleId = titleId;
		}

		public final int menuItemId;
		public final int titleId;
	}

	public void setMenuType(TrackMenuType menuType) {
		this.menuType = menuType;
	}

	@Override
	public int getMainLayoutId() {
		return R.layout.track_menu;
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
	public SelectedGpxFile getSelectedGpx() {
		return selectedGpxFile;
	}

	@Override
	public TrackDisplayHelper getDisplayHelper() {
		return displayHelper;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		displayHelper = new TrackDisplayHelper(app);
		updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();
		toolbarHeightPx = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		if (selectedGpxFile == null && savedInstanceState != null) {
			String path = savedInstanceState.getString(TRACK_FILE_NAME);
			boolean showCurrentTrack = savedInstanceState.getBoolean(CURRENT_RECORDING);
			MapActivity mapActivity = requireMapActivity();
			loadSelectedGpxFile(mapActivity, path, showCurrentTrack, new CallbackWithObject<SelectedGpxFile>() {
				@Override
				public boolean processResult(SelectedGpxFile result) {
					setSelectedGpxFile(result);
					onSelectedGpxFileAvailable();
					if (getView() != null) {
						initContent(getView());
					}
					return true;
				}
			});
			if (savedInstanceState.containsKey(KEY_LATITUDE) && savedInstanceState.containsKey(KEY_LONGITUDE)) {
				double latitude = savedInstanceState.getDouble(KEY_LATITUDE);
				double longitude = savedInstanceState.getDouble(KEY_LONGITUDE);
				latLon = new LatLon(latitude, longitude);
			}
		} else if (selectedGpxFile != null) {
			onSelectedGpxFileAvailable();
			if (FileUtils.isTempFile(app, getGpx().path)) {
				app.getSelectedGpxHelper().selectGpxFile(selectedGpxFile.getGpxFile(), true, false);
			}
		}

		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				if (getCurrentMenuState() != MenuState.HEADER_ONLY && isPortrait()) {
					openMenuHeaderOnly();
				} else {
					dismiss();
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						MapContextMenu contextMenu = mapActivity.getContextMenu();
						PointDescription pointDescription = contextMenu.getPointDescription();
						if (pointDescription != null && pointDescription.isGpxPoint()) {
							contextMenu.init(contextMenu.getLatLon(), pointDescription, contextMenu.getObject());
							contextMenu.show();
						} else if (Algorithms.objectEquals(callingFragmentTag, QuickSearchDialogFragment.TAG)) {
							mapActivity.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
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
			File file = new File(selectedGpxFile.getGpxFile().path);
			displayHelper.setFile(file);
			displayHelper.setGpxDataItem(app.getGpxDbHelper().getItem(file));
		}
		displayHelper.setGpx(selectedGpxFile.getGpxFile());
		if (selectedGpxFile.getFilteredSelectedGpxFile() != null) {
			displayHelper.setFilteredGpxFile(selectedGpxFile.getFilteredSelectedGpxFile().getGpxFile());
		}
	}

	private void updateGpxTitle() {
		if (isCurrentRecordingTrack()) {
			gpxTitle = app.getString(R.string.shared_string_currently_recording_track);
		} else if (!Algorithms.isBlank(getGpx().getArticleTitle())) {
			gpxTitle = getGpx().getArticleTitle();
		} else {
			gpxTitle = GpxUiHelper.getGpxTitle(Algorithms.getFileWithoutDirs(getGpx().path));
		}
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public GPXFile getGpx() {
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

	public void setGpxPoint(SelectedGpxPoint point) {
		this.gpxPoint = point;
	}

	public void setAdjustMapPosition(boolean adjustMapPosition) {
		this.adjustMapPosition = adjustMapPosition;
	}

	private void setAnalyses(@Nullable GPXTrackAnalysis analyses) {
		this.analyses = analyses;
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

	private void shiftMapControls(int viewWidth) {
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(getContext());
		int shiftPosition = viewWidth;
		int start = isLayoutRtl ? 0 : shiftPosition;
		int end = isLayoutRtl ? shiftPosition : 0;
		AndroidUtils.setMargins((MarginLayoutParams) backButtonContainer.getLayoutParams(), start, 0, end, 0);
		AndroidUtils.setMargins((MarginLayoutParams) displayGroupsWidget.getLayoutParams(), start, 0, end, 0);
	}

	private void initContent(@NonNull View view) {
		setupCards();
		setupToolbar();
		updateHeader();
		updateHeadersBottomShadow();
		setupButtons(view);
		updateCardsLayout();
		if (menuType == TrackMenuType.OVERVIEW && isPortrait()) {
			calculateLayoutAndShowOverview();
		} else {
			calculateLayoutAndUpdateMenuState(null);
		}
	}

	private void updateHeader() {
		updateHeaderCard();
		headerTitle.setText(getHeaderTitle());

		if (menuType == TrackMenuType.POINTS) {
			AndroidUiHelper.updateVisibility(searchButton, true);
		} else {
			AndroidUiHelper.updateVisibility(toolbarTextView, true);
			AndroidUiHelper.updateVisibility(searchButton, false);
			AndroidUiHelper.updateVisibility(searchContainer, false);
		}
		AndroidUiHelper.updateVisibility(displayGroupsButton, hasPointsGroups());
		AndroidUiHelper.updateVisibility(headerIcon, menuType != TrackMenuType.OPTIONS);
	}

	@NonNull
	private CharSequence getHeaderTitle() {
		if (menuType == TrackMenuType.TRACK) {
			String title = app.getString(R.string.shared_string_gpx_track) + "\n" + gpxTitle;
			return UiUtilities.createCustomFontSpannable(FontCache.getRobotoRegular(app), title, gpxTitle);
		} else if (menuType == TrackMenuType.OPTIONS) {
			return app.getString(menuType.titleId);
		} else {
			return gpxTitle;
		}
	}

	private void updateHeadersBottomShadow() {
		View scrollView = getBottomScrollView();
		if (menuType == TrackMenuType.OVERVIEW) {
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
			if (menuType == TrackMenuType.TRACK) {
				updateBottomHeaderShadowVisibility(segmentsCard.isScrollToTopAvailable());
				segmentsCard.setScrollAvailabilityListener(this::updateBottomHeaderShadowVisibility);
			} else {
				segmentsCard.removeScrollAvailabilityListener();
			}
		}

		if (menuType != TrackMenuType.OVERVIEW && menuType != TrackMenuType.TRACK) {
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
		if (menuType == TrackMenuType.POINTS && !Algorithms.isEmpty(pointsCard.getGroups())) {
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
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (menuType == TrackMenuType.POINTS) {
					AndroidUiHelper.updateVisibility(toolbarTextView, true);
					AndroidUiHelper.updateVisibility(searchButton, true);
					AndroidUiHelper.updateVisibility(displayGroupsButton, hasPointsGroups());
					AndroidUiHelper.updateVisibility(searchContainer, false);
				}
				openMenuHeaderOnly();
			}
		});
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(toolbarContainer.getContext()));

		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AndroidUiHelper.updateVisibility(searchContainer, true);
				AndroidUiHelper.updateVisibility(searchButton, false);
				AndroidUiHelper.updateVisibility(displayGroupsButton, false);
				AndroidUiHelper.updateVisibility(toolbarTextView, false);
			}
		});
		searchEditText = toolbarContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_poi_filter);
		searchEditText.addTextChangedListener(
				new TextWatcher() {

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						if (pointsCard != null) {
							pointsCard.filter(s.toString());
						}
					}
				}
		);
		ImageView clearButton = toolbarContainer.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!Algorithms.isEmpty(searchEditText.getText())) {
					searchEditText.setText("");
					searchEditText.setSelection(0);
				}
				if (pointsCard != null) {
					pointsCard.updateContent();
				}
			}
		});
		backButtonContainer.setOnClickListener(v -> {
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
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				DisplayGroupsBottomSheet.showInstance(mapActivity, TrackMenuFragment.this, true);
			}
		};
		displayGroupsButton.setOnClickListener(listener);
		displayGroupsWidget.setOnClickListener(listener);
		updateDisplayGroupsWidget();
	}

	public void updateDisplayGroupsWidget() {
		boolean widgetVisible = hasPointsGroups() && !shouldShowWidgets();
		if (widgetVisible) {
			DisplayGroupsHolder displayGroupsHolder =
					DisplayPointsGroupsHelper.getGroups(app, displayHelper.getPointsOriginalGroups(), null);
			int total = displayGroupsHolder.groups.size();
			int hidden = selectedGpxFile.getHiddenGroups().size();
			int visible = total - hidden;
			TextView indication = displayGroupsWidget.findViewById(R.id.visible_display_groups_size);
			indication.setText(getString(
					R.string.ltr_or_rtl_combine_via_slash,
					String.valueOf(visible),
					String.valueOf(total)
			));
		}
		AndroidUiHelper.updateVisibility(displayGroupsWidget, widgetVisible);
	}

	private boolean hasPointsGroups() {
		return displayHelper.getPointsOriginalGroups().size() > 0;
	}

	@Override
	public void onPointGroupsVisibilityChanged() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app.getSelectedGpxHelper().updateSelectedGpxFile(selectedGpxFile);
			mapActivity.refreshMap();
			updateDisplayGroupsWidget();
			updatePointGroupsCard();
		}
	}

	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();
			if (menuType == TrackMenuType.TRACK) {
				if (segmentsCard != null && segmentsCard.getView() != null) {
					reattachCard(cardsContainer, segmentsCard);
				} else {
					segmentsCard = new SegmentsCard(mapActivity, displayHelper, gpxPoint, this);
					segmentsCard.setListener(this);
					cardsContainer.addView(segmentsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuType.OPTIONS) {
				if (optionsCard != null && optionsCard.getView() != null) {
					reattachCard(cardsContainer, optionsCard);
				} else {
					optionsCard = new OptionsCard(mapActivity, displayHelper, selectedGpxFile);
					optionsCard.setListener(this);
					cardsContainer.addView(optionsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuType.OVERVIEW) {
				if (overviewCard != null && overviewCard.getView() != null) {
					reattachCard(cardsContainer, overviewCard);
				} else {
					overviewCard = new OverviewCard(mapActivity, this, selectedGpxFile, analyses, this);
					overviewCard.setListener(this);
					cardsContainer.addView(overviewCard.build(mapActivity));
					if (isCurrentRecordingTrack()) {
						overviewCard.getBlockStatisticsBuilder().runUpdatingStatBlocksIfNeeded();
					}
				}

				if (descriptionCard != null && descriptionCard.getView() != null) {
					reattachCard(cardsContainer, descriptionCard);
				} else {
					descriptionCard = new DescriptionCard(getMapActivity(), this, displayHelper.getGpx());
					cardsContainer.addView(descriptionCard.build(mapActivity));
				}
				if (gpxInfoCard != null && gpxInfoCard.getView() != null) {
					reattachCard(cardsContainer, gpxInfoCard);
				} else {
					gpxInfoCard = new GpxInfoCard(getMapActivity(), displayHelper.getGpx());
					cardsContainer.addView(gpxInfoCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuType.POINTS) {
				if (pointsCard != null && pointsCard.getView() != null) {
					reattachCard(cardsContainer, pointsCard);
				} else {
					pointsCard = new TrackPointsCard(mapActivity, displayHelper, selectedGpxFile);
					pointsCard.setListener(this);
					cardsContainer.addView(pointsCard.build(mapActivity));
				}
			}
		}
	}

	private void reattachCard(@NonNull ViewGroup cardsContainer, @NonNull BaseCard card) {
		ViewGroup oldParent = card.getView() == null ? null : (ViewGroup) card.getView().getParent();
		if (oldParent != null) {
			oldParent.removeAllViews();
		}
		cardsContainer.addView(card.getView());
	}

	private void updateCardsLayout() {
		FrameLayout bottomContainer = getBottomContainer();
		if (bottomContainer == null) {
			return;
		}
		int colorId;
		if (menuType == TrackMenuType.OPTIONS) {
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
		if (currentMenuState != MenuState.FULL_SCREEN 
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
		if (animated && menuType == TrackMenuType.OVERVIEW) {
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && trackChartPoints != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		}
		updateControlsVisibility(true);
		startLocationUpdate();
		if (overviewCard != null && menuType == TrackMenuType.OVERVIEW && isCurrentRecordingTrack()) {
			overviewCard.getBlockStatisticsBuilder().runUpdatingStatBlocksIfNeeded();
		}
		if (pointsCard != null) {
			pointsCard.startListeningLocationUpdates();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
		}
		updateControlsVisibility(false);
		stopLocationUpdate();
		if (overviewCard != null) {
			overviewCard.getBlockStatisticsBuilder().stopUpdatingStatBlocks();
		}
		if (pointsCard != null) {
			pointsCard.stopListeningLocationUpdates();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		GPXFile gpxFile = getGpx();
		if (gpxFile != null && FileUtils.isTempFile(app, gpxFile.path)) {
			FileUtils.removeGpxFile(app, new File(gpxFile.path));
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
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateDistanceDirection();
			}
		});
	}

	private void updateDistanceDirection() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && overviewCard != null && overviewCard.getView() != null) {
			View view = overviewCard.getView();
			TextView distanceText = view.findViewById(R.id.distance);
			ImageView direction = view.findViewById(R.id.direction);
			app.getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, latLon);
		}
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
		}
	}

	@Override
	public void renamedTo(File file) {
		updateFile(file);
	}

	@Override
	public void onFileMove(@NonNull File src, @NonNull File dest) {
		File file = FileUtils.renameGpxFile(app, src, dest);
		if (file != null) {
			updateFile(file);
		} else {
			app.showToastMessage(R.string.file_can_not_be_renamed);
		}
	}

	private void updateFile(File file) {
		displayHelper.setFile(file);
		displayHelper.updateDisplayGroups();
		updateGpxTitle();
		toolbarTextView.setText(gpxTitle);
		updateHeader();
		updateContent();
	}

	public void updateControlsVisibility(boolean menuVisible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			updateDisplayGroupsWidget();
			boolean appbarButtonsVisible = getCurrentMenuState() != MenuState.FULL_SCREEN && !shouldShowWidgets();
			AndroidUiHelper.updateVisibility(backButtonContainer, appbarButtonsVisible);
			AndroidUiHelper.updateVisibility(displayGroupsWidget, appbarButtonsVisible || !isPortrait());

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
				&& menuType == TrackMenuType.OVERVIEW
				&& getCurrentMenuState() == MenuState.HEADER_ONLY;
	}

	public boolean shouldShowBottomControls(boolean menuVisible) {
		return !menuVisible || !isPortrait();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(TRACK_FILE_NAME, selectedGpxFile.getGpxFile().path);
		outState.putBoolean(CURRENT_RECORDING, selectedGpxFile.isShowCurrentTrack());
		if (latLon != null) {
			outState.putDouble(KEY_LATITUDE, latLon.getLatitude());
			outState.putDouble(KEY_LONGITUDE, latLon.getLongitude());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public int getStatusBarColorId() {
		if (getView() != null && getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
			return isNightMode() ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
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
		GPXFile gpxFile = getGpx();

		if (card instanceof OptionsCard || card instanceof OverviewCard) {
			if (buttonIndex == SHOW_ON_MAP_BUTTON_INDEX) {
				if (FileUtils.isTempFile(app, getGpx().path)) {
					File srcFile = displayHelper.getFile();
					File destFIle = new File(app.getAppPath(IndexConstants.GPX_TRAVEL_DIR), srcFile.getName());
					onFileMove(srcFile, destFIle);
					gpxFile = getGpx();
				} else {
					boolean gpxFileSelected = !isGpxFileSelected(app, gpxFile);
					app.getSelectedGpxHelper().selectGpxFile(gpxFile, gpxFileSelected, false);
				}
				updateContent();
				mapActivity.refreshMap();
			} else if (buttonIndex == APPEARANCE_BUTTON_INDEX) {
				TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile, this);
			} else if (buttonIndex == DIRECTIONS_BUTTON_INDEX) {
				MapActivityActions mapActions = mapActivity.getMapActions();
				GPXFile gpxFileToDisplay = displayHelper.getGpxFileToDisplay();
				if (gpxFileToDisplay != null) {
					if (gpxFileToDisplay.getNonEmptySegmentsCount() > 1) {
						TrackSelectSegmentBottomSheet.showInstance(fragmentManager, gpxFileToDisplay, this);
					} else {
						startNavigationForGPX(gpxFileToDisplay, mapActions, mapActivity);
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
				new OpenGpxDetailsTask(selectedGpxFile, null, mapActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				dismiss();
			} else if (buttonIndex == ANALYZE_BY_INTERVALS_BUTTON_INDEX) {
				TrkSegment segment = gpxFile.getGeneralSegment();
				if (segment == null) {
					List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
					if (!Algorithms.isEmpty(segments)) {
						segment = segments.get(0);
					}
				}
				GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
				List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
				if (segment != null && !Algorithms.isEmpty(items)) {
					SplitSegmentDialogFragment.showInstance(fragmentManager, displayHelper, items.get(0), segment);
				}
			} else if (buttonIndex == SHARE_BUTTON_INDEX) {
				OsmandApplication app = mapActivity.getMyApplication();
				if (gpxFile.showCurrentTrack) {
					GpxUiHelper.saveAndShareCurrentGpx(app, gpxFile);
				} else if (!Algorithms.isEmpty(gpxFile.path)) {
					GpxUiHelper.saveAndShareGpxWithAppearance(app, gpxFile);
				}
			} else if (buttonIndex == UPLOAD_OSM_BUTTON_INDEX) {
				OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getActivePlugin(OsmEditingPlugin.class);
				if (osmEditingPlugin != null) {
					GpxInfo gpxInfo = new GpxInfo();
					gpxInfo.gpx = gpxFile;
					gpxInfo.file = new File(gpxFile.path);
					osmEditingPlugin.sendGPXFiles(mapActivity, this, gpxInfo);
				}
			} else if (buttonIndex == EDIT_BUTTON_INDEX) {
				app.getSelectedGpxHelper().selectGpxFile(gpxFile, true, false);
				dismiss();
				String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);
				MeasurementToolFragment.showInstance(fragmentManager, fileName, false);
			} else if (buttonIndex == RENAME_BUTTON_INDEX) {
				FileUtils.renameFile(mapActivity, new File(gpxFile.path), this, true);
			} else if (buttonIndex == CHANGE_FOLDER_BUTTON_INDEX) {
				MoveGpxFileBottomSheet.showInstance(fragmentManager, this, gpxFile.path, true, false);
			} else if (buttonIndex == GPS_FILTER_BUTTON_INDEX) {
				GpsFilterFragment.showInstance(fragmentManager, selectedGpxFile, this);
			} else if (buttonIndex == DELETE_BUTTON_INDEX) {
				String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);

				AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, isNightMode()));
				builder.setTitle(getString(R.string.delete_confirmation_msg, fileName));
				builder.setMessage(R.string.are_you_sure);
				final String gpxFilePath = gpxFile.path;
				builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
						R.string.shared_string_ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (FileUtils.removeGpxFile(app, new File(gpxFilePath))) {
									dismiss();
								}
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
		GPXFile gpxFile = displayHelper.getGpxFileToDisplay();
		if (gpxFile != null) {
			QuadRect rect = gpxFile.getRect();
			adjustMapPosition(rect);
		}
	}

	private void fitSelectedPointsGroupOnMap(GpxDisplayGroup group) {
		DisplayGroupsHolder groupsHolder =
				DisplayPointsGroupsHelper.getGroups(app, displayHelper.getPointsOriginalGroups(), null);
		List<GpxDisplayItem> points = groupsHolder.getItemsByGroupName(group.getName());
		if (points != null) {
			QuadRect pointsRect = new QuadRect();
			for (GpxDisplayItem point : points) {
				GPXUtilities.updateQR(pointsRect, point.locationStart, 0, 0);
			}
			adjustMapPosition(pointsRect);
		}
	}

	public static void startNavigationForGPX(final GPXFile gpxFile, MapActivityActions mapActions, final MapActivity mapActivity) {
		if (mapActivity.getMyApplication().getRoutingHelper().isFollowingMode()) {
			final WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
			mapActions.stopNavigationActionConfirm(null, new Runnable() {
				@Override
				public void run() {
					MapActivity activity = activityRef.get();
					if (activity != null) {
						activity.getMapActions().enterRoutePlanningModeGivenGpx(gpxFile, null,
								null, null, true, true, MenuState.HEADER_ONLY);
					}
				}
			});
		} else {
			mapActions.stopNavigationWithoutConfirm();
			mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null,
					null, true, true, MenuState.HEADER_ONLY);
		}
	}

	public void updateToolbar(int y, boolean animated) {
		final MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (toolbarContainer != null && isPortrait()) {
				if (animated) {
					final float toolbarAlpha = getToolbarAlpha(y);
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
			}
		}
	}

	@Override
	protected void onHeaderClick() {
		if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
			updateMenuState();
		}
	}

	private void adjustMapPosition(QuadRect r) {
		int y = getMenuStatePosY(getCurrentMenuState());
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView();
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(mapActivity);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (r.left != 0 && r.right != 0) {
				mapActivity.getMapView().fitRectToMap(r.left, r.right, r.top, r.bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
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
		bottomNav.setOnNavigationItemSelectedListener(item -> {
			for (TrackMenuType type : TrackMenuType.values()) {
				if (type.menuItemId == item.getItemId()) {
					TrackMenuType prevMenuType = menuType;
					menuType = type;
					menuTypeChanged = prevMenuType != type;
					setupCards();
					updateHeader();
					updateHeadersBottomShadow();
					updateControlsVisibility(isVisible());
					updateCardsLayout();
					if (type == TrackMenuType.OVERVIEW && isPortrait() && overviewInitialHeight
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

	private void calculateLayoutAndUpdateMenuState(@Nullable TrackMenuType prevMenuType) {
		if (getCurrentMenuState() == 2 && overviewInitialHeight && prevMenuType == TrackMenuType.OVERVIEW) {
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
			updateMapControlsPos(TrackMenuFragment.this, overviewInitialPosY, true);
		});
	}

	private void updateMenuState() {
		if (menuType == TrackMenuType.OPTIONS) {
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
		updatePointGroupsCard();
		setupCards();
	}

	private void updatePointGroupsCard() {
		if (groupsCard != null) {
			groupsCard.updateContent();
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
	public void onPointsDeletionStarted() {

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
	public void showOptionsPopupMenu(View view, final TrkSegment segment, final boolean confirmDeletion, final GpxDisplayItem gpxItem) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			IconPopupMenu optionsPopupMenu = new IconPopupMenu(activity, view.findViewById(R.id.overflow_menu));
			Menu menu = optionsPopupMenu.getMenu();
			optionsPopupMenu.getMenuInflater().inflate(R.menu.track_segment_menu, menu);
			menu.findItem(R.id.action_edit).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_edit_dark));
			menu.findItem(R.id.action_delete).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			if (getGpx().showCurrentTrack) {
				menu.findItem(R.id.split_interval).setVisible(false);
			} else {
				menu.findItem(R.id.split_interval).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_split_interval));
			}
			optionsPopupMenu.setOnMenuItemClickListener(new IconPopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					int i = item.getItemId();
					if (i == R.id.action_edit) {
						editSegment();
						return true;
					} else if (i == R.id.action_delete) {
						FragmentActivity activity = getActivity();
						if (!confirmDeletion) {
							deleteAndSaveSegment(segment);
						} else if (activity != null) {
							AlertDialog.Builder builder = new AlertDialog.Builder(activity);
							builder.setMessage(R.string.recording_delete_confirm);
							builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									deleteAndSaveSegment(segment);
								}
							});
							builder.setNegativeButton(R.string.shared_string_cancel, null);
							builder.show();
						}
						return true;
					} else if (i == R.id.split_interval) {
						openSplitInterval(gpxItem, segment);
					}
					return false;
				}
			});
			optionsPopupMenu.show();
		}
	}

	@Override
	public void onSegmentSelect(GPXFile gpxFile, int selectedSegment) {
		app.getSettings().GPX_ROUTE_SEGMENT.set(selectedSegment);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			startNavigationForGPX(gpxFile, mapActivity.getMapActions(), mapActivity);
			GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
			if (paramsBuilder != null) {
				paramsBuilder.setSelectedSegment(selectedSegment);
				app.getRoutingHelper().onSettingsChanged(true);
			}
			dismiss();
		}
	}

	private void editSegment() {
		GPXFile gpxFile = getGpx();
		openPlanRoute(new GpxData(gpxFile));
		hide();
	}

	public void openPlanRoute(GpxData gpxData) {
		QuadRect qr = gpxData.getRect();
		getMapActivity().getMapView().fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
		MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
		editingContext.setGpxData(gpxData);
		MeasurementToolFragment.showInstance(getFragmentManager(), editingContext);
	}

	private void deleteAndSaveSegment(TrkSegment segment) {
		if (deleteSegment(segment)) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				boolean showOnMap = GpxSelectionHelper.isGpxFileSelected(app, gpx);
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpx, showOnMap, false);
				saveGpx(showOnMap ? selectedGpxFile : null, gpx);
			}
		}
	}

	private boolean deleteSegment(TrkSegment segment) {
		if (segment != null) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				return gpx.removeTrkSegment(segment);
			}
		}
		return false;
	}

	private void saveGpx(final SelectedGpxFile selectedGpxFile, GPXFile gpxFile) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (selectedGpxFile != null) {
					List<GpxDisplayGroup> groups = displayHelper.getDisplayGroups(new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT});
					selectedGpxFile.setDisplayGroups(groups, app);
					selectedGpxFile.processPoints(app);
				}
				updateContent();
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
	public void onFinishFiltering(@NonNull GPXFile filteredGpxFile) {
		displayHelper.setFilteredGpxFile(filteredGpxFile);
		updateContent();
	}

	@Override
	public void onDismissGpsFilterFragment(boolean savedCopy, @Nullable String savedFilePath) {
		if (savedCopy) {
			dismiss();
		}
	}

	public static void openTrack(@NonNull Context context, @Nullable File file, Bundle prevIntentParams) {
		openTrack(context, file, prevIntentParams, null);
	}

	public static void openTrack(@NonNull Context context, @Nullable File file, @Nullable Bundle prevIntentParams,
	                             @Nullable String returnScreenName) {
		openTrack(context, file, prevIntentParams, returnScreenName, TrackMenuType.OVERVIEW);
	}

	public static void openTrack(@NonNull Context context, @Nullable File file, @Nullable Bundle prevIntentParams,
	                             @Nullable String returnScreenName, TrackMenuType tabToOpen) {
		boolean currentRecording = file == null;
		String path = file != null ? file.getAbsolutePath() : null;
		if (context instanceof MapActivity) {
			TrackMenuFragment.showInstance((MapActivity) context, path, currentRecording, null, null, null);
		} else {
			Bundle bundle = new Bundle();
			bundle.putString(TRACK_FILE_NAME, path);
			bundle.putBoolean(OPEN_TRACK_MENU, true);
			bundle.putBoolean(CURRENT_RECORDING, currentRecording);
			bundle.putString(RETURN_SCREEN_NAME, returnScreenName);
			bundle.putString(OPEN_TAB_NAME, tabToOpen.name());
			MapActivity.launchMapActivityMoveToTop(context, prevIntentParams, null, bundle);
		}
	}

	public static void loadSelectedGpxFile(@NonNull MapActivity mapActivity, @Nullable String path,
	                                       boolean showCurrentTrack,
	                                       final CallbackWithObject<SelectedGpxFile> callback) {
		OsmandApplication app = mapActivity.getMyApplication();
		SelectedGpxFile selectedGpxFile;
		if (showCurrentTrack) {
			selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		} else {
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(path);
		}
		if (selectedGpxFile != null) {
			callback.processResult(selectedGpxFile);
		} else if (!Algorithms.isEmpty(path)) {
			final ProgressDialog[] progress = new ProgressDialog[1];
			if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
				String title = app.getString(R.string.loading_smth, "");
				progress[0] = ProgressDialog.show(mapActivity, title, app.getString(R.string.loading_data));
			}
			final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
			GpxFileLoaderTask gpxFileLoaderTask = new GpxFileLoaderTask(new File(path), new CallbackWithObject<GPXFile>() {
				@Override
				public boolean processResult(GPXFile result) {
					SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(result, true, false);
					if (selectedGpxFile != null) {
						callback.processResult(selectedGpxFile);
					}
					MapActivity mapActivity = mapActivityRef.get();
					if (progress[0] != null && AndroidUtils.isActivityNotDestroyed(mapActivity)) {
						progress[0].dismiss();
					}
					return true;
				}
			});
			gpxFileLoaderTask.execute();
		}
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @Nullable String path,
	                                boolean showCurrentTrack,
	                                @Nullable final String returnScreenName,
	                                @Nullable final String callingFragmentTag,
	                                @Nullable final String tabToOpenName) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		loadSelectedGpxFile(mapActivity, path, showCurrentTrack, new CallbackWithObject<SelectedGpxFile>() {
			@Override
			public boolean processResult(SelectedGpxFile selectedGpxFile) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null && selectedGpxFile != null) {
					showInstance(mapActivity, selectedGpxFile, null, returnScreenName, callingFragmentTag, tabToOpenName, true, null);
				}
				return true;
			}
		});
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @Nullable SelectedGpxPoint gpxPoint) {
		return showInstance(mapActivity, selectedGpxFile, gpxPoint, null, null, null, false, null);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @Nullable SelectedGpxPoint gpxPoint,
	                                   @Nullable String returnScreenName,
	                                   @Nullable String callingFragmentTag,
	                                   @Nullable String tabToOpenName,
	                                   boolean adjustMapPosition,
	                                   @Nullable GPXTrackAnalysis analyses) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, MenuState.HEADER_ONLY);

			TrackMenuFragment fragment = new TrackMenuFragment();
			fragment.setArguments(args);
			fragment.setRetainInstance(true);
			fragment.setAnalyses(analyses);
			fragment.setSelectedGpxFile(selectedGpxFile);
			fragment.setReturnScreenName(returnScreenName);
			fragment.setCallingFragmentTag(callingFragmentTag);
			fragment.setAdjustMapPosition(adjustMapPosition);
			if (tabToOpenName != null) {
				fragment.setMenuType(TrackMenuType.valueOf(tabToOpenName));
			}

			if (gpxPoint != null) {
				WptPt wptPt = gpxPoint.getSelectedPoint();
				fragment.setLatLon(new LatLon(wptPt.lat, wptPt.lon));
				fragment.setGpxPoint(gpxPoint);
			} else {
				QuadRect rect = selectedGpxFile.getGpxFile().getRect();
				LatLon latLonRect = new LatLon(rect.centerY(), rect.centerX());
				fragment.setLatLon(latLonRect);
			}

			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}