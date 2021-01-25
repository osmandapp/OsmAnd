package net.osmand.plus.track;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.FileUtils.RenameCallback;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.OpenGpxDetailsTask;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.GPXTabItemType;
import net.osmand.plus.myplaces.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SplitSegmentDialogFragment;
import net.osmand.plus.myplaces.TrackActivityFragmentAdapter;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.widgets.IconPopupMenu;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static net.osmand.plus.activities.TrackActivity.CURRENT_RECORDING;
import static net.osmand.plus.activities.TrackActivity.TRACK_FILE_NAME;
import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.isGpxFileSelected;
import static net.osmand.plus.track.OptionsCard.ANALYZE_BY_INTERVALS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.ANALYZE_ON_MAP_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.CHANGE_FOLDER_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DELETE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.JOIN_GAPS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.RENAME_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHARE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.UPLOAD_OSM_BUTTON_INDEX;

public class TrackMenuFragment extends ContextMenuScrollFragment implements CardListener,
		SegmentActionsListener, RenameCallback, OnTrackFileMoveListener, OsmAndLocationListener, OsmAndCompassListener {

	public static final String TAG = TrackMenuFragment.class.getName();
	private static final Log log = PlatformUtil.getLog(TrackMenuFragment.class);

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SelectedGpxFile selectedGpxFile;

	private View routeMenuTopShadowAll;
	private TextView headerTitle;
	private ImageView headerIcon;
	private BottomNavigationView bottomNav;
	private TrackMenuType menuType = TrackMenuType.OVERVIEW;
	private SegmentsCard segmentsCard;
	private OptionsCard optionsCard;
	private DescriptionCard descriptionCard;
	private OverviewCard overviewCard;

	private TrackChartPoints trackChartPoints;

	private int menuTitleHeight;
	private String gpxTitle;
	private UpdateLocationViewCache updateLocationViewCache;
	private Location lastLocation = null;
	private Float heading;
	private boolean locationUpdateStarted;

	public enum TrackMenuType {
		OVERVIEW(R.id.action_overview, R.string.shared_string_overview),
		TRACK(R.id.action_track, R.string.shared_string_gpx_tracks),
		POINTS(R.id.action_points, R.string.shared_string_gpx_points),
		OPTIONS(R.id.action_options, R.string.shared_string_options);

		TrackMenuType(@DrawableRes int iconId, @StringRes int titleId) {
			this.iconId = iconId;
			this.titleId = titleId;
		}

		public final int iconId;
		public final int titleId;
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
		return 0;
	}

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HEADER_ONLY;
	}

	public TrackDisplayHelper getDisplayHelper() {
		return displayHelper;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		displayHelper = new TrackDisplayHelper(app);

		Bundle arguments = getArguments();
		if (arguments != null) {
			String gpxFilePath = arguments.getString(TRACK_FILE_NAME);
			boolean currentRecording = arguments.getBoolean(CURRENT_RECORDING, false);
			if (currentRecording) {
				selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			} else {
				File file = new File(gpxFilePath);
				displayHelper.setFile(file);
				displayHelper.setGpxDataItem(gpxDbHelper.getItem(file));
				selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			}
			displayHelper.setGpx(selectedGpxFile.getGpxFile());
			String fileName = Algorithms.getFileWithoutDirs(getGpx().path);
			gpxTitle = GpxUiHelper.getGpxTitle(fileName);
		}
	}

	public GPXFile getGpx() {
		return displayHelper.getGpx();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			bottomNav = view.findViewById(R.id.bottom_navigation);
			routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);
			headerTitle = view.findViewById(R.id.title);
			headerIcon = view.findViewById(R.id.icon_view);
			updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();

			if (isPortrait()) {
				AndroidUiHelper.updateVisibility(getTopShadow(), true);
				AndroidUtils.setBackground(view.getContext(), getBottomContainer(), isNightMode(),
						R.color.list_background_color_light, R.color.list_background_color_dark);
			} else {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				bottomNav.setLayoutParams(params);
			}

			setupCards();
			updateHeader();
			setupButtons(view);
			enterTrackAppearanceMode();
			runLayoutListener();
		}
		return view;
	}

	private void setHeaderTitle(String text, boolean iconVisibility) {
		headerTitle.setText(text);
		AndroidUiHelper.updateVisibility(headerIcon, iconVisibility);
	}

	private void updateHeader() {
		ViewGroup headerContainer = (ViewGroup) routeMenuTopShadowAll;
		if (menuType == TrackMenuType.OVERVIEW) {
			setHeaderTitle(gpxTitle, true);
			if (overviewCard != null && overviewCard.getView() != null) {
				ViewGroup parent = ((ViewGroup) overviewCard.getView().getParent());
				if (parent != null) {
					parent.removeView(overviewCard.getView());
				}
				headerContainer.addView(overviewCard.getView());
			} else {
				overviewCard = new OverviewCard(getMapActivity(), displayHelper, this);
				overviewCard.setListener(this);
				headerContainer.addView(overviewCard.build(getMapActivity()));
			}
		} else {
			if (overviewCard != null && overviewCard.getView() != null) {
				headerContainer.removeView(overviewCard.getView());
			}
			boolean isOptions = menuType == TrackMenuType.OPTIONS;
			setHeaderTitle(isOptions ? app.getString(menuType.titleId) : gpxTitle, !isOptions);
		}
	}

	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();
			if (menuType == TrackMenuType.TRACK) {
				if (segmentsCard != null && segmentsCard.getView() != null) {
					ViewGroup parent = (ViewGroup) segmentsCard.getView().getParent();
					if (parent != null) {
						parent.removeAllViews();
					}
					cardsContainer.addView(segmentsCard.getView());
				} else {
					segmentsCard = new SegmentsCard(mapActivity, displayHelper, this);
					segmentsCard.setListener(this);
					cardsContainer.addView(segmentsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuType.OPTIONS) {
				if (optionsCard != null && optionsCard.getView() != null) {
					ViewGroup parent = (ViewGroup) optionsCard.getView().getParent();
					if (parent != null) {
						parent.removeAllViews();
					}
					cardsContainer.addView(optionsCard.getView());
				} else {
					optionsCard = new OptionsCard(mapActivity, displayHelper);
					optionsCard.setListener(this);
					cardsContainer.addView(optionsCard.build(mapActivity));
				}
			} else if (menuType == TrackMenuType.OVERVIEW) {
				if (descriptionCard != null && descriptionCard.getView() != null) {
					ViewGroup parent = ((ViewGroup) descriptionCard.getView().getParent());
					if (parent != null) {
						cardsContainer.removeView(descriptionCard.getView());
					}
					cardsContainer.addView(descriptionCard.getView());
				} else {
					descriptionCard = new DescriptionCard(getMapActivity(), displayHelper.getGpx());
					cardsContainer.addView(descriptionCard.build(mapActivity));
				}
			}
		}
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = routeMenuTopShadowAll.getHeight()
				+ bottomNav.getHeight();
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
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY || menuState == MenuState.HALF_SCREEN;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adjustMapPosition(getHeight());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitTrackAppearanceMode();
		updateStatusBarColor();
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && trackChartPoints != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		}
		updateHeader();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
		}
		stopLocationUpdate();
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
		View view = overviewCard.getView();
		if (mapActivity != null && view != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			TextView distanceText = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			app.getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, menu.getLatLon());
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
		updateHeader();
		updateContent();
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
		outState.putString(TRACK_FILE_NAME, getGpx().path);
		outState.putBoolean(CURRENT_RECORDING, selectedGpxFile.isShowCurrentTrack());
		super.onSaveInstanceState(outState);
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
		if (card instanceof OptionsCard || card instanceof OverviewCard) {
			final GPXFile gpxFile = getGpx();
			if (buttonIndex == SHOW_ON_MAP_BUTTON_INDEX) {
				boolean gpxFileSelected = !isGpxFileSelected(app, gpxFile);
				app.getSelectedGpxHelper().selectGpxFile(gpxFile, gpxFileSelected, false);
				mapActivity.refreshMap();
			} else if (buttonIndex == APPEARANCE_BUTTON_INDEX) {
				TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile);
			} else if (buttonIndex == DIRECTIONS_BUTTON_INDEX) {
				MapActivityActions mapActions = mapActivity.getMapActions();
				if (app.getRoutingHelper().isFollowingMode()) {
					mapActions.stopNavigationActionConfirm(null, new Runnable() {
						@Override
						public void run() {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpxFile, null,
										null, null, true, true, MenuState.HEADER_ONLY);
							}
						}
					});
				} else {
					mapActions.stopNavigationWithoutConfirm();
					mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null,
							null, true, true, MenuState.HEADER_ONLY);
				}
				dismiss();
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
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
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
				OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
				if (osmEditingPlugin != null) {
					GpxInfo gpxInfo = new GpxInfo();
					gpxInfo.gpx = gpxFile;
					gpxInfo.file = new File(gpxFile.path);
					osmEditingPlugin.sendGPXFiles(mapActivity, this, gpxInfo);
				}
			} else if (buttonIndex == EDIT_BUTTON_INDEX) {
				String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);
				MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), fileName);
				dismiss();
			} else if (buttonIndex == RENAME_BUTTON_INDEX) {
				FileUtils.renameFile(mapActivity, new File(gpxFile.path), this, true);
			} else if (buttonIndex == CHANGE_FOLDER_BUTTON_INDEX) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				MoveGpxFileBottomSheet.showInstance(fragmentManager, this, gpxFile.path, true);
			} else if (buttonIndex == DELETE_BUTTON_INDEX) {
				String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);

				AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, isNightMode()));
				builder.setTitle(getString(R.string.delete_confirmation_msg, fileName));
				builder.setMessage(R.string.are_you_sure);
				builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
						R.string.shared_string_ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (FileUtils.removeGpxFile(app, new File((gpxFile.path)))) {
									dismiss();
								}
							}
						});
				builder.show();
			}
		}
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
	protected void onHeaderClick() {
		adjustMapPosition(getViewY());
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && mapActivity.getMapView() != null) {
			GPXFile gpxFile = getGpx();
			QuadRect r = gpxFile.getRect();

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
		}
	}

	/*private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
			}
		}
	}*/

	private void setupButtons(View view) {
		ColorStateList navColorStateList = AndroidUtils.createBottomNavColorStateList(getContext(), isNightMode());
		BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
		bottomNav.setItemIconTintList(navColorStateList);
		bottomNav.setItemTextColor(navColorStateList);
		bottomNav.setSelectedItemId(R.id.action_overview);
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				for (TrackMenuType type : TrackMenuType.values()) {
					if (type.iconId == item.getItemId()) {
						menuType = type;
						setupCards();
						updateHeader();
						break;
					}
				}
				return true;
			}
		});
	}

	@Override
	public void updateContent() {
		if (segmentsCard != null) {
			segmentsCard.updateContent();
		}
		if (optionsCard != null) {
			optionsCard.updateContent();
		}
		if (descriptionCard != null) {
			descriptionCard.updateContent();
		}
		setupCards();
	}

	@Override
	public void onChartTouch() {
		getBottomScrollView().requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void scrollBy(int px) {

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
		if (fragmentManager != null) {
			SplitSegmentDialogFragment.showInstance(fragmentManager, displayHelper, gpxItem, trkSegment);
		}
	}

	@Override
	public void openAnalyzeOnMap(GpxDisplayItem gpxItem) {
		TrackDetailsMenu trackDetailsMenu = getMapActivity().getTrackDetailsMenu();
		trackDetailsMenu.setGpxItem(gpxItem);
		trackDetailsMenu.show();
		close();
	}

	@Override
	public void showOptionsPopupMenu(View view, final TrkSegment segment, final boolean confirmDeletion) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			IconPopupMenu optionsPopupMenu = new IconPopupMenu(activity, view.findViewById(R.id.overflow_menu));
			Menu menu = optionsPopupMenu.getMenu();
			optionsPopupMenu.getMenuInflater().inflate(R.menu.track_segment_menu, menu);
			menu.findItem(R.id.action_edit).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_edit_dark));
			menu.findItem(R.id.action_delete).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			optionsPopupMenu.setOnMenuItemClickListener(new IconPopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					int i = item.getItemId();
					if (i == R.id.action_edit) {
						editSegment(segment);
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
					}
					return false;
				}
			});
			optionsPopupMenu.show();
		}
	}

	private void editSegment(TrkSegment segment) {
		GPXFile gpxFile = getGpx();
		openPlanRoute(new GpxData(gpxFile));
		close();
	}

	public void openPlanRoute(GpxData gpxData) {
		QuadRect qr = gpxData.getRect();
		getMapActivity().getMapView().fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
		MeasurementEditingContext editingContext = new MeasurementEditingContext();
		editingContext.setGpxData(gpxData);
		MeasurementToolFragment.showInstance(getFragmentManager(), editingContext);
	}

	private void deleteAndSaveSegment(TrkSegment segment) {
		if (deleteSegment(segment)) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				boolean showOnMap = TrackActivityFragmentAdapter.isGpxFileSelected(app, gpx);
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

	public static boolean showInstance(@NonNull MapActivity mapActivity, String path, boolean showCurrentTrack) {
		try {
			Bundle args = new Bundle();
			args.putString(TRACK_FILE_NAME, path);
			args.putBoolean(CURRENT_RECORDING, showCurrentTrack);
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, MenuState.HEADER_ONLY);

			TrackMenuFragment fragment = new TrackMenuFragment();
			fragment.setArguments(args);
			fragment.setRetainInstance(true);

			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, fragment.getFragmentTag())
					.addToBackStack(fragment.getFragmentTag())
					.commitAllowingStateLoss();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}